package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.service.MainAdminService;

@RestController
@RequestMapping("/test/api/v1/bank")
public class MainAdminController {

    @Autowired
    private MainAdminService mainAdminService;

    @Autowired
    private MainAdminRepository mainAdminRepository;

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
            @RequestBody ForgotPasswordRequestDto request) {
        return mainAdminService.forgotPassword(request);
    }

    // Direct Login — bankCode + username + password → JWT (no OTP)
    @PostMapping("/direct-login")
    public ResponseEntity<RestWithStatusList> directLogin(
            @RequestBody MainAdminVerifyDto dto) {
        return mainAdminService.directLogin(dto);
    }

    // Forgot Password Step 2 — sirf OTP verify (password reset nahi)
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(
            @RequestBody ForgotPasswordRequestDto request) {
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

            @RequestParam String bankCode,
            @RequestParam String username) {

        return mainAdminService.verifyEmail(
                bankCode,
                username);
    }

    @PostMapping("/schedule-inactivate/{id}")
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long id, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.scheduleInactivate(id, by);
    }

    @PostMapping("/undo-inactivate/{id}")
    public ResponseEntity<RestWithStatusList> undoInactivate(
            @PathVariable Long id, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.undoInactivate(id, by);
    }

    @PostMapping("/schedule-reactivate/{id}")
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long id, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.scheduleReactivate(id, by);
    }

    @PostMapping("/undo-reactivate/{id}")
    public ResponseEntity<RestWithStatusList> undoReactivate(
            @PathVariable Long id, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.undoReactivate(id, by);
    }

    @PostMapping("/schedule-block-by-bank/{bankId}")
    public ResponseEntity<RestWithStatusList> scheduleBlockByBankId(
            @PathVariable Long bankId,
            @org.springframework.web.bind.annotation.RequestParam(value = "reason", required = false, defaultValue = "") String reason,
            Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.scheduleBlockByBankId(bankId, by, reason);
    }

    @PostMapping("/undo-block-by-bank/{bankId}")
    public ResponseEntity<RestWithStatusList> undoBlockByBankId(
            @PathVariable Long bankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return mainAdminService.undoBlockByBankId(bankId, by);
    }

    @GetMapping("/my-status")
    public ResponseEntity<RestWithStatusList> getMyStatus(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Not authenticated", new java.util.ArrayList<>()));
        }
        String name = authentication.getName();
        java.util.Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> adminOpt =
                mainAdminRepository.findFirstByUsername(name);
        if (!adminOpt.isPresent()) {
            adminOpt = mainAdminRepository.findFirstByEmailAndStatusNot(name, "BLOCKED");
        }
        return adminOpt
                .map(admin -> ResponseEntity.ok(
                        new RestWithStatusList("SUCCESS", admin.getStatus(), new java.util.ArrayList<>())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestWithStatusList("FAILURE", "Admin not found", new java.util.ArrayList<>())));
    }

}
