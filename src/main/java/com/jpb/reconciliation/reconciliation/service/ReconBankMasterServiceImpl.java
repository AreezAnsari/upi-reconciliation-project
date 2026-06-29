package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.CBankProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProductMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createBank(ReconBankMaster bank, String createdBy) {
        if (bank.getBankCode() == null || bank.getBankCode().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank code is required.", null));
        }
        if (reconBankMasterRepository.existsByBankCode(bank.getBankCode().trim().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Bank code already exists.", null));
        }
        if (reconBankMasterRepository.existsByBankName(bank.getBankName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Bank name already exists.", null));
        }
        bank.setBankCode(bank.getBankCode().trim().toUpperCase());
        bank.setCreatedAt(LocalDateTime.now());
        bank.setCreatedBy(createdBy);
        if (bank.getStatus() == null) bank.setStatus("REQUEST");

        // Determine admin userType based on parentBankId
        boolean isBranch = bank.getParentBankId() != null;
        String adminUserType = isBranch ? "BRANCH_ADMIN" : "BANK_ADMIN";
        String roleName = isBranch ? "BRANCH_ADMIN_" : "BANK_ADMIN_";

        ReconBankMaster saved = reconBankMasterRepository.save(bank);
        saveProductMappings(saved.getBankId(), bank, createdBy);
        String roleCode = roleName + saved.getBankCode();
        createDefaultAdminMenus(saved.getBankCode(), roleCode, isBranch, createdBy);

        // Audit: bank/branch created
        saveAuditLog("RECON_BANK_MASTER", saved.getBankId(), "CREATE", null, null,
                createdBy, adminUserType, saved.getBankId(),
                "Bank " + (isBranch ? "(Branch) " : "") + saved.getBankCode() + " onboarded");

        if (bank.getPrimaryEmail() != null && !bank.getPrimaryEmail().trim().isEmpty()) {
            try {
                String defaultPwd = generatePassword(10);
                String username = deriveUsername(bank.getPrimaryEmail(), bank.getPrimaryFullName());

                // PRIMARY user — starts ACTIVE_PENDING, goes through 3-step login
                ReconUser primary = buildAdminUser(
                        saved.getBankId(), bank.getPrimaryFullName(), bank.getPrimaryEmail(),
                        bank.getPrimaryMobile(), username, adminUserType, "PRIMARY",
                        defaultPwd, "REQUEST", createdBy);

                Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findByRoleCode(roleCode);
                roleOpt.ifPresent(r -> primary.setRoleId(r.getRoleId()));

                ReconUser savedPrimary = reconUserRepository.saveAndFlush(primary);
                savePasswordHistory(savedPrimary, createdBy);

                // Store username + defaultPassword (plaintext) in bank record for 3-step verify
                saved.setBankAdminUsername(username);
                saved.setDefaultPassword(defaultPwd);
                reconBankMasterRepository.save(saved);

                // Audit: primary admin created
                saveAuditLog("RCN_RECON_USER", savedPrimary.getUserId(), "CREATE", null,
                        "userType=" + adminUserType + ",contactRank=PRIMARY,status=REQUEST",
                        createdBy, adminUserType, saved.getBankId(),
                        "Primary " + adminUserType + " created: " + username);

                // Welcome email — link goes to verify-email page (which calls backend then redirects to login with mode=verify)
                String verifyPage = isBranch ? "/branch-verify-email" : "/verify-email";
                String verifyLink = frontendUrl + verifyPage
                        + "?bankCode=" + saved.getBankCode()
                        + "&username=" + username;
                emailService.sendBankAdminWelcome(
                        savedPrimary.getEmail(), savedPrimary.getFullName(),
                        saved.getBankName(), saved.getBankCode(),
                        username, defaultPwd, verifyLink);

                // SECONDARY user — always INACTIVE, future use
                if (bank.getSecondaryEmail() != null && !bank.getSecondaryEmail().trim().isEmpty()) {
                    String secUsername = deriveUsername(bank.getSecondaryEmail(), bank.getSecondaryFullName());
                    String secDefaultPwd = generatePassword(10);

                    ReconUser secondary = buildAdminUser(
                            saved.getBankId(), bank.getSecondaryFullName(), bank.getSecondaryEmail(),
                            bank.getSecondaryMobile(), secUsername, adminUserType, "SECONDARY",
                            secDefaultPwd, "INACTIVE", createdBy);
                    roleOpt.ifPresent(r -> secondary.setRoleId(r.getRoleId()));

                    ReconUser savedSecondary = reconUserRepository.saveAndFlush(secondary);
                    savePasswordHistory(savedSecondary, createdBy);

                    saveAuditLog("RCN_RECON_USER", savedSecondary.getUserId(), "CREATE", null,
                            "userType=" + adminUserType + ",contactRank=SECONDARY,status=INACTIVE",
                            createdBy, adminUserType, saved.getBankId(),
                            "Secondary " + adminUserType + " created (INACTIVE): " + secUsername);

                    logger.info("Secondary {} created (INACTIVE): {} for bank {}", adminUserType, secUsername, saved.getBankCode());
                }

                logger.info("Primary {} created and welcome email sent: {} for bank {}", adminUserType, username, saved.getBankCode());
            } catch (Exception e) {
                logger.error("Failed to create admin users for bank {}: {}", saved.getBankCode(), e.getMessage(), e);
            }
        }

        logger.info("ReconBankMaster created: {} by {}", saved.getBankCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS",
                        (isBranch ? "Branch" : "Bank") + " created successfully.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        List<ReconBankMaster> banks = reconBankMasterRepository.findAll();
        banks.forEach(this::enrichWithProducts);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Banks fetched.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
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
        logger.info("ReconBankMaster updated: {} by {}", bankId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster status updated to {} for ID: {} by {}", status, bankId, updatedBy);
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
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        reconBankMasterRepository.save(existing);
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
        existing.setPreBlockStatus(existing.getStatus());
        existing.setStatus("BLOCKED");
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster BLOCKED: bankId={} preBlockStatus={} by {}", bankId, existing.getPreBlockStatus(), updatedBy);
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
        logger.info("ReconBankMaster UNBLOCKED: bankId={} restored to {} by {}", bankId, restoreStatus, updatedBy);
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
        existing.setInactivateScheduledAt(scheduledAt);
        existing.setInactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster INACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);
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
        existing.setReactivateScheduledAt(scheduledAt);
        existing.setReactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster REACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);
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
        existing.setBlockScheduledAt(scheduledAt);
        existing.setBlockScheduledBy(scheduledBy);
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster BLOCK scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);
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
        reconBankMasterRepository.save(existing);
        logger.info("ReconBankMaster {} schedule cancelled for bankId={} by {}", scheduleType, bankId, updatedBy);
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
        int digits = 1000 + RNG.nextInt(9000); // 4-digit number: 1000–9999
        return "Recon@" + digits;
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

            // Master Menu 1: My Organization
            ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, createdBy);
            String myOrgId = String.valueOf(myOrg.getMenuId());

            String prefix = isBranch ? "/branch-admin" : "/bank-admin";
            for (String[] item : Arrays.asList(
                new String[]{"Dashboard",         prefix + "/my-organization/overview"},
                new String[]{"Branches",          prefix + "/my-organization/branches"},
                new String[]{"Branch Onboarding", prefix + "/branch-onboarding"},
                new String[]{"Hierarchy",         prefix + "/my-organization/hierarchy"},
                new String[]{"User Status",       prefix + "/my-organization/user-status"},
                new String[]{"Admin Status",      prefix + "/my-organization/admin-status"}
            )) {
                saveMenu(myOrgId, "Main", item[0], item[1], roleId, createdBy);
            }

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



