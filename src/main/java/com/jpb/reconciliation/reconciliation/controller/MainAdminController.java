package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.MainAdminService;

@RestController
@RequestMapping("/test/api/v1/institution")
public class MainAdminController {

    @Autowired
    private MainAdminService mainAdminService;

    // Step 1 — Verify default credentials from email
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            @RequestBody MainAdminVerifyDto dto) {
        return mainAdminService.verifyCredentials(dto);
    }

    // Step 2 — Set new password (first time only)
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(
            @RequestBody MainAdminSetPasswordDto dto) {
        return mainAdminService.setNewPassword(dto);
    }

    // Step 3 — Login with new password → sends OTP
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(
            @RequestBody MainAdminVerifyDto dto) {
        return mainAdminService.login(dto);
    }

    // Forgot Password Step 1 — send OTP via email OR username
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        return mainAdminService.forgotPassword(request);
    }

    // Direct Login — institutionCode + username + password → JWT (no OTP)
    @PostMapping("/direct-login")
    public ResponseEntity<RestWithStatusList> directLogin(
            @RequestBody MainAdminVerifyDto dto) {
        return mainAdminService.directLogin(dto);
    }

    // Forgot Password Step 2 — sirf OTP verify (password reset nahi)
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(
            @RequestBody ForgotPasswordRequest request) {
        return mainAdminService.verifyForgotOtp(request);
    }

    // Forgot Password Step 3 — verify OTP + set new password
    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return mainAdminService.resetPassword(request);
    }


    @PostMapping("/check-user-status")
    public ResponseEntity<RestWithStatusList> checkUserStatus(
            @RequestBody MainAdminVerifyDto dto) {

        return mainAdminService.checkUserStatus(dto);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<RestWithStatusList> verifyEmail(

            @RequestParam String institutionCode,
            @RequestParam String username) {

        return mainAdminService.verifyEmail(
                institutionCode,
                username);
    }
}
