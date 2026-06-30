package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.dto.RecCreateRoleRequestDTO;
//import com.jpb.reconciliation.reconciliation.dto.RecPermissionLabelDTO;
import com.jpb.reconciliation.reconciliation.dto.RecPermissionRowDTO;
import com.jpb.reconciliation.reconciliation.dto.RecRoleResponseDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.RecModule;
import com.jpb.reconciliation.reconciliation.entity.RecRole;
import com.jpb.reconciliation.reconciliation.entity.RecRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.RecRoleModulePermission;
import com.jpb.reconciliation.reconciliation.enums.RoleStatus;
import com.jpb.reconciliation.reconciliation.enums.RoleType;
import com.jpb.reconciliation.reconciliation.enums.StandardRole;
import com.jpb.reconciliation.reconciliation.mapper.RecRoleMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RecModuleRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleRepository;
import com.jpb.reconciliation.reconciliation.enums.RoleCompatibilityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecRoleServiceImpl implements RecRoleService {

    private final RecRoleRepository          roleRepo;
    private final RecRoleMasterRepository    masterRepo;
    private final RecModuleRepository        moduleRepo;
    private final RoleCompatibilityValidator compatibilityValidator;
    private final RecRoleMapper              roleMapper;
    private final RecRoleCodeGeneratorService codeGenerator;
    private final AdminContextResolver        contextResolver;
    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager em;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    // ✅ FIX 1: noRollbackFor prevents transaction being marked rollback-only
    // when we catch the exception and return a FAILURE response
    @Transactional(noRollbackFor = {Exception.class})
    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {
        // Resolve bank/branch context from JWT — same as AddUser pattern
        AdminContext ctx = contextResolver.resolve(
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication());
        req.setCreatedBy(ctx.getUsername());
        req.setBankCode(ctx.getBankCode());
        req.setBranchCode(ctx.getBranchCode());

        try {

            // ✅ FIX 2: Validate input is not null
            if (req.getRoleNames() == null || req.getRoleNames().isEmpty()) {
                return RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Please select at least one role name")
                        .data(Collections.emptyList())
                        .build();
            }

            // 1. Parse and validate enum values
            RoleType roleType = parseRoleType(req.getRoleType());

            // 2. Validate role combination rules before any DB work
            compatibilityValidator.validate(req.getRoleNames());

            // 3. Resolve RecRoleMaster for every name in the list
            Set<RecRoleMaster> masters = req.getRoleNames().stream()
                    .map(this::resolveRoleMaster)
                    .collect(Collectors.toSet());

            // 4. Build combined display name e.g. "MAKER + SUPERVISOR"
            String combinedName = req.getRoleNames().stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.joining(" + "));

            // 5. Derive roleMasterName (primary category for grouping/filtering)
            //    Single role  → "MAKER"
            //    Composite    → "MAKER + CHECKER" (or use only first if you prefer)
            String roleMasterName = req.getRoleNames().size() == 1
                    ? req.getRoleNames().get(0).trim().toUpperCase()
                    : combinedName;

            // 6. Duplicate check — scoped to bank/branch context.
            //    Same name + same roleType within the same bank/branch is a duplicate.
            //    We do NOT block same name with different roleType
            //    (e.g. "MAKER" can exist as RECON_USER and BANK_USER).
            boolean duplicateExists;
            if (ctx.getBranchCode() != null) {
                duplicateExists = roleRepo.existsByRoleNameIgnoreCaseAndRoleTypeAndBranchCodeAndStatusNot(
                        combinedName, roleType.name(), ctx.getBranchCode(), "DELETED");
            } else if (ctx.getBankCode() != null) {
                duplicateExists = roleRepo.existsByRoleNameIgnoreCaseAndRoleTypeAndBankCodeAndBranchCodeIsNullAndStatusNot(
                        combinedName, roleType.name(), ctx.getBankCode(), "DELETED");
            } else {
                duplicateExists = roleRepo.existsByRoleNameIgnoreCaseAndRoleTypeAndStatusNot(
                        combinedName, roleType.name(), "DELETED");
            }

            if (duplicateExists) {
                if (!req.isForceCreate()) {
                    // Don't hard-fail — tell the frontend a duplicate exists so it can show
                    // the confirmation dialog. Use a distinct status so the UI can branch on it.
                    return RestWithStatusList.builder()
                            .status("DUPLICATE_ROLE_EXISTS")
                            .statusMsg("A role named '" + combinedName + "' already exists for role type '"
                                    + roleType.name() + "'. Create anyway?")
                            .data(Collections.emptyList())
                            .build();
                }
                // forceCreate=true → admin confirmed. Log for audit (maker/checker platform).
                log.info("forceCreate=true: creating duplicate role '{}' (type={}) requested by {}",
                        combinedName, roleType.name(), req.getCreatedBy());
            }

            // 7. Generate role code.
            //    Reuse the code already reserved during preview (GET /preview-code) so the
            //    number shown to the user always matches what gets saved. Only generate a
            //    fresh code if no reservation was sent (e.g. edit mode, or API called directly).
            String generatedRoleCode;
            boolean hasReservedCode = req.getReservedRoleCode() != null && !req.getReservedRoleCode().trim().isEmpty();

            if (hasReservedCode && !roleRepo.existsByRoleCode(req.getReservedRoleCode())) {
                // Reserved code is present AND still unclaimed — safe to reuse.
                generatedRoleCode = req.getReservedRoleCode();
                log.info("Using RESERVED role code: {}", generatedRoleCode);
            } else {
                // No reservation, or it went stale (role name changed after preview,
                // someone else claimed it, etc.) — generate a fresh one now.
                generatedRoleCode = codeGenerator.generateNextCode(roleMasterName);
                if (hasReservedCode) {
                    log.warn("Reserved code {} was stale/already taken — generated FRESH code: {}",
                            req.getReservedRoleCode(), generatedRoleCode);
                } else {
                    log.info("Using FRESH role code: {}", generatedRoleCode);
                }
            }

            log.info("Creating role: combinedName=[{}] roleMasterName=[{}] generatedCode=[{}] masters=[{}]",
                    combinedName,
                    roleMasterName,
                    generatedRoleCode,
                    masters.stream()
                           .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
                           .map(m -> m.getRoleName() + "(" + m.getRoleCode() + ")")
                           .collect(Collectors.joining(", ")));

            // 8. Build RecRole entity
            RecRole role = RecRole.builder()
                    .roleName(combinedName)
                    .roleCode(generatedRoleCode)
                    .roleMasterName(roleMasterName)
                    .roleType(roleType.name())
                    .status("ACTIVE")
                    .department(req.getDepartment())
                    .description(req.getDescription())
                    .validFrom(req.getValidFrom())
                    .validTo(req.getValidTo())
                    .createdBy(req.getCreatedBy())
                    .bankCode(req.getBankCode())
                    .branchCode(req.getBranchCode())
                    .sessionTimeout(req.getSessionTimeout())
                    .assignedUserId(req.getAssignedUserId())
                    .assignedUserName(req.getAssignedUserName())
                    .assignedUserEmail(req.getAssignedUserEmail())
                    .build();

            // 9. Wire all masters into join table
            masters.forEach(role::addRoleMaster);

            // 10. Attach module permissions
            if (req.getPermissions() != null && !req.getPermissions().isEmpty()) {
                // Caller supplied explicit permissions (e.g. from privileges modal) — use those.
                req.getPermissions().forEach(p -> role.addPermission(buildPermission(p)));
            } else if (req.isForceCreate()) {

                // Multiple duplicates can now legitimately exist (each with its own unique
                // roleCode). Clone permissions from the MOST RECENTLY CREATED one, since
                // that's the closest analogue to "the current version" of this role.
                List<RecRole> existingMatches = roleRepo
                        .findAllByRoleNameIgnoreCaseAndRoleTypeOrderByCreatedAtDesc(combinedName, roleType.name());

                if (!existingMatches.isEmpty()) {
                    RecRole mostRecent = existingMatches.get(0);
                    RecRole existingWithPerms = roleRepo.findByIdWithPermissions(mostRecent.getId())
                            .orElse(mostRecent);
                    existingWithPerms.getPermissions().forEach(existingPerm -> {
                        RecRoleModulePermission clonedPerm = RecRoleModulePermission.builder()
                                .module(existingPerm.getModule())
                                .hasAccess(existingPerm.isHasAccess())
                                .canView(existingPerm.isCanView())
                                .canCreate(existingPerm.isCanCreate())
                                .canEdit(existingPerm.isCanEdit())
                                .canApprove(existingPerm.isCanApprove())
                                .canDownload(existingPerm.isCanDownload())
                                .build();
                        role.addPermission(clonedPerm);
                    });
                    log.info("Cloned {} permission rows from most recent matching role id={} (of {} matches) onto new role",
                            existingWithPerms.getPermissions().size(), mostRecent.getId(), existingMatches.size());
                }
            }

            // 11. Persist
            RecRole savedRole;
            try {
                savedRole = roleRepo.save(role);
            } catch (DataIntegrityViolationException e) {
                log.warn("Role code {} collided at insert time (race condition) — retrying with a fresh code", generatedRoleCode);
                String retryCode = codeGenerator.generateNextCode(roleMasterName);
                role.setRoleCode(retryCode);
                savedRole = roleRepo.save(role);
                log.info("Retry succeeded with role code: {}", retryCode);
            }
            roleRepo.flush();

            final RecRole saved = savedRole; // now safely final for the lambda below

            RecRole withCode = roleRepo.findByIdWithPermissions(saved.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Role not found after save, id=" + saved.getId()));

            log.info("Role created → id={}, roleCode={}, roleMasterName={}, masters={}",
                    withCode.getId(),
                    withCode.getRoleCode(),
                    withCode.getRoleMasterName(),
                    masters.stream()
                           .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
                           .map(m -> m.getRoleName() + "(" + m.getRoleCode() + ")")
                           .collect(Collectors.joining(", ")));

            RecRoleResponseDTO responseDTO = roleMapper.toResponseDTO(withCode);
            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Role created successfully")
                    .data(Collections.singletonList(responseDTO))
                    .build();

        // ✅ FIX 5: Catch DB-level duplicate as safety net
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate role constraint violation: {}", e.getMessage());
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("Role already exists — duplicate entry detected")
                    .data(Collections.emptyList())
                    .build();

        } catch (IllegalArgumentException e) {
            // validation errors (roleType, status, external fields, compatibility)
            log.warn("Validation error while creating role: {}", e.getMessage());
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg(e.getMessage())
                    .data(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error while creating role: {}", e.getMessage(), e);
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("An internal error occurred: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE ROLE BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RestWithStatusList updateRole(Long id, RecCreateRoleRequestDTO req) {

        try {

            RecRole role = roleRepo.findByIdWithPermissions(id)
                    .orElseThrow(() ->
                            new RuntimeException("Role not found: " + id));

            // Role Type
            if (req.getRoleType() != null) {
                RoleType roleType = parseRoleType(req.getRoleType());
                role.setRoleType(roleType.name());
            }

            // Basic Fields
            role.setDepartment(req.getDepartment());
            role.setDescription(req.getDescription());
            role.setValidFrom(req.getValidFrom());
            role.setValidTo(req.getValidTo());
            role.setSessionTimeout(req.getSessionTimeout());

            // Assigned User
            role.setAssignedUserId(req.getAssignedUserId());
            role.setAssignedUserName(req.getAssignedUserName());
            role.setAssignedUserEmail(req.getAssignedUserEmail());

            // Permissions
            if (req.getPermissions() != null) {
                role.getPermissions().clear();
                req.getPermissions()
                        .forEach(p ->
                                role.addPermission(buildPermission(p)));
            }

            RecRole updatedRole = roleRepo.save(role);

            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Role updated successfully")
                    .data(Collections.singletonList(
                            roleMapper.toResponseDTO(updatedRole)))
                    .build();

        } catch (Exception e) {

            log.error("Error updating role", e);

            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("Failed to update role: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE ROLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RestWithStatusList deleteRole(Long id) {
        AdminContext ctx = contextResolver.resolve(
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication());

        RecRole role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if ("DELETED".equals(role.getStatus())) {
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("Role is already deleted")
                    .data(Collections.emptyList())
                    .build();
        }

        role.setStatus("DELETED");
        roleRepo.save(role);

        log.info("Role soft-deleted: id={}, roleCode={}, roleName={}, deletedBy={}",
                id, role.getRoleCode(), role.getRoleName(), ctx.getUsername());

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Role deleted successfully")
                .data(Collections.emptyList())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PRIVILEGES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load existing permissions for a role, joined with module name.
     */
    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getPrivileges(Long roleId) {
        RecRole role = roleRepo.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        // Build permission list with menuName joined from RCN_MENU_MASTER
        List<Map<String, Object>> permissions = role.getPermissions().stream().map(p -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("moduleId",   p.getModule() != null ? p.getModule().getId()   : null);
            row.put("moduleName", p.getModule() != null ? p.getModule().getName() : null);
            row.put("menuId",     p.getMenuId());   // null = product-level row
            row.put("hasAccess",  p.isHasAccess());
            row.put("canView",    p.isCanView());
            row.put("canCreate",  p.isCanCreate());
            row.put("canEdit",    p.isCanEdit());
            row.put("canApprove", p.isCanApprove());
            row.put("canDownload",p.isCanDownload());

            // Join menuName from RCN_MENU_MASTER for non-null menuId rows
            if (p.getMenuId() != null) {
                menuMasterRepository.findByMenuId(p.getMenuId()).ifPresent(menu -> {
                    row.put("menuName", menu.getMenuName());
                    row.put("menuType", menu.getMenuType());
                });
            }

            return row;
        }).collect(Collectors.toList());

        // Wrap in the same shape the frontend expects:
        // { data: [{ permissions: [...] }] }
        Map<String, Object> wrapper = new java.util.LinkedHashMap<>();
        wrapper.put("id",          role.getId());
        wrapper.put("roleName",    role.getRoleName());
        wrapper.put("roleCode",    role.getRoleCode());
        wrapper.put("roleType",    role.getRoleType());
        wrapper.put("permissions", permissions);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Privileges fetched successfully")
                .data(Collections.singletonList(wrapper))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getRole(Long id) {
        RecRole role = roleRepo.findByIdWithPermissions(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if ("DELETED".equals(role.getStatus())) {
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("This role has been deleted")
                    .data(Collections.emptyList())
                    .build();
        }
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Role fetched successfully")
                .data(Collections.singletonList(roleMapper.toResponseDTO(role)))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL ROLES
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getAllRoles() {
        AdminContext ctx = contextResolver.resolve(
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication());

        List<RecRole> roles;
        if (ctx.getBranchCode() != null) {
            roles = roleRepo.findByBranchCodeAndStatusNot(ctx.getBranchCode(), "DELETED");
        } else if (ctx.getBankCode() != null) {
            roles = roleRepo.findByBankCodeAndBranchCodeIsNullAndStatusNot(ctx.getBankCode(), "DELETED");
        } else {
            roles = roleRepo.findByStatusNot("DELETED");
        }

        if (roles.isEmpty()) {
            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("No roles found")
                    .data(Collections.emptyList())
                    .build();
        }

        // ── Fetch privilege summary for ALL roles in ONE query ──────────────
        // Returns: [roleId, privilegeCount, comma-separated product names]
        List<Long> roleIds = roles.stream()
                .map(RecRole::getId)
                .collect(Collectors.toList());

        // Native query — works on Oracle with LISTAGG
        @SuppressWarnings("unchecked")
        List<Object[]> privRows = em.createNativeQuery("SELECT " +
                "p.ROLE_ID, " +
                "COUNT(p.ID) AS PRIV_COUNT, " +
                "LISTAGG(DISTINCT m.NAME, ',') WITHIN GROUP (ORDER BY m.NAME) AS PROD_NAMES " +
                "FROM REC_ROLE_MODULE_PERMISSIONS_TEST p " +
                "JOIN REC_MODULES_TEST m ON m.ID = p.MODULE_ID " +
                "WHERE p.HAS_ACCESS = 1 " +
                "AND p.ROLE_ID IN (:roleIds) " +
                "GROUP BY p.ROLE_ID")
                .setParameter("roleIds", roleIds)
                .getResultList();

        // Build maps: roleId → count, roleId → productList
        Map<Long, Integer>      countMap = new HashMap<>();
        Map<Long, List<String>> prodMap  = new HashMap<>();

        for (Object[] row : privRows) {
            Long   rid      = ((Number) row[0]).longValue();
            int    cnt      = ((Number) row[1]).intValue();
            String prodsCsv = row[2] != null ? (String) row[2] : "";

            countMap.put(rid, cnt);
            prodMap.put(rid,
                    prodsCsv.isEmpty()
                            ? Collections.emptyList()
                            : Arrays.asList(prodsCsv.split(","))
            );
        }

        // ── Build response: role fields + privilegeCount + assignedProducts ──
        List<Map<String, Object>> result = roles.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",                r.getId());
            m.put("roleName",          r.getRoleName());
            m.put("roleCode",          r.getRoleCode());
            m.put("roleType",          r.getRoleType());
            m.put("roleMasterName",    r.getRoleMasterName());
            m.put("description",       r.getDescription());
            m.put("department",        r.getDepartment());
            m.put("sessionTimeout",    r.getSessionTimeout());
            m.put("validFrom",         r.getValidFrom());
            m.put("validTo",           r.getValidTo());
            m.put("bankCode",          r.getBankCode());
            m.put("branchCode",        r.getBranchCode());
            m.put("assignedUserId",    r.getAssignedUserId());
            m.put("assignedUserName",  r.getAssignedUserName());
            m.put("assignedUserEmail", r.getAssignedUserEmail());
            m.put("createdAt",         r.getCreatedAt());
            m.put("createdBy",         r.getCreatedBy());
            // ── privilege summary ──
            int cnt = countMap.getOrDefault(r.getId(), 0);
            m.put("privilegeCount", cnt);
            m.put("assignedProducts",
                    cnt > 0
                            ? prodMap.getOrDefault(r.getId(), Collections.emptyList())
                            : Collections.emptyList());
            return m;
        }).collect(Collectors.toList());

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Roles fetched successfully")
                .data(new ArrayList<>(roles))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL MODULES
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getAllModules() {
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Modules fetched successfully")
                .data(Collections.unmodifiableList(roleMapper.toModuleDTOList(moduleRepo.findAll())))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE PERMISSIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {

        try {
            // STEP 1: Delete ALL existing permissions for this role
            em.createQuery("DELETE FROM RecRoleModulePermission p WHERE p.role.id = :roleId")
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            // STEP 2: Flush + clear so Hibernate sees a clean state
            em.flush();
            em.clear();

            // STEP 3: Re-fetch role after clear
            RecRole role = roleRepo.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

            // STEP 4: Build and attach new permissions
            for (RecPermissionRowDTO dto : dtos) {
                RecRoleModulePermission permission = buildPermission(dto);
                // menuId is already set in buildPermission below
                permission.setRole(role);
                role.addPermission(permission);
            }

            roleRepo.save(role);

            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Privileges updated successfully")
                    .data(Collections.singletonList(roleMapper.toResponseDTO(role)))
                    .build();

        } catch (Exception e) {
            log.error("Error updating permissions", e);
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg(e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resolveRoleMaster
    // ─────────────────────────────────────────────────────────────────────────

    private RecRoleMaster resolveRoleMaster(String roleName) {
        // Uses fromRoleName (not the deprecated getCodeByRoleName)
        StandardRole category = StandardRole.fromRoleName(roleName);

        if (category.isStandard()) {
            return masterRepo.findByRoleName(roleName.toUpperCase())
                    .orElseGet(() -> {
                        log.warn("RecRoleMaster not seeded for '{}' — creating from enum", roleName);
                        return masterRepo.save(RecRoleMaster.builder()
                                .roleName(roleName.toUpperCase())
                                .roleCode(category.getBaseCode())
                                .isSystemRole(true)
                                .status(RoleStatus.ACTIVE.name())
                                .build());
                    });
        } else {
            String normalized = roleName.trim().toUpperCase().replace(" ", "_");
            return masterRepo.findByRoleName(normalized)
                    .orElseGet(() -> {
                        int nextCode = nextCustomRoleCode(); // pulls ROLE_CODE_CUSTOM_SEQ.NEXTVAL
                        log.info("Custom RecRoleMaster → name={}, code={}", normalized, nextCode);
                        return masterRepo.save(RecRoleMaster.builder()
                                .roleName(normalized)
                                .roleCode(nextCode)
                                .isSystemRole(false)
                                .status(RoleStatus.REQUEST.name())
                                .build());
                    });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private RecRoleModulePermission buildPermission(RecPermissionRowDTO p) {
        RecModule module = moduleRepo.findById(p.getModuleId())
                .orElseThrow(() -> new RuntimeException(
                        "Module not found with id: " + p.getModuleId()));
        return RecRoleModulePermission.builder()
                .module(module)
                .menuId(p.getMenuId())
                .hasAccess(p.isHasAccess())
                .canView(p.isCanView())
                .canCreate(p.isCanCreate())
                .canEdit(p.isCanEdit())
                .canApprove(p.isCanApprove())
                .canDownload(p.isCanDownload())
                .build();
    }

    private RoleType parseRoleType(String raw) {
        try {
            return RoleType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid roleType '" + raw + "'. Allowed: RECON_USER, BANK_USER, BRANCH_USER.");
        }
    }

    private RoleStatus parseRoleStatus(String raw) {
        try {
            return RoleStatus.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid status '" + raw + "'. Allowed: "
                            + Arrays.toString(RoleStatus.values()));
        }
    }

    /**
     * Pulls the next custom role code from ROLE_CODE_CUSTOM_SEQ.
     * NEXTVAL is atomic at the DB level — two concurrent requests can never
     * receive the same number, which is what eliminates the preview/actual
     * mismatch (previously caused by a non-atomic MAX(roleCode)+1 read).
     */
    private int nextCustomRoleCode() {
        Number next = (Number) em.createNativeQuery(
                "SELECT ROLE_CODE_CUSTOM_SEQ.NEXTVAL FROM dual")
                .getSingleResult();
        return next.intValue();
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

}