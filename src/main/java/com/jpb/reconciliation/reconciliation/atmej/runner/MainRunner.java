package com.jpb.reconciliation.reconciliation.atmej.runner;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.atmej.config.AppConfig;
import com.jpb.reconciliation.reconciliation.atmej.db.DataSourceFactory;
import com.jpb.reconciliation.reconciliation.atmej.db.EjTransactionRepository;
import com.jpb.reconciliation.reconciliation.atmej.parser.EjTransactionParser;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.nio.file.Paths;

/**
 * Command-line entry point.
 *
 * <p>Usage:
 * <pre>
 *   java -jar atm-ej-loader.jar [file-or-dir]...
 * </pre>
 *
 * <p>If no argument is supplied, the directory configured in
 * {@code input.dir} is scanned for files matching {@code input.glob}.
 * Each {@code .txt} file is processed independently; failure of one file does
 * not stop the others.
 *
 * <p>Exit codes:
 * <ul>
 *   <li>{@code 0} - all files loaded successfully (or no files found)</li>
 *   <li>{@code 1} - one or more files failed</li>
 *   <li>{@code 2} - configuration / startup error</li>
 * </ul>
 */
public final class MainRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MainRunner.class);

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        AppConfig cfg;
        try {
            cfg = AppConfig.load();
        } catch (RuntimeException e) {
            System.err.println("Configuration error: " + e.getMessage());
            return 2;
        }

        String batchId = "batch-" + UUID.randomUUID();
        LOG.info("Starting atm-ej-loader, batch_id={}", batchId);

        List<Path> files;
        try {
            files = collectFiles(cfg, args);
        } catch (IOException e) {
            LOG.error("Failed to enumerate input files: {}", e.toString());
            return 2;
        }

        if (files.isEmpty()) {
            LOG.warn("No input files found.");
            return 0;
        }
        LOG.info("Found {} files to process.", files.size());

        DataSource ds = DataSourceFactory.create(cfg);
        try {
            EjTransactionRepository repo   = new EjTransactionRepository(ds);
            EjTransactionParser     parser = new EjTransactionParser(
                    batchId,
                    cfg.getOrDefault("parser.atmIdPrefix", ""));

            String archiveDirStr = cfg.get("input.archiveDir");
            Path   archiveDir    = (archiveDirStr != null && !archiveDirStr.trim().isEmpty())
                                       ? Paths.get(archiveDirStr) : null;

            EjFileLoadService svc = new EjFileLoadService(
                    parser,
                    repo,
                    cfg.getInt("db.batchSize", 500),
                    cfg.inputCharset(),
                    cfg.getBoolean("input.archiveOnSuccess", false),
                    archiveDir);

            long totalParsed = 0, totalInserted = 0, totalErrors = 0;
            int  failedFiles = 0;

            for (Path f : files) {
                try {
                    EjFileLoadService.LoadResult r = svc.loadFile(f);
                    totalParsed   += r.parsed;
                    totalInserted += r.inserted;
                    totalErrors   += r.errors;
                    if (!r.success) failedFiles++;
                } catch (RuntimeException e) {
                    LOG.error("Unhandled error processing {}: {}", f, e.toString(), e);
                    failedFiles++;
                }
            }

            LOG.info("=========================================================");
            LOG.info("RUN COMPLETE  batch_id={}", batchId);
            LOG.info("  files       : {} ({} failed)", files.size(), failedFiles);
            LOG.info("  parsed      : {}", totalParsed);
            LOG.info("  inserted    : {}", totalInserted);
            LOG.info("  errors      : {}", totalErrors);
            LOG.info("=========================================================");

            return failedFiles == 0 ? 0 : 1;

        } finally {
        	if (ds instanceof HikariDataSource) {
        	    ((HikariDataSource) ds).close();
        	}
        }
    }

    private static List<Path> collectFiles(AppConfig cfg, String[] args) throws IOException {
        List<Path> result = new ArrayList<>();

        if (args != null && args.length > 0) {
            for (String a : args) {
                Path p = Paths.get(a);
                if (Files.isDirectory(p)) {
                    addMatching(p, cfg.getOrDefault("input.glob", "*.txt"), result);
                } else if (Files.isRegularFile(p)) {
                    result.add(p);
                } else {
                    LOG.warn("Skipping argument (not a file or directory): {}", a);
                }
            }
        } else {
            String dir = cfg.get("input.dir");
            if (dir == null || dir.trim().isEmpty()) {
                LOG.error("No input given and 'input.dir' is not configured.");
                return Collections.emptyList();
            }
            addMatching(Paths.get(dir), cfg.getOrDefault("input.glob", "*.txt"), result);
        }

        result.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return result;
    }

    private static void addMatching(Path dir, String glob, List<Path> out) throws IOException {
        if (!Files.isDirectory(dir)) {
            LOG.warn("Configured input.dir is not a directory: {}", dir);
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) out.add(p);
            }
        }
    }
}
