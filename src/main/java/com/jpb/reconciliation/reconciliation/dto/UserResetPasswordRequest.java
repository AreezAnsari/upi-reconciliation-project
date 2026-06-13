// ─────────────────────────────────────────────────────────────────────────────
// FILE 5: UserResetPasswordRequest.java
// POST /api/v1/user/auth/reset-password
// Body: { email OR (institutionCode + username), otp, newPassword, confirmNewPassword }
// ─────────────────────────────────────────────────────────────────────────────
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class UserResetPasswordRequest {
    private String email;
    private String institutionCode;
    private String username;
    private String otp;
    private String newPassword;
    private String confirmNewPassword;
}
