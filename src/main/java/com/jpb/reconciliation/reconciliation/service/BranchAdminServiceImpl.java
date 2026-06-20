package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;

@Service
public class BranchAdminServiceImpl implements BranchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(BranchAdminServiceImpl.class);

    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private BranchBankRepository branchBankRepository;
    @Autowired private AddUserRepository addUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpService otpService;
    @Autowired private EmailService emailService;
    @Autowired private JwtHelper jwtHelper;
    @Autowired private AdminReplacementRepository adminReplacementRepository;

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
        branchAdmin.setUsername(username);
        branchAdmin.setEmail(bank.getPrimaryEmail());
        branchAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        branchAdmin.setPasswordSet(1);
        branchAdmin.setStatus("VERIFIED");
        branchAdmin.setCreatedAt(LocalDateTime.now());
        branchAdmin.setCreatedBy(bank.getCreatedBy());
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
        if ("BLOCKED".equalsIgnoreCase(userStatus) || "BLOCK".equalsIgnoreCase(userStatus)) {
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
        if ("BLOCKED".equalsIgnoreCase(status) || "BLOCK".equalsIgnoreCase(status)) {
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

        String accessToken  = jwtHelper.generateToken(userDetails);
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

        if ("BLOCK".equalsIgnoreCase(user.getStatus()) || "BLOCKED".equalsIgnoreCase(user.getStatus())) {
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
            // Sync to BRANCH_ADMIN
            user.setStatus("ACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy("SYSTEM");
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
            emailService.sendInactivatePendingWarning(admin.getEmail(), admin.getUsername(),
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
            emailService.sendInactivateCancelled(admin.getEmail(), admin.getUsername(),
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
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String reactivateAt = admin.getReactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            emailService.sendReactivatePendingNotification(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), reactivateAt);
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
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            emailService.sendReactivateCancelled(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode());
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

        // Chain cascade only when admin was ACTIVE; INACTIVE admin → individual block only
        try {
            Optional<BranchBank> parentBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            if ("ACTIVE".equalsIgnoreCase(admin.getPreBlockStatus()) && parentBranchOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBranchOpt.get().getStatus())) {
                List<AddUser> branchUsers = addUserRepository.findByBranchCode(admin.getBranchCode());
                for (AddUser user : branchUsers) {
                    if (user.getStatus() != AddUser.UserStatus.BLOCK && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
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
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock branch admin: user cascade failed for branch {}: {}", admin.getBranchCode(), e.getMessage());
        }

        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String blockAt = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), blockAt);
        } catch (Exception e) {
            logger.warn("scheduleBlock: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
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
            emailService.sendBlockCancelled(admin.getEmail(), admin.getUsername(),
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
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String reactivateAt = admin.getReactivateScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            emailService.sendReactivatePendingNotification(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), reactivateAt);
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
        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            emailService.sendReactivateCancelled(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode());
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

        // Chain cascade only when admin was ACTIVE; INACTIVE → individual block only
        if ("ACTIVE".equalsIgnoreCase(preStatus)) {
            try {
                Optional<BranchBank> parentBranchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                if (parentBranchOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBranchOpt.get().getStatus())) {
                    List<AddUser> branchUsers = addUserRepository.findByBranchCode(admin.getBranchCode());
                    for (AddUser user : branchUsers) {
                        if (user.getStatus() != AddUser.UserStatus.BLOCK && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
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
                }
            } catch (Exception e) {
                logger.warn("scheduleBlockByBranchBankId: user cascade failed for branch {}: {}", branchBankId, e.getMessage());
            }
        }

        try {
            Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
            String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
            String blockAt = admin.getBlockScheduledAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
            emailService.sendBlockWarning(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), blockAt);
        } catch (Exception e) {
            logger.warn("scheduleBlockByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
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
            emailService.sendBlockCancelled(admin.getEmail(), admin.getUsername(),
                    branchName, admin.getBranchCode(), restored);
        } catch (Exception e) {
            logger.warn("undoBlockByBranchBankId: email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
        }
        logger.info("Block undone for branch admin (branch {}) by {}. Restored to {}", branchBankId, undoneBy, restored);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Branch admin restored to " + restored + ".", new ArrayList<>()), HttpStatus.OK);
    }
}
