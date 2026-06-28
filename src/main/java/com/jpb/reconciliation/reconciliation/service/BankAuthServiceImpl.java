package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.OtpManager;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.OtpManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class BankAuthServiceImpl implements BankAuthService {

    private static final Logger logger = LoggerFactory.getLogger(BankAuthServiceImpl.class);
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private ReconPasswordManagerRepository reconPasswordManagerRepository;
    @Autowired private OtpManagerRepository otpManagerRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtHelper jwtHelper;
    @Autowired private CustomUserDetailService customUserDetailService;
    @Autowired private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ── Step 0: verify-email ────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(String username, String bankCode) {
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(username.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            userOpt = reconUserRepository.findByEmail(username.trim().toLowerCase());
        }
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found.", null));
        }
        ReconUser user = userOpt.get();
        if ("BLOCKED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Account is BLOCKED. Contact administrator.", null));
        }
        if ("INACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Account is INACTIVE. Contact administrator.", null));
        }
        String userType = user.getPasswordSet() != null && user.getPasswordSet() == 1 ? "OLD_USER" : "NEW_USER";
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", userType, Collections.singletonList(user.getUsername())));
    }

    // ── Step 1: verify-credentials ──────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyCredentials(String username, String bankCode, String defaultPassword) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Password already set. Please login directly.", null));
        }

        if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, "verify-credentials failed", username, user.getUserType(), user.getBankId(), "Invalid default password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }

        auditLog("RCN_RECON_USER", user.getUserId(), "VERIFY",
                null, "default credentials verified", username, user.getUserType(), user.getBankId(), "Step 1 passed");
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Credentials verified.", Collections.singletonList(user.getUsername())));
    }

    // ── Step 2: set-password ────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> setPassword(String username, String bankCode,
                                                           String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Passwords do not match.", null));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Password must be at least 8 characters.", null));
        }

        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found.", null));
        }
        ReconUser user = userOpt.get();

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Password already set.", null));
        }

        // Check password not same as default
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "New password cannot be the same as the default password.", null));
        }

        // Check password history
        for (ReconPasswordManager pm : reconPasswordManagerRepository.findByReconUser(user)) {
            if (passwordEncoder.matches(newPassword, pm.getUserPassword())) {
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList("FAILURE", "Password was used before. Choose a different one.", null));
            }
        }

        String oldHash = user.getPasswordHash();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordSet(1);
        user.setStatus("VERIFIED");
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(username);
        reconUserRepository.save(user);

        // Save to password history
        ReconPasswordManager pm = new ReconPasswordManager();
        pm.setReconUser(user);
        pm.setUserPassword(user.getPasswordHash());
        pm.setCreatedAt(LocalDateTime.now());
        pm.setCreatedBy(username);
        pm.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pm);

        // Clear default password from RECON_BANK_MASTER
        if (bankCode != null && !bankCode.trim().isEmpty()) {
            reconBankMasterRepository.findByBankCode(bankCode.trim().toUpperCase()).ifPresent(bank -> {
                bank.setDefaultPassword(null);
                bank.setUpdatedAt(LocalDateTime.now());
                bank.setUpdatedBy(username);
                reconBankMasterRepository.save(bank);
            });
        }

        auditLog("RCN_RECON_USER", user.getUserId(), "UPDATE",
                "passwordSet=0,status=ACTIVE_PENDING",
                "passwordSet=1,status=VERIFIED",
                username, user.getUserType(), user.getBankId(), "Password set, status→VERIFIED");

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Password set successfully. Proceed to login.", null));
    }

    // ── Step 3a: login → send OTP ───────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> loginSendOtp(String username, String password) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        ResponseEntity<RestWithStatusList> guard = guardStatus(user);
        if (guard != null) return guard;

        if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Please set your password first.", null));
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, null, username, user.getUserType(), user.getBankId(), "Wrong password on login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }

        // Generate & send OTP
        String otp = generateOtp(6);
        otpManagerRepository.invalidatePreviousOtps(user.getEmail());
        OtpManager otpEntity = new OtpManager();
        otpEntity.setEmailId(user.getEmail());
        otpEntity.setOtpCode(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otpEntity.setIsUsed("N");
        otpEntity.setCreatedAt(LocalDateTime.now());
        otpManagerRepository.save(otpEntity);

        emailService.sendForgotPasswordOtp(user.getEmail(), user.getFullName(), otp, 10);

        auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN",
                null, "OTP sent", username, user.getUserType(), user.getBankId(), "Login step: OTP dispatched");

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "OTP sent to registered email.", null));
    }

    // ── Step 3b: verify OTP → status=ACTIVE → JWT ──────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyOtp(String username, String otpCode, HttpServletResponse response) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "User not found.", null));
        }
        ReconUser user = userOpt.get();

        Optional<OtpManager> otpOpt = otpManagerRepository
                .findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(user.getEmail(), "N");

        if (!otpOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "No active OTP found. Please request again.", null));
        }
        OtpManager otp = otpOpt.get();
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            otp.setIsUsed("Y");
            otpManagerRepository.save(otp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "OTP expired. Please login again.", null));
        }
        if (!otp.getOtpCode().equals(otpCode.trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid OTP.", null));
        }

        // Mark OTP used
        otp.setIsUsed("Y");
        otpManagerRepository.save(otp);

        // First-time OTP login: VERIFIED → ACTIVE; subsequent logins already ACTIVE
        if ("VERIFIED".equals(user.getStatus()) || "ACTIVE_PENDING".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
            user.setApprovedYn("Y");
            user.setApprovedBy("SYSTEM");
        }
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN",
                null, "status→ACTIVE", username, user.getUserType(), user.getBankId(), "OTP verified, login successful");

        return ResponseEntity.ok(buildJwtResponse(user, response));
    }

    // ── Step 3 (direct login — no OTP) ─────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> directLogin(String username, String password, HttpServletResponse response) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        ResponseEntity<RestWithStatusList> guard = guardStatus(user);
        if (guard != null) return guard;

        if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Please set your password first.", null));
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, null, username, user.getUserType(), user.getBankId(), "Wrong password (direct-login)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid credentials.", null));
        }

        if ("VERIFIED".equals(user.getStatus()) || "ACTIVE_PENDING".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
            user.setApprovedYn("Y");
            user.setApprovedBy("SYSTEM");
        }
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN",
                null, "direct-login,status→ACTIVE", username, user.getUserType(), user.getBankId(), "Direct login successful");

        return ResponseEntity.ok(buildJwtResponse(user, response));
    }

    // ── Forgot password: send OTP ───────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> forgotPasswordSendOtp(String email) {
        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            // Don't reveal if email exists
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "If this email is registered, an OTP will be sent.", null));
        }
        ReconUser user = userOpt.get();
        if ("BLOCKED".equals(user.getStatus()) || "INACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Account is " + user.getStatus() + ". Contact administrator.", null));
        }

        String otp = generateOtp(6);
        otpManagerRepository.invalidatePreviousOtps(user.getEmail());
        OtpManager otpEntity = new OtpManager();
        otpEntity.setEmailId(user.getEmail());
        otpEntity.setOtpCode(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otpEntity.setIsUsed("N");
        otpEntity.setCreatedAt(LocalDateTime.now());
        otpManagerRepository.save(otpEntity);

        emailService.sendForgotPasswordOtp(user.getEmail(), user.getFullName(), otp, 10);

        auditLog("RCN_RECON_USER", user.getUserId(), "FORGOT_PWD",
                null, "OTP sent", email, user.getUserType(), user.getBankId(), "Forgot password OTP dispatched");

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "If this email is registered, an OTP will be sent.", null));
    }

    // ── Forgot password: verify OTP ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> forgotVerifyOtp(String email, String otpCode) {
        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid request.", null));
        }
        ReconUser user = userOpt.get();
        Optional<OtpManager> otpOpt = otpManagerRepository
                .findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(user.getEmail(), "N");
        if (!otpOpt.isPresent() || !otpOpt.get().getOtpCode().equals(otpCode.trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid or expired OTP.", null));
        }
        if (otpOpt.get().getExpiryTime().isBefore(LocalDateTime.now())) {
            otpOpt.get().setIsUsed("Y");
            otpManagerRepository.save(otpOpt.get());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "OTP expired. Please request again.", null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "OTP verified.", null));
    }

    // ── Forgot password: reset password ─────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> forgotResetPassword(String email, String otpCode,
                                                                    String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Passwords do not match.", null));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Password must be at least 8 characters.", null));
        }

        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "Invalid request.", null));
        }
        ReconUser user = userOpt.get();

        Optional<OtpManager> otpOpt = otpManagerRepository
                .findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(user.getEmail(), "N");
        if (!otpOpt.isPresent() || !otpOpt.get().getOtpCode().equals(otpCode.trim())
                || otpOpt.get().getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList("FAILURE", "OTP invalid or expired.", null));
        }

        // Check password history
        for (ReconPasswordManager pm : reconPasswordManagerRepository.findByReconUser(user)) {
            if (passwordEncoder.matches(newPassword, pm.getUserPassword())) {
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList("FAILURE", "Password was used before. Choose a different one.", null));
            }
        }

        // Mark OTP used
        otpOpt.get().setIsUsed("Y");
        otpManagerRepository.save(otpOpt.get());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordSet(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(email);
        reconUserRepository.save(user);

        ReconPasswordManager pm = new ReconPasswordManager();
        pm.setReconUser(user);
        pm.setUserPassword(user.getPasswordHash());
        pm.setCreatedAt(LocalDateTime.now());
        pm.setCreatedBy(email);
        pm.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pm);

        auditLog("RCN_RECON_USER", user.getUserId(), "RESET_PWD",
                null, "password reset via OTP", email, user.getUserType(), user.getBankId(), "Forgot password reset successful");

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Password reset successfully. You can now login.", null));
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private Optional<ReconUser> findUser(String identifier) {
        String id = identifier.trim().toLowerCase();
        Optional<ReconUser> opt = reconUserRepository.findByUsername(id);
        if (!opt.isPresent()) opt = reconUserRepository.findByEmail(id);
        return opt;
    }

    private ResponseEntity<RestWithStatusList> guardStatus(ReconUser user) {
        if ("BLOCKED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE",
                            "Account is BLOCKED. Reason: " + (user.getBlockReason() != null ? user.getBlockReason() : "Contact administrator."), null));
        }
        if ("INACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Account is INACTIVE. Contact administrator.", null));
        }
        if ("ACTIVE_PENDING".equals(user.getStatus()) && (user.getPasswordSet() == null || user.getPasswordSet() == 0)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Please complete first-time setup (set password).", null));
        }
        return null;
    }

    private RestWithStatusList buildJwtResponse(ReconUser user, HttpServletResponse response) {
        UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getUsername());
        String accessToken  = jwtHelper.generateToken(userDetails);
        String refreshToken = jwtHelper.generateTokenForRefresh(user.getUsername());

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(8 * 60 * 60);
        response.addCookie(refreshCookie);

        AuthResponse auth = new AuthResponse(
                accessToken, refreshToken,
                user.getUserId(), user.getRoleId(), user.getUserType(),
                user.getFullName(), user.getUsername());
        return new RestWithStatusList("SUCCESS", "Login successful.", Collections.singletonList(auth));
    }

    private String generateOtp(int digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    private void auditLog(String table, Long recordId, String operation,
                           String oldVal, String newVal, String actor,
                           String actorType, Long bankId, String label) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(table);
            log.setRecordId(recordId);
            log.setOperation(operation);
            log.setOldValue(oldVal);
            log.setNewValue(newVal);
            log.setActorUsername(actor);
            log.setActorType(actorType);
            log.setBankId(bankId);
            log.setActionLabel(label);
            log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("AuditLog save failed: {}", e.getMessage());
        }
    }
}
