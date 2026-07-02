package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReconBankMasterServiceImpl implements ReconBankMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconBankMasterServiceImpl.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private ReconRoleMasterRepository reconRoleMasterRepository;
    @Autowired private MenuMasterRepository menuMasterRepository;
    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconPasswordManagerRepository reconPasswordManagerRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private CBankProductMapRepository bankProductMapRepository;
    @Autowired private ReconProductMasterRepository productMasterRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.logo.upload-dir:/home/reconapp/kalrecon/bank_logos/}")
    private String logoUploadDir;

    private static final List<String> ALLOWED_LOGO_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/tiff", "image/tif"
    );
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createBank(ReconBankMaster bank, String createdBy) {
        // Name uniqueness is intentionally not enforced here — matches old backend
        // behavior (MainBankServiceImpl/BranchBankServiceImpl.checkNameExists()),
        // where the same institution name is always allowed for both banks and branches.

        // ── Determine branch vs bank ─────────────────────────────────────────
        boolean isBranch     = bank.getParentBankId() != null;
        String adminUserType = isBranch ? "BRANCH_ADMIN" : "BANK_ADMIN";
        String roleName      = isBranch ? "BRANCH_ADMIN_" : "BANK_ADMIN_";
        bank.setBankLevel(isBranch ? "BRANCH" : "BANK");

        // ── Resolve / generate BANK_CODE ─────────────────────────────────────
        // Use frontend's pre-generated preview code if it's an unused 8-digit
        // code — keeps the code shown during onboarding identical to what gets
        // persisted. Applies to both Bank and Branch creation.
        String generatedCode;
        String frontendCode = bank.getBankCode();
        boolean frontendCodeUsable = frontendCode != null && frontendCode.matches("\\d{8}")
                && !reconBankMasterRepository.existsByBankCodeAndContactRank(frontendCode, "PRIMARY");
        if (frontendCodeUsable) {
            generatedCode = frontendCode;
        } else if (isBranch) {
            // Fallback: generate using parent bank's first-4 prefix + timestamp last-4
            generatedCode = generateBranchCode(bank.getParentBankId());
        } else {
            generatedCode = generateBankCode();
        }
        bank.setBankCode(generatedCode);

        // ── Map transient address lines → regAddress ─────────────────────────
        String combinedAddress = Stream.of(bank.getRegAddressLine1(), bank.getRegAddressLine2(), bank.getRegAddressLine3())
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.joining(", "));
        if (!combinedAddress.trim().isEmpty()) bank.setRegAddress(combinedAddress);

        // ── Map transient phone parts → regPhone ─────────────────────────────
        String phoneCode = bank.getRegPhoneCode();
        String cityCode  = bank.getRegCityCode();
        String phoneNum  = bank.getRegPhone();
        if ((phoneCode != null && !phoneCode.trim().isEmpty()) || (cityCode != null && !cityCode.trim().isEmpty())) {
            String combined = Stream.of(phoneCode, cityCode, phoneNum)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.joining("-"));
            bank.setRegPhone(combined);
        }

        bank.setContactRank("PRIMARY");
        bank.setCreatedAt(LocalDateTime.now());
        bank.setCreatedBy(createdBy);
        if (bank.getStatus() == null) bank.setStatus("REQUEST");

        // ── Resolve KalAdmin userId (stored as parentUserId on admin users) ──
        Long kalAdminUserId = reconUserRepository.findByUsername(createdBy)
                .map(ReconUser::getUserId).orElse(null);

        // ── Save PRIMARY bank row ────────────────────────────────────────────
        ReconBankMaster savedPrimaryBank = reconBankMasterRepository.save(bank);
        saveProductMappings(savedPrimaryBank.getBankId(), bank, createdBy);

        String roleCode = roleName + generatedCode;
        createDefaultAdminMenus(generatedCode, roleCode, isBranch, createdBy);

        saveAuditLog("RECON_BANK_MASTER", savedPrimaryBank.getBankId(), "CREATE", null, null,
                createdBy, adminUserType, savedPrimaryBank.getBankId(),
                "Bank " + (isBranch ? "(Branch) " : "") + generatedCode + " onboarded");

        // ── Save SECONDARY bank row (if secondary contact provided) ──────────
        if (bank.getSecondaryEmail() != null && !bank.getSecondaryEmail().trim().isEmpty()) {
            ReconBankMaster secondaryRow = new ReconBankMaster();
            secondaryRow.setBankCode(generatedCode);
            secondaryRow.setBankName(bank.getBankName());
            secondaryRow.setBankType(bank.getBankType());
            secondaryRow.setParentBankId(bank.getParentBankId());
            secondaryRow.setContactRank("SECONDARY");
            secondaryRow.setFullName(bank.getSecondaryFullName());
            secondaryRow.setEmail(bank.getSecondaryEmail().trim().toLowerCase());
            secondaryRow.setMobileNumber(bank.getSecondaryMobileNumber());
            secondaryRow.setAltMobileNumber(bank.getSecondaryAltMobile());
            secondaryRow.setStatus("INACTIVE");
            secondaryRow.setCreatedAt(LocalDateTime.now());
            secondaryRow.setCreatedBy(createdBy);
            reconBankMasterRepository.save(secondaryRow);
            logger.info("Secondary bank row saved for bankCode={}", generatedCode);
        }

        // ── Create PRIMARY admin user in RCN_RECON_USER ──────────────────────
        if (bank.getEmail() != null && !bank.getEmail().trim().isEmpty()) {
            try {
                String defaultPwd = generatePassword(10);
                String username   = deriveUsername(bank.getEmail(), bank.getFullName());

                ReconUser primaryUser = buildAdminUser(
                        savedPrimaryBank.getBankId(), bank.getFullName(), bank.getEmail(),
                        bank.getMobileNumber(), username, adminUserType, "PRIMARY",
                        defaultPwd, "REQUEST", createdBy);
                primaryUser.setParentUserId(kalAdminUserId);

                Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findByRoleCode(roleCode);
                roleOpt.ifPresent(r -> primaryUser.setRoleId(r.getRoleId()));

                ReconUser savedPrimaryUser = reconUserRepository.saveAndFlush(primaryUser);
                savePasswordHistory(savedPrimaryUser, createdBy);

                savedPrimaryBank.setBankAdminUsername(username);
                savedPrimaryBank.setDefaultPassword(passwordEncoder.encode(defaultPwd));
                reconBankMasterRepository.save(savedPrimaryBank);

                saveAuditLog("RCN_RECON_USER", savedPrimaryUser.getUserId(), "CREATE", null,
                        "userType=" + adminUserType + ",contactRank=PRIMARY,status=REQUEST",
                        createdBy, adminUserType, savedPrimaryBank.getBankId(),
                        "Primary " + adminUserType + " created: " + username);

                String verifyPage = isBranch ? "/branch-verify-email" : "/verify-email";
                String verifyLink = frontendUrl + verifyPage
                        + "?bankCode=" + generatedCode
                        + "&username=" + username;
                emailService.sendBankAdminWelcome(
                        savedPrimaryUser.getEmail(), savedPrimaryUser.getFullName(),
                        savedPrimaryBank.getBankName(), generatedCode,
                        username, defaultPwd, verifyLink);

                // ── Create SECONDARY admin user in RCN_RECON_USER ────────────
                if (bank.getSecondaryEmail() != null && !bank.getSecondaryEmail().trim().isEmpty()) {
                    String secUsername   = deriveUsername(bank.getSecondaryEmail(), bank.getSecondaryFullName());
                    String secDefaultPwd = generatePassword(10);

                    ReconUser secondaryUser = buildAdminUser(
                            savedPrimaryBank.getBankId(), bank.getSecondaryFullName(), bank.getSecondaryEmail(),
                            bank.getSecondaryMobileNumber(), secUsername, adminUserType, "SECONDARY",
                            secDefaultPwd, "INACTIVE", createdBy);
                    secondaryUser.setParentUserId(kalAdminUserId);
                    roleOpt.ifPresent(r -> secondaryUser.setRoleId(r.getRoleId()));

                    ReconUser savedSecondaryUser = reconUserRepository.saveAndFlush(secondaryUser);
                    savePasswordHistory(savedSecondaryUser, createdBy);

                    saveAuditLog("RCN_RECON_USER", savedSecondaryUser.getUserId(), "CREATE", null,
                            "userType=" + adminUserType + ",contactRank=SECONDARY,status=INACTIVE",
                            createdBy, adminUserType, savedPrimaryBank.getBankId(),
                            "Secondary " + adminUserType + " created (INACTIVE): " + secUsername);

                    logger.info("Secondary {} created (INACTIVE): {} for bank {}", adminUserType, secUsername, generatedCode);
                }

                logger.info("Primary {} created and welcome email sent: {} for bank {}", adminUserType, username, generatedCode);
            } catch (Exception e) {
                logger.error("Failed to create admin users for bank {}: {}", generatedCode, e.getMessage(), e);
            }
        }

        logger.info("ReconBankMaster created: {} by {}", generatedCode, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS",
                        (isBranch ? "Branch" : "Bank") + " created successfully.", Collections.singletonList(savedPrimaryBank)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        List<ReconBankMaster> banks = reconBankMasterRepository.findAllPrimary();
        banks.forEach(this::enrichWithProducts);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Banks fetched.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findPrimaryById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster found = opt.get();
        enrichWithProducts(found);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank found.", Collections.singletonList(found)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankByCode(String bankCode) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findByBankCode(bankCode);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with code: " + bankCode, null));
        }
        ReconBankMaster found = opt.get();
        enrichWithProducts(found);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank found.", Collections.singletonList(found)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBanksByStatus(String status) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Banks fetched by status.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBanksByType(String bankType) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByBankType(bankType);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Banks fetched by type.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBranchBanks(Long parentBankId) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByParentBankId(parentBankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Branch banks fetched.", banks));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateBank(Long bankId, ReconBankMaster bank, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if (bank.getBankName() != null) existing.setBankName(bank.getBankName());
        if (bank.getBankCategory() != null) existing.setBankCategory(bank.getBankCategory());
        if (bank.getRegAddress() != null) existing.setRegAddress(bank.getRegAddress());
        if (bank.getRegCity() != null) existing.setRegCity(bank.getRegCity());
        if (bank.getRegState() != null) existing.setRegState(bank.getRegState());
        if (bank.getRegCountry() != null) existing.setRegCountry(bank.getRegCountry());
        if (bank.getRegPhone() != null) existing.setRegPhone(bank.getRegPhone());
        if (bank.getSelectedProducts() != null && !bank.getSelectedProducts().isEmpty()) {
            saveProductMappings(bankId, bank, updatedBy);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);
        saveAuditLog("RECON_BANK_MASTER", bankId, "UPDATE", null, null, updatedBy, null, bankId, "Bank updated");
        logger.info("ReconBankMaster updated: {} by {}", bankId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status, String updatedBy) {
        List<String> validStatuses = Arrays.asList(
                "REQUEST", "VERIFIED", "ACTIVE", "INACTIVE", "BLOCKED",
                "BLOCK_PENDING", "INACTIVE_PENDING", "ACTIVE_PENDING");
        if (status == null || !validStatuses.contains(status.toUpperCase())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Invalid status. Allowed: " + validStatuses, null));
        }
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        String oldStatus = existing.getStatus();
        String newStatus = status.toUpperCase();

        // BLOCKED is permanent — only unblockBank() can restore
        if ("BLOCKED".equals(oldStatus)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "This institution is permanently BLOCKED. Its status cannot be changed.", null));
        }

        // Transition validation
        Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
        allowedTransitions.put("REQUEST",          Arrays.asList("VERIFIED", "ACTIVE", "INACTIVE", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("VERIFIED",         Arrays.asList("ACTIVE", "INACTIVE", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("ACTIVE",           Arrays.asList("INACTIVE", "INACTIVE_PENDING", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("INACTIVE",         Arrays.asList("ACTIVE", "ACTIVE_PENDING", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("INACTIVE_PENDING", Arrays.asList("ACTIVE", "INACTIVE", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("ACTIVE_PENDING",   Arrays.asList("ACTIVE", "INACTIVE", "BLOCK_PENDING", "BLOCKED"));
        allowedTransitions.put("BLOCK_PENDING",    Arrays.asList("ACTIVE", "INACTIVE", "BLOCKED"));
        List<String> allowed = allowedTransitions.getOrDefault(oldStatus, new ArrayList<>());
        if (!allowed.contains(newStatus)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Cannot change status from '" + oldStatus + "' to '" + newStatus + "'. Allowed: " + allowed, null));
        }

        existing.setStatus(newStatus);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Sync status to this institution's admin users (BANK_ADMIN / BRANCH_ADMIN)
        syncAdminStatus(bankId, newStatus, updatedBy);

        saveAuditLog("RECON_BANK_MASTER", bankId, "STATUS_CHANGE", oldStatus, newStatus, updatedBy, null, bankId,
                "Status changed: " + oldStatus + " → " + newStatus);
        logger.info("ReconBankMaster status updated to {} for ID: {} by {}", newStatus, bankId, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), oldStatus, newStatus);
            }
        } catch (Exception e) {
            logger.warn("Status change email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Status Changed to " + newStatus, existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteBank(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        String prevStatus = existing.getStatus();
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        reconBankMasterRepository.save(existing);
        syncAdminStatus(bankId, "INACTIVE", "SYSTEM");
        saveAuditLog("RECON_BANK_MASTER", bankId, "DELETE", prevStatus, "INACTIVE", null, null, bankId,
                "Bank soft-deleted (INACTIVE)");
        logger.info("ReconBankMaster soft-deleted (INACTIVE): {}", bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkBankCodeExists(String bankCode) {
        boolean exists = reconBankMasterRepository.existsByBankCode(bankCode);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkBankNameExists(String bankName) {
        boolean exists = reconBankMasterRepository.existsByBankName(bankName);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> blockBank(Long bankId, String reason, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank is already BLOCKED.", null));
        }
        String preBlock = existing.getStatus();
        existing.setPreBlockStatus(preBlock);
        existing.setStatus("BLOCKED");
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Cascade BLOCKED to all users of this institution
        cascadeBlockUsersNow(bankId, reason, updatedBy);

        // If top-level bank: cascade BLOCKED to all branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if ("BLOCKED".equals(branch.getStatus())) continue;
                branch.setPreBlockStatus(branch.getStatus());
                branch.setStatus("BLOCKED");
                branch.setBlockReason(reason);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(updatedBy);
                reconBankMasterRepository.save(branch);
                cascadeBlockUsersNow(branch.getBankId(), reason, updatedBy);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankStatusNotification(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(), branch.getPreBlockStatus(), "BLOCKED",
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Branch block cascade email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "BLOCK", preBlock, "BLOCKED", updatedBy, null, bankId,
                "Bank BLOCKED. Reason: " + reason);
        logger.info("ReconBankMaster BLOCKED: bankId={} preBlockStatus={} by {}", bankId, preBlock, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), preBlock, "BLOCKED");
            }
        } catch (Exception e) {
            logger.warn("Block email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Blocked", existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank blocked successfully.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> unblockBank(Long bankId, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if (!"BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank is not currently BLOCKED.", null));
        }
        String restoreStatus = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : "ACTIVE";
        existing.setStatus(restoreStatus);
        existing.setPreBlockStatus(null);
        existing.setBlockReason(null);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Restore all BLOCKED users of this institution to their pre-block status
        cascadeUnblockUsers(bankId, updatedBy);

        // If top-level bank: restore blocked branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if (!"BLOCKED".equals(branch.getStatus())) continue;
                String branchRestored = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : "ACTIVE";
                branch.setStatus(branchRestored);
                branch.setPreBlockStatus(null);
                branch.setBlockReason(null);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(updatedBy);
                reconBankMasterRepository.save(branch);
                cascadeUnblockUsers(branch.getBankId(), updatedBy);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankBlockCancelled(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Branch unblock email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "UNBLOCK", "BLOCKED", restoreStatus, updatedBy, null, bankId,
                "Bank UNBLOCKED, status restored to: " + restoreStatus);
        logger.info("ReconBankMaster UNBLOCKED: bankId={} restored to {} by {}", bankId, restoreStatus, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendBlockCancelled(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), restoreStatus);
            }
        } catch (Exception e) {
            logger.warn("Unblock email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Unblocked", existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank unblocked. Status restored to: " + restoreStatus, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus()) || "BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Cannot schedule inactivation — institution is " + existing.getStatus() + ".", null));
        }
        if ("INACTIVE".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Institution is already INACTIVE.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setStatus("INACTIVE_PENDING");
        existing.setInactivateScheduledAt(scheduledAt);
        existing.setInactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        // Cascade INACTIVE_PENDING to all active users of this institution (warning popup on their side)
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if ("INACTIVE".equals(u.getStatus()) || "BLOCKED".equals(u.getStatus())
                    || "BLOCK_PENDING".equals(u.getStatus())) continue;
            u.setStatus("INACTIVE_PENDING");
            u.setInactivateScheduledAt(scheduledAt);
            u.setInactivateScheduledBy("CASCADE:" + scheduledBy);
            u.setUpdatedAt(LocalDateTime.now());
            u.setUpdatedBy(scheduledBy);
            reconUserRepository.save(u);
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "INACTIVATE@" + scheduledAt, scheduledBy, null, bankId,
                "Inactivation scheduled at: " + scheduledAt);
        logger.info("ReconBankMaster INACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Warning email to institution primary contact + actor confirmation
        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendInactivatePendingWarning(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Inactivate warning email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Inactivation Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank inactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus()) || "BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Cannot schedule reactivation — institution is " + existing.getStatus() + ".", null));
        }
        if ("ACTIVE".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Institution is already ACTIVE.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setStatus("ACTIVE_PENDING");
        existing.setReactivateScheduledAt(scheduledAt);
        existing.setReactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        // Cascade ACTIVE_PENDING to inactive users of this institution
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if (!"INACTIVE".equals(u.getStatus()) && !"INACTIVE_PENDING".equals(u.getStatus())) continue;
            u.setStatus("ACTIVE_PENDING");
            u.setReactivateScheduledAt(scheduledAt);
            u.setReactivateScheduledBy("CASCADE:" + scheduledBy);
            u.setInactivateScheduledAt(null);
            u.setInactivateScheduledBy(null);
            u.setUpdatedAt(LocalDateTime.now());
            u.setUpdatedBy(scheduledBy);
            reconUserRepository.save(u);
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "REACTIVATE@" + scheduledAt, scheduledBy, null, bankId,
                "Reactivation scheduled at: " + scheduledAt);
        logger.info("ReconBankMaster REACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Notification email to institution primary contact + actor confirmation
        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendReactivatePendingNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Reactivate pending email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Reactivation Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank reactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, LocalDateTime scheduledAt, String scheduledBy, String reason) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Institution is already permanently BLOCKED.", null));
        }
        if ("BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                    "Block is already scheduled for this institution.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setPreBlockStatus(oldStatus);
        existing.setStatus("BLOCK_PENDING");
        existing.setBlockScheduledAt(scheduledAt);
        existing.setBlockScheduledBy(scheduledBy);
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // Cascade BLOCK_PENDING to all users of this institution + warning email each
        cascadeScheduleBlockUsers(bankId, scheduledAt, scheduledBy, reason,
                existing.getBankName(), existing.getBankCode(), atFormatted);

        // If top-level bank: cascade BLOCK_PENDING to all branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if ("BLOCKED".equals(branch.getStatus()) || "BLOCK_PENDING".equals(branch.getStatus())) continue;
                branch.setPreBlockStatus(branch.getStatus());
                branch.setStatus("BLOCK_PENDING");
                branch.setBlockScheduledAt(scheduledAt);
                branch.setBlockScheduledBy("CASCADE:" + scheduledBy);
                branch.setBlockReason(reason);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(scheduledBy);
                reconBankMasterRepository.save(branch);
                cascadeScheduleBlockUsers(branch.getBankId(), scheduledAt, scheduledBy, reason,
                        branch.getBankName(), branch.getBankCode(), atFormatted);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankBlockWarning(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(),
                                existing.getBankName(), existing.getBankCode(), atFormatted);
                    }
                } catch (Exception e) {
                    logger.warn("Branch block warning email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "BLOCK@" + scheduledAt, scheduledBy, null, bankId,
                "Block scheduled at: " + scheduledAt + ". Reason: " + reason);
        logger.info("ReconBankMaster BLOCK scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Warning email to institution primary contact + actor confirmation
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendBlockWarning(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Block warning email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Block Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank block scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> cancelSchedule(Long bankId, String scheduleType, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if (scheduleType == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "scheduleType is required (INACTIVATE / REACTIVATE / BLOCK).", null));
        }
        switch (scheduleType.toUpperCase()) {
            case "INACTIVATE":
                existing.setInactivateScheduledAt(null);
                existing.setInactivateScheduledBy(null);
                // Restore status + cascade users back to ACTIVE
                if ("INACTIVE_PENDING".equals(existing.getStatus())) {
                    existing.setStatus("ACTIVE");
                    for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                        if (!"INACTIVE_PENDING".equals(u.getStatus())) continue;
                        u.setStatus("ACTIVE");
                        u.setInactivateScheduledAt(null);
                        u.setInactivateScheduledBy(null);
                        u.setUpdatedAt(LocalDateTime.now());
                        u.setUpdatedBy(updatedBy);
                        reconUserRepository.save(u);
                    }
                }
                try {
                    if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                        emailService.sendInactivateCancelled(existing.getEmail(), existing.getFullName(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Inactivate cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                }
                break;
            case "REACTIVATE":
                existing.setReactivateScheduledAt(null);
                existing.setReactivateScheduledBy(null);
                // Restore status + cascade users back to INACTIVE
                if ("ACTIVE_PENDING".equals(existing.getStatus())) {
                    existing.setStatus("INACTIVE");
                    for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                        if (!"ACTIVE_PENDING".equals(u.getStatus()) || u.getReactivateScheduledAt() == null) continue;
                        u.setStatus("INACTIVE");
                        u.setReactivateScheduledAt(null);
                        u.setReactivateScheduledBy(null);
                        u.setUpdatedAt(LocalDateTime.now());
                        u.setUpdatedBy(updatedBy);
                        reconUserRepository.save(u);
                    }
                }
                try {
                    if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                        emailService.sendReactivateCancelled(existing.getEmail(), existing.getFullName(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Reactivate cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                }
                break;
            case "BLOCK":
                existing.setBlockScheduledAt(null);
                existing.setBlockScheduledBy(null);
                existing.setBlockReason(null);
                // Restore status from preBlockStatus + cascade users and branches back
                if ("BLOCK_PENDING".equals(existing.getStatus())) {
                    String restored = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : "ACTIVE";
                    existing.setStatus(restored);
                    existing.setPreBlockStatus(null);
                    cascadeCancelBlockUsers(bankId, updatedBy, existing.getBankName(), existing.getBankCode());
                    if (existing.getParentBankId() == null) {
                        for (ReconBankMaster branch : reconBankMasterRepository.findByParentBankId(bankId)) {
                            if (!"BLOCK_PENDING".equals(branch.getStatus())) continue;
                            String branchRestored = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : "ACTIVE";
                            branch.setStatus(branchRestored);
                            branch.setPreBlockStatus(null);
                            branch.setBlockScheduledAt(null);
                            branch.setBlockScheduledBy(null);
                            branch.setBlockReason(null);
                            branch.setUpdatedAt(LocalDateTime.now());
                            branch.setUpdatedBy(updatedBy);
                            reconBankMasterRepository.save(branch);
                            cascadeCancelBlockUsers(branch.getBankId(), updatedBy, branch.getBankName(), branch.getBankCode());
                            try {
                                if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                                    emailService.sendBranchBankBlockCancelled(branch.getEmail(), branch.getFullName(),
                                            branch.getBankName(), branch.getBankCode(),
                                            existing.getBankName(), existing.getBankCode());
                                }
                            } catch (Exception e) {
                                logger.warn("Branch block cancel email failed for {}: {}", branch.getBankCode(), e.getMessage());
                            }
                        }
                    }
                    try {
                        if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                            emailService.sendBlockCancelled(existing.getEmail(), existing.getFullName(),
                                    existing.getBankName(), existing.getBankCode(), restored);
                        }
                    } catch (Exception e) {
                        logger.warn("Block cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                    }
                }
                break;
            default:
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList("FAILURE", "Invalid scheduleType. Use INACTIVATE, REACTIVATE, or BLOCK.", null));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);
        saveAuditLog("RECON_BANK_MASTER", bankId, "CANCEL_SCHEDULE", scheduleType, null, updatedBy, null, bankId,
                scheduleType + " schedule cancelled");
        logger.info("ReconBankMaster {} schedule cancelled for bankId={} by {}", scheduleType, bankId, updatedBy);
        notifyActor(updatedBy, scheduleType + " Schedule Cancelled", existing.getBankName(), existing.getBankCode(), null);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", scheduleType + " schedule cancelled.", null));
    }

    // ── Helper: build a ReconUser for primary/secondary admin ──────────────────
    private ReconUser buildAdminUser(Long bankId, String fullName, String email,
                                     String mobile, String username, String userType,
                                     String contactRank, String plainPassword,
                                     String status, String createdBy) {
        ReconUser u = new ReconUser();
        u.setBankId(bankId);
        u.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName : username);
        u.setEmail(email.trim().toLowerCase());
        u.setMobileNumber(mobile);
        u.setUsername(username);
        u.setUserType(userType);
        u.setContactRank(contactRank);
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setPasswordSet(0);
        u.setStatus(status);
        u.setApprovedYn("N");
        u.setCreatedAt(LocalDateTime.now());
        u.setCreatedBy(createdBy);
        return u;
    }

    // ── Helper: save to RCN_RECON_PWD_MANAGER ──────────────────────────────────
    private void savePasswordHistory(ReconUser user, String createdBy) {
        ReconPasswordManager pwd = new ReconPasswordManager();
        pwd.setReconUser(user);
        pwd.setUserPassword(user.getPasswordHash());
        pwd.setCreatedAt(LocalDateTime.now());
        pwd.setCreatedBy(createdBy);
        pwd.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pwd);
    }

    // ── Helper: save to AUDIT_LOG ───────────────────────────────────────────────
    private void saveAuditLog(String tableName, Long recordId, String operation,
                               String oldValue, String newValue,
                               String actorUsername, String actorType,
                               Long bankId, String actionLabel) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setOperation(operation);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setActorUsername(actorUsername);
            log.setActorType(actorType);
            log.setBankId(bankId);
            log.setActionLabel(actionLabel);
            log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("AuditLog save failed: {}", e.getMessage());
        }
    }

    // ── Status flow helpers ──────────────────────────────────────────────────────

    // Email to the actor (admin who performed the action) — resolved from RCN_RECON_USER
    private void notifyActor(String actorUsername, String action, String targetName, String targetCode, String scheduledAt) {
        try {
            if (actorUsername == null || "UNKNOWN".equals(actorUsername) || "SCHEDULER".equals(actorUsername)) return;
            Optional<ReconUser> actor = reconUserRepository.findByUsername(actorUsername);
            if (!actor.isPresent()) actor = reconUserRepository.findByEmail(actorUsername);
            if (actor.isPresent() && actor.get().getEmail() != null && !actor.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(actor.get().getEmail(),
                        actor.get().getFullName() != null ? actor.get().getFullName() : actorUsername,
                        action, targetName, targetCode, scheduledAt);
            }
        } catch (Exception e) {
            logger.warn("Actor confirmation email failed for {}: {}", actorUsername, e.getMessage());
        }
    }

    // Direct status sync to this institution's admin users only (BANK_ADMIN / BRANCH_ADMIN)
    private void syncAdminStatus(Long bankId, String newStatus, String updatedBy) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BANK_ADMIN".equals(u.getUserType()) && !"BRANCH_ADMIN".equals(u.getUserType())) continue;
                if ("BLOCKED".equals(u.getStatus())) continue;
                u.setStatus(newStatus);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                logger.info("Admin status synced: {} → {}", u.getUsername(), newStatus);
            }
        } catch (Exception e) {
            logger.warn("Admin status sync failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // Immediate BLOCKED cascade to all users of an institution
    private void cascadeBlockUsersNow(Long bankId, String reason, String updatedBy) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if ("BLOCKED".equals(u.getStatus())) continue;
                if (u.getPreBlockStatus() == null) u.setPreBlockStatus(u.getStatus());
                u.setStatus("BLOCKED");
                u.setBlockReason(reason);
                u.setBlockScheduledAt(null);
                u.setBlockScheduledBy(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                try {
                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockedNotification(u.getEmail(), u.getFullName());
                    }
                } catch (Exception e) {
                    logger.warn("User block email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // Restore BLOCKED users to their pre-block status
    private void cascadeUnblockUsers(Long bankId, String updatedBy) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BLOCKED".equals(u.getStatus())) continue;
                String restored = u.getPreBlockStatus() != null ? u.getPreBlockStatus() : "ACTIVE";
                u.setStatus(restored);
                u.setPreBlockStatus(null);
                u.setBlockReason(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
            }
        } catch (Exception e) {
            logger.warn("Unblock cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // BLOCK_PENDING cascade to all users + per-user warning email
    private void cascadeScheduleBlockUsers(Long bankId, LocalDateTime scheduledAt, String scheduledBy,
                                           String reason, String entityName, String entityCode, String atFormatted) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if ("BLOCKED".equals(u.getStatus()) || "BLOCK_PENDING".equals(u.getStatus())) continue;
                u.setPreBlockStatus(u.getStatus());
                u.setStatus("BLOCK_PENDING");
                u.setBlockScheduledAt(scheduledAt);
                u.setBlockScheduledBy("CASCADE:" + scheduledBy);
                u.setBlockReason(reason);
                u.setInactivateScheduledAt(null);
                u.setInactivateScheduledBy(null);
                u.setReactivateScheduledAt(null);
                u.setReactivateScheduledBy(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(scheduledBy);
                reconUserRepository.save(u);
                try {
                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockWarning(u.getEmail(),
                                u.getFullName() != null ? u.getFullName() : u.getUsername(),
                                entityName, entityCode, atFormatted);
                    }
                } catch (Exception e) {
                    logger.warn("User block warning email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Schedule block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // Restore BLOCK_PENDING users to pre-block status + per-user cancellation email
    private void cascadeCancelBlockUsers(Long bankId, String updatedBy, String entityName, String entityCode) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BLOCK_PENDING".equals(u.getStatus())) continue;
                String restored = u.getPreBlockStatus() != null ? u.getPreBlockStatus() : "ACTIVE";
                u.setStatus(restored);
                u.setPreBlockStatus(null);
                u.setBlockScheduledAt(null);
                u.setBlockScheduledBy(null);
                u.setBlockReason(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                try {
                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockCancelled(u.getEmail(),
                                u.getFullName() != null ? u.getFullName() : u.getUsername(),
                                entityName, entityCode, restored);
                    }
                } catch (Exception e) {
                    logger.warn("User block cancel email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Cancel block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // ── Product helpers ──────────────────────────────────────────────────────────

    private void saveProductMappings(Long bankId, ReconBankMaster bank, String actorBy) {
        List<String> products = bank.getSelectedProducts();
        if (products == null || products.isEmpty()) return;
        bankProductMapRepository.deleteByBankId(bankId);
        bankProductMapRepository.flush();
        Map<String, ReconBankMaster.ProductDateEntry> dates = bank.getProductDates();
        for (String productName : products) {
            Optional<ReconProductMaster> prodOpt = productMasterRepository.findByProductName(productName);
            if (!prodOpt.isPresent()) {
                ReconProductMaster newProd = new ReconProductMaster();
                newProd.setProductName(productName);
                newProd.setStatus("ACTIVE");
                newProd.setCreatedAt(LocalDateTime.now());
                newProd.setCreatedBy(actorBy);
                prodOpt = Optional.of(productMasterRepository.save(newProd));
            }
            ReconBankMaster.ProductDateEntry entry = (dates != null) ? dates.get(productName) : null;
            CBankProductMap mapping = new CBankProductMap();
            mapping.setBankId(bankId);
            mapping.setProductId(prodOpt.get().getProductId());
            mapping.setValidFrom(entry != null ? entry.getValidFrom() : null);
            mapping.setValidTo(entry != null ? entry.getValidTo() : null);
            mapping.setStatus("ACTIVE");
            bankProductMapRepository.save(mapping);
        }
        logger.info("Product mappings saved for bankId={}: {}", bankId, products);
    }

    private void enrichWithProducts(ReconBankMaster bank) {
        List<CBankProductMap> mappings = bankProductMapRepository.findByBankIdAndStatus(bank.getBankId(), "ACTIVE");
        List<String> products = new ArrayList<>();
        Map<String, ReconBankMaster.ProductDateEntry> dates = new LinkedHashMap<>();
        for (CBankProductMap m : mappings) {
            productMasterRepository.findById(m.getProductId()).ifPresent(prod -> {
                products.add(prod.getProductName());
                dates.put(prod.getProductName(),
                        new ReconBankMaster.ProductDateEntry(m.getValidFrom(), m.getValidTo()));
            });
        }
        bank.setSelectedProducts(products);
        bank.setProductDates(dates);
    }

    private String generatePassword(int length) {
        int digits = 1000 + RNG.nextInt(9000);
        return "Recon@" + digits;
    }

    // Generates a unique 8-digit bank code from current timestamp (last 8 digits)
    private String generateBankCode() {
        String base = String.valueOf(System.currentTimeMillis());
        String code = base.substring(base.length() - 8);
        while (reconBankMasterRepository.existsByBankCodeAndContactRank(code, "PRIMARY")) {
            code = String.valueOf(System.currentTimeMillis()).substring(base.length() - 8);
        }
        return code;
    }

    // Generates a unique 8-digit branch code: first 4 of parent bank code + last 4 of timestamp
    private String generateBranchCode(Long parentBankId) {
        String prefix = "0000";
        if (parentBankId != null) {
            Optional<ReconBankMaster> parent = reconBankMasterRepository.findPrimaryById(parentBankId);
            if (parent.isPresent() && parent.get().getBankCode() != null
                    && parent.get().getBankCode().length() >= 4) {
                prefix = parent.get().getBankCode().substring(0, 4);
            }
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            String epochStr = String.valueOf(System.currentTimeMillis() + attempt);
            String suffix   = epochStr.substring(epochStr.length() - 4);
            String candidate = prefix + suffix;
            if (!reconBankMasterRepository.existsByBankCodeAndContactRank(candidate, "PRIMARY")) {
                return candidate;
            }
        }
        // Ultimate fallback: full timestamp
        return generateBankCode();
    }

    // Public helper used by the generate-branch-code endpoint
    public String generateBranchCodePublic(Long parentBankId) {
        return generateBranchCode(parentBankId);
    }

    // Public helper used by the generate-code endpoint (top-level bank preview code)
    public String generateBankCodePublic() {
        return generateBankCode();
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> uploadLogo(Long bankId, MultipartFile file, String uploadedBy) {
        Optional<ReconBankMaster> optional = reconBankMasterRepository.findPrimaryById(bankId);
        if (!optional.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "No file provided.", null));
        }
        if (!ALLOWED_LOGO_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Invalid file type. Accepted formats: JPG, TIF.", null));
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "File size exceeds 2 MB limit.", null));
        }
        try {
            File uploadDir = new File(logoUploadDir);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            ReconBankMaster bank = optional.get();
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String savedFilename = bank.getBankCode() + "_logo" + extension;
            Path filePath = Paths.get(logoUploadDir + savedFilename);
            Files.write(filePath, file.getBytes());

            bank.setLogoPath(filePath.toString());
            bank.setUpdatedAt(LocalDateTime.now());
            bank.setUpdatedBy(uploadedBy);
            reconBankMasterRepository.save(bank);

            logger.info("Logo uploaded for bank {}: {}", bankId, filePath);
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Logo uploaded successfully.", null));
        } catch (IOException e) {
            logger.error("Logo upload failed for bank {}: {}", bankId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList("FAILURE", "Logo upload failed. Please try again.", null));
        }
    }

    @Override
    public ResponseEntity<byte[]> getLogoImage(String bankCode) {
        Optional<ReconBankMaster> optional = reconBankMasterRepository.findByBankCodeAndContactRank(bankCode, "PRIMARY");
        if (!optional.isPresent()) return ResponseEntity.notFound().build();
        String logoPath = optional.get().getLogoPath();
        if (logoPath == null || logoPath.trim().isEmpty()) return ResponseEntity.notFound().build();
        try {
            String cleanPath = logoPath.trim().replaceAll("^\"|\"$", "");
            if (cleanPath.startsWith("'") && cleanPath.endsWith("'"))
                cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
            cleanPath = cleanPath.replace('\\', '/');
            Path path = Paths.get(cleanPath);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            byte[] imageBytes = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to serve logo for bank {}: {}", bankCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String deriveUsername(String email, String fullName) {
        String base;
        if (fullName != null && fullName.trim().length() > 0) {
            String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            base = parts.length > 1 ? parts[0] + "." + parts[parts.length - 1] : parts[0];
        } else {
            base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9.]", "");
        }
        String candidate = base;
        int suffix = 1;
        while (reconUserRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private void createDefaultAdminMenus(String bankCode, String roleCode, boolean isBranch, String createdBy) {
        try {

            // Role already exists? Skip
            Optional<ReconRoleMaster> existingRole = reconRoleMasterRepository.findByRoleCode(roleCode);
            if (existingRole.isPresent()) return;

            // Create role for this bank's admin
            ReconRoleMaster role = new ReconRoleMaster();
            role.setRoleCode(roleCode);
            role.setRoleName((isBranch ? "Branch Admin - " : "Bank Admin - ") + bankCode);
            role.setRoleType("EXTERNAL");
            role.setRoleDesc("Default " + (isBranch ? "Branch" : "Bank") + " Admin role for " + bankCode);
            role.setStatus("ACTIVE");
            role.setCreatedBy(createdBy);
            role.setCreatedAt(java.time.LocalDateTime.now());
            ReconRoleMaster savedRole = reconRoleMasterRepository.save(role);
            Long roleId = savedRole.getRoleId();

            String prefix = isBranch ? "/branch-admin" : "/bank-admin";

            if (isBranch) {
                // Branch Admin: standalone Dashboard at top level
                saveMenu(null, "Master", "Dashboard", prefix + "/dashboard", roleId, createdBy);

                // My Organization
                ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, createdBy);
                String myOrgId = String.valueOf(myOrg.getMenuId());
                for (String[] item : Arrays.asList(
                    new String[]{"Overview",     prefix + "/my-organization/overview"},
                    new String[]{"My Hierarchy", prefix + "/my-organization/hierarchy"},
                    new String[]{"User Status",  prefix + "/my-organization/admin-status"}
                )) {
                    saveMenu(myOrgId, "Main", item[0], item[1], roleId, createdBy);
                }
            } else {
                // Bank Admin: My Organization (no standalone Dashboard for Bank Admin)
                ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, createdBy);
                String myOrgId = String.valueOf(myOrg.getMenuId());
                for (String[] item : Arrays.asList(
                    new String[]{"Overview",           prefix + "/my-organization/overview"},
                    new String[]{"Branches",           prefix + "/my-organization/branches"},
                    new String[]{"Branch Onboarding",  prefix + "/branch-onboarding"},
                    new String[]{"My Hierarchy",       prefix + "/my-organization/hierarchy"},
                    new String[]{"User Status",        prefix + "/my-organization/user-status"},
                    new String[]{"Branch Admin Status",prefix + "/my-organization/admin-status"}
                )) {
                    saveMenu(myOrgId, "Main", item[0], item[1], roleId, createdBy);
                }
            }

            // Administration (same for both)
            ReconMenuMaster adminMenu = saveMenu(null, "Master", "Administration", null, roleId, createdBy);
            String adminMenuId = String.valueOf(adminMenu.getMenuId());
            for (String[] item : Arrays.asList(
                new String[]{"Add User",  prefix + "/add-user"},
                new String[]{"Add Role",  prefix + "/admin/add-new-role"},
                new String[]{"Add Menu",  prefix + "/add-menu"},
                new String[]{"User List", prefix + "/user-list"},
                new String[]{"Role List", prefix + "/role-list"},
                new String[]{"Menu List", prefix + "/menu-list"}
            )) {
                saveMenu(adminMenuId, "Main", item[0], item[1], roleId, createdBy);
            }

            logger.info("Default {} menus created for: {}", isBranch ? "Branch Admin" : "Bank Admin", bankCode);
        } catch (Exception e) {
            logger.error("Failed to create default menus for bank {}: {}", bankCode, e.getMessage());
        }
    }

    private ReconMenuMaster saveMenu(String parentMenuCode, String menuType, String menuName,
                                     String menuUrl, Long roleId, String createdBy) {
        ReconMenuMaster m = new ReconMenuMaster();
        m.setMenuType(menuType);
        m.setMenuName(menuName);
        m.setMenuUrl(menuUrl);
        m.setParentMenuCode(parentMenuCode);
        m.setSubMenu("N");
        m.setStatus("Y");
        m.setRoleId(roleId);
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        return menuMasterRepository.save(m);
    }
}



