package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class UpiAdjRequestDto {
    private String adjDate;
    private int page = 0;     // 👈 NEW - default first page
    private int size = 50;    // 👈 NEW - default page size
}