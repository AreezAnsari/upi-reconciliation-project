package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.KalVerifyEmailResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.SubSuperUser;
import com.jpb.reconciliation.reconciliation.repository.KalSuperUserRepository;

@Service
public class KalSuperUserServiceImpl implements KalSuperService {

    private static final Logger logger =
            LoggerFactory.getLogger(KalSuperUserServiceImpl.class);

    @Autowired
    private KalSuperUserRepository kalSuperUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    // =========================================================================
    // verifyEmail
    // GET /test/api/v1/institution/verify-email?institutionCode=xxx&username=yyy
    //
    // KAL_SUPER_USER mein dhundho:
    //   - Record nahi / passwordSet=false → NEW_USER
    //   - passwordSet=true                → OLD_USER
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(
            String institutionCode, String username) {

        logger.info("verifyEmail — institutionCode={} username={}",
                institutionCode, username);

        if (institutionCode == null || institutionCode.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Institution code and username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<SubSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndUsername(
                        institutionCode.trim(),
                        username.trim());

        String userStatus;

        if (optUser.isPresent()
                && optUser.get().getPasswordSet() != null
                && optUser.get().getPasswordSet() == 1) {

            userStatus = "OLD_USER";

            logger.info("verifyEmail → OLD_USER for username={}", username);

        } else {

            userStatus = "NEW_USER";

            logger.info("verifyEmail → NEW_USER for username={}", username);
        }

        KalVerifyEmailResponseDto responseDto =
                new KalVerifyEmailResponseDto(
                        userStatus,
                        institutionCode.trim(),
                        username.trim());

        List<Object> data = new ArrayList<>();
        data.add(responseDto);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Email verified successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // checkUserStatus
    // POST /test/api/v1/institution/check-user-status
    // Body: { institutionCode, username }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> checkUserStatus(KalSuperUserVerifyDto dto) {

        logger.info("checkUserStatus — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<SubSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (!optUser.isPresent() ||
                optUser.get().getPasswordSet() == null ||
                optUser.get().getPasswordSet() != 1){
            return new ResponseEntity<>(
                    new RestWithStatusList("NEW_USER",
                            "New user. Complete setup.", null),
                    HttpStatus.OK);
        }

        return new ResponseEntity<>(
                new RestWithStatusList("OLD_USER",
                        "Login directly.", null),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 1 — verifyCredentials
    // POST /test/api/v1/institution/verify-credentials
    // Body: { institutionCode, username, defaultPassword }
    //
    // KAL_SUPER_USER mein dhundho → default password compare karo
    // passwordSet=true → ALREADY_VERIFIED
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(KalSuperUserVerifyDto dto) {

        logger.info("verifyCredentials — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (dto.getInstitutionCode() == null || dto.getUsername() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Institution Code and Username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String institutionCode = dto.getInstitutionCode().trim();
        String username        = dto.getUsername().trim();

        Optional<SubSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndUsername(
                        institutionCode, username);

        if (!optUser.isPresent()) {
            logger.warn("verifyCredentials — user not found: institutionCode={} username={}",
                    institutionCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        SubSuperUser user = optUser.get();

        // Already verified — seedha login par bhejo
        if (user.getPasswordSet() != null &&
                user.getPasswordSet() == 1) {
            logger.info("verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // Default password verify karo
        // ✅ FIX: dto variables use karo — 'code' aur 'username' undefined tha
        boolean passwordMatch = false;

        if (user.getPassword() != null && dto.getDefaultPassword() != null) {
            // Case A: BCrypt encoded (agar admin ne encoded store kiya)
            try {
                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(), user.getPassword());
            } catch (Exception e) {
                logger.warn("BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            // Case B: Plain text (agar admin ne plain text store kiya)
            if (!passwordMatch) {
                passwordMatch = dto.getDefaultPassword().equals(user.getPassword());
            }
        }

        if (!passwordMatch) {
            // ✅ FIX: 'code' variable undefined tha — dto variables use karo
            logger.warn("verifyCredentials — password mismatch for institutionCode={} username={}",
                    institutionCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Default Password. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        logger.info("verifyCredentials → SUCCESS for username={}", username);
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Credentials verified. Please set your new password.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 2 — setNewPassword
    // POST /test/api/v1/institution/set-password
    // Body: { institutionCode, username, newPassword }
    //
    // BCrypt encode karke save karo, passwordSet=true, status=VERIFIED
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(KalSuperUserSetPasswordDto dto) {

        logger.info("setNewPassword — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<SubSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode().trim(),
                        dto.getUsername().trim());

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "User not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        SubSuperUser user = optUser.get();

        // Already set — dobara set nahi hoga
        if (user.getPasswordSet() != null &&
                user.getPasswordSet() == 1) {
            logger.info("setNewPassword → ALREADY_VERIFIED for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordSet(1);   // ONE-TIME FLAG
        user.setStatus("VERIFIED");
        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        logger.info("setNewPassword → SUCCESS for username={}", dto.getUsername());
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 3 — login → OTP bhejo
    // POST /test/api/v1/institution/login
    // Body: { institutionCode, username, defaultPassword }
    //
    // institutionCode optional — agar empty/null to username se dhundho
    // Password verify → OTP bhejo → email return karo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(KalSuperUserVerifyDto dto) {

        logger.info("login — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<SubSuperUser> optUser;

        // institutionCode available ho to exact lookup karo
        if (dto.getInstitutionCode() != null && !dto.getInstitutionCode().trim().isEmpty()) {
            optUser = kalSuperUserRepository.findByInstitutionCodeAndUsername(
                    dto.getInstitutionCode().trim(),
                    dto.getUsername().trim());
        } else {
            // Sirf username se dhundho (direct login case)
            optUser = kalSuperUserRepository.findByUsername(dto.getUsername().trim());
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "User not found. Please complete account setup first.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        SubSuperUser user = optUser.get();

        // passwordSet check
        if (user.getPasswordSet() == null ||
                user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Status check
        String status = user.getStatus();
        if ("BLOCK".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account is " + status.toUpperCase() + ". Please contact admin.", null),
                    HttpStatus.FORBIDDEN);
        }

        // Password match
        if (!passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword())) {
            logger.warn("login — password mismatch for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid password. Please try again.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // OTP bhejo
        String email = user.getEmail();
        try {
            otpService.generateAndSendOtp(email);
            logger.info("OTP sent to: {}", email);
        } catch (Exception e) {
            logger.error("OTP send failed for {}: {}", email, e.getMessage());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Failed to send OTP. Please try again.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        List<Object> data = new ArrayList<>();
        data.add(email);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // FORGOT PASSWORD — Step A: OTP bhejo
    // POST /test/api/v1/institution/forgot-password
    // Body: { email }  OR  { institutionCode, username }
    //
    // ✅ FIX: OTP actually send karo — pehle sirf logger tha
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request) {

        logger.info("forgotPassword — email={} username={} institutionCode={}",
                request.getEmail(), request.getUsername(), request.getInstitutionCode());

        SubSuperUser user = null;

        // Strategy 1: Email se dhundho
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<SubSuperUser> byEmail =
                    kalSuperUserRepository.findFirstByEmail(request.getEmail().trim());
            if (byEmail.isPresent()) {
                user = byEmail.get();
                logger.info("forgotPassword — user found by email: {}", request.getEmail());
            }
        }

        // Strategy 2: institutionCode + username se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<SubSuperUser> byUsername =
                    kalSuperUserRepository.findByInstitutionCodeAndUsername(
                            request.getInstitutionCode().trim(),
                            request.getUsername().trim());
            if (byUsername.isPresent()) {
                user = byUsername.get();
                logger.info("forgotPassword — user found by username: {}", request.getUsername());
            }
        }

        // Security: user nahi mila to bhi SUCCESS (enumeration attack prevent)
        if (user == null) {
            logger.warn("forgotPassword — user not found");
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "If your credentials are valid, an OTP has been sent to your registered email.",
                            new ArrayList<>()),
                    HttpStatus.OK);
        }

        if ("BLOCK".equalsIgnoreCase(user.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account is blocked. Contact KalInfotech admin.", null),
                    HttpStatus.FORBIDDEN);
        }

        // 6-digit OTP generate karo
        String otp = generateOtp();

        // DB mein store karo — 10 min expiry
        user.setForgotOtp(otp);
        user.setForgotOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        // ✅ FIX: OTP actually send karo (pehle sirf logger tha — email nahi jaati thi)
        String email = user.getEmail();
        try {
            emailService.sendForgotPasswordOtp(email, user.getUsername(), otp, 10);
            logger.info("Forgot password OTP sent to: {}", email);
        } catch (Exception e) {
            logger.error("Forgot password OTP send failed for {}: {}", email, e.getMessage());
            // OTP clear karo agar email fail ho
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            kalSuperUserRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Failed to send OTP. Please try again.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Masked email return karo
        String maskedEmail = maskEmail(email);
        List<Object> data = new ArrayList<>();
        data.add(maskedEmail);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "OTP sent to your registered email.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // FORGOT PASSWORD — Step B: OTP verify + password reset
    // POST /test/api/v1/institution/reset-password
    // Body: { email, otp, newPassword, confirmNewPassword }
    //   OR  { institutionCode, username, otp, newPassword, confirmNewPassword }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request) {

        logger.info("resetPassword — institutionCode={} username={} email={}",
                request.getInstitutionCode(), request.getUsername(), request.getEmail());

        if (request.getNewPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Passwords do not match.", null),
                    HttpStatus.BAD_REQUEST);
        }

        SubSuperUser user = null;

        // Email se dhundho
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<SubSuperUser> byEmail =
                    kalSuperUserRepository.findFirstByEmail(request.getEmail().trim());
            if (byEmail.isPresent()) user = byEmail.get();
        }

        // Username + institutionCode se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<SubSuperUser> byUsername =
                    kalSuperUserRepository.findByInstitutionCodeAndUsername(
                            request.getInstitutionCode().trim(),
                            request.getUsername().trim());
            if (byUsername.isPresent()) user = byUsername.get();
        }

        if (user == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid credentials.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // OTP null check
        if (user.getForgotOtp() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "No OTP found. Please request a new one.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Expiry check
        if (LocalDateTime.now().isAfter(user.getForgotOtpExpiry())) {
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            kalSuperUserRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "OTP has expired. Please request a new one.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // OTP match
        if (!user.getForgotOtp().trim().equals(request.getOtp().trim())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid OTP.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // Password reset + OTP clear (one-time use)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForgotOtp(null);
        user.setForgotOtpExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        logger.info("Password reset successfully for user={}",
                request.getUsername() != null ? request.getUsername() : request.getEmail());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password reset successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    // ✅ FIX: generateOtp() method add kiya — pehle missing tha
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local   = parts[0];
        String domain  = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        return local.substring(0, 2) + "***@" + domain;
    }
}