package com.jpb.reconciliation.reconciliation.hyosung.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Raw, unparsed transaction block carrying the source-file lines as captured
 * by {@link com.bank.atmej.reader.EjFileReader}.
 *
 * <p>Lines retain their original whitespace (the Hyosung NDC EJ format uses
 * indentation as a structural cue inside JOURNAL DATA and [TRANSACTION
 * RECORD] sub-blocks, so we preserve everything verbatim). Stripping happens
 * inside the parser, never here.
 *
 * <p>The reader sets {@link #isMalformed()} when the block was emitted
 * because a fresh {@code TRANSACTION START} appeared before a closing
 * {@code TRANSACTION END}. The parser uses this to distinguish a
 * <em>malformed</em> block (mid-stream framing error) from a
 * <em>truncated</em> block (file ends mid-transaction).
 */
public final class RawTransactionBlock {

    private final String fileName;
    private final long   lineStart;
    private final long   lineEnd;
    private final List<String> lines;
    private final boolean malformed;

    public RawTransactionBlock(String fileName, long lineStart, long lineEnd, List<String> lines) {
        this(fileName, lineStart, lineEnd, lines, false);
    }

    public RawTransactionBlock(String fileName, long lineStart, long lineEnd,
                                List<String> lines, boolean malformed) {
        this.fileName  = Objects.requireNonNull(fileName);
        this.lineStart = lineStart;
        this.lineEnd   = lineEnd;
        // Defensive copy + unmodifiable wrapper. Avoids List.copyOf (Java 10+).
        this.lines     = Collections.unmodifiableList(new ArrayList<String>(Objects.requireNonNull(lines)));
        this.malformed = malformed;
    }

    public String getFileName()    { return fileName; }
    public long   getLineStart()   { return lineStart; }
    public long   getLineEnd()     { return lineEnd; }
    public List<String> getLines() { return lines; }
    public boolean isMalformed()   { return malformed; }

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
