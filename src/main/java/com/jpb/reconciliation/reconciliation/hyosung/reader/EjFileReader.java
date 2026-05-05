package com.jpb.reconciliation.reconciliation.hyosung.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.hyosung.model.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.hyosung.parser.EjPatterns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Streams {@link RawTransactionBlock} instances from a Hyosung NDC EJ file.
 *
 * <p>Memory footprint is O(block size) - never the whole file - so this scales
 * to multi-gigabyte logs. Blocks are produced lazily; the returned {@link Stream}
 * must be closed (use try-with-resources).
 *
 * <h3>Block boundaries</h3>
 * <ul>
 *   <li><b>Start</b>: a framed line whose body matches {@code TRANSACTION START}
 *       or {@code CARDLESS TRANSACTION START}.</li>
 *   <li><b>End</b>: the next framed line whose body is {@code TRANSACTION END}.</li>
 *   <li><b>Truncated</b>: if EOF is reached before {@code TRANSACTION END} the
 *       partial block is still emitted; the parser will mark it
 *       {@code TRUNCATED}.</li>
 *   <li><b>Malformed</b>: if a fresh START is seen before END, the open block
 *       is closed (logged as malformed) and the new START begins a new block.</li>
 * </ul>
 *
 * <p>This reader is <b>not thread-safe</b>: a single instance manages a single
 * file handle.
 */
public final class EjFileReader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EjFileReader.class);

    private final String         fileName;
    private final BufferedReader reader;
    private long currentLineNo = 0;

    public EjFileReader(Path file, Charset charset) throws IOException {
        this.fileName = file.getFileName().toString();
        this.reader   = Files.newBufferedReader(file, charset);
    }

    public String getFileName() { return fileName; }

    /** Returns a stream of transaction blocks. Must be closed (try-with-resources). */
    public Stream<RawTransactionBlock> stream() {
        Iterator<RawTransactionBlock> it = new BlockIterator();
        Spliterator<RawTransactionBlock> sp = Spliterators.spliteratorUnknownSize(
                it, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(sp, false).onClose(this::closeQuietly);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private void closeQuietly() {
        try { close(); } catch (IOException ignored) {}
    }

    /** Reads one logical line and tracks the line number. */
    private String readLine() throws IOException {
        String s = reader.readLine();
        if (s == null) return null;
        currentLineNo++;
        return s;
    }

    // =========================================================================
    // Iterator
    // =========================================================================
    private final class BlockIterator implements Iterator<RawTransactionBlock> {

        // When we encounter a fresh START while inside a block, we capture the
        // new START line here so the next iteration emits it without re-reading.
        private String pendingStart;
        private long   pendingStartLineNo;

        private RawTransactionBlock next = null;
        private boolean exhausted = false;

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (exhausted) return false;
            try {
                next = readNextBlock();
            } catch (IOException e) {
                throw new UncheckedIOException("I/O error reading " + fileName, e);
            }
            if (next == null) exhausted = true;
            return next != null;
        }

        @Override
        public RawTransactionBlock next() {
            if (!hasNext()) throw new NoSuchElementException();
            RawTransactionBlock b = next;
            next = null;
            return b;
        }

        private RawTransactionBlock readNextBlock() throws IOException {
            List<String> lines = new ArrayList<String>(64);
            long startLine;
            String firstLine;

            // 1. Decide the starting line - either a buffered pendingStart
            //    or the next STX/CARDLESS-STX we can find in the file.
            if (pendingStart != null) {
                firstLine          = pendingStart;
                startLine          = pendingStartLineNo;
                pendingStart       = null;
                pendingStartLineNo = 0;
            } else {
                String found = seekNextStart();
                if (found == null) return null;       // EOF without finding any START
                firstLine = found;
                startLine = currentLineNo;
            }
            lines.add(firstLine);

            // 2. Read until TRANSACTION END, premature START, or EOF.
            long lineEnd = startLine;
            String s;
            while ((s = readLine()) != null) {
                String body = bodyOf(s);

                if (body != null
                        && (EjPatterns.TXN_START.matcher(body).matches()
                            || EjPatterns.CARDLESS_TXN_START.matcher(body).matches())) {
                    LOG.warn("Malformed: {} at line {} of {} without preceding TRANSACTION END; "
                            + "emitting partial block.", body, currentLineNo, fileName);
                    pendingStart       = s;
                    pendingStartLineNo = currentLineNo;
                    return new RawTransactionBlock(fileName, startLine, lineEnd, lines, true);
                }

                lines.add(s);
                lineEnd = currentLineNo;

                if (body != null && EjPatterns.TXN_END.matcher(body).matches()) {
                    return new RawTransactionBlock(fileName, startLine, lineEnd, lines);
                }
            }

            // EOF before END
            LOG.warn("Truncated transaction block at end of {} (lines {}-{})",
                    fileName, startLine, lineEnd);
            return new RawTransactionBlock(fileName, startLine, lineEnd, lines);
        }

        /** Skip forward through non-START lines. Returns the START line, or null at EOF. */
        private String seekNextStart() throws IOException {
            String s;
            while ((s = readLine()) != null) {
                String body = bodyOf(s);
                if (body == null) continue;
                if (EjPatterns.TXN_START.matcher(body).matches()
                        || EjPatterns.CARDLESS_TXN_START.matcher(body).matches()) {
                    return s;
                }
            }
            return null;
        }

        /** Returns the trimmed body of a framed line, or null if the line is not framed. */
        private String bodyOf(String line) {
            if (line == null) return null;
            Matcher m = EjPatterns.LINE_FRAME.matcher(line);
            return m.matches() ? m.group(4).trim() : null;
        }
    }
}
