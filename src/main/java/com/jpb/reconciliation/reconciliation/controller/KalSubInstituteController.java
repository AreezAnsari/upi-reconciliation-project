package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.KalSubInstituteService;

@RestController
@RequestMapping("/test/api/v1/sub-institution")
public class KalSubInstituteController {

    @Autowired
    private KalSubInstituteService kalSubInstituteService;

    // Step 1 — Verify default credentials from email
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            @RequestBody KalSubInstituteVerifyDto dto) {

        return kalSubInstituteService.verifyCredentials(dto);
    }

    // Step 2 — Set new password (first time only)
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(
            @RequestBody KalSubInstituteSetPasswordDto dto) {

        return kalSubInstituteService.setNewPassword(dto);
    }

    // Step 3 — Login with new password → sends OTP
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(
            @RequestBody KalSubInstituteVerifyDto dto) {

        return kalSubInstituteService.login(dto);
    }

    // Forgot Password Step 1 — send OTP via email OR username
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        return kalSubInstituteService.forgotPassword(request);
    }

    // Forgot Password Step 2 — verify OTP + set new password
    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        return kalSubInstituteService.resetPassword(request);
    }

    @PostMapping("/check-user-status")
    public ResponseEntity<RestWithStatusList> checkUserStatus(
            @RequestBody KalSubInstituteVerifyDto dto) {

        return kalSubInstituteService.checkUserStatus(dto);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<RestWithStatusList> verifyEmail(

            @RequestParam String institutionCode,
            @RequestParam String username) {

        return kalSubInstituteService.verifyEmail(
                institutionCode,
                username);
    }
}