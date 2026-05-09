package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.hyosung.model.EjTransaction;
import com.jpb.reconciliation.reconciliation.hyosung.model.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.hyosung.parser.EjTransactionParser;
import com.jpb.reconciliation.reconciliation.hyosung.reader.EjFileReader;
import com.jpb.reconciliation.reconciliation.db.HyosungEjTransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HyosungEjFileLoadServiceImpl implements HyosungEjFileLoadService {

    private static final Logger logger = LoggerFactory.getLogger(HyosungEjFileLoadServiceImpl.class);

    private final HyosungEjTransactionRepository ejRepository;

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
    // Request handler — business logic moved from controller
    // -------------------------------------------------------------------------
    @Override
    public EjLoadResult loadEjFiles(Map<String, Object> request) {
        String inputDir = (String) request.get("inputDir");
        if (inputDir == null || inputDir.trim().isEmpty()) {
            return new EjLoadResult(0, 0, 0, 0, false, 0, 0, null, "inputDir is required");
        }

        Path dirPath = Paths.get(inputDir.trim());
        if (!Files.isDirectory(dirPath)) {
            return new EjLoadResult(0, 0, 0, 0, false, 0, 0, null,
                    "inputDir does not exist: " + inputDir);
        }

        String glob = getOrDefault(request, "glob", "*.txt");

        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) files.add(p);
            }
        } catch (IOException e) {
            logger.error("Could not read directory {}: {}", inputDir, e.getMessage());
            return new EjLoadResult(0, 0, 0, 0, false, 0, 0, null,
                    "Could not read directory: " + e.getMessage());
        }

        if (files.isEmpty()) {
            logger.warn("No files found in directory: {}", inputDir);
            return new EjLoadResult(0, 0, 0, 0, true, 0, 0, null, "No files found in: " + inputDir);
        }

        String batchId = "batch-" + UUID.randomUUID();
        logger.info("Hyosung EJ Load started. batchId={}, files={}, dir={}", batchId, files.size(), inputDir);

        EjLoadResult result = loadDirectory(dirPath, glob);

        logger.info("Hyosung EJ Load complete. batchId={}, status={}, inserted={}",
                batchId, result.overallStatus(), result.inserted);

        return new EjLoadResult(result.parsed, result.inserted, result.errors,
                result.elapsedMs, result.success, result.totalFiles, result.failedFiles,
                batchId, null);
    }

    // -------------------------------------------------------------------------
    // Single file load
    // -------------------------------------------------------------------------
    @Override
    public EjLoadResult loadFile(Path file) {
        String batchId = "batch-" + UUID.randomUUID();
        EjTransactionParser parser = new EjTransactionParser(batchId, atmIdPrefix);
        Charset charset = resolveCharset();

        long t0 = System.nanoTime();
        long parsedCount = 0, insertedCount = 0, errorCount = 0;
        List<EjTransaction> batch = new ArrayList<>(batchSize);
        boolean fileLevelFailure = false;

        logger.info("Loading Hyosung EJ file: {}", file);

        try (EjFileReader reader = new EjFileReader(file, charset);
                Stream<RawTransactionBlock> blocks = reader.stream()) {

            for (RawTransactionBlock block : (Iterable<RawTransactionBlock>) blocks::iterator) {
                EjTransaction txn;
                try {
                    txn = parser.parse(block);
                    parsedCount++;
                } catch (Exception ex) {
                    errorCount++;
                    logger.error("Parser error on block {} lines {}-{}: {}", block.getFileName(),
                            block.getLineStart(), block.getLineEnd(), ex.getMessage());
                    continue;
                }

                batch.add(txn);
                if (batch.size() >= batchSize) {
                    insertedCount += flush(batch);
                }
            }
            insertedCount += flush(batch);

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
            logger.warn("Aborted {} : parsed={}, inserted={}, errors={}, elapsed={}ms",
                    file.getFileName(), parsedCount, insertedCount, errorCount, elapsedMs);
            return new EjLoadResult(parsedCount, insertedCount, errorCount + 1,
                    elapsedMs, false, 1, 1, null, null);
        }

        if (archiveOnSuccess && archiveDirStr != null && !archiveDirStr.trim().isEmpty()) {
            archive(file, Paths.get(archiveDirStr));
        }

        logger.info("Done {} : parsed={}, inserted={}, errors={}, elapsed={}ms",
                file.getFileName(), parsedCount, insertedCount, errorCount, elapsedMs);

        return new EjLoadResult(parsedCount, insertedCount, errorCount,
                elapsedMs, true, 1, 0, null, null);
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
                if (Files.isRegularFile(p)) files.add(p);
            }
        } catch (IOException e) {
            logger.error("Failed to list files in directory {}: {}", inputDir, e.getMessage());
            return new EjLoadResult(0, 0, 0, 0, false, 0, 0, null, null);
        }

        if (files.isEmpty()) {
            logger.warn("No files found in directory: {}", inputDir);
            return new EjLoadResult(0, 0, 0, 0, true, 0, 0, null, null);
        }

        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        logger.info("Found {} files to process in: {}", files.size(), inputDir);

        long totalParsed = 0, totalInserted = 0, totalErrors = 0, totalElapsed = 0;
        int failedFiles = 0;

        for (Path file : files) {
            EjLoadResult result = loadFile(file);
            totalParsed   += result.parsed;
            totalInserted += result.inserted;
            totalErrors   += result.errors;
            totalElapsed  += result.elapsedMs;
            if (!result.success) failedFiles++;
        }

        boolean overallSuccess = failedFiles == 0;
        logger.info("Directory load complete: files={}, failed={}, inserted={}, elapsed={}ms",
                files.size(), failedFiles, totalInserted, totalElapsed);

        return new EjLoadResult(totalParsed, totalInserted, totalErrors,
                totalElapsed, overallSuccess, files.size(), failedFiles, null, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private int flush(List<EjTransaction> batch) {
        if (batch.isEmpty()) return 0;
        try {
            int n = ejRepository.insertBatch(batch);
            batch.clear();
            return n;
        } catch (SQLException e) {
            throw new BatchInsertFailedException(
                    "Batch insert failed (size=" + batch.size() + "): " + e.getMessage(), e);
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

    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        String str = val.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }

    private static final class BatchInsertFailedException extends RuntimeException {
        BatchInsertFailedException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}