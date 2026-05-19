package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.KalSuperUser;
import com.jpb.reconciliation.reconciliation.entity.TestInstitution;
import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;
import com.jpb.reconciliation.reconciliation.repository.KalSuperUserRepository;
import com.jpb.reconciliation.reconciliation.repository.TestInstitutionRepository;

@Service
public class KalSuperServiceImp implements KalSuperService {

    private Logger logger =
            LoggerFactory.getLogger(KalSuperServiceImp.class);

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

    @Autowired
    private KalSuperUserRepository kalSuperUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    // ─────────────────────────────────────────────────────────────────────
    // STEP 1 — Verify default credentials from TEST_INSTITUTION
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            KalSuperUserVerifyDto dto) {

        Optional<TestInstitution> optInst =
                testInstitutionRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode(),
                                dto.getUsername());

        if (!optInst.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid Institution Code or Super User ID",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        TestInstitution institution = optInst.get();

        if (!dto.getDefaultPassword()
                .equals(institution.getDefaultPassword())) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid Default Password",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Credentials verified successfully.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 2 — Set new password and save in KAL_SUPER_USER
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(
            KalSuperUserSetPasswordDto dto) {

        // ✅ Get institution from TEST_INSTITUTION
        Optional<TestInstitution> optInst =
                testInstitutionRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode(),
                                dto.getUsername());

        if (!optInst.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Institution not found.",
                            null),
                    HttpStatus.NOT_FOUND);
        }

        TestInstitution institution = optInst.get();

        // ✅ Get email from TEST_INSTITUTION
        String email = institution.getPrimaryEmail();

        // ✅ Check existing user
        Optional<KalSuperUser> optUser =
                kalSuperUserRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode(),
                                dto.getUsername());

        KalSuperUser superUser;

        if (!optUser.isPresent()) {

            superUser = new KalSuperUser();

            superUser.setInstitutionCode(
                    dto.getInstitutionCode());

            // ✅ DB mandatory column
            superUser.setUsername(
                    dto.getUsername());

            // ✅ Actual required field
            superUser.setSuperUserId(
                    dto.getUsername());

            superUser.setCreatedAt(
                    LocalDateTime.now());

            logger.info(
                    "Creating new Super User: {}",
                    dto.getUsername());

        } else {

            superUser = optUser.get();

            logger.info(
                    "Updating existing Super User: {}",
                    dto.getUsername());
        }

        // ✅ Required fields only
        superUser.setEmail(email);

        superUser.setPassword(
                passwordEncoder.encode(
                        dto.getNewPassword()));

        superUser.setStatus("ACTIVE");

        // ✅ Save user
        kalSuperUserRepository.save(superUser);

        logger.info(
                "Super User saved successfully: {}",
                dto.getUsername());

        // ✅ Sir's rule: VERIFIED status is set HERE — after successful new password setup
        // This is the correct moment: user proved identity (Step 1) AND set new password (Step 2)
        // Token also nullified here — link is now dead (success path)
        if ("REQUEST".equals(institution.getStatus())) {
            institution.setStatus("VERIFIED");
            logger.info("Institution {} status set to VERIFIED after successful password setup",
                    dto.getInstitutionCode());
        }

        // ✅ default_password null karo — kaam khatam, dobara use nahi hoga
        institution.setDefaultPassword(null);

        // ✅ Token null karo — link expired on success (still valid 36hrs on failure path)
        institution.setVerificationToken(null);
        institution.setTokenExpiry(LocalDateTime.now()); // expire immediately on success

        institution.setUpdatedAt(LocalDateTime.now());
        testInstitutionRepository.save(institution);

        logger.info("default_password and token cleared for institution: {}",
                dto.getInstitutionCode());

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Password set successfully. Please login with your new password.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 3 — Login and send OTP
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> login(
            KalSuperUserVerifyDto dto) {

        Optional<KalSuperUser> optUser =
                kalSuperUserRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode(),
                                dto.getUsername());

        if (!optUser.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "User not found. Please set password first.",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        KalSuperUser user = optUser.get();

        // ── Check institution status before allowing login ──
        Optional<TestInstitution> optInst =
                testInstitutionRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode(),
                                dto.getUsername());

        if (optInst.isPresent()) {
            String instStatus = optInst.get().getStatus();

            if ("RETIRED".equals(instStatus)) {
                return new ResponseEntity<>(
                        new RestWithStatusList(
                                "FAILURE",
                                "This institution has been permanently retired. Access is no longer possible.",
                                null),
                        HttpStatus.FORBIDDEN);
            }

            if ("BLOCKED".equals(instStatus)) {
                return new ResponseEntity<>(
                        new RestWithStatusList(
                                "FAILURE",
                                "This institution is currently blocked. Please contact KalInfotech Admin.",
                                null),
                        HttpStatus.FORBIDDEN);
            }

            if ("INACTIVE".equals(instStatus)) {
                return new ResponseEntity<>(
                        new RestWithStatusList(
                                "FAILURE",
                                "This institution is inactive. Please contact KalInfotech Admin.",
                                null),
                        HttpStatus.FORBIDDEN);
            }
        }

        // ✅ Verify password
        if (!passwordEncoder.matches(
                dto.getDefaultPassword(),
                user.getPassword())) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid password.",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        String email = user.getEmail();

        try {

            otpService.generateAndSendOtp(email);
            // OTP is atomic — if this line is reached, email was delivered successfully
            logger.info("[OTP-OK] OTP sent successfully to: {}", email);

        } catch (EmailDeliveryException e) {
            // OTP already rolled back inside OtpService.generateAndSendOtp()
            logger.error("[OTP-FAIL] Email delivery failed for: {} | reason: {}", email, e.getMessage());
            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "We could not deliver the OTP to " + email + ". " +
                            "Please verify the email address is correct and try again.",
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            logger.error("[OTP-FAIL] Unexpected error while sending OTP to: {} | reason: {}", email, e.getMessage());
            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "An unexpected error occurred while sending OTP. Please try again.",
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ✅ Return email to frontend
        java.util.List<Object> data = new ArrayList<>();

        data.add(email);

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "OTP sent successfully.",
                        data),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 3.5 — After OTP verified → Set institution status ACTIVE
    // Called from OtpController after successful OTP verification
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> activateInstitution(String email) {

        // Find KalSuperUser by email
        Optional<KalSuperUser> optUser =
                kalSuperUserRepository.findByEmail(email);

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Super User not found for email: " + email, null),
                    HttpStatus.NOT_FOUND);
        }

        KalSuperUser user = optUser.get();

        Optional<TestInstitution> optInst =
        	    testInstitutionRepository.findByInstitutionCodeAndSuperUserId(
        	        user.getInstitutionCode(),
        	        user.getSuperUserId()   // ya user.getUsername() — jo column map hai
        	    );

        if (!optInst.isPresent()) {
            logger.warn("Institution not found for activateInstitution: {}", email);
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "Login successful.", new ArrayList<>()),
                    HttpStatus.OK);
        }

        TestInstitution institution = optInst.get();

        // RETIRED check — never change
        if ("RETIRED".equals(institution.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Retired institution cannot be activated.", null),
                    HttpStatus.FORBIDDEN);
        }

        // Set ACTIVE only if currently VERIFIED
        // (INACTIVE and BLOCKED should NOT become ACTIVE automatically)
        if ("VERIFIED".equals(institution.getStatus())) {
            institution.setStatus("ACTIVE");
            institution.setUpdatedAt(LocalDateTime.now());
            testInstitutionRepository.save(institution);
            logger.info("Institution {} status set to ACTIVE after first login",
                    institution.getInstitutionCode());
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Institution activated successfully.", new ArrayList<>()),
                HttpStatus.OK);
    }
}