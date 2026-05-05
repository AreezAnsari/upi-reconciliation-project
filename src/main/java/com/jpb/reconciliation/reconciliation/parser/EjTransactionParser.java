package com.jpb.reconciliation.reconciliation.parser;

import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.util.EjTextUtils;
import com.jpb.reconciliation.reconciliation.dto.EjRawTransactionBlock;
import com.jpb.reconciliation.reconciliation.dto.EjTransaction;
import com.jpb.reconciliation.reconciliation.dto.EjTransaction.Status;
import com.jpb.reconciliation.reconciliation.dto.EjTransaction.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;

/**
 * Parses a single {@link EjRawTransactionBlock} into an {@link EjTransaction}.
 *
 * <p>The parser holds only immutable configuration on the instance; all
 * mutable parsing state lives in a per-call {@link ParseState} object. A
 * single parser instance can therefore be reused for many blocks
 * <em>sequentially</em>, but is <b>not thread-safe across concurrent
 * {@code parse} calls if the same configuration is reused</em> - in practice
 * this is fine because the only instance fields are immutable strings.
 *
 * <p>Errors during parsing are caught at the field level and logged; the
 * parser always returns a non-null result so a single bad field never sinks
 * an entire row. The full raw block is always preserved for audit.
 */
public final class EjTransactionParser {

    private static final Logger LOG       = LoggerFactory.getLogger(EjTransactionParser.class);
    private static final Logger PARSE_ERR = LoggerFactory.getLogger("com.bank.atmej.parser.errors");

    private static final DateTimeFormatter LOG_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /** Configuration: identifies which token in the receipt's date/time/atm-id row is the ATM ID. */
    private final String atmIdPrefix;

    /** UUID of the load run that inserted this row (groups together a single Java run). */
    private final String loadBatchId;

    /**
     * @param loadBatchId identifier propagated to every row produced by this parser.
     * @param atmIdPrefix prefix that the ATM identifier must start with for the receipt
     *                    line to be accepted (e.g. {@code "EFNJ"}). Use the empty string
     *                    to disable the guard - then any non-blank token in the right
     *                    position will be taken as the ATM ID.
     */
    public EjTransactionParser(String loadBatchId, String atmIdPrefix) {
        this.loadBatchId = loadBatchId;
        this.atmIdPrefix = (atmIdPrefix != null) ? atmIdPrefix : "";
    }

    /** Convenience constructor with an empty prefix guard. */
    public EjTransactionParser(String loadBatchId) {
        this(loadBatchId, "");
    }

    public EjTransaction parse(EjRawTransactionBlock block) {
        ParseState s = new ParseState();

        for (String raw : block.getLines()) {
            try {
                parseLine(EjTextUtils.stripLeading(raw), s);
            } catch (RuntimeException ex) {
                PARSE_ERR.warn("file={} lines={}-{} unable to parse line: '{}' ({})",
                        block.getFileName(), block.getLineStart(), block.getLineEnd(),
                        EjTextUtils.truncate(raw, 200), ex.toString());
            }
        }

        finalizeStatus(s);

        return EjTransaction.builder()
                .fileName(block.getFileName())
                .fileLineStart(block.getLineStart())
                .fileLineEnd(block.getLineEnd())
                .sequenceNumber(s.sequenceNumber)
                .logDate(s.logDate)
                .logTime(s.logTime)
                .transactionDateTime(buildDateTime(s.logDate, s.logTime, block))
                .atmId(s.atmId)
                .receiptDate(s.receiptDate)
                .receiptTime(s.receiptTime)
                .cardNumber(s.cardNumber)
                .cardNumberRaw(s.cardNumberRaw)
                .txnNo(EjTextUtils.nullIfBlank(s.txnNo))
                .referenceNo(EjTextUtils.nullIfBlank(s.referenceNo))
                .responseCode(EjTextUtils.nullIfBlank(s.responseCode))
                .functionId(s.functionId)
                .txnSerialNo(s.txnSerialNo)
                .transactionType(s.txnType != null ? s.txnType : Type.OTHER)
                .transactionStatus(s.txnStatus != null ? s.txnStatus : Status.UNKNOWN)
                .accountType(s.accountType)
                .fromAccount(s.fromAccount)
                .requestAmount(s.requestAmount)
                .amountEntered(s.amountEntered)
                .withdrawalAmount(s.withdrawalAmount)
                .modBalance(s.modBalance)
                .availBalance(s.availBalance)
                .opcode(s.opcode)
                .notesPresented(s.notesPresented)
                .notesTaken(s.notesTaken)
                .notesStacked(s.notesStacked)
                .dispenseCount (s.cashSeen ? s.dispenseTotal  : null)
                .rejectedCount (s.cashSeen ? s.rejectedTotal  : null)
                .remainingCount(s.cashSeen ? s.remainingTotal : null)
                .errorSeverity(s.errorSeverity)
                .diagnosticStatus(s.diagnosticStatus)
                .pinEntered(s.pinEntered)
                .cardInserted(s.cardInserted)
                .admin(s.admin)
                .rawTransactionBlock(block.asText())
                .loadBatchId(loadBatchId)
                .build();
    }

    // ============================ line dispatch ============================
    private void parseLine(String line, ParseState s) {
        if (line.isEmpty()) {
            // Blank line terminates the cassette-totals section.
            s.cashSectionActive = false;
            return;
        }

        // ---- sequence header (only the first match wins) ------------------
        if (s.sequenceNumber == null) {
            Matcher m = EjPatterns.SEQ_HEADER.matcher(line);
            if (m.find()) {
                s.sequenceNumber = m.group(1);
                s.logDate        = m.group(2);
                s.logTime        = m.group(3);
            }
        }

        // ---- card-reader telemetry ----------------------------------------
        if (line.contains("CARD INSERTED")) s.cardInserted = true;

        Matcher m = EjPatterns.CARD_RAW.matcher(line);
        if (m.find()) s.cardNumberRaw = m.group(1);

        m = EjPatterns.AMOUNT_ENTERED.matcher(line);
        if (m.find()) {
            // Last-entered amount wins (the customer can re-enter).
            s.amountEntered = parseDecimal(m.group(1));
        }

        if (EjPatterns.PIN_ENTERED.matcher(line).find()) s.pinEntered = true;

        m = EjPatterns.OPCODE.matcher(line);
        if (m.find()) {
            String op = m.group(1);
            // Strip trailing control chars / odd whitespace; keep internal spaces.
            op = op.replaceAll("[\\p{Cntrl}]+$", "").replaceAll("\\s+$", "");
            s.opcode = op;
        }

        m = EjPatterns.REQUEST_AMOUNT.matcher(line);
        if (m.find() && m.group(1) != null) {
            s.requestAmount = parseDecimal(m.group(1));
        }

        m = EjPatterns.RESPONSE_RECEIVED.matcher(line);
        if (m.find()) {
            s.functionId  = m.group(1).trim();
            s.txnSerialNo = m.group(2).trim();
        }

        // ---- receipt body --------------------------------------------------
        m = EjPatterns.RECEIPT_DATE_TIME_ATM.matcher(line);
        if (m.find() && s.atmId == null) {
            String candidate = m.group(3);
            if (candidate != null && (atmIdPrefix.isEmpty() || candidate.startsWith(atmIdPrefix))) {
                s.receiptDate = m.group(1);
                s.receiptTime = m.group(2);
                s.atmId       = candidate;
            }
        }

        m = EjPatterns.RECEIPT_CARD_NUMBER.matcher(line);
        if (m.find()) s.cardNumber = m.group(1);

        m = EjPatterns.RECEIPT_TXN_NO.matcher(line);
        if (m.find()) {
            String v = m.group(1).trim();
            s.txnNo = v.isEmpty() ? null : v;
        }

        m = EjPatterns.RECEIPT_REFERENCE.matcher(line);
        if (m.find()) {
            String v = m.group(1).trim();
            s.referenceNo = v.isEmpty() ? null : v;
        }

        m = EjPatterns.RECEIPT_RESPONSE.matcher(line);
        if (m.find()) {
            String v = m.group(1).trim();
            s.responseCode = v.isEmpty() ? null : v;
        }

        m = EjPatterns.RECEIPT_WITHDRAWAL.matcher(line);
        if (m.find()) {
            s.withdrawalAmount = parseDecimal(m.group(1));
            if (s.txnType == null) s.txnType = Type.WITHDRAWAL;
            s.isWithdrawal = true;
        }

        m = EjPatterns.RECEIPT_BAL_INQUIRY.matcher(line);
        if (m.find()) {
            s.txnType          = Type.BALANCE_INQUIRY;
            s.accountType      = m.group(1);
            s.isBalanceInquiry = true;
        }

        m = EjPatterns.RECEIPT_FROM_AC.matcher(line);
        if (m.find()) s.fromAccount = m.group(1);

        m = EjPatterns.RECEIPT_MOD_BAL.matcher(line);
        if (m.find()) s.modBalance = parseDecimal(m.group(1));

        m = EjPatterns.RECEIPT_AVAIL_BAL.matcher(line);
        if (m.find()) s.availBalance = parseDecimal(m.group(1));

        // ---- account type literal (failure receipts: bare line "SAVINGS" etc) ----
        if (s.accountType == null && EjPatterns.ACCOUNT_TYPE_LITERALS.contains(line.trim())) {
            s.accountType = line.trim().replaceFirst("^\\*+", "");
        }

        // ---- failure / cancel / timeout markers ---------------------------
        if (EjPatterns.RECEIPT_UNABLE_TO_PROCESS.matcher(line).find()
                || EjPatterns.RECEIPT_UNABLE_TO_DISPENSE.matcher(line).find()) {
            s.isFailed = true;
            if (s.txnType == null)   s.txnType   = Type.FAILED;
            if (s.txnStatus == null) s.txnStatus = Status.FAILED;
        }
        if (EjPatterns.CUSTOMER_CANCELLED.matcher(line).find()) s.txnStatus = Status.CUSTOMER_CANCELLED;
        if (EjPatterns.CUSTOMER_TIMEOUT.matcher(line).find())   s.txnStatus = Status.CUSTOMER_TIMEOUT;

        // ---- notes / cash dispenser ---------------------------------------
        m = EjPatterns.NOTES_PRESENTED.matcher(line);
        if (m.find()) s.notesPresented = m.group(1);
        if (EjPatterns.NOTES_TAKEN.matcher(line).find())   s.notesTaken   = true;
        if (EjPatterns.NOTES_STACKED.matcher(line).find()) s.notesStacked = true;

        // ---- cassette totals (multi-block: there are usually two CASH TOTAL
        // sections per withdrawal, one for cassettes 1-4, one for 5-7) ------
        if (EjPatterns.CASH_TOTAL_HEADER.matcher(line).find()) {
            s.cashSectionActive = true;
            s.cashSeen          = true;
            return;
        }
        if (s.cashSectionActive) {
            if (EjPatterns.DISPENSED_ROW.matcher(line).find()) {
                s.dispenseTotal += sumNumbers(line);
            } else if (EjPatterns.REJECTED_ROW.matcher(line).find()) {
                s.rejectedTotal += sumNumbers(line);
            } else if (EjPatterns.REMAINING_ROW.matcher(line).find()) {
                s.remainingTotal += sumNumbers(line);
            } else if (line.startsWith("[020t") || line.startsWith("DENOMINATION")) {
                // still inside the dispenser block - keep cashSectionActive true
            } else {
                s.cashSectionActive = false;
            }
        }

        // ---- diagnostics --------------------------------------------------
        m = EjPatterns.ERROR_SEVERITY.matcher(line);
        if (m.find()) s.errorSeverity = m.group(1).trim();

        m = EjPatterns.DIAGNOSTIC_STATUS.matcher(line);
        if (m.find()) s.diagnosticStatus = m.group(1).trim();

        // ---- admin / subtotal --------------------------------------------
        if (EjPatterns.ADMIN_SUBTOTAL.matcher(line).find()
                || EjPatterns.ADMIN_BALANCE.matcher(line).find()) {
            s.admin   = true;
            s.txnType = Type.ADMIN_SUBTOTAL;
        }
    }

    // ============================ post-processing ==========================
    private void finalizeStatus(ParseState s) {
        if (s.txnStatus != null) return; // already set (CANCELLED / TIMEOUT / FAILED)

        if (s.admin) {
            s.txnStatus = Status.ADMIN;
            return;
        }
        if (s.isFailed) {
            s.txnStatus = Status.FAILED;
            return;
        }
        if (isSuccess(s.responseCode) && (s.isWithdrawal || s.isBalanceInquiry)) {
            s.txnStatus = Status.SUCCESS;
            return;
        }
        if (!s.cardInserted) {
            s.txnStatus = Status.NO_CARD;
            return;
        }
        s.txnStatus = Status.INCOMPLETE;
    }

    private static boolean isSuccess(String responseCode) {
        if (responseCode == null) return false;
        String c = responseCode.trim();
        return "000".equals(c) || "00".equals(c);
    }

    // ============================ helpers ==================================
    static long sumNumbers(String line) {
        long sum = 0;
        Matcher m = EjPatterns.CASSETTE_NUMBER.matcher(line);
        while (m.find()) {
            try {
                sum += Long.parseLong(m.group());
            } catch (NumberFormatException e) {
                LOG.warn("Skipping unparseable number token: {}", e.getMessage());
            }
        }
        return sum;
    }

    static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse decimal value, returning null: {}", e.getMessage());
            return null;
        }
    }

    private static LocalDateTime buildDateTime(String logDate, String logTime, EjRawTransactionBlock block) {
        if (logDate == null || logTime == null) return null;
        try {
            LocalDate date = LocalDate.parse(logDate, LOG_DATE);
            LocalTime time = (logTime.length() == 5)
                    ? LocalTime.parse(logTime + ":00")
                    : LocalTime.parse(logTime);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            PARSE_ERR.warn("Cannot build timestamp from {} {} (file={} lines={}-{})",
                    logDate, logTime, block.getFileName(), block.getLineStart(), block.getLineEnd());
            return null;
        }
    }

    // ============================ parse state ==============================
    /** Mutable scratch state used while walking lines of a single block. */
    private static final class ParseState {
        // identity
        String sequenceNumber, logDate, logTime;
        String atmId, receiptDate, receiptTime;
        // card
        String cardNumber, cardNumberRaw;
        // switch
        String txnNo, referenceNo, responseCode, functionId, txnSerialNo;
        // classification
        Type   txnType;
        Status txnStatus;
        String accountType, fromAccount;
        // amounts
        BigDecimal requestAmount, amountEntered, withdrawalAmount, modBalance, availBalance;
        // protocol
        String opcode;
        // notes / cash
        String notesPresented;
        boolean notesTaken, notesStacked;
        long dispenseTotal, rejectedTotal, remainingTotal;
        boolean cashSectionActive;
        boolean cashSeen;
        // diagnostics
        String errorSeverity, diagnosticStatus;
        // flags
        boolean pinEntered, cardInserted, admin;
        // helpers
        boolean isWithdrawal, isBalanceInquiry, isFailed;
    }
}
