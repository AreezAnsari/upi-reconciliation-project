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

        // ✅ default_password null karo — kaam khatam, dobara use nahi hoga
        institution.setDefaultPassword(null);
        testInstitutionRepository.save(institution);

        logger.info(
                "default_password cleared for institution: {}",
                dto.getInstitutionCode());

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Password set successfully. Please login.",
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

            logger.info(
                    "OTP sent successfully to: {}",
                    email);

        } catch (Exception e) {

            logger.error(
                    "OTP send failed: {}",
                    e.getMessage());

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Failed to send OTP.",
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
}