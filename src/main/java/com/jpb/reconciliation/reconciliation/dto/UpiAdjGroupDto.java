package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpiAdjGroupDto {
    private String               category;
    private int                  totalRecords;
    private double               totalAmount;
    private List<UpiAdjItemDto>  items;
}