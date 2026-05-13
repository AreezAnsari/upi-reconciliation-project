// ✅ FIX 1: Correct package — dto, NOT service
// ✅ FIX 2: Supports email OR username (user dono mein se kuch bhi de sakta hai)
// ✅ FIX 3: @Data Lombok annotation — all getters/setters auto-generate hote hain

package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {

    // ✅ User email OR username de sakta hai — dono optional
    // Backend dono se user dhundega
    private String emailId;    // Email se dhundna (agar user email deta hai)

    private String username;   // Username se dhundna (agar user username deta hai)

    // ✅ institutionCode — identify karne ke liye (optional if email unique hai)
    private String institutionCode;
}