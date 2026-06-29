package com.jpb.reconciliation.reconciliation.service;

import lombok.Data;

@Data
public class ResetPasswordDto {

    private String emailId;

    private String otp;

    private String newPassword;

    private String confirmPassword;
}