package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.UserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(@RequestBody Map<String, String> body) {
        return userAuthService.verifyCredentials(body.get("bankCode"), body.get("username"), body.get("defaultPassword"));
    }

    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(@RequestBody Map<String, String> body) {
        return userAuthService.setPassword(body.get("bankCode"), body.get("username"), body.get("newPassword"));
    }

    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(@RequestBody Map<String, String> body) {
        return userAuthService.login(body.get("bankCode"), body.get("username"), body.get("password"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<RestWithStatusList> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        return userAuthService.verifyOtp(email, otp);
    }

    @PostMapping("/check-status")
    public ResponseEntity<RestWithStatusList> checkStatus(@RequestBody Map<String, String> body) {
        return userAuthService.checkStatus(body.get("email"), body.get("username"));
    }

    @PostMapping("/account-status")
    public ResponseEntity<RestWithStatusList> getAccountStatus(@RequestBody Map<String, String> body) {
        return userAuthService.getAccountStatus(body.get("email"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(@RequestBody Map<String, String> body) {
        return userAuthService.forgotPassword(body.get("email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<RestWithStatusList> resetPassword(@RequestBody Map<String, String> body) {
        return userAuthService.resetPassword(
                body.get("email"),
                body.get("otp"),
                body.get("newPassword"),
                body.get("confirmNewPassword"));
    }
}
