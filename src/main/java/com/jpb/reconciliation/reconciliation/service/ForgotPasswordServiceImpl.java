package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.entity.PasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Generate OTP and send email
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmailId().trim().toLowerCase();

        Optional<ReconUser> userOpt = reconUserRepository.findByEmailId(email);
        if (!userOpt.isPresent()) {
            logger.warn("Forgot password requested for unregistered email: {}", email);
            return ResponseEntity.ok(
                new ForgotPasswordResponseDto("404",
                    "This email address is not registered in our system. " +
                    "Please check and try again, or contact your administrator.")
            );
        }

        ReconUser user = userOpt.get();

        String otpCode = null;
        try {
            // Generate and store OTP FIRST — we'll roll it back if delivery fails
            otpCode = otpService.generateOtpForEmail(email);

            // Attempt email delivery — throws RuntimeException on failure
            emailService.sendForgotPasswordOtp(email, user.getUserName(), otpCode, otpService.getOtpExpiryMinutes());

            logger.info("OTP generated and delivered successfully to: {}", email);

        } catch (Exception e) {
            // ── Roll back: remove the undelivered OTP from memory ─────────────
            // Without this, a user could guess/brute-force an OTP that was
            // never actually delivered to their inbox.
            if (otpCode != null) {
                otpService.invalidateOtp(email);
                logger.warn("OTP invalidated for {} after failed email delivery.", email);
            }
            logger.error("Failed to send OTP to {}: {}", email, e.getMessage());
            return ResponseEntity.ok(
                new ForgotPasswordResponseDto("500",
                    "We were unable to deliver the OTP to your email address. " +
                    "Please check that your email is correct and try again. " +
                    "If the issue persists, contact support@kalinfotech.com")
            );
        }

        return ResponseEntity.ok(
            new ForgotPasswordResponseDto("200", "OTP sent successfully to " + email)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Verify OTP and reset password
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ForgotPasswordResponseDto> resetPassword(ResetPasswordRequest request) {
        String email      = request.getEmailId().trim().toLowerCase();
        String otp        = request.getOtpCode().trim();
        String newPwd     = request.getNewPassword();
        String confirmPwd = request.getConfirmNewPassword();

        if (!newPwd.equals(confirmPwd)) {
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto("400", "Passwords do not match."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);
        switch (result) {
            case SUCCESS:
                break;
            case EXPIRED:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto("400", "OTP has expired. Please request a new one.")
                );
            case INVALID:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto("400", "Invalid OTP. Please check and try again.")
                );
            case MAX_ATTEMPTS_EXCEEDED:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto("400", "Too many incorrect attempts. Please request a new OTP.")
                );
            case NOT_FOUND:
            default:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto("400", "OTP not found or expired. Please request a new one.")
                );
        }

        Optional<ReconUser> userOpt = reconUserRepository.findByEmailId(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.ok(new ForgotPasswordResponseDto("400", "User not found."));
        }

        ReconUser user = userOpt.get();
        PasswordManager pm = user.getPasswordManager();
        pm.setUserPassword(passwordEncoder.encode(newPwd));
        pm.setExpirationDate(LocalDateTime.now());
        user.setPasswordManager(pm);
        reconUserRepository.save(user);

        logger.info("Password reset successfully for: {}", email);
        return ResponseEntity.ok(
            new ForgotPasswordResponseDto("200", "Password reset successful.")
        );
    }
}
