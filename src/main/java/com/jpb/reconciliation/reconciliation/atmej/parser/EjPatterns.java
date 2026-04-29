//package com.jpb.reconciliation.reconciliation.atmej.parser;
//
//import java.util.Set;
//import java.util.regex.Pattern;
//
///**
// * Central registry of regular-expression patterns used across the loader.
// *
// * <p>Patterns are compiled once at class-load time. Keeping them here (rather
// * than scattered through {@link EjTransactionParser}) makes it easy to add
// * tests, reuse them in the reader, or override them for variant vendor formats
// * without touching parsing logic.
// *
// * <h3>Sections</h3>
// * <ol>
// *   <li><b>Boundary patterns</b> - used by {@code EjFileReader} to delimit a
// *       transaction block.</li>
// *   <li><b>Field patterns</b> - used by {@code EjTransactionParser} to pull
// *       individual fields out of a block's lines.</li>
// * </ol>
// */
//public final class EjPatterns {
//
//    private EjPatterns() {}
//
//    // =========================================================================
//    // 1. BOUNDARY PATTERNS  (also used by the reader)
//    // =========================================================================
//
//    /** Sequence header: e.g. "[020t*476*04/02/2025*06:26:55*". Time may be HH:MI or HH:MI:SS. */
//    public static final Pattern SEQ_HEADER =
//            Pattern.compile("\\[\\d+t\\*(\\d+)\\*(\\d{2}/\\d{2}/\\d{4})\\*(\\d{2}:\\d{2}(?::\\d{2})?)\\*");
//
//    public static final Pattern TXN_START = Pattern.compile("\\*TRANSACTION START\\*");
//    public static final Pattern TXN_END   = Pattern.compile("TRANSACTION END");
//
//    /** Marker that starts a non-transaction segment between consecutive transactions. */
//    public static final String  PRIMARY_CARD_READER_MARKER = "*PRIMARY CARD READER ACTIVATED*";
//
//    // =========================================================================
//    // 2. FIELD PATTERNS  (used by the parser)
//    // =========================================================================
//
//    // ---- Card-reader / EMV chatter ------------------------------------------
//    public static final Pattern CARD_INSERTED = Pattern.compile("CARD INSERTED");
//    /** Masked PAN as captured from card chip:  "CARD: 512652******2153" */
//    public static final Pattern CARD_RAW = Pattern.compile("CARD:\\s*(\\d{6}\\*+\\d{3,4})");
//
//    public static final Pattern PIN_ENTERED = Pattern.compile("PIN ENTERED");
//
//    /** Customer keypad amount entry: "08:06:44 AMOUNT 10000 ENTERED" */
//    public static final Pattern AMOUNT_ENTERED = Pattern.compile("AMOUNT\\s+(\\d+)\\s+ENTERED");
//
//    /**
//     * OPCODE is everything after "OPCODE = ". Preserve internal spacing
//     * (e.g. "CA   C") - the trailing chars carry meaning to the switch.
//     */
//    public static final Pattern OPCODE = Pattern.compile("OPCODE\\s*=\\s*(.+?)\\s*$");
//
//    /** "REQUEST SENT" optionally followed by "[AMOUNT=00010000]" */
//    public static final Pattern REQUEST_AMOUNT = Pattern.compile("REQUEST SENT(?:\\s*\\[AMOUNT=(\\d+)])?");
//
//    /** "RESPONSE RECEIVED [FUNCTION ID=B,  TXN SN NO=6437]" */
//    public static final Pattern RESPONSE_RECEIVED =
//            Pattern.compile("RESPONSE RECEIVED\\s*\\[FUNCTION ID=([^,]+?),\\s*TXN SN NO=([^\\]]+)]");
//
//    // ---- Cash dispenser telemetry ------------------------------------------
//    /** "NOTES PRESENTED 0,0,20,0,0,0,0" */
//    public static final Pattern NOTES_PRESENTED = Pattern.compile("NOTES PRESENTED\\s+([\\d,]+)");
//    public static final Pattern NOTES_TAKEN     = Pattern.compile("NOTES TAKEN");
//    public static final Pattern NOTES_STACKED   = Pattern.compile("NOTES STACKED");
//
//    /** Cassette breakdown rows. */
//    public static final Pattern CASH_TOTAL_HEADER = Pattern.compile("^\\s*CASH TOTAL\\b");
//    public static final Pattern DISPENSED_ROW     = Pattern.compile("^\\s*\\+?DISPENSED\\b");
//    public static final Pattern REJECTED_ROW      = Pattern.compile("^\\s*\\+?REJECTED\\b");
//    public static final Pattern REMAINING_ROW     = Pattern.compile("^\\s*\\+?REMAINING\\b");
//    public static final Pattern CASSETTE_NUMBER   = Pattern.compile("\\d+");
//
//    // ---- Diagnostic trailer ------------------------------------------------
//    public static final Pattern ERROR_SEVERITY    = Pattern.compile("Error Severity\\s*:\\s*(.+?)\\s*$");
//    public static final Pattern DIAGNOSTIC_STATUS = Pattern.compile("Diagnostic Status\\s*:\\s*(.+?)\\s*$");
//
//    // ---- Customer outcome markers ------------------------------------------
//    public static final Pattern CUSTOMER_CANCELLED = Pattern.compile("CUSTOMER CANCELLED");
//    public static final Pattern CUSTOMER_TIMEOUT   = Pattern.compile("CUSTOMER TIMEOUT");
//
//    // ---- Receipt body fields -----------------------------------------------
//    /**
//     * The receipt's identifying line:  " 02-APR-2025      06:33   EFNJ000389010 ".
//     * Time may be HH:MI or HH:MI:SS. The third group is the ATM ID candidate;
//     * the parser applies an additional prefix check (configurable) to confirm.
//     */
//    public static final Pattern RECEIPT_DATE_TIME_ATM =
//            Pattern.compile("(\\d{2}-[A-Z]{3}-\\d{4})\\s+(\\d{2}:\\d{2}(?::\\d{2})?)\\s+(\\S+)");
//
//    public static final Pattern RECEIPT_CARD_NUMBER = Pattern.compile("^CARD NUMBER\\s+(\\S+)");
//    public static final Pattern RECEIPT_TXN_NO      = Pattern.compile("^TXN NO\\s*(.*)$");
//    public static final Pattern RECEIPT_REFERENCE   = Pattern.compile("^REFERENCE NO\\s*(.*)$");
//    public static final Pattern RECEIPT_RESPONSE    = Pattern.compile("^RESPONSE CODE\\s*(.*)$");
//
//    public static final Pattern RECEIPT_WITHDRAWAL =
//            Pattern.compile("WITHDRAWAL\\s+RS\\.\\s+([\\d.]+)");
//    public static final Pattern RECEIPT_BAL_INQUIRY =
//            Pattern.compile("BAL\\.INQUIRY\\s+(\\S+)");
//    public static final Pattern RECEIPT_FROM_AC =
//            Pattern.compile("FROM\\s+A/C\\s+(\\S+)");
//    public static final Pattern RECEIPT_MOD_BAL =
//            Pattern.compile("\\*?MOD\\s+BAL\\s+RS\\.\\s+([\\d.]+)");
//    public static final Pattern RECEIPT_AVAIL_BAL =
//            Pattern.compile("\\*?AVAIL\\s+BAL\\s+RS\\.\\s+([\\d.]+)");
//
//    // ---- Failure / outcome labels in receipt body --------------------------
//    public static final Pattern RECEIPT_UNABLE_TO_PROCESS =
//            Pattern.compile("UNABLE TO PROCESS|SORRY UNABLE TO PROCESS");
//    public static final Pattern RECEIPT_UNABLE_TO_DISPENSE =
//            Pattern.compile("UNABLE TO DISPENSE");
//
//    // ---- Admin / machine subtotal markers ----------------------------------
//    public static final Pattern ADMIN_SUBTOTAL = Pattern.compile("TXN TYPE: MACHINE SUBTOTAL");
//    public static final Pattern ADMIN_BALANCE  = Pattern.compile("ADMIN BALANCE");
//    public static final Pattern BGL_BALANCE    = Pattern.compile("BGL BALANCE\\s+RS\\.\\s*([\\d.]+)");
//
//    /** Bare account-type lines that appear inside failure receipts. */
//    public static final Set<String> ACCOUNT_TYPE_LITERALS = Set.of(
//            "SAVINGS", "CHECKING", "DEFAULT", "CREDIT CARD",
//            "*SAVINGS", "*CHECKING", "*DEFAULT", "*CREDIT CARD"
//    );
//}
