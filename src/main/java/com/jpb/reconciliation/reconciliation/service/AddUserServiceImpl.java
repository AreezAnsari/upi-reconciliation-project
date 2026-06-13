package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddUserServiceImpl implements AddUserService {

    private final AddUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final EmailService      emailService;

    /**
     * Base URL of your frontend app.
     * Set in application.properties:  app.frontend.base-url=https://yourapp.com
     *
     * The verify link will be:
     *   {baseUrl}/verify?instCode=ABC&username=john
     */
    // Login page URL — user visits this after getting email
    // Set in application.properties: app.frontend.base-url=http://localhost:5173
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // =========================================================================
    // CREATE USER
    //
    // Flow:
    //   1. Validate duplicates
    //   2. Generate defaultPassword (e.g. RAJE4823)
    //   3. toEntity() → status=REQUEST, passwordSet=0 (mapper sets these)
    //   4. Set password + defaultPassword on entity AFTER toEntity()
    //      (PasswordEncoder is a Spring bean — cannot be used in static mapper)
    //   5. Save to DB
    //   6. Send welcome email with 3 credentials:
    //      institutionCode + username + defaultPassword
    //
    // User becomes ACTIVE only after full verification:
    //   verifyCredentials → setPassword → login → verifyOTP → ACTIVE
    // =========================================================================

    @Override
    public RestWithStatusList createUser(AddUserRequest request,
                                         String createdBy,
                                         String instCode) {
        try {
            // ── 1. Duplicate checks ───────────────────────────────────────────
            if (userRepository.existsByFullName(request.getFullName())) {
                throw new RuntimeException("Full Name '" + request.getFullName() + "' already exists");
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username '" + request.getUsername() + "' already exists");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' already exists");
            }

            // ── 2. External field validation ──────────────────────────────────
            if ("EXTERNAL".equalsIgnoreCase(request.getUserType())) {
                validateExternalFields(request);
            }

            // ── 3. Generate default password ──────────────────────────────────
            // Format: First 4 chars of username (UPPERCASE) + random 4-digit number
            // Matches KalSuperUser pattern: e.g. username=rajesh.kumar → RAJE4823
            String defaultPassword = generateDefaultPassword(request.getUsername());

            // ── 4. Build entity (status=REQUEST, passwordSet=0 set in mapper) ─
            AddUser user = AddUserMapper.toEntity(request, createdBy, instCode);

            // ✅ Set password fields HERE — not in mapper (PasswordEncoder is a Spring bean)
            // defaultPassword → plain text stored temporarily, shown in email
            // password        → BCrypt encoded, used for login verification
            // passwordSet     → 0 means user has NOT yet set their own password
            user.setDefaultPassword(defaultPassword);
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setPasswordSet(0);

            // ── 5. Save to DB ─────────────────────────────────────────────────
            userRepository.save(user);

            log.info("User created → id={}, username={}, instCode={}, status={}",
                    user.getId(), user.getUsername(), instCode, user.getStatus());

            // ── 6. Send welcome email (3 credentials only) ───────────────────
            // Email contains: institutionCode + username + defaultPassword
            // User visits: frontendBaseUrl/user-verify?instCode=XXX&username=YYY
            String loginPageUrl = frontendBaseUrl + "/user-verify"
                    + "?instCode=" + instCode
                    + "&username=" + user.getUsername();

            try {
                emailService.sendUserWelcomeEmail(
                        user.getEmail(),
                        user.getFullName(),
                        instCode,
                        user.getUsername(),
                        defaultPassword,    // ← plain text for email only
                        loginPageUrl        // ← link to verification page
                );
                log.info("Welcome email sent → {}", user.getEmail());
            } catch (Exception emailEx) {
                // Email failure does NOT rollback user creation
                // Admin can see credentials on success screen
                log.error("Welcome email failed for {}: {}", user.getEmail(), emailEx.getMessage());
            }

            // ── 7. Return success response ────────────────────────────────────
            // defaultPassword included in response so admin success screen can display it
            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("User created successfully. A welcome email has been sent to " + user.getEmail() + ".")
                    .data(Arrays.asList(AddUserMapper.toResponse(user)))
                    .build();

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg(e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public RestWithStatusList getUserById(Long id) {
        AddUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User fetched successfully")
                .data(Arrays.asList(AddUserMapper.toResponse(user)))
                .build();
    }

    @Override
    public RestWithStatusList getAllUsers() {
        List<AddUserResponse> users = userRepository.findAll()
                .stream()
                .map(AddUserMapper::toResponse)
                .collect(Collectors.toList());
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

    @Override
    public RestWithStatusList getUsersByInstitution(String instCode) {
        List<AddUserResponse> users = userRepository.findAll()
                .stream()
                .filter(u -> instCode.equals(u.getInstitutionCode()))
                .map(AddUserMapper::toResponse)
                .collect(Collectors.toList());
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public RestWithStatusList updateUser(Long id, AddUserRequest request) {
        AddUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if ("EXTERNAL".equalsIgnoreCase(request.getUserType())) {
            validateExternalFields(request);
        }

        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());
        user.setMobileNumber(request.getMobileNumber());
        user.setRole(AddUser.Role.valueOf(request.getRole().toUpperCase()));
        user.setRoleType(AddUser.RoleType.valueOf(request.getRoleType().toUpperCase()));
        user.setUserType(AddUser.UserType.valueOf(request.getUserType().toUpperCase()));

        boolean isExternal = "EXTERNAL".equalsIgnoreCase(request.getUserType());
        user.setExternalDepartmentName(isExternal ? request.getExternalDepartmentName() : null);
        user.setExternalSupervisorName(isExternal ? request.getExternalSupervisorName() : null);
        user.setExternalSupervisorEmail(isExternal ? request.getExternalSupervisorEmail() : null);
        user.setExternalSupervisorPhone(isExternal ? request.getExternalSupervisorPhone() : null);

        userRepository.save(user);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User updated successfully")
                .data(Arrays.asList(AddUserMapper.toResponse(user)))
                .build();
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    @Override
    public RestWithStatusList deactivateUser(Long id) {
        AddUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setStatus(AddUser.UserStatus.INACTIVE);
        userRepository.save(user);
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User deactivated successfully")
                .data(Collections.emptyList())
                .build();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Override
    public RestWithStatusList searchUsers(String instCode, String term) {
        List<AddUserResponse> users = userRepository.searchUsers(instCode, term)
                .stream()
                .map(AddUserMapper::toResponse)
                .collect(Collectors.toList());
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Search completed")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Generates a default password.
     * Format: First 4 chars of username (uppercase) + 4-digit random number.
     * Example: username="johnDoe" → "JOHN4823"
     */
    private String generateDefaultPassword(String username) {
        String prefix = username.length() >= 4
                ? username.substring(0, 4).toUpperCase()
                : username.toUpperCase();
        int randomNum = 1000 + (int) (Math.random() * 9000); // 1000–9999
        return prefix + randomNum;
    }

    private void validateExternalFields(AddUserRequest req) {
        if (isBlank(req.getExternalDepartmentName()))
            throw new IllegalArgumentException("externalDepartmentName is required for EXTERNAL users");
        if (isBlank(req.getExternalSupervisorName()))
            throw new IllegalArgumentException("externalSupervisorName is required for EXTERNAL users");
        if (isBlank(req.getExternalSupervisorEmail()))
            throw new IllegalArgumentException("externalSupervisorEmail is required for EXTERNAL users");
        if (isBlank(req.getExternalSupervisorPhone()))
            throw new IllegalArgumentException("externalSupervisorPhone is required for EXTERNAL users");
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}