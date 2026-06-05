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

import com.jpb.reconciliation.reconciliation.dto.BranchAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
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
    // GET /test/api/v1/subinstitution/verify-email?institutionCode=xxx&username=yyy
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(String institutionCode, String username) {
        logger.info("branchAdmin.verifyEmail — institutionCode={} username={}", institutionCode, username);

        if (institutionCode == null || institutionCode.trim().isEmpty() ||
                username == null || username.trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Institution code and username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // defaultPassword==null in SUB_TEST_INSTITUTION means password was already set
        Optional<BranchBank> optInst = branchBankRepository
                .findByInstitutionCodeAndSuperUserId(institutionCode.trim(), username.trim());

        String userStatus;
        if (optInst.isPresent() && optInst.get().getDefaultPassword() == null) {
            userStatus = "OLD_USER";
            logger.info("branchAdmin.verifyEmail → OLD_USER for username={}", username);
        } else {
            userStatus = "NEW_USER";
            logger.info("branchAdmin.verifyEmail → NEW_USER for username={}", username);
        }

        MainAdminVerifyEmailResponseDto responseDto =
                new MainAdminVerifyEmailResponseDto(userStatus, institutionCode.trim(), username.trim());
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
        logger.info("branchAdmin.checkUserStatus — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<BranchBank> optInst = branchBankRepository
                .findByInstitutionCodeAndSuperUserId(dto.getInstitutionCode(), dto.getUsername());

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
    // POST /test/api/v1/subinstitution/verify-credentials
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.verifyCredentials — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (dto.getInstitutionCode() == null || dto.getUsername() == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Institution Code and Username are required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String institutionCode = dto.getInstitutionCode().trim();
        String username        = dto.getUsername().trim();

        Optional<BranchBank> optInst =
                branchBankRepository.findByInstitutionCodeAndSuperUserId(institutionCode, username);

        if (!optInst.isPresent()) {
            logger.warn("branchAdmin.verifyCredentials — institution not found: {} {}", institutionCode, username);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Invalid Institution Code or Username. Please check your email.", null),
                    HttpStatus.BAD_REQUEST);
        }

        BranchBank institution = optInst.get();

        // defaultPassword==null means password was already set
        if (institution.getDefaultPassword() == null) {
            logger.info("branchAdmin.verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // BCrypt match (Case A) + plain text fallback (Case B — old records)
        boolean passwordMatch = false;
        if (dto.getDefaultPassword() != null && institution.getDefaultPassword() != null) {
            try {
                passwordMatch = passwordEncoder.matches(
                        dto.getDefaultPassword(), institution.getDefaultPassword());
            } catch (Exception e) {
                logger.warn("branchAdmin: BCrypt match failed, trying plain text: {}", e.getMessage());
            }
            if (!passwordMatch) {
                passwordMatch = dto.getDefaultPassword().equals(institution.getDefaultPassword());
            }
        }

        if (!passwordMatch) {
            logger.warn("branchAdmin.verifyCredentials — password mismatch for {} {}", institutionCode, username);
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
    // POST /test/api/v1/subinstitution/set-password
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> setNewPassword(BranchAdminSetPasswordDto dto) {
        logger.info("branchAdmin.setNewPassword — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        Optional<BranchBank> optInst = branchBankRepository
                .findByInstitutionCodeAndSuperUserId(
                        dto.getInstitutionCode().trim(), dto.getUsername().trim());

        if (!optInst.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Institution not found. Please verify credentials first.", null),
                    HttpStatus.NOT_FOUND);
        }

        BranchBank institution = optInst.get();

        // defaultPassword==null means password was already set
        if (institution.getDefaultPassword() == null) {
            logger.info("branchAdmin.setNewPassword → ALREADY_VERIFIED for username={}", dto.getUsername());
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED",
                            "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        // INSERT new BRANCH_ADMIN record
        BranchAdmin branchAdmin = new BranchAdmin();
        branchAdmin.setInstitutionCode(dto.getInstitutionCode().trim());
        branchAdmin.setUsername(dto.getUsername().trim());
        branchAdmin.setEmail(institution.getPrimaryEmail());
        branchAdmin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        branchAdmin.setPasswordSet(1);
        branchAdmin.setStatus("VERIFIED");
        branchAdmin.setCreatedAt(LocalDateTime.now());
        branchAdmin.setCreatedBy(institution.getCreatedBy());
        branchAdminRepository.save(branchAdmin);
        logger.info("BRANCH_ADMIN record created for username={} institutionCode={}",
                dto.getUsername(), dto.getInstitutionCode());

        // Update SUB_TEST_INSTITUTION → VERIFIED, wipe defaultPassword & token
        institution.setStatus("VERIFIED");
        institution.setDefaultPassword(null);
        institution.setVerificationToken(null);
        institution.setTokenExpiry(LocalDateTime.now());
        institution.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(institution);
        logger.info("BranchBank {} status → VERIFIED after password setup", dto.getInstitutionCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS",
                        "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // =========================================================================
    // STEP 3 — login → OTP bhejo
    // POST /test/api/v1/subinstitution/login
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> login(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.login — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Username is required.", null),
                    HttpStatus.BAD_REQUEST);
        }

        Optional<BranchAdmin> optUser = Optional.empty();

        if (dto.getInstitutionCode() != null && !dto.getInstitutionCode().trim().isEmpty()) {
            optUser = branchAdminRepository.findByInstitutionCodeAndUsername(
                    dto.getInstitutionCode().trim(), dto.getUsername().trim());

            // Bridge lookup via BranchBank if composite miss
            if (!optUser.isPresent()) {
                Optional<BranchBank> instOpt = branchBankRepository
                        .findByInstitutionCodeAndSuperUserId(
                                dto.getInstitutionCode().trim(), dto.getUsername().trim());
                if (instOpt.isPresent() && instOpt.get().getPrimaryEmail() != null) {
                    optUser = branchAdminRepository.findFirstByEmail(instOpt.get().getPrimaryEmail().trim());
                }
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }

        BranchAdmin user = optUser.get();

        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE",
                            "Account setup incomplete. Please set your password first.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Status check from BRANCH_ADMIN (synced with SUB_TEST_INSTITUTION)
        String userStatus = user.getStatus();
        if ("BLOCKED".equalsIgnoreCase(userStatus) || "BLOCK".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your institution account is currently inactive. Please contact your administrator.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(userStatus)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your institution account has been scheduled for permanent block. Please contact your administrator immediately.",
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
        data.add(maskedEmail);

        return new ResponseEntity<>(
                new RestWithStatusList("OTP_SENT", "OTP sent to " + maskedEmail, data),
                HttpStatus.OK);
    }

    // =========================================================================
    // directLogin — No OTP (for direct auth flow)
    // POST /test/api/v1/subinstitution/direct-login
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> directLogin(BranchAdminVerifyDto dto) {
        logger.info("branchAdmin.directLogin — institutionCode={} username={}",
                dto.getInstitutionCode(), dto.getUsername());

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

        String enteredCode = dto.getInstitutionCode().trim();
        String enteredUser = dto.getUsername().trim();

        Optional<BranchAdmin> optUser = branchAdminRepository
                .findByInstitutionCodeAndUsername(enteredCode, enteredUser);

        // Bridge via BranchBank if composite miss
        if (!optUser.isPresent()) {
            Optional<BranchBank> instOpt = branchBankRepository
                    .findByInstitutionCodeAndSuperUserId(enteredCode, enteredUser);
            if (instOpt.isPresent() && instOpt.get().getPrimaryEmail() != null) {
                optUser = branchAdminRepository.findFirstByEmail(instOpt.get().getPrimaryEmail().trim());
            }
        }

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Invalid Institution Code or Username.", null),
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
                    new RestWithStatusList("FAILURE", "Invalid Institution Code or Username.", null),
                    HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your institution account is currently inactive. Please contact your administrator.",
                            null),
                    HttpStatus.OK);
        }
        if ("BLOCK_PENDING".equalsIgnoreCase(status)) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your institution account has been scheduled for permanent block. Please contact your administrator immediately.",
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
        data.add(user.getInstitutionCode());
        data.add(accessToken);
        data.add(refreshToken);

        logger.info("branchAdmin.directLogin — success for username={} institutionCode={}",
                user.getUsername(), user.getInstitutionCode());

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Login successful.", data),
                HttpStatus.OK);
    }

    // =========================================================================
    // forgotPassword — OTP bhejo
    // =========================================================================
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request) {
        logger.info("branchAdmin.forgotPassword — email={} username={}", request.getEmail(), request.getUsername());

        BranchAdmin user = null;

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<BranchAdmin> byEmail = branchAdminRepository.findFirstByEmail(request.getEmail().trim());
            if (byEmail.isPresent()) user = byEmail.get();
        }

        if (user == null &&
                request.getUsername() != null && !request.getUsername().trim().isEmpty() &&
                request.getInstitutionCode() != null && !request.getInstitutionCode().trim().isEmpty()) {
            Optional<BranchAdmin> byUsername = branchAdminRepository.findByInstitutionCodeAndUsername(
                    request.getInstitutionCode().trim(), request.getUsername().trim());
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
    public ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequest request) {
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
                request.getEmail(), request.getUsername(), request.getInstitutionCode());
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
        Optional<BranchAdmin> optUser = branchAdminRepository.findFirstByEmailOrderByIdAsc(email);

        if (!optUser.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "Login successful.", new ArrayList<>()), HttpStatus.OK);
        }

        BranchAdmin user = optUser.get();

        Optional<BranchBank> optInst = branchBankRepository
                .findByInstitutionCodeAndSuperUserId(user.getInstitutionCode(), user.getUsername());

        if (!optInst.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "Login successful.", new ArrayList<>()), HttpStatus.OK);
        }

        BranchBank institution = optInst.get();

        if ("BLOCKED".equals(institution.getStatus())) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Blocked institution cannot be activated.", null),
                    HttpStatus.FORBIDDEN);
        }

        if ("VERIFIED".equals(institution.getStatus())) {
            institution.setStatus("ACTIVE");
            institution.setUpdatedAt(LocalDateTime.now());
            branchBankRepository.save(institution);
            // Sync to BRANCH_ADMIN
            user.setStatus("ACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(user);
            logger.info("[BRANCH-ACTIVATE] BranchBank {} status → ACTIVE after first login",
                    institution.getInstitutionCode());
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Institution activated successfully.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private BranchAdmin findUserForForgotPassword(ForgotPasswordRequest request) {
        return findUserByEmailOrUsername(request.getEmail(), request.getUsername(), request.getInstitutionCode());
    }

    private BranchAdmin findUserByEmailOrUsername(String email, String username, String institutionCode) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<BranchAdmin> byEmail = branchAdminRepository.findFirstByEmail(email.trim());
            if (byEmail.isPresent()) return byEmail.get();
        }
        if (username != null && !username.trim().isEmpty() &&
                institutionCode != null && !institutionCode.trim().isEmpty()) {
            Optional<BranchAdmin> byUsername = branchAdminRepository.findByInstitutionCodeAndUsername(
                    institutionCode.trim(), username.trim());
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
}
