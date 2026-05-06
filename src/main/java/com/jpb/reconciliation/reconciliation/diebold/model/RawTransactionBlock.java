package com.jpb.reconciliation.reconciliation.diebold.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable holder for one Diebold EJ transaction block.
 * Each block is delimited by *********************************** separators.
 */
public final class RawTransactionBlock {

    private final String       fileName;
    private final long         lineStart;
    private final long         lineEnd;
    private final List<String> lines;
    private final boolean      malformed;

    public RawTransactionBlock(String fileName, long lineStart, long lineEnd,
                               List<String> lines, boolean malformed) {
        this.fileName  = fileName;
        this.lineStart = lineStart;
        this.lineEnd   = lineEnd;
        this.lines     = Collections.unmodifiableList(lines);
        this.malformed = malformed;
    }

    public RawTransactionBlock(String fileName, long lineStart, long lineEnd,
                               List<String> lines) {
        this(fileName, lineStart, lineEnd, lines, false);
    }

    public String       getFileName()  { return fileName;  }
    public long         getLineStart() { return lineStart; }
    public long         getLineEnd()   { return lineEnd;   }
    public List<String> getLines()     { return lines;     }
    public boolean      isMalformed()  { return malformed; }

    public String asText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }
}