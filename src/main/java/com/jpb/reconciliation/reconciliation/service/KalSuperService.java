package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface KalSuperService {

    // Step 1 — Verify default credentials (KAL_SUPER_USER mein check karo)
    ResponseEntity<RestWithStatusList> verifyCredentials(KalSuperUserVerifyDto dto);

    // Step 2 — Set new password (KAL_SUPER_USER mein save karo)
    ResponseEntity<RestWithStatusList> setNewPassword(KalSuperUserSetPasswordDto dto);

    // Step 3 — Login → OTP bhejo
    ResponseEntity<RestWithStatusList> login(KalSuperUserVerifyDto dto);

    // Forgot Password — Step A: OTP bhejo
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request);

    // Forgot Password — Step B: OTP verify + new password set karo
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);

    // ✅ NEW: KAL_SUPER_USER se check karo — NEW_USER ya OLD_USER
    //         Email link par click karne ke baad call hoga
    //         institutionCode + username dono se dhundho
    ResponseEntity<RestWithStatusList> checkUserStatus(KalSuperUserVerifyDto dto);

    // ✅ NEW: Email link verification — institutionCodkale + username se
    //         KAL_SUPER_USER mein dhundho → userStatus return karo
    //         VerifyEmail.jsx yeh call karega
    
    ResponseEntity<RestWithStatusList> verifyEmail(String institutionCode, String username);
    
}