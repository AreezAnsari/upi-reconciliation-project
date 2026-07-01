package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.repository.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class KalAdminAuthServiceImpl implements KalAdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(KalAdminAuthServiceImpl.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconPasswordManagerRepository reconPasswordManagerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createKalAdmin(KalUserDto dto) {

        // ── Duplicate checks ──────────────────────────────────────────────────
        if (reconUserRepository.existsByUsername(dto.getUsername().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE",
                            "Username '" + dto.getUsername() + "' already exists.", null));
        }
        if (reconUserRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE",
                            "Email '" + dto.getEmail() + "' is already registered.", null));
        }

        // ── Derive full name from username (e.g. "john.doe" → "John Doe") ────
        String username = dto.getUsername().trim().toLowerCase();
        String[] parts  = username.split("\\.");
        StringBuilder nameBuilder = new StringBuilder();
        for (String part : parts) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String fullName = nameBuilder.toString();

        // ── BCrypt-encode the password ────────────────────────────────────────
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // ── 1. Save to RCN_RECON_USER ─────────────────────────────────────────
        ReconUser user = new ReconUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setMobileNumber(dto.getPhone().trim());
        user.setUserType("KAL_ADMIN");
        user.setDesignation(null);
        user.setDepartment(null);
        user.setStatus("ACTIVE");
        user.setApprovedYn("Y");
        user.setPasswordHash(encodedPassword);
        user.setPasswordSet(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setBankId(null);

        // ── Assign KAL_ADMIN role from RECON_ROLE_MASTER ─────────────────────
        // Menus are linked to this role via C_ROLE_MENU_MAP / RECON_MENU_MASTER.ROLE_ID
        Optional<ReconRoleMaster> kalAdminRole = reconRoleMasterRepository.findByRoleCode("KAL_ADMIN");
        kalAdminRole.ifPresent(role -> user.setRoleId(role.getRoleId()));
        if (!kalAdminRole.isPresent()) {
            logger.warn("KAL_ADMIN role not found in RECON_ROLE_MASTER — user created without role/menu assignment");
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("SYSTEM");

        ReconUser savedUser = reconUserRepository.save(user);
        logger.info("KalAdmin created — userId={}, username={}", savedUser.getUserId(), username);

        // ── 2. Save to RCN_RECON_PWD_MANAGER ─────────────────────────────────
        ReconPasswordManager pwdManager = new ReconPasswordManager();
        pwdManager.setReconUser(savedUser);
        pwdManager.setUserPassword(encodedPassword);
        pwdManager.setExpirationDate(LocalDateTime.now().plusDays(90));
        pwdManager.setCreatedAt(LocalDateTime.now());
        pwdManager.setCreatedBy("SYSTEM");

        reconPasswordManagerRepository.save(pwdManager);
        logger.info("Password record saved in RCN_RECON_PWD_MANAGER for userId={}", savedUser.getUserId());

        // ── 3. Save to AUDIT_LOG ──────────────────────────────────────────────
        AuditLog auditLog = new AuditLog();
        auditLog.setTableName("RCN_RECON_USER");
        auditLog.setRecordId(savedUser.getUserId());
        auditLog.setOperation("INSERT");
        auditLog.setActorUserId(savedUser.getUserId());
        auditLog.setActorUsername(username);
        auditLog.setActorType("KAL_ADMIN");
        auditLog.setEntityType("KAL_ADMIN");
        auditLog.setBankId(null);
        auditLog.setChangedAt(LocalDateTime.now());
        auditLog.setOldValue(null);
        auditLog.setNewValue("{\"username\":\"" + username + "\",\"email\":\"" + savedUser.getEmail()
                + "\",\"status\":\"ACTIVE\",\"userType\":\"KAL_ADMIN\"}");
        auditLog.setActionLabel("KalAdmin account created");
        auditLog.setRemarks("Self-registration via /api/v2/admin/auth/create");

        auditLogRepository.save(auditLog);
        logger.info("Audit log recorded for KalAdmin creation — userId={}", savedUser.getUserId());

        // RECON_AUTH_TOKEN is NOT inserted here — tokens are only created during login/OTP flows.

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "KalAdmin account created successfully.", new ArrayList<>()));
    }
}
