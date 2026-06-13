// ─────────────────────────────────────────────────────────────────────────────
// FILE 4: UserForgotPasswordRequest.java
// POST /api/v1/user/auth/forgot-password
// Body: { email } OR { institutionCode, username }
// ─────────────────────────────────────────────────────────────────────────────
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class UserForgotPasswordRequest {
    private String email;
    private String institutionCode;
    private String username;
}