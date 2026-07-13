package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class UpiAdjItemDto {
    private String category;
    private String flag;
    private String adjustmentType;
    private int    count;
    private double amount;
    private String ttumStatus;
    private int    remJioCount;   
    private int    benJioCount;   
}