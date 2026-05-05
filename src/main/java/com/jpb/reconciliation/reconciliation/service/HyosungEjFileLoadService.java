package com.jpb.reconciliation.reconciliation.service;

import java.nio.file.Path;

public interface HyosungEjFileLoadService {

    /**
     * Single EJ file ko parse karke DB mein insert karta hai.
     *
     * @param file  .txt EJ file ka path
     * @return      LoadResult — parsed/inserted/errors counts + success flag
     */
    EjLoadResult loadFile(Path file);

    /**
     * Ek directory ke saare matching files load karta hai.
     *
     * @param inputDir  folder jahan EJ .txt files hain
     * @param glob      file pattern, e.g. "*.txt"
     * @return          total summary result
     */
    EjLoadResult loadDirectory(Path inputDir, String glob);

    // -------------------------------------------------------------------------
    // Result DTO — interface ke andar rakha taaki dono (service + controller)
    // same class use karein bina extra import ke
    // -------------------------------------------------------------------------
    final class EjLoadResult {

        public final long    parsed;
        public final long    inserted;
        public final long    errors;
        public final long    elapsedMs;
        public final boolean success;
        public final int     totalFiles;
        public final int     failedFiles;

        public EjLoadResult(long parsed, long inserted, long errors,
                            long elapsedMs, boolean success,
                            int totalFiles, int failedFiles) {
            this.parsed      = parsed;
            this.inserted    = inserted;
            this.errors      = errors;
            this.elapsedMs   = elapsedMs;
            this.success     = success;
            this.totalFiles  = totalFiles;
            this.failedFiles = failedFiles;
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