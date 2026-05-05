package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.service.HyosungEjFileLoadService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/atmej/hyosung")
public class HyosungEjLoaderController {

    private static final Logger LOG = LoggerFactory.getLogger(HyosungEjLoaderController.class);

    @Autowired
    private HyosungEjFileLoadService ejFileLoadService;

    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadEjFiles(@RequestBody Map<String, Object> request) {

        // --- 1. Validate ---
        String inputDir = (String) request.get("inputDir");
        if (inputDir == null || inputDir.trim().isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "inputDir is required");
            return ResponseEntity.badRequest().body(err);
        }

        Path dirPath = Paths.get(inputDir.trim());
        if (!Files.isDirectory(dirPath)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "inputDir does not exist: " + inputDir);
            return ResponseEntity.badRequest().body(err);
        }

        // --- 2. Optional params ---
        String glob = getOrDefault(request, "glob", "*.txt");

        // --- 3. Files collect karo sirf count ke liye ---
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) files.add(p);
            }
        } catch (IOException e) {
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

        LOG.info("ATM EJ Load triggered. Files found: {}, dir: {}", files.size(), inputDir);

        // --- 4. Service call karo ---
        String batchId = "batch-" + UUID.randomUUID();
        HyosungEjFileLoadService.EjLoadResult result = ejFileLoadService.loadDirectory(dirPath, glob);

        // --- 5. Response banao ---
        String overallStatus = result.failedFiles == 0
                ? "SUCCESS"
                : (result.failedFiles == result.totalFiles ? "FAILED" : "PARTIAL");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status",        overallStatus);
        response.put("batchId",       batchId);
        response.put("totalFiles",    result.totalFiles);
        response.put("failedFiles",   result.failedFiles);
        response.put("totalParsed",   result.parsed);
        response.put("totalInserted", result.inserted);
        response.put("totalErrors",   result.errors);
        response.put("elapsedMs",     result.elapsedMs);

        LOG.info("ATM EJ Load complete. batchId={}, status={}, inserted={}",
                batchId, overallStatus, result.inserted);

        HttpStatus httpStatus = result.failedFiles == 0 ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(httpStatus).body(response);
    }

    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        String str = val.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }
}