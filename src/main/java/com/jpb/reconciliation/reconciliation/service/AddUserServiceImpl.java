package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddUserServiceImpl implements AddUserService {

    private final AddUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public RestWithStatusList createUser(AddUserRequest request,
                                         String createdBy,
                                         String instCode) {
        try {
            // 1. Derive username from fullName if not provided by frontend
            if (userRepository.existsByUsername(request.getFullName())) {
            	throw new RuntimeException("Full Name '" + request.getFullName() + "' already exists");
                
            }

            // 2. Duplicate checks
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username '" + request.getUsername() + "' already exists");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' already exists");
            }

            // 3. External field validation
            if ("EXTERNAL".equalsIgnoreCase(request.getUserType())) {
                validateExternalFields(request);
            }

            // 4. Build and save entity
            AddUser user = AddUserMapper.toEntity(request, createdBy, instCode);
            userRepository.save(user);

            log.info("User created → id={}, username={}, role={}, type={}",
                    user.getId(), user.getUsername(), user.getRole(), user.getUserType());

            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("User created successfully")
                    .data(Arrays.asList(AddUserMapper.toResponse(user)))
                    .build();

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg(e.getMessage())
                    .data(java.util.Collections.emptyList())
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
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());
        user.setMobileNumber(request.getMobileNumber());
        user.setRole(AddUser.Role.valueOf(request.getRole().toUpperCase()));
        user.setUserType(AddUser.UserType.valueOf(request.getUserType().toUpperCase()));
        // External fields
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
                .data(java.util.Collections.emptyList())
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

    // ── Private helpers ───────────────────────────────────────────────────────

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