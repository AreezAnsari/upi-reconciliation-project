package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class BranchAdminVerifyDto {
    private String institutionCode;
    private String username;
    private String defaultPassword;
}
