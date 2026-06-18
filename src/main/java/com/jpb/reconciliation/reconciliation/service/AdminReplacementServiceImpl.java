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

    @Autowired private MainAdminRepository mainAdminRepository;
    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private AddUserRepository addUserRepository;
    @Autowired private AdminReplacementRepository replacementRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> replace(AdminReplacementRequest request, String replacedBy) {
        String type = request.getEntityType();
        if (type == null || type.trim().isEmpty()) {
            return fail("entityType is required (MAIN_ADMIN / BRANCH_ADMIN / USER)");
        }
        switch (type.toUpperCase()) {
            case "MAIN_ADMIN":    return replaceMainAdmin(request, replacedBy);
            case "BRANCH_ADMIN":  return replaceBranchAdmin(request, replacedBy);
            case "USER":          return replaceUser(request, replacedBy);
            default:              return fail("Unknown entityType: " + type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN_ADMIN replacement
    // ─────────────────────────────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> replaceMainAdmin(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");
        if (req.getNewUsername() == null || req.getNewUsername().trim().isEmpty()) return fail("newUsername is required");

        Optional<MainAdmin> originalOpt = mainAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original admin not found with id: " + req.getOriginalEntityId());

        MainAdmin original = originalOpt.get();
        if (!"INACTIVE".equalsIgnoreCase(original.getStatus())) {
            return fail("Original admin must be INACTIVE to be replaced. Current status: " + original.getStatus());
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(
                original.getId(), "MAIN_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this admin.");
        }

        if (mainAdminRepository.existsByBankCodeAndUsername(original.getBankCode(), req.getNewUsername().trim())) {
            return fail("Username '" + req.getNewUsername().trim() + "' already exists for bank " + original.getBankCode());
        }

        String tempPassword = generateTempPassword();

        MainAdmin replacement = new MainAdmin();
        replacement.setBankCode(original.getBankCode());
        replacement.setUsername(req.getNewUsername().trim());
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(1);
        replacement.setStatus("ACTIVE");
        replacement.setCreatedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        replacement.setUpdatedAt(LocalDateTime.now());
        mainAdminRepository.save(replacement);
        logger.info("Replacement MainAdmin created: username={}, bankCode={}", replacement.getUsername(), replacement.getBankCode());

        AdminReplacement record = buildRecord("MAIN_ADMIN", original.getBankCode(), null,
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBankCode(), replacedBy, req.getReason());
        sendIncomingEmail(replacement.getEmail(), replacement.getUsername(), original.getBankCode(), replacement.getUsername(), tempPassword);

        return success(record, replacement.getId());
    }

    // ─────────────────────────────────────────────────────────────────────
    // BRANCH_ADMIN replacement
    // ─────────────────────────────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> replaceBranchAdmin(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");
        if (req.getNewUsername() == null || req.getNewUsername().trim().isEmpty()) return fail("newUsername is required");

        Optional<BranchAdmin> originalOpt = branchAdminRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original branch admin not found with id: " + req.getOriginalEntityId());

        BranchAdmin original = originalOpt.get();
        if (!"INACTIVE".equalsIgnoreCase(original.getStatus())) {
            return fail("Original branch admin must be INACTIVE to be replaced. Current status: " + original.getStatus());
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(
                original.getId(), "BRANCH_ADMIN", "ACTIVE")) {
            return fail("An active replacement already exists for this branch admin.");
        }

        if (branchAdminRepository.existsByBranchCodeAndUsername(original.getBranchCode(), req.getNewUsername().trim())) {
            return fail("Username '" + req.getNewUsername().trim() + "' already exists for branch " + original.getBranchCode());
        }

        String tempPassword = generateTempPassword();

        BranchAdmin replacement = new BranchAdmin();
        replacement.setBranchCode(original.getBranchCode());
        replacement.setUsername(req.getNewUsername().trim());
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(1);
        replacement.setStatus("ACTIVE");
        replacement.setCreatedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        replacement.setUpdatedAt(LocalDateTime.now());
        branchAdminRepository.save(replacement);
        logger.info("Replacement BranchAdmin created: username={}, branchCode={}", replacement.getUsername(), replacement.getBranchCode());

        AdminReplacement record = buildRecord("BRANCH_ADMIN", null, original.getBranchCode(),
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        sendOutgoingEmail(original.getEmail(), original.getUsername(), original.getBranchCode(), replacedBy, req.getReason());
        sendIncomingEmail(replacement.getEmail(), replacement.getUsername(), original.getBranchCode(), replacement.getUsername(), tempPassword);

        return success(record, replacement.getId());
    }

    // ─────────────────────────────────────────────────────────────────────
    // USER replacement
    // ─────────────────────────────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> replaceUser(AdminReplacementRequest req, String replacedBy) {
        if (req.getOriginalEntityId() == null) return fail("originalEntityId is required");
        if (req.getNewEmail() == null || req.getNewEmail().trim().isEmpty()) return fail("newEmail is required");
        if (req.getNewUsername() == null || req.getNewUsername().trim().isEmpty()) return fail("newUsername is required");
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) return fail("fullName is required for USER replacement");
        if (req.getRole() == null || req.getRole().trim().isEmpty()) return fail("role is required for USER replacement");
        if (req.getUserType() == null || req.getUserType().trim().isEmpty()) return fail("userType is required for USER replacement");

        Optional<AddUser> originalOpt = addUserRepository.findById(req.getOriginalEntityId());
        if (!originalOpt.isPresent()) return fail("Original user not found with id: " + req.getOriginalEntityId());

        AddUser original = originalOpt.get();
        if (original.getStatus() != AddUser.UserStatus.INACTIVE) {
            return fail("Original user must be INACTIVE to be replaced. Current status: " + original.getStatus());
        }

        if (replacementRepository.existsByOriginalEntityIdAndEntityTypeAndStatus(
                original.getId(), "USER", "ACTIVE")) {
            return fail("An active replacement already exists for this user.");
        }

        if (addUserRepository.existsByUsername(req.getNewUsername().trim())) {
            return fail("Username '" + req.getNewUsername().trim() + "' already exists.");
        }
        if (addUserRepository.existsByEmail(req.getNewEmail().trim())) {
            return fail("Email '" + req.getNewEmail().trim() + "' already exists.");
        }

        AddUser.Role role;
        try {
            role = AddUser.Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fail("Invalid role: " + req.getRole());
        }

        AddUser.UserType userType;
        try {
            userType = AddUser.UserType.valueOf(req.getUserType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fail("Invalid userType: " + req.getUserType());
        }

        String tempPassword = generateTempPassword();

        AddUser replacement = new AddUser();
        replacement.setFullName(req.getFullName().trim());
        replacement.setUsername(req.getNewUsername().trim());
        replacement.setEmail(req.getNewEmail().trim());
        replacement.setRole(role);
        replacement.setUserType(userType);
        replacement.setRoleType(userType.name());
        replacement.setDepartment(req.getDepartment());
        replacement.setDesignation(req.getDesignation());
        replacement.setMobileNumber(req.getMobileNumber());
        replacement.setBankCode(original.getBankCode());
        replacement.setBranchCode(original.getBranchCode());
        replacement.setDefaultPassword(passwordEncoder.encode(tempPassword));
        replacement.setPasswordSet(1);
        replacement.setStatus(AddUser.UserStatus.ACTIVE);
        replacement.setCreatedBy(replacedBy);

        if (AddUser.UserType.EXTERNAL.equals(userType)) {
            replacement.setExternalDepartmentName(req.getExternalDepartmentName());
            replacement.setExternalSupervisorName(req.getExternalSupervisorName());
            replacement.setExternalSupervisorEmail(req.getExternalSupervisorEmail());
            replacement.setExternalSupervisorPhone(req.getExternalSupervisorPhone());
        }

        addUserRepository.save(replacement);
        logger.info("Replacement User created: username={}, bankCode={}, branchCode={}", replacement.getUsername(), replacement.getBankCode(), replacement.getBranchCode());

        String entityCode = original.getBranchCode() != null ? original.getBranchCode() : original.getBankCode();
        AdminReplacement record = buildRecord("USER", original.getBankCode(), original.getBranchCode(),
                original.getId(), replacement.getId(), req.getReason(), replacedBy);
        replacementRepository.save(record);

        sendOutgoingEmail(original.getEmail(), original.getFullName(), entityCode, replacedBy, req.getReason());
        sendIncomingEmail(replacement.getEmail(), replacement.getFullName(), entityCode, replacement.getUsername(), tempPassword);

        return success(record, replacement.getId());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────
    private AdminReplacement buildRecord(String entityType, String bankCode, String branchCode,
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

    private String generateTempPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    private void sendOutgoingEmail(String toEmail, String contactName, String entityCode,
                                   String replacedBy, String reason) {
        try {
            emailService.sendReplacementOutgoingNotification(toEmail, contactName, entityCode, replacedBy, reason);
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
