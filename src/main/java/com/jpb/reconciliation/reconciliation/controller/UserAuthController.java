package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.service.UserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints for REC_USER (institution sub-users created via Add User).
 *
 * Base path: /api/v1/user/auth
 *
 * ─────────────────────────────────────────────────────────────────
 * FULL FLOW:
 *
 *  [Admin creates user via POST /api/v1/user/create]
 *      → Welcome email sent: institutionCode + username + defaultPassword + Verify button
 *
 *  1. POST /api/v1/user/auth/check-status
 *      → Returns NEW_USER or OLD_USER
 *
 *  2. POST /api/v1/user/auth/verify-credentials
 *      → Validates institutionCode + username + defaultPassword
 *
 *  3. POST /api/v1/user/auth/set-password
 *      → User sets their own password (one-time)
 *      → status: REQUEST → VERIFIED
 *
 *  4. POST /api/v1/user/auth/login
 *      → Password verified → OTP sent to email
 *
 *  5. POST /api/v1/user/auth/verify-otp
 *      → OTP verified → status: VERIFIED → ACTIVE
 *
 *  Forgot Password:
 *  6. POST /api/v1/user/auth/forgot-password  → OTP to email
 *  7. POST /api/v1/user/auth/reset-password   → OTP + new password
 * ─────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/v1/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    // ── Pre-check: NEW_USER or OLD_USER? ─────────────────────────────────────

    /**
     * Called when user lands on the verify page (from email link).
     * Returns NEW_USER (needs setup) or OLD_USER (can login directly).
     *
     * POST /api/v1/user/auth/check-status
     * Body: { "institutionCode": "ABC", "username": "john" }
     */
    @PostMapping("/check-status")
    public ResponseEntity<RestWithStatusList> checkUserStatus(
            @RequestBody UserVerifyCredentialsDto dto) {
        return userAuthService.checkUserStatus(dto);
    }

    // ── Step 1: Verify default credentials ───────────────────────────────────

    /**
     * Verifies the credentials sent in the welcome email.
     *
     * POST /api/v1/user/auth/verify-credentials
     * Body: { "institutionCode": "ABC", "username": "john", "defaultPassword": "JOHN4823" }
     *
     * Response status:
     *   SUCCESS          → proceed to set-password
     *   ALREADY_VERIFIED → redirect to login
     *   FAILURE          → invalid credentials
     */
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            @RequestBody UserVerifyCredentialsDto dto) {
        return userAuthService.verifyCredentials(dto);
    }

    // ── Step 2: Set new password ──────────────────────────────────────────────

    /**
     * Sets the user's own password for the first time.
     * One-time only — if passwordSet=1, returns ALREADY_VERIFIED.
     *
     * POST /api/v1/user/auth/set-password
     * Body: { "institutionCode": "ABC", "username": "john", "newPassword": "MyPass@123" }
     */
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setNewPassword(
            @RequestBody UserSetPasswordDto dto) {
        return userAuthService.setNewPassword(dto);
    }

    // ── Step 3: Login → OTP sent ──────────────────────────────────────────────

    /**
     * Verifies password and sends OTP to registered email.
     * Returns masked email in response data.
     *
     * POST /api/v1/user/auth/login
     * Body: { "institutionCode": "ABC", "username": "john", "password": "MyPass@123" }
     */
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(
            @RequestBody UserLoginDto dto) {
        return userAuthService.login(dto);
    }

    // ── Step 4: Verify OTP → Activate user ───────────────────────────────────

    /**
     * Verifies the login OTP and activates the user (status → ACTIVE).
     * Returns username, role, institutionCode on success.
     *
     * POST /api/v1/user/auth/verify-otp
     * Body: { "email": "john@example.com", "otp": "482931" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<RestWithStatusList> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        return userAuthService.verifyOtpAndActivate(email, otp);
    }

    // ── Forgot Password: Step A ───────────────────────────────────────────────

    /**
     * Sends a forgot-password OTP to the user's registered email.
     *
     * POST /api/v1/user/auth/forgot-password
     * Body: { "email": "john@example.com" }
     *   OR  { "institutionCode": "ABC", "username": "john" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(
            @RequestBody UserForgotPasswordRequest request) {
        return userAuthService.forgotPassword(request);
    }

    // ── Forgot Password: Step B ───────────────────────────────────────────────

    /**
     * Verifies OTP and resets password.
     *
     * POST /api/v1/user/auth/reset-password
     * Body: {
     *   "email": "john@example.com",       ← OR institutionCode + username
     *   "otp": "482931",
     *   "newPassword": "NewPass@123",
     *   "confirmNewPassword": "NewPass@123"
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(
            @RequestBody UserResetPasswordRequest request) {
        return userAuthService.resetPassword(request);
    }
}