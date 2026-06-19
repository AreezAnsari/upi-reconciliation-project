package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final AddUserRepository        userRepository;
    private final AdminContextResolver     contextResolver;
    private final PasswordEncoder          passwordEncoder;
    private final EmailService             emailService;
    private final BranchBankRepository     branchBankRepository;
    private final BranchAdminRepository    branchAdminRepository;
    private final AdminReplacementRepository replacementRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

            // Bank Admin → bankCode only; Branch Admin → bankCode (parent) + branchCode (both)
            String storedBankCode   = ctx.getBankCode();
            String storedBranchCode = ctx.getBranchCode();

            AddUser user = AddUserMapper.toEntity(
                    request,
                    ctx.getUsername(),
                    storedBankCode,
                    storedBranchCode,
                    passwordEncoder.encode(rawPassword) // BCrypt stored, same as MainBankServiceImpl
            );
            userRepository.save(user);

            log.info("User created → id={}, username={}, createdBy={}, bankCode={}, branchCode={}",
                    user.getId(), user.getUsername(), user.getCreatedBy(),
                    user.getBankCode(), user.getBranchCode());

            // Send welcome email with credentials (same flow as admin onboarding)
            // Bank user  : ?bankCode=BANK_CODE
            // Branch user: ?bankCode=BANK_CODE&branchCode=BRANCH_CODE
            String verifyLink = user.getBranchCode() != null
                    ? frontendUrl + "/user-verify?bankCode=" + user.getBankCode()
                            + "&branchCode=" + user.getBranchCode()
                            + "&username=" + user.getUsername() + "&mode=verify"
                    : frontendUrl + "/user-verify?bankCode=" + user.getBankCode()
                            + "&username=" + user.getUsername() + "&mode=verify";
            // Code shown in email: Branch Code for branch user, Bank Code for bank user
            boolean isBranchUser = user.getBranchCode() != null;
            String displayCode  = isBranchUser ? user.getBranchCode() : user.getBankCode();
            String codeLabel    = isBranchUser ? "Branch Code" : "Bank Code";
            emailService.sendUserWelcome(
                    user.getEmail(),
                    user.getFullName(),
                    displayCode,
                    codeLabel,
                    user.getUsername(),
                    rawPassword,
                    verifyLink
            );

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
        List<AddUser> rawUsers;
        if (ctx.getBranchCode() != null) {
            rawUsers = userRepository.findByBranchCode(ctx.getBranchCode());
        } else if (ctx.getBankCode() != null) {
            rawUsers = userRepository.findByBankCodeAndBranchCodeIsNull(ctx.getBankCode());
        } else {
            rawUsers = userRepository.findByCreatedBy(ctx.getUsername());
        }
        List<AddUserResponse> users = rawUsers.stream()
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

        // Hierarchy hiding: if BranchAdmin has an active replacement, hide users
        try {
            branchBankRepository.findByBranchCode(branchCode).ifPresent(branch -> {
                if (branch.getBranchAdminId() != null) {
                    branchAdminRepository.findByBranchCodeAndUsername(
                            branchCode, branch.getBranchAdminId())
                        .ifPresent(ba -> {
                            java.util.List<AdminReplacement> recs = replacementRepository
                                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                            ba.getId(), "BRANCH_ADMIN",
                                            Arrays.asList("ACTIVE", "PERMANENT"));
                            if (!recs.isEmpty()) {
                                throw new HierarchyHiddenException();
                            }
                        });
                }
            });
        } catch (HierarchyHiddenException e) {
            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Users fetched successfully")
                    .data(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.warn("getUsersByBranchCode: hierarchy-hide check failed for {}: {}", branchCode, e.getMessage());
        }

        // Fetch users and enrich with replacement info
        List<AddUser> rawUsers = userRepository.findByBranchCode(branchCode);
        List<AddUserResponse> users = new java.util.ArrayList<>();
        for (AddUser u : rawUsers) {
            AddUserResponse resp = AddUserMapper.toResponse(u);
            java.util.List<AdminReplacement> recs = replacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                            u.getId(), "USER", Arrays.asList("ACTIVE", "PERMANENT"));
            if (!recs.isEmpty()) {
                AdminReplacement rec = recs.get(0);
                resp.setReplacementStatus(rec.getStatus());
                userRepository.findById(rec.getReplacementEntityId())
                        .ifPresent(rep -> resp.setReplacedByUsername(rep.getUsername()));
            }
            users.add(resp);
        }

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

    private static class HierarchyHiddenException extends RuntimeException {
        HierarchyHiddenException() { super("hierarchy-hidden"); }
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
        List<AddUser> rawUsers;
        if (ctx.getBranchCode() != null) {
            rawUsers = userRepository.findByBranchCode(ctx.getBranchCode());
        } else if (ctx.getBankCode() != null) {
            rawUsers = userRepository.findByBankCodeAndBranchCodeIsNull(ctx.getBankCode());
        } else {
            rawUsers = userRepository.findByCreatedBy(ctx.getUsername());
        }
        String lowerTerm = term != null ? term.toLowerCase() : "";
        List<AddUserResponse> users = rawUsers.stream()
                .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerTerm))
                          || (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerTerm)))
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

        user.setStatus(AddUser.UserStatus.INACTIVE_PENDING);
        user.setInactivateScheduledAt(LocalDateTime.now());
        user.setInactivateScheduledBy(scheduledBy);
        user.setReactivateScheduledAt(null);
        user.setReactivateScheduledBy(null);
        userRepository.save(user);
        BlockScheduleServiceImpl.flagPendingWork();

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

        user.setStatus(AddUser.UserStatus.ACTIVE);
        user.setInactivateScheduledAt(null);
        user.setInactivateScheduledBy(null);
        userRepository.save(user);

        // Cancel any pending replacement since inactivation was undone
        try {
            Optional<AdminReplacement> pendingRep = replacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(user.getId(), "USER", "PENDING");
            if (pendingRep.isPresent()) {
                pendingRep.get().setStatus("CANCELLED");
                replacementRepository.save(pendingRep.get());
                log.info("[UNDO-INACTIVATE] Cancelled PENDING replacement for user {} due to undo", user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-INACTIVATE] Failed to cancel pending replacement for {}: {}", user.getUsername(), e.getMessage());
        }

        try {
            if (user.getEmail() != null) {
                emailService.sendInactivateCancelled(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-INACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Inactivation cancelled. User restored to ACTIVE.");
    }

    @Override
    public RestWithStatusList scheduleReactivateUser(Long id, String scheduledBy) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() != AddUser.UserStatus.INACTIVE)
            return fail("User must be INACTIVE to schedule reactivation. Current: " + user.getStatus());

        user.setStatus(AddUser.UserStatus.ACTIVE_PENDING);
        user.setReactivateScheduledAt(LocalDateTime.now());
        user.setReactivateScheduledBy(scheduledBy);
        user.setInactivateScheduledAt(null);
        user.setInactivateScheduledBy(null);
        userRepository.save(user);
        BlockScheduleServiceImpl.flagPendingWork();

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

        user.setStatus(AddUser.UserStatus.INACTIVE);
        user.setReactivateScheduledAt(null);
        user.setReactivateScheduledBy(null);
        userRepository.save(user);

        try {
            if (user.getEmail() != null) {
                emailService.sendReactivateCancelled(user.getEmail(),
                        user.getFullName(), user.getUsername(), user.getUsername());
            }
        } catch (Exception e) {
            log.warn("[UNDO-REACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        return ok("Reactivation cancelled. User restored to INACTIVE.");
    }

    @Override
    public RestWithStatusList scheduleBlockUser(Long id, String scheduledBy, String reason) {
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
        user.setBlockScheduledBy(scheduledBy);
        user.setBlockReason(reason);
        user.setInactivateScheduledAt(null);
        user.setInactivateScheduledBy(null);
        user.setReactivateScheduledAt(null);
        user.setReactivateScheduledBy(null);
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
