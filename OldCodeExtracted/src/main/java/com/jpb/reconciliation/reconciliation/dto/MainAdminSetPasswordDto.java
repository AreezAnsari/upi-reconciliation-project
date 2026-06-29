package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class MainAdminSetPasswordDto {

    private String bankCode;

    private String username;

    private String newPassword;
}
