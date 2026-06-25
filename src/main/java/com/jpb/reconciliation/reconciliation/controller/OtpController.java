package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.MainAdminService;
import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "*")
public class OtpController {

    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private MainAdminService mainAdminService;

    @Autowired
    private MainBankRepository mainBankRepository;

    @Autowired
    private BranchAdminRepository branchAdminRepository;

    @Autowired
    private BranchBankRepository branchBankRepository;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    // ───────────────── SEND OTP ─────────────────

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email is required."));
        }

        try {
            otpService.generateAndSendOtp(email);
            logger.info("[OTP-SEND] OTP sent successfully to: {}", maskEmail(email));

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);

        } catch (EmailDeliveryException e) {
            // OTP was already rolled back inside OtpService — safe to return error
            logger.error("[OTP-SEND-FAIL] Email delivery failed for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again. " +
                            "If the problem persists, contact support@kalinfotech.com"));
        } catch (Exception e) {
            logger.error("[OTP-SEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("An unexpected error occurred. Please try again."));
        }
    }

    // ───────────────── VERIFY OTP — JWT return karo on success ─────────────────

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email and OTP are required."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);

        if (result == OtpVerifyResult.SUCCESS) {

            // ── Bank status → ACTIVE (first login ke baad) ──
            try {
                mainAdminService.activateBank(email);
            } catch (Exception e) {
                System.out.println("Warning: Could not activate bank for "
                    + email + ": " + e.getMessage());
                e.printStackTrace();
            }

            // ── Resolve bankCode, branchCode, and JWT subject by email ──
            // Bank Admin  → subject = email   (MainAdmin lookup uses email)
            // Branch Admin → subject = username (avoids collision with BANK_ADMIN email records)
            String resolvedBankCode   = null;
            String resolvedBranchCode = null;
            String jwtSubject         = email; // default for Bank Admin

            java.util.Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> mainBankOpt =
                    mainBankRepository.findFirstByPrimaryEmailAndStatusNot(email, "BLOCKED");
            if (mainBankOpt.isPresent()) {
                resolvedBankCode = mainBankOpt.get().getBankCode();
            } else {
                java.util.Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> baOpt =
                        branchAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(email, "BLOCKED");
                if (baOpt.isPresent()) {
                    resolvedBranchCode = baOpt.get().getBranchCode();
                    resolvedBankCode = branchBankRepository.findByBranchCode(resolvedBranchCode)
                            .map(bb -> mainBankRepository.findById(bb.getParentBankId())
                                    .map(bnk -> bnk.getBankCode())
                                    .orElse(null))
                            .orElse(null);
                    jwtSubject = baOpt.get().getUsername(); // Branch Admin: username as subject
                } else {
                    // Replacement bank admin: their email is not the bank's primaryEmail,
                    // so look them up directly in MainAdmin to get bankCode and real username.
                    java.util.Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> maOpt =
                            mainAdminRepository.findFirstByEmailAndStatusNot(email, "BLOCKED");
                    if (maOpt.isPresent() && maOpt.get().getBankCode() != null) {
                        resolvedBankCode = maOpt.get().getBankCode();
                        jwtSubject = maOpt.get().getUsername(); // use actual DB username, not email
                        logger.info("[OTP-VERIFY] Replacement bank admin detected — username='{}' bankCode='{}'",
                                jwtSubject, resolvedBankCode);
                    }
                }
            }
            logger.info("[OTP-VERIFY] bankCode={} branchCode={} jwtSubject={} for {}",
                    resolvedBankCode, resolvedBranchCode, jwtSubject, maskEmail(email));

            // ── JWT generate karo resolved subject se ──
            UserDetails userDetails = User.builder()
                    .username(jwtSubject)
                    .password("")
                    .authorities(new ArrayList<>())
                    .build();

            String jwtRole      = (resolvedBranchCode != null) ? "BRANCH_ADMIN" : "BANK_ADMIN";
            String accessToken  = jwtHelper.generateToken(userDetails, jwtRole);
            String refreshToken = jwtHelper.generateTokenForRefresh(jwtSubject);

            Map<String, Object> res = new HashMap<>();
            res.put("success",      true);
            res.put("message",      "OTP verified successfully.");
            res.put("accessToken",  accessToken);
            res.put("refreshToken", refreshToken);
            res.put("email",        email);
            res.put("bankCode",     resolvedBankCode);
            if (resolvedBranchCode != null) {
                res.put("branchCode", resolvedBranchCode);
            }

            return ResponseEntity.ok(res);

        } else if (result == OtpVerifyResult.INVALID) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("Invalid OTP. Please try again."));

        } else if (result == OtpVerifyResult.EXPIRED) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("OTP has expired. Please request a new one."));

        } else if (result == OtpVerifyResult.NOT_FOUND) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("No OTP found. Please request a new one."));

        } else if (result == OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED) {

            return ResponseEntity
                    .status(429)
                    .body(errorResponse("Too many incorrect attempts. Please request a new OTP."));

        } else {

            return ResponseEntity
                    .status(500)
                    .body(errorResponse("Something went wrong."));
        }
    }

    // ───────────────── RESEND OTP ─────────────────

    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email is required."));
        }

        try {
            otpService.resendOtp(email);
            logger.info("[OTP-RESEND] OTP resent successfully to: {}", maskEmail(email));

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "New OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);

        } catch (EmailDeliveryException e) {
            // OTP rollback already handled inside OtpService
            logger.error("[OTP-RESEND-FAIL] Email delivery failed for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again."));
        } catch (Exception e) {
            logger.error("[OTP-RESEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("An unexpected error occurred while resending OTP. Please try again."));
        }
    }

    // ───────────────── SET PASSWORD ─────────────────

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(
            @RequestBody Map<String, String> request) {

        try {
            String bankId   = request.get("bankId");
            String username = request.get("username");
            String email    = request.get("email");
            String password = request.get("password");

            System.out.println("======================================");
            System.out.println("Bank Admin PASSWORD SET");
            System.out.println("Bank ID  : " + bankId);
            System.out.println("Username : " + username);
            System.out.println("Email    : " + email);
            System.out.println("Password : " + password);
            System.out.println("======================================");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password Saved Successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(errorResponse(e.getMessage()));
        }
    }

    // ───────────────── HELPERS ─────────────────

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", message);
        return res;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "*****" + local.charAt(local.length() - 1) + domain;
    }
}