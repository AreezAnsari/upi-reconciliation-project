package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiAdjTtumStageItemDto {

    private String adjType;       // ADJTYPE from DB
    private String stage;         // display name
    private String flag;          // FLAG_MAP se
    private String txn;           // U2 or U3
    private String rcGroup;       // RC- + RESPONSE code (e.g. "RC-00", "RC-RB") ← NEW
    private String role;          // JIO role — REM/BEN/ISS/ACQ
    private String fin;           // Financial / Non-Financial
    private String dr;            // DR account code
    private String cr;            // CR account code
    private boolean ttumRequired; // true = Financial
    private int count;
    private double amount;
}