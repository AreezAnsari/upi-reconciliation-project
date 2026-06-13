// ─────────────────────────────────────────────────────────────────────────────
// FILE 2: UserSetPasswordDto.java
// POST /api/v1/user/auth/set-password
// Body: { institutionCode, username, newPassword }
// ─────────────────────────────────────────────────────────────────────────────
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class UserSetPasswordDto {
    private String institutionCode;
    private String username;
    private String newPassword;
}