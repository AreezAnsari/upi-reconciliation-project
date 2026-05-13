package com.jpb.reconciliation.reconciliation.reader;

import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.util.EjTextUtils;
import com.jpb.reconciliation.reconciliation.dto.EjRawTransactionBlock;
import com.jpb.reconciliation.reconciliation.parser.EjPatterns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Streams {@link EjRawTransactionBlock} instances from an EJ log file.
 *
 * <p>Memory footprint is O(block size) - never the whole file - so this scales
 * to multi-gigabyte logs. Blocks are produced lazily; the returned {@link Stream}
 * must be closed (use try-with-resources).
 *
 * <h3>Block boundaries</h3>
 * <ul>
 *   <li><b>Start</b>: {@code *TRANSACTION START*}. The block prepends the most
 *       recent sequence-header line (pattern
 *       {@code [020t*N*MM/DD/YYYY*HH:MI:SS*}) when one was seen within the
 *       previous few lines, so the parser can extract sequence_number / log_date
 *       / log_time without re-scanning.</li>
 *   <li><b>End</b>: {@code TRANSACTION END}, plus any trailing
 *       {@code Error Severity} / {@code Diagnostic Status} lines that appear
 *       before the next sequence header or {@code *PRIMARY CARD READER ACTIVATED*}.
 *       Note: any line between {@code TRANSACTION END} and the next event
 *       marker is attached to the preceding block - this is by design so we
 *       don't lose late-arriving diagnostics, but it means orphan text
 *       between blocks (rare in real files) ends up in the prior block's
 *       audit copy.</li>
 *   <li><b>Truncated</b>: if EOF is reached before {@code TRANSACTION END} the
 *       partial block is still emitted; the parser will mark it INCOMPLETE.</li>
 * </ul>
 *
 * <p>This class is <b>not thread-safe</b>: a single instance manages a single
 * file handle and a one-line pushback slot. Use one instance per file, ideally
 * via try-with-resources.
 */
public final class EjFileReader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EjFileReader.class);

    /** Sliding window of recently-read lines, used to find the sequence header. */
    private static final int LOOKBACK_LINES = 4;

    private final String         fileName;
    private final BufferedReader reader;

    /** Pushback for one line - the trailing-diagnostic loop sometimes needs to "un-read". */
    private NumberedLine pushed;
    private long currentLineNo = 0;

    public EjFileReader(Path file, Charset charset) throws IOException {
        this.fileName = file.getFileName().toString();
        this.reader   = Files.newBufferedReader(file, charset);
    }

    public String getFileName() { return fileName; }

    /** Returns a stream of transaction blocks. Must be closed (try-with-resources). */
    public Stream<EjRawTransactionBlock> stream() {
        Iterator<EjRawTransactionBlock> it = new BlockIterator();
        Spliterator<EjRawTransactionBlock> sp = Spliterators.spliteratorUnknownSize(
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

    /** Reads one logical line, honouring the pushback slot. */
    private NumberedLine nextLine() throws IOException {
        if (pushed != null) {
            NumberedLine p = pushed;
            pushed = null;
            return p;
        }
        String s = reader.readLine();
        if (s == null) return null;
        currentLineNo++;
        return new NumberedLine(currentLineNo, s);
    }

    private void pushBack(NumberedLine nl) {
        if (pushed != null) {
            throw new IllegalStateException("Pushback slot already occupied (line " + nl.lineNo + ")");
        }
        pushed = nl;
    }

    // ============== iterator over transaction blocks ========================
    private final class BlockIterator implements Iterator<EjRawTransactionBlock> {
        private final Deque<NumberedLine> lookback = new ArrayDeque<>(LOOKBACK_LINES);
        private EjRawTransactionBlock next = null;
        private boolean exhausted = false;

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (exhausted) return false;
            try {
                next = readNextBlock();
            } catch (IOException e) {
                LOG.error("I/O error reading file: {}", e.getMessage());
                throw new UncheckedIOException("I/O error reading " + fileName, e);
            }
            if (next == null) exhausted = true;
            return next != null;
        }

        @Override
        public EjRawTransactionBlock next() {
            if (!hasNext()) throw new NoSuchElementException();
            EjRawTransactionBlock b = next;
            next = null;
            return b;
        }

        private EjRawTransactionBlock readNextBlock() throws IOException {
            // 1. seek forward until *TRANSACTION START*
            NumberedLine nl;
            while ((nl = nextLine()) != null) {
                pushLookback(nl);
                if (EjPatterns.TXN_START.matcher(EjTextUtils.stripLeading(nl.text)).find()) {
                    return readBlockFromStart(nl);
                }
            }
            return null; // EOF
        }

        private EjRawTransactionBlock readBlockFromStart(NumberedLine startLine) throws IOException {
            List<String> blockLines = new ArrayList<>(64);
            long lineStart = startLine.lineNo;

            // Prepend most-recent sequence header from lookback if any.
            NumberedLine seqHeaderLine = null;
            for (NumberedLine n : lookback) {
                if (n == startLine) continue;
                if (EjPatterns.SEQ_HEADER.matcher(EjTextUtils.stripLeading(n.text)).find()) {
                    seqHeaderLine = n;
                }
            }
            if (seqHeaderLine != null) {
                blockLines.add(seqHeaderLine.text);
                lineStart = seqHeaderLine.lineNo;
            }
            blockLines.add(startLine.text);

            // 2. body up to TRANSACTION END
            long lineEnd = startLine.lineNo;
            boolean foundEnd = false;
            NumberedLine cur;
            while ((cur = nextLine()) != null) {
                String s = EjTextUtils.stripLeading(cur.text);

                // Defensive: if we encounter a fresh TRANSACTION START before END,
                // the prior block is malformed - emit what we have and push the
                // new start back for the next iteration to pick up.
                if (EjPatterns.TXN_START.matcher(s).find()) {
                    LOG.warn("Malformed: TRANSACTION START at line {} of {} without preceding END; emitting partial block.",
                            cur.lineNo, fileName);
                    pushBack(cur);
                    return new EjRawTransactionBlock(fileName, lineStart, lineEnd, blockLines);
                }
                blockLines.add(cur.text);
                lineEnd = cur.lineNo;
                pushLookback(cur);
                if (EjPatterns.TXN_END.matcher(s).find()) {
                    foundEnd = true;
                    break;
                }
            }

            if (!foundEnd) {
                LOG.warn("Truncated transaction block at end of {} (lines {}-{})",
                        fileName, lineStart, lineEnd);
                return new EjRawTransactionBlock(fileName, lineStart, lineEnd, blockLines);
            }

            // 3. trailing diagnostic lines until next event (or EOF).
            while ((cur = nextLine()) != null) {
                String s = EjTextUtils.stripLeading(cur.text);
                boolean isNextEvent = EjPatterns.SEQ_HEADER.matcher(s).find()
                        || s.contains(EjPatterns.PRIMARY_CARD_READER_MARKER);
                if (isNextEvent) {
                    pushBack(cur);
                    break;
                }
                blockLines.add(cur.text);
                lineEnd = cur.lineNo;
                pushLookback(cur);
            }

            return new EjRawTransactionBlock(fileName, lineStart, lineEnd, blockLines);
        }

        private void pushLookback(NumberedLine nl) {
            while (lookback.size() >= LOOKBACK_LINES) lookback.removeFirst();
            lookback.addLast(nl);
        }
    }

    private static final class NumberedLine {
        final long lineNo;
        final String text;
        NumberedLine(long lineNo, String text) {
            this.lineNo = lineNo;
            this.text = text;
        }
    }}
