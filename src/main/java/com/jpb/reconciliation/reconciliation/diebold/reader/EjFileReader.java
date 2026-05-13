package com.jpb.reconciliation.reconciliation.diebold.reader;

import com.jpb.reconciliation.reconciliation.diebold.model.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.diebold.parser.EjPatterns;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Streaming reader for Diebold ATM Electronic Journal files.
 *
 * <p>Diebold EJ files use {@code ***...***} lines as block separators.
 * Each block starts with a 19-char header line (XNNNNNNDDMMYYHHMMSS)
 * followed by the block body lines until the next separator.
 *
 * <p>Only blocks with header prefix "1" (receipt), "4" (card event),
 * or "0" (error/failed) are emitted. Lines before the first valid
 * header are silently skipped.
 *
 * <p>The reader is {@link Closeable} — always use in try-with-resources.
 */
public final class EjFileReader implements Closeable {

    private final BufferedReader reader;
    private final String         fileName;

    public EjFileReader(Path file, Charset charset) throws IOException {
        this.reader   = Files.newBufferedReader(file, charset);
        this.fileName = file.getFileName().toString();
    }

    public EjFileReader(Path file) throws IOException {
        this(file, StandardCharsets.ISO_8859_1);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Returns a sequential {@link Stream} of parsed blocks.
     * The caller must close the stream (or use try-with-resources) to
     * release the underlying file handle.
     */
    public Stream<RawTransactionBlock> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new BlockIterator(), Spliterator.ORDERED),
                false);
    }

    // ===================================================================
    private final class BlockIterator implements Iterator<RawTransactionBlock> {

        private String                nextLine    = null;
        private boolean               eof         = false;
        private long                  lineNumber  = 0;
        private RawTransactionBlock   pending     = null;

        BlockIterator() {
            advance(); // prime the first line
        }

        @Override
        public boolean hasNext() {
            if (pending != null) return true;
            pending = readNextBlock();
            return pending != null;
        }

        @Override
        public RawTransactionBlock next() {
            if (!hasNext()) throw new NoSuchElementException();
            RawTransactionBlock b = pending;
            pending = null;
            return b;
        }

        private RawTransactionBlock readNextBlock() {
            // Skip until we find a valid block header line
            while (nextLine != null && !isBlockHeader(nextLine)) {
                advance();
            }
            if (nextLine == null) return null; // EOF

            // We are on the header line
            String headerLine   = nextLine;
            long   blockStart   = lineNumber;
            List<String> lines  = new ArrayList<String>();
            lines.add(headerLine);
            advance();

            // Collect body lines until separator or EOF
            while (nextLine != null && !EjPatterns.SEPARATOR.matcher(nextLine).matches()) {
                if (isBlockHeader(nextLine)) {
                    // Missing separator — emit current block as-is (malformed)
                    return new RawTransactionBlock(fileName, blockStart, lineNumber - 1, lines, true);
                }
                lines.add(nextLine);
                advance();
            }

            long blockEnd = lineNumber;
            // Consume the separator line
            if (nextLine != null) {
                advance();
            }

            return new RawTransactionBlock(fileName, blockStart, blockEnd, lines, false);
        }

        private boolean isBlockHeader(String line) {
            return line != null && EjPatterns.BLOCK_HEADER.matcher(line.trim()).matches();
        }

        private void advance() {
            if (eof) { nextLine = null; return; }
            try {
                nextLine = reader.readLine();
                if (nextLine == null) {
                    eof = true;
                } else {
                    lineNumber++;
                }
            } catch (IOException e) {
                eof = true;
                nextLine = null;
                throw new UncheckedIOException(e);
            }
        }
    }
}