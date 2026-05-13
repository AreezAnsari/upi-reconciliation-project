package com.jpb.reconciliation.reconciliation.hyosung.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable DTO representing one parsed ATM transaction extracted from a
 * Hyosung NDC EJ file. Built via {@link Builder}.
 *
 * <p>Fields are deliberately wide so a single bad block never loses data we
 * already extracted. Numeric fields use {@link BigDecimal} for amounts and
 * {@code Long} for note counts; boolean event flags are exposed as primitive
 * {@code boolean} but stored as {@code Y}/{@code N} CHAR(1) in Oracle.
 *
 * <p>Field naming mirrors the {@code atm_ej_transaction} table - any new
 * field must be added in DDL, in the builder, in the parser's result
 * assembly, and in the JDBC bind() block. Nothing else.
 */
public final class EjTransaction {

    /** Distinct outcomes detected by the parser. Mirrored as a CHECK constraint in DDL. */
    public enum Status {
        SUCCESS,
        FAILED,
        UNABLE_TO_PROCESS,
        DECLINED_INSUFFICIENT,
        TIMEOUT,
        CASH_NOT_TAKEN,
        CASH_TAKEN_LATE,
        INCOMPLETE,
        NO_CARD,
        MALFORMED,
        TRUNCATED,
        ADMIN,
        UNKNOWN
    }

    /** Distinct types detected by the parser. Mirrored as a CHECK constraint in DDL. */
    public enum Type {
        WITHDRAWAL,
        BALANCE_INQUIRY,
        CARDLESS_WITHDRAWAL,
        ADMIN_SUBTOTAL,
        FAILED,
        INCOMPLETE,
        OTHER
    }

    // ---- source identity ---------------------------------------------------
    private final String fileName;
    private final long fileLineStart;
    private final long fileLineEnd;

    // ---- timestamps from the [NNNNNN][MM/DD/YYYY HH:MM:SS] frame -----------
    /** [NNNNNN] of the START line (e.g. "000002"). */
    private final String sequenceNoFirst;
    /** [NNNNNN] of the END line. */
    private final String sequenceNoLast;
    private final LocalDateTime firstEventTs;
    private final LocalDateTime lastEventTs;

    // ---- card / EMV --------------------------------------------------------
    private final String cardNumberRaw;
    private final String emvAid;
    private final String emvAppLabel;

    // ---- input -------------------------------------------------------------
    private final BigDecimal amountEntered;
    private final boolean    pinEntered;
    private final boolean    cardless;

    // ---- protocol / switch -------------------------------------------------
    private final String     opcode;
    private final String     functionId;
    private final BigDecimal amountRecorded;        // from [TRANSACTION RECORD] Amount
    private final String     transSeqNumber;        // from [TRANSACTION RECORD] Trans SEQ Number
    private final String     ltSerno;
    private final String     fidResponse;
    private final String     fidNext;
    private final Long       ltDispNotes1;
    private final Long       ltDispNotes2;
    private final Long       ltDispNotes3;
    private final Long       ltDispNotes4;
    private final Long       ltDispNotes5;
    private final Long       ltDispNotes6;
    private final Long       ltDispNotes7;

    // ---- cassette / dispenser telemetry from [TRANSACTION RECORD] ----------
    private final String denomination;
    private final String requestCount;
    private final String pickupCount;
    private final String dispenseCount;
    private final String remainCount;
    private final String rejectCount;

    // ---- 'Notes Dispensed:' breakdown --------------------------------------
    private final Long notesTypeA;
    private final Long notesTypeB;
    private final Long notesTypeC;
    private final Long notesTypeD;
    private final Long notesTypeE;
    private final Long notesTypeF;
    private final Long notesTypeG;

    // ---- in-block lifecycle flags ------------------------------------------
    private final boolean dispenseComplete;
    private final boolean cashPresented;
    private final boolean cashTaken;
    private final boolean cashTakenAfterTimeout;
    private final boolean cashNotTaken;
    private final boolean cardEjected;
    private final boolean cardTaken;
    private final boolean transactionTimeout;
    private final boolean hostTimeout;

    // ---- receipt body ------------------------------------------------------
    private final String receiptDate;
    private final String receiptTime;
    private final String atmId;
    private final String receiptCardNumber;
    private final String receiptAccountNumber;
    private final String txnNo;
    private final String referenceNo;
    private final String responseCode;
    private final String accountType;
    private final BigDecimal withdrawalAmount;
    private final BigDecimal transactionAmount;     // YONO variant
    private final String fromAccount;
    private final BigDecimal modBalance;
    private final BigDecimal availBalance;
    private final boolean unableToProcess;
    private final boolean declinedInsufficient;
    private final boolean adminSubtotal;

    // ---- classification ----------------------------------------------------
    private final Type   transactionType;
    private final Status transactionStatus;

    // ---- audit -------------------------------------------------------------
    private final String rawTransactionBlock;
    private final String loadBatchId;

    private EjTransaction(Builder b) {
        this.fileName              = b.fileName;
        this.fileLineStart         = b.fileLineStart;
        this.fileLineEnd           = b.fileLineEnd;
        this.sequenceNoFirst       = b.sequenceNoFirst;
        this.sequenceNoLast        = b.sequenceNoLast;
        this.firstEventTs          = b.firstEventTs;
        this.lastEventTs           = b.lastEventTs;
        this.cardNumberRaw         = b.cardNumberRaw;
        this.emvAid                = b.emvAid;
        this.emvAppLabel           = b.emvAppLabel;
        this.amountEntered         = b.amountEntered;
        this.pinEntered            = b.pinEntered;
        this.cardless              = b.cardless;
        this.opcode                = b.opcode;
        this.functionId            = b.functionId;
        this.amountRecorded        = b.amountRecorded;
        this.transSeqNumber        = b.transSeqNumber;
        this.ltSerno               = b.ltSerno;
        this.fidResponse           = b.fidResponse;
        this.fidNext               = b.fidNext;
        this.ltDispNotes1          = b.ltDispNotes1;
        this.ltDispNotes2          = b.ltDispNotes2;
        this.ltDispNotes3          = b.ltDispNotes3;
        this.ltDispNotes4          = b.ltDispNotes4;
        this.ltDispNotes5          = b.ltDispNotes5;
        this.ltDispNotes6          = b.ltDispNotes6;
        this.ltDispNotes7          = b.ltDispNotes7;
        this.denomination          = b.denomination;
        this.requestCount          = b.requestCount;
        this.pickupCount           = b.pickupCount;
        this.dispenseCount         = b.dispenseCount;
        this.remainCount           = b.remainCount;
        this.rejectCount           = b.rejectCount;
        this.notesTypeA            = b.notesTypeA;
        this.notesTypeB            = b.notesTypeB;
        this.notesTypeC            = b.notesTypeC;
        this.notesTypeD            = b.notesTypeD;
        this.notesTypeE            = b.notesTypeE;
        this.notesTypeF            = b.notesTypeF;
        this.notesTypeG            = b.notesTypeG;
        this.dispenseComplete      = b.dispenseComplete;
        this.cashPresented         = b.cashPresented;
        this.cashTaken             = b.cashTaken;
        this.cashTakenAfterTimeout = b.cashTakenAfterTimeout;
        this.cashNotTaken          = b.cashNotTaken;
        this.cardEjected           = b.cardEjected;
        this.cardTaken             = b.cardTaken;
        this.transactionTimeout    = b.transactionTimeout;
        this.hostTimeout           = b.hostTimeout;
        this.receiptDate           = b.receiptDate;
        this.receiptTime           = b.receiptTime;
        this.atmId                 = b.atmId;
        this.receiptCardNumber     = b.receiptCardNumber;
        this.receiptAccountNumber  = b.receiptAccountNumber;
        this.txnNo                 = b.txnNo;
        this.referenceNo           = b.referenceNo;
        this.responseCode          = b.responseCode;
        this.accountType           = b.accountType;
        this.withdrawalAmount      = b.withdrawalAmount;
        this.transactionAmount     = b.transactionAmount;
        this.fromAccount           = b.fromAccount;
        this.modBalance            = b.modBalance;
        this.availBalance          = b.availBalance;
        this.unableToProcess       = b.unableToProcess;
        this.declinedInsufficient  = b.declinedInsufficient;
        this.adminSubtotal         = b.adminSubtotal;
        this.transactionType       = (b.transactionType   != null) ? b.transactionType   : Type.OTHER;
        this.transactionStatus     = (b.transactionStatus != null) ? b.transactionStatus : Status.UNKNOWN;
        this.rawTransactionBlock   = b.rawTransactionBlock;
        this.loadBatchId           = b.loadBatchId;
    }

    public static Builder builder() { return new Builder(); }

    // ---- accessors --------------------------------------------------------
    public String getFileName()                  { return fileName; }
    public long   getFileLineStart()             { return fileLineStart; }
    public long   getFileLineEnd()               { return fileLineEnd; }
    public String getSequenceNoFirst()           { return sequenceNoFirst; }
    public String getSequenceNoLast()            { return sequenceNoLast; }
    public LocalDateTime getFirstEventTs()       { return firstEventTs; }
    public LocalDateTime getLastEventTs()        { return lastEventTs; }
    public String getCardNumberRaw()             { return cardNumberRaw; }
    public String getEmvAid()                    { return emvAid; }
    public String getEmvAppLabel()               { return emvAppLabel; }
    public BigDecimal getAmountEntered()         { return amountEntered; }
    public boolean isPinEntered()                { return pinEntered; }
    public boolean isCardless()                  { return cardless; }
    public String getOpcode()                    { return opcode; }
    public String getFunctionId()                { return functionId; }
    public BigDecimal getAmountRecorded()        { return amountRecorded; }
    public String getTransSeqNumber()            { return transSeqNumber; }
    public String getLtSerno()                   { return ltSerno; }
    public String getFidResponse()               { return fidResponse; }
    public String getFidNext()                   { return fidNext; }
    public Long getLtDispNotes1()                { return ltDispNotes1; }
    public Long getLtDispNotes2()                { return ltDispNotes2; }
    public Long getLtDispNotes3()                { return ltDispNotes3; }
    public Long getLtDispNotes4()                { return ltDispNotes4; }
    public Long getLtDispNotes5()                { return ltDispNotes5; }
    public Long getLtDispNotes6()                { return ltDispNotes6; }
    public Long getLtDispNotes7()                { return ltDispNotes7; }
    public String getDenomination()              { return denomination; }
    public String getRequestCount()              { return requestCount; }
    public String getPickupCount()               { return pickupCount; }
    public String getDispenseCount()             { return dispenseCount; }
    public String getRemainCount()               { return remainCount; }
    public String getRejectCount()               { return rejectCount; }
    public Long getNotesTypeA()                  { return notesTypeA; }
    public Long getNotesTypeB()                  { return notesTypeB; }
    public Long getNotesTypeC()                  { return notesTypeC; }
    public Long getNotesTypeD()                  { return notesTypeD; }
    public Long getNotesTypeE()                  { return notesTypeE; }
    public Long getNotesTypeF()                  { return notesTypeF; }
    public Long getNotesTypeG()                  { return notesTypeG; }
    public boolean isDispenseComplete()          { return dispenseComplete; }
    public boolean isCashPresented()             { return cashPresented; }
    public boolean isCashTaken()                 { return cashTaken; }
    public boolean isCashTakenAfterTimeout()     { return cashTakenAfterTimeout; }
    public boolean isCashNotTaken()              { return cashNotTaken; }
    public boolean isCardEjected()               { return cardEjected; }
    public boolean isCardTaken()                 { return cardTaken; }
    public boolean isTransactionTimeout()        { return transactionTimeout; }
    public boolean isHostTimeout()               { return hostTimeout; }
    public String getReceiptDate()               { return receiptDate; }
    public String getReceiptTime()               { return receiptTime; }
    public String getAtmId()                     { return atmId; }
    public String getReceiptCardNumber()         { return receiptCardNumber; }
    public String getReceiptAccountNumber()      { return receiptAccountNumber; }
    public String getTxnNo()                     { return txnNo; }
    public String getReferenceNo()               { return referenceNo; }
    public String getResponseCode()              { return responseCode; }
    public String getAccountType()               { return accountType; }
    public BigDecimal getWithdrawalAmount()      { return withdrawalAmount; }
    public BigDecimal getTransactionAmount()     { return transactionAmount; }
    public String getFromAccount()               { return fromAccount; }
    public BigDecimal getModBalance()            { return modBalance; }
    public BigDecimal getAvailBalance()          { return availBalance; }
    public boolean isUnableToProcess()           { return unableToProcess; }
    public boolean isDeclinedInsufficient()      { return declinedInsufficient; }
    public boolean isAdminSubtotal()             { return adminSubtotal; }
    public Type getTransactionType()             { return transactionType; }
    public Status getTransactionStatus()         { return transactionStatus; }
    public String getRawTransactionBlock()       { return rawTransactionBlock; }
    public String getLoadBatchId()               { return loadBatchId; }

    @Override
    public String toString() {
        return "EjTransaction{file=" + fileName
                + ", lines=" + fileLineStart + "-" + fileLineEnd
                + ", seq=" + sequenceNoFirst + "-" + sequenceNoLast
                + ", type=" + transactionType
                + ", status=" + transactionStatus + "}";
    }

    // ====================================================================
    // Builder
    // ====================================================================
    public static final class Builder {
        private String fileName;
        private long fileLineStart, fileLineEnd;
        private String sequenceNoFirst, sequenceNoLast;
        private LocalDateTime firstEventTs, lastEventTs;
        private String cardNumberRaw, emvAid, emvAppLabel;
        private BigDecimal amountEntered;
        private boolean pinEntered, cardless;
        private String opcode, functionId, transSeqNumber, ltSerno, fidResponse, fidNext;
        private BigDecimal amountRecorded;
        private Long ltDispNotes1, ltDispNotes2, ltDispNotes3, ltDispNotes4,
                     ltDispNotes5, ltDispNotes6, ltDispNotes7;
        private String denomination, requestCount, pickupCount, dispenseCount, remainCount, rejectCount;
        private Long notesTypeA, notesTypeB, notesTypeC, notesTypeD,
                     notesTypeE, notesTypeF, notesTypeG;
        private boolean dispenseComplete, cashPresented, cashTaken,
                        cashTakenAfterTimeout, cashNotTaken,
                        cardEjected, cardTaken,
                        transactionTimeout, hostTimeout;
        private String receiptDate, receiptTime, atmId;
        private String receiptCardNumber, receiptAccountNumber;
        private String txnNo, referenceNo, responseCode, accountType, fromAccount;
        private BigDecimal withdrawalAmount, transactionAmount, modBalance, availBalance;
        private boolean unableToProcess, declinedInsufficient, adminSubtotal;
        private Type transactionType;
        private Status transactionStatus;
        private String rawTransactionBlock, loadBatchId;

        public Builder fileName(String v)              { this.fileName = v; return this; }
        public Builder fileLineStart(long v)           { this.fileLineStart = v; return this; }
        public Builder fileLineEnd(long v)             { this.fileLineEnd = v; return this; }
        public Builder sequenceNoFirst(String v)       { this.sequenceNoFirst = v; return this; }
        public Builder sequenceNoLast(String v)        { this.sequenceNoLast = v; return this; }
        public Builder firstEventTs(LocalDateTime v)   { this.firstEventTs = v; return this; }
        public Builder lastEventTs(LocalDateTime v)    { this.lastEventTs = v; return this; }
        public Builder cardNumberRaw(String v)         { this.cardNumberRaw = v; return this; }
        public Builder emvAid(String v)                { this.emvAid = v; return this; }
        public Builder emvAppLabel(String v)           { this.emvAppLabel = v; return this; }
        public Builder amountEntered(BigDecimal v)     { this.amountEntered = v; return this; }
        public Builder pinEntered(boolean v)           { this.pinEntered = v; return this; }
        public Builder cardless(boolean v)             { this.cardless = v; return this; }
        public Builder opcode(String v)                { this.opcode = v; return this; }
        public Builder functionId(String v)            { this.functionId = v; return this; }
        public Builder amountRecorded(BigDecimal v)    { this.amountRecorded = v; return this; }
        public Builder transSeqNumber(String v)        { this.transSeqNumber = v; return this; }
        public Builder ltSerno(String v)               { this.ltSerno = v; return this; }
        public Builder fidResponse(String v)           { this.fidResponse = v; return this; }
        public Builder fidNext(String v)               { this.fidNext = v; return this; }
        public Builder ltDispNotes1(Long v)            { this.ltDispNotes1 = v; return this; }
        public Builder ltDispNotes2(Long v)            { this.ltDispNotes2 = v; return this; }
        public Builder ltDispNotes3(Long v)            { this.ltDispNotes3 = v; return this; }
        public Builder ltDispNotes4(Long v)            { this.ltDispNotes4 = v; return this; }
        public Builder ltDispNotes5(Long v)            { this.ltDispNotes5 = v; return this; }
        public Builder ltDispNotes6(Long v)            { this.ltDispNotes6 = v; return this; }
        public Builder ltDispNotes7(Long v)            { this.ltDispNotes7 = v; return this; }
        public Builder denomination(String v)          { this.denomination = v; return this; }
        public Builder requestCount(String v)          { this.requestCount = v; return this; }
        public Builder pickupCount(String v)           { this.pickupCount = v; return this; }
        public Builder dispenseCount(String v)         { this.dispenseCount = v; return this; }
        public Builder remainCount(String v)           { this.remainCount = v; return this; }
        public Builder rejectCount(String v)           { this.rejectCount = v; return this; }
        public Builder notesTypeA(Long v)              { this.notesTypeA = v; return this; }
        public Builder notesTypeB(Long v)              { this.notesTypeB = v; return this; }
        public Builder notesTypeC(Long v)              { this.notesTypeC = v; return this; }
        public Builder notesTypeD(Long v)              { this.notesTypeD = v; return this; }
        public Builder notesTypeE(Long v)              { this.notesTypeE = v; return this; }
        public Builder notesTypeF(Long v)              { this.notesTypeF = v; return this; }
        public Builder notesTypeG(Long v)              { this.notesTypeG = v; return this; }
        public Builder dispenseComplete(boolean v)     { this.dispenseComplete = v; return this; }
        public Builder cashPresented(boolean v)        { this.cashPresented = v; return this; }
        public Builder cashTaken(boolean v)            { this.cashTaken = v; return this; }
        public Builder cashTakenAfterTimeout(boolean v){ this.cashTakenAfterTimeout = v; return this; }
        public Builder cashNotTaken(boolean v)         { this.cashNotTaken = v; return this; }
        public Builder cardEjected(boolean v)          { this.cardEjected = v; return this; }
        public Builder cardTaken(boolean v)            { this.cardTaken = v; return this; }
        public Builder transactionTimeout(boolean v)   { this.transactionTimeout = v; return this; }
        public Builder hostTimeout(boolean v)          { this.hostTimeout = v; return this; }
        public Builder receiptDate(String v)           { this.receiptDate = v; return this; }
        public Builder receiptTime(String v)           { this.receiptTime = v; return this; }
        public Builder atmId(String v)                 { this.atmId = v; return this; }
        public Builder receiptCardNumber(String v)     { this.receiptCardNumber = v; return this; }
        public Builder receiptAccountNumber(String v)  { this.receiptAccountNumber = v; return this; }
        public Builder txnNo(String v)                 { this.txnNo = v; return this; }
        public Builder referenceNo(String v)           { this.referenceNo = v; return this; }
        public Builder responseCode(String v)          { this.responseCode = v; return this; }
        public Builder accountType(String v)           { this.accountType = v; return this; }
        public Builder fromAccount(String v)           { this.fromAccount = v; return this; }
        public Builder withdrawalAmount(BigDecimal v)  { this.withdrawalAmount = v; return this; }
        public Builder transactionAmount(BigDecimal v) { this.transactionAmount = v; return this; }
        public Builder modBalance(BigDecimal v)        { this.modBalance = v; return this; }
        public Builder availBalance(BigDecimal v)      { this.availBalance = v; return this; }
        public Builder unableToProcess(boolean v)      { this.unableToProcess = v; return this; }
        public Builder declinedInsufficient(boolean v) { this.declinedInsufficient = v; return this; }
        public Builder adminSubtotal(boolean v)        { this.adminSubtotal = v; return this; }
        public Builder transactionType(Type v)         { this.transactionType = v; return this; }
        public Builder transactionStatus(Status v)     { this.transactionStatus = v; return this; }
        public Builder rawTransactionBlock(String v)   { this.rawTransactionBlock = v; return this; }
        public Builder loadBatchId(String v)           { this.loadBatchId = v; return this; }

        public EjTransaction build() { return new EjTransaction(this); }
    }
}
