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
import com.jpb.reconciliation.reconciliation.repository.RecModuleRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleRepository;
import com.jpb.reconciliation.reconciliation.enums.RoleCompatibilityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
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
            RoleType   roleType   = parseRoleType(req.getRoleType());
//            RoleStatus roleStatus = parseRoleStatus(req.getStatus());

            // 2. Validate role combination rules before any DB work
            compatibilityValidator.validate(req.getRoleNames());

            // 3. External fields mandatory when EXTERNAL
//            if (roleType == RoleType.EXTERNAL) {
//                validateExternalFields(req);
//            }

            // 4. Resolve RecRoleMaster for every name in the list
            Set<RecRoleMaster> masters = req.getRoleNames().stream()
                    .map(this::resolveRoleMaster)
                    .collect(Collectors.toSet());

            // 5. Build combined display name e.g. "MAKER + SUPERVISOR"
            String combinedName = req.getRoleNames().stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.joining(" + "));
            
         // 6. Derive roleMasterName (primary category for grouping/filtering)
            //    Single role  → "MAKER"
            //    Composite    → "MAKER + CHECKER" (or use only first if you prefer)
            String roleMasterName = req.getRoleNames().size() == 1
                    ? req.getRoleNames().get(0).trim().toUpperCase()
                    : combinedName;

//            // ✅ FIX 3: Check duplicate BEFORE hitting DB constraint
//            if (roleRepo.existsByRoleName(combinedName)) {
//                return RestWithStatusList.builder()
//                        .status("FAILURE")
//                        .statusMsg("Role '" + combinedName + "' already exists")
//                        .data(Collections.emptyList())
//                        .build();
//            }
            
         // 7. Duplicate check: same name + same roleType is a duplicate.
            //    We do NOT block same name with different roleType
            //    (e.g. "MAKER" can exist as RECON_USER and BANK_USER).
            //    We also do NOT block same name + same type if they are
            //    in different departments — remove the check below if you want
            //    fully unlimited duplicates (sequence alone enforces uniqueness via roleCode).
            if (roleRepo.existsByRoleNameIgnoreCaseAndRoleType(combinedName, roleType.name())) {
                return RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("A role named '" + combinedName + "' already exists for role type '"
                                + roleType.name() + "'. Use a different Role Type or add a unique description.")
                        .data(Collections.emptyList())
                        .build();
            }

            // 6. Generate role code
//            String generatedRoleCode;
//            if (masters.size() == 1) {
//                RecRoleMaster master = masters.iterator().next();
//                generatedRoleCode = String.valueOf(master.getRoleCode());
//            } else {
//                generatedRoleCode = masters.stream()
//                        .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
//                        .map(m -> String.valueOf(m.getRoleCode()))
//                        .collect(Collectors.joining("-"));
//            }
            
            String generatedRoleCode = codeGenerator.generateNextCode(combinedName);
            
            log.info("Creating role: combinedName=[{}] generatedCode=[{}] masters=[{}]",
                    combinedName,
                    roleMasterName,
                    generatedRoleCode,
                    masters.stream()
                           .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
                           .map(m -> m.getRoleName() + "(" + m.getRoleCode() + ")")
                           .collect(Collectors.joining(", ")));

            // 7. Build RecRole entity
            RecRole role = RecRole.builder()
                    .roleName(combinedName)
                    .roleCode(generatedRoleCode)
                    .roleMasterName(roleMasterName)        
                    .roleType(roleType.name())
//                    .status(roleStatus.name())
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
//                    .externalDepartmentName(
//                            roleType == RoleType.EXTERNAL ? req.getExternalDepartmentName() : null)
//                    .externalSupervisorName(
//                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorName() : null)
//                    .externalSupervisorEmail(
//                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorEmail() : null)
//                    .externalSupervisorPhone(
//                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorPhone() : null)
                    .build();

            // 8. Wire all masters into join table
            masters.forEach(role::addRoleMaster);

            // 9. Attach module permissions
            if (req.getPermissions() != null) {
                req.getPermissions().forEach(p -> role.addPermission(buildPermission(p)));
            }

            // 10. Persist
            RecRole saved = roleRepo.save(role);
            roleRepo.flush();

            RecRole withCode = roleRepo.findByIdWithPermissions(saved.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Role not found after save, id=" + saved.getId()));

            log.info("Role created → id={}, roleCode={},roleMasterName={}, masters={}",
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
    // GET PRIVILEGES
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Load existing permissions for a role, joined with module name.
     * Paste inside RecRoleServiceImpl (implements RecRoleService).
     * Also add the signature to RecRoleService interface.
     */
    
    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getPrivileges(Long roleId) {
        RecRole role = roleRepo.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
 
        // roleMapper.toResponseDTO already includes permissions list
        RecRoleResponseDTO dto = roleMapper.toResponseDTO(role);
 
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Privileges fetched successfully")
                .data(Collections.singletonList(dto))
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
            // Branch Admin → only their branch's roles
            roles = roleRepo.findByBranchCode(ctx.getBranchCode());
        } else if (ctx.getBankCode() != null) {
            // Bank Admin → only their bank's roles (excluding branch-scoped ones)
            roles = roleRepo.findByBankCodeAndBranchCodeIsNull(ctx.getBankCode());
        } else {
            // KAL Super Admin → all roles
            roles = roleRepo.findAll();
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
//            m.put("status",            r.getStatus());
            m.put("bankCode",          r.getBankCode());
            m.put("branchCode",        r.getBranchCode());
            m.put("assignedUserId",    r.getAssignedUserId());
            m.put("assignedUserName",  r.getAssignedUserName());
            m.put("assignedUserEmail", r.getAssignedUserEmail());
            m.put("createdAt",         r.getCreatedAt());
            m.put("createdBy",         r.getCreatedBy());
            // ── NEW: privilege summary ──
            int cnt = countMap.getOrDefault(r.getId(), 0);
            m.put("privilegeCount",   cnt);
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

//    @Override
//    @Transactional(noRollbackFor = {Exception.class})  // ✅ FIX 7: Same fix here
//    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {
//        try {
//            RecRole role = roleRepo.findByIdWithPermissions(roleId)
//                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            role.getPermissions().clear();
//            dtos.forEach(p -> role.addPermission(buildPermission(p)));
//
//            RecRole updated = roleRepo.save(role);
//            roleRepo.flush();
//
//            return RestWithStatusList.builder()
//                    .status("SUCCESS")
//                    .statusMsg("Permissions updated successfully")
//                    .data(Collections.singletonList(roleMapper.toResponseDTO(updated)))
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Error updating permissions: {}", e.getMessage(), e);
//            return RestWithStatusList.builder()
//                    .status("FAILURE")
//                    .statusMsg("Failed to update permissions: " + e.getMessage())
//                    .data(Collections.emptyList())
//                    .build();
//        }
//    }
    
    @Override
    @Transactional
    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {

        try {

            RecRole role = roleRepo.findByIdWithPermissions(roleId)
                    .orElseThrow(() ->
                            new RuntimeException("Role not found: " + roleId));

            // STEP 1 : Delete existing permissions from DB
            em.createQuery("DELETE FROM RecRoleModulePermission p WHERE p.role.id = :roleId")
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            // STEP 2 : Flush delete immediately
            em.flush();

            // STEP 3 : Clear persistence context
            em.clear();

            // Fetch role again after clear
            role = roleRepo.findById(roleId)
                    .orElseThrow(() ->
                            new RuntimeException("Role not found: " + roleId));

            // STEP 4 : Add new permissions
            for (RecPermissionRowDTO dto : dtos) {

                RecRoleModulePermission permission = buildPermission(dto);

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
    	// ← FIX: use fromRoleName (not deprecated getCodeByRoleName)
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
                        int nextCode = masterRepo.findMaxCustomRoleCode()
                                .map(max -> max + 1)
                                .orElse(9001);
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
    
 // Add this method to RecRoleServiceImpl, near resolveRoleMaster

//    @Override
//    @Transactional
//    public RecPermissionRowDTO resolveOrCreateModuleRow(RecPermissionLabelDTO labelDto) {
//        String normalizedLabel = labelDto.getModuleLabel().trim();
//
//        RecModule module = moduleRepo.findByModuleNameIgnoreCase(normalizedLabel)
//                .orElseGet(() -> {
//                    log.warn("RecModule not seeded for '{}' — creating from label", normalizedLabel);
//                    RecModule m = RecModule.builder()
//                            .name(normalizedLabel)
//                            .build();
//                    return moduleRepo.save(m);
//                });
//
//        return RecPermissionRowDTO.builder()
//                .moduleId(module.getId())
//                .hasAccess(labelDto.isHasAccess())
//                .canView(labelDto.isCanView())
//                .canCreate(labelDto.isCanCreate())
//                .canEdit(labelDto.isCanEdit())
//                .canApprove(labelDto.isCanApprove())
//                .canDownload(labelDto.isCanDownload())
//                .build();
//    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private RecRoleModulePermission buildPermission(RecPermissionRowDTO p) {
        RecModule module = moduleRepo.findById(p.getModuleId())
                .orElseThrow(() -> new RuntimeException(
                        "Module not found with id: " + p.getModuleId()));
        return RecRoleModulePermission.builder()
                .module(module)
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

//    private void validateExternalFields(RecCreateRoleRequestDTO req) {
//        if (isBlank(req.getExternalDepartmentName()))
//            throw new IllegalArgumentException(
//                    "externalDepartmentName is required for EXTERNAL roles");
//        if (isBlank(req.getExternalSupervisorName()))
//            throw new IllegalArgumentException(
//                    "externalSupervisorName is required for EXTERNAL roles");
//        if (isBlank(req.getExternalSupervisorEmail()))
//            throw new IllegalArgumentException(
//                    "externalSupervisorEmail is required for EXTERNAL roles");
//        if (isBlank(req.getExternalSupervisorPhone()))
//            throw new IllegalArgumentException(
//                    "externalSupervisorPhone is required for EXTERNAL roles");
//    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }


}