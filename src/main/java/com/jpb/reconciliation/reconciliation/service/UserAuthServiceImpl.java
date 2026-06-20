package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthServiceImpl.class);

    private final AddUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final OtpService        otpService;
    private final JwtHelper         jwtHelper;

    // ── Helper: find user by username, then verify bank code ──
    private Optional<AddUser> findUser(String bankCode, String username) {
        String trimmed = username != null ? username.trim() : "";
        Optional<AddUser> opt = userRepository.findByUsername(trimmed);
        // Case-insensitive fallback: email links may preserve original-case name instead of stored lowercase username
        if (!opt.isPresent()) opt = userRepository.findByUsernameIgnoreCase(trimmed);
        if (!opt.isPresent()) return Optional.empty();
        AddUser u = opt.get();
        String bk = bankCode != null ? bankCode.trim() : "";
        boolean match = bk.equals(u.getBankCode() != null ? u.getBankCode() : "")
                     || bk.equals(u.getBranchCode() != null ? u.getBranchCode() : "");
        return match ? opt : Optional.empty();
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 2) return email;
        return email.charAt(0) + "*****" + email.charAt(at - 1) + email.substring(at);
    }

    private ResponseEntity<RestWithStatusList> fail(String msg) {
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", msg, null), HttpStatus.OK);
    }

    // ── STEP 1: verify default credentials ──────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> verifyCredentials(String bankCode, String username, String defaultPassword) {
        if (username == null || username.trim().isEmpty())
            return fail("Username is required.");
        if (defaultPassword == null || defaultPassword.trim().isEmpty())
            return fail("Default password is required.");

        Optional<AddUser> opt = findUser(bankCode, username);
        if (!opt.isPresent()) {
            logger.warn("userAuth.verifyCredentials — not found: bankCode={} username={}", bankCode, username);
            return fail("Invalid credentials. Please check your welcome email.");
        }
        AddUser user = opt.get();

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            logger.info("userAuth.verifyCredentials → ALREADY_VERIFIED for username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED", "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        if (!passwordEncoder.matches(defaultPassword.trim(), user.getDefaultPassword())) {
            logger.warn("userAuth.verifyCredentials — password mismatch for username={}", username);
            return fail("Invalid credentials. Please check your welcome email.");
        }

        logger.info("userAuth.verifyCredentials → SUCCESS for username={}", username);
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Credentials verified. Please set your new password.", null),
                HttpStatus.OK);
    }

    // ── STEP 2: set new password ─────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> setPassword(String bankCode, String username, String newPassword) {
        if (username == null || username.trim().isEmpty())
            return fail("Username is required.");
        if (newPassword == null || newPassword.trim().length() < 8)
            return fail("Password must be at least 8 characters.");

        Optional<AddUser> opt = findUser(bankCode, username);
        if (!opt.isPresent()) return fail("User not found.");

        AddUser user = opt.get();

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            return new ResponseEntity<>(
                    new RestWithStatusList("ALREADY_VERIFIED", "Password already set. Please login directly.", null),
                    HttpStatus.OK);
        }

        user.setDefaultPassword(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordSet(1);
        if (user.getStatus() == AddUser.UserStatus.REQUEST) {
            user.setStatus(AddUser.UserStatus.VERIFIED);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("userAuth.setPassword → SUCCESS for username={}", username);
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Password set successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }

    // ── STEP 3: login — send OTP ─────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> login(String bankCode, String username, String password) {
        if (username == null || username.trim().isEmpty())
            return fail("Username is required.");

        Optional<AddUser> opt = findUser(bankCode, username);
        if (!opt.isPresent()) {
            logger.warn("userAuth.login — not found: bankCode={} username={}", bankCode, username);
            return fail("Invalid credentials.");
        }
        AddUser user = opt.get();

        if (user.getPasswordSet() == null || user.getPasswordSet() != 1) {
            return fail("Account setup incomplete. Please set your password first.");
        }

        AddUser.UserStatus userStatus = user.getStatus();

        // Hard denials — no OTP
        if (userStatus == AddUser.UserStatus.BLOCK || userStatus == AddUser.UserStatus.RETIRED) {
            logger.warn("userAuth.login — blocked/retired: username={}", username);
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCKED",
                            "Your account has been permanently blocked. Please contact your administrator.", null),
                    HttpStatus.OK);
        }
        if (userStatus == AddUser.UserStatus.INACTIVE) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE",
                            "Your account is currently inactive. Please contact your administrator.", null),
                    HttpStatus.OK);
        }

        // Password match
        if (!passwordEncoder.matches(password != null ? password.trim() : "", user.getDefaultPassword())) {
            logger.warn("userAuth.login — password mismatch for username={}", username);
            return fail("Invalid password. Please try again.");
        }

        // Send OTP
        String email = user.getEmail();
        try {
            otpService.generateAndSendOtp(email);
            logger.info("userAuth.login — OTP sent to: {}", maskEmail(email));
        } catch (Exception e) {
            logger.error("userAuth.login — OTP send failed for {}: {}", maskEmail(email), e.getMessage());
            return fail("Failed to send OTP. Please try again.");
        }

        List<Object> data = new ArrayList<>();
        data.add(email);            // index 0 — actual email (used by frontend for OTP verification call)
        data.add(maskEmail(email)); // index 1 — masked email for display

        if (userStatus == AddUser.UserStatus.INACTIVE_PENDING) {
            return new ResponseEntity<>(
                    new RestWithStatusList("INACTIVE_PENDING",
                            "Your account is scheduled for inactivation. Please contact your administrator if this was not intended.",
                            data),
                    HttpStatus.OK);
        }
        if (userStatus == AddUser.UserStatus.BLOCK_PENDING) {
            return new ResponseEntity<>(
                    new RestWithStatusList("BLOCK_PENDING",
                            "Your account has been scheduled for permanent block. Please contact your administrator immediately.",
                            data),
                    HttpStatus.OK);
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "OTP sent successfully.", data),
                HttpStatus.OK);
    }

    // ── STEP 4: verify OTP → mark ACTIVE ────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> verifyOtp(String email, String otp) {
        if (email == null || email.trim().isEmpty()) return fail("Email is required.");
        if (otp == null || otp.trim().isEmpty())     return fail("OTP is required.");

        OtpService.OtpVerifyResult result = otpService.verifyOtp(email.trim(), otp.trim());

        if (result == OtpService.OtpVerifyResult.SUCCESS) {
            Optional<AddUser> opt = userRepository.findByEmail(email.trim());
            if (opt.isPresent()) {
                AddUser user = opt.get();
                if (user.getStatus() == AddUser.UserStatus.REQUEST
                        || user.getStatus() == AddUser.UserStatus.VERIFIED) {
                    user.setStatus(AddUser.UserStatus.ACTIVE);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                }
            }
            List<Object> data = new ArrayList<>();
            if (opt.isPresent()) {
                AddUser user = opt.get();
                data.add(user.getUsername());
                data.add(user.getBankCode() != null ? user.getBankCode() : user.getBranchCode());
                // Generate JWT so frontend can redirect to dashboard (mirrors admin OTP flow)
                UserDetails userDetails = User.builder()
                        .username(user.getUsername())
                        .password("")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();
                data.add(jwtHelper.generateToken(userDetails));
                data.add(jwtHelper.generateTokenForRefresh(user.getUsername()));
            }
            logger.info("userAuth.verifyOtp → SUCCESS for email={}", maskEmail(email));
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "OTP verified. Account activated.", data),
                    HttpStatus.OK);
        }

        String errMsg = result == OtpService.OtpVerifyResult.EXPIRED         ? "OTP has expired. Please request a new one."
                      : result == OtpService.OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED ? "Too many attempts. Please request a new OTP."
                      : result == OtpService.OtpVerifyResult.NOT_FOUND        ? "No OTP found. Please request a new one."
                      : "Invalid OTP. Please try again.";
        return fail(errMsg);
    }

    // ── CHECK STATUS: NEW_USER vs OLD_USER ───────────────────────────────────────
    // REQUEST → account not yet verified → NEW_USER (show verification flow)
    // VERIFIED / ACTIVE / any other status → password already set → OLD_USER (go to login)
    @Override
    public ResponseEntity<RestWithStatusList> checkStatus(String bankCode, String username) {
        if (username == null || username.trim().isEmpty()) return fail("Username is required.");
        Optional<AddUser> opt = findUser(bankCode, username);
        if (!opt.isPresent()) return fail("User not found.");
        AddUser user = opt.get();
        String status = (user.getStatus() == AddUser.UserStatus.REQUEST) ? "NEW_USER" : "OLD_USER";
        return new ResponseEntity<>(new RestWithStatusList(status, status, null), HttpStatus.OK);
    }

    // ── GET ACCOUNT STATUS: returns ACTIVE/INACTIVE/BLOCKED/etc for polling ─────
    @Override
    public ResponseEntity<RestWithStatusList> getAccountStatus(String email) {
        if (email == null || email.trim().isEmpty()) return fail("Email is required.");
        Optional<AddUser> opt = userRepository.findByEmail(email.trim());
        if (!opt.isPresent()) return fail("User not found.");
        String statusStr = opt.get().getStatus() != null ? opt.get().getStatus().name() : "UNKNOWN";
        return new ResponseEntity<>(new RestWithStatusList(statusStr, statusStr, null), HttpStatus.OK);
    }

    // ── FORGOT PASSWORD: send OTP ────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> forgotPassword(String email) {
        if (email == null || email.trim().isEmpty()) return fail("Email is required.");
        Optional<AddUser> opt = userRepository.findByEmail(email.trim());
        if (!opt.isPresent()) return fail("No account found with this email address.");

        try {
            otpService.generateAndSendOtp(email.trim());
            List<Object> data = new ArrayList<>();
            data.add(maskEmail(email.trim()));
            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "OTP sent to your registered email.", data),
                    HttpStatus.OK);
        } catch (Exception e) {
            logger.error("userAuth.forgotPassword — OTP send failed for {}: {}", maskEmail(email), e.getMessage());
            return fail("Failed to send OTP. Please try again.");
        }
    }

    // ── RESET PASSWORD ───────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> resetPassword(String email, String otp, String newPassword, String confirmNewPassword) {
        if (email == null || email.trim().isEmpty())         return fail("Email is required.");
        if (otp == null || otp.trim().isEmpty())             return fail("OTP is required.");
        if (newPassword == null || newPassword.length() < 8) return fail("Password must be at least 8 characters.");
        if (!newPassword.equals(confirmNewPassword))         return fail("Passwords do not match.");

        OtpService.OtpVerifyResult result = otpService.verifyOtp(email.trim(), otp.trim());
        if (result != OtpService.OtpVerifyResult.SUCCESS) {
            String errMsg = result == OtpService.OtpVerifyResult.EXPIRED ? "OTP has expired."
                          : result == OtpService.OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED ? "Too many attempts. Please request a new OTP."
                          : "Invalid OTP.";
            return fail(errMsg);
        }

        Optional<AddUser> opt = userRepository.findByEmail(email.trim());
        if (!opt.isPresent()) return fail("User not found.");

        AddUser user = opt.get();
        user.setDefaultPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSet(1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("userAuth.resetPassword → SUCCESS for email={}", maskEmail(email));
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Password reset successfully. Please login.", new ArrayList<>()),
                HttpStatus.OK);
    }
}
