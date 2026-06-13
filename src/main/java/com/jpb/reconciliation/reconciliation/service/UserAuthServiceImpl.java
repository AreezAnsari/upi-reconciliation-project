package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Auth flow for REC_USER table (AddUser entity).
 *
 * Mirrors KalSuperUserServiceImpl logic but operates on AddUser/REC_USER.
 *
 * Flow:
 *  CREATE USER (AddUserServiceImpl) → welcome email sent
 *      ↓
 *  STEP 1 — verifyCredentials (institutionCode + username + defaultPassword)
 *      ↓
 *  STEP 2 — setNewPassword (BCrypt encode, passwordSet=1, status=VERIFIED)
 *      ↓
 *  STEP 3 — login (password check → OTP sent)
 *      ↓
 *  STEP 4 — verifyOtpAndActivate (OTP check → status=ACTIVE)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final AddUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final EmailService      emailService;
    private final OtpService        otpService;   // existing OtpService used

    // =========================================================================
    // CHECK USER STATUS
    // POST /api/v1/user/auth/check-status
    // Called from the verify-email page to decide: NEW_USER or OLD_USER
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> checkUserStatus(UserVerifyCredentialsDto dto) {

        log.info("checkUserStatus — instCode={} username={}", dto.getInstitutionCode(), dto.getUsername());

        Optional<AddUser> optUser = userRepository
                .findByInstitutionCodeAndUsername(dto.getInstitutionCode(), dto.getUsername());

        if (!optUser.isPresent() ||
                optUser.get().getPasswordSet() == null ||
                optUser.get().getPasswordSet() != 1) {

            return ResponseEntity.ok(new RestWithStatusList(
                    "NEW_USER", "New user. Please complete setup.", null));
        }

        return ResponseEntity.ok(new RestWithStatusList(
                "OLD_USER", "Password already set. Please login.", null));
    }

    // =========================================================================
    // STEP 1 — verifyCredentials
    // POST /api/v1/user/auth/verify-credentials
    // Body: { institutionCode, username, defaultPassword }
    //
    // Compare defaultPassword against stored value.
    // If passwordSet=1 → ALREADY_VERIFIED (redirect to login).
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(UserVerifyCredentialsDto dto) {

        log.info("verifyCredentials — instCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (isBlank(dto.getInstitutionCode()) || isBlank(dto.getUsername())) {
            return badRequest("Institution Code and Username are required.");
        }

        Optional<AddUser> optUser = userRepository
                .findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode().trim(),
                        dto.getUsername().trim());

        if (!optUser.isPresent()) {
            log.warn("verifyCredentials — user not found: instCode={} username={}",
                    dto.getInstitutionCode(), dto.getUsername());
            return badRequest("Invalid Institution Code or Username. Please check your email.");
        }

        AddUser user = optUser.get();

        // Already verified → redirect to login
        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            log.info("verifyCredentials → ALREADY_VERIFIED for username={}", dto.getUsername());
            return ResponseEntity.ok(new RestWithStatusList(
                    "ALREADY_VERIFIED",
                    "Password already set. Please login directly.", null));
        }

        // Verify default password
        boolean passwordMatch = false;

        if (user.getPassword() != null && dto.getDefaultPassword() != null) {
            // Try BCrypt match first (password is stored encoded in AddUserServiceImpl)
            try {
                passwordMatch = passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword());
            } catch (Exception e) {
                log.warn("BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            // Fallback: plain text match (safety net)
            if (!passwordMatch && dto.getDefaultPassword().equals(user.getDefaultPassword())) {
                passwordMatch = true;
            }
        }

        if (!passwordMatch) {
            log.warn("verifyCredentials — password mismatch for username={}", dto.getUsername());
            return badRequest("Invalid Default Password. Please check your welcome email.");
        }

        log.info("verifyCredentials → SUCCESS for username={}", dto.getUsername());
        return ResponseEntity.ok(new RestWithStatusList(
                "SUCCESS",
                "Credentials verified. Please set your new password.",
                new ArrayList<>()));
    }

    // =========================================================================
    // STEP 2 — setNewPassword
    // POST /api/v1/user/auth/set-password
    // Body: { institutionCode, username, newPassword }
    //
    // BCrypt encode → save, passwordSet=1, status=VERIFIED
    // Clear defaultPassword (one-time use done)
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(UserSetPasswordDto dto) {

        log.info("setNewPassword — instCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<AddUser> optUser = userRepository
                .findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode().trim(),
                        dto.getUsername().trim());

        if (!optUser.isPresent()) {
            return notFound("User not found. Please verify credentials first.");
        }

        AddUser user = optUser.get();

        // Already set → do not allow overwrite via this flow
        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            log.info("setNewPassword → ALREADY_VERIFIED for username={}", dto.getUsername());
            return ResponseEntity.ok(new RestWithStatusList(
                    "ALREADY_VERIFIED",
                    "Password already set. Please login directly.", null));
        }

        // Encode and save new password
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordSet(1);
        user.setDefaultPassword(null);              // clear default password — done
        user.setStatus(AddUser.UserStatus.VERIFIED); // REQUEST → VERIFIED
        userRepository.save(user);

        log.info("setNewPassword → SUCCESS for username={}", dto.getUsername());
        return ResponseEntity.ok(new RestWithStatusList(
                "SUCCESS",
                "Password set successfully. Please login.", new ArrayList<>()));
    }

    // =========================================================================
    // STEP 3 — login
    // POST /api/v1/user/auth/login
    // Body: { institutionCode, username, password }
    //
    // Check password → send OTP → return masked email
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> login(UserLoginDto dto) {

        log.info("login — instCode={} username={}", dto.getInstitutionCode(), dto.getUsername());

        if (isBlank(dto.getUsername())) {
            return badRequest("Username is required.");
        }

        Optional<AddUser> optUser;

        if (!isBlank(dto.getInstitutionCode())) {
            optUser = userRepository.findByInstitutionCodeAndUsername(
                    dto.getInstitutionCode().trim(), dto.getUsername().trim());
        } else {
            optUser = userRepository.findFirstByUsername(dto.getUsername().trim());
        }

        if (!optUser.isPresent()) {
            return unauthorized("User not found. Please complete account setup first.");
        }

        AddUser user = optUser.get();

        // Must have set password first
        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return badRequest("Account setup incomplete. Please set your password first.");
        }

        // Status check
        AddUser.UserStatus status = user.getStatus();
        if (status == AddUser.UserStatus.BLOCK || status == AddUser.UserStatus.INACTIVE) {
            return forbidden("Account is " + status.name() + ". Please contact admin.");
        }

        // Password match
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("login — password mismatch for username={}", dto.getUsername());
            return unauthorized("Invalid password. Please try again.");
        }

        // Send OTP via existing OtpService
        String email = user.getEmail();
        try {
            otpService.generateAndSendOtp(email);
            log.info("Login OTP sent to: {}", email);
        } catch (Exception e) {
            log.error("OTP send failed for {}: {}", email, e.getMessage());
            return serverError("Failed to send OTP. Please try again.");
        }

        List<Object> data = new ArrayList<>();
        
        // data[0] = masked email for UI
        data.add(maskEmail(email));

        // data[1] = actual email for OTP verification
        data.add(email);
        return ResponseEntity.ok(new RestWithStatusList(
                "SUCCESS", "OTP sent to your registered email.", data));
    }

    // =========================================================================
    // STEP 4 — verifyOtpAndActivate
    // POST /api/v1/user/auth/verify-otp
    // Body: { email, otp }
    //
    // OTP verified by OtpService → status = ACTIVE
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> verifyOtpAndActivate(String email, String otp) {

        log.info("verifyOtpAndActivate — email={}", email);

        // Verify OTP via existing OtpService
        OtpService.OtpVerifyResult otpResult;

        try {
            otpResult = otpService.verifyOtp(email, otp);
        } catch (Exception e) {
            log.error("OTP verification error for {}: {}", email, e.getMessage());
            return badRequest("OTP verification failed. Please try again.");
        }

        if (otpResult != OtpService.OtpVerifyResult.SUCCESS) {
            return unauthorized("Invalid or expired OTP.");
        }

        // Find user and activate
        Optional<AddUser> optUser = userRepository.findFirstByEmailOrderByIdAsc(email);
        if (!optUser.isPresent()) {
            return notFound("User not found for email: " + email);
        }

        AddUser user = optUser.get();

        // BLOCK check — never activate a blocked user
        if (user.getStatus() == AddUser.UserStatus.BLOCK) {
            return forbidden("Account is blocked. Please contact admin.");
        }

        // VERIFIED → ACTIVE (first login activates)
        if (user.getStatus() == AddUser.UserStatus.VERIFIED) {
            user.setStatus(AddUser.UserStatus.ACTIVE);
            userRepository.save(user);
            log.info("User {} status → ACTIVE after first OTP verification", user.getUsername());
        }

        List<Object> data = new ArrayList<>();
        data.add(user.getUsername());
        data.add(user.getRole().name());
        data.add(user.getInstitutionCode());

        boolean firstLogin = false;

        if (user.getStatus() == AddUser.UserStatus.VERIFIED) {
            user.setStatus(AddUser.UserStatus.ACTIVE);
            userRepository.save(user);
            firstLogin = true;
        }

        return ResponseEntity.ok(
            new RestWithStatusList(
                "SUCCESS",
                firstLogin
                    ? "Account activated successfully."
                    : "Login successful.",
                data
            )
        );
    }

    // =========================================================================
    // FORGOT PASSWORD — Step A: Send OTP
    // POST /api/v1/user/auth/forgot-password
    // Body: { email } OR { institutionCode, username }
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(UserForgotPasswordRequest request) {

        log.info("forgotPassword — email={} username={}", request.getEmail(), request.getUsername());

        AddUser user = null;

        // Strategy 1: Find by email
        if (!isBlank(request.getEmail())) {
            Optional<AddUser> byEmail = userRepository.findFirstByEmail(request.getEmail().trim());
            if (byEmail.isPresent()) user = byEmail.get();
        }

        // Strategy 2: Find by institutionCode + username
        if (user == null && !isBlank(request.getUsername()) && !isBlank(request.getInstitutionCode())) {
            Optional<AddUser> byUsername = userRepository.findByInstitutionCodeAndUsername(
                    request.getInstitutionCode().trim(), request.getUsername().trim());
            if (byUsername.isPresent()) user = byUsername.get();
        }

        // Security: Always return SUCCESS to prevent enumeration attack
        if (user == null) {
            log.warn("forgotPassword — user not found");
            return ResponseEntity.ok(new RestWithStatusList(
                    "SUCCESS",
                    "If your credentials are valid, an OTP has been sent to your registered email.",
                    new ArrayList<>()));
        }

        if (user.getStatus() == AddUser.UserStatus.BLOCK) {
            return forbidden("Account is blocked. Please contact admin.");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP with 10-min expiry
        user.setForgotOtp(otp);
        user.setForgotOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Send OTP via email
        try {
            emailService.sendForgotPasswordOtp(user.getEmail(), user.getUsername(), otp, 10);
            log.info("Forgot-password OTP sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Forgot-password OTP email failed for {}: {}", user.getEmail(), e.getMessage());
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            userRepository.save(user);
            return serverError("Failed to send OTP. Please try again.");
        }

        List<Object> data = new ArrayList<>();
        data.add(maskEmail(user.getEmail()));

        return ResponseEntity.ok(new RestWithStatusList(
                "SUCCESS", "OTP sent to your registered email.", data));
    }

    // =========================================================================
    // FORGOT PASSWORD — Step B: Verify OTP + Reset Password
    // POST /api/v1/user/auth/reset-password
    // Body: { email OR (institutionCode + username), otp, newPassword, confirmNewPassword }
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(UserResetPasswordRequest request) {

        log.info("resetPassword — username={} email={}", request.getUsername(), request.getEmail());

        if (request.getNewPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return badRequest("Passwords do not match.");
        }

        AddUser user = null;

        if (!isBlank(request.getEmail())) {
            Optional<AddUser> byEmail = userRepository.findFirstByEmail(request.getEmail().trim());
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null && !isBlank(request.getUsername()) && !isBlank(request.getInstitutionCode())) {
            Optional<AddUser> byUsername = userRepository.findByInstitutionCodeAndUsername(
                    request.getInstitutionCode().trim(), request.getUsername().trim());
            if (byUsername.isPresent()) user = byUsername.get();
        }

        if (user == null) {
            return unauthorized("Invalid credentials.");
        }

        if (user.getForgotOtp() == null) {
            return badRequest("No OTP found. Please request a new one.");
        }

        if (LocalDateTime.now().isAfter(user.getForgotOtpExpiry())) {
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            userRepository.save(user);
            return badRequest("OTP has expired. Please request a new one.");
        }

        if (!user.getForgotOtp().trim().equals(request.getOtp().trim())) {
            return unauthorized("Invalid OTP.");
        }

        // Reset password + clear OTP
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(1);
        user.setForgotOtp(null);
        user.setForgotOtpExpiry(null);
        userRepository.save(user);

        log.info("Password reset successfully for user={}", user.getUsername());
        return ResponseEntity.ok(new RestWithStatusList(
                "SUCCESS", "Password reset successfully. Please login.", new ArrayList<>()));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local   = parts[0];
        String domain  = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        return local.substring(0, 2) + "***@" + domain;
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    // ── Response Helpers ──────────────────────────────────────────────────────

    private ResponseEntity<RestWithStatusList> badRequest(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<RestWithStatusList> unauthorized(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<RestWithStatusList> forbidden(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<RestWithStatusList> notFound(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<RestWithStatusList> serverError(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}