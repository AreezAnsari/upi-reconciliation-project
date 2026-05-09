package com.jpb.reconciliation.reconciliation.service;

import java.nio.file.Path;
import java.util.Map;

public interface HyosungEjFileLoadService {

    EjLoadResult loadFile(Path file);

    EjLoadResult loadDirectory(Path inputDir, String glob);

    /**
     * Validates request, collects files, runs loadDirectory,
     * and returns a response map ready for the controller.
     */
    EjLoadResult loadEjFiles(Map<String, Object> request);

    final class EjLoadResult {

        public final long    parsed;
        public final long    inserted;
        public final long    errors;
        public final long    elapsedMs;
        public final boolean success;
        public final int     totalFiles;
        public final int     failedFiles;
        public final String  batchId;
        public final String  errorMessage;

        public EjLoadResult(long parsed, long inserted, long errors,
                            long elapsedMs, boolean success,
                            int totalFiles, int failedFiles,
                            String batchId, String errorMessage) {
            this.parsed       = parsed;
            this.inserted     = inserted;
            this.errors       = errors;
            this.elapsedMs    = elapsedMs;
            this.success      = success;
            this.totalFiles   = totalFiles;
            this.failedFiles  = failedFiles;
            this.batchId      = batchId;
            this.errorMessage = errorMessage;
        }

        public String overallStatus() {
            return failedFiles == 0 ? "SUCCESS"
                    : (failedFiles == totalFiles ? "FAILED" : "PARTIAL");
        }

        @Override
        public String toString() {
            return "EjLoadResult{parsed=" + parsed
                    + ", inserted=" + inserted
                    + ", errors=" + errors
                    + ", elapsedMs=" + elapsedMs
                    + ", success=" + success
                    + ", totalFiles=" + totalFiles
                    + ", failedFiles=" + failedFiles + "}";
        }
    }
}