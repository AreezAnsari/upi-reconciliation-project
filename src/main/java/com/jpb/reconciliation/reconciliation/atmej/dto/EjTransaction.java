package com.jpb.reconciliation.reconciliation.atmej.dto;

import java.math.BigDecimal; 
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Single parsed ATM EJ transaction record. Immutable; create via {@link Builder}.
 *
 * <p>Field naming and types mirror the {@code atm_ej_transaction} Oracle table.
 * Numeric amounts use {@link BigDecimal} to avoid float precision loss; counts
 * use {@link Long}. Boolean flags are stored as {@code Y}/{@code N} chars in the
 * database but exposed as {@code boolean} primitives in Java.
 */
public final class EjTransaction {

    /** Distinct outcomes detected by the parser. */
    public enum Status {
        SUCCESS, FAILED, CUSTOMER_CANCELLED, CUSTOMER_TIMEOUT,
        INCOMPLETE, NO_CARD, ADMIN, UNKNOWN
    }

    /** Distinct types detected by the parser. */
    public enum Type {
        WITHDRAWAL, BALANCE_INQUIRY, FAILED, ADMIN_SUBTOTAL, OTHER
    }

    // ---- source identity ---------------------------------------------------
    private final String fileName;
    private final long fileLineStart;
    private final long fileLineEnd;

    // ---- sequence header ---------------------------------------------------
    private final String sequenceNumber;
    private final String logDate;
    private final String logTime;
    private final LocalDateTime transactionDateTime;

    // ---- ATM / receipt -----------------------------------------------------
    private final String atmId;
    private final String receiptDate;
    private final String receiptTime;

    // ---- card --------------------------------------------------------------
    private final String cardNumber;
    private final String cardNumberRaw;

    // ---- switch ------------------------------------------------------------
    private final String txnNo;
    private final String referenceNo;
    private final String responseCode;
    private final String functionId;
    private final String txnSerialNo;

    // ---- classification ----------------------------------------------------
    private final Type    transactionType;
    private final Status  transactionStatus;
    private final String  accountType;
    private final String  fromAccount;

    // ---- amounts -----------------------------------------------------------
    private final BigDecimal requestAmount;
    private final BigDecimal amountEntered;
    private final BigDecimal withdrawalAmount;
    private final BigDecimal modBalance;
    private final BigDecimal availBalance;

    // ---- protocol ----------------------------------------------------------
    private final String opcode;

    // ---- cash telemetry ----------------------------------------------------
    private final String  notesPresented;
    private final boolean notesTaken;
    private final boolean notesStacked;
    private final Long    dispenseCount;
    private final Long    rejectedCount;
    private final Long    remainingCount;

    // ---- diagnostics -------------------------------------------------------
    private final String errorSeverity;
    private final String diagnosticStatus;

    // ---- behaviour flags ---------------------------------------------------
    private final boolean pinEntered;
    private final boolean cardInserted;
    private final boolean admin;

    // ---- audit -------------------------------------------------------------
    private final String rawTransactionBlock;
    private final String loadBatchId;

    private EjTransaction(Builder b) {
        this.fileName            = b.fileName;
        this.fileLineStart       = b.fileLineStart;
        this.fileLineEnd         = b.fileLineEnd;
        this.sequenceNumber      = b.sequenceNumber;
        this.logDate             = b.logDate;
        this.logTime             = b.logTime;
        this.transactionDateTime = b.transactionDateTime;
        this.atmId               = b.atmId;
        this.receiptDate         = b.receiptDate;
        this.receiptTime         = b.receiptTime;
        this.cardNumber          = b.cardNumber;
        this.cardNumberRaw       = b.cardNumberRaw;
        this.txnNo               = b.txnNo;
        this.referenceNo         = b.referenceNo;
        this.responseCode        = b.responseCode;
        this.functionId          = b.functionId;
        this.txnSerialNo         = b.txnSerialNo;
        this.transactionType     = b.transactionType   != null ? b.transactionType   : Type.OTHER;
        this.transactionStatus   = b.transactionStatus != null ? b.transactionStatus : Status.UNKNOWN;
        this.accountType         = b.accountType;
        this.fromAccount         = b.fromAccount;
        this.requestAmount       = b.requestAmount;
        this.amountEntered       = b.amountEntered;
        this.withdrawalAmount    = b.withdrawalAmount;
        this.modBalance          = b.modBalance;
        this.availBalance        = b.availBalance;
        this.opcode              = b.opcode;
        this.notesPresented      = b.notesPresented;
        this.notesTaken          = b.notesTaken;
        this.notesStacked        = b.notesStacked;
        this.dispenseCount       = b.dispenseCount;
        this.rejectedCount       = b.rejectedCount;
        this.remainingCount      = b.remainingCount;
        this.errorSeverity       = b.errorSeverity;
        this.diagnosticStatus    = b.diagnosticStatus;
        this.pinEntered          = b.pinEntered;
        this.cardInserted        = b.cardInserted;
        this.admin               = b.admin;
        this.rawTransactionBlock = Objects.requireNonNull(b.rawTransactionBlock,
                "raw transaction block must always be preserved for audit");
        this.loadBatchId         = b.loadBatchId;
    }

    public static Builder builder() { return new Builder(); }

    // -------- accessors -----------------------------------------------------
    public String  getFileName()             { return fileName; }
    public long    getFileLineStart()        { return fileLineStart; }
    public long    getFileLineEnd()          { return fileLineEnd; }
    public String  getSequenceNumber()       { return sequenceNumber; }
    public String  getLogDate()              { return logDate; }
    public String  getLogTime()              { return logTime; }
    public LocalDateTime getTransactionDateTime() { return transactionDateTime; }
    public String  getAtmId()                { return atmId; }
    public String  getReceiptDate()          { return receiptDate; }
    public String  getReceiptTime()          { return receiptTime; }
    public String  getCardNumber()           { return cardNumber; }
    public String  getCardNumberRaw()        { return cardNumberRaw; }
    public String  getTxnNo()                { return txnNo; }
    public String  getReferenceNo()          { return referenceNo; }
    public String  getResponseCode()         { return responseCode; }
    public String  getFunctionId()           { return functionId; }
    public String  getTxnSerialNo()          { return txnSerialNo; }
    public Type    getTransactionType()      { return transactionType; }
    public Status  getTransactionStatus()    { return transactionStatus; }
    public String  getAccountType()          { return accountType; }
    public String  getFromAccount()          { return fromAccount; }
    public BigDecimal getRequestAmount()     { return requestAmount; }
    public BigDecimal getAmountEntered()     { return amountEntered; }
    public BigDecimal getWithdrawalAmount()  { return withdrawalAmount; }
    public BigDecimal getModBalance()        { return modBalance; }
    public BigDecimal getAvailBalance()      { return availBalance; }
    public String  getOpcode()               { return opcode; }
    public String  getNotesPresented()       { return notesPresented; }
    public boolean isNotesTaken()            { return notesTaken; }
    public boolean isNotesStacked()          { return notesStacked; }
    public Long    getDispenseCount()        { return dispenseCount; }
    public Long    getRejectedCount()        { return rejectedCount; }
    public Long    getRemainingCount()       { return remainingCount; }
    public String  getErrorSeverity()        { return errorSeverity; }
    public String  getDiagnosticStatus()     { return diagnosticStatus; }
    public boolean isPinEntered()            { return pinEntered; }
    public boolean isCardInserted()          { return cardInserted; }
    public boolean isAdmin()                 { return admin; }
    public String  getRawTransactionBlock()  { return rawTransactionBlock; }
    public String  getLoadBatchId()          { return loadBatchId; }

    @Override
    public String toString() {
        return "EjTransaction{file=" + fileName
                + ", lines=" + fileLineStart + "-" + fileLineEnd
                + ", seq=" + sequenceNumber
                + ", type=" + transactionType + "/" + transactionStatus
                + ", atm=" + atmId
                + ", txnNo=" + txnNo + "}";
    }

    // ============================ Builder ===================================
    public static final class Builder {
        private String  fileName;
        private long    fileLineStart;
        private long    fileLineEnd;
        private String  sequenceNumber;
        private String  logDate;
        private String  logTime;
        private LocalDateTime transactionDateTime;
        private String  atmId;
        private String  receiptDate;
        private String  receiptTime;
        private String  cardNumber;
        private String  cardNumberRaw;
        private String  txnNo;
        private String  referenceNo;
        private String  responseCode;
        private String  functionId;
        private String  txnSerialNo;
        private Type    transactionType;
        private Status  transactionStatus;
        private String  accountType;
        private String  fromAccount;
        private BigDecimal requestAmount;
        private BigDecimal amountEntered;
        private BigDecimal withdrawalAmount;
        private BigDecimal modBalance;
        private BigDecimal availBalance;
        private String  opcode;
        private String  notesPresented;
        private boolean notesTaken;
        private boolean notesStacked;
        private Long    dispenseCount;
        private Long    rejectedCount;
        private Long    remainingCount;
        private String  errorSeverity;
        private String  diagnosticStatus;
        private boolean pinEntered;
        private boolean cardInserted;
        private boolean admin;
        private String  rawTransactionBlock;
        private String  loadBatchId;

        public Builder fileName(String v)             { this.fileName = v; return this; }
        public Builder fileLineStart(long v)          { this.fileLineStart = v; return this; }
        public Builder fileLineEnd(long v)            { this.fileLineEnd = v; return this; }
        public Builder sequenceNumber(String v)       { this.sequenceNumber = v; return this; }
        public Builder logDate(String v)              { this.logDate = v; return this; }
        public Builder logTime(String v)              { this.logTime = v; return this; }
        public Builder transactionDateTime(LocalDateTime v) { this.transactionDateTime = v; return this; }
        public Builder atmId(String v)                { this.atmId = v; return this; }
        public Builder receiptDate(String v)          { this.receiptDate = v; return this; }
        public Builder receiptTime(String v)          { this.receiptTime = v; return this; }
        public Builder cardNumber(String v)           { this.cardNumber = v; return this; }
        public Builder cardNumberRaw(String v)        { this.cardNumberRaw = v; return this; }
        public Builder txnNo(String v)                { this.txnNo = v; return this; }
        public Builder referenceNo(String v)          { this.referenceNo = v; return this; }
        public Builder responseCode(String v)         { this.responseCode = v; return this; }
        public Builder functionId(String v)           { this.functionId = v; return this; }
        public Builder txnSerialNo(String v)          { this.txnSerialNo = v; return this; }
        public Builder transactionType(Type v)        { this.transactionType = v; return this; }
        public Builder transactionStatus(Status v)    { this.transactionStatus = v; return this; }
        public Builder accountType(String v)          { this.accountType = v; return this; }
        public Builder fromAccount(String v)          { this.fromAccount = v; return this; }
        public Builder requestAmount(BigDecimal v)    { this.requestAmount = v; return this; }
        public Builder amountEntered(BigDecimal v)    { this.amountEntered = v; return this; }
        public Builder withdrawalAmount(BigDecimal v) { this.withdrawalAmount = v; return this; }
        public Builder modBalance(BigDecimal v)       { this.modBalance = v; return this; }
        public Builder availBalance(BigDecimal v)     { this.availBalance = v; return this; }
        public Builder opcode(String v)               { this.opcode = v; return this; }
        public Builder notesPresented(String v)       { this.notesPresented = v; return this; }
        public Builder notesTaken(boolean v)          { this.notesTaken = v; return this; }
        public Builder notesStacked(boolean v)        { this.notesStacked = v; return this; }
        public Builder dispenseCount(Long v)          { this.dispenseCount = v; return this; }
        public Builder rejectedCount(Long v)          { this.rejectedCount = v; return this; }
        public Builder remainingCount(Long v)         { this.remainingCount = v; return this; }
        public Builder errorSeverity(String v)        { this.errorSeverity = v; return this; }
        public Builder diagnosticStatus(String v)     { this.diagnosticStatus = v; return this; }
        public Builder pinEntered(boolean v)          { this.pinEntered = v; return this; }
        public Builder cardInserted(boolean v)        { this.cardInserted = v; return this; }
        public Builder admin(boolean v)               { this.admin = v; return this; }
        public Builder rawTransactionBlock(String v)  { this.rawTransactionBlock = v; return this; }
        public Builder loadBatchId(String v)          { this.loadBatchId = v; return this; }

        public EjTransaction build() {
            return new EjTransaction(this);
        }
    }
}
