package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.KalAdminDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.KalAdminPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;

@Service
public class KalAdminServiceImpl implements KalAdminService {

    private Logger logger = LoggerFactory.getLogger(KalAdminServiceImpl.class);

    @Autowired
    private KalAdminRepository KalAdminRepository;

    @Autowired
    private RoleManageRepository roleManageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ADMIN role ID — inserted in RCN_ROLE_MASTER
    private static final Long ADMIN_ROLE_ID = 5L;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> register(KalAdminDto dto) {
        RestWithStatusList restWithStatusList;

        // ── Validations ──────────────────────────────────────────

        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Username is required.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }
        if (!dto.getUsername().matches("^[a-z]+\\.[a-z]+$")) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Username must be in firstname.lastname format (all lowercase letters only).", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }
        if (dto.getEmail() == null || !dto.getEmail().endsWith("@kalinfotech.com")) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Only @kalinfotech.com email addresses are allowed.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }
        if (dto.getPhone() == null || !dto.getPhone().matches("^[0-9]{10}$")) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Phone number must be exactly 10 digits.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }
        if (dto.getPassword() == null ||
                !dto.getPassword().matches("^(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=.*[0-9]).{8,}$")) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        // ── Duplicate checks (RCN_RECON_USER) ────────────────────

        if (KalAdminRepository.existsByUserName(dto.getUsername().trim().toLowerCase())) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Username '" + dto.getUsername() + "' already exists.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.CONFLICT);
        }
        if (KalAdminRepository.existsByEmailId(dto.getEmail().trim().toLowerCase())) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Email '" + dto.getEmail() + "' is already registered.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.CONFLICT);
        }

        // ── Encode password ──────────────────────────────────────
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // ── Full name — derive from username if not given ────────
        String fullName;
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            fullName = dto.getFullName().trim();
        } else {
            String[] parts = dto.getUsername().split("\\.");
            StringBuilder name = new StringBuilder();
            for (String part : parts) {
                if (name.length() > 0) name.append(" ");
                name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            fullName = name.toString();
        }

        // ── Save to RCN_RECON_USER + RCN_PASSWORD_MANAGER ───────
        Role adminRole = roleManageRepository.findByRoleId(ADMIN_ROLE_ID);
        if (adminRole == null) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Admin role not configured. Please contact system administrator.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        KalAdmin KalAdmin = new KalAdmin();
        KalAdmin.setUserName(dto.getUsername().trim().toLowerCase());
        KalAdmin.setEmailId(dto.getEmail().trim().toLowerCase());
        KalAdmin.setMobileNumber(Long.parseLong(dto.getPhone().trim()));
        KalAdmin.setDesignation(dto.getDesignation() != null ? dto.getDesignation() : "Admin");
        KalAdmin.setBank("KalInfotech");
        KalAdmin.setType("Internal");
        KalAdmin.setUserStatus("ACTIVE");
        KalAdmin.setApprovedYn("Y");              // Admin — no approval needed
        KalAdmin.setRole(adminRole);
        KalAdmin.setCreatedAt(LocalDateTime.now());
        KalAdmin.setCreatedBy(dto.getUsername().trim().toLowerCase());

        KalAdminPasswordManager pwdManager = new KalAdminPasswordManager();
        pwdManager.setUserPassword(encodedPassword);
        pwdManager.setExpirationDate(LocalDateTime.now());
        pwdManager.setCreatedAt(LocalDateTime.now());
        pwdManager.setKalAdmin(KalAdmin);
        KalAdmin.setPasswordManager(pwdManager);

        KalAdminRepository.save(KalAdmin);
        KalAdminRepository.flush();
        logger.info("KalAdmin saved for login: {}", KalAdmin.getUserName());

        restWithStatusList = new RestWithStatusList("SUCCESS", "Employee Registered Successfully", new ArrayList<>());
        return new ResponseEntity<>(restWithStatusList, HttpStatus.CREATED);
    }
}
