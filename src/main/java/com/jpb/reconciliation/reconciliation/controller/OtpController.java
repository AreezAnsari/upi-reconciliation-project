package com.jpb.reconciliation.reconciliation.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.service.OtpService;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin("*")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/verify")
    public Map<String, Object> verifyOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        OtpService.OtpVerifyResult result =
                otpService.verifyOtp(email, otp);

        Map<String, Object> response = new HashMap<>();

        if (result == OtpService.OtpVerifyResult.SUCCESS) {
            response.put("success", true);
            response.put("message", "OTP verified");
        } else {
            response.put("success", false);
            response.put("message", "Invalid OTP");
        }

        return response;
    }
}