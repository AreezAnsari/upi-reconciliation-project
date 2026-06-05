package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface MainAdminService {

    // Step 1 — Verify default credentials (KAL_SUPER_USER mein check karo)
    ResponseEntity<RestWithStatusList> verifyCredentials(MainAdminVerifyDto dto);

    // Step 2 — Set new password (KAL_SUPER_USER mein save karo)
    ResponseEntity<RestWithStatusList> setNewPassword(MainAdminSetPasswordDto dto);

    // Step 3 — Login → OTP bhejo
    ResponseEntity<RestWithStatusList> login(MainAdminVerifyDto dto);

    // Direct Login — institutionCode + username + password → JWT (no OTP)
    ResponseEntity<RestWithStatusList> directLogin(MainAdminVerifyDto dto);

    // Forgot Password — Step A: OTP bhejo
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request);

    // Forgot Password — Step B: OTP sirf verify karo (password reset nahi)
    ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequest request);

    // Forgot Password — Step C: OTP verify + new password set karo
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);

    // ✅ NEW: KAL_SUPER_USER se check karo — NEW_USER ya OLD_USER
    //         Email link par click karne ke baad call hoga
    //         institutionCode + username dono se dhundho
    ResponseEntity<RestWithStatusList> checkUserStatus(MainAdminVerifyDto dto);

    // ✅ NEW: Email link verification — institutionCodkale + username se
    //         KAL_SUPER_USER mein dhundho → userStatus return karo
    //         VerifyEmail.jsx yeh call karega

    ResponseEntity<RestWithStatusList> verifyEmail(String institutionCode, String username);

    // OTP verify ke baad institution status → ACTIVE karo
    ResponseEntity<RestWithStatusList> activateInstitution(String email);

}
