package com.jpb.reconciliation.reconciliation.hyosung.parser;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Central registry of regex patterns used across the loader.
 *
 * <p>All patterns are compiled once at class-load time. Centralising them
 * here makes it easy to add tests, reuse them in the reader, or override
 * them for variant vendor formats without touching parsing logic.
 *
 * <h3>Sections</h3>
 * <ol>
 *   <li><b>Frame</b> - the per-line {@code [NNNNNN][MM/DD/YYYY HH:MM:SS]} envelope.</li>
 *   <li><b>Boundaries</b> - block start/end markers (used by the reader).</li>
 *   <li><b>Event lines</b> - structured events inside a transaction block.</li>
 *   <li><b>JOURNAL DATA</b> - indented receipt body fields.</li>
 *   <li><b>[TRANSACTION RECORD]</b> - structured key/value section.</li>
 *   <li><b>Notes Dispensed</b> - denomination breakdown after dispense.</li>
 * </ol>
 */
public final class EjPatterns {

    private EjPatterns() {}

    // =========================================================================
    // 1. FRAME PATTERN  (every Hyosung EJ event line starts with this)
    // =========================================================================

    /**
     * The standard line frame:  "[NNNNNN][MM/DD/YYYY HH:MM:SS]<event-text>".
     * Group 1 = sequence number (6 digits), 2 = date, 3 = time, 4 = body.
     */
    public static final Pattern LINE_FRAME =
            Pattern.compile("^\\[(\\d{6})\\]\\[(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}:\\d{2}:\\d{2})\\](.*)$");

    // =========================================================================
    // 2. BOUNDARY MARKERS  (matched against the body of a framed line)
    // =========================================================================

    /** "TRANSACTION START" body. */
    public static final Pattern TXN_START          = Pattern.compile("^TRANSACTION START$");

    /** "CARDLESS TRANSACTION START" body (YONO / cardless flow). */
    public static final Pattern CARDLESS_TXN_START = Pattern.compile("^CARDLESS TRANSACTION START$");

    /** "TRANSACTION END" body. */
    public static final Pattern TXN_END            = Pattern.compile("^TRANSACTION END$");

    /** EJ file boundary markers (logged once at the top/bottom of each daily file). */
    public static final Pattern EJ_FILE_START_MARKER = Pattern.compile("^.*Electronic Journal$");
    public static final Pattern EJ_FILE_END_MARKER   = Pattern.compile("^.*Electronic Journal END$");

    // =========================================================================
    // 3. EVENT LINES (body of a [NNNNNN][...] line - parsed in main loop)
    // =========================================================================

    /** "CARD NUMBER 524272XXXXXX3845" - top-level card-reader event. */
    public static final Pattern EV_CARD_NUMBER =
            Pattern.compile("^CARD NUMBER\\s+(\\S+)");

    /** "EMV FINAL APP SELECTION SUCCESS: A0000000041010(Debit Mastercard)" */
    public static final Pattern EV_EMV_APP =
            Pattern.compile("^EMV FINAL APP SELECTION (?:SUCCESS|FAIL):\\s*([0-9A-Fa-f]+)\\((.+?)\\)");

    /** "ENTERED AMOUNT : [50000]" */
    public static final Pattern EV_ENTERED_AMOUNT =
            Pattern.compile("^ENTERED AMOUNT\\s*:\\s*\\[(\\d+)\\]");

    public static final Pattern EV_PIN_ENTERED      = Pattern.compile("^PIN ENTERED$");
    public static final Pattern EV_DISPENSE_DONE    = Pattern.compile("^DISPENSE COMPLETE$");
    public static final Pattern EV_CASH_PRESENTED   = Pattern.compile("^CASH PRESENTED$");
    public static final Pattern EV_CASH_TAKEN       = Pattern.compile("^CASH TAKEN$");
    public static final Pattern EV_CASH_TIMEOUT     = Pattern.compile("^CASH TAKEN AFTER TIMEOUT$");
    public static final Pattern EV_CASH_NOT_TAKEN   = Pattern.compile("^Cash Not Taken\\b.*");
    public static final Pattern EV_CARD_EJECTED     = Pattern.compile("^CARD EJECTED$");
    public static final Pattern EV_CARD_TAKEN       = Pattern.compile("^CARD TAKEN$");
    public static final Pattern EV_TXN_TIMEOUT      = Pattern.compile("^TRANSACTION TIMEOUT$");
    public static final Pattern EV_HOST_TIMEOUT     = Pattern.compile("^HOST TIMEOUT$");

    /** "TRANSACTION REQUESTING: OPcode[AB     A]" - keep internal whitespace. */
    public static final Pattern EV_TXN_REQUESTING =
            Pattern.compile("^TRANSACTION REQUESTING:\\s*OPcode\\[(.*?)\\]");

    /** "TRANSACTION REPLIED: FID[5] NEXT[558]" */
    public static final Pattern EV_TXN_REPLIED =
            Pattern.compile("^TRANSACTION REPLIED:\\s*FID\\[(.+?)\\]\\s*NEXT\\[(.+?)\\]");

    /** "LTSERNO [1993]" */
    public static final Pattern EV_LTSERNO =
            Pattern.compile("^LTSERNO\\s*\\[(\\d+)\\]");

    /** "LTDISPNOTES1COUNT [00000]" through "LTDISPNOTES7COUNT [00000]" */
    public static final Pattern EV_LTDISPNOTES =
            Pattern.compile("^LTDISPNOTES(\\d)COUNT\\s*\\[(\\d+)\\]");

    /** "JOURNAL DATA" - opens a multi-line indented receipt body. */
    public static final Pattern EV_JOURNAL_DATA = Pattern.compile("^JOURNAL DATA$");

    /** "TRANSACTION DATA (COMPLETED)" - usually followed by [TRANSACTION RECORD]. */
    public static final Pattern EV_TXN_DATA_COMPLETED =
            Pattern.compile("^TRANSACTION DATA \\(COMPLETED\\)$");

    /** "Notes Dispensed:" - opens the type-A..G breakdown sub-block. */
    public static final Pattern EV_NOTES_DISPENSED_HDR =
            Pattern.compile("^Notes Dispensed:$");

    // =========================================================================
    // 4. JOURNAL DATA (indented receipt body, no [NNN][...] frame)
    // =========================================================================

    /**
     * The receipt's identifying line:
     *   "    05-APR-2025      02:51   T1BS000949173"
     * Time may be HH:MI or HH:MI:SS.
     *
     * <p>Anchored at start-of-line (with optional indent) so a stray date-like
     * substring elsewhere on the line cannot match. The trailing token must
     * not contain whitespace - it's the ATM identifier.
     */
    public static final Pattern J_DATE_TIME_ATM =
            Pattern.compile("^\\s*(\\d{2}-[A-Z]{3}-\\d{4})\\s+(\\d{2}:\\d{2}(?::\\d{2})?)\\s+(\\S+)");

    public static final Pattern J_CARD_NUMBER     = Pattern.compile("^CARD NUMBER\\s+(\\S+)");
    /** YONO variant: "ACCOUNT NUMBER : XXXXXXXXXXXXX3433" */
    public static final Pattern J_ACCOUNT_NUMBER  = Pattern.compile("^ACCOUNT NUMBER\\s*:\\s*(\\S+)");

    public static final Pattern J_TXN_NO          = Pattern.compile("^TXN NO\\s*(.*)$");
    /** YONO variant: "TRANSACTION NO       2133" */
    public static final Pattern J_TXN_NO_YONO     = Pattern.compile("^TRANSACTION NO\\s+(.*)$");

    public static final Pattern J_REF_NO          = Pattern.compile("^REFERENCE NO\\s*(.*)$");
    /** YONO variant: "REFERENCE NUMBER     464626" */
    public static final Pattern J_REF_NO_YONO     = Pattern.compile("^REFERENCE NUMBER\\s*(.*)$");

    public static final Pattern J_RESPONSE_CODE   = Pattern.compile("^RESPONSE CODE\\s*(.*)$");

    public static final Pattern J_BAL_INQUIRY     = Pattern.compile("^BAL\\.INQUIRY\\s+(\\S+)");
    public static final Pattern J_WITHDRAWAL      = Pattern.compile("^WITHDRAWAL\\s+RS\\.\\s+([\\d.]+)");
    /** YONO variant: "TRANSACTION AMOUNT           3000.00" */
    public static final Pattern J_TXN_AMOUNT      = Pattern.compile("^TRANSACTION AMOUNT\\s+([\\d.]+)");
    public static final Pattern J_FROM_AC         = Pattern.compile("^FROM\\s+A/?C\\s+(\\S+)");
    public static final Pattern J_MOD_BAL         = Pattern.compile("^[*#]?MOD\\s+BAL\\s+RS\\.\\s*([\\d.]+)");
    public static final Pattern J_AVAIL_BAL       = Pattern.compile("^[*#]?AVAIL\\s+BAL\\s+RS\\.\\s*([\\d.]+)");

    /** Bare account-type lines that appear inside failure receipts. */
    public static final Set<String> ACCOUNT_TYPE_LITERALS;
    static {
        Set<String> s = new HashSet<String>(Arrays.asList(
                "SAVINGS", "CHECKING", "DEFAULT", "CREDIT CARD",
                "*SAVINGS", "*CHECKING", "*DEFAULT", "*CREDIT CARD"
        ));
        ACCOUNT_TYPE_LITERALS = Collections.unmodifiableSet(s);
    }

    public static final Pattern J_UNABLE_TO_PROCESS =
            Pattern.compile("UNABLE TO PROCESS");
    public static final Pattern J_DECLINED_INSUFFICIENT =
            Pattern.compile("TRANSACTION DECLINED DUE TO INSUFFICIENT BALANCE");
    /** Admin / cassette balance receipt header. */
    public static final Pattern J_ADMIN_SUBTOTAL =
            Pattern.compile("^TXN TYPE:\\s*MACHINE SUBTOTAL$");

    // =========================================================================
    // 5. [TRANSACTION RECORD] - keyed lines until blank
    // =========================================================================

    /** Marker that opens the [TRANSACTION RECORD] sub-block. */
    public static final Pattern TR_HEADER = Pattern.compile("^\\[TRANSACTION RECORD\\]$");

    public static final Pattern TR_OPCODE       = Pattern.compile("^OPCode\\s*\\[(.*?)\\]");
    public static final Pattern TR_FUNCTION_ID  = Pattern.compile("^Function ID\\s*\\[(.*?)\\]");
    public static final Pattern TR_AMOUNT       = Pattern.compile("^Amount\\s*\\[(\\d+)\\]");
    public static final Pattern TR_TRANS_SEQ    = Pattern.compile("^Trans SEQ Number\\s*\\[(\\d+)\\]");
    public static final Pattern TR_DENOMINATION = Pattern.compile("^Denomination\\s*\\[(.+?)\\]");
    public static final Pattern TR_REQUEST_CNT  = Pattern.compile("^Request Count\\s*\\[(.+?)\\]");
    public static final Pattern TR_PICKUP_CNT   = Pattern.compile("^Pickup Count\\s*\\[(.+?)\\]");
    public static final Pattern TR_DISPENSE_CNT = Pattern.compile("^Dispense Count\\s*\\[(.+?)\\]");
    public static final Pattern TR_REMAIN_CNT   = Pattern.compile("^Remain Count\\s*\\[(.+?)\\]");
    public static final Pattern TR_REJECT_CNT   = Pattern.compile("^Reject Count\\s*\\[(.+?)\\]");

    // =========================================================================
    // 6. Notes Dispensed:
    //    "TypeA(INR50)     = 0"
    // =========================================================================

    public static final Pattern ND_TYPE =
            Pattern.compile("^Type([A-G])\\(([^)]+)\\)\\s*=\\s*(\\d+)");
}
