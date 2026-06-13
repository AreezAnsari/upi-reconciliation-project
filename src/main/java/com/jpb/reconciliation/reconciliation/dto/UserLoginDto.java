// ─────────────────────────────────────────────────────────────────────────────
// FILE 3: UserLoginDto.java
// POST /api/v1/user/auth/login
// Body: { institutionCode, username, password }
// ─────────────────────────────────────────────────────────────────────────────
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class UserLoginDto {
    private String institutionCode;
    private String username;
    private String password;    // user's own password (after first setup)
}