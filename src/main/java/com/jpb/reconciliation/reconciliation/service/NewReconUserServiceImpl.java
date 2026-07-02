package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class NewReconUserServiceImpl implements NewReconUserService {

    private static final Logger logger = LoggerFactory.getLogger(NewReconUserServiceImpl.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconPasswordManagerRepository reconPasswordManagerRepository;

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        } else {
            user.setApprovedYn("N");
            if (user.getStatus() == null) {
                user.setStatus("ACTIVE_PENDING");
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
        existing.setStatus("ACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(approvedBy);
        reconUserRepository.save(existing);
        logger.info("ReconUser approved: {} by {}", userId, approvedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User approved successfully.", null));
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
        boolean exists = reconUserRepository.existsByEmail(email);
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



