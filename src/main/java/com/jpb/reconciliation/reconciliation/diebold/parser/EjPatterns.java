package com.jpb.reconciliation.reconciliation.diebold.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compiled regex patterns for Diebold EJ log parsing.
 *
 * <p>Diebold block header format:
 * <pre>XNNNNNNDDMMYYHHMMSS</pre>
 * where X = block-type prefix (1=receipt, 4=card-event, 0=error/failed),
 * NNNNNN = 6-digit sequence, DDMMYY = date, HHMMSS = time.
 *
 * <p>Blocks are separated by lines of asterisks (*).
 */
public final class EjPatterns {

    private EjPatterns() {}

    // ── block structure ───────────────────────────────────────────────────
    /** Header line: XNNNNNNDDMMYYHHMMSS (19 chars, all digits) */
    public static final Pattern BLOCK_HEADER =
            Pattern.compile("^([014])([0-9]{6})([0-9]{6})([0-9]{6})$");

    /** Block separator line */
    public static final Pattern SEPARATOR =
            Pattern.compile("^\\*{10,}\\s*$");

    // ── receipt body patterns ─────────────────────────────────────────────
    /** DATE TIME ATM ID header line (optional leading spaces) */
    public static final Pattern DATE_TIME_ATM_HDR =
            Pattern.compile("^\\s*DATE\\s+TIME\\s+ATM\\s+ID\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * ATM ID data line: " DD-MON-YYYY  HH:MM  ATMID"
     * Groups: 1=date, 2=time, 3=atm_id
     */
    public static final Pattern DATE_TIME_ATM_DATA =
    		Pattern.compile(
    				"^\\s*(\\d{1,2}-[A-Z]{3}-\\d{4})\\s+(\\d{2}:\\d{2}(?::\\d{2})?)\\s+([A-Z0-9]+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** CARD :xxxxxx  or  CARD NO xxxxxx  or  CARD NUMBER xxxxxx */
    public static final Pattern CARD_NUMBER =
            Pattern.compile(
                    "^\\s*CARD(?:\\s+NO\\.?|\\s+NUMBER)?\\s*[:\\s]\\s*([0-9X*]{10,20})\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * TXN number — several variants seen in the wild:
     * "TXN.NO: 1234", "TXN. NO 1234", "TXN NO. 1234", "TRANSACTION NO : 1234"
     * Group 1 = txn number
     */
    public static final Pattern TXN_NO =
            Pattern.compile(
                    "^\\s*(?:TRANSACTION\\s+NO|TXN\\.?\\s*NO\\.?)\\s*[:\\s]\\s*(\\d+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** REFERENCE NO. 432708008212 or REFERENCE NUMBER : 899911 */
    public static final Pattern REFERENCE_NO =
            Pattern.compile(
                    "^\\s*REFERENCE\\s+(?:NO\\.?|NUMBER)\\s*[:\\s]\\s*(\\S+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** RESPONSE CODE 000  or  RESPONSE CODE : 000 */
    public static final Pattern RESPONSE_CODE =
            Pattern.compile(
                    "^\\s*RESPONSE\\s+CODE\\s*[:\\s]\\s*(\\S+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** WITHDRAWAL RS.1000.00  (optional spaces around RS.) */
    public static final Pattern WITHDRAWAL =
            Pattern.compile(
                    "^\\s*WITHDRAWAL\\s+RS\\.?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** TRANSACTION AMOUNT : 2000.00 (YONO cardless) */
    public static final Pattern TXN_AMOUNT =
            Pattern.compile(
                    "^\\s*TRANSACTION\\s+AMOUNT\\s*[:\\s]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** BAL. INQUIRY  SAVINGS / BAL. INQUIRY  DEFAULT */
    public static final Pattern BAL_INQUIRY =
            Pattern.compile(
                    "^\\s*BAL\\.?\\s+INQUIRY\\s+(.+)$",
                    Pattern.CASE_INSENSITIVE);

    /** FROM A/C XXXXX  or  FROM A/C DEFAULT  or  FROM A/C SAVINGS */
    public static final Pattern FROM_AC =
            Pattern.compile(
                    "^\\s*FROM\\s+A/C\\s+(\\S+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** MOD  BAL  RS.978.19 */
    public static final Pattern MOD_BAL =
            Pattern.compile(
                    "^\\s*MOD\\s+BAL\\s+RS\\.?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** AVAIL BAL  RS.978.19  or  AVAIL BAL  RS.978.19 (various spacing + optional semicolon) */
    public static final Pattern AVAIL_BAL =
    		Pattern.compile(
    				"^\\s*AVAIL\\s+BAL[e;]*\\s*RS\\.?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** ACCOUNT NUMBER : XXXXXXXXXXXXX8657 (YONO) */
    public static final Pattern ACCOUNT_NUMBER =
            Pattern.compile(
                    "^\\s*ACCOUNT\\s+NUMBER\\s*[:\\s]\\s*(\\S+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** YONO WITHDRAWAL TRANSACTION IS SUCCESSFUL */
    public static final Pattern YONO_SUCCESS =
            Pattern.compile(".*YONO\\s+WITHDRAWAL.*", Pattern.CASE_INSENSITIVE);

    /** UNABLE TO PROCESS */
    public static final Pattern UNABLE_TO_PROCESS =
            Pattern.compile(".*UNABLE\\s+TO\\s+PROCESS.*", Pattern.CASE_INSENSITIVE);

    // ── error block (type 0) patterns ──────────────────────────────────────
    /** Error code line: "000SV77:77:02:01 03/10/25 09:12:43"  group 1=code, 2=datetime */
    public static final Pattern ERROR_HDR =
            Pattern.compile(
                    "^([0-9A-F]{3}[A-Z0-9:]+)\\s+(\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}).*$");

    /** SERIAL # 2926 */
    public static final Pattern SERIAL_NO =
            Pattern.compile(
                    "^\\s*SERIAL\\s*#\\s*(\\d+).*$",
                    Pattern.CASE_INSENSITIVE);

    /** Known error descriptions */
    public static final Pattern PROCESSING_RESTRICTIONS =
            Pattern.compile(".*PROCESSING\\s+RESTRICTIONS.*", Pattern.CASE_INSENSITIVE);

    public static final Pattern CARD_DATA_ERROR =
            Pattern.compile(".*CARD\\s+DATA\\s+ERROR.*", Pattern.CASE_INSENSITIVE);

    public static final Pattern OUT_OF_SEQUENCE =
            Pattern.compile(".*OUT\\s+OF\\s+SEQUENCE.*", Pattern.CASE_INSENSITIVE);

    public static final Pattern IO_ERROR =
            Pattern.compile(".*I/O\\s+ERROR.*", Pattern.CASE_INSENSITIVE);

    // ── card event (type 4) patterns ──────────────────────────────────────
    /** CARD INSERTED (5446 70XX XXXX 3938) */
    public static final Pattern CARD_INSERTED =
            Pattern.compile(
                    "^\\s*CARD\\s+INSERTED\\s+\\(([^)]+)\\).*$",
                    Pattern.CASE_INSENSITIVE);
    
    public static final Pattern CARD_ACTION_FAILURE =
        Pattern.compile(".*CARD\\s+ACTION\\s+ANALYSIS\\s+FAILURE.*", Pattern.CASE_INSENSITIVE);

    public static final Pattern TRANSACTION_DECLINED =
        Pattern.compile(".*TRANSACTION\\s+DECLINED.*", Pattern.CASE_INSENSITIVE);

    // ── account type literals ─────────────────────────────────────────────
    public static final Set<String> ACCOUNT_TYPE_LITERALS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "SAVINGS", "DEFAULT", "CHECKING", "CREDIT CARD", "CREDIT",
                    "CURRENT", "SB", "CA")));
    
}