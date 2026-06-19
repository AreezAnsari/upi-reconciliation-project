package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminReplacementRequest;
import com.jpb.reconciliation.reconciliation.dto.AdminReplacementResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@Service
public class AdminReplacementServiceImpl implements AdminReplacementService {

    private static final Logger logger = LoggerFactory.getLogger(AdminReplacementServiceImpl.class);

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Autowired private MainAdminRepository mainAdminRepository;
    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private AddUserRepository addUserRepository;
    @Autowired private AdminReplacementRepository replacementRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────
    // VALIDATE EMAIL — lightweight check, no DB write
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> validateReplacementEmail(AdminReplacementRequest req) {
        String type = req.getEntityType();
        String email = req.getNewEmail();
        if (email == null || email.trim().isEmpty()) return fail("newEmail is required");
        if (type == null || type.trim().isEmpty()) return fail("entityType is required");

        if ("MAIN_ADMIN".equalsIgnoreCase(type)) {
            if (mainAdminRepository.existsByEmail(email.trim())) {
                Optional<MainAdmin> existingOpt = mainAdminRepository.findFirstByEmail(email.trim());
                boolean isFormerReplacement = existingOpt.isPresent() &&
                        replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                                existingOpt.get().getId(), "MAIN_ADMIN", "RESTORED");
                if (!isFormerReplacement) {
                    return fail("Email '" + email.trim() + "' is already registered as a bank admin.");
                }
            }
        } else if ("BRANCH_ADMIN".equalsIgnoreCase(type)) {
            if (branchAdminRepository.existsByEmail(email.trim())) {
                Optional<BranchAdmin> existingOpt = branchAdminRepository.findFirstByEmail(email.trim());
                boolean isFormerReplacement = existingOpt.isPresent() &&
                        replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                                existingOpt.get().getId(), "BRANCH_ADMIN", "RESTORED");
                if (!isFormerReplacement) {
                    return fail("Email '" + email.trim() + "' is already registered as a branch admin.");
                }
            }
        } else if ("USER".equalsIgnoreCase(type)) {
            if (addUserRepository.existsByEmail(email.trim())) {
                Optional<AddUser> existingOpt = addUserRepository.findByEmail(email.trim());
                boolean isFormerReplacement = existingOpt.isPresent() &&
                        replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                                existingOpt.get().getId(), "USER", "RESTORED");
                if (!isFormerReplacement) {
                    return fail("Email '" + email.trim() + "' already exists.");
                }
            }
        } else {
            return fail("Unknown entityType: " + type);
        }
        return ok("Email is available.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCHEDULE PENDING — stores intent, no admin created yet (Bugs 3 & 5)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> schedulePendingReplacement(AdminReplacementRequest request, String scheduledBy) {
        String type = request.getEntityType();
        if (type == null || type.trim().isEmpty()) return fail("entityType is required");
        switch (type.toUpperCase()) {
            case "MAIN_ADMIN":   return schedulePendingMainAdmin(request, scheduledBy);
            case "BRANCH_ADMIN": return schedulePendingBranchAdmin(request, scheduledBy);
            case "USER":         return schedulePendingUser(request, scheduledBy);
            default:             return fail("Unknown entityType: " + type);
        }
    }

    private ResponseEntity<RestWithStatusList> schedulePendingMainAdmin(AdminReplacementRequest req, String scheduledBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) return fail("fullName is required");

        Optional<MainAdmin> originalOpt = mainAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original admin not found with id: " + req.getOriginalEntityId());
        MainAdmin original = originalOpt.get();

        if (mainAdminRepository.existsByEmail(req.getNewEmail().trim())) {
            Optional<MainAdmin> existingOpt = mainAdminRepository.findFirstByEmail(req.getNewEmail().trim());
            boolean isFormerReplacement = existingOpt.isPresent() &&
                    replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                            existingOpt.get().getId(), "MAIN_ADMIN", "RESTORED");
            if (!isFormerReplacement) {
                return fail("Email '" + req.getNewEmail().trim() + "' is already registered as a bank admin.");
            }
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "MAIN_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this admin.");
        }

        // Cancel any leftover PENDING record (e.g. from a previous undo) before creating a fresh one
        Optional<AdminReplacement> existingPending = replacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "MAIN_ADMIN", "PENDING");
        if (existingPending.isPresent()) {
            AdminReplacement old = existingPending.get();
            old.setStatus("CANCELLED");
            replacementRepository.save(old);
        }

        AdminReplacement r = buildPendingRecord("MAIN_ADMIN", original.getBankCode(), null,
                original.getId(), req, scheduledBy);
        replacementRepository.save(r);
        logger.info("Pending replacement scheduled for MainAdmin id={} bank={}", original.getId(), original.getBankCode());
        return ok("Replacement scheduled. Will take effect after inactivation is confirmed.");
    }

    private ResponseEntity<RestWithStatusList> schedulePendingBranchAdmin(AdminReplacementRequest req, String scheduledBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) return fail("fullName is required");

        Optional<BranchAdmin> originalOpt = branchAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original branch admin not found with id: " + req.getOriginalEntityId());
        BranchAdmin original = originalOpt.get();

        if (branchAdminRepository.existsByEmail(req.getNewEmail().trim())) {
            Optional<BranchAdmin> existingOpt = branchAdminRepository.findFirstByEmail(req.getNewEmail().trim());
            boolean isFormerReplacement = existingOpt.isPresent() &&
                    replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                            existingOpt.get().getId(), "BRANCH_ADMIN", "RESTORED");
            if (!isFormerReplacement) {
                return fail("Email '" + req.getNewEmail().trim() + "' is already registered as a branch admin.");
            }
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "BRANCH_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this branch admin.");
        }

        // Cancel any leftover PENDING record (e.g. from a previous undo) before creating a fresh one
        Optional<AdminReplacement> existingPendingBranch = replacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "BRANCH_ADMIN", "PENDING");
        if (existingPendingBranch.isPresent()) {
            AdminReplacement old = existingPendingBranch.get();
            old.setStatus("CANCELLED");
            replacementRepository.save(old);
        }

        AdminReplacement r = buildPendingRecord("BRANCH_ADMIN", null, original.getBranchCode(),
                original.getId(), req, scheduledBy);
        replacementRepository.save(r);
        logger.info("Pending replacement scheduled for BranchAdmin id={} branch={}", original.getId(), original.getBranchCode());
        return ok("Replacement scheduled. Will take effect after inactivation is confirmed.");
    }

    private ResponseEntity<RestWithStatusList> schedulePendingUser(AdminReplacementRequest req, String scheduledBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");

        Optional<AddUser> originalOpt = addUserRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original user not found with id: " + req.getOriginalEntityId());
        AddUser original = originalOpt.get();

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "USER", "ACTIVE")) {
            return fail("An active replacement already exists for this user.");
        }

        // Cancel any leftover PENDING record (e.g. from a previous undo) before creating a fresh one
        Optional<AdminReplacement> existingPendingUser = replacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "USER", "PENDING");
        if (existingPendingUser.isPresent()) {
            AdminReplacement old = existingPendingUser.get();
            old.setStatus("CANCELLED");
            replacementRepository.save(old);
        }

        if (addUserRepository.existsByEmail(req.getNewEmail().trim())) {
            Optional<AddUser> existingOpt = addUserRepository.findByEmail(req.getNewEmail().trim());
            boolean isFormerReplacement = existingOpt.isPresent() &&
                    replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                            existingOpt.get().getId(), "USER", "RESTORED");
            if (!isFormerReplacement) {
                return fail("Email '" + req.getNewEmail().trim() + "' already exists.");
            }
        }

        AdminReplacement r = buildPendingRecord("USER", original.getBankCode(), original.getBranchCode(),
                original.getId(), req, scheduledBy);
        replacementRepository.save(r);
        logger.info("Pending replacement scheduled for User id={}", original.getId());
        return ok("Replacement scheduled. Will take effect after inactivation is confirmed.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // FINALIZE — called by scheduler after INACTIVE_PENDING → INACTIVE
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void finalizeMainAdminPending(AdminReplacement pending) {
        Optional<MainAdmin> originalOpt = mainAdminRepository.findById(pending.getOriginalEntityId());
        if (!originalOpt.isPresent()) {
            logger.warn("finalizeMainAdminPending: original admin {} not found — cancelling", pending.getOriginalEntityId());
            pending.setStatus("CANCELLED");
            replacementRepository.save(pending);
            return;
        }
        MainAdmin original = originalOpt.get();

        String repFullName = pending.getPendingFullName();
        String tempPassword = generateTempPassword();

        // Check if a RESTORED former replacement with this email already exists — reuse instead of creating new
        Optional<MainAdmin> existingOpt = mainAdminRepository.findFirstByEmail(pending.getPendingEmail());
        boolean isFormerReplacement = existingOpt.isPresent() &&
                replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                        existingOpt.get().getId(), "MAIN_ADMIN", "RESTORED");

        if (!isFormerReplacement && mainAdminRepository.existsByBankCodeAndEmail(original.getBankCode(), pending.getPendingEmail())) {
            logger.warn("finalizeMainAdminPending: email {} already taken — cancelling", pending.getPendingEmail());
            pending.setStatus("CANCELLED");
            replacementRepository.save(pending);
            return;
        }

        MainAdmin replacement;
        String generatedUsername;
        if (isFormerReplacement) {
            // Reuse existing record — reset to onboarding state
            replacement = existingOpt.get();
            generatedUsername = replacement.getUsername();
            replacement.setPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUESTED");
            replacement.setUpdatedAt(LocalDateTime.now());
            replacement.setUpdatedBy(pending.getReplacedBy());
        } else {
            generatedUsername = generateMainAdminId(repFullName, original.getBankCode());
            replacement = new MainAdmin();
            replacement.setBankCode(original.getBankCode());
            replacement.setUsername(generatedUsername);
            replacement.setEmail(pending.getPendingEmail());
            replacement.setPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUESTED");
            replacement.setCreatedBy(pending.getReplacedBy());
            replacement.setCreatedAt(LocalDateTime.now());
            replacement.setUpdatedAt(LocalDateTime.now());
        }
        mainAdminRepository.save(replacement);

        pending.setReplacementEntityId(replacement.getId());
        pending.setStatus("ACTIVE");
        replacementRepository.save(pending);

        String actionedByName = formatUsername(pending.getReplacedBy());
        String verifyLink = frontendUrl + "/verify-email?bankCode=" + original.getBankCode() + "&username=" + generatedUsername;
        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBankCode(),
                actionedByName, pending.getReason(), repFullName, pending.getPendingEmail(), pending.getPendingOrderedBy());
        sendReplacementAdminWelcomeEmail(replacement.getEmail(), repFullName,
                original.getBankCode(), generatedUsername, tempPassword, verifyLink);

        logger.info("Finalized pending MainAdmin replacement: {} → {}", original.getUsername(), generatedUsername);
    }

    @Override
    @Transactional
    public void finalizeBranchAdminPending(AdminReplacement pending) {
        Optional<BranchAdmin> originalOpt = branchAdminRepository.findById(pending.getOriginalEntityId());
        if (!originalOpt.isPresent()) {
            logger.warn("finalizeBranchAdminPending: original branch admin {} not found — cancelling", pending.getOriginalEntityId());
            pending.setStatus("CANCELLED");
            replacementRepository.save(pending);
            return;
        }
        BranchAdmin original = originalOpt.get();

        String repFullName = pending.getPendingFullName();
        String tempPassword = generateTempPassword();

        // Check if a RESTORED former replacement with this email already exists — reuse instead of creating new
        Optional<BranchAdmin> existingBranchOpt = branchAdminRepository.findFirstByEmail(pending.getPendingEmail());
        boolean isFormerReplacementBranch = existingBranchOpt.isPresent() &&
                replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                        existingBranchOpt.get().getId(), "BRANCH_ADMIN", "RESTORED");

        if (!isFormerReplacementBranch && branchAdminRepository.existsByBranchCodeAndEmail(original.getBranchCode(), pending.getPendingEmail())) {
            logger.warn("finalizeBranchAdminPending: email {} already taken — cancelling", pending.getPendingEmail());
            pending.setStatus("CANCELLED");
            replacementRepository.save(pending);
            return;
        }

        BranchAdmin replacement;
        String generatedUsername;
        if (isFormerReplacementBranch) {
            // Reuse existing record — reset to onboarding state
            replacement = existingBranchOpt.get();
            generatedUsername = replacement.getUsername();
            replacement.setPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUESTED");
            replacement.setUpdatedAt(LocalDateTime.now());
            replacement.setUpdatedBy(pending.getReplacedBy());
        } else {
            generatedUsername = generateBranchAdminId(repFullName, original.getBranchCode());
            replacement = new BranchAdmin();
            replacement.setBranchCode(original.getBranchCode());
            replacement.setUsername(generatedUsername);
            replacement.setEmail(pending.getPendingEmail());
            replacement.setPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUESTED");
            replacement.setCreatedBy(pending.getReplacedBy());
            replacement.setCreatedAt(LocalDateTime.now());
            replacement.setUpdatedAt(LocalDateTime.now());
        }
        branchAdminRepository.save(replacement);

        pending.setReplacementEntityId(replacement.getId());
        pending.setStatus("ACTIVE");
        replacementRepository.save(pending);

        String actionedByName = formatUsername(pending.getReplacedBy());
        String verifyLink = frontendUrl + "/branch-verify-email?bankCode=" + original.getBranchCode() + "&username=" + generatedUsername;
        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBranchCode(),
                actionedByName, pending.getReason(), repFullName, pending.getPendingEmail(), pending.getPendingOrderedBy());
        sendReplacementAdminWelcomeEmail(replacement.getEmail(), repFullName,
                original.getBranchCode(), generatedUsername, tempPassword, verifyLink);

        logger.info("Finalized pending BranchAdmin replacement: {} → {}", original.getUsername(), generatedUsername);
    }

    @Override
    @Transactional
    public void finalizeUserPending(AdminReplacement pending) {
        Optional<AddUser> originalOpt = addUserRepository.findById(pending.getOriginalEntityId());
        if (!originalOpt.isPresent()) {
            logger.warn("finalizeUserPending: original user {} not found — cancelling", pending.getOriginalEntityId());
            pending.setStatus("CANCELLED");
            replacementRepository.save(pending);
            return;
        }
        AddUser original = originalOpt.get();

        String newEmail    = pending.getPendingEmail() != null ? pending.getPendingEmail() : "";
        String repFullName = pending.getPendingFullName() != null ? pending.getPendingFullName() : newEmail;
        // Generate username from full name (e.g. "Aadil Ansari" → "aadil.ansari")
        String newUsername = generateUserUsername(repFullName);

        // Check if a RESTORED former replacement with this email already exists — reuse instead of creating new
        Optional<AddUser> existingUserOpt = addUserRepository.findByEmail(newEmail);
        boolean isFormerReplacementUser = existingUserOpt.isPresent() &&
                replacementRepository.existsByReplacementEntityIdAndEntityTypeAndStatus(
                        existingUserOpt.get().getId(), "USER", "RESTORED");

        if (!isFormerReplacementUser) {
            if (addUserRepository.existsByUsername(newUsername)) {
                logger.warn("finalizeUserPending: username {} already taken — cancelling", newUsername);
                pending.setStatus("CANCELLED");
                replacementRepository.save(pending);
                return;
            }
            if (addUserRepository.existsByEmail(newEmail)) {
                logger.warn("finalizeUserPending: email {} already taken — cancelling", newEmail);
                pending.setStatus("CANCELLED");
                replacementRepository.save(pending);
                return;
            }
        }

        AddUser.Role role = original.getRole() != null ? original.getRole() : AddUser.Role.MAKER;
        AddUser.UserType userType = original.getUserType() != null ? original.getUserType() : AddUser.UserType.INTERNAL;
        String tempPassword = generateTempPassword();

        AddUser replacement;
        if (isFormerReplacementUser) {
            // Reuse existing record — reset to onboarding state
            replacement = existingUserOpt.get();
            replacement.setDefaultPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus(AddUser.UserStatus.REQUEST);
            replacement.setUpdatedAt(LocalDateTime.now());
        } else {
            replacement = new AddUser();
            replacement.setFullName(repFullName);
            replacement.setUsername(newUsername);
            replacement.setEmail(newEmail);
            replacement.setRole(role);
            replacement.setUserType(userType);
            replacement.setRoleType(original.getRoleType() != null ? original.getRoleType() : userType.name());
            replacement.setDepartment(original.getDepartment());
            replacement.setDesignation(original.getDesignation());
            replacement.setMobileNumber(pending.getPendingMobile());
            replacement.setBankCode(original.getBankCode());
            replacement.setBranchCode(original.getBranchCode());
            replacement.setDefaultPassword(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus(AddUser.UserStatus.REQUEST);
            replacement.setCreatedBy(pending.getReplacedBy());
        }
        addUserRepository.save(replacement);

        pending.setReplacementEntityId(replacement.getId());
        pending.setStatus("ACTIVE");
        replacementRepository.save(pending);

        String entityCode = original.getBranchCode() != null ? original.getBranchCode() : original.getBankCode();
        String actionedByName = formatUsername(pending.getReplacedBy());
        sendOutgoingEmail(original.getEmail(), original.getFullName(), entityCode,
                actionedByName, pending.getReason(), repFullName, newEmail, pending.getPendingOrderedBy());
        sendIncomingEmail(replacement.getEmail(), repFullName, entityCode, newUsername, tempPassword);

        logger.info("Finalized pending User replacement: {} → {}", original.getUsername(), newUsername);
    }

    // ─────────────────────────────────────────────────────────────────────
    // IMMEDIATE REPLACE — kept for backward compatibility (not used by frontend)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> replace(AdminReplacementRequest request, String replacedBy) {
        String type = request.getEntityType();
        if (type == null || type.trim().isEmpty()) return fail("entityType is required");
        switch (type.toUpperCase()) {
            case "MAIN_ADMIN":    return replaceMainAdmin(request, replacedBy);
            case "BRANCH_ADMIN":  return replaceBranchAdmin(request, replacedBy);
            case "USER":          return replaceUser(request, replacedBy);
            default:              return fail("Unknown entityType: " + type);
        }
    }

    private ResponseEntity<RestWithStatusList> replaceMainAdmin(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");

        Optional<MainAdmin> originalOpt = mainAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original admin not found with id: " + req.getOriginalEntityId());
        MainAdmin original = originalOpt.get();

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "MAIN_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this admin.");
        }
        if (mainAdminRepository.existsByBankCodeAndEmail(original.getBankCode(), req.getNewEmail().trim())) {
            return fail("Email '" + req.getNewEmail().trim() + "' is already registered for bank " + original.getBankCode());
        }

        String repFullName = (req.getFullName() != null && !req.getFullName().trim().isEmpty())
                ? req.getFullName().trim() : req.getNewEmail().trim().split("@")[0];
        String generatedUsername = generateMainAdminId(repFullName, original.getBankCode());
        String tempPassword = generateTempPassword();

        MainAdmin replacement = new MainAdmin();
        replacement.setBankCode(original.getBankCode());
        replacement.setUsername(generatedUsername);
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(0);
        replacement.setStatus("REQUESTED");
        replacement.setCreatedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        replacement.setUpdatedAt(LocalDateTime.now());
        mainAdminRepository.save(replacement);

        AdminReplacement record = buildActiveRecord("MAIN_ADMIN", original.getBankCode(), null,
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        String actionedByName = formatUsername(replacedBy);
        String verifyLink = frontendUrl + "/verify-email?bankCode=" + original.getBankCode() + "&username=" + generatedUsername;
        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBankCode(),
                actionedByName, req.getReason(), repFullName, req.getNewEmail().trim(), req.getOrderedBy());
        sendReplacementAdminWelcomeEmail(replacement.getEmail(), repFullName,
                original.getBankCode(), generatedUsername, tempPassword, verifyLink);

        return success(record, replacement.getId());
    }

    private ResponseEntity<RestWithStatusList> replaceBranchAdmin(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");

        Optional<BranchAdmin> originalOpt = branchAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original branch admin not found with id: " + req.getOriginalEntityId());
        BranchAdmin original = originalOpt.get();

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "BRANCH_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this branch admin.");
        }
        if (branchAdminRepository.existsByBranchCodeAndEmail(original.getBranchCode(), req.getNewEmail().trim())) {
            return fail("Email '" + req.getNewEmail().trim() + "' is already registered for branch " + original.getBranchCode());
        }

        String repFullName = (req.getFullName() != null && !req.getFullName().trim().isEmpty())
                ? req.getFullName().trim() : req.getNewEmail().trim().split("@")[0];
        String generatedUsername = generateBranchAdminId(repFullName, original.getBranchCode());
        String tempPassword = generateTempPassword();

        BranchAdmin replacement = new BranchAdmin();
        replacement.setBranchCode(original.getBranchCode());
        replacement.setUsername(generatedUsername);
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(0);
        replacement.setStatus("REQUESTED");
        replacement.setCreatedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        replacement.setUpdatedAt(LocalDateTime.now());
        branchAdminRepository.save(replacement);

        AdminReplacement record = buildActiveRecord("BRANCH_ADMIN", null, original.getBranchCode(),
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        String actionedByName = formatUsername(replacedBy);
        String verifyLink = frontendUrl + "/branch-verify-email?bankCode=" + original.getBranchCode() + "&username=" + generatedUsername;
        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBranchCode(),
                actionedByName, req.getReason(), repFullName, req.getNewEmail().trim(), req.getOrderedBy());
        sendReplacementAdminWelcomeEmail(replacement.getEmail(), repFullName,
                original.getBranchCode(), generatedUsername, tempPassword, verifyLink);

        return success(record, replacement.getId());
    }

    private ResponseEntity<RestWithStatusList> replaceUser(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");

        Optional<AddUser> originalOpt = addUserRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original user not found with id: " + req.getOriginalEntityId());
        AddUser original = originalOpt.get();
        String userStatusStr = original.getStatus() != null ? original.getStatus().name() : "";
        if (!"INACTIVE".equalsIgnoreCase(userStatusStr) && !"INACTIVE_PENDING".equalsIgnoreCase(userStatusStr)) {
            return fail("Original user must be INACTIVE to be replaced. Current status: " + userStatusStr);
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(original.getId(), "USER", "ACTIVE")) {
            return fail("An active replacement already exists for this user.");
        }
        if (addUserRepository.existsByEmail(req.getNewEmail().trim())) {
            return fail("Email '" + req.getNewEmail().trim() + "' already exists.");
        }

        // Generate username from full name (e.g. "Aadil Ansari" → "aadil.ansari")
        String fullNameForUsername = (req.getFullName() != null && !req.getFullName().trim().isEmpty())
                ? req.getFullName().trim() : req.getNewEmail().trim().split("@")[0];
        String generatedUsername = generateUserUsername(fullNameForUsername);

        String roleStr = (req.getRole() != null && !req.getRole().trim().isEmpty())
                ? req.getRole().trim() : (original.getRole() != null ? original.getRole().name() : "MAKER");
        AddUser.Role role;
        try { role = AddUser.Role.valueOf(roleStr.toUpperCase()); } catch (IllegalArgumentException e) { return fail("Invalid role: " + roleStr); }

        String userTypeStr = (req.getUserType() != null && !req.getUserType().trim().isEmpty())
                ? req.getUserType().trim() : (original.getUserType() != null ? original.getUserType().name() : "INTERNAL");
        AddUser.UserType userType;
        try { userType = AddUser.UserType.valueOf(userTypeStr.toUpperCase()); } catch (IllegalArgumentException e) { return fail("Invalid userType: " + userTypeStr); }

        String tempPassword = generateTempPassword();

        AddUser replacement = new AddUser();
        replacement.setFullName(req.getFullName().trim());
        replacement.setUsername(generatedUsername);
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setRole(role);
        replacement.setUserType(userType);
        replacement.setRoleType(original.getRoleType() != null ? original.getRoleType() : userType.name());
        replacement.setDepartment(req.getDepartment());
        replacement.setDesignation(req.getDesignation());
        replacement.setMobileNumber(req.getMobileNumber());
        replacement.setBankCode(original.getBankCode());
        replacement.setBranchCode(original.getBranchCode());
        replacement.setDefaultPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(0);
        replacement.setStatus(AddUser.UserStatus.REQUEST);
        replacement.setCreatedBy(replacedBy);

        if (AddUser.UserType.EXTERNAL.equals(userType)) {
            replacement.setExternalDepartmentName(req.getExternalDepartmentName());
            replacement.setExternalSupervisorName(req.getExternalSupervisorName());
            replacement.setExternalSupervisorEmail(req.getExternalSupervisorEmail());
            replacement.setExternalSupervisorPhone(req.getExternalSupervisorPhone());
        }
        addUserRepository.save(replacement);

        String entityCode = original.getBranchCode() != null ? original.getBranchCode() : original.getBankCode();
        AdminReplacement record = buildActiveRecord("USER", original.getBankCode(), original.getBranchCode(),
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        String repName = req.getFullName() != null ? req.getFullName().trim() : replacement.getUsername();
        String actionedByName = formatUsername(replacedBy);
        sendOutgoingEmail(original.getEmail(), original.getFullName(), entityCode,
                actionedByName, req.getReason(), repName, req.getNewEmail().trim(), req.getOrderedBy());
        sendIncomingEmail(replacement.getEmail(), replacement.getFullName(), entityCode, replacement.getUsername(), tempPassword);

        return success(record, replacement.getId());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────
    private AdminReplacement buildPendingRecord(String entityType, String bankCode, String branchCode,
                                                Long originalId, AdminReplacementRequest req, String scheduledBy) {
        AdminReplacement r = new AdminReplacement();
        r.setEntityType(entityType);
        r.setBankCode(bankCode);
        r.setBranchCode(branchCode);
        r.setOriginalEntityId(originalId);
        r.setReplacementEntityId(0L); // sentinel: no replacement created yet
        r.setStatus("PENDING");
        r.setReason(req.getReason());
        r.setReplacedBy(scheduledBy);
        r.setReplacedAt(LocalDateTime.now());
        r.setPendingEmail(req.getNewEmail() != null ? req.getNewEmail().trim() : null);
        r.setPendingFullName(req.getFullName() != null ? req.getFullName().trim() : null);
        r.setPendingMobile(req.getMobileNumber());
        r.setPendingOrderedBy(req.getOrderedBy());
        return r;
    }

    private AdminReplacement buildActiveRecord(String entityType, String bankCode, String branchCode,
                                               Long originalId, Long replacementId,
                                               String reason, String replacedBy) {
        AdminReplacement r = new AdminReplacement();
        r.setEntityType(entityType);
        r.setBankCode(bankCode);
        r.setBranchCode(branchCode);
        r.setOriginalEntityId(originalId);
        r.setReplacementEntityId(replacementId);
        r.setReason(reason);
        r.setStatus("ACTIVE");
        r.setReplacedAt(LocalDateTime.now());
        r.setReplacedBy(replacedBy);
        return r;
    }

    // Converts "areez.ansari" → "Areez Ansari" for email display
    private String formatUsername(String username) {
        if (username == null || username.trim().isEmpty()) return "";
        String[] parts = username.split("[.\\-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : username;
    }

    private String generateMainAdminId(String fullName, String bankCode) {
        String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (parts.length == 1) {
            candidates.add(parts[0]);
        } else {
            candidates.add(parts[0] + "." + parts[1]);
            if (parts.length >= 3) candidates.add(parts[0] + "." + parts[1] + "." + parts[2]);
        }
        for (String c : candidates) {
            if (!mainAdminRepository.existsByBankCodeAndUsername(bankCode, c)) return c;
        }
        String base = candidates.get(0);
        for (int i = 2; i <= 99; i++) {
            String candidate = base + i;
            if (!mainAdminRepository.existsByBankCodeAndUsername(bankCode, candidate)) return candidate;
        }
        return base + System.currentTimeMillis();
    }

    private String generateUserUsername(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "user." + System.currentTimeMillis();
        }
        String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (parts.length == 1) {
            candidates.add(parts[0]);
        } else {
            candidates.add(parts[0] + "." + parts[1]);
            if (parts.length >= 3) candidates.add(parts[0] + "." + parts[1] + "." + parts[2]);
        }
        for (String c : candidates) {
            if (!addUserRepository.existsByUsername(c)) return c;
        }
        String base = candidates.get(0);
        for (int i = 2; i <= 99; i++) {
            String candidate = base + i;
            if (!addUserRepository.existsByUsername(candidate)) return candidate;
        }
        return base + System.currentTimeMillis();
    }

    private String generateBranchAdminId(String fullName, String branchCode) {
        String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z\\s]", "").split("\\s+");
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (parts.length == 1) {
            candidates.add(parts[0]);
        } else {
            candidates.add(parts[0] + "." + parts[1]);
            if (parts.length >= 3) candidates.add(parts[0] + "." + parts[1] + "." + parts[2]);
        }
        for (String c : candidates) {
            if (!branchAdminRepository.existsByBranchCodeAndUsername(branchCode, c)) return c;
        }
        String base = candidates.get(0);
        for (int i = 2; i <= 99; i++) {
            String candidate = base + i;
            if (!branchAdminRepository.existsByBranchCodeAndUsername(branchCode, candidate)) return candidate;
        }
        return base + System.currentTimeMillis();
    }

    private String generateTempPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    private void sendOutgoingEmail(String toEmail, String contactName, String entityCode,
                                   String replacedBy, String reason,
                                   String replacementFullName, String replacementEmail, String orderedBy) {
        try {
            emailService.sendReplacementOutgoingNotification(toEmail, contactName, entityCode,
                    replacedBy, reason, replacementFullName, replacementEmail, orderedBy);
        } catch (Exception e) {
            logger.warn("Outgoing replacement email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendIncomingEmail(String toEmail, String contactName, String entityCode,
                                   String username, String tempPassword) {
        try {
            emailService.sendReplacementWelcome(toEmail, contactName, entityCode, username, tempPassword);
        } catch (Exception e) {
            logger.warn("Incoming replacement email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendReplacementAdminWelcomeEmail(String toEmail, String contactName,
                                                   String bankCode, String userId,
                                                   String defaultPassword, String verifyLink) {
        try {
            emailService.sendReplacementAdminWelcome(toEmail, contactName, bankCode, userId, defaultPassword, verifyLink);
        } catch (Exception e) {
            logger.warn("Replacement admin welcome email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    private ResponseEntity<RestWithStatusList> ok(String msg) {
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", msg, new java.util.ArrayList<>()));
    }

    private ResponseEntity<RestWithStatusList> success(AdminReplacement record, Long replacementEntityId) {
        AdminReplacementResponse resp = new AdminReplacementResponse();
        resp.setReplacementId(record.getId());
        resp.setEntityType(record.getEntityType());
        resp.setOriginalEntityId(record.getOriginalEntityId());
        resp.setReplacementEntityId(replacementEntityId);
        resp.setStatus(record.getStatus());
        resp.setReplacedAt(record.getReplacedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        resp.setReplacedBy(record.getReplacedBy());
        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Replacement completed successfully.", Arrays.asList(resp)),
                HttpStatus.OK);
    }

    private ResponseEntity<RestWithStatusList> fail(String msg) {
        logger.warn("AdminReplacement failed: {}", msg);
        return new ResponseEntity<>(
                new RestWithStatusList("FAILURE", msg, null),
                HttpStatus.OK);
    }
}
