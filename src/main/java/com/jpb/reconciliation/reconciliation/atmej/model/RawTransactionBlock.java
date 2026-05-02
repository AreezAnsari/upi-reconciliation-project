package com.jpb.reconciliation.reconciliation.atmej.model;

import java.util.ArrayList; 
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Raw, unparsed transaction block carrying the source-file lines as captured
 * by {@link com.bank.atmej.reader.EjFileReader}.
 *
 * <p>Lines retain their original whitespace and any control characters (the
 * NCR APTRA EJ format includes ESC bytes and tab indentation). Stripping
 * happens inside the parser, never here.
 */
public final class RawTransactionBlock {

    private final String fileName;
    private final long   lineStart;
    private final long   lineEnd;
    private final List<String> lines;

    public RawTransactionBlock(String fileName, long lineStart, long lineEnd, List<String> lines) {
        this.fileName  = Objects.requireNonNull(fileName);
        this.lineStart = lineStart;
        this.lineEnd   = lineEnd;
        this.lines = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(lines)));
    }

    public String getFileName()      { return fileName; }
    public long   getLineStart()     { return lineStart; }
    public long   getLineEnd()       { return lineEnd; }
    public List<String> getLines()   { return Collections.unmodifiableList(lines); }

    /** Reconstructs the block as a single {@code \n}-separated string. */
    public String asText() {
        return String.join("\n", lines);
    }

    public int size() {
        return lines.size();
    }

    @Override
    public String toString() {
        return "RawTransactionBlock{file=" + fileName
                + ", lines=" + lineStart + "-" + lineEnd
                + ", size=" + lines.size() + "}";
    }
}

