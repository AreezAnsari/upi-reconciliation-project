package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

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

        otpService.generateAndSendOtp(email);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "OTP sent to " + maskEmail(email));

        return ResponseEntity.ok(res);
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

        otpService.resendOtp(email);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "OTP resent to " + maskEmail(email));

        return ResponseEntity.ok(res);
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