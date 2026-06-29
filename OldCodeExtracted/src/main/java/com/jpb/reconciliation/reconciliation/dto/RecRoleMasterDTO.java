package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecRoleMasterDTO {

    private Long    id;
    private String  roleName;    // e.g. "MAKER"
    private Integer roleCode;    // e.g. 1001 (from StandardRole enum)
    private Boolean isSystemRole;
    private String  status;
}