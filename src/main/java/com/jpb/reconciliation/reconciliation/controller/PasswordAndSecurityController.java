package com.jpb.reconciliation.reconciliation.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.PasswordExpiryDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.service.OtpService;

@RestController
@RequestMapping("/test/api/v1/security")
public class PasswordAndSecurityController {

    @Autowired private BranchAdminRepository branchRepo;
    @Autowired private MainAdminRepository mainRepo;
    @Autowired private KalAdminRepository kalRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpService otpService; // ← already exists in your project

    // ═══════════════════════════════════════════════════════
    // STEP 1 — Send OTP before password change
    // POST /test/api/v1/security/send-otp
    // Body: { userId, userType }  OR  { bankCode, username, userType }
    // ═══════════════════════════════════════════════════════
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody PasswordExpiryDto dto) {

        // ✅ Debug log
        System.out.println("=== Send OTP ===");
        System.out.println("userType   : " + dto.getUserType());
        System.out.println("userId     : " + dto.getUserId());
        System.out.println("username   : " + dto.getUsername());
        System.out.println("bankCode   : " + dto.getBankCode());
        System.out.println("branchCode : " + dto.getBranchCode());
        System.out.println("================");

        String email = null;
        try {
        	// BRANCH_ADMIN section mein
        	if ("BRANCH_ADMIN".equals(dto.getUserType())) {
        	    BranchAdmin user;
        	    if (dto.getUserId() != null) {
        	        user = branchRepo.findById(dto.getUserId())
        	            .orElseThrow(() -> new RuntimeException("User not found"));
        	    } else {
        	        // ✅ branchCode pehle try karo, phir bankCode
        	        String code = (dto.getBranchCode() != null && !dto.getBranchCode().isEmpty())
        	            ? dto.getBranchCode()
        	            : dto.getBankCode();

        	        user = branchRepo
        	            .findByBranchCodeAndUsername(code, dto.getUsername())
        	            .orElseThrow(() -> new RuntimeException("User not found"));
        	    }
        	    email = user.getEmail();
        	

            } else if ("SUPER_USER".equals(dto.getUserType())) {

                MainAdmin user;
                if (dto.getUserId() != null) {
                    user = mainRepo.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                } else {
                    String code = dto.getBankCode();
                    if (code == null || code.isEmpty()) {
                        return ResponseEntity.badRequest().body(
                            new RestWithStatusList("FAILURE",
                                "bankCode required for SUPER_USER", null));
                    }
                    user = mainRepo
                        .findByBankCodeAndUsername(code, dto.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                }
                email = user.getEmail();

            } else {
                // ADMIN / KAL_ADMIN
                KalAdmin user;
                if (dto.getUserId() != null) {
                    user = kalRepo.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                } else {
                    user = kalRepo.findByUserName(dto.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                }
                email = user.getEmailId();
            }

            otpService.generateAndSendOtp(email);
            String masked = maskEmail(email);
            List<Object> data = new ArrayList<>();
            data.add(masked);
            return ResponseEntity.ok(
                new RestWithStatusList("SUCCESS", "OTP sent to " + masked, data));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RestWithStatusList("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestWithStatusList("FAILURE",
                    "Failed to send OTP: " + e.getMessage(), null));
        }
    }

    // ═══════════════════════════════════════════════════════
    // STEP 2 — Verify OTP + Change Password
    // Body: { userId, userType, currentPassword, newPassword, otp }
    //   OR  { bankCode, username, userType, currentPassword, newPassword, otp }
    // ═══════════════════════════════════════════════════════

    // 1. Branch Admin
    @PostMapping("/branch/change-password")
    @Transactional
    public ResponseEntity<?> changeBranchPassword(@RequestBody PasswordExpiryDto dto) {
        try {
            // ✅ FIX: fall back to bankCode+username lookup when userId is not sent
            // (this is what was throwing the uncaught RuntimeException → 500)
            BranchAdmin user = (dto.getUserId() != null)
                ? branchRepo.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"))
                : branchRepo.findByBranchCodeAndUsername(dto.getBankCode(), dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Verify current password
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Current password is incorrect.", null));
            }

            // ✅ Verify OTP
            OtpService.OtpVerifyResult otpResult = otpService.verifyOtp(user.getEmail(), dto.getOtp());
            if (otpResult != OtpService.OtpVerifyResult.SUCCESS) {
                String msg = otpErrorMessage(otpResult);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", msg, null));
            }

            LocalDateTime now = LocalDateTime.now();
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setPasswordUpdatedAt(now);
            user.setStatus("ACTIVE");
            branchRepo.save(user);

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Password updated successfully",
                    java.util.Collections.singletonList(now.toString())));

        } catch (Exception e) {
            // ✅ FIX: never let an uncaught exception fall through to a raw 500 —
            // always return a structured FAILURE response the frontend can read
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestWithStatusList("FAILURE", "Password update failed: " + e.getMessage(), null));
        }
    }

    // 2. Main (Bank) Admin
    @PostMapping("/bank/change-password")
    @Transactional
    public ResponseEntity<?> changeMainPassword(@RequestBody PasswordExpiryDto dto) {
        try {
            // ✅ FIX: same userId-or-(bankCode+username) fallback
            MainAdmin user = (dto.getUserId() != null)
                ? mainRepo.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"))
                : mainRepo.findByBankCodeAndUsername(dto.getBankCode(), dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Verify current password
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Current password is incorrect.", null));
            }

            // ✅ Verify OTP
            OtpService.OtpVerifyResult otpResult = otpService.verifyOtp(user.getEmail(), dto.getOtp());
            if (otpResult != OtpService.OtpVerifyResult.SUCCESS) {
                String msg = otpErrorMessage(otpResult);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", msg, null));
            }

            LocalDateTime now = LocalDateTime.now();
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setPasswordUpdatedAt(now);
            user.setStatus("ACTIVE");
            mainRepo.save(user);

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Password updated successfully",
                    java.util.Collections.singletonList(now.toString())));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestWithStatusList("FAILURE", "Password update failed: " + e.getMessage(), null));
        }
    }

    // 3. Kal Admin
    @PostMapping("/kal/change-password")
    @Transactional
    public ResponseEntity<?> changeKalPassword(@RequestBody PasswordExpiryDto dto) {
        try {
            // ✅ FIX: same fallback — KalAdmin lookup by username when userId is null
            KalAdmin user = (dto.getUserId() != null)
                ? kalRepo.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"))
                : kalRepo.findByUserName(dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Verify current password
            if (user.getPasswordManager() == null ||
                !passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordManager().getUserPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Current password is incorrect.", null));
            }

            // ✅ Verify OTP — use emailId (KalAdmin's email field), same as sendOtp
            OtpService.OtpVerifyResult otpResult =
                    otpService.verifyOtp(user.getEmailId(), dto.getOtp());
            if (otpResult != OtpService.OtpVerifyResult.SUCCESS) {
                String msg = otpErrorMessage(otpResult);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", msg, null));
            }

            LocalDateTime now = LocalDateTime.now();
            user.getPasswordManager().setUserPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.getPasswordManager().setExpirationDate(now.plusDays(90));
            user.setPasswordUpdatedAt(now);
            user.setUserStatus("ACTIVE");
            kalRepo.save(user);

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Password updated successfully",
                    java.util.Collections.singletonList(now.toString())));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestWithStatusList("FAILURE", "Password update failed: " + e.getMessage(), null));
        }
    }

    // ── Private helpers ───────────────────────────────────

    /**
     * Old-style if/else instead of Java 14+ arrow-switch expression,
     * since this project compiles on an older Java level (8/11) where
     * "switch (x) { case A -> ...; }" is not valid syntax.
     */
    private String otpErrorMessage(OtpService.OtpVerifyResult otpResult) {
        if (otpResult == OtpService.OtpVerifyResult.EXPIRED) {
            return "OTP has expired. Please request a new one.";
        } else if (otpResult == OtpService.OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED) {
            return "Too many incorrect attempts. Please request a new OTP.";
        } else if (otpResult == OtpService.OtpVerifyResult.NOT_FOUND) {
            return "OTP not found. Please request a new one.";
        } else {
            return "Invalid OTP. Please try again.";
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        return local.charAt(0) + "***@" + domain;
    }
}