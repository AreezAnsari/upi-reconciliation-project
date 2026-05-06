package com.jpb.reconciliation.reconciliation.diebold.parser;

import com.jpb.reconciliation.reconciliation.diebold.model.EjTransaction;
import com.jpb.reconciliation.reconciliation.diebold.model.EjTransaction.Status;
import com.jpb.reconciliation.reconciliation.diebold.model.EjTransaction.Type;
import com.jpb.reconciliation.reconciliation.diebold.model.RawTransactionBlock;
import com.jpb.reconciliation.reconciliation.util.EjTextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Parses one Diebold {@link RawTransactionBlock} into one {@link EjTransaction}.
 *
 * <p>Diebold header format: XNNNNNNDDMMYYHHMMSS
 * <ul>
 *   <li>X = block type: 1=receipt, 4=card-event, 0=error/failed</li>
 *   <li>NNNNNN = 6-digit sequence number</li>
 *   <li>DDMMYY = date</li>
 *   <li>HHMMSS = time</li>
 * </ul>
 *
 * <p>The parser always returns a non-null result — a bad field never drops the row.
 * The full raw block is always preserved for audit.
 */
public final class EjTransactionParser {

    private static final Logger LOG       = LoggerFactory.getLogger(EjTransactionParser.class);
    private static final Logger PARSE_ERR = LoggerFactory.getLogger(
            "com.jpb.reconciliation.reconciliation.diebold.parser.errors");

    /** Header date: DDMMYY → parsed as ddMMyy */
    private static final DateTimeFormatter HDR_DT =
            DateTimeFormatter.ofPattern("MMddyyHHmmss");

    private final String loadBatchId;
    private final String atmIdPrefix;

    public EjTransactionParser(String loadBatchId, String atmIdPrefix) {
        this.loadBatchId  = loadBatchId;
        this.atmIdPrefix  = (atmIdPrefix != null) ? atmIdPrefix : "";
    }

    public EjTransactionParser(String loadBatchId) {
        this(loadBatchId, "");
    }

    // ===================================================================
    public EjTransaction parse(RawTransactionBlock block) {
        ParseState s = new ParseState();

        try {
            parseHeader(block, s);
            parseBody(block, s);
        } catch (RuntimeException ex) {
            PARSE_ERR.warn("Unhandled error in block file={} lines={}-{}: {}",
                    block.getFileName(), block.getLineStart(), block.getLineEnd(), ex.toString());
        }

        classify(s, block.isMalformed());

        return EjTransaction.builder()
                .fileName(block.getFileName())
                .fileLineStart(block.getLineStart())
                .fileLineEnd(block.getLineEnd())
                .blockTypePrefix(s.blockTypePrefix)
                .sequenceNumber(s.sequenceNumber)
                .headerDate(s.headerDate)
                .headerTime(s.headerTime)
                .headerDateTime(s.headerDateTime)
                .location(s.location)
                .receiptDate(s.receiptDate)
                .receiptTime(s.receiptTime)
                .atmId(s.atmId)
                .cardNumber(s.cardNumber)
                .txnNo(EjTextUtils.nullIfBlank(s.txnNo))
                .referenceNo(EjTextUtils.nullIfBlank(s.referenceNo))
                .responseCode(EjTextUtils.nullIfBlank(s.responseCode))
                .accountType(s.accountType)
                .fromAccount(s.fromAccount)
                .withdrawalAmount(s.withdrawalAmount)
                .transactionAmount(s.transactionAmount)
                .modBalance(s.modBalance)
                .availBalance(s.availBalance)
                .serialNo(s.serialNo)
                .unableToProcess(s.unableToProcess)
                .cardlessWithdrawal(s.cardlessWithdrawal)
                .processingRestrictions(s.processingRestrictions)
                .cardDataError(s.cardDataError)
                .pinEntered(s.pinEntered)
                .errorCode(s.errorCode)
                .errorDescription(s.errorDescription)
                .transactionType(s.txnType)
                .transactionStatus(s.txnStatus)
                .rawTransactionBlock(block.asText())
                .loadBatchId(loadBatchId)
                .build();
    }

    // ── header parsing ───────────────────────────────────────────────────
    private void parseHeader(RawTransactionBlock block, ParseState s) {
        List<String> lines = block.getLines();
        if (lines.isEmpty()) return;

        String header = lines.get(0).trim();
        Matcher m = EjPatterns.BLOCK_HEADER.matcher(header);
        if (!m.matches()) return;

        s.blockTypePrefix = m.group(1);
        s.sequenceNumber  = m.group(2);
        s.headerDate      = m.group(3); // DDMMYY
        s.headerTime      = m.group(4); // HHMMSS

        try {
            s.headerDateTime = LocalDateTime.parse(s.headerDate + s.headerTime, HDR_DT);
        } catch (DateTimeParseException e) {
            PARSE_ERR.warn("Cannot parse header datetime: {} {} file={}", s.headerDate, s.headerTime,
                    block.getFileName());
        }
    }

    // ── body parsing ────────────────────────────────────────────────────
    private void parseBody(RawTransactionBlock block, ParseState s) {
        List<String> lines = block.getLines();
        boolean expectAtmData   = false;
        boolean locationCaptured = false;

        for (int i = 1; i < lines.size(); i++) {
            String raw     = lines.get(i);
            String line    = (raw == null) ? "" : raw;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            try {
                // Skip LUNO000 status line
                if (trimmed.matches("^[0-9]{3}[A-Z].*") || trimmed.equalsIgnoreCase("LUNO000")) {
                    // could be LUNO000 or error code line
                    Matcher em = EjPatterns.ERROR_HDR.matcher(trimmed);
                    if (em.matches()) {
                        s.errorCode = em.group(1);
                    }
                    continue;
                }

                // DATE TIME ATM ID header
                if (EjPatterns.DATE_TIME_ATM_HDR.matcher(trimmed).matches()) {
                    expectAtmData = true;
                    continue;
                }

                // ATM data line
                if (expectAtmData) {
                    Matcher dm = EjPatterns.DATE_TIME_ATM_DATA.matcher(trimmed);
                    if (dm.matches()) {
                        s.receiptDate = dm.group(1);
                        s.receiptTime = dm.group(2);
                        String candidate = dm.group(3);
                        if (atmIdPrefix.isEmpty() || candidate.startsWith(atmIdPrefix)) {
                            s.atmId = candidate;
                        }
                        expectAtmData = false;
                        continue;
                    }
                    expectAtmData = false;
                }

                // Location (2nd non-LUNO line in receipt blocks, before DATE TIME ATM ID)
                if ("1".equals(s.blockTypePrefix) && !locationCaptured
                        && s.receiptDate == null
                        && !trimmed.startsWith("CARD")
                        && !trimmed.startsWith("TXN")
                        && !trimmed.startsWith("RESPONSE")
                        && !trimmed.startsWith("BAL")
                        && !trimmed.startsWith("FROM")
                        && !trimmed.startsWith("AVAIL")
                        && !trimmed.startsWith("MOD")
                        && !trimmed.startsWith("ACCOUNT")
                        && !trimmed.startsWith("REFERENCE")
                        && !trimmed.startsWith("WELCOME")
                        && !trimmed.matches("[0-9A-F]{8}")  // checksum
                        && trimmed.length() > 3) {
                    s.location = trimmed;
                    locationCaptured = true;
                    continue;
                }

                Matcher m;

                // Card number
                if ((m = EjPatterns.CARD_NUMBER.matcher(trimmed)).matches()) {
                    if (s.cardNumber == null) s.cardNumber = m.group(1).replaceAll("\\s+", "");
                    continue;
                }

                // Card inserted (type 4 blocks)
                if ((m = EjPatterns.CARD_INSERTED.matcher(trimmed)).matches()) {
                    if (s.cardNumber == null) s.cardNumber = m.group(1).replaceAll("\\s+", "");
                    continue;
                }

                // TXN number
                if ((m = EjPatterns.TXN_NO.matcher(trimmed)).matches()) {
                    s.txnNo = m.group(1).trim(); continue;
                }

                // Reference number
                if ((m = EjPatterns.REFERENCE_NO.matcher(trimmed)).matches()) {
                    s.referenceNo = m.group(1).trim(); continue;
                }

                // Response code
                if ((m = EjPatterns.RESPONSE_CODE.matcher(trimmed)).matches()) {
                    s.responseCode = m.group(1).trim(); continue;
                }

                // Withdrawal amount
                if ((m = EjPatterns.WITHDRAWAL.matcher(trimmed)).matches()) {
                    s.withdrawalAmount = parseDecimal(m.group(1)); continue;
                }

                // YONO transaction amount
                if ((m = EjPatterns.TXN_AMOUNT.matcher(trimmed)).matches()) {
                    s.transactionAmount = parseDecimal(m.group(1));
                    s.cardlessWithdrawal = true;
                    continue;
                }

                // Balance inquiry
                if ((m = EjPatterns.BAL_INQUIRY.matcher(trimmed)).matches()) {
                    s.accountType = m.group(1).trim(); continue;
                }

                // From account
                if ((m = EjPatterns.FROM_AC.matcher(trimmed)).matches()) {
                    s.fromAccount = m.group(1).trim(); continue;
                }

                // MOD balance
                if ((m = EjPatterns.MOD_BAL.matcher(trimmed)).matches()) {
                    s.modBalance = parseDecimal(m.group(1)); continue;
                }

                // Avail balance
                if ((m = EjPatterns.AVAIL_BAL.matcher(trimmed)).matches()) {
                    s.availBalance = parseDecimal(m.group(1)); continue;
                }

                // YONO account number
                if ((m = EjPatterns.ACCOUNT_NUMBER.matcher(trimmed)).matches()) {
                    if (s.cardNumber == null) s.cardNumber = m.group(1); continue;
                }

                // YONO success flag
                if (EjPatterns.YONO_SUCCESS.matcher(trimmed).matches()) {
                    s.cardlessWithdrawal = true; continue;
                }

                // Unable to process
                if (EjPatterns.UNABLE_TO_PROCESS.matcher(trimmed).matches()) {
                    s.unableToProcess = true; continue;
                }

                // Serial number (error blocks)
                if ((m = EjPatterns.SERIAL_NO.matcher(trimmed)).matches()) {
                    s.serialNo = m.group(1); continue;
                }

                // Error descriptions
                if (EjPatterns.PROCESSING_RESTRICTIONS.matcher(trimmed).matches()) {
                    s.processingRestrictions = true;
                    s.errorDescription = trimmed.replaceAll("[A-F0-9]{8}\\s*$", "").trim();
                    continue;
                }
                if (EjPatterns.CARD_DATA_ERROR.matcher(trimmed).matches()) {
                    s.cardDataError = true;
                    s.errorDescription = trimmed.replaceAll("[A-F0-9]{8}\\s*$", "").trim();
                    continue;
                }
                if (EjPatterns.OUT_OF_SEQUENCE.matcher(trimmed).matches()) {
                    s.errorDescription = "OUT OF SEQUENCE"; continue;
                }
                if (EjPatterns.IO_ERROR.matcher(trimmed).matches()) {
                    s.errorDescription = "I/O ERROR"; continue;
                }

                // Account type literals
                if (s.accountType == null && EjPatterns.ACCOUNT_TYPE_LITERALS.contains(trimmed.toUpperCase())) {
                    s.accountType = trimmed; continue;
                }

            } catch (RuntimeException ex) {
                PARSE_ERR.warn("file={} lines={}-{} unable to parse line: '{}' ({})",
                        block.getFileName(), block.getLineStart(), block.getLineEnd(),
                        EjTextUtils.truncate(trimmed, 200), ex.toString());
            }
        }
    }

    // ── classification ───────────────────────────────────────────────────
    private static void classify(ParseState s, boolean malformed) {
        // Type
        if (s.cardlessWithdrawal && s.transactionAmount != null) {
            s.txnType = Type.CARDLESS_WITHDRAWAL;
        } else if (s.withdrawalAmount != null) {
            s.txnType = Type.WITHDRAWAL;
        } else if (s.accountType != null && !s.unableToProcess && !s.processingRestrictions) {
            s.txnType = Type.BALANCE_INQUIRY;
        } else if (s.unableToProcess || s.processingRestrictions || s.cardDataError) {
            s.txnType = Type.FAILED;
        } else if ("4".equals(s.blockTypePrefix)) {
            s.txnType = Type.OTHER;
        } else {
            s.txnType = Type.INCOMPLETE;
        }

        // Status
        if (s.processingRestrictions) {
            s.txnStatus = Status.PROCESSING_RESTRICTIONS;
        } else if (s.cardDataError) {
            s.txnStatus = Status.CARD_DATA_ERROR;
        } else if (s.unableToProcess) {
            s.txnStatus = Status.UNABLE_TO_PROCESS;
        } else if (malformed) {
            s.txnStatus = Status.MALFORMED;
        } else if (isSuccess(s.responseCode)) {
            s.txnStatus = Status.SUCCESS;
        } else if (s.responseCode != null) {
            s.txnStatus = Status.FAILED;
        } else if ("0".equals(s.blockTypePrefix)) {
            s.txnStatus = Status.FAILED;
        } else if (s.cardNumber != null) {
            s.txnStatus = Status.INCOMPLETE;
        } else {
            s.txnStatus = Status.NO_CARD;
        }
    }

    private static boolean isSuccess(String rc) {
        if (rc == null) return false;
        String c = rc.trim();
        return "000".equals(c) || "00".equals(c);
    }

    private static BigDecimal parseDecimal(String raw) {
        if (EjTextUtils.isBlank(raw)) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            PARSE_ERR.warn("Cannot parse decimal: '{}'", raw);
            return null;
        }
    }

    // ── state ────────────────────────────────────────────────────────────
    private static final class ParseState {
        String        blockTypePrefix;
        String        sequenceNumber;
        String        headerDate;
        String        headerTime;
        LocalDateTime headerDateTime;
        String        location;
        String        receiptDate;
        String        receiptTime;
        String        atmId;
        String        cardNumber;
        String        txnNo;
        String        referenceNo;
        String        responseCode;
        String        accountType;
        String        fromAccount;
        BigDecimal    withdrawalAmount;
        BigDecimal    transactionAmount;
        BigDecimal    modBalance;
        BigDecimal    availBalance;
        String        serialNo;
        boolean       unableToProcess;
        boolean       cardlessWithdrawal;
        boolean       processingRestrictions;
        boolean       cardDataError;
        boolean       pinEntered;
        String        errorCode;
        String        errorDescription;
        Type          txnType;
        Status        txnStatus;
    }
}