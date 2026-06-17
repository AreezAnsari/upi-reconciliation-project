package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.BranchAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.BranchAdminService;

@RestController
@RequestMapping("/test/api/v1/branch")
@CrossOrigin(origins = "*")
public class BranchAdminController {

    @Autowired
    private BranchAdminService branchAdminService;

    // Step 0 — verify email link (NEW_USER / OLD_USER)
    @GetMapping("/verify-email")
    public ResponseEntity<RestWithStatusList> verifyEmail(
            @RequestParam String bankCode,
            @RequestParam String username) {
        return branchAdminService.verifyEmail(bankCode, username);
    }

    // Check user status inline
    @PostMapping("/check-user-status")
    public ResponseEntity<RestWithStatusList> checkUserStatus(@RequestBody BranchAdminVerifyDto dto) {
        return branchAdminService.checkUserStatus(dto);
    }

    // Step 1 — verify default credentials from email
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(@RequestBody BranchAdminVerifyDto dto) {
        return branchAdminService.verifyCredentials(dto);
    }

    // Step 2 — set new password (first time only)
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(@RequestBody BranchAdminSetPasswordDto dto) {
        return branchAdminService.setNewPassword(dto);
    }

    // Step 3 — login with new password → sends OTP
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(@RequestBody BranchAdminVerifyDto dto) {
        return branchAdminService.login(dto);
    }

    // Direct login (no OTP)
    @PostMapping("/direct-login")
    public ResponseEntity<RestWithStatusList> directLogin(@RequestBody BranchAdminVerifyDto dto) {
        return branchAdminService.directLogin(dto);
    }

    // Activate after OTP verification
    @PostMapping("/activate")
    public ResponseEntity<RestWithStatusList> activate(@RequestParam String email) {
        return branchAdminService.activateBranchAdmin(email);
    }

    // Forgot Password Step 1 — send OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        return branchAdminService.forgotPassword(request);
    }

    // Forgot Password Step 2 — verify OTP only
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(@RequestBody ForgotPasswordRequestDto request) {
        return branchAdminService.verifyForgotOtp(request);
    }

    // Forgot Password Step 3 — reset password
    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(@RequestBody ResetPasswordRequest request) {
        return branchAdminService.resetPassword(request);
    }

    // ─── Branch Admin status by BranchBank ID (KalAdmin Admin Status page) ───

    @PostMapping("/schedule-inactivate-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> scheduleInactivateAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.scheduleInactivateByBranchBankId(branchBankId, by);
    }

    @PostMapping("/undo-inactivate-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> undoInactivateAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.undoInactivateByBranchBankId(branchBankId, by);
    }

    @PostMapping("/schedule-reactivate-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> scheduleReactivateAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.scheduleReactivateByBranchBankId(branchBankId, by);
    }

    @PostMapping("/undo-reactivate-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> undoReactivateAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.undoReactivateByBranchBankId(branchBankId, by);
    }

    @PostMapping("/schedule-block-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> scheduleBlockAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.scheduleBlockByBranchBankId(branchBankId, by);
    }

    @PostMapping("/undo-block-admin/{branchBankId}")
    public ResponseEntity<RestWithStatusList> undoBlockAdmin(
            @PathVariable Long branchBankId, Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return branchAdminService.undoBlockByBranchBankId(branchBankId, by);
    }

}

