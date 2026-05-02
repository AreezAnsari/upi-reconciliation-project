package com.jpb.reconciliation.reconciliation.service;

/**
 * EmailService — sends transactional emails from ReconXpert.Ai
 *
 * All methods are fire-and-forget — they throw RuntimeException on failure
 * so callers can decide whether to propagate or swallow.
 */
public interface EmailService {

    /**
     * Send OTP email for Forgot Password flow.
     *
     * @param toEmail    recipient email address
     * @param userName   recipient's username (shown in email greeting)
     * @param otpCode    the 6-digit OTP
     * @param expiryMins how many minutes before OTP expires
     */
    void sendForgotPasswordOtp(String toEmail, String userName, String otpCode, int expiryMins);
}