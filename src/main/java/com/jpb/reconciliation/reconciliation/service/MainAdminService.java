package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface MainAdminService {

    // Step 1 — Verify default credentials (BANK_ADMIN mein check karo)
    ResponseEntity<RestWithStatusList> verifyCredentials(MainAdminVerifyDto dto);

    // Step 2 — Set new password (BANK_ADMIN mein save karo)
    ResponseEntity<RestWithStatusList> setNewPassword(MainAdminSetPasswordDto dto);

    // Step 3 — Login → OTP bhejo
    ResponseEntity<RestWithStatusList> login(MainAdminVerifyDto dto);

    // Direct Login — bankCode + username + password → JWT (no OTP)
    ResponseEntity<RestWithStatusList> directLogin(MainAdminVerifyDto dto);

    // Forgot Password — Step A: OTP bhejo
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequestDto request);

    // Forgot Password — Step B: OTP sirf verify karo (password reset nahi)
    ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequestDto request);

    // Forgot Password — Step C: OTP verify + new password set karo
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);

    // ✅ NEW: BANK_ADMIN se check karo — NEW_USER ya OLD_USER
    //         Email link par click karne ke baad call hoga
    //         bankCode + username dono se dhundho
    ResponseEntity<RestWithStatusList> checkUserStatus(MainAdminVerifyDto dto);

    // ✅ NEW: Email link verification — bankCodkale + username se
    //         BANK_ADMIN mein dhundho → userStatus return karo
    //         VerifyEmail.jsx yeh call karega

    ResponseEntity<RestWithStatusList> verifyEmail(String bankCode, String username);

    // OTP verify ke baad bank status → ACTIVE karo
    ResponseEntity<RestWithStatusList> activateBank(String email);

}
