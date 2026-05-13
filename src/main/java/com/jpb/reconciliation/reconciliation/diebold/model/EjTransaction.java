package com.jpb.reconciliation.reconciliation.diebold.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Parsed representation of one Diebold EJ transaction block.
 * Use {@link Builder} to construct instances.
 */
public final class EjTransaction {

    // ── enums ────────────────────────────────────────────────────────────────
    public enum Type {
        WITHDRAWAL, BALANCE_INQUIRY, CARDLESS_WITHDRAWAL,
        FAILED, INCOMPLETE, OTHER
    }

    public enum Status {
        SUCCESS, FAILED, UNABLE_TO_PROCESS, PROCESSING_RESTRICTIONS,
        CARD_DATA_ERROR, OUT_OF_SEQUENCE, IO_ERROR,
        INCOMPLETE, NO_CARD, MALFORMED, TRUNCATED, UNKNOWN
    }

    // ── fields ───────────────────────────────────────────────────────────────
    private final String        fileName;
    private final long          fileLineStart;
    private final long          fileLineEnd;

    // header
    private final String        blockTypePrefix;   // "1", "4", "0"
    private final String        sequenceNumber;    // 6-digit
    private final String        headerDate;        // DDMMYY from header
    private final String        headerTime;        // HHMMSS from header
    private final LocalDateTime headerDateTime;

    // receipt fields
    private final String        location;
    private final String        receiptDate;       // DD-MON-YYYY
    private final String        receiptTime;       // HH:MM
    private final String        atmId;
    private final String        cardNumber;        // masked PAN
    private final String        txnNo;
    private final String        referenceNo;
    private final String        responseCode;
    private final String        accountType;       // SAVINGS / DEFAULT etc.
    private final String        fromAccount;
    private final BigDecimal    withdrawalAmount;
    private final BigDecimal    transactionAmount; // YONO
    private final BigDecimal    modBalance;
    private final BigDecimal    availBalance;
    private final String        serialNo;          // SERIAL # from error blocks

    // flags
    private final boolean       unableToProcess;
    private final boolean       cardlessWithdrawal;  // YONO
    private final boolean       processingRestrictions;
    private final boolean       cardDataError;
    private final boolean       pinEntered;

    // error info (type 0 blocks)
    private final String        errorCode;
    private final String        errorDescription;

    // classification
    private final Type          transactionType;
    private final Status        transactionStatus;

    // audit
    private final String        rawTransactionBlock;
    private final String        loadBatchId;

    private EjTransaction(Builder b) {
        this.fileName               = b.fileName;
        this.fileLineStart          = b.fileLineStart;
        this.fileLineEnd            = b.fileLineEnd;
        this.blockTypePrefix        = b.blockTypePrefix;
        this.sequenceNumber         = b.sequenceNumber;
        this.headerDate             = b.headerDate;
        this.headerTime             = b.headerTime;
        this.headerDateTime         = b.headerDateTime;
        this.location               = b.location;
        this.receiptDate            = b.receiptDate;
        this.receiptTime            = b.receiptTime;
        this.atmId                  = b.atmId;
        this.cardNumber             = b.cardNumber;
        this.txnNo                  = b.txnNo;
        this.referenceNo            = b.referenceNo;
        this.responseCode           = b.responseCode;
        this.accountType            = b.accountType;
        this.fromAccount            = b.fromAccount;
        this.withdrawalAmount       = b.withdrawalAmount;
        this.transactionAmount      = b.transactionAmount;
        this.modBalance             = b.modBalance;
        this.availBalance           = b.availBalance;
        this.serialNo               = b.serialNo;
        this.unableToProcess        = b.unableToProcess;
        this.cardlessWithdrawal     = b.cardlessWithdrawal;
        this.processingRestrictions = b.processingRestrictions;
        this.cardDataError          = b.cardDataError;
        this.pinEntered             = b.pinEntered;
        this.errorCode              = b.errorCode;
        this.errorDescription       = b.errorDescription;
        this.transactionType        = b.transactionType;
        this.transactionStatus      = b.transactionStatus;
        this.rawTransactionBlock    = b.rawTransactionBlock;
        this.loadBatchId            = b.loadBatchId;
    }

    public static Builder builder() { return new Builder(); }

    // ── getters ──────────────────────────────────────────────────────────────
    public String        getFileName()               { return fileName; }
    public long          getFileLineStart()           { return fileLineStart; }
    public long          getFileLineEnd()             { return fileLineEnd; }
    public String        getBlockTypePrefix()         { return blockTypePrefix; }
    public String        getSequenceNumber()          { return sequenceNumber; }
    public String        getHeaderDate()              { return headerDate; }
    public String        getHeaderTime()              { return headerTime; }
    public LocalDateTime getHeaderDateTime()          { return headerDateTime; }
    public String        getLocation()                { return location; }
    public String        getReceiptDate()             { return receiptDate; }
    public String        getReceiptTime()             { return receiptTime; }
    public String        getAtmId()                   { return atmId; }
    public String        getCardNumber()              { return cardNumber; }
    public String        getTxnNo()                   { return txnNo; }
    public String        getReferenceNo()             { return referenceNo; }
    public String        getResponseCode()            { return responseCode; }
    public String        getAccountType()             { return accountType; }
    public String        getFromAccount()             { return fromAccount; }
    public BigDecimal    getWithdrawalAmount()        { return withdrawalAmount; }
    public BigDecimal    getTransactionAmount()       { return transactionAmount; }
    public BigDecimal    getModBalance()              { return modBalance; }
    public BigDecimal    getAvailBalance()            { return availBalance; }
    public String        getSerialNo()                { return serialNo; }
    public boolean       isUnableToProcess()          { return unableToProcess; }
    public boolean       isCardlessWithdrawal()       { return cardlessWithdrawal; }
    public boolean       isProcessingRestrictions()   { return processingRestrictions; }
    public boolean       isCardDataError()            { return cardDataError; }
    public boolean       isPinEntered()               { return pinEntered; }
    public String        getErrorCode()               { return errorCode; }
    public String        getErrorDescription()        { return errorDescription; }
    public Type          getTransactionType()         { return transactionType; }
    public Status        getTransactionStatus()       { return transactionStatus; }
    public String        getRawTransactionBlock()     { return rawTransactionBlock; }
    public String        getLoadBatchId()             { return loadBatchId; }

    // ── builder ──────────────────────────────────────────────────────────────
    public static final class Builder {
        private String        fileName;
        private long          fileLineStart;
        private long          fileLineEnd;
        private String        blockTypePrefix;
        private String        sequenceNumber;
        private String        headerDate;
        private String        headerTime;
        private LocalDateTime headerDateTime;
        private String        location;
        private String        receiptDate;
        private String        receiptTime;
        private String        atmId;
        private String        cardNumber;
        private String        txnNo;
        private String        referenceNo;
        private String        responseCode;
        private String        accountType;
        private String        fromAccount;
        private BigDecimal    withdrawalAmount;
        private BigDecimal    transactionAmount;
        private BigDecimal    modBalance;
        private BigDecimal    availBalance;
        private String        serialNo;
        private boolean       unableToProcess;
        private boolean       cardlessWithdrawal;
        private boolean       processingRestrictions;
        private boolean       cardDataError;
        private boolean       pinEntered;
        private String        errorCode;
        private String        errorDescription;
        private Type          transactionType;
        private Status        transactionStatus;
        private String        rawTransactionBlock;
        private String        loadBatchId;

        public Builder fileName(String v)               { this.fileName = v; return this; }
        public Builder fileLineStart(long v)            { this.fileLineStart = v; return this; }
        public Builder fileLineEnd(long v)              { this.fileLineEnd = v; return this; }
        public Builder blockTypePrefix(String v)        { this.blockTypePrefix = v; return this; }
        public Builder sequenceNumber(String v)         { this.sequenceNumber = v; return this; }
        public Builder headerDate(String v)             { this.headerDate = v; return this; }
        public Builder headerTime(String v)             { this.headerTime = v; return this; }
        public Builder headerDateTime(LocalDateTime v)  { this.headerDateTime = v; return this; }
        public Builder location(String v)               { this.location = v; return this; }
        public Builder receiptDate(String v)            { this.receiptDate = v; return this; }
        public Builder receiptTime(String v)            { this.receiptTime = v; return this; }
        public Builder atmId(String v)                  { this.atmId = v; return this; }
        public Builder cardNumber(String v)             { this.cardNumber = v; return this; }
        public Builder txnNo(String v)                  { this.txnNo = v; return this; }
        public Builder referenceNo(String v)            { this.referenceNo = v; return this; }
        public Builder responseCode(String v)           { this.responseCode = v; return this; }
        public Builder accountType(String v)            { this.accountType = v; return this; }
        public Builder fromAccount(String v)            { this.fromAccount = v; return this; }
        public Builder withdrawalAmount(BigDecimal v)   { this.withdrawalAmount = v; return this; }
        public Builder transactionAmount(BigDecimal v)  { this.transactionAmount = v; return this; }
        public Builder modBalance(BigDecimal v)         { this.modBalance = v; return this; }
        public Builder availBalance(BigDecimal v)       { this.availBalance = v; return this; }
        public Builder serialNo(String v)               { this.serialNo = v; return this; }
        public Builder unableToProcess(boolean v)       { this.unableToProcess = v; return this; }
        public Builder cardlessWithdrawal(boolean v)    { this.cardlessWithdrawal = v; return this; }
        public Builder processingRestrictions(boolean v){ this.processingRestrictions = v; return this; }
        public Builder cardDataError(boolean v)         { this.cardDataError = v; return this; }
        public Builder pinEntered(boolean v)            { this.pinEntered = v; return this; }
        public Builder errorCode(String v)              { this.errorCode = v; return this; }
        public Builder errorDescription(String v)       { this.errorDescription = v; return this; }
        public Builder transactionType(Type v)          { this.transactionType = v; return this; }
        public Builder transactionStatus(Status v)      { this.transactionStatus = v; return this; }
        public Builder rawTransactionBlock(String v)    { this.rawTransactionBlock = v; return this; }
        public Builder loadBatchId(String v)            { this.loadBatchId = v; return this; }

        public EjTransaction build() { return new EjTransaction(this); }
    }
}