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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.security.JwtHelper;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyEmailResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;

@Service
public class MainAdminServiceImpl implements MainAdminService {

    private static final Logger logger =
            LoggerFactory.getLogger(MainAdminServiceImpl.class);

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MainBankRepository mainBankRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtHelper jwtHelper;

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

        // TEST_INSTITUTION is source of truth — defaultPassword==null means password already set
        Optional<MainBank> optInst =
                mainBankRepository.findByInstitutionCodeAndSuperUserId(
                        institutionCode.trim(),
                        username.trim());

        String userStatus;

        if (optInst.isPresent() && optInst.get().getDefaultPassword() == null) {
            userStatus = "OLD_USER";
            logger.info("verifyEmail → OLD_USER for username={}", username);
        } else {
            userStatus = "NEW_USER";
            logger.info("verifyEmail → NEW_USER for username={}", username);
        }

        MainAdminVerifyEmailResponseDto responseDto =
                new MainAdminVerifyEmailResponseDto(
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
    public ResponseEntity<RestWithStatusList> checkUserStatus(MainAdminVerifyDto dto) {

        logger.info("checkUserStatus — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        // TEST_INSTITUTION is source of truth — defaultPassword==null means password already set
        Optional<MainBank> optInst =
                mainBankRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode(),
                        dto.getUsername());

        if (!optInst.isPresent() || optInst.get().getDefaultPassword() != null) {
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
    public ResponseEntity<RestWithStatusList> verifyCredentials(MainAdminVerifyDto dto) {

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

        // TEST_INSTITUTION is source of truth for default credential verification
        Optional<MainBank> optInst =
                mainBankRepository.findByInstitutionCodeAndSuperUserId(
                        institutionCode, username);

        if (!optInst.isPresent()) {
            logger.warn("verifyCredentials — institution not found: institutionCode={} username={}",
                    institutionCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        MainBank institution = optInst.get();

        // defaultPassword==null means password was already set → direct login
        if (institution.getDefaultPassword() == null) {
            logger.info("verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // Verify default password — Case A: BCrypt (new institutions), Case B: plaintext (old institutions)
        boolean passwordMatch = false;
        if (dto.getDefaultPassword() != null && institution.getDefaultPassword() != null) {
            try {
                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(), institution.getDefaultPassword());
            } catch (Exception e) {
                logger.warn("BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            if (!passwordMatch) {
                passwordMatch = dto.getDefaultPassword().equals(institution.getDefaultPassword());
            }
        }

        if (!passwordMatch) {
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
    public ResponseEntity<RestWithStatusList> setNewPassword(MainAdminSetPasswordDto dto) {

        logger.info("setNewPassword — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        // TEST_INSTITUTION is source of truth — validate first
        Optional<MainBank> optInst =
                mainBankRepository.findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode().trim(),
                        dto.getUsername().trim());

        if (!optInst.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Institution not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        MainBank institution = optInst.get();

        // defaultPassword==null means password was already set — cannot set again
        if (institution.getDefaultPassword() == null) {
            logger.info("setNewPassword → ALREADY_VERIFIED for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // INSERT new record into KAL_SUPER_USER (first-time password setup)
        MainAdmin mainAdmin = new MainAdmin();
        mainAdmin.setInstitutionCode(dto.getInstitutionCode().trim());
        mainAdmin.setUsername(dto.getUsername().trim());
        mainAdmin.setEmail(institution.getPrimaryEmail());
        mainAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        mainAdmin.setPasswordSet(1);
        mainAdmin.setStatus("VERIFIED");
        mainAdmin.setCreatedAt(LocalDateTime.now());
        mainAdmin.setCreatedBy(institution.getCreatedBy()); // admin username from TEST_INSTITUTION
        mainAdminRepository.save(mainAdmin);
        logger.info("KAL_SUPER_USER record created for username={} institutionCode={}",
                dto.getUsername(), dto.getInstitutionCode());

        // Update TEST_INSTITUTION → VERIFIED, wipe defaultPassword & token
        institution.setStatus("VERIFIED");
        institution.setDefaultPassword(null);       // default password null — kaam khatam
        institution.setVerificationToken(null);     // link dead on success
        institution.setTokenExpiry(LocalDateTime.now());
        institution.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(institution);
        logger.info("Institution {} status → VERIFIED after password setup", dto.getInstitutionCode());

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
    public ResponseEntity<RestWithStatusList> login(MainAdminVerifyDto dto) {

        logger.info("login — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<MainAdmin> optUser = Optional.empty();

        // Primary lookup: institutionCode + username = composite identity
        // Same username can exist at different institutions — institutionCode disambiguates
        if (dto.getInstitutionCode() != null && !dto.getInstitutionCode().trim().isEmpty()) {
            String enteredCode = dto.getInstitutionCode().trim();
            String enteredUser = dto.getUsername().trim();

            // Step 1: Composite lookup (works when KAL_SUPER_USER.institution_code is correct)
            optUser = mainAdminRepository.findByInstitutionCodeAndUsername(enteredCode, enteredUser);

            // Step 2: TEST_INSTITUTION bridge — handles stale/wrong institution_code in KAL_SUPER_USER
            // TEST_INSTITUTION is authoritative; bridge via primary_email.
            // Skip BLOCKED records so re-onboarded users with the same email can log in.
            if (!optUser.isPresent()) {
                logger.warn("login — composite miss, trying MainBank bridge for institutionCode={} username={}",
                        enteredCode, enteredUser);
                Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> instOpt =
                        mainBankRepository.findByInstitutionCodeAndSuperUserId(enteredCode, enteredUser);
                if (instOpt.isPresent()) {
                    String primaryEmail = instOpt.get().getPrimaryEmail();
                    if (primaryEmail != null && !primaryEmail.trim().isEmpty()) {
                        // Prefer non-BLOCKED record — re-onboarding creates a 2nd record with same email
                        optUser = mainAdminRepository.findFirstByEmailAndStatusNot(primaryEmail.trim(), "BLOCKED");
                        if (optUser.isPresent()) {
                            logger.info("login — MainBank bridge hit via email={} for username={}", primaryEmail, enteredUser);
                        } else {
                            logger.warn("login — MainBank bridge: no KAL_SUPER_USER entry for email={}", primaryEmail);
                        }
                    }
                } else {
                    logger.warn("login — MainBank bridge miss: institutionCode={} superUserId={} not found",
                            enteredCode, enteredUser);
                }
            }

            // Step 3: Legacy fallback — old records where institution_code was stored as NULL
            if (!optUser.isPresent()) {
                Optional<MainAdmin> legacy =
                        mainAdminRepository.findFirstByUsernameAndInstitutionCodeIsNull(enteredUser);
                if (legacy.isPresent()) {
                    Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> parentInst =
                            mainBankRepository.findFirstBySuperUserId(enteredUser);
                    if (parentInst.isPresent() && enteredCode.equals(parentInst.get().getInstitutionCode())) {
                        optUser = legacy;
                        logger.info("login — legacy NULL-code record matched for username={}", enteredUser);
                    } else {
                        logger.warn("login — all strategies exhausted for institutionCode={} username={}", enteredCode, enteredUser);
                    }
                }
            }
        } else {
            logger.warn("login — institutionCode missing for username={}", dto.getUsername());
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        MainAdmin user = optUser.get();

        // passwordSet check
        if (user.getPasswordSet() == null ||
                user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Status check from KAL_SUPER_USER (synced with TEST_INSTITUTION) ──
        String userStatus = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(userStatus) || "BLOCK".equalsIgnoreCase(userStatus)) {
            logger.warn("login BLOCKED: institution={} username={}", dto.getInstitutionCode(), dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your institution account is currently inactive. Please contact the KalInfotech administrator to reactivate your account.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your institution account has been scheduled for permanent block. Please contact the KalInfotech administrator immediately to avoid losing access.",
                            null),
                    HttpStatus.OK);
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
        mainAdminRepository.save(user);

        List<Object> data = new ArrayList<>();
        data.add(email);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // DIRECT LOGIN — institutionCode + username + password → JWT (no OTP)
    // POST /test/api/v1/institution/direct-login
    // Body: { institutionCode, username, defaultPassword }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> directLogin(MainAdminVerifyDto dto) {

        logger.info("directLogin — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        // institutionCode required
        if (dto.getInstitutionCode() == null || dto.getInstitutionCode().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Institution Code is required.", null),
                    HttpStatus.BAD_REQUEST);
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }
        if (dto.getDefaultPassword() == null || dto.getDefaultPassword().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Password is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String enteredCode = dto.getInstitutionCode().trim();
        String enteredUser = dto.getUsername().trim();

        // Step 1: Composite lookup
        Optional<MainAdmin> optUser = mainAdminRepository.findByInstitutionCodeAndUsername(
                enteredCode, enteredUser);

        // Step 2: TEST_INSTITUTION bridge — handles stale/wrong institution_code in KAL_SUPER_USER
        // Skip BLOCKED records so re-onboarded users with the same email can log in.
        if (!optUser.isPresent()) {
            logger.warn("directLogin — composite miss, trying MainBank bridge for institutionCode={} username={}",
                    enteredCode, enteredUser);
            Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> instOpt =
                    mainBankRepository.findByInstitutionCodeAndSuperUserId(enteredCode, enteredUser);
            if (instOpt.isPresent()) {
                String primaryEmail = instOpt.get().getPrimaryEmail();
                if (primaryEmail != null && !primaryEmail.trim().isEmpty()) {
                    // Prefer non-BLOCKED record — re-onboarding creates a 2nd record with same email
                    optUser = mainAdminRepository.findFirstByEmailAndStatusNot(primaryEmail.trim(), "BLOCKED");
                    if (optUser.isPresent()) {
                        logger.info("directLogin — MainBank bridge hit via email={} for username={}", primaryEmail, enteredUser);
                    }
                }
            }
        }

        // Step 3: Legacy fallback — old records where institution_code was stored as NULL
        if (!optUser.isPresent()) {
            Optional<MainAdmin> legacy =
                    mainAdminRepository.findFirstByUsernameAndInstitutionCodeIsNull(enteredUser);
            if (legacy.isPresent()) {
                Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> parentInst =
                        mainBankRepository.findFirstBySuperUserId(enteredUser);
                if (parentInst.isPresent() && enteredCode.equals(parentInst.get().getInstitutionCode())) {
                    optUser = legacy;
                    logger.info("directLogin — legacy NULL-code record matched for username={}", enteredUser);
                } else {
                    logger.warn("directLogin — all strategies exhausted for institutionCode={} username={}", enteredCode, enteredUser);
                }
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        MainAdmin user = optUser.get();

        // Password set hona chahiye
        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Status check from KAL_SUPER_USER (synced with TEST_INSTITUTION) ──
        String status = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(status) || "BLOCK".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your institution account is currently inactive. Please contact the KalInfotech administrator to reactivate your account.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your institution account has been scheduled for permanent block. Please contact the KalInfotech administrator immediately to avoid losing access.",
                            null),
                    HttpStatus.OK);
        }

        // Password verify
        if (!passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword())) {
            logger.warn("directLogin — password mismatch for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid password. Please try again.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // JWT generate karo — username as subject
        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .password("")
                .authorities(new ArrayList<>())
                .build();

        String accessToken  = jwtHelper.generateToken(userDetails);
        String refreshToken = jwtHelper.generateTokenForRefresh(user.getUsername());

        // Institution code bhi return karo
        List<Object> data = new ArrayList<>();
        data.add(user.getUsername());
        data.add(user.getInstitutionCode());
        data.add(accessToken);
        data.add(refreshToken);

        logger.info("directLogin — success for username={} institutionCode={}",
                user.getUsername(), user.getInstitutionCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Login successful.", data),
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

        MainAdmin user = null;

        // Strategy 1: Email se dhundho — skip BLOCKED so re-onboarded user can reset password
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<MainAdmin> byEmail =
                    mainAdminRepository.findFirstByEmailAndStatusNot(request.getEmail().trim(), "BLOCKED");
            if (byEmail.isPresent()) {
                user = byEmail.get();
                logger.info("forgotPassword — user found by email: {}", request.getEmail());
            }
        }

        // Strategy 2: institutionCode + username se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<MainAdmin> byUsername =
                    mainAdminRepository.findByInstitutionCodeAndUsername(
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
        mainAdminRepository.save(user);

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
            mainAdminRepository.save(user);
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
    // FORGOT PASSWORD — Step B: OTP sirf verify karo (password reset nahi)
    // POST /test/api/v1/institution/verify-forgot-otp
    // Body: { email, otp }  OR  { institutionCode, username, otp }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequest request) {

        logger.info("verifyForgotOtp — email={} username={}", request.getEmail(), request.getUsername());

        MainAdmin user = null;

        // Skip BLOCKED records so re-onboarded user can verify OTP
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<MainAdmin> byEmail = mainAdminRepository.findFirstByEmailAndStatusNot(request.getEmail().trim(), "BLOCKED");
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {
            Optional<MainAdmin> byUsername = mainAdminRepository.findByInstitutionCodeAndUsername(
                    request.getInstitutionCode().trim(), request.getUsername().trim());
            if (byUsername.isPresent()) user = byUsername.get();
        }

        if (user == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid credentials.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        if (user.getForgotOtp() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "No OTP found. Please request a new one.", null),
                    HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(user.getForgotOtpExpiry())) {
            user.setForgotOtp(null); user.setForgotOtpExpiry(null);
            mainAdminRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "OTP has expired. Please request a new one.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String otpInput = request.getOtp() != null ? request.getOtp().trim() : "";
        if (!user.getForgotOtp().trim().equals(otpInput)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid OTP. Please try again.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        logger.info("verifyForgotOtp — OTP verified for user={}", user.getUsername());
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP verified successfully.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // FORGOT PASSWORD — Step C: OTP verify + password reset
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

        MainAdmin user = null;

        // Email se dhundho — skip BLOCKED so re-onboarded user can reset password
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<MainAdmin> byEmail =
                    mainAdminRepository.findFirstByEmailAndStatusNot(request.getEmail().trim(), "BLOCKED");
            if (byEmail.isPresent()) user = byEmail.get();
        }

        // Username + institutionCode se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {

            Optional<MainAdmin> byUsername =
                    mainAdminRepository.findByInstitutionCodeAndUsername(
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
            mainAdminRepository.save(user);
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
        mainAdminRepository.save(user);

        logger.info("Password reset successfully for user={}",
                request.getUsername() != null ? request.getUsername() : request.getEmail());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password reset successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3.5 — After OTP verified → Set institution status ACTIVE
    // Called from OtpController after successful OTP verification
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> activateInstitution(String email) {

        // Find the active (non-BLOCKED) MainAdmin by email — newest record first (highest ID).
        // If the same email was re-onboarded after a BLOCK, OrderByIdAsc would wrongly return the
        // old BLOCKED record and the new institution would never become ACTIVE after first login.
        Optional<MainAdmin> optUser = mainAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(email, "BLOCKED");

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Super User not found for email: " + email, null),
                    HttpStatus.NOT_FOUND);
        }

        MainAdmin user = optUser.get();

        // Find institution by institutionCode + superUserId (superUserId = username)
        Optional<MainBank> optInst = mainBankRepository
                .findByInstitutionCodeAndSuperUserId(
                        user.getInstitutionCode(),
                        user.getUsername());

        if (!optInst.isPresent()) {
            logger.warn("[ACTIVATE] Institution not found for email: {}", email);
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "Login successful.", new ArrayList<>()),
                    HttpStatus.OK);
        }

        MainBank institution = optInst.get();

        // BLOCKED — permanently blocked, kabhi ACTIVE mat karo
        if ("BLOCKED".equals(institution.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Blocked institution cannot be activated.", null),
                    HttpStatus.FORBIDDEN);
        }

        // Sirf VERIFIED → ACTIVE (INACTIVE/BLOCKED automatically ACTIVE nahi honge)
        if ("VERIFIED".equals(institution.getStatus())) {
            institution.setStatus("ACTIVE");
            institution.setUpdatedAt(LocalDateTime.now());
            mainBankRepository.save(institution);
            // Sync to KAL_SUPER_USER
            user.setStatus("ACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(user);
            logger.info("[ACTIVATE] Institution {} status → ACTIVE after first login",
                    institution.getInstitutionCode());
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Institution activated successfully.", new ArrayList<>()),
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
