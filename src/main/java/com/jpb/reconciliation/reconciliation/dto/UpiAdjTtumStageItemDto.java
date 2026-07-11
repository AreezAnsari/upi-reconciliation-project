package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the "Stage-wise TTUM Decision" table, resolved from real
 * REC_UPI_ADJ_DATA rows for a given ADJDATE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiAdjTtumStageItemDto {

    private String adjType;       // ADJTYPE value from DB (e.g. "Fraud Chargeback Raise")
    private String stage;         // Friendly display name for the stage
    private String flag;          // reused from existing FLAG_MAP
    private String txn;           // "U2" or "U3" -- from TRANSACTION_TYPE column
    private String role;          // "BEN/ACQ" or "REM/ISS" -- Jio's actual role that day
    private String fin;           // "Financial" / "Non-Financial"
    private String dr;            // DR account code ("" if none required)
    private String cr;            // CR account code ("" if none required)
    private boolean ttumRequired; // true only when fin == Financial
    private int count;            // how many txns had this ADJTYPE+role+txn that day
    private double amount;        // total TRAN_AMOUNT for this group that day
}