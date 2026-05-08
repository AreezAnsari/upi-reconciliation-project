package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class KalSuperUserSetPasswordDto {

    private String institutionCode;

    private String username;

    private String newPassword;
}