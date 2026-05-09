package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class KalSuperUserVerifyDto {
    private String institutionCode;
    private String username;
    private String defaultPassword;
    private String email; // ✅ ADD KARO
}