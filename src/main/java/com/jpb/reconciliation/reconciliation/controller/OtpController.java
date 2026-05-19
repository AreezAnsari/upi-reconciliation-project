package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin("*")
public class OtpController {

    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtHelper jwtHelper;

    // ───────── SEND OTP ─────────
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(error("Email is required"));
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
                    .body(error("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again. " +
                            "If the problem persists, contact support@kalinfotech.com"));
        } catch (Exception e) {
            logger.error("[OTP-SEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(error("An unexpected error occurred. Please try again."));
        }
    }

    // ───────── VERIFY OTP + JWT ─────────
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(error("Email and OTP required"));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);

        if (result == OtpVerifyResult.SUCCESS) {

            UserDetails userDetails = User.builder()
                    .username(email)
                    .password("")
                    .authorities(new ArrayList<>())
                    .build();

            String accessToken  = jwtHelper.generateToken(userDetails);
            String refreshToken = jwtHelper.generateTokenForRefresh(email);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "OTP verified");
            res.put("accessToken", accessToken);
            res.put("refreshToken", refreshToken);
            res.put("email", email);

            return ResponseEntity.ok(res);
        }

        return ResponseEntity.status(400).body(error("Invalid OTP"));
    }

    // ───────── RESEND OTP ─────────
    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");

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
                    .body(error("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again."));
        } catch (Exception e) {
            logger.error("[OTP-RESEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(error("An unexpected error occurred while resending OTP. Please try again."));
        }
    }

    // ───────── SET PASSWORD ─────────
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody Map<String, String> request) {

        System.out.println("PASSWORD SET REQUEST:");
        System.out.println(request);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Password saved successfully");

        return ResponseEntity.ok(res);
    }

    // ───────── HELPERS ─────────
    private Map<String, Object> error(String msg) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", msg);
        return res;
    }

    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at < 2) return email;
        return email.charAt(0) + "*****" + email.charAt(at - 1) + email.substring(at);
    }
}