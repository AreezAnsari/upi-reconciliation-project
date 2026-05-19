package com.jpb.reconciliation.reconciliation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Autowired
    private JavaMailSender mailSender;

    // Temporary in-memory store — no DB table needed
    // Key = email (lowercase), Value = OtpEntry
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    // ─── Generate & Send OTP — ATOMIC ────────────────────────────────────────
    // OTP is stored first, then email is attempted.
    // If email delivery fails, the OTP is removed (rollback) so an
    // undelivered OTP can never be brute-forced or accidentally used.
    public void generateAndSendOtp(String email) {
        String key = email.toLowerCase();
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Store OTP tentatively
        otpStore.put(key, new OtpEntry(otp, expiry, 0));

        try {
            sendOtpEmail(email, otp);
            logger.info("[OTP-OK] OTP generated and delivered to: {}", key);
        } catch (Exception e) {
            // ── Rollback: remove undelivered OTP from memory ──────────────────
            otpStore.remove(key);
            logger.error("[OTP-ROLLBACK] OTP removed for {} after email delivery failure: {}", key, e.getMessage());
            throw new EmailDeliveryException(
                "OTP could not be delivered to " + email + ". Please verify the email address and try again.",
                email, e);
        }
    }

    // Generates + stores OTP but returns the code so caller can send via EmailService
    public String generateOtpForEmail(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStore.put(email.toLowerCase(), new OtpEntry(otp, expiry, 0));
        return otp;
    }

    public int getOtpExpiryMinutes() {
        return OTP_EXPIRY_MINUTES;
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────────

    public OtpVerifyResult verifyOtp(String email, String submittedOtp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            return OtpVerifyResult.NOT_FOUND;
        }
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(key);
            return OtpVerifyResult.EXPIRED;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(key);
            return OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED;
        }
        if (!entry.otp.equals(submittedOtp)) {
            entry.attempts++;
            return OtpVerifyResult.INVALID;
        }

        otpStore.remove(key); // one-time use — remove after success
        return OtpVerifyResult.SUCCESS;
    }

    // ─── Resend OTP ──────────────────────────────────────────────────────────

    public void resendOtp(String email) {
        generateAndSendOtp(email); // resets expiry and attempts
    }

    // ─── Invalidate OTP (call when email delivery fails) ─────────────────────
    // Removes the OTP from memory so an undelivered OTP cannot be used later.
    public void invalidateOtp(String email) {
        otpStore.remove(email.toLowerCase());
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // always 6 digits
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@reconxpert.ai");
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai - Your OTP for Login");
            helper.setText(buildEmailHtml(otp), true);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP login email — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email to: " + toEmail, toEmail, e);
        }
    }

    // Java 8 compatible — plain string concatenation instead of text blocks
    private String buildEmailHtml(String otp) {
        return "<div style='font-family: Arial, sans-serif; max-width: 480px; margin: auto; padding: 32px; border: 1px solid #e5e7eb; border-radius: 8px;'>"
             + "  <div style='text-align: center; margin-bottom: 24px;'>"
             + "    <span style='font-size: 20px; font-weight: 600; color: #1e3a5f;'>ReconXpert.Ai</span><br/>"
             + "    <span style='font-size: 12px; color: #6b7280;'>by KalInfotech</span>"
             + "  </div>"
             + "  <p style='color: #374151; font-size: 15px;'>Your One-Time Password (OTP) for login is:</p>"
             + "  <div style='text-align: center; margin: 24px 0;'>"
             + "    <span style='display: inline-block; font-size: 36px; font-weight: 700; letter-spacing: 12px;"
             + "                 color: #1e3a5f; background: #f0f4ff; padding: 16px 28px; border-radius: 8px;'>"
             + otp
             + "    </span>"
             + "  </div>"
             + "  <p style='color: #6b7280; font-size: 13px;'>This OTP is valid for <strong>5 minutes</strong> and can only be used once.</p>"
             + "  <p style='color: #6b7280; font-size: 13px;'>If you did not request this, please ignore this email.</p>"
             + "  <hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'/>"
             + "  <p style='color: #9ca3af; font-size: 11px; text-align: center;'>ReconXpert.Ai | KalInfotech | Do not reply to this email</p>"
             + "</div>";
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;
        int attempts;

        OtpEntry(String otp, LocalDateTime expiry, int attempts) {
            this.otp = otp;
            this.expiry = expiry;
            this.attempts = attempts;
        }
    }

    public enum OtpVerifyResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        NOT_FOUND,
        MAX_ATTEMPTS_EXCEEDED
    }
}