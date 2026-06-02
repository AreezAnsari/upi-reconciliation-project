package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class KalSubInstituteVerifyDto {

    private String institutionCode;

    // ✅ username field — frontend "User ID" se aata hai
    private String username;

    // ✅ defaultPassword — Step 1 aur Step 3 dono mein use hota hai
    private String defaultPassword;
}