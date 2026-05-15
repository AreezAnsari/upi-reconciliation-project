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

import com.jpb.reconciliation.reconciliation.dto.KalEmployeeDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.KalEmployeeAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalEmployeePassword;
import com.jpb.reconciliation.reconciliation.entity.PasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.repository.KalEmployeeRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;

@Service
public class KalAuthServiceImpl implements KalAuthService {

    private Logger logger = LoggerFactory.getLogger(KalAuthServiceImpl.class);

    @Autowired
    private KalEmployeeRepository kalEmployeeRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private RoleManageRepository roleManageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ADMIN role ID — inserted in RCN_ROLE_MASTER
    private static final Long ADMIN_ROLE_ID = 5L;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> register(KalEmployeeDto dto) {
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

        // ── Duplicate checks ─────────────────────────────────────

        if (kalEmployeeRepository.existsByUsername(dto.getUsername().trim().toLowerCase())) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Username '" + dto.getUsername() + "' already exists.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.CONFLICT);
        }
        if (kalEmployeeRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
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

        // ── Step 1: Save to KAL_EMPLOYEE_ADMIN ──────────────────
        KalEmployeeAdmin employee = new KalEmployeeAdmin();
        employee.setUsername(dto.getUsername().trim().toLowerCase());
        employee.setEmail(dto.getEmail().trim().toLowerCase());
        employee.setMobile(dto.getPhone().trim());
        employee.setFullName(fullName);
        employee.setDesignation(dto.getDesignation() != null ? dto.getDesignation() : "Admin");
        employee.setStatus("ACTIVE");
        employee.setCreatedAt(LocalDateTime.now());

        KalEmployeePassword empPwd = new KalEmployeePassword();
        empPwd.setUserPassword(encodedPassword);
        empPwd.setExpirationDate(LocalDateTime.now());
        empPwd.setCreatedAt(LocalDateTime.now());
        empPwd.setKalEmployee(employee);
        employee.setKalEmployeePassword(empPwd);

        kalEmployeeRepository.save(employee);
        kalEmployeeRepository.flush();
        logger.info("KalEmployee saved: {}", employee.getUsername());

        // ── Step 2: Save to RCN_RECON_USER for login flow ───────
        Role adminRole = roleManageRepository.findByRoleId(ADMIN_ROLE_ID);
        if (adminRole == null) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Admin role not configured. Please contact system administrator.", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ReconUser reconUser = new ReconUser();
        reconUser.setUserName(dto.getUsername().trim().toLowerCase());
        reconUser.setEmailId(dto.getEmail().trim().toLowerCase());
        reconUser.setMobileNumber(Long.parseLong(dto.getPhone().trim()));
        reconUser.setDesignation(dto.getDesignation() != null ? dto.getDesignation() : "Admin");
        reconUser.setInstitution("KalInfotech");
        reconUser.setType("Internal");
        reconUser.setUserStatus("ACTIVE");
        reconUser.setApprovedYn("Y");              // Admin — no approval needed
        reconUser.setRole(adminRole);
        reconUser.setCreatedAt(LocalDateTime.now());
        reconUser.setCreatedBy("SYSTEM");

        PasswordManager pwdManager = new PasswordManager();
        pwdManager.setUserPassword(encodedPassword);
        pwdManager.setExpirationDate(LocalDateTime.now());
        pwdManager.setCreatedAt(LocalDateTime.now());
        pwdManager.setReconUser(reconUser);
        reconUser.setPasswordManager(pwdManager);

        reconUserRepository.save(reconUser);
        reconUserRepository.flush();
        logger.info("ReconUser saved for login: {}", reconUser.getUserName());

        restWithStatusList = new RestWithStatusList("SUCCESS", "Employee Registered Successfully", new ArrayList<>());
        return new ResponseEntity<>(restWithStatusList, HttpStatus.CREATED);
    }
}