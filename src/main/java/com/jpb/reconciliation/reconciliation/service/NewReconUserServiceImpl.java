package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class NewReconUserServiceImpl implements NewReconUserService {

    private static final Logger logger = LoggerFactory.getLogger(NewReconUserServiceImpl.class);
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconPasswordManagerRepository reconPasswordManagerRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createUser(ReconUser user, String createdBy) {
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Full name is required.", null));
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Username is required.", null));
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Email is required.", null));
        }
        if (user.getUserType() == null || user.getUserType().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "User type is required.", null));
        }
        if (reconUserRepository.existsByUsername(user.getUsername().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Username already exists.", null));
        }
        if (reconUserRepository.existsByEmail(user.getEmail().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Email already registered.", null));
        }
        user.setUsername(user.getUsername().trim().toLowerCase());
        user.setEmail(user.getEmail().trim().toLowerCase());

        // Role dropdown in AddUser.jsx sends the role's NAME (e.g. "MAKER"), not its ID —
        // "DEFAULT" means no specific role. Resolve to ROLE_ID here (was silently dropped
        // before since ReconUser has no matching "role" field).
        if (user.getRoleId() == null && user.getRoleName() != null
                && !user.getRoleName().trim().isEmpty() && !"DEFAULT".equalsIgnoreCase(user.getRoleName().trim())) {
            reconRoleMasterRepository.findByRoleName(user.getRoleName().trim())
                    .ifPresent(r -> user.setRoleId(r.getRoleId()));
        }

        // Links this user to whoever created them, for hierarchy display (My Organization /
        // OrgHierarchy) — mirrors ReconBankMasterServiceImpl.buildAdminUser's parentUserId.
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(createdBy);
        actorOpt.map(ReconUser::getUserId).ifPresent(user::setParentUserId);
        boolean actorIsAdmin = actorOpt.isPresent() && UserConstants.isAdminUserType(actorOpt.get().getUserType());

        String defaultPwd = null;
        if (user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash().trim()));
            user.setPasswordSet(1);
        } else {
            user.setPasswordSet(0);
        }

        if ("KAL_ADMIN".equals(user.getUserType())) {
            user.setApprovedYn("Y");
            user.setStatus("ACTIVE");
            if (user.getRoleId() == null) {
                Optional<ReconRoleMaster> kalAdminRole = reconRoleMasterRepository.findByRoleCode("KAL_ADMIN");
                if (kalAdminRole.isPresent()) {
                    user.setRoleId(kalAdminRole.get().getRoleId());
                }
            }
        } else if (actorIsAdmin) {
            // Admin creates a user directly — no maker-checker approval needed. Goes straight
            // to REQUEST (awaiting the user's own email verification / password setup), and
            // gets emailed immediately.
            user.setApprovedYn("Y");
            user.setApprovedBy(createdBy);
            user.setStatus("REQUEST");
            if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
                // passwordSet stays 0 — it's a DEFAULT/temporary password, not one the user
                // chose. 1 would make verify-email misreport them as OLD_USER and skip the
                // mandatory verify-credentials -> set-password flow (same bug fixed here also
                // existed in approveUser below). Matches buildAdminUser's own convention.
                defaultPwd = generatePassword();
                user.setPasswordHash(passwordEncoder.encode(defaultPwd));
                user.setPasswordSet(0);
            }
        } else {
            // A Maker created this user — goes to the Checker queue first. Password/email
            // are deferred to approveUser(), once a Checker actually approves it.
            // PENDING_APPROVAL — not ACTIVE_PENDING, which is reserved exclusively for the
            // Inactive->reactivating scheduling flow (UserStatusServiceImpl/StatusSchedulerService).
            user.setApprovedYn("N");
            if (user.getStatus() == null) {
                user.setStatus("PENDING_APPROVAL");
            }
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(createdBy);
        ReconUser saved = reconUserRepository.saveAndFlush(user);

        ReconPasswordManager pwd = new ReconPasswordManager();
        pwd.setReconUser(saved);
        pwd.setUserPassword(saved.getPasswordHash());
        pwd.setCreatedAt(LocalDateTime.now());
        pwd.setCreatedBy(createdBy);
        pwd.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pwd);

        if (defaultPwd != null) {
            sendUserWelcomeEmail(saved, defaultPwd);
        }

        logger.info("ReconUser created: {} by {}", saved.getUsername(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "User created successfully.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllUsers() {
        List<ReconUser> users = reconUserRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched.", users));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUserById(Long userId) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUserByUsername(String username) {
        Optional<ReconUser> opt = reconUserRepository.findByUsername(username);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found: " + username, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUsersByBankId(Long bankId) {
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched by bank.", users));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUsersByBankCode(String bankCode) {
        Optional<com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster> bankOpt =
                reconBankMasterRepository.findByBankCode(bankCode);
        if (!bankOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No data", Collections.emptyList()));
        }
        List<ReconUser> users = reconUserRepository.findByBankId(bankOpt.get().getBankId());
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched by bank.", users));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUsersByRoleId(Long roleId) {
        List<ReconUser> users = reconUserRepository.findByRoleId(roleId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched by role.", users));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUsersByStatus(String status) {
        List<ReconUser> users = reconUserRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched by status.", users));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUsersByType(String userType) {
        List<ReconUser> users = reconUserRepository.findByUserType(userType);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched by type.", users));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateUser(Long userId, ReconUser user, String updatedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        if (user.getFullName() != null) existing.setFullName(user.getFullName());
        if (user.getMobileNumber() != null) existing.setMobileNumber(user.getMobileNumber());
        if (user.getDesignation() != null) existing.setDesignation(user.getDesignation());
        if (user.getDepartment() != null) existing.setDepartment(user.getDepartment());
        if (user.getContactRank() != null) existing.setContactRank(user.getContactRank());
        if (user.getRoleId() != null) existing.setRoleId(user.getRoleId());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser updated: {} by {}", userId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long userId, String status, String updatedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser status updated to {} for ID: {} by {}", status, userId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveUser(Long userId, String approvedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        existing.setApprovedYn("Y");
        existing.setApprovedBy(approvedBy);
        // A Checker approving a Maker-created user is the moment it becomes usable — same
        // REQUEST status (awaiting the user's own email verification) an Admin-direct-created
        // user gets immediately. The password is generated here (not at creation) since this
        // is the first point the user is actually meant to log in.
        existing.setStatus("REQUEST");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(approvedBy);
        if (existing.getPasswordSet() == null || existing.getPasswordSet() == 0) {
            String defaultPwd = generatePassword();
            existing.setPasswordHash(passwordEncoder.encode(defaultPwd));
            // Stays 0 — a temporary/default password, not one the user chose. Same fix as
            // createUser() above: 1 would make verify-email misreport OLD_USER and skip the
            // mandatory verify-credentials -> set-password flow.
            existing.setPasswordSet(0);
            ReconUser saved = reconUserRepository.save(existing);

            ReconPasswordManager pwdHist = new ReconPasswordManager();
            pwdHist.setReconUser(saved);
            pwdHist.setUserPassword(saved.getPasswordHash());
            pwdHist.setCreatedAt(LocalDateTime.now());
            pwdHist.setCreatedBy(approvedBy);
            pwdHist.setExpirationDate(LocalDateTime.now().plusDays(90));
            reconPasswordManagerRepository.save(pwdHist);

            sendUserWelcomeEmail(saved, defaultPwd);
        } else {
            reconUserRepository.save(existing);
        }
        logger.info("ReconUser approved: {} by {}", userId, approvedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User approved successfully.", null));
    }

    private String generatePassword() {
        int digits = 1000 + RNG.nextInt(9000);
        return "Recon@" + digits;
    }

    private void sendUserWelcomeEmail(ReconUser user, String defaultPwd) {
        try {
            if (user.getBankId() == null) return;
            Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findPrimaryById(user.getBankId());
            if (!bankOpt.isPresent()) return;
            ReconBankMaster bank = bankOpt.get();
            boolean isBranch = "BRANCH".equals(bank.getBankLevel());
            String verifyLink = frontendUrl + "/user-verify"
                    + "?bankCode=" + bank.getBankCode()
                    + (isBranch ? "&branchCode=" + bank.getBankCode() : "")
                    + "&username=" + user.getUsername();
            emailService.sendUserWelcome(user.getEmail(), user.getFullName(),
                    bank.getBankCode(), isBranch ? "Branch Code" : "Bank Code",
                    user.getUsername(), defaultPwd, verifyLink);
        } catch (Exception e) {
            logger.error("Failed to send welcome email for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteUser(Long userId) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkUsernameExists(String username) {
        boolean exists = reconUserRepository.existsByUsername(username);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        String emailLc = email == null ? null : email.trim().toLowerCase();
        boolean exists = emailLc != null && !emailLc.isEmpty()
                && (reconUserRepository.existsByEmail(emailLc) || reconBankMasterRepository.existsByEmail(emailLc));
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> blockUser(Long userId, String reason, String updatedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "User is already BLOCKED.", null));
        }
        existing.setPreBlockStatus(existing.getStatus());
        existing.setStatus("BLOCKED");
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser BLOCKED: userId={} preBlockStatus={} by {}", userId, existing.getPreBlockStatus(), updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User blocked successfully.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> unblockUser(Long userId, String updatedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        if (!"BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "User is not currently BLOCKED.", null));
        }
        String restoreStatus = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : "ACTIVE";
        existing.setStatus(restoreStatus);
        existing.setPreBlockStatus(null);
        existing.setBlockReason(null);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser UNBLOCKED: userId={} restored to {} by {}", userId, restoreStatus, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User unblocked. Status restored to: " + restoreStatus, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, java.time.LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconUser existing = opt.get();
        existing.setInactivateScheduledAt(scheduledAt);
        existing.setInactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser INACTIVATE scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Inactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, java.time.LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconUser existing = opt.get();
        existing.setReactivateScheduledAt(scheduledAt);
        existing.setReactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser REACTIVATE scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Reactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, java.time.LocalDateTime scheduledAt, String scheduledBy, String reason) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconUser existing = opt.get();
        existing.setBlockScheduledAt(scheduledAt);
        existing.setBlockScheduledBy(scheduledBy);
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser BLOCK scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Block scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> cancelSchedule(Long userId, String scheduleType, String updatedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "User not found with ID: " + userId, null));
        }
        ReconUser existing = opt.get();
        if (scheduleType == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "scheduleType is required (INACTIVATE / REACTIVATE / BLOCK).", null));
        }
        switch (scheduleType.toUpperCase()) {
            case "INACTIVATE":
                existing.setInactivateScheduledAt(null);
                existing.setInactivateScheduledBy(null);
                break;
            case "REACTIVATE":
                existing.setReactivateScheduledAt(null);
                existing.setReactivateScheduledBy(null);
                break;
            case "BLOCK":
                existing.setBlockScheduledAt(null);
                existing.setBlockScheduledBy(null);
                break;
            default:
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList("FAILURE", "Invalid scheduleType. Use INACTIVATE, REACTIVATE, or BLOCK.", null));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser {} schedule cancelled for userId={} by {}", scheduleType, userId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", scheduleType + " schedule cancelled.", null));
    }
}



