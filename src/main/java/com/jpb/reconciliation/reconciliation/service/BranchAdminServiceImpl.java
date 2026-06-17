package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
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
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;

@Service
public class BranchAdminServiceImpl implements BranchAdminService {

    private static final Logger logger = LoggerFactory.getLogger(BranchAdminServiceImpl.class);

    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private BranchBankRepository branchBankRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpService otpService;
    @Autowired private EmailService emailService;
    @Autowired private JwtHelper jwtHelper;

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

        // defaultPassword==null in Branch_Bank means password was already set
        Optional<BranchBank> optInst = branchBankRepository
                .findByBranchCodeAndBranchAdminId(BranchCode.trim(), username.trim());

        String userStatus;
        if (optInst.isPresent() && optInst.get().getDefaultPassword() == null) {
            userStatus = "OLD_USER";
            logger.info("branchAdmin.verifyEmail → OLD_USER for username={}", username);
        } else {
            userStatus = "NEW_USER";
            logger.info("branchAdmin.verifyEmail → NEW_USER for username={}", username);
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

        Optional<BranchBank> optInst = branchBankRepository
                .findByBranchCodeAndBranchAdminId(
                        dto.getBranchCode().trim(), dto.getUsername().trim());

        if (!optInst.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Bank not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        BranchBank bank = optInst.get();

        // defaultPassword==null means password was already set
        if (bank.getDefaultPassword() == null) {
            logger.info("branchAdmin.setNewPassword → ALREADY_VERIFIED for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // INSERT new BRANCH_ADMIN record
        BranchAdmin branchAdmin = new BranchAdmin();
        branchAdmin.setBranchCode(dto.getBranchCode().trim());
        branchAdmin.setUsername(dto.getUsername().trim());
        branchAdmin.setEmail(bank.getPrimaryEmail());
        branchAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        branchAdmin.setPasswordSet(1);
        branchAdmin.setStatus("VERIFIED");
        branchAdmin.setCreatedAt(LocalDateTime.now());
        branchAdmin.setCreatedBy(bank.getCreatedBy());
        branchAdminRepository.save(branchAdmin);
        logger.info("BRANCH_ADMIN record created for username={} BranchCode={}",
                dto.getUsername(), dto.getBranchCode());

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
                    new RestWithStatusList("FAILURE", "Invalid Bank Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your bank account is currently inactive. Please contact your administrator.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your bank account has been scheduled for permanent block. Please contact your administrator immediately.",
                            null),
                    HttpStatus.OK);
        }

        // Password match
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

        String maskedEmail = maskEmail(email);
        List<Object> data = new ArrayList<>();
        data.add(email); // actual email needed for OTP verification; masked only for display in statusMsg

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
        admin.setPreInactivateStatus(admin.getStatus());
        admin.setStatus("INACTIVE_PENDING");
        admin.setInactivateScheduledAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
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
        String restored = admin.getPreInactivateStatus() != null ? admin.getPreInactivateStatus() : "ACTIVE";
        admin.setStatus(restored);
        admin.setInactivateScheduledAt(null);
        admin.setPreInactivateStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        logger.info("Inactivation undone for branch admin {} by {}. Restored to {}", id, undoneBy, restored);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Inactivation cancelled. Branch admin restored to " + restored + ".", data), HttpStatus.OK);
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
        admin.setPreReactivateStatus(admin.getStatus());
        admin.setStatus("ACTIVE_PENDING");
        admin.setReactivateScheduledAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
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
        String restored = admin.getPreReactivateStatus() != null ? admin.getPreReactivateStatus() : "INACTIVE";
        admin.setStatus(restored);
        admin.setReactivateScheduledAt(null);
        admin.setPreReactivateStatus(null);
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(undoneBy);
        branchAdminRepository.save(admin);
        logger.info("Reactivation undone for branch admin {} by {}. Restored to {}", id, undoneBy, restored);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Reactivation cancelled. Branch admin restored to " + restored + ".", data), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long id, String scheduledBy) {
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
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(scheduledBy);
        branchAdminRepository.save(admin);
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
        logger.info("Block undone for branch admin {} by {}. Restored to {}", id, undoneBy, restored);
        List<Object> data = new ArrayList<>();
        data.add(admin);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Block cancelled. Branch admin restored to " + restored + ".", data), HttpStatus.OK);
    }
}
