package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.atmej.db.EjTransactionRepository;
import com.jpb.reconciliation.reconciliation.atmej.parser.EjTransactionParser;
import com.jpb.reconciliation.reconciliation.atmej.runner.EjFileLoadService;
import com.jpb.reconciliation.reconciliation.atmej.runner.EjFileLoadService.LoadResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

/**
 * REST Controller to trigger ATM EJ file loading into DB.
 *
 * Endpoint : POST /api/v1/atmej/load
 *
 * Request Body:
 * {
 *   "inputDir"  : "C:/path/to/NCR",          // required
 *   "glob"      : "*.txt",                    // optional, default *.txt
 *   "batchSize" : 500,                        // optional, default 500
 *   "charset"   : "ISO-8859-1"               // optional, default ISO-8859-1
 * }
 */
@RestController
@RequestMapping("/api/v1/atmej")
public class AtmEjLoaderController {

    private static final Logger LOG = LoggerFactory.getLogger(AtmEjLoaderController.class);

    @Autowired
    private DataSource dataSource;

    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadEjFiles(@RequestBody Map<String, Object> request) {

        // --- 1. Input validate karo ---
        String inputDir = (String) request.get("inputDir");
        if (inputDir == null || inputDir.trim().isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "InputDir is Required");
            return ResponseEntity.badRequest().body(err);
        }

        Path dirPath = Paths.get(inputDir.trim());
        if (!Files.isDirectory(dirPath)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "inputDir does not exist or is not a directory: " + inputDir);
            return ResponseEntity.badRequest().body(err);
        }

        // --- 2. Optional params ---
        String glob = getOrDefault(request, "glob", "*.txt");
        int batchSize;
        try {
            batchSize = Integer.parseInt(getOrDefault(request, "batchSize", "500"));
        } catch (NumberFormatException e) {
            batchSize = 500;
        }
        String charsetStr = getOrDefault(request, "charset", "ISO-8859-1");
        Charset charset;
        try {
            charset = Charset.forName(charsetStr);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Invalid charset: " + charsetStr + " " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }

        // --- 3. Files collect karo ---
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) files.add(p);
            }
        } catch (IOException e) {
            LOG.error("Failed to list files in directory: {}", inputDir, e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Could not read directory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        if (files.isEmpty()) {
            Map<String, Object> noFiles = new LinkedHashMap<>();
            noFiles.put("status", "NO_FILES");
            noFiles.put("message", "No files found in: " + inputDir);
            noFiles.put("filesFound", 0);
            return ResponseEntity.ok(noFiles);
        }

        files.sort(new Comparator<Path>() {
            @Override
            public int compare(Path a, Path b) {
                return a.getFileName().toString().compareTo(b.getFileName().toString());
            }
        });
        LOG.info("ATM EJ Load triggered. Files found: {}, dir: {}", files.size(), inputDir);

        // --- 4. Service setup karo ---
        String batchId = "batch-" + UUID.randomUUID();
        EjTransactionRepository repo   = new EjTransactionRepository(dataSource);
        EjTransactionParser     parser = new EjTransactionParser(batchId, "");
        EjFileLoadService       svc    = new EjFileLoadService(
                                            parser, repo, batchSize,
                                            charset, false, null);

        // --- 5. Har file process karo ---
        long totalParsed = 0, totalInserted = 0, totalErrors = 0;
        List<Map<String, Object>> fileResults = new ArrayList<>();
        int failedFiles = 0;

        for (Path file : files) {
            try {
                LoadResult result = svc.loadFile(file);
                totalParsed    += result.parsed;
                totalInserted  += result.inserted;
                totalErrors    += result.errors;
                if (!result.success) failedFiles++;

                Map<String, Object> fileEntry = new LinkedHashMap<>();
                fileEntry.put("file",      file.getFileName().toString());
                fileEntry.put("parsed",    result.parsed);
                fileEntry.put("inserted",  result.inserted);
                fileEntry.put("errors",    result.errors);
                fileEntry.put("success",   result.success);
                fileEntry.put("elapsedMs", result.elapsedNanos / 1_000_000L);
                fileResults.add(fileEntry);

            } catch (Exception e) {
                LOG.error("Unhandled error processing file: {}", file, e);
                failedFiles++;

                Map<String, Object> fileEntry = new LinkedHashMap<>();
                fileEntry.put("file",    file.getFileName().toString());
                fileEntry.put("success", false);
                fileEntry.put("error",   e.getMessage() != null ? e.getMessage() : "Unknown error");
                fileResults.add(fileEntry);
            }
        }

        // --- 6. Response banao ---
        String overallStatus = failedFiles == 0
                ? "SUCCESS"
                : (failedFiles == files.size() ? "FAILED" : "PARTIAL");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",        overallStatus);
        response.put("batchId",       batchId);
        response.put("totalFiles",    files.size());
        response.put("failedFiles",   failedFiles);
        response.put("totalParsed",   totalParsed);
        response.put("totalInserted", totalInserted);
        response.put("totalErrors",   totalErrors);
        response.put("files",         fileResults);

        LOG.info("ATM EJ Load complete. batchId={}, status={}, inserted={}", batchId, overallStatus, totalInserted);

        HttpStatus httpStatus = failedFiles == 0 ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Java 8 safe helper: request map se value lo, null/missing hone par defaultValue return karo.
     */
    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        String str = val.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }
}