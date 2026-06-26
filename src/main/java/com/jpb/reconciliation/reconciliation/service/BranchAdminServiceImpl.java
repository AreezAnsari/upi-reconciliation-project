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

import com.jpb.reconciliation.reconciliation.dto.BranchAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyEmailResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.security.PasswordAndSecurityUserAccessValidator;

@Service
public class BranchAdminServiceImpl implements BranchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(BranchAdminServiceImpl.class);

    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private BranchBankRepository branchBankRepository;
    @Autowired private AddUserRepository addUserRepository;
    @Autowired private MainAdminRepository mainAdminRepository;
    @Autowired private MainBankRepository mainBankRepository;
    @Autowired private KalAdminRepository kalAdminRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpService otpService;
    @Autowired private EmailService emailService;
    @Autowired private JwtHelper jwtHelper;
    @Autowired private AdminReplacementRepository adminReplacementRepository;
    @Autowired private PasswordAndSecurityUserAccessValidator validator;

    // =========================================================================
    // verifyEmail — NEW_USER / OLD_USER check
    // GET /test/api/v1/BranchBank/verify-email?BranchCode=xxx&username=yyy
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(String BranchCode, String username) {
        logger.info("branchAdmin.verifyEmail — BranchCode={} username={}", BranchCode, username);

        if (BranchCode == null || BranchCode.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "bank code and username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String userStatus;

        // Check branch_admin directly — covers replacement admins who have no branch_bank entry
        Optional<BranchAdmin> adminOpt = branchAdminRepository
                .findByBranchCodeAndUsername(BranchCode.trim(), username.trim());
        if (adminOpt.isPresent() && (adminOpt.get().getPasswordSet() == 1 || "ACTIVE".equalsIgnoreCase(adminOpt.get().getStatus()))) {
            userStatus = "OLD_USER";
            logger.info("branchAdmin.verifyEmail → OLD_USER (branch_admin active) for username={}", username);
        } else {
            // Fallback: check branch_bank defaultPassword for original admins
            Optional<BranchBank> optInst = branchBankRepository
                    .findByBranchCodeAndBranchAdminId(BranchCode.trim(), username.trim());
            if (optInst.isPresent() && optInst.get().getDefaultPassword() == null) {
                userStatus = "OLD_USER";
                logger.info("branchAdmin.verifyEmail → OLD_USER (defaultPassword null) for username={}", username);
            } else {
                userStatus = "NEW_USER";
                logger.info("branchAdmin.verifyEmail → NEW_USER for username={}", username);
            }
        }

        MainAdminVerifyEmailResponseDto responseDto =
                new MainAdminVerifyEmailResponseDto(userStatus, BranchCode.trim(), username.trim());
        List<Object> data = new ArrayList<>();
        data.add(responseDto);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Email verified successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // checkUserStatus — NEW_USER / OLD_USER for inline check
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> checkUserStatus(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.checkUserStatus — BranchCode={} username={}",
                dto.getBranchCode(), dto.getUsername());

        Optional<BranchBank> optInst = branchBankRepository
                .findByBranchCodeAndBranchAdminId(dto.getBranchCode(), dto.getUsername());

        if (!optInst.isPresent() || optInst.get().getDefaultPassword() != null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("NEW_USER", "New user. Complete setup.", null),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(
                new RestWithStatusList("OLD_USER", "Login directly.", null),
                HttpStatus.OK);
    }
    public void login(BranchAdmin user) {

        validator.validate(user.getStatus());
    }
    // =========================================================================
    // STEP 1 — verifyCredentials
    // POST /test/api/v1/BranchBank/verify-credentials
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.verifyCredentials — BranchCode={} username={}",
                dto.getBranchCode(), dto.getUsername());

        if (dto.getBranchCode() == null || dto.getUsername() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "bank Code and Username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String BranchCode = dto.getBranchCode().trim();
        String username        = dto.getUsername().trim();

        Optional<BranchBank> optInst =
                branchBankRepository.findByBranchCodeAndBranchAdminId(BranchCode, username);

        if (!optInst.isPresent()) {
            // Fallback: replacement branch admin — exists directly in BRANCH_ADMIN with passwordSet=0
            Optional<BranchAdmin> repOpt = branchAdminRepository.findByBranchCodeAndUsername(BranchCode, username);
            if (repOpt.isPresent() && repOpt.get().getPasswordSet() == 0) {
                BranchAdmin repAdmin = repOpt.get();
                if (repAdmin.getPassword() == null) {
                    return new ResponseEntity<>(
                            new RestWithStatusList("FAILURE", "Invalid bank Code or Username. Please check your email.", null),
                            HttpStatus.BAD_REQUEST);
                }
                boolean repMatch = false;
                try {
                    repMatch = passwordEncoder.matches(dto.getDefaultPassword(), repAdmin.getPassword());
                } catch (Exception e) {
                    logger.warn("branchAdmin: BCrypt match failed for replacement admin: {}", e.getMessage());
                }
                if (!repMatch) {
                    logger.warn("branchAdmin.verifyCredentials — password mismatch for replacement admin {} {}", BranchCode, username);
                    return new ResponseEntity<>(
                            new RestWithStatusList("FAILURE", "Invalid Default Password. Please check your email.", null),
                            HttpStatus.BAD_REQUEST);
                }
                logger.info("branchAdmin.verifyCredentials → SUCCESS (replacement admin) for username={}", username);
                return new ResponseEntity<>(
                        new RestWithStatusList("SUCCESS", "Credentials verified. Please set your new password.", new ArrayList<>()),
                        HttpStatus.OK);
            }

            logger.warn("branchAdmin.verifyCredentials — bank not found: {} {}", BranchCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid bank Code or Username. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        BranchBank Branch = optInst.get();

        // defaultPassword==null means password was already set
        if (Branch.getDefaultPassword() == null) {
            logger.info("branchAdmin.verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // BCrypt match (Case A) + plain text fallback (Case B — old records)
        boolean passwordMatch = false;
        if (dto.getDefaultPassword() != null && Branch.getDefaultPassword() != null) {
            try {
                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(), Branch.getDefaultPassword());
            } catch (Exception e) {
                logger.warn("branchAdmin: BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            if (!passwordMatch) {
                passwordMatch = dto.getDefaultPassword().equals(Branch.getDefaultPassword());
            }
        }

        if (!passwordMatch) {
            logger.warn("branchAdmin.verifyCredentials — password mismatch for {} {}", BranchCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Default Password. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        logger.info("branchAdmin.verifyCredentials → SUCCESS for username={}", username);
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Credentials verified. Please set your new password.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 2 — setNewPassword → INSERT into BRANCH_ADMIN
    // POST /test/api/v1/Branch Bank/set-password
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(BranchAdminSetPasswordDto dto) {
        logger.info("branchAdmin.setNewPassword — BranchCode={} username={}",
                dto.getBranchCode(), dto.getUsername());

        String branchCode = dto.getBranchCode().trim();
        String username    = dto.getUsername().trim();

        Optional<BranchBank> optInst = branchBankRepository
                .findByBranchCodeAndBranchAdminId(branchCode, username);

        if (!optInst.isPresent()) {
            // Fallback: replacement branch admin — update the existing BRANCH_ADMIN record
            Optional<BranchAdmin> repOpt = branchAdminRepository.findByBranchCodeAndUsername(branchCode, username);
            if (repOpt.isPresent() && repOpt.get().getPasswordSet() == 0) {
                BranchAdmin repAdmin = repOpt.get();
                repAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                repAdmin.setPasswordSet(1);
                repAdmin.setStatus("VERIFIED");
                repAdmin.setUpdatedAt(LocalDateTime.now());
                repAdmin.setUpdatedBy(username);
                branchAdminRepository.save(repAdmin);
                logger.info("branchAdmin.setNewPassword → SUCCESS (replacement admin) for username={} branchCode={}", username, branchCode);
                return new ResponseEntity<>(
                        new RestWithStatusList("SUCCESS", "Password set successfully. Please login.", new ArrayList<>()),
                        HttpStatus.OK);
            }

            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        BranchBank bank = optInst.get();

        // defaultPassword==null means password was already set
        if (bank.getDefaultPassword() == null) {
            logger.info("branchAdmin.setNewPassword → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // INSERT new BRANCH_ADMIN record (original admin first-time setup)
        BranchAdmin branchAdmin = new BranchAdmin();
        branchAdmin.setBranchCode(branchCode);
        branchAdmin.setUsername(username.toLowerCase());
        branchAdmin.setEmail(bank.getPrimaryEmail());
        branchAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        branchAdmin.setPasswordSet(1);
        branchAdmin.setStatus("VERIFIED");
        branchAdmin.setCreatedAt(LocalDateTime.now());
        // Normalize createdBy: if stored as email, resolve to actual username
        String rawCreatedBy = bank.getCreatedBy();
        String resolvedCreatedBy = rawCreatedBy;
        if (rawCreatedBy != null && rawCreatedBy.contains("@")) {
            Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> creatorOpt =
                    mainAdminRepository.findFirstByEmail(rawCreatedBy);
            if (creatorOpt.isPresent()) resolvedCreatedBy = creatorOpt.get().getUsername();
        }
        branchAdmin.setCreatedBy(resolvedCreatedBy);
        branchAdmin.setCreatedBy(bank.getCreatedBy());
        branchAdmin.setPasswordUpdatedAt(LocalDateTime.now());
        branchAdminRepository.save(branchAdmin);
        logger.info("BRANCH_ADMIN record created for username={} BranchCode={}", username, branchCode);

        // Update BRANCH_BANK → VERIFIED, wipe defaultPassword & token
        bank.setStatus("VERIFIED");
        bank.setDefaultPassword(null);
        bank.setVerificationToken(null);
        bank.setTokenExpiry(LocalDateTime.now());
        bank.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(bank);
        logger.info("BranchBank {} status → VERIFIED after password setup", dto.getBranchCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 3 — login → OTP bhejo
    // POST /test/api/v1/Branch Bank/login
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.login — BranchCode={} username={}",
                dto.getBranchCode(), dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<BranchAdmin> optUser = Optional.empty();

        if (dto.getBranchCode() != null && !dto.getBranchCode().trim().isEmpty()) {
            optUser = branchAdminRepository.findByBranchCodeAndUsername(
                    dto.getBranchCode().trim(), dto.getUsername().trim());

            // Bridge lookup via BranchBank if composite miss.
            // Skip BLOCKED records so re-onboarded users with same email can log in.
            if (!optUser.isPresent()) {
                Optional<BranchBank> bnkOpt = branchBankRepository
                        .findByBranchCodeAndBranchAdminId(
                                dto.getBranchCode().trim(), dto.getUsername().trim());
                if (bnkOpt.isPresent() && bnkOpt.get().getPrimaryEmail() != null) {
                    optUser = branchAdminRepository.findFirstByEmailAndStatusNot(
                            bnkOpt.get().getPrimaryEmail().trim(), "BLOCKED");
                }
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Bank Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        BranchAdmin user = optUser.get();

        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Status check from BRANCH_ADMIN (synced with BRANCH_BANK)
        String userStatus = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCKED",
                            "Your branch account has been permanently blocked. Please contact your administrator.", null),
                    HttpStatus.OK);
        }
        if ("INACTIVE".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your branch account is currently inactive. Please contact your administrator.",
                            null),
                    HttpStatus.OK);
        }

        // Password match (done before INACTIVE_PENDING / BLOCK_PENDING so OTP is only sent on valid credentials)
        if (!passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid password. Please try again.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // Send OTP
        String email = user.getEmail();
        try {
            otpService.generateAndSendOtp(email);
            logger.info("branchAdmin OTP sent to: {}", email);
        } catch (Exception e) {
            logger.error("branchAdmin OTP send failed for {}: {}", email, e.getMessage());
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Failed to send OTP. Please try again.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Object> data = new ArrayList<>();
        data.add(email);

        if ("INACTIVE_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE_PENDING",
                            "Your branch account is scheduled for inactivation. Please contact your administrator if this was not intended.",
                            data),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your branch account has been scheduled for permanent block. Please contact your administrator immediately.",
                            data),
                    HttpStatus.OK);
        }
        if ("ACTIVE_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("ACTIVE_PENDING",
                            "Your branch account reactivation is in progress. You may proceed to login — your account will be fully active shortly.",
                            data),
                    HttpStatus.OK);
        }
        data.add(user.getPasswordUpdatedAt() != null
                ? user.getPasswordUpdatedAt().toString()
                : null);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent successfully.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // directLogin — No OTP (for direct auth flow)
    // POST /test/api/v1/Branch Bank/direct-login
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> directLogin(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.directLogin — BranchCode={} username={}",
                dto.getBranchCode(), dto.getUsername());

        if (dto.getBranchCode() == null || dto.getBranchCode().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Bank Code is required.", null),
                    HttpStatus.BAD_REQUEST);
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String enteredCode = dto.getBranchCode().trim();
        String enteredUser = dto.getUsername().trim();

        Optional<BranchAdmin> optUser = branchAdminRepository
                .findByBranchCodeAndUsername(enteredCode, enteredUser);

        // Bridge via BranchBank if composite miss.
        // Skip BLOCKED records so re-onboarded users with same email can log in.
        if (!optUser.isPresent()) {
            Optional<BranchBank> bnkOpt = branchBankRepository
                    .findByBranchCodeAndBranchAdminId(enteredCode, enteredUser);
            if (bnkOpt.isPresent() && bnkOpt.get().getPrimaryEmail() != null) {
                optUser = branchAdminRepository.findFirstByEmailAndStatusNot(
                        bnkOpt.get().getPrimaryEmail().trim(), "BLOCKED");
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Bank Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        BranchAdmin user = optUser.get();

        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String status = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Bank Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your bank account is currently inactive. Please contact your administrator.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your bank account has been scheduled for permanent block. Please contact your administrator immediately.",
                            null),
                    HttpStatus.OK);
        }

        if (dto.getDefaultPassword() == null || dto.getDefaultPassword().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Password is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(dto.getDefaultPassword(), user.getPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid password. Please try again.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        // JWT generate
        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .password("")
                .authorities(new ArrayList<>())
                .build();

        String accessToken  = jwtHelper.generateToken(userDetails, "BRANCH_ADMIN");
        String refreshToken = jwtHelper.generateTokenForRefresh(user.getUsername());

        List<Object> data = new ArrayList<>();
        data.add(user.getUsername());
        data.add(user.getBranchCode());
        data.add(accessToken);
        data.add(refreshToken);

        logger.info("branchAdmin.directLogin — success for username={} BranchCode={}",
                user.getUsername(), user.getBranchCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Login successful.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // forgotPassword — OTP bhejo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequestDto request) {
        logger.info("branchAdmin.forgotPassword — email={} username={}", request.getEmail(), request.getUsername());

        BranchAdmin user = null;

        // Skip BLOCKED records so re-onboarded user can reset password
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<BranchAdmin> byEmail = branchAdminRepository.findFirstByEmailAndStatusNot(
                    request.getEmail().trim(), "BLOCKED");
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {
            Optional<BranchAdmin> byUsername = branchAdminRepository.findByBranchCodeAndUsername(
                    request.getBankCode().trim(), request.getUsername().trim());
            if (byUsername.isPresent()) user = byUsername.get();
        }

        if (user == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS",
                            "If your credentials are valid, an OTP has been sent to your registered email.",
                            new ArrayList<>()),
                    HttpStatus.OK);
        }

        if ("BLOCKED".equalsIgnoreCase(user.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Account is blocked. Contact administrator.", null),
                    HttpStatus.FORBIDDEN);
        }

        String otp = generateOtp();
        user.setForgotOtp(otp);
        user.setForgotOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setUpdatedAt(LocalDateTime.now());
        branchAdminRepository.save(user);

        String email = user.getEmail();
        try {
            emailService.sendForgotPasswordOtp(email, user.getUsername(), otp, 10);
            logger.info("branchAdmin forgot password OTP sent to: {}", email);
        } catch (Exception e) {
            logger.error("branchAdmin OTP send failed for {}: {}", email, e.getMessage());
            user.setForgotOtp(null);
            user.setForgotOtpExpiry(null);
            branchAdminRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Failed to send OTP. Please try again.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Object> data = new ArrayList<>();
        data.add(maskEmail(email));
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent to your registered email.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // verifyForgotOtp — OTP sirf verify karo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequestDto request) {
        BranchAdmin user = findUserForForgotPassword(request);
        if (user == null) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "Invalid credentials.", null), HttpStatus.UNAUTHORIZED);

        if (user.getForgotOtp() == null) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "No OTP found. Please request a new one.", null), HttpStatus.BAD_REQUEST);

        if (LocalDateTime.now().isAfter(user.getForgotOtpExpiry())) {
            user.setForgotOtp(null); user.setForgotOtpExpiry(null);
            branchAdminRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "OTP has expired. Please request a new one.", null), HttpStatus.BAD_REQUEST);
        }

        String otpInput = request.getOtp() != null ? request.getOtp().trim() : "";
        if (!user.getForgotOtp().trim().equals(otpInput)) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "Invalid OTP. Please try again.", null), HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP verified successfully.", new ArrayList<>()), HttpStatus.OK);
    }

    // =========================================================================
    // resetPassword — OTP verify + new password set
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request) {
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Passwords do not match.", null), HttpStatus.BAD_REQUEST);
        }

        BranchAdmin user = findUserByEmailOrUsername(
                request.getEmail(), request.getUsername(), request.getBankCode());
        if (user == null) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "Invalid credentials.", null), HttpStatus.UNAUTHORIZED);

        if (user.getForgotOtp() == null) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "No OTP found. Please request a new one.", null), HttpStatus.BAD_REQUEST);

        if (LocalDateTime.now().isAfter(user.getForgotOtpExpiry())) {
            user.setForgotOtp(null); user.setForgotOtpExpiry(null);
            branchAdminRepository.save(user);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "OTP has expired.", null), HttpStatus.BAD_REQUEST);
        }

        if (!user.getForgotOtp().trim().equals(request.getOtp().trim())) return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", "Invalid OTP.", null), HttpStatus.UNAUTHORIZED);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(1);
        user.setForgotOtp(null);
        user.setForgotOtpExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        branchAdminRepository.save(user);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Password reset successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // activateBranchAdmin — After OTP verified → set status ACTIVE
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> activateBranchAdmin(String email) {
        // Find the active (non-BLOCKED) BranchAdmin by email — newest record first (highest ID).
        // If the same email was re-onboarded after a BLOCK, OrderByIdAsc would wrongly return the
        // old BLOCKED record and the new branch bank would never become ACTIVE after first login.
        Optional<BranchAdmin> optUser = branchAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(email, "BLOCKED");

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "Login successful.", new ArrayList<>()), HttpStatus.OK);
        }

        BranchAdmin user = optUser.get();

        Optional<BranchBank> optInst = branchBankRepository
                .findByBranchCodeAndBranchAdminId(user.getBranchCode(), user.getUsername());

        if (!optInst.isPresent()) {
            // Replacement branch admin — no BranchBank record; promote VERIFIED → ACTIVE on first login
            if ("VERIFIED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
                user.setUpdatedAt(LocalDateTime.now());
                user.setUpdatedBy("SYSTEM");
                branchAdminRepository.save(user);
                logger.info("[ACTIVATE] Replacement branch admin {} → ACTIVE after first login", user.getUsername());
            }
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "Login successful.", new ArrayList<>()), HttpStatus.OK);
        }

        BranchBank bank = optInst.get();

        if ("BLOCKED".equals(bank.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Blocked bank cannot be activated.", null),
                    HttpStatus.FORBIDDEN);
        }

        if ("VERIFIED".equals(bank.getStatus())) {
            bank.setStatus("ACTIVE");
            bank.setUpdatedAt(LocalDateTime.now());
            branchBankRepository.save(bank);
            // Sync to BRANCH_ADMIN; capture full name for future reference
            user.setStatus("ACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy("SYSTEM");
            if (user.getFullName() == null && bank.getPrimaryFullName() != null) {
                user.setFullName(bank.getPrimaryFullName());
            }
            branchAdminRepository.save(user);
            logger.info("[BRANCH-ACTIVATE] BranchBank {} status → ACTIVE after first login",
                    bank.getBranchCode());
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "bank activated successfully.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private BranchAdmin findUserForForgotPassword(ForgotPasswordRequestDto request) {
        return findUserByEmailOrUsername(request.getEmail(), request.getUsername(), request.getBankCode());
    }

    private BranchAdmin findUserByEmailOrUsername(String email, String username, String BranchCode) {
        // Skip BLOCKED records — re-onboarded user with same email must not hit old BLOCKED record
        if (email != null && !email.trim().isEmpty()) {
            Optional<BranchAdmin> byEmail = branchAdminRepository.findFirstByEmailAndStatusNot(email.trim(), "BLOCKED");
            if (byEmail.isPresent()) return byEmail.get();
        }
        if (username != null && !username.trim().isEmpty() &&
                BranchCode != null && !BranchCode.trim().isEmpty()) {
            Optional<BranchAdmin> byUsername = branchAdminRepository.findByBranchCodeAndUsername(
                    BranchCode.trim(), username.trim());
            if (byUsername.isPresent()) return byUsername.get();
        }
        return null;
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local  = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        StringBuilder sb = new StringBuilder();
        sb.append(local.charAt(0));
        for (int i = 0; i < local.length() - 2; i++) sb.append('*');
        sb.append(local.charAt(local.length() - 1));
        sb.append('@').append(domain);
        return sb.toString();
    }

    // ── Schedule / Undo status transitions for Branch Admin ─────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long id, String scheduledBy) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin must be ACTIVE to schedule inactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE_PENDING");
        admin.setInactivateScheduledAt(LocalDateTime.now());
        admin.setInactivateScheduledBy(scheduledBy);
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String inactivateAt = admin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendInactivatePendingWarning(contact[0], contact[1],
                    branchName, admin.getBranchCode(), inactivateAt);
        } catch (Exception e) {
            logger.warn("scheduleInactivate: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation scheduled for branch admin {} by {}", id, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation scheduled. Branch admin will be INACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoInactivate(Long id, String undoneBy) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"INACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled inactivation found for this branch admin.", null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE");
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        // Cancel any pending replacement since inactivation was undone
        try {
            Optional<AdminReplacement> pendingRep = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "PENDING");
            if (pendingRep.isPresent()) {
                pendingRep.get().setStatus("CANCELLED");
                adminReplacementRepository.save(pendingRep.get());
                logger.info("Cancelled PENDING replacement for branch admin {} due to undo", admin.getUsername());
            }
        } catch (Exception e) {
            logger.warn("undoInactivate: failed to cancel pending replacement for {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendInactivateCancelled(contact[0], contact[1],
                    branchName, admin.getBranchCode());
        } catch (Exception e) {
            logger.warn("undoInactivate: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation undone for branch admin {} by {}. Restored to ACTIVE", id, undoneBy);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation cancelled. Branch admin restored to ACTIVE.", data), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long id, String scheduledBy) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"INACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin must be INACTIVE to schedule reactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE_PENDING");
        admin.setReactivateScheduledAt(LocalDateTime.now());
        admin.setReactivateScheduledBy(scheduledBy);
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();
        // Transition any active replacement to INACTIVE_PENDING so they are notified their tenure is ending
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        BranchAdmin repAdmin = repAdminOpt.get();
                        if ("ACTIVE".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("INACTIVE_PENDING");
                            repAdmin.setInactivateScheduledAt(LocalDateTime.now());
                            repAdmin.setInactivateScheduledBy(scheduledBy);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(scheduledBy);
                            branchAdminRepository.save(repAdmin);
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    Optional<BranchBank> repBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                                    String repBranchName = repBranchOpt.isPresent() ? repBranchOpt.get().getBranchNameFull() : admin.getBranchCode();
                                    String inactivateAt = repAdmin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                                    emailService.sendInactivatePendingWarning(repAdmin.getEmail(), repAdmin.getUsername(),
                                            repBranchName, admin.getBranchCode(), inactivateAt);
                                }
                            } catch (Exception e2) {
                                logger.warn("scheduleReactivate: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleReactivate: replacement transition failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String reactivateAt = admin.getReactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendReactivatePendingNotification(contact[0], contact[1],
                    branchName, admin.getBranchCode(), reactivateAt);
            final String branchNameCopy = branchName;
            final String reactivateAtCopy = reactivateAt;
            notifyActor(scheduledBy, "Reactivation Scheduled", branchNameCopy, admin.getBranchCode(), reactivateAtCopy);
        } catch (Exception e) {
            logger.warn("scheduleReactivate: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation scheduled for branch admin {} by {}", id, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation scheduled. Branch admin will be ACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoReactivate(Long id, String undoneBy) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"ACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled reactivation found for this branch admin.", null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE");
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        // Restore replacement admin to ACTIVE since reactivation was undone
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        BranchAdmin repAdmin = repAdminOpt.get();
                        if ("INACTIVE_PENDING".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("ACTIVE");
                            repAdmin.setInactivateScheduledAt(null);
                            repAdmin.setInactivateScheduledBy(null);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(undoneBy);
                            branchAdminRepository.save(repAdmin);
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    Optional<BranchBank> repBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                                    String repBranchName = repBranchOpt.isPresent() ? repBranchOpt.get().getBranchNameFull() : admin.getBranchCode();
                                    emailService.sendInactivateCancelled(repAdmin.getEmail(), repAdmin.getUsername(),
                                            repBranchName, admin.getBranchCode());
                                }
                            } catch (Exception e2) {
                                logger.warn("undoReactivate: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("undoReactivate: replacement restoration failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendReactivateCancelled(contact[0], contact[1],
                    branchName, admin.getBranchCode());
            final String branchNameCopy = branchName;
            notifyActor(undoneBy, "Reactivation Cancelled", branchNameCopy, admin.getBranchCode(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        } catch (Exception e) {
            logger.warn("undoReactivate: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation undone for branch admin {} by {}. Restored to INACTIVE", id, undoneBy);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation cancelled. Branch admin restored to INACTIVE.", data), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long id, String scheduledBy, String reason) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if ("BLOCKED".equalsIgnoreCase(admin.getStatus()) || "BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin is already blocked or pending block.", null), HttpStatus.OK);
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
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();

        // Pre-compute email helpers — shared by cascade + admin email + actor email
        Optional<BranchBank> branchForEmail = branchBankRepository.findByBranchCode(admin.getBranchCode());
        final String branchNameForEmail = branchForEmail.isPresent() ? branchForEmail.get().getBranchNameFull() : admin.getBranchCode();
        final String blockAtForEmail = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        // Chain cascade only when admin was ACTIVE; INACTIVE admin → individual block only
        try {
            Optional<BranchBank> parentBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            if ("ACTIVE".equalsIgnoreCase(admin.getPreBlockStatus()) && parentBranchOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBranchOpt.get().getStatus())) {
                List<AddUser> branchUsers = addUserRepository.findByBranchCode(admin.getBranchCode());
                for (AddUser user : branchUsers) {
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
                        try {
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                emailService.sendBlockWarning(user.getEmail(),
                                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                        branchNameForEmail, admin.getBranchCode(), blockAtForEmail);
                            }
                        } catch (Exception emailEx) {
                            logger.warn("scheduleBlock: user email failed for {}: {}", user.getUsername(), emailEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock branch admin: user cascade failed for branch {}: {}", admin.getBranchCode(), e.getMessage());
        }

        try {
            // Individual block — email goes directly to the admin being blocked, not their replacement
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(),
                    branchNameForEmail, admin.getBranchCode(), blockAtForEmail);
        } catch (Exception e) {
            logger.warn("scheduleBlock: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        notifyActor(scheduledBy, "Block Scheduled", branchNameForEmail, admin.getBranchCode(), blockAtForEmail);
        logger.info("Block scheduled for branch admin {} by {}", id, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block scheduled. Branch admin will be BLOCKED in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long id, String undoneBy) {
        Optional<BranchAdmin> opt = branchAdminRepository.findById(id);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found: " + id, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled block found for this branch admin.", null), HttpStatus.OK);
        }
        String restored = admin.getPreBlockStatus() != null ? admin.getPreBlockStatus() : "INACTIVE";
        admin.setStatus(restored);
        admin.setBlockScheduledAt(null);
        admin.setPreBlockStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendBlockCancelled(contact[0], contact[1],
                    branchName, admin.getBranchCode(), restored);
        } catch (Exception e) {
            logger.warn("undoBlock: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block undone for branch admin {} by {}. Restored to {}", id, undoneBy, restored);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Branch admin restored to " + restored + ".", data), HttpStatus.OK);
    }

    // ─── BranchBank-ID based operations (for KalAdmin Admin Status page) ─────────

    private Optional<BranchAdmin> findAdminByBranchBankId(Long branchBankId) {
        Optional<BranchBank> branchOpt = branchBankRepository.findById(branchBankId);
        if (!branchOpt.isPresent()) return Optional.empty();
        BranchBank branch = branchOpt.get();
        return branchAdminRepository.findByBranchCodeAndUsername(branch.getBranchCode(), branch.getBranchAdminId());
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivateByBranchBankId(Long branchBankId, String scheduledBy) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin must be ACTIVE to schedule inactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE_PENDING");
        admin.setInactivateScheduledAt(LocalDateTime.now());
        admin.setInactivateScheduledBy(scheduledBy);
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String inactivateAt = admin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            emailService.sendInactivatePendingWarning(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), inactivateAt);
            final String branchNameCopy = branchName;
            final String inactivateAtCopy = inactivateAt;
            notifyActor(scheduledBy, "Inactivation Scheduled", branchNameCopy, admin.getBranchCode(), inactivateAtCopy);
        } catch (Exception e) {
            logger.warn("scheduleInactivateByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation scheduled for branch admin (branch {}) by {}", branchBankId, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation scheduled. Branch admin will be INACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoInactivateByBranchBankId(Long branchBankId, String undoneBy) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"INACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled inactivation found for this branch admin.", null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE");
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        // Cancel any pending replacement since inactivation was undone
        try {
            Optional<AdminReplacement> pendingRep = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "PENDING");
            if (pendingRep.isPresent()) {
                pendingRep.get().setStatus("CANCELLED");
                adminReplacementRepository.save(pendingRep.get());
                logger.info("Cancelled PENDING replacement for branch admin {} due to undo", admin.getUsername());
            }
        } catch (Exception e) {
            logger.warn("undoInactivateByBranchBankId: failed to cancel pending replacement for {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            emailService.sendInactivateCancelled(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode());
            final String branchNameCopy = branchName;
            notifyActor(undoneBy, "Inactivation Cancelled", branchNameCopy, admin.getBranchCode(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        } catch (Exception e) {
            logger.warn("undoInactivateByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Inactivation undone for branch admin (branch {}) by {}", branchBankId, undoneBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation cancelled. Branch admin restored to ACTIVE.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivateByBranchBankId(Long branchBankId, String scheduledBy) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"INACTIVE".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin must be INACTIVE to schedule reactivation. Current: " + admin.getStatus(), null), HttpStatus.OK);
        }
        admin.setStatus("ACTIVE_PENDING");
        admin.setReactivateScheduledAt(LocalDateTime.now());
        admin.setReactivateScheduledBy(scheduledBy);
        admin.setInactivateScheduledAt(null);
        admin.setInactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();
        // Transition any active replacement to INACTIVE_PENDING so they are notified their tenure is ending
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        BranchAdmin repAdmin = repAdminOpt.get();
                        if ("ACTIVE".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("INACTIVE_PENDING");
                            repAdmin.setInactivateScheduledAt(LocalDateTime.now());
                            repAdmin.setInactivateScheduledBy(scheduledBy);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(scheduledBy);
                            branchAdminRepository.save(repAdmin);
                            logger.info("Replacement branch admin {} set to INACTIVE_PENDING as original {} is reactivating", repAdmin.getUsername(), admin.getUsername());
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    Optional<BranchBank> repBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                                    String repBranchName = repBranchOpt.isPresent() ? repBranchOpt.get().getBranchNameFull() : admin.getBranchCode();
                                    String inactivateAt = repAdmin.getInactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                                    emailService.sendInactivatePendingWarning(repAdmin.getEmail(), repAdmin.getUsername(),
                                            repBranchName, admin.getBranchCode(), inactivateAt);
                                }
                            } catch (Exception e2) {
                                logger.warn("scheduleReactivateByBranchBankId: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleReactivateByBranchBankId: replacement transition failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String reactivateAt = admin.getReactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendReactivatePendingNotification(contact[0], contact[1],
                    branchName, admin.getBranchCode(), reactivateAt);
            final String branchNameCopy = branchName;
            final String reactivateAtCopy = reactivateAt;
            notifyActor(scheduledBy, "Reactivation Scheduled", branchNameCopy, admin.getBranchCode(), reactivateAtCopy);
        } catch (Exception e) {
            logger.warn("scheduleReactivateByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation scheduled for branch admin (branch {}) by {}", branchBankId, scheduledBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation scheduled. Branch admin will be ACTIVE in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoReactivateByBranchBankId(Long branchBankId, String undoneBy) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"ACTIVE_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled reactivation found for this branch admin.", null), HttpStatus.OK);
        }
        admin.setStatus("INACTIVE");
        admin.setReactivateScheduledAt(null);
        admin.setReactivateScheduledBy(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        // Restore replacement admin to ACTIVE since reactivation was undone
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            for (AdminReplacement rep : reps) {
                if (rep.getReplacementEntityId() != null) {
                    Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        BranchAdmin repAdmin = repAdminOpt.get();
                        if ("INACTIVE_PENDING".equalsIgnoreCase(repAdmin.getStatus())) {
                            repAdmin.setStatus("ACTIVE");
                            repAdmin.setInactivateScheduledAt(null);
                            repAdmin.setInactivateScheduledBy(null);
                            repAdmin.setUpdatedAt(LocalDateTime.now());
                            repAdmin.setUpdatedBy(undoneBy);
                            branchAdminRepository.save(repAdmin);
                            logger.info("Replacement branch admin {} restored to ACTIVE as original {} reactivation was undone", repAdmin.getUsername(), admin.getUsername());
                            try {
                                if (repAdmin.getEmail() != null && !repAdmin.getEmail().isEmpty()) {
                                    Optional<BranchBank> repBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                                    String repBranchName = repBranchOpt.isPresent() ? repBranchOpt.get().getBranchNameFull() : admin.getBranchCode();
                                    emailService.sendInactivateCancelled(repAdmin.getEmail(), repAdmin.getUsername(),
                                            repBranchName, admin.getBranchCode());
                                }
                            } catch (Exception e2) {
                                logger.warn("undoReactivateByBranchBankId: replacement email failed for {}: {}", repAdmin.getUsername(), e2.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("undoReactivateByBranchBankId: replacement restoration failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendReactivateCancelled(contact[0], contact[1],
                    branchName, admin.getBranchCode());
            final String branchNameCopy = branchName;
            notifyActor(undoneBy, "Reactivation Cancelled", branchNameCopy, admin.getBranchCode(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        } catch (Exception e) {
            logger.warn("undoReactivateByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Reactivation undone for branch admin (branch {}) by {}", branchBankId, undoneBy);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation cancelled. Branch admin restored to INACTIVE.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlockByBranchBankId(Long branchBankId, String scheduledBy, String reason) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if ("BLOCKED".equalsIgnoreCase(admin.getStatus()) || "BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin is already blocked or pending block.", null), HttpStatus.OK);
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
        branchAdminRepository.save(admin);
        BlockScheduleServiceImpl.flagPendingWork();

        // Pre-compute email helpers — shared by cascade + admin email + actor email
        Optional<BranchBank> branchForEmail2 = branchBankRepository.findByBranchCode(admin.getBranchCode());
        final String branchNameForEmail2 = branchForEmail2.isPresent() ? branchForEmail2.get().getBranchNameFull() : admin.getBranchCode();
        final String blockAtForEmail2 = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        // Chain cascade only when admin was ACTIVE; INACTIVE → individual block only
        if ("ACTIVE".equalsIgnoreCase(preStatus)) {
            try {
                Optional<BranchBank> parentBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                if (parentBranchOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBranchOpt.get().getStatus())) {
                    List<AddUser> branchUsers = addUserRepository.findByBranchCode(admin.getBranchCode());
                    for (AddUser user : branchUsers) {
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
                            try {
                                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                    emailService.sendBlockWarning(user.getEmail(),
                                            user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                            branchNameForEmail2, admin.getBranchCode(), blockAtForEmail2);
                                }
                            } catch (Exception emailEx) {
                                logger.warn("scheduleBlockByBranchBankId: user email failed for {}: {}", user.getUsername(), emailEx.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("scheduleBlockByBranchBankId: user cascade failed for branch {}: {}", branchBankId, e.getMessage());
            }
        }

        try {
            // Individual block — email goes directly to the admin being blocked, not their replacement
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(),
                    branchNameForEmail2, admin.getBranchCode(), blockAtForEmail2);
        } catch (Exception e) {
            logger.warn("scheduleBlockByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        notifyActor(scheduledBy, "Block Scheduled", branchNameForEmail2, admin.getBranchCode(), blockAtForEmail2);
        logger.info("Block scheduled for branch admin (branch {}) by {}. Pre-status: {}", branchBankId, scheduledBy, preStatus);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block scheduled. Branch admin will be BLOCKED in 30 seconds.", new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlockByBranchBankId(Long branchBankId, String undoneBy) {
        Optional<BranchAdmin> opt = findAdminByBranchBankId(branchBankId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Branch admin not found for branch: " + branchBankId, null), HttpStatus.OK);
        }
        BranchAdmin admin = opt.get();
        if (!"BLOCK_PENDING".equalsIgnoreCase(admin.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No scheduled block found for this branch admin.", null), HttpStatus.OK);
        }
        String restored = admin.getPreBlockStatus() != null ? admin.getPreBlockStatus() : "INACTIVE";
        admin.setStatus(restored);
        admin.setBlockScheduledAt(null);
        admin.setBlockScheduledBy(null);
        admin.setBlockReason(null);
        admin.setPreBlockStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String[] contact = resolveBranchAdminContact(admin);
            emailService.sendBlockCancelled(contact[0], contact[1],
                    branchName, admin.getBranchCode(), restored);
            final String branchNameCopy = branchName;
            notifyActor(undoneBy, "Block Cancelled", branchNameCopy, admin.getBranchCode(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        } catch (Exception e) {
            logger.warn("undoBlockByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block undone for branch admin (branch {}) by {}. Restored to {}", branchBankId, undoneBy, restored);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Branch admin restored to " + restored + ".", new ArrayList<>()), HttpStatus.OK);
    }

    private void notifyActor(String actorBy, String action, String targetName, String targetCode, String when) {
        try {
            Optional<MainAdmin> ma = mainAdminRepository.findFirstByUsername(actorBy);
            if (ma.isPresent() && ma.get().getEmail() != null && !ma.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ma.get().getEmail(), ma.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            Optional<BranchAdmin> ba = branchAdminRepository.findFirstByUsername(actorBy);
            if (ba.isPresent() && ba.get().getEmail() != null && !ba.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ba.get().getEmail(), ba.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            kalAdminRepository.findByUserName(actorBy).ifPresent(ka -> {
                if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                    emailService.sendActorActionConfirmation(ka.getEmailId(), ka.getUserName(), action, targetName, targetCode, when);
                }
            });
        } catch (Exception e) {
            logger.warn("[ACTOR-CONFIRM] Email failed for {}: {}", actorBy, e.getMessage());
        }
    }

    private String[] resolveBranchAdminContact(BranchAdmin admin) {
        try {
            List<AdminReplacement> reps = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                            admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
            if (!reps.isEmpty() && reps.get(0).getReplacementEntityId() > 0) {
                Optional<BranchAdmin> rep = branchAdminRepository.findById(reps.get(0).getReplacementEntityId());
                if (rep.isPresent() && rep.get().getEmail() != null && !rep.get().getEmail().isEmpty()) {
                    return new String[]{rep.get().getEmail(), rep.get().getUsername()};
                }
            }
        } catch (Exception e) {
            logger.warn("resolveBranchAdminContact failed for {}: {}", admin.getUsername(), e.getMessage());
        }
        return new String[]{admin.getEmail(), admin.getUsername()};
    }

    // =========================================================================
    // getAllBranchAdmins — fetch branch admins directly from BRANCH_ADMIN table
    // GET /test/api/v1/branch/get-all-admins
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> getAllBranchAdmins(String callerUsername) {
        List<BranchAdmin> admins;

        // Resolve caller's bank to filter branch admins
        Optional<MainAdmin> callerOpt = mainAdminRepository.findFirstByUsername(callerUsername);
        if (callerOpt.isPresent()) {
            String bankCode = callerOpt.get().getBankCode();
            Optional<MainBank> bankOpt2 = mainBankRepository.findByBankCode(bankCode);
            if (bankOpt2.isPresent()) {
                List<BranchBank> branches = branchBankRepository.findByParentBankId(bankOpt2.get().getBankId());
                List<String> branchCodes = new ArrayList<>();
                for (BranchBank bb : branches) branchCodes.add(bb.getBranchCode());
                admins = branchCodes.isEmpty() ? new ArrayList<>()
                    : branchAdminRepository.findAllByBranchCodeIn(branchCodes);
            } else {
                admins = new ArrayList<>();
            }
        } else {
            admins = branchAdminRepository.findAll();
        }

        List<Object> result = new ArrayList<>();

        for (BranchAdmin admin : admins) {
            // Skip admins who are currently acting as a replacement for someone else —
            // they will appear as a replacement row under their original admin.
            // Exception: if this admin is ALSO being replaced themselves, show them as a
            // primary row (they have moved on from the replacement role).
            boolean isRestoredReplacement = false;
            List<AdminReplacement> asRepOf = adminReplacementRepository
                .findByReplacementEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT", "RESTORED"));
            if (!asRepOf.isEmpty()) {
                List<AdminReplacement> theirOwnReps = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                        admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                if (theirOwnReps.isEmpty()) {
                    boolean allRestored = asRepOf.stream().allMatch(r -> "RESTORED".equals(r.getStatus()));
                    if (!allRestored) {
                        continue; // Still an ACTIVE/PERMANENT replacement — show under original
                    }
                    if ("INACTIVE".equalsIgnoreCase(admin.getStatus())) {
                        continue; // Fully INACTIVE — hide from list
                    }
                    isRestoredReplacement = true;
                }
            }

            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());

            List<AdminReplacement> reps = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                    admin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));

            boolean hasPermanentRep = false;
            for (AdminReplacement r : reps) {
                if ("PERMANENT".equals(r.getStatus())) { hasPermanentRep = true; break; }
            }

            String displayName = branchOpt.isPresent()
                ? (hasPermanentRep
                    ? (admin.getFullName() != null ? admin.getFullName() : admin.getUsername())
                    : branchOpt.get().getPrimaryFullName())
                : (admin.getFullName() != null ? admin.getFullName() : admin.getUsername());

            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("adminId", admin.getId());
            row.put("adminUsername", admin.getUsername());
            row.put("adminStatus", admin.getStatus());
            row.put("branchCode", admin.getBranchCode());
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

            if (branchOpt.isPresent()) {
                BranchBank branch = branchOpt.get();
                row.put("branchId", branch.getBranchId());
                row.put("branchNameFull", branch.getBranchNameFull());
                row.put("branchNameShort", branch.getBranchNameShort() != null ? branch.getBranchNameShort() : branch.getBranchNameFull());
                row.put("primaryMobile", branch.getPrimaryMobile());
                row.put("primaryMobileCode", branch.getPrimaryMobileCode());
                row.put("regCity", branch.getRegCity());
                row.put("commCity", branch.getCommCity());
            }

            if (!reps.isEmpty()) {
                AdminReplacement rep = reps.get(0);
                row.put("replacementStatus", rep.getStatus());
                if (rep.getReplacementEntityId() != null) {
                    Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        BranchAdmin repAdmin = repAdminOpt.get();
                        row.put("replacedByUsername", repAdmin.getEmail());

                        // Only add repRow if the replacement admin is NOT themselves being replaced.
                        // If they are, they will appear as their own primary row in the loop,
                        // and adding them here too would cause a duplicate entry.
                        List<AdminReplacement> repAdminOwnReps = adminReplacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                repAdmin.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                        if (repAdminOwnReps.isEmpty()) {
                            java.util.Map<String, Object> repRow = new java.util.HashMap<>();
                            repRow.put("adminId", repAdmin.getId());
                            repRow.put("adminStatus", repAdmin.getStatus());
                            repRow.put("branchCode", repAdmin.getBranchCode());
                            repRow.put("primaryEmail", repAdmin.getEmail());
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
                            if (branchOpt.isPresent()) {
                                BranchBank branch = branchOpt.get();
                                repRow.put("branchId", branch.getBranchId());
                                repRow.put("branchNameFull", branch.getBranchNameFull());
                                repRow.put("branchNameShort", branch.getBranchNameShort() != null ? branch.getBranchNameShort() : branch.getBranchNameFull());
                                repRow.put("primaryMobileCode", branch.getPrimaryMobileCode());
                                repRow.put("regCity", branch.getRegCity());
                                repRow.put("commCity", branch.getCommCity());
                            }
                            result.add(repRow);
                        }
                    }
                }
            }

            result.add(row);
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Branch admins fetched.", result));
    }
}
