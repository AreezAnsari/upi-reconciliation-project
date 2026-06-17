package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddUserServiceImpl implements AddUserService {

    private final AddUserRepository    userRepository;
    private final AdminContextResolver contextResolver;
    private final PasswordEncoder      passwordEncoder;
    private final EmailService         emailService;

    @Override
    public RestWithStatusList createUser(AddUserRequest request, Authentication authentication) {
        try {
            AdminContext ctx = contextResolver.resolve(authentication);

            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username '" + request.getUsername() + "' already exists");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email '" + request.getEmail() + "' already exists");
            }
            if ("EXTERNAL".equalsIgnoreCase(request.getUserType())) {
                validateExternalFields(request);
            }

            String rawPassword = generateDefaultPassword();

            AddUser user = AddUserMapper.toEntity(
                    request,
                    ctx.getUsername(),
                    ctx.getBankCode(),
                    ctx.getBranchCode(),
                    passwordEncoder.encode(rawPassword) // BCrypt stored, same as MainBankServiceImpl
            );
            userRepository.save(user);

            log.info("User created → id={}, username={}, createdBy={}, bankCode={}, branchCode={}",
                    user.getId(), user.getUsername(), user.getCreatedBy(),
                    user.getBankCode(), user.getBranchCode());

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
                    .data(Collections.emptyList())
                    .build();
        }
    }

    @Override
    public RestWithStatusList getUsersByCreator(Authentication authentication) {
        AdminContext ctx = contextResolver.resolve(authentication);
        List<AddUserResponse> users = userRepository.findByCreatedBy(ctx.getUsername())
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
    public RestWithStatusList getUsersByBankCode(String bankCode) {
        if (bankCode == null || bankCode.trim().isEmpty()) {
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("bankCode is required")
                    .data(Collections.emptyList())
                    .build();
        }
        List<AddUserResponse> users = userRepository.findByBankCode(bankCode)
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
    public RestWithStatusList getUsersByBranchCode(String branchCode) {
        if (branchCode == null || branchCode.trim().isEmpty()) {
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("branchCode is required")
                    .data(Collections.emptyList())
                    .build();
        }
        List<AddUserResponse> users = userRepository.findByBranchCode(branchCode)
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

    @Override
    public RestWithStatusList searchByCreator(Authentication authentication, String term) {
        AdminContext ctx = contextResolver.resolve(authentication);
        List<AddUserResponse> users = userRepository.searchByCreator(ctx.getUsername(), term)
                .stream()
                .map(AddUserMapper::toResponse)
                .collect(Collectors.toList());
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Search completed")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

    // ── Schedule / Undo status transitions ───────────────────────────────────────

    @Override
    public RestWithStatusList scheduleInactivateUser(Long id, String scheduledBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.ACTIVE)
            return fail("User must be ACTIVE to schedule inactivation. Current: " + user.getStatus());

        user.setPreInactivateStatus(user.getStatus().name());
        user.setStatus(AddUser.UserStatus.INACTIVE_PENDING);
        user.setInactivateScheduledAt(LocalDateTime.now());
        user.setReactivateScheduledAt(null);
        user.setPreReactivateStatus(null);
        userRepository.save(user);

        String inactivateAt = user.getInactivateScheduledAt()
                .plusSeconds(30)  // DEMO: 30s — production: plusMinutes(30)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (user.getEmail() != null) {
                emailService.sendInactivatePendingWarning(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername(), inactivateAt);
            }
        } catch (Exception e) {
            log.warn("[INACTIVATE-WARN] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Inactivation scheduled. User will be INACTIVE in 30 seconds.");
    }

    @Override
    public RestWithStatusList undoInactivateUser(Long id, String undoneBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.INACTIVE_PENDING)
            return fail("No scheduled inactivation found for this user.");

        AddUser.UserStatus restored = user.getPreInactivateStatus() != null
                ? AddUser.UserStatus.valueOf(user.getPreInactivateStatus())
                : AddUser.UserStatus.ACTIVE;
        user.setStatus(restored);
        user.setInactivateScheduledAt(null);
        user.setPreInactivateStatus(null);
        userRepository.save(user);

        try {
            if (user.getEmail() != null) {
                emailService.sendInactivateCancelled(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-INACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Inactivation cancelled. User restored to " + restored + ".");
    }

    @Override
    public RestWithStatusList scheduleReactivateUser(Long id, String scheduledBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.INACTIVE)
            return fail("User must be INACTIVE to schedule reactivation. Current: " + user.getStatus());

        user.setPreReactivateStatus(user.getStatus().name());
        user.setStatus(AddUser.UserStatus.ACTIVE_PENDING);
        user.setReactivateScheduledAt(LocalDateTime.now());
        user.setInactivateScheduledAt(null);
        user.setPreInactivateStatus(null);
        userRepository.save(user);

        String reactivateAt = user.getReactivateScheduledAt()
                .plusSeconds(30)  // DEMO: 30s — production: plusHours(1)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (user.getEmail() != null) {
                emailService.sendReactivatePendingNotification(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername(), reactivateAt);
            }
        } catch (Exception e) {
            log.warn("[REACTIVATE-PEND] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Reactivation scheduled. User will be ACTIVE in 30 seconds.");
    }

    @Override
    public RestWithStatusList undoReactivateUser(Long id, String undoneBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.ACTIVE_PENDING)
            return fail("No scheduled reactivation found for this user.");

        AddUser.UserStatus restored = user.getPreReactivateStatus() != null
                ? AddUser.UserStatus.valueOf(user.getPreReactivateStatus())
                : AddUser.UserStatus.INACTIVE;
        user.setStatus(restored);
        user.setReactivateScheduledAt(null);
        user.setPreReactivateStatus(null);
        userRepository.save(user);

        try {
            if (user.getEmail() != null) {
                emailService.sendReactivateCancelled(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-REACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Reactivation cancelled. User restored to " + restored + ".");
    }

    @Override
    public RestWithStatusList scheduleBlockUser(Long id, String scheduledBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() == AddUser.UserStatus.BLOCK)
            return fail("User is already permanently BLOCKED.");
        if (user.getStatus() == AddUser.UserStatus.BLOCK_PENDING)
            return fail("Block is already scheduled for this user.");

        user.setPreBlockStatus(user.getStatus().name());
        user.setStatus(AddUser.UserStatus.BLOCK_PENDING);
        user.setBlockScheduledAt(LocalDateTime.now());
        user.setInactivateScheduledAt(null);
        user.setPreInactivateStatus(null);
        user.setReactivateScheduledAt(null);
        user.setPreReactivateStatus(null);
        userRepository.save(user);

        // window depends on prior status: ACTIVE→BLOCK=4hr/30s, INACTIVE→BLOCK=1hr/30s (same 30s demo)
        String blockAt = user.getBlockScheduledAt()
                .plusSeconds(30)  // DEMO: 30s — production: ACTIVE→plusHours(4), INACTIVE→plusHours(1)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (user.getEmail() != null) {
                emailService.sendInactivatePendingWarning(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername(), blockAt);
            }
        } catch (Exception e) {
            log.warn("[BLOCK-WARN] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Block scheduled. User will be permanently BLOCKED in 30 seconds.");
    }

    @Override
    public RestWithStatusList undoBlockUser(Long id, String undoneBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.BLOCK_PENDING)
            return fail("No scheduled block found for this user.");

        AddUser.UserStatus restored = user.getPreBlockStatus() != null
                ? AddUser.UserStatus.valueOf(user.getPreBlockStatus())
                : AddUser.UserStatus.INACTIVE;
        user.setStatus(restored);
        user.setBlockScheduledAt(null);
        user.setPreBlockStatus(null);
        userRepository.save(user);

        try {
            if (user.getEmail() != null) {
                emailService.sendInactivateCancelled(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-BLOCK] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Block cancelled. User restored to " + restored + ".");
    }

    private RestWithStatusList ok(String msg) {
        return RestWithStatusList.builder().status("SUCCESS").statusMsg(msg).data(Collections.emptyList()).build();
    }

    private RestWithStatusList fail(String msg) {
        return RestWithStatusList.builder().status("FAILURE").statusMsg(msg).data(Collections.emptyList()).build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    // Same logic as MainBankServiceImpl and BranchBankServiceImpl
    private String generateDefaultPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
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
