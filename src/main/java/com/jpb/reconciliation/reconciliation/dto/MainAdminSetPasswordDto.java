package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class MainAdminSetPasswordDto {

    private String institutionCode;

    private String username;

    private String newPassword;
}
