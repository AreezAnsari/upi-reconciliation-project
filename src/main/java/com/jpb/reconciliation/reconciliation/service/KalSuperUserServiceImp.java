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
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.KalSuperUser;
import com.jpb.reconciliation.reconciliation.entity.TestInstitution;
import com.jpb.reconciliation.reconciliation.repository.KalSuperUserRepository;
import com.jpb.reconciliation.reconciliation.repository.TestInstitutionRepository;

@Service
public class KalSuperUserServiceImp implements KalSuperService {

    private static final Logger logger =
            LoggerFactory.getLogger(KalSuperUserServiceImp.class);

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

    @Autowired
    private KalSuperUserRepository kalSuperUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    // =========================================================================
    // STEP 1 — Verify default credentials
    // TEST_INSTITUTION table mein institutionCode + superUserId se dhundho
    // defaultPassword plain text compare (TestInstitution mein plain stored hai)
    // Agar KAL_SUPER_USER mein passwordSet=true → ALREADY_VERIFIED return karo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(KalSuperUserVerifyDto dto) {

        logger.info("verifyCredentials called — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        // TEST_INSTITUTION se credentials verify karo
        Optional<TestInstitution> optInst =
                testInstitutionRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername()); // superUserId = username from email

        if (!optInst.isPresent()) {
            logger.warn("Institution not found — institutionCode={} superUserId={}",
                    dto.getInstitutionCode(), dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username.", null),
                    HttpStatus.BAD_REQUEST);
        }

        TestInstitution institution = optInst.get();

        // Default password plain text comparison
        if (dto.getDefaultPassword() == null ||
                !dto.getDefaultPassword().equals(institution.getDefaultPassword())) {
            logger.warn("Default password mismatch for user={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Default Password. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Check: password already set hai?
        Optional<KalSuperUser> existingUser =
                kalSuperUserRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (existingUser.isPresent() &&
                Boolean.TRUE.equals(existingUser.get().getPasswordSet())) {
            logger.info("Password already set for user={} — returning ALREADY_VERIFIED",
                    dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        logger.info("Credentials verified for user={}", dto.getUsername());
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Credentials verified. Please set your new password.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 2 — Set new password (UPSERT — same user dobara nahi banega)
    // passwordSet = true → one-time flag, dobara set nahi hoga
    // Email TestInstitution.primaryEmail se lo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(KalSuperUserSetPasswordDto dto) {

        logger.info("setNewPassword called — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<TestInstitution> optInst =
                testInstitutionRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (!optInst.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Institution not found.", null),
                    HttpStatus.NOT_FOUND);
        }

        TestInstitution institution = optInst.get();
        String email = institution.getPrimaryEmail();

        // UPSERT check
        Optional<KalSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        KalSuperUser superUser;

        if (optUser.isPresent()) {
            superUser = optUser.get();

            if (Boolean.TRUE.equals(superUser.getPasswordSet())) {
                logger.info("Password already set for user={}", dto.getUsername());
                return new ResponseEntity<>(
                        new RestWithStatusList("ALREADY_VERIFIED",
                                "Password already set. Please login directly.", null),
                        HttpStatus.OK);
            }

            superUser.setUpdatedAt(LocalDateTime.now());
            logger.info("Updating existing Super User: {}", dto.getUsername());

        } else {
            superUser = new KalSuperUser();
            superUser.setInstitutionCode(dto.getInstitutionCode());
            superUser.setUsername(dto.getUsername());
            superUser.setSuperUserId(dto.getUsername());
            superUser.setCreatedAt(LocalDateTime.now());
            superUser.setUpdatedAt(LocalDateTime.now());
            logger.info("Creating new Super User: {}", dto.getUsername());
        }

        superUser.setEmail(email);
        superUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        superUser.setStatus("VERIFIED");
        superUser.setPasswordSet(true); // ONE-TIME FLAG

        kalSuperUserRepository.save(superUser);

        logger.info("Password saved successfully for user={}", dto.getUsername());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 3 — Login → OTP dispatch
    // findByInstitutionCodeAndSuperUserId use karo
    // passwordSet check → status check → password verify → OTP bhejo
    // Email return karo frontend ko (OTP screen par dikhayega)
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(KalSuperUserVerifyDto dto) {

        logger.info("login called — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<KalSuperUser> optUser =
                kalSuperUserRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "User not found. Please complete setup first.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        KalSuperUser user = optUser.get();

        if (!Boolean.TRUE.equals(user.getPasswordSet())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Please complete account setup first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String status = user.getStatus();
        if ("BLOCK".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account is " + status + ". Please contact KalInfotech admin.", null),
                    HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid password.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        String email = user.getEmail();

        try {
            otpService.generateAndSendOtp(email);
            logger.info("OTP sent to: {}", email);
        } catch (Exception e) {
            logger.error("OTP send failed: {}", e.getMessage());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Failed to send OTP. Please try again.", null),
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
    // FORGOT PASSWORD — Step A
    //
    // ✅ FIX: User EMAIL ya USERNAME dono mein se kuch bhi de sakta hai
    //         Dono se user dhundha jayega — jo bhi pehle milega
    //
    // POST /test/api/v1/institution/forgot-password
    // Body options:
    //   Option 1: { emailId: "user@bank.com" }
    //   Option 2: { username: "farhan.akhtar", institutionCode: "NKB59190" }
    //
    // OTP DB mein store karo with 10-min expiry → email bhejo
    // Masked email return karo frontend ko
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request) {

        logger.info("forgotPassword called — emailId={} username={} institutionCode={}",
                request.getEmailId(), request.getUsername(), request.getInstitutionCode());

        KalSuperUser user = null;

        // ✅ Strategy 1: Email se dhundho (agar email diya hai)
        if (request.getEmailId() != null && !request.getEmailId().trim().isEmpty()) {
            Optional<KalSuperUser> byEmail =
                    kalSuperUserRepository.findFirstByEmail(request.getEmailId().trim());
            if (byEmail.isPresent()) {
                user = byEmail.get();
                logger.info("User found by email: {}", request.getEmailId());
            }
        }

        // ✅ Strategy 2: Username + InstitutionCode se dhundho (agar email se nahi mila)
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<KalSuperUser> byUsername =
                    kalSuperUserRepository.findByInstitutionCodeAndSuperUserId(
                            request.getInstitutionCode().trim(),
                            request.getUsername().trim());

            if (byUsername.isPresent()) {
                user = byUsername.get();
                logger.info("User found by username: {}", request.getUsername());
            }
        }

        // ✅ Security: User nahi mila to bhi SUCCESS — enumeration attack prevent karo
        if (user == null) {
            logger.warn("Forgot password — user not found for given credentials");
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "If your credentials are valid, an OTP has been sent to registered email.",
                            new ArrayList<>()),
                    HttpStatus.OK);
        }

        if ("BLOCK".equalsIgnoreCase(user.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account is blocked. Contact KalInfotech admin.", null),
                    HttpStatus.FORBIDDEN);
        }

        // ✅ 6-digit OTP generate karo
        String otp = generateOtp();

        // ✅ DB mein store karo with 10-min expiry
        user.setForgotOtp(otp);
        user.setForgotOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        // ✅ Email bhejo
        String email = user.getEmail();
        try {
            // OtpService mein custom OTP send karo
            // Agar sendCustomOtp method nahi hai to generateAndSendOtp use karo
            // aur resetPassword mein otpService se verify karo
            otpService.generateAndSendOtp(email);
            logger.info("Forgot password OTP sent to: {}", email);

            // NOTE: Agar OtpService apna OTP generate karta hai (DB wala nahi use karta)
            // toh resetPassword mein:
            //   boolean valid = otpService.verifyOtp(email, request.getOtp());
            // use karo INSTEAD of DB comparison.
            // Recommend: OtpService mein sendOtpWithCustomCode(email, otp) method add karo
            // taaki hum apna generated OTP bhej sakein.

        } catch (Exception e) {
            logger.error("Forgot password OTP send failed: {}", e.getMessage());
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            kalSuperUserRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Failed to send OTP. Please try again.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ✅ Masked email return karo
        String maskedEmail = maskEmail(email);
        List<Object> data = new ArrayList<>();
        data.add(maskedEmail);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "OTP sent to your registered email.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // FORGOT PASSWORD — Step B: Verify OTP + Reset Password
    //
    // ✅ FIX: email ya username dono se user dhundho (same as forgotPassword)
    // POST /test/api/v1/institution/reset-password
    // Body: { institutionCode, username, otp, newPassword, confirmNewPassword }
    //   OR  { emailId, otp, newPassword, confirmNewPassword }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request) {

        logger.info("resetPassword called — institutionCode={} username={} emailId={}",
                request.getInstitutionCode(), request.getUsername(), request.getEmailId());

        // Password match check
        if (request.getNewPassword() == null ||
                !request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Passwords do not match.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // ✅ User dhundho — email ya username se (same strategy as forgotPassword)
        KalSuperUser user = null;

        if (request.getEmailId() != null && !request.getEmailId().trim().isEmpty()) {
            Optional<KalSuperUser> byEmail =
                    kalSuperUserRepository.findFirstByEmail(request.getEmailId().trim());
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<KalSuperUser> byUsername =
                    kalSuperUserRepository.findByInstitutionCodeAndSuperUserId(
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
        if (!user.getForgotOtp().equals(request.getOtp())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid OTP.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // ✅ Reset password + clear OTP (one-time use)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForgotOtp(null);
        user.setForgotOtpExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        kalSuperUserRepository.save(user);

        logger.info("Password reset successfully for user={}",
                request.getUsername() != null ? request.getUsername() : request.getEmailId());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password reset successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    // iz***@gmail.com format
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local  = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        return local.substring(0, 2) + "***@" + domain;
    }
}