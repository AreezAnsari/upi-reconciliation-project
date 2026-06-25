package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.entity.UserDelegation;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.UserDelegationRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddUserServiceImpl implements AddUserService {

    private final AddUserRepository          userRepository;
    private final AdminContextResolver       contextResolver;
    private final PasswordEncoder            passwordEncoder;
    private final EmailService               emailService;
    private final BranchBankRepository       branchBankRepository;
    private final MainBankRepository         mainBankRepository;
    private final BranchAdminRepository      branchAdminRepository;
    private final MainAdminRepository        mainAdminRepository;
    private final KalAdminRepository         kalAdminRepository;
    private final AdminReplacementRepository replacementRepository;
    private final UserDelegationRepository   delegationRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public RestWithStatusList createUser(AddUserRequest request, Authentication authentication) {
        try {
            AdminContext ctx = contextResolver.resolve(authentication);

            // Skip BLOCKED users — a BLOCKED user's email is treated as free for re-onboarding
            if (userRepository.findFirstByEmailAndStatusNot(request.getEmail(), AddUser.UserStatus.BLOCKED).isPresent()) {
                throw new RuntimeException("Email '" + request.getEmail() + "' already exists");
            }
            if ("EXTERNAL".equalsIgnoreCase(request.getUserType())) {
                validateExternalFields(request);
            }

            String rawPassword = generateDefaultPassword();

            // Bank Admin / Bank User  → bankCode only (branchCode is null in their context)
            // Branch Admin / Branch User → bankCode (parent) + branchCode (both)
            String storedBankCode   = ctx.getBankCode();
            String storedBranchCode = ctx.getBranchCode(); // null for bank-side actors, non-null for branch-side actors

            AddUser user = AddUserMapper.toEntity(
                    request,
                    ctx.getUsername(),
                    storedBankCode,
                    storedBranchCode,
                    passwordEncoder.encode(rawPassword) // BCrypt stored, same as MainBankServiceImpl
            );
            // Set parentId when creator is a USER (not a Bank/Branch Admin)
            if (ctx.getCreatorUserId() != null) {
                user.setParentId(ctx.getCreatorUserId());
            }
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
                            + "&username=" + user.getUsername()
                            + "&email=" + user.getEmail() + "&mode=verify"
                    : frontendUrl + "/user-verify?bankCode=" + user.getBankCode()
                            + "&username=" + user.getUsername()
                            + "&email=" + user.getEmail() + "&mode=verify";
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

        // If creatorUserId is set → USER logged in → show ONLY their children (parentId = their id)
        if (ctx.getCreatorUserId() != null) {
            log.info("[FETCH-USERS] User {} (ID: {}) fetching children. parentId lookup",
                    ctx.getUsername(), ctx.getCreatorUserId());
            rawUsers = userRepository.findByParentId(ctx.getCreatorUserId());
            log.info("[FETCH-USERS] Found {} children for user ID {}", rawUsers.size(), ctx.getCreatorUserId());
        }
        // Branch Admin → first-level users under this branch (parentId IS NULL)
        else if (ctx.getBranchCode() != null) {
            log.info("[FETCH-USERS] Branch Admin {} fetching first-level branch users (parentId=null)", ctx.getUsername());
            rawUsers = userRepository.findByBranchCodeAndParentIdIsNull(ctx.getBranchCode());
            log.info("[FETCH-USERS] Found {} first-level branch users", rawUsers.size());
        }
        // Bank Admin → first-level bank users without a branch (parentId IS NULL)
        else if (ctx.getBankCode() != null) {
            log.info("[FETCH-USERS] Bank Admin {} fetching first-level bank users (parentId=null)", ctx.getUsername());
            rawUsers = userRepository.findByBankCodeAndBranchCodeIsNullAndParentIdIsNull(ctx.getBankCode());
            log.info("[FETCH-USERS] Found {} first-level bank users", rawUsers.size());
        }
        // Fallback: Kal Admin or other
        else {
            log.info("[FETCH-USERS] Fallback: fetching users created by {}", ctx.getUsername());
            rawUsers = userRepository.findByCreatedBy(ctx.getUsername());
            log.info("[FETCH-USERS] Found {} users", rawUsers.size());
        }

        List<AddUserResponse> users = new ArrayList<>();
        for (AddUser u : rawUsers) {
            if (u.getStatus() == AddUser.UserStatus.INACTIVE || u.getStatus() == AddUser.UserStatus.REQUEST) {
                boolean isRestored = replacementRepository
                        .existsByReplacementEntityIdAndEntityTypeAndStatus(u.getId(), "USER", "RESTORED");
                if (isRestored) continue;
            }
            AddUserResponse resp = AddUserMapper.toResponse(u);
            enrichReplacement(resp, u.getId());
            users.add(resp);
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new ArrayList<>(users))
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
    public RestWithStatusList getUsersByCreatorUsername(String creatorUsername) {
        if (creatorUsername == null || creatorUsername.trim().isEmpty()) {
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("creatorUsername is required")
                    .data(Collections.emptyList())
                    .build();
        }
        List<AddUser> rawUsers = userRepository.findByCreatedBy(creatorUsername);
        List<AddUserResponse> users = new ArrayList<>();
        for (AddUser u : rawUsers) {
            if (u.getStatus() == AddUser.UserStatus.INACTIVE || u.getStatus() == AddUser.UserStatus.REQUEST) {
                boolean isRestored = replacementRepository
                        .existsByReplacementEntityIdAndEntityTypeAndStatus(u.getId(), "USER", "RESTORED");
                if (isRestored) continue;
            }
            AddUserResponse resp = AddUserMapper.toResponse(u);
            enrichReplacement(resp, u.getId());
            users.add(resp);
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new ArrayList<>(users))
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
        List<AddUser> rawUsers = userRepository.findByBankCodeAndParentIdIsNull(bankCode);
        List<AddUserResponse> users = new java.util.ArrayList<>();
        for (AddUser u : rawUsers) {
            if (u.getStatus() == AddUser.UserStatus.INACTIVE || u.getStatus() == AddUser.UserStatus.REQUEST) {
                boolean isRestored = replacementRepository
                        .existsByReplacementEntityIdAndEntityTypeAndStatus(u.getId(), "USER", "RESTORED");
                if (isRestored) continue;
            }
            AddUserResponse resp = AddUserMapper.toResponse(u);
            enrichReplacement(resp, u.getId());
            users.add(resp);
        }
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

        // Fetch first-level users only (parentId IS NULL = not created by another user)
        List<AddUser> rawUsers = userRepository.findByBranchCodeAndParentIdIsNull(branchCode);
        List<AddUserResponse> users = new java.util.ArrayList<>();
        for (AddUser u : rawUsers) {
            // Skip INACTIVE users who were replacement users whose tenure has ended (RESTORED)
            if (u.getStatus() == AddUser.UserStatus.INACTIVE || u.getStatus() == AddUser.UserStatus.REQUEST) {
                boolean isRestoredReplacement = replacementRepository
                        .existsByReplacementEntityIdAndEntityTypeAndStatus(u.getId(), "USER", "RESTORED");
                if (isRestoredReplacement) continue;
            }

            AddUserResponse resp = AddUserMapper.toResponse(u);
            enrichReplacement(resp, u.getId());
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
        user.setUsername(request.getUsername().trim().toLowerCase());
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
        if (ctx.getCreatorUserId() != null) {
            rawUsers = userRepository.findByParentId(ctx.getCreatorUserId());
        } else if (ctx.getBranchCode() != null) {
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
                String[] org = resolveOrgInfo(user);
                emailService.sendInactivatePendingWarning(user.getEmail(),
                        user.getFullName(), org[0], org[1], inactivateAt);
            }
        } catch (Exception e) {
            log.warn("[INACTIVATE-WARN] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(scheduledBy, "Inactivation Scheduled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), inactivateAt);

        // Notify parent if this user was created by another user
        notifyParent(user, "Child User Inactivation Scheduled", inactivateAt);

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
                String[] org = resolveOrgInfo(user);
                emailService.sendInactivateCancelled(user.getEmail(),
                        user.getFullName(), org[0], org[1]);
            }
        } catch (Exception e) {
            log.warn("[UNDO-INACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(undoneBy, "Inactivation Cancelled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        // Notify parent
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        notifyParent(user, "Child User Inactivation Cancelled", nowStr);

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
                String[] org = resolveOrgInfo(user);
                emailService.sendReactivatePendingNotification(user.getEmail(),
                        user.getFullName(), org[0], org[1], reactivateAt);
            }
        } catch (Exception e) {
            log.warn("[REACTIVATE-PEND] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(scheduledBy, "Reactivation Scheduled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), reactivateAt);

        // Notify parent
        notifyParent(user, "Child User Reactivation Scheduled", reactivateAt);

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
                String[] org = resolveOrgInfo(user);
                emailService.sendReactivateCancelled(user.getEmail(),
                        user.getFullName(), org[0], org[1]);
            }
        } catch (Exception e) {
            log.warn("[UNDO-REACTIVATE] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(undoneBy, "Reactivation Cancelled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        // Notify parent
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        notifyParent(user, "Child User Reactivation Cancelled", nowStr);

        return ok("Reactivation cancelled. User restored to INACTIVE.");
    }

    @Override
    public RestWithStatusList scheduleBlockUser(Long id, String scheduledBy, String reason) {
        Optional<AddUser> opt = userRepository.findById(id);
        if (!opt.isPresent()) return fail("User not found: " + id);
        AddUser user = opt.get();
        if (user.getStatus() == AddUser.UserStatus.BLOCKED)
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
        BlockScheduleServiceImpl.flagPendingWork();

        // Chain block all descendants
        cascadeBlockUser(user.getId(), scheduledBy, reason);

        // window depends on prior status: ACTIVE→BLOCK=4hr/30s, INACTIVE→BLOCK=1hr/30s (same 30s demo)
        String blockAt = user.getBlockScheduledAt()
                .plusSeconds(30)  // DEMO: 30s — production: ACTIVE→plusHours(4), INACTIVE→plusHours(1)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (user.getEmail() != null) {
                String[] org = resolveOrgInfo(user);
                emailService.sendBlockWarning(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        org[0], org[1], blockAt);
            }
        } catch (Exception e) {
            log.warn("[BLOCK-WARN] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(scheduledBy, "Block Scheduled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), blockAt);

        // Notify parent
        notifyParent(user, "Child User Block Scheduled", blockAt);

        return ok("Block scheduled. User and all descendants will be permanently BLOCKED in 30 seconds.");
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
                String[] org = resolveOrgInfo(user);
                emailService.sendBlockCancelled(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        org[0], org[1], restored.name());
            }
        } catch (Exception e) {
            log.warn("[UNDO-BLOCK] Email failed for user {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(undoneBy, "Block Cancelled",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        // Notify parent
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        notifyParent(user, "Child User Block Cancelled", nowStr);

        return ok("Block cancelled. User restored to " + restored + ".");
    }

    private void cascadeBlockUser(Long userId, String scheduledBy, String reason) {
        Queue<Long> queue = new ArrayDeque<>();
        queue.offer(userId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<AddUser> children = userRepository.findByParentId(currentId);
            for (AddUser child : children) {
                if (child.getStatus() != AddUser.UserStatus.BLOCKED && child.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
                    child.setPreBlockStatus(child.getStatus().name());
                    child.setStatus(AddUser.UserStatus.BLOCK_PENDING);
                    child.setBlockScheduledAt(LocalDateTime.now());
                    child.setBlockScheduledBy("CASCADE:" + scheduledBy);
                    child.setBlockReason(reason != null ? reason : "Cascaded from parent block");
                    child.setInactivateScheduledAt(null);
                    child.setInactivateScheduledBy(null);
                    child.setReactivateScheduledAt(null);
                    child.setReactivateScheduledBy(null);
                    userRepository.save(child);
                    queue.offer(child.getId());
                    log.info("[CASCADE-BLOCK] User {} set to BLOCK_PENDING due to parent block", child.getUsername());
                }
            }
        }
    }

    private void notifyParent(AddUser user, String action, String when) {
        if (user.getParentId() == null) return;
        try {
            Optional<AddUser> parentOpt = userRepository.findById(user.getParentId());
            if (!parentOpt.isPresent()) return;
            AddUser parent = parentOpt.get();
            if (parent.getEmail() == null || parent.getEmail().isEmpty()) return;
            // Use parent's email directly (no replacement checking for users like for admins)
            emailService.sendActorActionConfirmation(parent.getEmail(), parent.getUsername(),
                    action, user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    user.getUsername(), when);
        } catch (Exception e) {
            log.warn("[PARENT-NOTIFY] Failed to notify parent of user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private void notifyActor(String actorBy, String action, String targetName, String targetCode, String when) {
        try {
            Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> ma = mainAdminRepository.findFirstByUsername(actorBy);
            if (ma.isPresent() && ma.get().getEmail() != null && !ma.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ma.get().getEmail(), ma.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> ba = branchAdminRepository.findFirstByUsername(actorBy);
            if (ba.isPresent() && ba.get().getEmail() != null && !ba.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ba.get().getEmail(), ba.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            kalAdminRepository.findByUserName(actorBy).ifPresent(ka -> {
                if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                    emailService.sendActorActionConfirmation(ka.getEmailId(), ka.getUserName(), action, targetName, targetCode, when);
                }
            });
        } catch (Exception e) {
            log.warn("[ACTOR-CONFIRM] Email failed for {}: {}", actorBy, e.getMessage());
        }
    }

    private RestWithStatusList ok(String msg) {
        return RestWithStatusList.builder().status("SUCCESS").statusMsg(msg).data(Collections.emptyList()).build();
    }

    private RestWithStatusList fail(String msg) {
        return RestWithStatusList.builder().status("FAILURE").statusMsg(msg).data(Collections.emptyList()).build();
    }

    // Returns [orgName, entityCode] for email templates — uses bank/branch full name when available
    private String[] resolveOrgInfo(AddUser user) {
        String entityCode = user.getBranchCode() != null ? user.getBranchCode() : user.getBankCode();
        String orgName = entityCode;
        try {
            if (user.getBranchCode() != null) {
                Optional<BranchBank> br = branchBankRepository.findByBranchCode(user.getBranchCode());
                if (br.isPresent() && br.get().getBranchNameFull() != null) orgName = br.get().getBranchNameFull();
            } else if (user.getBankCode() != null) {
                Optional<MainBank> bk = mainBankRepository.findByBankCode(user.getBankCode());
                if (bk.isPresent() && bk.get().getBankNameFull() != null) orgName = bk.get().getBankNameFull();
            }
        } catch (Exception e) {
            log.warn("resolveOrgInfo failed for user {}: {}", user.getUsername(), e.getMessage());
        }
        if (entityCode == null) entityCode = user.getUsername();
        if (orgName == null) orgName = entityCode;
        return new String[]{orgName, entityCode};
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

    @Override
    public RestWithStatusList getUserHierarchy(Authentication authentication) {
        AdminContext ctx = contextResolver.resolve(authentication);
        if (!ctx.isUserCreator()) {
            return fail("Only users with created sub-users can fetch hierarchy");
        }
        List<AddUserResponse> hierarchy = buildUserHierarchyForParent(ctx.getCreatorUserId());
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User hierarchy fetched successfully")
                .data(new ArrayList<>(hierarchy))
                .build();
    }

    @Override
    public RestWithStatusList getUserHierarchyByBankCode(String bankCode) {
        if (bankCode == null || bankCode.trim().isEmpty()) {
            return fail("bankCode is required");
        }
        List<AddUser> roots = userRepository.findByBankCodeAndParentIdIsNull(bankCode);
        List<AddUserResponse> result = new ArrayList<>();
        for (AddUser root : roots) {
            AddUserResponse resp = toResponseWithChildren(root);
            result.add(resp);
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User hierarchy fetched successfully")
                .data(new ArrayList<>(result))
                .build();
    }

    @Override
    public RestWithStatusList getUserHierarchyByBranchCode(String branchCode) {
        if (branchCode == null || branchCode.trim().isEmpty()) {
            return fail("branchCode is required");
        }
        List<AddUser> roots = userRepository.findByBranchCodeAndParentIdIsNull(branchCode);
        List<AddUserResponse> result = new ArrayList<>();
        for (AddUser root : roots) {
            AddUserResponse resp = toResponseWithChildren(root);
            result.add(resp);
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User hierarchy fetched successfully")
                .data(new ArrayList<>(result))
                .build();
    }

    @Override
    public RestWithStatusList getUserHierarchyByBankDirect(String bankCode) {
        if (bankCode == null || bankCode.trim().isEmpty()) {
            return fail("bankCode is required");
        }
        List<AddUser> roots = userRepository.findByBankCodeAndBranchCodeIsNullAndParentIdIsNull(bankCode);
        List<AddUserResponse> result = new ArrayList<>();
        for (AddUser root : roots) {
            result.add(toResponseWithChildren(root));
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User hierarchy fetched successfully")
                .data(new ArrayList<>(result))
                .build();
    }

    private List<AddUserResponse> buildUserHierarchyForParent(Long parentId) {
        List<AddUser> children = userRepository.findByParentId(parentId);
        List<AddUserResponse> result = new ArrayList<>();
        for (AddUser child : children) {
            if (child.getStatus() == AddUser.UserStatus.INACTIVE || child.getStatus() == AddUser.UserStatus.REQUEST) {
                boolean isRestored = replacementRepository
                        .existsByReplacementEntityIdAndEntityTypeAndStatus(child.getId(), "USER", "RESTORED");
                if (isRestored) continue;
            }
            result.add(toResponseWithChildren(child));
        }
        return result;
    }

    private AddUserResponse toResponseWithChildren(AddUser user) {
        AddUserResponse resp = AddUserMapper.toResponse(user);
        enrichReplacement(resp, user.getId());
        List<AddUserResponse> children = buildUserHierarchyForParent(user.getId());
        resp.setChildren(children);
        return resp;
    }

    private void enrichReplacement(AddUserResponse resp, Long userId) {
        List<AdminReplacement> asRepOf = replacementRepository
                .findByReplacementEntityIdAndEntityTypeAndStatusIn(userId, "USER", Arrays.asList("ACTIVE", "PERMANENT"));
        if (!asRepOf.isEmpty()) {
            resp.setReplacementAdminRow(true);
            resp.setReplacementStatus(asRepOf.get(0).getStatus());
        } else {
            List<AdminReplacement> recs = replacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatusIn(userId, "USER", Arrays.asList("ACTIVE", "PERMANENT"));
            if (!recs.isEmpty()) {
                AdminReplacement rec = recs.get(0);
                resp.setReplacementStatus(rec.getStatus());
                userRepository.findById(rec.getReplacementEntityId())
                        .ifPresent(rep -> resp.setReplacedByUsername(rep.getUsername()));
            }
        }
    }

    @Override
    public RestWithStatusList getUserAncestors(Long userId) {
        Optional<AddUser> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) return fail("User not found");

        List<AddUserResponse> ancestors = new ArrayList<>();
        Long currentParentId = userOpt.get().getParentId();
        while (currentParentId != null) {
            Optional<AddUser> parentOpt = userRepository.findById(currentParentId);
            if (!parentOpt.isPresent()) break;
            AddUser parent = parentOpt.get();
            ancestors.add(AddUserMapper.toResponse(parent));
            currentParentId = parent.getParentId();
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Ancestors fetched")
                .data(new ArrayList<>(ancestors))
                .build();
    }

    @Override
    public RestWithStatusList delegateUser(Long userId, Long delegateeId, String reason, String delegatedBy) {
        Optional<AddUser> delegatorOpt = userRepository.findById(userId);
        if (!delegatorOpt.isPresent()) return fail("Delegator user not found");

        Optional<AddUser> delegateeOpt = userRepository.findById(delegateeId);
        if (!delegateeOpt.isPresent()) return fail("Delegatee user not found");

        AddUser delegator = delegatorOpt.get();
        AddUser delegatee = delegateeOpt.get();

        if (delegationRepository.existsByDelegatorUserIdAndStatus(userId, "ACTIVE")) {
            return fail("An active delegation already exists for this user");
        }

        // Transfer delegator's direct children to delegatee, remembering original parentId
        List<AddUser> children = userRepository.findByParentId(userId);
        for (AddUser child : children) {
            child.setPreDelegationParentId(child.getParentId());
            child.setParentId(delegateeId);
            userRepository.save(child);
        }

        // Schedule delegator for inactivation
        delegator.setStatus(AddUser.UserStatus.INACTIVE_PENDING);
        delegator.setInactivateScheduledAt(LocalDateTime.now().plusSeconds(30));
        delegator.setInactivateScheduledBy(delegatedBy);
        userRepository.save(delegator);

        // Create delegation record
        UserDelegation delegation = new UserDelegation();
        delegation.setDelegatorUserId(userId);
        delegation.setDelegateeUserId(delegateeId);
        delegation.setReason(reason);
        delegation.setStatus("ACTIVE");
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setCreatedBy(delegatedBy);
        delegationRepository.save(delegation);

        // Determine org name for delegator email
        String orgName = delegator.getBranchCode() != null ? delegator.getBranchCode()
                : delegator.getBankCode() != null ? delegator.getBankCode() : "";

        // Notify delegator
        emailService.sendDelegationToDelegate(
                delegator.getEmail(), delegator.getFullName(),
                delegatee.getFullName(), reason, orgName);

        // Notify delegatee
        emailService.sendDelegationToDelegatee(
                delegatee.getEmail(), delegatee.getFullName(),
                delegator.getFullName(), reason);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Delegation created successfully")
                .data(Collections.emptyList())
                .build();
    }
}
