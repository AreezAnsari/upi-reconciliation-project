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
@CrossOrigin(origins = "*")
public class OtpController {

    @Autowired
    private OtpService otpService;

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

            Map<String, Object> res = new HashMap<>();

            res.put("success", true);

            res.put("message", "OTP sent to " + maskEmail(email));

            return ResponseEntity.ok(res);

        } catch (Exception e) {

            return ResponseEntity
                    .status(500)
                    .body(errorResponse("Failed to send OTP. Please try again."));
        }
    }

    // ───────────────── VERIFY OTP ─────────────────

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        String otp = body.get("otp");

        if (email == null || otp == null) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email and OTP are required."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);

        if (result == OtpVerifyResult.SUCCESS) {

            Map<String, Object> res = new HashMap<>();

            res.put("success", true);

            res.put("message", "OTP verified successfully.");

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

            Map<String, Object> res = new HashMap<>();

            res.put("success", true);

            res.put("message", "New OTP sent to " + maskEmail(email));

            return ResponseEntity.ok(res);

        } catch (Exception e) {

            return ResponseEntity
                    .status(500)
                    .body(errorResponse("Failed to resend OTP."));
        }
    }

    // ───────────────── SET PASSWORD ─────────────────
    // NO DATABASE SAVE
    // ONLY SUCCESS RESPONSE + CONSOLE PRINT

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(
            @RequestBody Map<String, String> request) {

        try {

            String institutionId = request.get("institutionId");

            String username = request.get("username");

            String email = request.get("email");

            String password = request.get("password");

            System.out.println("======================================");

            System.out.println("SUPER USER PASSWORD SET");

            System.out.println("Institution ID : " + institutionId);

            System.out.println("Username       : " + username);

            System.out.println("Email          : " + email);

            System.out.println("Password       : " + password);

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

        if (atIndex <= 2) {

            return email;
        }

        String local = email.substring(0, atIndex);

        String domain = email.substring(atIndex);

        return local.charAt(0)
                + "*****"
                + local.charAt(local.length() - 1)
                + domain;
    }
}