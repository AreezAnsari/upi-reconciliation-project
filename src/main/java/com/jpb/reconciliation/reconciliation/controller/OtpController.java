package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "*") // update to your frontend URL in production
public class OtpController {

    @Autowired
    private OtpService otpService;

    // ─── POST /api/otp/send ───────────────────────────────────────────────────
    // Call this right after credentials are verified successfully
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("Email is required."));
        }

        try {
            otpService.generateAndSendOtp(email);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse("Failed to send OTP. Please try again."));
        }
    }

    // ─── POST /api/otp/verify ─────────────────────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(errorResponse("Email and OTP are required."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);

        // Java 8 compatible — if/else instead of switch expression
        if (result == OtpVerifyResult.SUCCESS) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "OTP verified successfully.");
            return ResponseEntity.ok(res);

        } else if (result == OtpVerifyResult.INVALID) {
            return ResponseEntity.status(400).body(errorResponse("Invalid OTP. Please try again."));

        } else if (result == OtpVerifyResult.EXPIRED) {
            return ResponseEntity.status(400).body(errorResponse("OTP has expired. Please request a new one."));

        } else if (result == OtpVerifyResult.NOT_FOUND) {
            return ResponseEntity.status(400).body(errorResponse("No OTP found. Please request a new one."));

        } else if (result == OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED) {
            return ResponseEntity.status(429).body(errorResponse("Too many incorrect attempts. Please request a new OTP."));

        } else {
            return ResponseEntity.status(500).body(errorResponse("Something went wrong."));
        }
    }

    // ─── POST /api/otp/resend ─────────────────────────────────────────────────
    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("Email is required."));
        }

        try {
            otpService.resendOtp(email);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "New OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorResponse("Failed to resend OTP."));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("message", message);
        return res;
    }

    private String maskEmail(String email) {
        // e.g. rajesh.garg@sbi.com → r*****g@sbi.com
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "*****" + local.charAt(local.length() - 1) + domain;
    }
}