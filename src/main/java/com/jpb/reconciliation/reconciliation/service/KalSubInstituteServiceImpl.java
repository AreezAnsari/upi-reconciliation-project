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
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSubInstituteVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.KalVerifyEmailResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.KalSubInstitute;
import com.jpb.reconciliation.reconciliation.repository.KalSubInstituteRepository;

@Service
public class KalSubInstituteServiceImpl implements KalSubInstituteService {

    private static final Logger logger =
            LoggerFactory.getLogger(KalSubInstituteServiceImpl.class);

    @Autowired
    private KalSubInstituteRepository kalSubInstituteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpServiceImpl otpService;

    @Autowired
    private EmailService emailService;

    // =========================================================================
    // verifyEmail
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(
            String institutionCode, String username) {

        logger.info("verifyEmail — institutionCode={} username={}",
                institutionCode, username);

        if (institutionCode == null || institutionCode.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Institution code and username are required.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<KalSubInstitute> optUser =
                kalSubInstituteRepository.findByInstitutionCodeAndUsername(
                        institutionCode.trim(),
                        username.trim());

        String userStatus;

        if (optUser.isPresent()
                && optUser.get().getPasswordSet() != null
                && optUser.get().getPasswordSet() == 1) {

            userStatus = "OLD_USER";

        } else {

            userStatus = "NEW_USER";
        }

        KalVerifyEmailResponseDto responseDto =
                new KalVerifyEmailResponseDto(
                        userStatus,
                        institutionCode.trim(),
                        username.trim());

        List<Object> data = new ArrayList<>();
        data.add(responseDto);

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Email verified successfully.",
                        data),
                HttpStatus.OK);
    }

    // =========================================================================
    // checkUserStatus
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> checkUserStatus(
            KalSubInstituteVerifyDto dto) {

        Optional<KalSubInstitute> optUser =
                kalSubInstituteRepository.findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (!optUser.isPresent()
                || optUser.get().getPasswordSet() == null
                || optUser.get().getPasswordSet() != 1) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "NEW_USER",
                            "New user. Complete setup.",
                            null),
                    HttpStatus.OK);
        }

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "OLD_USER",
                        "Login directly.",
                        null),
                HttpStatus.OK);
    }

    // =========================================================================
    // verifyCredentials
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            KalSubInstituteVerifyDto dto) {

        if (dto.getInstitutionCode() == null
                || dto.getUsername() == null) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Institution Code and Username are required.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        String institutionCode = dto.getInstitutionCode().trim();
        String username = dto.getUsername().trim();

        Optional<KalSubInstitute> optUser =
                kalSubInstituteRepository.findByInstitutionCodeAndUsername(
                        institutionCode,
                        username);
        System.out.println("Institution Code = " + institutionCode);
        System.out.println("Username = " + username);
        System.out.println("User Found = " + optUser.isPresent());

        if (!optUser.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid Institution Code or Username.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        KalSubInstitute user = optUser.get();

        if (user.getPasswordSet() != null
                && user.getPasswordSet() == 1) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "ALREADY_VERIFIED",
                            "Password already set. Please login directly.",
                            null),
                    HttpStatus.OK);
        }

        boolean passwordMatch = false;

        if (user.getPassword() != null
                && dto.getDefaultPassword() != null) {

            try {

                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(),
                        user.getPassword());

            } catch (Exception e) {

                logger.warn("BCrypt check failed");
            }

            if (!passwordMatch) {

                passwordMatch =
                        dto.getDefaultPassword().equals(user.getPassword());
            }
        }

        if (!passwordMatch) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid Default Password.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Credentials verified. Please set your new password.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // setNewPassword
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(
            KalSubInstituteSetPasswordDto dto) {

        Optional<KalSubInstitute> optUser =
                kalSubInstituteRepository.findByInstitutionCodeAndUsername(
                        dto.getInstitutionCode().trim(),
                        dto.getUsername().trim());

        if (!optUser.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "User not found.",
                            null),
                    HttpStatus.NOT_FOUND);
        }

        KalSubInstitute user = optUser.get();

        if (user.getPasswordSet() != null
                && user.getPasswordSet() == 1) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "ALREADY_VERIFIED",
                            "Password already set.",
                            null),
                    HttpStatus.OK);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordSet(1);
        user.setStatus("VERIFIED");
        user.setUpdatedAt(LocalDateTime.now());

        kalSubInstituteRepository.save(user);

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Password set successfully.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // login
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(
            KalSubInstituteVerifyDto dto) {

        if (dto.getUsername() == null
                || dto.getUsername().trim().isEmpty()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Username is required.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<KalSubInstitute> optUser;

        if (dto.getInstitutionCode() != null
                && !dto.getInstitutionCode().trim().isEmpty()) {

            optUser =
                    kalSubInstituteRepository
                            .findByInstitutionCodeAndUsername(
                                    dto.getInstitutionCode().trim(),
                                    dto.getUsername().trim());

        } else {

            optUser =
                    kalSubInstituteRepository
                            .findByUsername(
                                    dto.getUsername().trim());
        }

        if (!optUser.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "User not found.",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        KalSubInstitute user = optUser.get();

        if (user.getPasswordSet() == null
                || user.getPasswordSet() != 1) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Please set password first.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

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

        try {

            // ✅ GENERATE OTP
            String otp = otpService.generateOtp();

            // ✅ SAVE OTP IN DB
            user.setForgotOtp(otp);

            user.setForgotOtpExpiry(
                    LocalDateTime.now().plusMinutes(5)
            );

            user.setUpdatedAt(LocalDateTime.now());

            kalSubInstituteRepository.save(user);

            // ✅ SEND OTP MAIL
            emailService.sendForgotPasswordOtp(
                    user.getEmail(),
                    user.getUsername(),
                    otp,
                    5
            );

        } catch (Exception e) {

            logger.error(
                    "Failed to send OTP: {}",
                    e.getMessage()
            );

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Failed to send OTP.",
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Object> data = new ArrayList<>();

        data.add(user.getEmail());

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "OTP sent successfully.",
                        data),
                HttpStatus.OK);
    }

    // =========================================================================
    // forgotPassword
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(
            ForgotPasswordRequest request) {

        logger.info("forgotPassword — email={}",
                request.getEmailId());

        KalSubInstitute user = null;

        if (request.getEmailId() != null
                && !request.getEmailId().trim().isEmpty()) {

            Optional<KalSubInstitute> byEmail =
                    kalSubInstituteRepository.findFirstByEmail(
                            request.getEmailId().trim());

            if (byEmail.isPresent()) {
                user = byEmail.get();
            }
        }

        if (user == null) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "SUCCESS",
                            "If your credentials are valid, an OTP has been sent.",
                            new ArrayList<>()),
                    HttpStatus.OK);
        }

        String otp = generateOtp();

        user.setForgotOtp(otp);
        user.setForgotOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setUpdatedAt(LocalDateTime.now());

        kalSubInstituteRepository.save(user);

        try {

            emailService.sendForgotPasswordOtp(
                    user.getEmail(),
                    user.getUsername(),
                    otp,
                    10);

        } catch (Exception e) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Failed to send OTP.",
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "OTP sent successfully.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // resetPassword
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(
            ResetPasswordRequest request) {

        logger.info("resetPassword — email={}",
                request.getEmailId());

        if (request.getNewPassword() == null
                || !request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Passwords do not match.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<KalSubInstitute> byEmail =
                kalSubInstituteRepository.findFirstByEmail(
                        request.getEmailId().trim());

        if (!byEmail.isPresent()) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid credentials.",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        KalSubInstitute user = byEmail.get();

        if (user.getForgotOtp() == null) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "No OTP found.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now()
                .isAfter(user.getForgotOtpExpiry())) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "OTP expired.",
                            null),
                    HttpStatus.BAD_REQUEST);
        }

        if (!user.getForgotOtp()
                .equals(request.getOtpCode().trim())) {

            return new ResponseEntity<>(
                    new RestWithStatusList(
                            "FAILURE",
                            "Invalid OTP.",
                            null),
                    HttpStatus.UNAUTHORIZED);
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        user.setForgotOtp(null);
        user.setForgotOtpExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());

        kalSubInstituteRepository.save(user);

        return new ResponseEntity<>(
                new RestWithStatusList(
                        "SUCCESS",
                        "Password reset successfully.",
                        new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    public String generateOtp() {

        return String.format("%06d",
                new Random().nextInt(1000000));
    }
}