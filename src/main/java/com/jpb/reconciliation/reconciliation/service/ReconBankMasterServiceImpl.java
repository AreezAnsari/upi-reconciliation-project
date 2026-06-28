package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconPasswordManagerRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
        if (bank.getStatus() == null) {
            bank.setStatus("ACTIVE");
        }
        ReconBankMaster saved = reconBankMasterRepository.save(bank);
        createDefaultBankAdminMenus(saved.getBankCode(), createdBy);

        // Auto-create Bank Admin user in RCN_RECON_USER and send welcome email
        String adminUserId = null;
        String defaultPassword = null;
        if (bank.getPrimaryEmail() != null && !bank.getPrimaryEmail().trim().isEmpty()) {
            try {
                defaultPassword = generatePassword(10);
                String username = deriveUsername(bank.getPrimaryEmail(), bank.getPrimaryFullName());

                ReconUser admin = new ReconUser();
                admin.setBankId(saved.getBankId());
                admin.setFullName(bank.getPrimaryFullName() != null ? bank.getPrimaryFullName() : username);
                admin.setEmail(bank.getPrimaryEmail().trim().toLowerCase());
                admin.setMobileNumber(bank.getPrimaryMobile());
                admin.setUsername(username);
                admin.setUserType("SUPER_USER");
                admin.setContactRank("PRIMARY");
                admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
                admin.setPasswordSet(0);
                admin.setStatus("ACTIVE_PENDING");
                admin.setApprovedYn("N");
                admin.setCreatedAt(LocalDateTime.now());
                admin.setCreatedBy(createdBy);

                Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findByRoleCode("BANK_ADMIN_" + saved.getBankCode());
                if (roleOpt.isPresent()) {
                    admin.setRoleId(roleOpt.get().getRoleId());
                }

                ReconUser savedAdmin = reconUserRepository.saveAndFlush(admin);
                adminUserId = savedAdmin.getUsername();

                ReconPasswordManager pwd = new ReconPasswordManager();
                pwd.setReconUser(savedAdmin);
                pwd.setUserPassword(savedAdmin.getPasswordHash());
                pwd.setCreatedAt(LocalDateTime.now());
                pwd.setCreatedBy(createdBy);
                pwd.setExpirationDate(LocalDateTime.now().plusDays(90));
                reconPasswordManagerRepository.save(pwd);

                String verifyLink = frontendUrl + "/bank-admin-login";
                emailService.sendBankAdminWelcome(
                        savedAdmin.getEmail(),
                        savedAdmin.getFullName(),
                        saved.getBankName(),
                        saved.getBankCode(),
                        adminUserId,
                        defaultPassword,
                        verifyLink
                );
                logger.info("Bank Admin user created and welcome email sent: {} for bank {}", adminUserId, saved.getBankCode());
            } catch (Exception e) {
                logger.error("Failed to create Bank Admin user for bank {}: {}", saved.getBankCode(), e.getMessage());
            }
        }

        logger.info("ReconBankMaster created: {} by {}", saved.getBankCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Bank created successfully.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        List<ReconBankMaster> banks = reconBankMasterRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Banks fetched.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with ID: " + bankId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankByCode(String bankCode) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findByBankCode(bankCode);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Bank not found with code: " + bankCode, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank found.", Collections.singletonList(opt.get())));
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

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        return sb.toString();
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

    private void createDefaultBankAdminMenus(String bankCode, String createdBy) {
        try {
            String roleCode = "BANK_ADMIN_" + bankCode;

            // Role already exists? Skip
            Optional<ReconRoleMaster> existingRole = reconRoleMasterRepository.findByRoleCode(roleCode);
            if (existingRole.isPresent()) return;

            // Create role for this bank's admin
            ReconRoleMaster role = new ReconRoleMaster();
            role.setRoleCode(roleCode);
            role.setRoleName("Bank Admin - " + bankCode);
            role.setRoleType("EXTERNAL");
            role.setRoleDesc("Default Bank Admin role for " + bankCode);
            role.setStatus("ACTIVE");
            role.setCreatedBy(createdBy);
            role.setCreatedAt(java.time.LocalDateTime.now());
            ReconRoleMaster savedRole = reconRoleMasterRepository.save(role);
            Long roleId = savedRole.getRoleId();

            // Master Menu 1: My Organization
            ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, createdBy);
            String myOrgId = String.valueOf(myOrg.getMenuId());

            for (String[] item : Arrays.asList(
                new String[]{"Dashboard",         "/bank-admin/my-organization/overview"},
                new String[]{"Branches",          "/bank-admin/my-organization/branches"},
                new String[]{"Branch Onboarding", "/bank-admin/branch-onboarding"},
                new String[]{"Hierarchy",         "/bank-admin/my-organization/hierarchy"},
                new String[]{"User Status",       "/bank-admin/my-organization/user-status"},
                new String[]{"Admin Status",      "/bank-admin/my-organization/admin-status"}
            )) {
                saveMenu(myOrgId, "Main", item[0], item[1], roleId, createdBy);
            }

            // Master Menu 2: Administration
            ReconMenuMaster admin = saveMenu(null, "Master", "Administration", null, roleId, createdBy);
            String adminId = String.valueOf(admin.getMenuId());

            for (String[] item : Arrays.asList(
                new String[]{"Add User",  "/bank-admin/add-user"},
                new String[]{"Add Role",  "/bank-admin/admin/add-new-role"},
                new String[]{"Add Menu",  "/bank-admin/add-menu"},
                new String[]{"User List", "/bank-admin/user-list"},
                new String[]{"Role List", "/bank-admin/role-list"},
                new String[]{"Menu List", "/bank-admin/menu-list"}
            )) {
                saveMenu(adminId, "Main", item[0], item[1], roleId, createdBy);
            }

            logger.info("Default Bank Admin menus created for bank: {}", bankCode);
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



