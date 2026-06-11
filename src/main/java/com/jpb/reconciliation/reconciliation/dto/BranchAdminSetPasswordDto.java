package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class BranchAdminSetPasswordDto {
    private String branchCode;
    private String username;
    private String newPassword;
}
