// ─────────────────────────────────────────────────────────────────────────────
// FILE 1: UserVerifyCredentialsDto.java
// POST /api/v1/user/auth/verify-credentials
// Body: { institutionCode, username, defaultPassword }
// ─────────────────────────────────────────────────────────────────────────────
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class UserVerifyCredentialsDto {
    private String institutionCode;
    private String username;
    private String defaultPassword;
}