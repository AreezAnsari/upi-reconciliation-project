package com.jpb.reconciliation.reconciliation.hyosung.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.hyosung.model.EjTransaction;
import com.jpb.reconciliation.reconciliation.hyosung.model.EjTransaction.Type;
import com.jpb.reconciliation.reconciliation.hyosung.model.EjTransaction.Status;
import com.jpb.reconciliation.reconciliation.hyosung.model.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.util.EjTextUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Parses one {@link RawTransactionBlock} into one {@link EjTransaction}.
 *
 * <p>Holds only immutable configuration on the instance; all mutable parsing
 * state lives in a per-call {@link ParseState} object. A single parser
 * instance can therefore be reused for many blocks <em>sequentially</em>.
 *
 * <p>Errors during parsing are caught at the field level and logged; the
 * parser always returns a non-null result so a single bad field never sinks
 * a whole row. The full raw block is always preserved in {@code rawTransactionBlock}
 * for audit / future re-parsing.
 *
 * <h3>Strategy</h3>
 * The block is walked once, top-to-bottom. The walker is a small state
 * machine with three sub-modes:
 * <ul>
 *   <li><b>top-level</b> - matching framed event lines like
 *       {@code [000004][...]EMV FINAL APP SELECTION SUCCESS}.</li>
 *   <li><b>journal</b> - inside a {@code JOURNAL DATA} block, consuming
 *       indented receipt lines until the next framed line.</li>
 *   <li><b>record</b> - inside a {@code [TRANSACTION RECORD]} block, consuming
 *       key/value lines until a blank line.</li>
 *   <li><b>notes</b> - inside a {@code Notes Dispensed:} block, consuming
 *       {@code TypeA(INR50) = 0} lines until something else appears.</li>
 * </ul>
 */
public final class EjTransactionParser {

    private static final Logger LOG       = LoggerFactory.getLogger(EjTransactionParser.class);
    private static final Logger PARSE_ERR = LoggerFactory.getLogger("com.bank.atmej.parser.errors");

    private static final DateTimeFormatter LINE_TS =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    private final String atmIdPrefix;
    private final String loadBatchId;

    /**
     * @param loadBatchId  identifier propagated to every row produced by this parser.
     * @param atmIdPrefix  prefix that the ATM identifier must start with for
     *                     the receipt line to be accepted (e.g. {@code "T1"}).
     *                     Empty string disables the guard.
     */
    public EjTransactionParser(String loadBatchId, String atmIdPrefix) {
        this.loadBatchId = loadBatchId;
        this.atmIdPrefix = (atmIdPrefix != null) ? atmIdPrefix : "";
    }

    public EjTransactionParser(String loadBatchId) {
        this(loadBatchId, "");
    }

    public EjTransaction parse(RawTransactionBlock block) {
        ParseState s = new ParseState();

        // Reader signals 'malformed' when it had to close a block because a
        // fresh START appeared before END. Truncation is detected here:
        // a clean block always ends with a framed TRANSACTION END line.
        boolean malformed = block.isMalformed();
        boolean truncated = !malformed && !endsWithTransactionEnd(block);

        try {
            walk(block, s);
        } catch (RuntimeException ex) {
            // The walker itself catches per-line errors; this is a paranoid
            // outer guard so a totally unexpected bug does not lose the row.
            PARSE_ERR.warn("Unhandled error walking block file={} lines={}-{}: {}",
                    block.getFileName(), block.getLineStart(), block.getLineEnd(), ex.toString());
        }

        finalizeClassification(s, truncated, malformed);

        return EjTransaction.builder()
                .fileName(block.getFileName())
                .fileLineStart(block.getLineStart())
                .fileLineEnd(block.getLineEnd())
                .sequenceNoFirst(s.sequenceNoFirst)
                .sequenceNoLast(s.sequenceNoLast)
                .firstEventTs(s.firstEventTs)
                .lastEventTs(s.lastEventTs)
                .cardNumberRaw(s.cardNumberRaw)
                .emvAid(s.emvAid)
                .emvAppLabel(s.emvAppLabel)
                .amountEntered(s.amountEntered)
                .pinEntered(s.pinEntered)
                .cardless(s.cardless)
                .opcode(s.opcode)
                .functionId(s.functionId)
                .amountRecorded(s.amountRecorded)
                .transSeqNumber(s.transSeqNumber)
                .ltSerno(s.ltSerno)
                .fidResponse(s.fidResponse)
                .fidNext(s.fidNext)
                .ltDispNotes1(s.ltDispNotes[0])
                .ltDispNotes2(s.ltDispNotes[1])
                .ltDispNotes3(s.ltDispNotes[2])
                .ltDispNotes4(s.ltDispNotes[3])
                .ltDispNotes5(s.ltDispNotes[4])
                .ltDispNotes6(s.ltDispNotes[5])
                .ltDispNotes7(s.ltDispNotes[6])
                .denomination(s.denomination)
                .requestCount(s.requestCount)
                .pickupCount(s.pickupCount)
                .dispenseCount(s.dispenseCount)
                .remainCount(s.remainCount)
                .rejectCount(s.rejectCount)
                .notesTypeA(s.notesType[0])
                .notesTypeB(s.notesType[1])
                .notesTypeC(s.notesType[2])
                .notesTypeD(s.notesType[3])
                .notesTypeE(s.notesType[4])
                .notesTypeF(s.notesType[5])
                .notesTypeG(s.notesType[6])
                .dispenseComplete(s.dispenseComplete)
                .cashPresented(s.cashPresented)
                .cashTaken(s.cashTaken)
                .cashTakenAfterTimeout(s.cashTakenAfterTimeout)
                .cashNotTaken(s.cashNotTaken)
                .cardEjected(s.cardEjected)
                .cardTaken(s.cardTaken)
                .transactionTimeout(s.transactionTimeout)
                .hostTimeout(s.hostTimeout)
                .receiptDate(s.receiptDate)
                .receiptTime(s.receiptTime)
                .atmId(s.atmId)
                .receiptCardNumber(s.receiptCardNumber)
                .receiptAccountNumber(s.receiptAccountNumber)
                .txnNo(EjTextUtils.nullIfBlank(s.txnNo))
                .referenceNo(EjTextUtils.nullIfBlank(s.referenceNo))
                .responseCode(EjTextUtils.nullIfBlank(s.responseCode))
                .accountType(s.accountType)
                .withdrawalAmount(s.withdrawalAmount)
                .transactionAmount(s.transactionAmount)
                .fromAccount(s.fromAccount)
                .modBalance(s.modBalance)
                .availBalance(s.availBalance)
                .unableToProcess(s.unableToProcess)
                .declinedInsufficient(s.declinedInsufficient)
                .adminSubtotal(s.adminSubtotal)
                .transactionType(s.txnType)
                .transactionStatus(s.txnStatus)
                .rawTransactionBlock(block.asText())
                .loadBatchId(loadBatchId)
                .build();
    }

    private static boolean endsWithTransactionEnd(RawTransactionBlock block) {
        List<String> lines = block.getLines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String s = lines.get(i);
            if (s == null) continue;
            Matcher m = EjPatterns.LINE_FRAME.matcher(s);
            if (m.matches()) {
                return EjPatterns.TXN_END.matcher(m.group(4).trim()).matches();
            }
        }
        return false;
    }

    // ============================ walker ===================================
    private void walk(RawTransactionBlock block, ParseState s) {
        Mode mode = Mode.TOP;
        List<String> journalBuffer = new ArrayList<String>();

        for (String raw : block.getLines()) {
            String line = (raw == null) ? "" : raw;

            try {
                Matcher fm = EjPatterns.LINE_FRAME.matcher(line);
                if (fm.matches()) {
                    // A framed line ends any open sub-block.
                    if (mode == Mode.JOURNAL) {
                        parseJournal(journalBuffer, s);
                        journalBuffer.clear();
                    }
                    mode = Mode.TOP;

                    String seq  = fm.group(1);
                    String date = fm.group(2);
                    String time = fm.group(3);
                    String body = fm.group(4).trim();

                    if (s.sequenceNoFirst == null) {
                        s.sequenceNoFirst = seq;
                        s.firstEventTs    = parseLineTs(date, time, block);
                    }
                    s.sequenceNoLast = seq;
                    s.lastEventTs    = parseLineTs(date, time, block);

                    Mode after = parseEventBody(body, s);
                    if (after == Mode.JOURNAL) {
                        mode = Mode.JOURNAL;
                        journalBuffer.clear();
                    } else if (after == Mode.NOTES) {
                        mode = Mode.NOTES;
                    } else if (after == Mode.RECORD) {
                        mode = Mode.RECORD;
                    }
                    continue;
                }

                // Not a framed line - delegated to the current mode.
                if (mode == Mode.JOURNAL) {
                    journalBuffer.add(line);
                    continue;
                }

                String stripped = line.trim();

                if (stripped.length() == 0) {
                    // Blank line semantics:
                    //  - in RECORD mode: end of [TRANSACTION RECORD] block
                    //  - in NOTES mode : tolerated; next non-blank decides
                    //  - in TOP mode   : ignored (decorative)
                    if (mode == Mode.RECORD) mode = Mode.TOP;
                    continue;
                }

                if (EjPatterns.TR_HEADER.matcher(stripped).matches()) {
                    mode = Mode.RECORD;
                    continue;
                }

                if (mode == Mode.RECORD) {
                    parseRecordLine(stripped, s);
                    continue;
                }

                if (mode == Mode.NOTES) {
                    Matcher m = EjPatterns.ND_TYPE.matcher(stripped);
                    if (m.matches()) {
                        parseNotesDispensedLine(m, s);
                    } else {
                        // line outside the ND format - exit notes mode.
                        mode = Mode.TOP;
                    }
                    continue;
                }
                // else: ignore unframed line in TOP mode (decorative continuations)

            } catch (RuntimeException ex) {
                PARSE_ERR.warn("file={} lines={}-{} unable to parse line: '{}' ({})",
                        block.getFileName(), block.getLineStart(), block.getLineEnd(),
                        EjTextUtils.truncate(line, 200), ex.toString());
            }
        }

        // Flush any open journal buffer at end of block.
        if (mode == Mode.JOURNAL && !journalBuffer.isEmpty()) {
            parseJournal(journalBuffer, s);
        }
    }

    /**
     * Returns the next mode the walker should switch into, or
     * {@code Mode.TOP} to remain at top-level.
     */
    private Mode parseEventBody(String body, ParseState s) {
        // Boundary markers - parser does not emit on these, reader handled them.
        if (EjPatterns.CARDLESS_TXN_START.matcher(body).matches()) {
            s.cardless = true;
            return Mode.TOP;
        }
        if (EjPatterns.TXN_START.matcher(body).matches()) return Mode.TOP;
        if (EjPatterns.TXN_END.matcher(body).matches())   return Mode.TOP;

        // Card / EMV / input
        Matcher m;
        if ((m = EjPatterns.EV_CARD_NUMBER.matcher(body)).matches() && s.cardNumberRaw == null) {
            s.cardNumberRaw = m.group(1); return Mode.TOP;
        }
        if ((m = EjPatterns.EV_EMV_APP.matcher(body)).matches()) {
            s.emvAid = m.group(1);
            s.emvAppLabel = m.group(2);
            return Mode.TOP;
        }
        if ((m = EjPatterns.EV_ENTERED_AMOUNT.matcher(body)).matches()) {
            s.amountEntered = parseDecimal(m.group(1));
            return Mode.TOP;
        }
        if (EjPatterns.EV_PIN_ENTERED.matcher(body).matches()) {
            s.pinEntered = true; return Mode.TOP;
        }

        // Switch protocol
        if ((m = EjPatterns.EV_TXN_REQUESTING.matcher(body)).matches()) {
            // Preserve internal whitespace (e.g. "AB     A"), trim only edges.
            s.opcode = stripCntrlTrailing(m.group(1));
            return Mode.TOP;
        }
        if ((m = EjPatterns.EV_TXN_REPLIED.matcher(body)).matches()) {
            s.fidResponse = m.group(1).trim();
            s.fidNext     = m.group(2).trim();
            return Mode.TOP;
        }
        if ((m = EjPatterns.EV_LTSERNO.matcher(body)).matches()) {
            s.ltSerno = m.group(1); return Mode.TOP;
        }
        if ((m = EjPatterns.EV_LTDISPNOTES.matcher(body)).matches()) {
            int idx = Integer.parseInt(m.group(1)) - 1;
            if (idx >= 0 && idx < 7) {
                try {
                    s.ltDispNotes[idx] = Long.parseLong(m.group(2));
                } catch (NumberFormatException ignored) {}
            }
            return Mode.TOP;
        }

        // Lifecycle flags
        if (EjPatterns.EV_DISPENSE_DONE.matcher(body).matches())    { s.dispenseComplete = true;       return Mode.TOP; }
        if (EjPatterns.EV_CASH_PRESENTED.matcher(body).matches())   { s.cashPresented = true;          return Mode.TOP; }
        if (EjPatterns.EV_CASH_TAKEN.matcher(body).matches())       { s.cashTaken = true;              return Mode.TOP; }
        if (EjPatterns.EV_CASH_TIMEOUT.matcher(body).matches())     { s.cashTakenAfterTimeout = true;  return Mode.TOP; }
        if (EjPatterns.EV_CASH_NOT_TAKEN.matcher(body).matches())   { s.cashNotTaken = true;           return Mode.TOP; }
        if (EjPatterns.EV_CARD_EJECTED.matcher(body).matches())     { s.cardEjected = true;            return Mode.TOP; }
        if (EjPatterns.EV_CARD_TAKEN.matcher(body).matches())       { s.cardTaken = true;              return Mode.TOP; }
        if (EjPatterns.EV_TXN_TIMEOUT.matcher(body).matches())      { s.transactionTimeout = true;     return Mode.TOP; }
        if (EjPatterns.EV_HOST_TIMEOUT.matcher(body).matches())     { s.hostTimeout = true;            return Mode.TOP; }

        // Sub-block openers
        if (EjPatterns.EV_JOURNAL_DATA.matcher(body).matches())          return Mode.JOURNAL;
        if (EjPatterns.EV_NOTES_DISPENSED_HDR.matcher(body).matches())   return Mode.NOTES;
        if (EjPatterns.EV_TXN_DATA_COMPLETED.matcher(body).matches())    return Mode.TOP; // no-op trigger

        return Mode.TOP;
    }

    // ============================ JOURNAL DATA =============================
    private void parseJournal(List<String> lines, ParseState s) {
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.length() == 0) continue;
            try {
                parseJournalLine(line, s);
            } catch (RuntimeException ex) {
                PARSE_ERR.warn("Unable to parse JOURNAL line '{}' ({})",
                		EjTextUtils.truncate(line, 200), ex.toString());
            }
        }
    }

    private void parseJournalLine(String line, ParseState s) {
        Matcher m;

        // ATM identification line
        m = EjPatterns.J_DATE_TIME_ATM.matcher(line);
        if (m.find() && s.atmId == null) {
            String candidate = m.group(3);
            if (candidate != null
                    && (atmIdPrefix.length() == 0 || candidate.startsWith(atmIdPrefix))) {
                s.receiptDate = m.group(1);
                s.receiptTime = m.group(2);
                s.atmId       = candidate;
            }
        }

        if ((m = EjPatterns.J_CARD_NUMBER.matcher(line)).matches()) {
            if (s.receiptCardNumber == null) s.receiptCardNumber = m.group(1);
            return;
        }
        if ((m = EjPatterns.J_ACCOUNT_NUMBER.matcher(line)).matches()) {
            s.receiptAccountNumber = m.group(1); return;
        }
        if ((m = EjPatterns.J_TXN_NO.matcher(line)).matches()) {
            String v = m.group(1).trim();
            s.txnNo = v.length() == 0 ? null : v;
            return;
        }
        if ((m = EjPatterns.J_TXN_NO_YONO.matcher(line)).matches() && s.txnNo == null) {
            String v = m.group(1).trim();
            s.txnNo = v.length() == 0 ? null : v;
            return;
        }
        if ((m = EjPatterns.J_REF_NO.matcher(line)).matches()) {
            String v = m.group(1).trim();
            s.referenceNo = v.length() == 0 ? null : v;
            return;
        }
        if ((m = EjPatterns.J_REF_NO_YONO.matcher(line)).matches() && s.referenceNo == null) {
            String v = m.group(1).trim();
            s.referenceNo = v.length() == 0 ? null : v;
            return;
        }
        if ((m = EjPatterns.J_RESPONSE_CODE.matcher(line)).matches()) {
            String v = m.group(1).trim();
            s.responseCode = v.length() == 0 ? null : v;
            return;
        }
        if ((m = EjPatterns.J_BAL_INQUIRY.matcher(line)).matches()) {
            s.accountType = m.group(1); return;
        }
        if ((m = EjPatterns.J_WITHDRAWAL.matcher(line)).matches()) {
            s.withdrawalAmount = parseDecimal(m.group(1)); return;
        }
        if ((m = EjPatterns.J_TXN_AMOUNT.matcher(line)).matches()) {
            s.transactionAmount = parseDecimal(m.group(1)); return;
        }
        if ((m = EjPatterns.J_FROM_AC.matcher(line)).matches()) {
            s.fromAccount = m.group(1); return;
        }
        if ((m = EjPatterns.J_MOD_BAL.matcher(line)).matches()) {
            s.modBalance = parseDecimal(m.group(1)); return;
        }
        if ((m = EjPatterns.J_AVAIL_BAL.matcher(line)).matches()) {
            s.availBalance = parseDecimal(m.group(1)); return;
        }
        if (EjPatterns.J_UNABLE_TO_PROCESS.matcher(line).find()) {
            s.unableToProcess = true; return;
        }
        if (EjPatterns.J_DECLINED_INSUFFICIENT.matcher(line).find()) {
            s.declinedInsufficient = true; return;
        }
        if (EjPatterns.J_ADMIN_SUBTOTAL.matcher(line).matches()) {
            s.adminSubtotal = true; return;
        }
        if (s.accountType == null && EjPatterns.ACCOUNT_TYPE_LITERALS.contains(line)) {
            s.accountType = line.replaceFirst("^\\*+", "");
        }
    }

    // ============================ [TRANSACTION RECORD] =====================
    private void parseRecordLine(String line, ParseState s) {
        Matcher m;
        if ((m = EjPatterns.TR_OPCODE.matcher(line)).matches()) {
            String op = stripCntrlTrailing(m.group(1));
            if (s.opcode == null) s.opcode = op;
            return;
        }
        if ((m = EjPatterns.TR_FUNCTION_ID.matcher(line)).matches()) {
            s.functionId = m.group(1).trim(); return;
        }
        if ((m = EjPatterns.TR_AMOUNT.matcher(line)).matches()) {
            s.amountRecorded = parseDecimal(m.group(1)); return;
        }
        if ((m = EjPatterns.TR_TRANS_SEQ.matcher(line)).matches()) {
            s.transSeqNumber = m.group(1); return;
        }
        if ((m = EjPatterns.TR_DENOMINATION.matcher(line)).matches()) {
            s.denomination = m.group(1); return;
        }
        if ((m = EjPatterns.TR_REQUEST_CNT.matcher(line)).matches()) {
            s.requestCount = m.group(1); return;
        }
        if ((m = EjPatterns.TR_PICKUP_CNT.matcher(line)).matches()) {
            s.pickupCount = m.group(1); return;
        }
        if ((m = EjPatterns.TR_DISPENSE_CNT.matcher(line)).matches()) {
            s.dispenseCount = m.group(1); return;
        }
        if ((m = EjPatterns.TR_REMAIN_CNT.matcher(line)).matches()) {
            s.remainCount = m.group(1); return;
        }
        if ((m = EjPatterns.TR_REJECT_CNT.matcher(line)).matches()) {
            s.rejectCount = m.group(1); return;
        }
    }

    // ============================ Notes Dispensed: =========================
    private void parseNotesDispensedLine(Matcher m, ParseState s) {
        char t = m.group(1).charAt(0);
        long count;
        try {
            count = Long.parseLong(m.group(3));
        } catch (NumberFormatException e) {
            return;
        }
        int idx = t - 'A';
        if (idx >= 0 && idx < 7) {
            s.notesType[idx] = count;
        }
    }

    // ============================ classification ===========================
    private static void finalizeClassification(ParseState s, boolean truncated, boolean malformed) {
        // Type
        if (s.adminSubtotal) {
            s.txnType = Type.ADMIN_SUBTOTAL;
        } else if (s.cardless) {
            s.txnType = Type.CARDLESS_WITHDRAWAL;
        } else if (s.withdrawalAmount != null) {
            s.txnType = Type.WITHDRAWAL;
        } else if (s.accountType != null && !s.unableToProcess && !s.declinedInsufficient) {
            s.txnType = Type.BALANCE_INQUIRY;
        } else if (s.unableToProcess || s.declinedInsufficient) {
            s.txnType = Type.FAILED;
        } else if (s.pinEntered || s.amountEntered != null) {
            s.txnType = Type.INCOMPLETE;
        } else {
            s.txnType = Type.OTHER;
        }

        // Status
        if (s.adminSubtotal) {
            s.txnStatus = Status.ADMIN;
        } else if (s.declinedInsufficient) {
            s.txnStatus = Status.DECLINED_INSUFFICIENT;
        } else if (s.unableToProcess) {
            s.txnStatus = Status.UNABLE_TO_PROCESS;
        } else if (s.transactionTimeout) {
            s.txnStatus = Status.TIMEOUT;
        } else if (s.cashTakenAfterTimeout) {
            s.txnStatus = Status.CASH_TAKEN_LATE;
        } else if (s.cashNotTaken) {
            s.txnStatus = Status.CASH_NOT_TAKEN;
        } else if (isSuccess(s.responseCode)) {
            s.txnStatus = Status.SUCCESS;
        } else if (truncated) {
            s.txnStatus = Status.TRUNCATED;
        } else if (malformed) {
            s.txnStatus = Status.MALFORMED;
        } else if (s.responseCode != null) {
            s.txnStatus = Status.FAILED;
        } else if (s.cardNumberRaw != null) {
            s.txnStatus = Status.INCOMPLETE;
        } else {
            s.txnStatus = Status.NO_CARD;
        }
    }

    private static boolean isSuccess(String responseCode) {
        if (responseCode == null) return false;
        String c = responseCode.trim();
        return "000".equals(c) || "00".equals(c);
    }

    // ============================ helpers ==================================
    private static BigDecimal parseDecimal(String raw) {
        if (EjTextUtils.isBlank(raw)) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            PARSE_ERR.warn("Cannot parse decimal value: '{}'", EjTextUtils.truncate(raw, 80));
            return null;
        }
    }

    private static String stripCntrlTrailing(String s) {
        if (s == null) return null;
        return s.replaceAll("[\\p{Cntrl}\\s]+$", "");
    }

    private static LocalDateTime parseLineTs(String date, String time, RawTransactionBlock block) {
        try {
            return LocalDateTime.parse(date + " " + time, LINE_TS);
        } catch (DateTimeParseException e) {
            PARSE_ERR.warn("Cannot parse timestamp {} {} (file={} lines={}-{})",
                    date, time, block.getFileName(), block.getLineStart(), block.getLineEnd());
            return null;
        }
    }

    // ============================ state ====================================
    private enum Mode { TOP, JOURNAL, RECORD, NOTES }

    private static final class ParseState {
        // identity / framing
        String sequenceNoFirst, sequenceNoLast;
        LocalDateTime firstEventTs, lastEventTs;

        // card / EMV / input
        String cardNumberRaw, emvAid, emvAppLabel;
        BigDecimal amountEntered;
        boolean pinEntered, cardless;

        // switch / protocol
        String opcode, functionId, transSeqNumber, ltSerno, fidResponse, fidNext;
        BigDecimal amountRecorded;
        Long[] ltDispNotes = new Long[7];

        // dispenser telemetry
        String denomination, requestCount, pickupCount, dispenseCount, remainCount, rejectCount;
        Long[] notesType = new Long[7];

        // lifecycle flags
        boolean dispenseComplete, cashPresented, cashTaken, cashTakenAfterTimeout, cashNotTaken;
        boolean cardEjected, cardTaken, transactionTimeout, hostTimeout;

        // receipt
        String receiptDate, receiptTime, atmId;
        String receiptCardNumber, receiptAccountNumber;
        String txnNo, referenceNo, responseCode, accountType, fromAccount;
        BigDecimal withdrawalAmount, transactionAmount, modBalance, availBalance;

        boolean unableToProcess, declinedInsufficient, adminSubtotal;

        // classification
        Type   txnType;
        Status txnStatus;
    }
}
