package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.transaction.Transactional;

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

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyEmailResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import java.util.List;

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
    private BranchBankRepository branchBankRepository;

    @Autowired
    private BranchAdminRepository branchAdminRepository;

    @Autowired
    private AddUserRepository addUserRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdminReplacementRepository adminReplacementRepository;

    @Autowired
    private KalAdminRepository kalAdminRepository;

    @Autowired
    private JwtHelper jwtHelper;

    // =========================================================================
    // verifyEmail
    // GET /test/api/v1/bank/verify-email?bankCode=xxx&username=yyy
    //
    // BANK_ADMIN mein dhundho:
    //   - Record nahi / passwordSet=false → NEW_USER
    //   - passwordSet=true                → OLD_USER
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(
            String bankCode, String username) {

        logger.info("verifyEmail — bankCode={} username={}",
                bankCode, username);

        if (bankCode == null || bankCode.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank code and username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String userStatus;

        // Check main_admin directly — covers replacement admins who have no main_bank entry
        Optional<MainAdmin> adminOpt = mainAdminRepository.findByBankCodeAndUsername(bankCode.trim(), username.trim());
        if (adminOpt.isPresent() && (adminOpt.get().getPasswordSet() == 1 || "ACTIVE".equalsIgnoreCase(adminOpt.get().getStatus()))) {
            userStatus = "OLD_USER";
            logger.info("verifyEmail → OLD_USER (main_admin active) for username={}", username);
        } else {
            // Fallback: check main_bank defaultPassword for original admins
            Optional<MainBank> optInst = mainBankRepository.findByBankCodeAndBankAdminId(bankCode.trim(), username.trim());
            if (optInst.isPresent() && optInst.get().getDefaultPassword() == null) {
                userStatus = "OLD_USER";
                logger.info("verifyEmail → OLD_USER (defaultPassword null) for username={}", username);
            } else {
                userStatus = "NEW_USER";
                logger.info("verifyEmail → NEW_USER for username={}", username);
            }
        }

        MainAdminVerifyEmailResponseDto responseDto =
                new MainAdminVerifyEmailResponseDto(
                        userStatus,
                        bankCode.trim(),
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
    // POST /test/api/v1/bank/check-user-status
    // Body: { bankCode, username }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> checkUserStatus(MainAdminVerifyDto dto) {

        logger.info("checkUserStatus — bankCode={} username={}",
                dto.getBankCode(), dto.getUsername());

        // TEST_BANK is source of truth — defaultPassword==null means password already set
        Optional<MainBank> optInst =
                mainBankRepository.findByBankCodeAndBankAdminId(
                        dto.getBankCode(),
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
    // POST /test/api/v1/bank/verify-credentials
    // Body: { bankCode, username, defaultPassword }
    //
    // BANK_ADMIN mein dhundho → default password compare karo
    // passwordSet=true → ALREADY_VERIFIED
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(MainAdminVerifyDto dto) {

        logger.info("verifyCredentials — bankCode={} username={}",
                dto.getBankCode(), dto.getUsername());

        if (dto.getBankCode() == null || dto.getUsername() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank Code and Username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String bankCode = dto.getBankCode().trim();
        String username        = dto.getUsername().trim();

        // TEST_BANK is source of truth for default credential verification (original admin flow)
        Optional<MainBank> optInst =
                mainBankRepository.findByBankCodeAndBankAdminId(
                        bankCode, username);

        if (!optInst.isPresent()) {
            // Fallback: replacement admin — exists directly in BANK_ADMIN with passwordSet=0
            Optional<MainAdmin> repOpt = mainAdminRepository.findByBankCodeAndUsername(bankCode, username);
            if (repOpt.isPresent() && repOpt.get().getPasswordSet() == 0) {
                MainAdmin repAdmin = repOpt.get();
                if (repAdmin.getPassword() == null) {
                    return new ResponseEntity<>(
                            new RestWithStatusList("FAILURE", "Invalid Bank Code or Username. Please check your email.", null),
                            HttpStatus.BAD_REQUEST);
                }
                boolean repMatch = false;
                try {
                    repMatch = passwordEncoder.matches(dto.getDefaultPassword(), repAdmin.getPassword());
                } catch (Exception e) {
                    logger.warn("BCrypt match failed for replacement admin: {}", e.getMessage());
                }
                if (!repMatch) {
                    logger.warn("verifyCredentials — password mismatch for replacement admin bankCode={} username={}", bankCode, username);
                    return new ResponseEntity<>(
                            new RestWithStatusList("FAILURE", "Invalid Default Password. Please check your email.", null),
                            HttpStatus.BAD_REQUEST);
                }
                logger.info("verifyCredentials → SUCCESS (replacement admin) for username={}", username);
                return new ResponseEntity<>(
                        new RestWithStatusList("SUCCESS", "Credentials verified. Please set your new password.", new ArrayList<>()),
                        HttpStatus.OK);
            }

            logger.warn("verifyCredentials — bank not found: bankCode={} username={}",
                    bankCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Bank Code or Username. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        MainBank bank = optInst.get();

        // defaultPassword==null means password was already set → direct login
        if (bank.getDefaultPassword() == null) {
            logger.info("verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // Verify default password — Case A: BCrypt (new banks), Case B: plaintext (old banks)
        boolean passwordMatch = false;
        if (dto.getDefaultPassword() != null && bank.getDefaultPassword() != null) {
            try {
                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(), bank.getDefaultPassword());
            } catch (Exception e) {
                logger.warn("BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            if (!passwordMatch) {
                passwordMatch = dto.getDefaultPassword().equals(bank.getDefaultPassword());
            }
        }

        if (!passwordMatch) {
            logger.warn("verifyCredentials — password mismatch for bankCode={} username={}",
                    bankCode, username);
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
    // POST /test/api/v1/bank/set-password
    // Body: { bankCode, username, newPassword }
    //
    // BCrypt encode karke save karo, passwordSet=true, status=VERIFIED
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(MainAdminSetPasswordDto dto) {

        logger.info("setNewPassword — bankCode={} username={}",
                dto.getBankCode(), dto.getUsername());

        String bankCode = dto.getBankCode().trim();
        String username  = dto.getUsername().trim();

        // TEST_BANK is source of truth — validate first (original admin flow)
        Optional<MainBank> optInst =
                mainBankRepository.findByBankCodeAndBankAdminId(bankCode, username);

        if (!optInst.isPresent()) {
            // Fallback: replacement admin — update the existing BANK_ADMIN record
            Optional<MainAdmin> repOpt = mainAdminRepository.findByBankCodeAndUsername(bankCode, username);
            if (repOpt.isPresent() && repOpt.get().getPasswordSet() == 0) {
                MainAdmin repAdmin = repOpt.get();
                repAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                repAdmin.setPasswordSet(1);
                repAdmin.setStatus("VERIFIED");
                repAdmin.setUpdatedAt(LocalDateTime.now());
                repAdmin.setUpdatedBy(username);
                mainAdminRepository.save(repAdmin);
                logger.info("setNewPassword → SUCCESS (replacement admin) for username={} bankCode={}", username, bankCode);
                return new ResponseEntity<>(
                        new RestWithStatusList("SUCCESS", "Password set successfully. Please login.", new ArrayList<>()),
                        HttpStatus.OK);
            }

            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        MainBank bank = optInst.get();

        // defaultPassword==null means password was already set — cannot set again
        if (bank.getDefaultPassword() == null) {
            logger.info("setNewPassword → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // INSERT new record into BANK_ADMIN (first-time password setup for original admin)
        MainAdmin mainAdmin = new MainAdmin();
        mainAdmin.setBankCode(bankCode);
        mainAdmin.setUsername(username.toLowerCase());
        mainAdmin.setEmail(bank.getPrimaryEmail());
        mainAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        mainAdmin.setPasswordSet(1);
        mainAdmin.setStatus("VERIFIED");
        mainAdmin.setCreatedAt(LocalDateTime.now());
        // Normalize createdBy: if stored as email, resolve to actual username
        String rawCreatedBy = bank.getCreatedBy();
        String resolvedCreatedBy = rawCreatedBy;
        if (rawCreatedBy != null && rawCreatedBy.contains("@")) {
            Optional<com.jpb.reconciliation.reconciliation.entity.KalAdmin> kalCreatorOpt =
                    kalAdminRepository.findByEmailId(rawCreatedBy);
            if (kalCreatorOpt.isPresent()) resolvedCreatedBy = kalCreatorOpt.get().getUserName();
        }
        mainAdmin.setCreatedBy(resolvedCreatedBy);
        mainAdminRepository.save(mainAdmin);
        logger.info("BANK_ADMIN record created for username={} bankCode={}", username, bankCode);

        // Update TEST_BANK → VERIFIED, wipe defaultPassword & token
        bank.setStatus("VERIFIED");
        bank.setDefaultPassword(null);
        bank.setVerificationToken(null);
        bank.setTokenExpiry(LocalDateTime.now());
        bank.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(bank);
        logger.info("Bank {} status → VERIFIED after password setup", bankCode);

        logger.info("setNewPassword → SUCCESS for username={}", username);
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 3 — login → OTP bhejo
    // POST /test/api/v1/bank/login
    // Body: { bankCode, username, defaultPassword }
    //
    // bankCode optional — agar empty/null to username se dhundho
    // Password verify → OTP bhejo → email return karo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(MainAdminVerifyDto dto) {

        logger.info("login — bankCode={} username={}",
                dto.getBankCode(), dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<MainAdmin> optUser = Optional.empty();

        // Primary lookup: bankCode + username = composite identity
        // Same username can exist at different banks — bankCode disambiguates
        if (dto.getBankCode() != null && !dto.getBankCode().trim().isEmpty()) {
            String enteredCode = dto.getBankCode().trim();
            String enteredUser = dto.getUsername().trim();

            // Step 1: Composite lookup (works when BANK_ADMIN.bank_code is correct)
            optUser = mainAdminRepository.findByBankCodeAndUsername(enteredCode, enteredUser);

            // Step 2: TEST_BANK bridge — handles stale/wrong bank_code in BANK_ADMIN
            // TEST_BANK is authoritative; bridge via primary_email.
            // Skip BLOCKED records so re-onboarded users with the same email can log in.
            if (!optUser.isPresent()) {
                logger.warn("login — composite miss, trying MainBank bridge for bankCode={} username={}",
                        enteredCode, enteredUser);
                Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> bnkOpt =
                        mainBankRepository.findByBankCodeAndBankAdminId(enteredCode, enteredUser);
                if (bnkOpt.isPresent()) {
                    String primaryEmail = bnkOpt.get().getPrimaryEmail();
                    if (primaryEmail != null && !primaryEmail.trim().isEmpty()) {
                        // Prefer non-BLOCKED record — re-onboarding creates a 2nd record with same email
                        optUser = mainAdminRepository.findFirstByEmailAndStatusNot(primaryEmail.trim(), "BLOCKED");
                        if (optUser.isPresent()) {
                            logger.info("login — MainBank bridge hit via email={} for username={}", primaryEmail, enteredUser);
                        } else {
                            logger.warn("login — MainBank bridge: no BANK_ADMIN entry for email={}", primaryEmail);
                        }
                    }
                } else {
                    logger.warn("login — MainBank bridge miss: bankCode={} superUserId={} not found",
                            enteredCode, enteredUser);
                }
            }

            // Step 3: Legacy fallback — old records where bank_code was stored as NULL
            if (!optUser.isPresent()) {
                Optional<MainAdmin> legacy =
                        mainAdminRepository.findFirstByUsernameAndBankCodeIsNull(enteredUser);
                if (legacy.isPresent()) {
                    Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> parentInst =
                            mainBankRepository.findFirstByBankAdminId(enteredUser);
                    if (parentInst.isPresent() && enteredCode.equals(parentInst.get().getBankCode())) {
                        optUser = legacy;
                        logger.info("login — legacy NULL-code record matched for username={}", enteredUser);
                    } else {
                        logger.warn("login — all strategies exhausted for bankCode={} username={}", enteredCode, enteredUser);
                    }
                }
            }
        } else {
            logger.warn("login — bankCode missing for username={}", dto.getUsername());
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Bank Code or Username.", null),
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

        // ── Status check from BANK_ADMIN (synced with TEST_BANK) ──
        String userStatus = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(userStatus)) {
            logger.warn("login BLOCKED: bank={} username={}", dto.getBankCode(), dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCKED",
                            "Your bank account has been permanently blocked. Please contact the KalInfotech administrator.", null),
                    HttpStatus.OK);
        }
        if ("INACTIVE".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your bank account is currently inactive. Please contact the KalInfotech administrator to reactivate your account.",
                            null),
                    HttpStatus.OK);
        }

        // Password match (done before INACTIVE_PENDING / BLOCK_PENDING so OTP is only sent on valid credentials)
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

        if ("INACTIVE_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE_PENDING",
                            "Your bank account is scheduled for inactivation. Please contact the KalInfotech administrator if this was not intended.",
                            data),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your bank account has been scheduled for permanent block. Please contact the KalInfotech administrator immediately to avoid losing access.",
                            data),
                    HttpStatus.OK);
        }
        if ("ACTIVE_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("ACTIVE_PENDING",
                            "Your bank account reactivation is in progress. You may proceed to login — your account will be fully active shortly.",
                            data),
                    HttpStatus.OK);
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // DIRECT LOGIN — bankCode + username + password → JWT (no OTP)
    // POST /test/api/v1/bank/direct-login
    // Body: { bankCode, username, defaultPassword }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> directLogin(MainAdminVerifyDto dto) {

        logger.info("directLogin — bankCode={} username={}",
                dto.getBankCode(), dto.getUsername());

        // bankCode required
        if (dto.getBankCode() == null || dto.getBankCode().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Bank Code is required.", null),
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

        String enteredCode = dto.getBankCode().trim();
        String enteredUser = dto.getUsername().trim();

        // Step 1: Composite lookup
        Optional<MainAdmin> optUser = mainAdminRepository.findByBankCodeAndUsername(
                enteredCode, enteredUser);

        // Step 2: TEST_BANK bridge — handles stale/wrong bank_code in BANK_ADMIN
        // Skip BLOCKED records so re-onboarded users with the same email can log in.
        if (!optUser.isPresent()) {
            logger.warn("directLogin — composite miss, trying MainBank bridge for bankCode={} username={}",
                    enteredCode, enteredUser);
            Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> bnkOpt =
                    mainBankRepository.findByBankCodeAndBankAdminId(enteredCode, enteredUser);
            if (bnkOpt.isPresent()) {
                String primaryEmail = bnkOpt.get().getPrimaryEmail();
                if (primaryEmail != null && !primaryEmail.trim().isEmpty()) {
                    // Prefer non-BLOCKED record — re-onboarding creates a 2nd record with same email
                    optUser = mainAdminRepository.findFirstByEmailAndStatusNot(primaryEmail.trim(), "BLOCKED");
                    if (optUser.isPresent()) {
                        logger.info("directLogin — MainBank bridge hit via email={} for username={}", primaryEmail, enteredUser);
                    }
                }
            }
        }

        // Step 3: Legacy fallback — old records where bank_code was stored as NULL
        if (!optUser.isPresent()) {
            Optional<MainAdmin> legacy =
                    mainAdminRepository.findFirstByUsernameAndBankCodeIsNull(enteredUser);
            if (legacy.isPresent()) {
                Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> parentInst =
                        mainBankRepository.findFirstByBankAdminId(enteredUser);
                if (parentInst.isPresent() && enteredCode.equals(parentInst.get().getBankCode())) {
                    optUser = legacy;
                    logger.info("directLogin — legacy NULL-code record matched for username={}", enteredUser);
                } else {
                    logger.warn("directLogin — all strategies exhausted for bankCode={} username={}", enteredCode, enteredUser);
                }
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Bank Code or Username.", null),
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

        // ── Status check from BANK_ADMIN (synced with TEST_BANK) ──
        String status = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Bank Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your bank account is currently inactive. Please contact the KalInfotech administrator to reactivate your account.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your bank account has been scheduled for permanent block. Please contact the KalInfotech administrator immediately to avoid losing access.",
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

        String accessToken  = jwtHelper.generateToken(userDetails, "BANK_ADMIN");
        String refreshToken = jwtHelper.generateTokenForRefresh(user.getUsername());

        // Bank code bhi return karo
        List<Object> data = new ArrayList<>();
        data.add(user.getUsername());
        data.add(user.getBankCode());
        data.add(accessToken);
        data.add(refreshToken);

        logger.info("directLogin — success for username={} bankCode={}",
                user.getUsername(), user.getBankCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Login successful.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // FORGOT PASSWORD — Step A: OTP bhejo
    // POST /test/api/v1/bank/forgot-password
    // Body: { email }  OR  { bankCode, username }
    //
    // ✅ FIX: OTP actually send karo — pehle sirf logger tha
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequestDto request) {

        logger.info("forgotPassword — email={} username={} bankCode={}",
                request.getEmail(), request.getUsername(), request.getBankCode());

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

        // Strategy 2: bankCode + username se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {

            Optional<MainAdmin> byUsername =
                    mainAdminRepository.findByBankCodeAndUsername(
                            request.getBankCode().trim(),
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

        if ("BLOCKED".equalsIgnoreCase(user.getStatus())) {
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
    // POST /test/api/v1/bank/verify-forgot-otp
    // Body: { email, otp }  OR  { bankCode, username, otp }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequestDto request) {

        logger.info("verifyForgotOtp — email={} username={}", request.getEmail(), request.getUsername());

        MainAdmin user = null;

        // Skip BLOCKED records so re-onboarded user can verify OTP
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<MainAdmin> byEmail = mainAdminRepository.findFirstByEmailAndStatusNot(request.getEmail().trim(), "BLOCKED");
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {
            Optional<MainAdmin> byUsername = mainAdminRepository.findByBankCodeAndUsername(
                    request.getBankCode().trim(), request.getUsername().trim());
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
    // POST /test/api/v1/bank/reset-password
    // Body: { email, otp, newPassword, confirmNewPassword }
    //   OR  { bankCode, username, otp, newPassword, confirmNewPassword }
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request) {

        logger.info("resetPassword — BankCode={} username={} email={}",
                request.getBankCode(), request.getUsername(), request.getEmail());

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

        // Username + bankCode se dhundho
        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {

            Optional<MainAdmin> byUsername =
                    mainAdminRepository.findByBankCodeAndUsername(
                            request.getBankCode().trim(),
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
        user.setPasswordSet(1);
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
    // STEP 3.5 — After OTP verified → Set bank status ACTIVE
    // Called from OtpController after successful OTP verification
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> activateBank(String email) {

        // Find the active (non-BLOCKED) MainAdmin by email — newest record first (highest ID).
        // If the same email was re-onboarded after a BLOCK, OrderByIdAsc would wrongly return the
        // old BLOCKED record and the new bank would never become ACTIVE after first login.
        Optional<MainAdmin> optUser = mainAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(email, "BLOCKED");

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank Admin not found for email: " + email, null),
                    HttpStatus.NOT_FOUND);
        }

        MainAdmin user = optUser.get();

        // Find bank by bankCode + superUserId (superUserId = username)
        Optional<MainBank> optInst = mainBankRepository
                .findByBankCodeAndBankAdminId(
                        user.getBankCode(),
                        user.getUsername());

        if (!optInst.isPresent()) {
            // Replacement admin — no MainBank record; promote VERIFIED → ACTIVE on first login
            if ("VERIFIED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
                user.setUpdatedAt(LocalDateTime.now());
                user.setUpdatedBy("SYSTEM");
                mainAdminRepository.save(user);
                logger.info("[ACTIVATE] Replacement admin {} → ACTIVE after first login", user.getUsername());
            }
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "Login successful.", new ArrayList<>()),
                    HttpStatus.OK);
        }

        MainBank bank = optInst.get();

        // BLOCKED — permanently blocked, kabhi ACTIVE mat karo
        if ("BLOCKED".equals(bank.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Blocked bank cannot be activated.", null),
                    HttpStatus.FORBIDDEN);
        }

        // Sirf VERIFIED → ACTIVE (INACTIVE/BLOCKED automatically ACTIVE nahi honge)
        if ("VERIFIED".equals(bank.getStatus())) {
            bank.setStatus("ACTIVE");
            bank.setUpdatedAt(LocalDateTime.now());
            mainBankRepository.save(bank);
            // Sync to BANK_ADMIN; capture full name for future reference
            user.setStatus("ACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy("SYSTEM");
            if (user.getFullName() == null && bank.getPrimaryFullName() != null) {
                user.setFullName(bank.getPrimaryFullName());
            }
            mainAdminRepository.save(user);
            logger.info("[ACTIVATE] Bank {} status → ACTIVE after first login",
                    bank.getBankCode());
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Bank activated successfully.", new ArrayList<>()),
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

    // ── Schedule / Undo status transitions for Bank Admin ───────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long bankId, String scheduledBy) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus()) && !"VERIFIED".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin must be ACTIVE to schedule inactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE_PENDING");
        admin.setInactivateScheduledAt(LocalDateTime.now());
        admin.setInactivateScheduledBy(scheduledBy);
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        BlockScheduleServiceImpl.flagPendingWork();
        admin.setUpdatedBy(scheduledBy);
        mainAdminRepository.save(admin);
        try {
            String inactivateAt = admin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendInactivatePendingWarning(contact[0], contact[1],
                    bank.getBankNameFull(), bank.getBankCode(), inactivateAt);
            sendKalActorConfirmation(scheduledBy, "Inactivation Scheduled", bank.getBankNameFull(), bank.getBankCode(), inactivateAt);
        } catch (Exception e) {
            logger.warn("scheduleInactivate: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation scheduled for bank admin {} (bank {}) by {}", admin.getUsername(), bankId, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation scheduled. Bank admin will be INACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoInactivate(Long bankId, String undoneBy) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"INACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled inactivation found for this bank admin.", null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE");
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        mainAdminRepository.save(admin);
        // Cancel any pending replacement since inactivation was undone
        try {
            Optional<AdminReplacement> pendingRep = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "MAIN_ADMIN", "PENDING");
            if (pendingRep.isPresent()) {
                pendingRep.get().setStatus("CANCELLED");
                adminReplacementRepository.save(pendingRep.get());
                logger.info("Cancelled PENDING replacement for bank admin {} due to undo", admin.getUsername());
            }
        } catch (Exception e) {
            logger.warn("undoInactivate: failed to cancel pending replacement for {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendInactivateCancelled(contact[0], contact[1],
                    bank.getBankNameFull(), bank.getBankCode());
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            sendKalActorConfirmation(undoneBy, "Inactivation Cancelled", bank.getBankNameFull(), bank.getBankCode(), nowStr);
        } catch (Exception e) {
            logger.warn("undoInactivate: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation undone for bank admin {} (bank {}) by {}. Restored to ACTIVE", admin.getUsername(), bankId, undoneBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation cancelled. Restored to ACTIVE.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long bankId, String scheduledBy) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"INACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin must be INACTIVE to schedule reactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE_PENDING");
        admin.setReactivateScheduledAt(LocalDateTime.now());
        admin.setReactivateScheduledBy(scheduledBy);
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        BlockScheduleServiceImpl.flagPendingWork();
        admin.setUpdatedBy(scheduledBy);
        mainAdminRepository.save(admin);
        // Transition any active replacement to INACTIVE_PENDING so they are notified their tenure is ending
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<MainAdmin> repAdminOpt = mainAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        MainAdmin repAdmin = repAdminOpt.get();
                        if ("ACTIVE".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("INACTIVE_PENDING");
                            repAdmin.setInactivateScheduledAt(LocalDateTime.now());
                            repAdmin.setInactivateScheduledBy(scheduledBy);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(scheduledBy);
                            mainAdminRepository.save(repAdmin);
                            logger.info("Replacement bank admin {} set to INACTIVE_PENDING as original {} is reactivating", repAdmin.getUsername(), admin.getUsername());
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    String inactivateAt = repAdmin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                                    emailService.sendInactivatePendingWarning(repAdmin.getEmail(), repAdmin.getUsername(),
                                            bank.getBankNameFull(), bank.getBankCode(), inactivateAt);
                                }
                            } catch (Exception e2) {
                                logger.warn("scheduleReactivate: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleReactivate: replacement transition failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            String reactivateAt = admin.getReactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendReactivatePendingNotification(contact[0], contact[1],
                    bank.getBankNameFull(), bank.getBankCode(), reactivateAt);
            sendKalActorConfirmation(scheduledBy, "Reactivation Scheduled", bank.getBankNameFull(), bank.getBankCode(), reactivateAt);
        } catch (Exception e) {
            logger.warn("scheduleReactivate: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation scheduled for bank admin {} (bank {}) by {}", admin.getUsername(), bankId, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation scheduled. Bank admin will be ACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoReactivate(Long bankId, String undoneBy) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"ACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled reactivation found for this bank admin.", null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE");
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        mainAdminRepository.save(admin);
        // Restore replacement admin to ACTIVE since reactivation was undone
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<MainAdmin> repAdminOpt = mainAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        MainAdmin repAdmin = repAdminOpt.get();
                        if ("INACTIVE_PENDING".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("ACTIVE");
                            repAdmin.setInactivateScheduledAt(null);
                            repAdmin.setInactivateScheduledBy(null);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(undoneBy);
                            mainAdminRepository.save(repAdmin);
                            logger.info("Replacement bank admin {} restored to ACTIVE as original {} reactivation was undone", repAdmin.getUsername(), admin.getUsername());
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    emailService.sendInactivateCancelled(repAdmin.getEmail(), repAdmin.getUsername(),
                                            bank.getBankNameFull(), bank.getBankCode());
                                }
                            } catch (Exception e2) {
                                logger.warn("undoReactivate: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("undoReactivate: replacement restoration failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendReactivateCancelled(contact[0], contact[1],
                    bank.getBankNameFull(), bank.getBankCode());
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            sendKalActorConfirmation(undoneBy, "Reactivation Cancelled", bank.getBankNameFull(), bank.getBankCode(), nowStr);
        } catch (Exception e) {
            logger.warn("undoReactivate: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation undone for bank admin {} (bank {}) by {}. Restored to INACTIVE", admin.getUsername(), bankId, undoneBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation cancelled. Restored to INACTIVE.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long id, String scheduledBy, String reason) {
        Optional<MainAdmin> opt = mainAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found: " + id, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if ("BLOCKED".equalsIgnoreCase(admin.getStatus()) || "BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin is already blocked or pending block.", null), HttpStatus.OK);
        }
        admin.setPreBlockStatus(admin.getStatus());
        admin.setStatus("BLOCK_PENDING");
        admin.setBlockScheduledAt(LocalDateTime.now());
        admin.setBlockScheduledBy(scheduledBy);
        admin.setBlockReason(reason);
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        mainAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();

        // Chain cascade only when admin was ACTIVE; INACTIVE admin → individual block only
        Optional<MainBank> parentBankOpt = mainBankRepository.findByBankCode(admin.getBankCode());
        if ("ACTIVE".equalsIgnoreCase(admin.getPreBlockStatus()) && parentBankOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBankOpt.get().getStatus())) {
            MainBank parentBank = parentBankOpt.get();
            // Cascade to branch admins
            final String blockReasonVal = reason;
            try {
                List<com.jpb.reconciliation.reconciliation.entity.BranchBank> branches =
                        branchBankRepository.findByParentBankId(parentBank.getBankId());
                for (com.jpb.reconciliation.reconciliation.entity.BranchBank branch : branches) {
                    branchAdminRepository.findByBranchCodeAndUsername(branch.getBranchCode(), branch.getBranchAdminId())
                        .ifPresent(ba -> {
                            if (!"BLOCKED".equalsIgnoreCase(ba.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(ba.getStatus())) {
                                ba.setPreBlockStatus(ba.getStatus());
                                ba.setStatus("BLOCK_PENDING");
                                ba.setBlockScheduledAt(LocalDateTime.now());
                                ba.setBlockScheduledBy(scheduledBy);
                                ba.setBlockReason(blockReasonVal);
                                ba.setInactivateScheduledAt(null);
                                ba.setInactivateScheduledBy(null);
                                ba.setReactivateScheduledAt(null);
                                ba.setReactivateScheduledBy(null);
                                ba.setUpdatedAt(LocalDateTime.now());
                                ba.setUpdatedBy(scheduledBy);
                                branchAdminRepository.save(ba);
                            }
                        });
                }
            } catch (Exception e) {
                logger.warn("scheduleBlock admin: branch admin cascade failed for bank {}: {}", admin.getBankCode(), e.getMessage());
            }
            // Cascade to users
            try {
                List<AddUser> users = addUserRepository.findByBankCode(admin.getBankCode());
                for (AddUser user : users) {
                    if (user.getStatus() != AddUser.UserStatus.BLOCKED && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
                        user.setPreBlockStatus(user.getStatus().name());
                        user.setStatus(AddUser.UserStatus.BLOCK_PENDING);
                        user.setBlockScheduledAt(LocalDateTime.now());
                        user.setBlockScheduledBy(scheduledBy);
                        user.setBlockReason(reason);
                        user.setInactivateScheduledAt(null);
                        user.setInactivateScheduledBy(null);
                        user.setReactivateScheduledAt(null);
                        user.setReactivateScheduledBy(null);
                        addUserRepository.save(user);
                    }
                }
            } catch (Exception e) {
                logger.warn("scheduleBlock admin: user cascade failed for bank {}: {}", admin.getBankCode(), e.getMessage());
            }
        }

        try {
            String bankName = parentBankOpt.isPresent() ? parentBankOpt.get().getBankNameFull() : admin.getBankCode();
            String blockAt = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            // Individual block — email goes directly to the admin being blocked, not their replacement
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(), bankName, admin.getBankCode(), blockAt);
            sendKalActorConfirmation(scheduledBy, "Block Scheduled", bankName, admin.getBankCode(), blockAt);
        } catch (Exception e) {
            logger.warn("scheduleBlock: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block scheduled for bank admin {} by {}", id, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block scheduled. Bank admin will be BLOCKED in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long id, String undoneBy) {
        Optional<MainAdmin> opt = mainAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found: " + id, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled block found for this bank admin.", null), HttpStatus.OK);
        }
        String restored = admin.getPreBlockStatus() != null ? admin.getPreBlockStatus() : "INACTIVE";
        admin.setStatus(restored);
        admin.setBlockScheduledAt(null);
        admin.setPreBlockStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        mainAdminRepository.save(admin);
        try {
            Optional<MainBank> bankForEmailOpt = mainBankRepository.findByBankCode(admin.getBankCode());
            String bankName = bankForEmailOpt.isPresent() ? bankForEmailOpt.get().getBankNameFull() : admin.getBankCode();
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendBlockCancelled(contact[0], contact[1], bankName, admin.getBankCode(), restored);
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            sendKalActorConfirmation(undoneBy, "Block Cancelled", bankName, admin.getBankCode(), nowStr);
        } catch (Exception e) {
            logger.warn("undoBlock: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block undone for bank admin {} by {}. Restored to {}", id, undoneBy, restored);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Bank admin restored to " + restored + ".", data), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlockByBankId(Long bankId, String scheduledBy, String reason) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if ("BLOCKED".equalsIgnoreCase(admin.getStatus()) || "BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin is already blocked or pending block.", null), HttpStatus.OK);
        }
        String preStatus = admin.getStatus();
        admin.setPreBlockStatus(preStatus);
        admin.setStatus("BLOCK_PENDING");
        admin.setBlockScheduledAt(LocalDateTime.now());
        admin.setBlockScheduledBy(scheduledBy);
        admin.setBlockReason(reason);
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        mainAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();

        // Chain cascade only when admin was ACTIVE; INACTIVE → individual block only
        if ("ACTIVE".equalsIgnoreCase(preStatus)) {
            Optional<MainBank> parentBankOpt = mainBankRepository.findByBankCode(admin.getBankCode());
            if (parentBankOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBankOpt.get().getStatus())) {
                MainBank parentBank = parentBankOpt.get();
                final String blockReasonVal = reason;
                try {
                    List<com.jpb.reconciliation.reconciliation.entity.BranchBank> branches =
                            branchBankRepository.findByParentBankId(parentBank.getBankId());
                    for (com.jpb.reconciliation.reconciliation.entity.BranchBank branch : branches) {
                        branchAdminRepository.findByBranchCodeAndUsername(branch.getBranchCode(), branch.getBranchAdminId())
                            .ifPresent(ba -> {
                                if (!"BLOCKED".equalsIgnoreCase(ba.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(ba.getStatus())) {
                                    ba.setPreBlockStatus(ba.getStatus());
                                    ba.setStatus("BLOCK_PENDING");
                                    ba.setBlockScheduledAt(LocalDateTime.now());
                                    ba.setBlockScheduledBy(scheduledBy);
                                    ba.setBlockReason(blockReasonVal);
                                    ba.setInactivateScheduledAt(null);
                                    ba.setInactivateScheduledBy(null);
                                    ba.setReactivateScheduledAt(null);
                                    ba.setReactivateScheduledBy(null);
                                    ba.setUpdatedAt(LocalDateTime.now());
                                    ba.setUpdatedBy(scheduledBy);
                                    branchAdminRepository.save(ba);
                                }
                            });
                    }
                } catch (Exception e) {
                    logger.warn("scheduleBlockByBankId: branch admin cascade failed for bank {}: {}", admin.getBankCode(), e.getMessage());
                }
                try {
                    List<AddUser> users = addUserRepository.findByBankCode(admin.getBankCode());
                    for (AddUser user : users) {
                        if (user.getStatus() != AddUser.UserStatus.BLOCKED && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
                            user.setPreBlockStatus(user.getStatus().name());
                            user.setStatus(AddUser.UserStatus.BLOCK_PENDING);
                            user.setBlockScheduledAt(LocalDateTime.now());
                            user.setBlockScheduledBy(scheduledBy);
                            user.setBlockReason(reason);
                            user.setInactivateScheduledAt(null);
                            user.setInactivateScheduledBy(null);
                            user.setReactivateScheduledAt(null);
                            user.setReactivateScheduledBy(null);
                            addUserRepository.save(user);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("scheduleBlockByBankId: user cascade failed for bank {}: {}", admin.getBankCode(), e.getMessage());
                }
            }
        }

        try {
            String blockAt = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            // Individual block — email goes directly to the admin being blocked, not their replacement
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(), bank.getBankNameFull(), bank.getBankCode(), blockAt);
            sendKalActorConfirmation(scheduledBy, "Block Scheduled", bank.getBankNameFull(), bank.getBankCode(), blockAt);
        } catch (Exception e) {
            logger.warn("scheduleBlockByBankId: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block scheduled for bank admin (bank {}) by {}. Pre-status: {}", bankId, scheduledBy, preStatus);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block scheduled. Bank admin will be BLOCKED in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlockByBankId(Long bankId, String undoneBy) {
        Optional<MainBank> bankOpt = mainBankRepository.findById(bankId);
        if (!bankOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank not found: " + bankId, null), HttpStatus.OK);
        }
        MainBank bank = bankOpt.get();
        Optional<MainAdmin> opt = mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId());
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Bank admin not found for bank: " + bankId, null), HttpStatus.OK);
        }
        MainAdmin admin = opt.get();
        if (!"BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled block found for this bank admin.", null), HttpStatus.OK);
        }
        String restored = admin.getPreBlockStatus() != null ? admin.getPreBlockStatus() : "INACTIVE";
        admin.setStatus(restored);
        admin.setBlockScheduledAt(null);
        admin.setBlockScheduledBy(null);
        admin.setBlockReason(null);
        admin.setPreBlockStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        mainAdminRepository.save(admin);
        try {
            String[] contact = resolveMainAdminContact(admin);
            emailService.sendBlockCancelled(contact[0], contact[1], bank.getBankNameFull(), bank.getBankCode(), restored);
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            sendKalActorConfirmation(undoneBy, "Block Cancelled", bank.getBankNameFull(), bank.getBankCode(), nowStr);
        } catch (Exception e) {
            logger.warn("undoBlockByBankId: email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block undone for bank admin (bank {}) by {}. Restored to {}", bankId, undoneBy, restored);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Bank admin restored to " + restored + ".", new ArrayList<>()), HttpStatus.OK);
    }

    private void sendKalActorConfirmation(String actorUsername, String action, String entityName, String entityCode, String scheduledAt) {
        try {
            Optional<MainAdmin> ma = mainAdminRepository.findFirstByUsername(actorUsername);
            if (ma.isPresent() && ma.get().getEmail() != null && !ma.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ma.get().getEmail(), ma.get().getUsername(), action, entityName, entityCode, scheduledAt);
                return;
            }
            Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> ba = branchAdminRepository.findFirstByUsername(actorUsername);
            if (ba.isPresent() && ba.get().getEmail() != null && !ba.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ba.get().getEmail(), ba.get().getUsername(), action, entityName, entityCode, scheduledAt);
                return;
            }
            kalAdminRepository.findByUserName(actorUsername).ifPresent(actor -> {
                if (actor.getEmailId() != null && !actor.getEmailId().isEmpty()) {
                    emailService.sendActorActionConfirmation(actor.getEmailId(), actor.getUserName(),
                            action, entityName, entityCode, scheduledAt);
                }
            });
        } catch (Exception e) {
            logger.warn("sendKalActorConfirmation: email failed for actor {}: {}", actorUsername, e.getMessage());
        }
    }

    private String[] resolveMainAdminContact(MainAdmin admin) {
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                            admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            if (!reps.isEmpty() && reps.get(0).getReplacementEntityId() > 0) {
                Optional<MainAdmin> rep = mainAdminRepository.findById(reps.get(0).getReplacementEntityId());
                if (rep.isPresent() && rep.get().getEmail() != null && !rep.get().getEmail().isEmpty()) {
                    return new String[]{rep.get().getEmail(), rep.get().getUsername()};
                }
            }
        } catch (Exception e) {
            logger.warn("resolveMainAdminContact failed for {}: {}", admin.getUsername(), e.getMessage());
        }
        return new String[]{admin.getEmail(), admin.getUsername()};
    }

    // =========================================================================
    // getAllBankAdmins — fetch all Bank Admins directly from BANK_ADMIN table
    // GET /test/api/v1/bank/get-all-admins
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> getAllBankAdmins(String callerUsername) {
        List<MainAdmin> admins = (callerUsername != null && !callerUsername.isEmpty())
            ? mainAdminRepository.findAllByCreatedBy(callerUsername)
            : mainAdminRepository.findAll();
        List<Object> result = new ArrayList<>();

        for (MainAdmin admin : admins) {
            // Skip admins who are currently acting as a replacement for someone else —
            // they will appear as a replacement row under their original admin.
            // Exception: if this admin is ALSO being replaced themselves, show them as a
            // primary row (they have moved on from the replacement role).
            boolean isRestoredReplacement = false;
            List<AdminReplacement> asRepOf = adminReplacementRepository
                .findByReplacementEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT", "RESTORED"));
            if (!asRepOf.isEmpty()) {
                List<AdminReplacement> theirOwnReps = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                        admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                if (theirOwnReps.isEmpty()) {
                    boolean allRestored = asRepOf.stream().allMatch(r -> "RESTORED".equals(r.getStatus()));
                    if (!allRestored) {
                        continue; // Still an ACTIVE/PERMANENT replacement — show under original
                    }
                    // All RESTORED: original was reactivated, this replacement's tenure is done
                    if ("INACTIVE".equalsIgnoreCase(admin.getStatus())) {
                        continue; // Fully INACTIVE — hide from list
                    }
                    // INACTIVE_PENDING: still counting down — show but flag as replacement row
                    isRestoredReplacement = true;
                }
            }

            Optional<MainBank> bankOpt = mainBankRepository.findByBankCode(admin.getBankCode());

            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));

            boolean hasPermanentRep = false;
            for (AdminReplacement r : reps) {
                if ("PERMANENT".equals(r.getStatus())) { hasPermanentRep = true; break; }
            }

            // If permanently replaced, MainBank.primaryFullName is now the replacement's name;
            // use stored fullName if available, otherwise fall back to username
            String displayName = bankOpt.isPresent()
                ? (hasPermanentRep
                    ? (admin.getFullName() != null ? admin.getFullName() : admin.getUsername())
                    : bankOpt.get().getPrimaryFullName())
                : (admin.getFullName() != null ? admin.getFullName() : admin.getUsername());

            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("adminId", admin.getId());
            row.put("adminStatus", admin.getStatus());
            row.put("bankCode", admin.getBankCode());
            row.put("primaryEmail", admin.getEmail());
            row.put("primaryFullName", displayName);
            row.put("updatedAt", admin.getUpdatedAt() != null ? admin.getUpdatedAt().toString() : null);
            row.put("blockReason", admin.getBlockReason());
            row.put("blockScheduledAt", admin.getBlockScheduledAt() != null ? admin.getBlockScheduledAt().toString() : null);
            row.put("inactivateScheduledAt", admin.getInactivateScheduledAt() != null ? admin.getInactivateScheduledAt().toString() : null);
            row.put("reactivateScheduledAt", admin.getReactivateScheduledAt() != null ? admin.getReactivateScheduledAt().toString() : null);
            row.put("preBlockStatus", admin.getPreBlockStatus());
            row.put("replacementStatus", null);
            row.put("replacedByUsername", null);
            row.put("replacementAdminRow", isRestoredReplacement);

            if (bankOpt.isPresent()) {
                MainBank bank = bankOpt.get();
                row.put("bankId", bank.getBankId());
                row.put("bankNameFull", bank.getBankNameFull());
                row.put("bankNameShort", bank.getBankNameShort() != null ? bank.getBankNameShort() : bank.getBankNameFull());
                row.put("bankAdminId", bank.getBankAdminId());
                row.put("primaryMobile", bank.getPrimaryMobile());
                row.put("primaryMobileCode", bank.getPrimaryMobileCode());
            }

            if (!reps.isEmpty()) {
                AdminReplacement rep = reps.get(0);
                row.put("replacementStatus", rep.getStatus());
                if (rep.getReplacementEntityId() != null) {
                    Optional<MainAdmin> repAdminOpt = mainAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        MainAdmin repAdmin = repAdminOpt.get();
                        row.put("replacedByUsername", repAdmin.getEmail());

                        // Only add repRow if the replacement admin is NOT themselves being replaced.
                        // If they are, they will appear as their own primary row in the loop,
                        // and adding them here too would cause a duplicate entry.
                        List<AdminReplacement> repAdminOwnReps = adminReplacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                repAdmin.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                        if (repAdminOwnReps.isEmpty()) {
                            java.util.Map<String, Object> repRow = new java.util.HashMap<>();
                            repRow.put("adminId", repAdmin.getId());
                            repRow.put("adminStatus", repAdmin.getStatus());
                            repRow.put("bankCode", repAdmin.getBankCode());
                            repRow.put("primaryEmail", repAdmin.getEmail());
                            repRow.put("bankAdminId", repAdmin.getUsername());
                            repRow.put("primaryFullName", rep.getPendingFullName() != null ? rep.getPendingFullName() : repAdmin.getUsername());
                            repRow.put("primaryMobile", rep.getPendingMobile() != null ? rep.getPendingMobile() : "");
                            repRow.put("updatedAt", repAdmin.getUpdatedAt() != null ? repAdmin.getUpdatedAt().toString() : null);
                            repRow.put("replacementStatus", rep.getStatus());
                            repRow.put("replacedByUsername", null);
                            repRow.put("replacementAdminRow", true);
                            repRow.put("blockReason", repAdmin.getBlockReason());
                            repRow.put("blockScheduledAt", repAdmin.getBlockScheduledAt() != null ? repAdmin.getBlockScheduledAt().toString() : null);
                            repRow.put("inactivateScheduledAt", repAdmin.getInactivateScheduledAt() != null ? repAdmin.getInactivateScheduledAt().toString() : null);
                            repRow.put("reactivateScheduledAt", repAdmin.getReactivateScheduledAt() != null ? repAdmin.getReactivateScheduledAt().toString() : null);
                            repRow.put("preBlockStatus", repAdmin.getPreBlockStatus());
                            if (bankOpt.isPresent()) {
                                MainBank bank = bankOpt.get();
                                repRow.put("bankId", bank.getBankId());
                                repRow.put("bankNameFull", bank.getBankNameFull());
                                repRow.put("bankNameShort", bank.getBankNameShort() != null ? bank.getBankNameShort() : bank.getBankNameFull());
                                repRow.put("primaryMobileCode", bank.getPrimaryMobileCode());
                            }
                            result.add(repRow);
                        }
                    }
                }
            }

            result.add(row);
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank admins fetched.", result));
    }
}
