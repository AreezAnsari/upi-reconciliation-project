package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.db.EjTransactionRepository;
import com.jpb.reconciliation.reconciliation.dto.EjRawTransactionBlock;
import com.jpb.reconciliation.reconciliation.dto.EjTransaction;
import com.jpb.reconciliation.reconciliation.parser.EjTransactionParser;
import com.jpb.reconciliation.reconciliation.reader.EjFileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class EjFileLoadServiceImpl implements EjFileLoadService {

	private static final Logger logger = LoggerFactory.getLogger(EjFileLoadServiceImpl.class);

	@Autowired
	private DataSource dataSource;

	@Value("${db.batchSize:500}")
	private int batchSize;

	@Value("${parser.atmIdPrefix:}")
	private String atmIdPrefix;

	@Value("${input.charset:ISO-8859-1}")
	private String charsetName;

	@Value("${input.archiveOnSuccess:false}")
	private boolean archiveOnSuccess;

	@Value("${input.archiveDir:}")
	private String archiveDirStr;

	// -------------------------------------------------------------------------
	// Single file load
	// -------------------------------------------------------------------------
	@Override
	public EjLoadResult loadFile(Path file) {
		String batchId = "batch-" + UUID.randomUUID();
		EjTransactionRepository repo = new EjTransactionRepository(dataSource);
		EjTransactionParser parser = new EjTransactionParser(batchId, atmIdPrefix);
		Charset charset = resolveCharset();

		long t0 = System.nanoTime();
		long parsedCount = 0, insertedCount = 0, errorCount = 0;
		List<EjTransaction> batch = new ArrayList<>(batchSize);
		boolean fileLevelFailure = false;

		logger.info("Loading EJ file: {}", file);

		try (EjFileReader reader = new EjFileReader(file, charset);
				Stream<EjRawTransactionBlock> blocks = reader.stream()) {

			for (EjRawTransactionBlock block : (Iterable<EjRawTransactionBlock>) blocks::iterator) {
				EjTransaction txn;
				try {
					txn = parser.parse(block);
					parsedCount++;
				} catch (Exception ex) {
					errorCount++;
					logger.error("Parser error on block {} lines {}-{}: {}", block.getFileName(), block.getLineStart(),
							block.getLineEnd(), ex.getMessage());
					continue;
				}

				batch.add(txn);
				if (batch.size() >= batchSize) {
					insertedCount += flush(batch, repo);
				}
			}
			insertedCount += flush(batch, repo);

		} catch (IOException | UncheckedIOException ioe) {
			logger.error("I/O error reading {}: {}", file, ioe.toString());
			fileLevelFailure = true;
		} catch (BatchInsertFailedException bie) {
			logger.error("Batch insert failed for {}: {}", file, bie.getMessage());
			fileLevelFailure = true;
		} catch (RuntimeException unexpected) {
			logger.error("Unexpected error processing {}: {}", file, unexpected.toString(), unexpected);
			fileLevelFailure = true;
		}

		long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

		if (fileLevelFailure) {
			logger.warn("Aborted {} : parsed={}, inserted={}, errors={}, elapsed={}ms", file.getFileName(), parsedCount,
					insertedCount, errorCount, elapsedMs);
			return new EjLoadResult(parsedCount, insertedCount, errorCount + 1, elapsedMs, false, 1, 1);
		}

		// Archive if configured
		if (archiveOnSuccess && archiveDirStr != null && !archiveDirStr.trim().isEmpty()) {
			archive(file, Paths.get(archiveDirStr));
		}

		logger.info("Done {} : parsed={}, inserted={}, errors={}, elapsed={}ms", file.getFileName(), parsedCount,
				insertedCount, errorCount, elapsedMs);

		return new EjLoadResult(parsedCount, insertedCount, errorCount, elapsedMs, true, 1, 0);
	}

	// -------------------------------------------------------------------------
	// Directory load
	// -------------------------------------------------------------------------
	@Override
	public EjLoadResult loadDirectory(Path inputDir, String glob) {
		List<Path> files = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir,
				(glob != null && !glob.trim().isEmpty()) ? glob : "*.txt")) {
			for (Path p : stream) {
				if (Files.isRegularFile(p))
					files.add(p);
			}
		} catch (IOException e) {
			logger.error("Failed to list files in directory {}: {}", inputDir, e.getMessage());
			return new EjLoadResult(0, 0, 0, 0, false, 0, 0);
		}

		if (files.isEmpty()) {
			logger.warn("No files found in directory: {}", inputDir);
			return new EjLoadResult(0, 0, 0, 0, true, 0, 0);
		}

		files.sort(Comparator.comparing(p -> p.getFileName().toString()));
		logger.info("Found {} files to process in: {}", files.size(), inputDir);

		long totalParsed = 0, totalInserted = 0, totalErrors = 0, totalElapsed = 0;
		int failedFiles = 0;

		for (Path file : files) {
			EjLoadResult result = loadFile(file);
			totalParsed += result.parsed;
			totalInserted += result.inserted;
			totalErrors += result.errors;
			totalElapsed += result.elapsedMs;
			if (!result.success)
				failedFiles++;
		}

		boolean overallSuccess = failedFiles == 0;
		logger.info("Directory load complete: files={}, failed={}, inserted={}, elapsed={}ms", files.size(),
				failedFiles, totalInserted, totalElapsed);

		return new EjLoadResult(totalParsed, totalInserted, totalErrors, totalElapsed, overallSuccess, files.size(),
				failedFiles);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------
	private int flush(List<EjTransaction> batch, EjTransactionRepository repo) {
		if (batch.isEmpty())
			return 0;
		try {
			int n = repo.insertBatch(batch);
			batch.clear();
			return n;
		} catch (SQLException e) {
			throw new BatchInsertFailedException("Batch insert failed (size=" + batch.size() + "): " + e.getMessage(),
					e);
		}
	}

	private void archive(Path file, Path archiveDir) {
		try {
			Files.createDirectories(archiveDir);
			Path target = archiveDir.resolve(file.getFileName());
			Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Archived {} -> {}", file, target);
		} catch (IOException e) {
			logger.warn("Could not archive {}: {}", file, e.toString());
		}
	}

	private Charset resolveCharset() {
		try {
			return Charset.forName(charsetName);
		} catch (Exception e) {
			logger.warn("Invalid charset '{}', falling back to ISO-8859-1", charsetName);
			return StandardCharsets.ISO_8859_1;
		}
	}

	private static final class BatchInsertFailedException extends RuntimeException {
		BatchInsertFailedException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
	
	public EjFileLoadServiceImpl(DataSource dataSource, int batchSize,
	        String atmIdPrefix, String charsetName,
	        boolean archiveOnSuccess, String archiveDirStr) {
	    this.dataSource       = dataSource;
	    this.batchSize        = batchSize;
	    this.atmIdPrefix      = atmIdPrefix;
	    this.charsetName      = charsetName;
	    this.archiveOnSuccess = archiveOnSuccess;
	    this.archiveDirStr    = archiveDirStr;
	}
}