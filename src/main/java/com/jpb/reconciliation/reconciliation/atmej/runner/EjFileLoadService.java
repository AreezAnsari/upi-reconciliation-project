package com.jpb.reconciliation.reconciliation.atmej.runner;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.atmej.db.EjTransactionRepository;
import com.jpb.reconciliation.reconciliation.atmej.dto.EjTransaction;
import com.jpb.reconciliation.reconciliation.atmej.dto.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.atmej.parser.EjTransactionParser;
import com.jpb.reconciliation.reconciliation.atmej.reader.EjFileReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orchestrates the EJ-file pipeline:
 * <pre>
 *   File -> {@link EjFileReader} -> {@link EjTransactionParser} -> {@link EjTransactionRepository}
 * </pre>
 *
 * <p>One call to {@link #loadFile(Path)} processes a single file. Records are
 * accumulated up to {@code batchSize} and inserted as a single JDBC batch.
 * On JDBC failure the file aborts (returning partial counts and
 * {@code success=false}); other files in the run still process.
 */
public final class EjFileLoadService {

    private static final Logger LOG = LoggerFactory.getLogger(EjFileLoadService.class);

    private final EjTransactionParser     parser;
    private final EjTransactionRepository repo;
    private final int                     batchSize;
    private final Charset                 charset;
    private final boolean                 archiveOnSuccess;
    private final Path                    archiveDir;

    public EjFileLoadService(EjTransactionParser parser,
                             EjTransactionRepository repo,
                             int batchSize,
                             Charset charset,
                             boolean archiveOnSuccess,
                             Path archiveDir) {
        this.parser           = parser;
        this.repo             = repo;
        this.batchSize        = batchSize;
        this.charset          = charset;
        this.archiveOnSuccess = archiveOnSuccess;
        this.archiveDir       = archiveDir;
    }

    public LoadResult loadFile(Path file) {
        LOG.info("Loading EJ file: {}", file);
        long t0 = System.nanoTime();

        long parsedCount = 0, insertedCount = 0, errorCount = 0;
        List<EjTransaction> batch = new ArrayList<>(batchSize);
        boolean fileLevelFailure = false;

        try (EjFileReader reader = new EjFileReader(file, charset);
            Stream<RawTransactionBlock> blocks = reader.stream()) {

            for (RawTransactionBlock block : (Iterable<RawTransactionBlock>) blocks::iterator) {
                EjTransaction txn;
                try {
                    txn = parser.parse(block);
                    parsedCount++;
                } catch (Exception ex) {
                    // Parser is built to never throw - this is paranoia.
                    errorCount++;
                    LOG.error("Parser error on block {} lines {}-{}: {}",
                            block.getFileName(), block.getLineStart(), block.getLineEnd(),
                            ex.getMessage());
                    continue;
                }

                batch.add(txn);
                if (batch.size() >= batchSize) {
                    insertedCount += flush(batch);
                }
            }
            insertedCount += flush(batch);

        } catch (IOException ioe) {
            LOG.error("I/O error reading {}: {}", file, ioe.toString());
            fileLevelFailure = true;
        } catch (UncheckedIOException uioe) {
            LOG.error("I/O error reading {}: {}", file, uioe.toString());
            fileLevelFailure = true;
        } catch (BatchInsertFailedException bie) {
            LOG.error("Batch insert failed for {}: {}", file, bie.getMessage());
            fileLevelFailure = true;
        } catch (RuntimeException unexpected) {
            LOG.error("Unexpected error processing {}: {}", file, unexpected.toString(), unexpected);
            fileLevelFailure = true;
        }

        long elapsedNanos = System.nanoTime() - t0;
        long elapsedMs    = elapsedNanos / 1_000_000L;

        if (fileLevelFailure) {
            LOG.warn("Aborted {} : parsed={}, inserted={}, errors={}, elapsed={}ms",
                    file.getFileName(), parsedCount, insertedCount, errorCount, elapsedMs);
            return new LoadResult(file, parsedCount, insertedCount, errorCount + 1,
                    elapsedNanos, false);
        }

        if (archiveOnSuccess && archiveDir != null) {
            archive(file);
        }

        LOG.info("Done {} : parsed={}, inserted={}, errors={}, elapsed={}ms",
                file.getFileName(), parsedCount, insertedCount, errorCount, elapsedMs);
        return new LoadResult(file, parsedCount, insertedCount, errorCount, elapsedNanos, true);
    }

    private int flush(List<EjTransaction> batch) {
        if (batch.isEmpty()) return 0;
        try {
            int n = repo.insertBatch(batch);
            batch.clear();
            return n;
        } catch (SQLException e) {
            // Wrap into a runtime exception so the iterator-based for-loop above
            // unwinds. Caught explicitly upstream so we can log a sensible
            // file-level message and continue with the next file.
            throw new BatchInsertFailedException(
                    "Batch insert failed (size=" + batch.size() + "): " + e.getMessage(), e);
        }
    }

    private void archive(Path file) {
        try {
            Files.createDirectories(archiveDir);
            Path target = archiveDir.resolve(file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Archived {} -> {}", file, target);
        } catch (IOException e) {
            LOG.warn("Could not archive {}: {}", file, e.toString());
        }
    }

    // ============================ result ===================================
    public static final class LoadResult {
        public final Path    file;
        public final long    parsed;
        public final long    inserted;
        public final long    errors;
        public final long    elapsedNanos;
        public final boolean success;

        LoadResult(Path file, long parsed, long inserted, long errors,
                long elapsedNanos, boolean success) {
            this.file = file;
            this.parsed = parsed;
            this.inserted = inserted;
            this.errors = errors;
            this.elapsedNanos = elapsedNanos;
            this.success = success;
        }

        @Override
        public String toString() {
            return "LoadResult{file=" + file
                    + ", parsed=" + parsed
                    + ", inserted=" + inserted
                    + ", errors=" + errors
                    + ", elapsedMs=" + (elapsedNanos / 1_000_000L)
                    + ", success=" + success + "}";
        }
    }

    /** Internal signal that a batch insert failed; caught upstream. */
    private static final class BatchInsertFailedException extends RuntimeException {
        BatchInsertFailedException(String msg, Throwable cause) { super(msg, cause); }
    }
}
