package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface KalSubInstituteService {

    // Step 1 — Verify default credentials (KAL_SUB_INSTITUTE mein check karo)
    ResponseEntity<RestWithStatusList> verifyCredentials(KalSubInstituteVerifyDto dto);

    // Step 2 — Set new password (KAL_SUB_INSTITUTE mein save karo)
    ResponseEntity<RestWithStatusList> setNewPassword(KalSubInstituteSetPasswordDto dto);

    // Step 3 — Login → OTP bhejo
    ResponseEntity<RestWithStatusList> login(KalSubInstituteVerifyDto dto);

    // Forgot Password — Step A: OTP bhejo
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request);

    // Forgot Password — Step B: OTP verify + new password set karo
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);

    // ✅ NEW: KAL_SUB_INSTITUTE se check karo — NEW_USER ya OLD_USER
    //         Email link par click karne ke baad call hoga
    //         institutionCode + username dono se dhundho
    ResponseEntity<RestWithStatusList> checkUserStatus(KalSubInstituteVerifyDto dto);

    // ✅ NEW: Email link verification — institutionCode + username se
    //         KAL_SUB_INSTITUTE mein dhundho → userStatus return karo
    //         VerifyEmail.jsx yeh call karega

    ResponseEntity<RestWithStatusList> verifyEmail(String institutionCode, String username);

}