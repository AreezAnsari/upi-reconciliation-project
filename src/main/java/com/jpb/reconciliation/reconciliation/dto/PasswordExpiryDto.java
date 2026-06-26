package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class PasswordExpiryDto {
    private Long   userId;          // KalAdmin ke liye
    private String username;        // ← ADD — BankAdmin/BranchAdmin ke liye
    private String bankCode;        // ← ADD — lookup ke liye
    private String branchCode; 
    private String currentPassword;
    private String newPassword;
    private String userType;
    private String otp;
}