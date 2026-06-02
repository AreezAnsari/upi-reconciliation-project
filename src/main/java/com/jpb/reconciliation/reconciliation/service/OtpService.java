package com.jpb.reconciliation.reconciliation.service;

public interface OtpService {

    // ✅ Generate OTP only
    String generateOtp();

    // ✅ Generate + Send OTP
    void generateAndSendOtp(String email);
}