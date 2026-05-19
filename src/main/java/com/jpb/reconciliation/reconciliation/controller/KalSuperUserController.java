package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.KalSuperService;

@RestController
@RequestMapping("/test/api/v1/institution")
public class KalSuperUserController {

    @Autowired
    private KalSuperService kalSuperService;

    // Step 1 — Verify default credentials from email
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            @RequestBody KalSuperUserVerifyDto dto) {
        return kalSuperService.verifyCredentials(dto);
    }

    // Step 2 — Set new password (first time only)
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(
            @RequestBody KalSuperUserSetPasswordDto dto) {
        return kalSuperService.setNewPassword(dto);
    }

    // Step 3 — Login with new password → sends OTP
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(
            @RequestBody KalSuperUserVerifyDto dto) {
        return kalSuperService.login(dto);
    }

    // Forgot Password Step 1 — send OTP via email OR username
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        return kalSuperService.forgotPassword(request);
    }

    // Forgot Password Step 2 — verify OTP + set new password
    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return kalSuperService.resetPassword(request);
    }
    
    
    @PostMapping("/check-user-status")
    public ResponseEntity<RestWithStatusList> checkUserStatus(
            @RequestBody KalSuperUserVerifyDto dto) {

        return kalSuperService.checkUserStatus(dto);
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<RestWithStatusList> verifyEmail(

            @RequestParam String institutionCode,
            @RequestParam String username) {

        return kalSuperService.verifyEmail(
                institutionCode,
                username);
    }
}