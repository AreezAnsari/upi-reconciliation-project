package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.*;
import org.springframework.http.ResponseEntity;

/**
 * Auth flow for REC_USER (AddUser table).
 *
 * Full flow:
 *  1. verifyCredentials  — check institutionCode + username + defaultPassword
 *  2. setNewPassword     — user sets their own password (first time only)
 *  3. login              — password verified → OTP sent
 *  4. verifyOtpAndActivate — OTP verified → status → ACTIVE
 *  5. forgotPassword     — send OTP to email
 *  6. resetPassword      — OTP + new password
 */
public interface UserAuthService {

    // Step 1: Verify default credentials from welcome email
    ResponseEntity<RestWithStatusList> verifyCredentials(UserVerifyCredentialsDto dto);

    // Step 2: Set new password (one-time, only if passwordSet=0)
    ResponseEntity<RestWithStatusList> setNewPassword(UserSetPasswordDto dto);

    // Step 3: Login with new password → OTP sent
    ResponseEntity<RestWithStatusList> login(UserLoginDto dto);

    // Step 4: Verify OTP → user becomes ACTIVE
    ResponseEntity<RestWithStatusList> verifyOtpAndActivate(String email, String otp);

    // Forgot Password: Step A — send OTP
    ResponseEntity<RestWithStatusList> forgotPassword(UserForgotPasswordRequest request);

    // Forgot Password: Step B — verify OTP + set new password
    ResponseEntity<RestWithStatusList> resetPassword(UserResetPasswordRequest request);

    // Check user status (NEW_USER / OLD_USER) — called from verify-email page
    ResponseEntity<RestWithStatusList> checkUserStatus(UserVerifyCredentialsDto dto);
}