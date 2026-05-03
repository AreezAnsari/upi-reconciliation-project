package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.entity.KalEmployeeAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalEmployeePassword;
import com.jpb.reconciliation.reconciliation.entity.OtpManager;
import com.jpb.reconciliation.reconciliation.entity.PasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.KalEmployeePasswordRepository;
import com.jpb.reconciliation.reconciliation.repository.KalEmployeeRepository;
import com.jpb.reconciliation.reconciliation.repository.OtpManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.PasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);

    // OTP valid for 5 minutes — reads from application-sit.yml → app.mail.otp-expiry-minutes
    @Value("${app.mail.otp-expiry-minutes:5}")
    private int OTP_EXPIRY_MINUTES;

    // Password validation: min 8 chars, 1 uppercase, 1 number, 1 special char
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Autowired
    private KalEmployeeRepository kalEmployeeRepository;

    @Autowired
    private KalEmployeePasswordRepository kalEmployeePasswordRepository;

    @Autowired
    private OtpManagerRepository otpManagerRepository;
    
    @Autowired
    private PasswordManagerRepository passwordManagerRepository;
    
    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────
    // STEP 1 — Forgot Password: validate email → generate OTP → send email
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ResponseDto> forgotPassword(ForgotPasswordRequest request) {

        // 1. Basic null check
        if (request.getEmailId() == null || request.getEmailId().trim().isEmpty()) {
            logger.warn("Forgot password called with empty email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Email ID is required."));
        }

        String emailId = request.getEmailId().trim().toLowerCase();

        Optional<KalEmployeeAdmin> userOptional1 = kalEmployeeRepository.findByEmailIgnoreCase(emailId);
        logger.info("findByEmailIgnoreCase result present: {}", userOptional1.isPresent());
        // 2. Check if user exists with this email
        Optional<KalEmployeeAdmin> userOptional = kalEmployeeRepository.findByEmailIgnoreCase(emailId);
        if (!userOptional.isPresent()) {
            logger.warn("Forgot password requested for non-existent email: {}", emailId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto("200",
                            "If this email is registered, an OTP has been sent to it."));
        }

        KalEmployeeAdmin user = userOptional.get();

        // 3. Check user is active
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            logger.warn("Forgot password attempted for inactive user: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Your account is not active. Please contact admin."));
        }

        // 4. Invalidate all previous unused OTPs for this email
        otpManagerRepository.invalidatePreviousOtps(emailId);
        logger.info("Previous OTPs invalidated for: {}", emailId);

        // 5. Generate fresh 6-digit OTP
        String otpCode = generateOtp();

        // 6. Save OTP to DB
        OtpManager otpManager = new OtpManager();
        otpManager.setEmailId(emailId);
        otpManager.setOtpCode(otpCode);
        otpManager.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpManager.setIsUsed("N");
        otpManager.setCreatedAt(LocalDateTime.now());
        otpManagerRepository.save(otpManager);
        logger.info("OTP saved to DB for: {}", emailId);

        // 7. Send real email via EmailService
        try {
            emailService.sendForgotPasswordOtp(
                emailId,
                user.getUsername(),
                otpCode,
                OTP_EXPIRY_MINUTES
            );
            logger.info("OTP email dispatched for: {}", emailId);
        } catch (Exception e) {
            logger.error("OTP email dispatch failed for {}: {}", emailId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto("500",
                            "OTP generated but email could not be sent. Please try again or contact support."));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto("200",
                        "OTP has been sent to your registered email. Valid for "
                        + OTP_EXPIRY_MINUTES + " minutes."));
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 2 — Reset Password: validate OTP → update password
    // ─────────────────────────────────────────────────────────────────────
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

        // 2. Password match check
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            logger.warn("Password mismatch on reset for: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "New password and confirm password do not match."));
        }

        // 3. Password strength validation
        if (!request.getNewPassword().matches(PASSWORD_REGEX)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400",
                            "Password must be at least 8 characters and include: "
                            + "1 uppercase letter, 1 number, and 1 special character (@$!%*?&)."));
        }

        // 4. Find user by email
        Optional<KalEmployeeAdmin> userOptional = kalEmployeeRepository.findByEmailIgnoreCase(emailId);
        if (!userOptional.isPresent()) {
            logger.warn("Reset password: no user found for email: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Invalid request. Please request a new OTP."));
        }

        KalEmployeeAdmin user = userOptional.get();

        // 5. Find latest valid (unused) OTP for this email
        Optional<OtpManager> otpOptional = otpManagerRepository
                .findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(emailId, "N");

        if (!otpOptional.isPresent()) {
            logger.warn("No valid OTP found for: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "No valid OTP found. Please request a new OTP."));
        }

        OtpManager otpManager = otpOptional.get();

        // 6. Check OTP expiry
        if (LocalDateTime.now().isAfter(otpManager.getExpiryTime())) {
            otpManager.setIsUsed("Y");
            otpManagerRepository.save(otpManager);
            logger.warn("Expired OTP used for: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "OTP has expired. Please request a new OTP."));
        }

        // 7. Check OTP matches
        if (!otpManager.getOtpCode().equals(request.getOtpCode().trim())) {
            logger.warn("Invalid OTP entered for: {}", emailId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto("400", "Invalid OTP. Please check and try again."));
        }

        // 8. Mark OTP as used — one-time use only
        otpManager.setIsUsed("Y");
        otpManagerRepository.save(otpManager);
        logger.info("OTP verified and consumed for: {}", emailId);

        // 9. Find and update password via KalEmployeePasswordRepository
        Optional<KalEmployeePassword> pwdOptional = kalEmployeePasswordRepository.findByKalEmployee(user);
        if (!pwdOptional.isPresent()) {
            logger.error("KalEmployeePassword not found for user: {}", emailId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto("500", "Unable to reset password. Please contact admin."));
        }

        KalEmployeePassword passwordRecord = pwdOptional.get();
        passwordRecord.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        passwordRecord.setExpirationDate(LocalDateTime.now().plusDays(90));
        passwordRecord.setUpdatedAt(LocalDateTime.now());
        kalEmployeePasswordRepository.save(passwordRecord);
        logger.info("Password reset successful for: {}", emailId);
        
        Optional<ReconUser> reconUserOptional = reconUserRepository.findByEmailId(emailId);
        if (reconUserOptional.isPresent()) {
            ReconUser reconUser = reconUserOptional.get();
            PasswordManager pwdManager = reconUser.getPasswordManager();
            if (pwdManager != null) {
                pwdManager.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
                pwdManager.setExpirationDate(LocalDateTime.now().plusDays(90));
                passwordManagerRepository.save(pwdManager);
                logger.info("PasswordManager (RCN) also updated for: {}", emailId);
            }
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto("200",
                        "Password has been reset successfully. You can now log in."));
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER — Generate 6-digit OTP
    // ─────────────────────────────────────────────────────────────────────
    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}