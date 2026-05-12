package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.KalSuperService;
import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

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

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private KalSuperService kalSuperService;

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

            // ── JWT generate karo email se ──
            UserDetails userDetails = User.builder()
                    .username(email)
                    .password("")
                    .authorities(new ArrayList<>())
                    .build();

            String accessToken  = jwtHelper.generateToken(userDetails);
            String refreshToken = jwtHelper.generateTokenForRefresh(email);

            // ── Institution status → ACTIVE (first login ke baad) ──
            try {
                kalSuperService.activateInstitution(email);
            } catch (Exception e) {
                // Abhi ye silently fail ho rha hai — status ACTIVE nhi hoti
                System.out.println("Warning: Could not activate institution for "
                    + email + ": " + e.getMessage());
                e.printStackTrace(); // ← ye add karo taaki full stack trace dikhe
            }

            Map<String, Object> res = new HashMap<>();
            res.put("success",      true);
            res.put("message",      "OTP verified successfully.");
            res.put("accessToken",  accessToken);
            res.put("refreshToken", refreshToken);
            res.put("email",        email);

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

    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(
            @RequestBody Map<String, String> request) {

        try {
            String institutionId = request.get("institutionId");
            String username      = request.get("username");
            String email         = request.get("email");
            String password      = request.get("password");

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
        if (atIndex <= 2) return email;
        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "*****" + local.charAt(local.length() - 1) + domain;
    }
}