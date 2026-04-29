package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class LookupRequestDTO {

    private String lookupName;
    private String lookupValue;
    private String lookupCode;
    private Integer sortOrder;

    private String lookupDesc;
    private String shortName;
    private String longName;

    private Long parentLookupId;
}