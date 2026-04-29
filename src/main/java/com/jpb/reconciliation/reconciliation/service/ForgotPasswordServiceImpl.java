package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime; 
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.entity.OtpManager;
import com.jpb.reconciliation.reconciliation.entity.TestPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.TestUser;
import com.jpb.reconciliation.reconciliation.repository.OtpManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.TestUserRepository;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);

    // OTP valid for 5 minutes
    private static final int OTP_EXPIRY_MINUTES = 5;

    // Password must have: min 8 chars, 1 uppercase, 1 number, 1 special character
    // Same rule as docs: "min 8 chars, 1 Caps, 1 Special char, 1 mathematical number"
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Autowired
    private TestUserRepository reconUserRepository;

    @Autowired
    private OtpManagerRepository otpManagerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────
    // STEP 1 — Forgot Password: validate email → generate OTP
    // ─────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ResponseDto> forgotPassword(ForgotPasswordRequest request) {

        // 1. Basic null check
        if (request.getEmailId() == null || request.getEmailId().trim().isEmpty()) {
            logger.warn("Forgot password called with empty email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Email ID is required."));
        }

        String emailId = request.getEmailId().trim().toLowerCase();

        // 2. Check if user exists with this email
        Optional<TestUser> userOptional = reconUserRepository.findByEmailId(emailId);
        if (!userOptional.isPresent()) {
            // For security: don't reveal if email exists or not
            logger.warn("Forgot password requested for non-existent email: {}", emailId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto("200",
                            "If this email is registered, an OTP has been sent to it."));
        }

        TestUser user = userOptional.get();

        // 3. Check user is active and approved
        if (!"active".equalsIgnoreCase(user.getUserStatus())) {
            logger.warn("Forgot password for inactive user: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Your account is not active. Please contact admin."));
        }

        // 4. Invalidate any previous unused OTPs for this email
        otpManagerRepository.invalidatePreviousOtps(emailId);
        logger.info("Previous OTPs invalidated for email: {}", emailId);

        // 5. Generate 6-digit OTP
        String otpCode = generateOtp();
        logger.info("OTP generated for email: {}", emailId);

        // 6. Save OTP to DB
        OtpManager otpManager = new OtpManager();
        otpManager.setEmailId(emailId);
        otpManager.setOtpCode(otpCode);
        otpManager.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpManager.setIsUsed("N");
        otpManager.setCreatedAt(LocalDateTime.now());
        otpManagerRepository.save(otpManager);
        logger.info("OTP saved in DB for email: {}", emailId);

        // 7. TODO: Send OTP via email when mail config is added to application.properties
        // emailService.sendOtpEmail(emailId, otpCode, user.getUserName());
        // For now OTP is logged — REMOVE this log in production!
        logger.info("======== OTP for {} is: {} (REMOVE IN PRODUCTION) ========", emailId, otpCode);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto("200",
                        "OTP has been sent to your registered email. Valid for " + OTP_EXPIRY_MINUTES + " minutes."));
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 2 — Reset Password: validate OTP + set new password
    // ─────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ResponseDto> resetPassword(ResetPasswordRequest request) {

        // 1. Null checks
        if (request.getEmailId() == null || request.getEmailId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Email ID is required."));
        }
        if (request.getOtpCode() == null || request.getOtpCode().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "OTP is required."));
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "New password is required."));
        }
        if (request.getConfirmNewPassword() == null || request.getConfirmNewPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Confirm password is required."));
        }

        String emailId = request.getEmailId().trim().toLowerCase();

        // 2. Check new password matches confirm password
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            logger.warn("Password and confirm password do not match for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "New password and confirm password do not match."));
        }

        // 3. Validate password strength (docs rule: min 8 chars, 1 uppercase, 1 special char, 1 number)
        if (!request.getNewPassword().matches(PASSWORD_REGEX)) {
            logger.warn("Password does not meet strength requirements for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400",
                            "Password must be at least 8 characters and include: " +
                            "1 uppercase letter, 1 number, and 1 special character (@$!%*?&)."));
        }

        // 4. Find user by email
        Optional<TestUser> userOptional = reconUserRepository.findByEmailId(emailId);
        if (!userOptional.isPresent()) {
            logger.warn("Reset password: user not found for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Invalid request. Please try forgot password again."));
        }

        TestUser user = userOptional.get();

        // 5. Find latest valid OTP for this email
        Optional<OtpManager> otpOptional = otpManagerRepository
                .findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(emailId, "N");

        if (!otpOptional.isPresent()) {
            logger.warn("No valid OTP found for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "No valid OTP found. Please request a new OTP."));
        }

        OtpManager otpManager = otpOptional.get();

        // 6. Check if OTP has expired
        if (LocalDateTime.now().isAfter(otpManager.getExpiryTime())) {
            otpManager.setIsUsed("Y");
            otpManagerRepository.save(otpManager);
            logger.warn("OTP expired for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "OTP has expired. Please request a new OTP."));
        }

        // 7. Check if OTP matches
        if (!otpManager.getOtpCode().equals(request.getOtpCode().trim())) {
            logger.warn("Invalid OTP entered for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400","Invalid OTP. Please check and try again."));
        }

        // 8. Mark OTP as used
        otpManager.setIsUsed("Y");
        otpManagerRepository.save(otpManager);
        logger.info("OTP verified and marked as used for email: {}", emailId);

        // 9. Update password in PasswordManager
        TestPasswordManager passwordManager = user.getPasswordManager();
        if (passwordManager == null) {
            logger.error("PasswordManager is null for user: {}", emailId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto("500", "Unable to reset password. Please contact admin."));
        }

        passwordManager.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        passwordManager.setExpirationDate(LocalDateTime.now());
        passwordManager.setCreatedAt(LocalDateTime.now());
        user.setPasswordManager(passwordManager);
        reconUserRepository.save(user);
        logger.info("Password successfully reset for email: {}", emailId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto("200", "Password has been reset successfully. You can now login."));
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // always 6 digits
        return String.valueOf(otp);
    }
}