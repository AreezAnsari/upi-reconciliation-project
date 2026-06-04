package com.jpb.reconciliation.reconciliation.service;

public interface EmailService {

    void sendForgotPasswordOtp(
            String email,
            String username,
            String otp,
            int expiryMinutes
    );
}