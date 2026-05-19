





package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RecCreateRoleRequestDTO;
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

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    // ✅ FIX 1: noRollbackFor prevents transaction being marked rollback-only
    // when we catch the exception and return a FAILURE response
    @Transactional(noRollbackFor = {Exception.class})
    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {
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
            RoleStatus roleStatus = parseRoleStatus(req.getStatus());

            // 2. Validate role combination rules before any DB work
            compatibilityValidator.validate(req.getRoleNames());

            // 3. External fields mandatory when EXTERNAL
            if (roleType == RoleType.EXTERNAL) {
                validateExternalFields(req);
            }

            // 4. Resolve RecRoleMaster for every name in the list
            Set<RecRoleMaster> masters = req.getRoleNames().stream()
                    .map(this::resolveRoleMaster)
                    .collect(Collectors.toSet());

            // 5. Build combined display name e.g. "MAKER + SUPERVISOR"
            String combinedName = req.getRoleNames().stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.joining(" + "));

            // ✅ FIX 3: Check duplicate BEFORE hitting DB constraint
            if (roleRepo.existsByRoleName(combinedName)) {
                return RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Role '" + combinedName + "' already exists")
                        .data(Collections.emptyList())
                        .build();
            }

            // 6. Generate role code
            String generatedRoleCode;
            if (masters.size() == 1) {
                RecRoleMaster master = masters.iterator().next();
                generatedRoleCode = String.valueOf(master.getRoleCode());
            } else {
                generatedRoleCode = masters.stream()
                        .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
                        .map(m -> String.valueOf(m.getRoleCode()))
                        .collect(Collectors.joining("-"));
            }

            // 7. Build RecRole entity
            RecRole role = RecRole.builder()
                    .roleName(combinedName)
                    .roleCode(generatedRoleCode)
                    .roleType(roleType.name())
                    .status(roleStatus.name())
                    .description(req.getDescription())
                    .validFrom(req.getValidFrom())
                    .validTo(req.getValidTo())
                    .createdBy(req.getCreatedBy())
                    // ✅ FIX 4: Map assigned user fields from DTO to entity
                    .assignedUserId(req.getAssignedUserId())
                    .assignedUserName(req.getAssignedUserName())
                    .assignedUserEmail(req.getAssignedUserEmail())
                    .externalDepartmentName(
                            roleType == RoleType.EXTERNAL ? req.getExternalDepartmentName() : null)
                    .externalSupervisorName(
                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorName() : null)
                    .externalSupervisorEmail(
                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorEmail() : null)
                    .externalSupervisorPhone(
                            roleType == RoleType.EXTERNAL ? req.getExternalSupervisorPhone() : null)
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

            log.info("Role created → id={}, roleCode={}, masters={}",
                    withCode.getId(),
                    withCode.getRoleCode(),
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
    @Transactional(readOnly = true)  // ✅ FIX 6: Added readOnly for performance
    public RestWithStatusList getAllRoles() {
        List<RecRole> roles = roleRepo.findAll();
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
    @Transactional(noRollbackFor = {Exception.class})  // ✅ FIX 7: Same fix here
    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {
        try {
            RecRole role = roleRepo.findByIdWithPermissions(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

            role.getPermissions().clear();
            dtos.forEach(p -> role.addPermission(buildPermission(p)));

            RecRole updated = roleRepo.save(role);
            roleRepo.flush();

            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Permissions updated successfully")
                    .data(Collections.singletonList(roleMapper.toResponseDTO(updated)))
                    .build();

        } catch (Exception e) {
            log.error("Error updating permissions: {}", e.getMessage(), e);
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("Failed to update permissions: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resolveRoleMaster
    // ─────────────────────────────────────────────────────────────────────────

    private RecRoleMaster resolveRoleMaster(String roleName) {
        Integer enumCode = StandardRole.getCodeByRoleName(roleName);

        if (enumCode != null) {
            return masterRepo.findByRoleName(roleName.toUpperCase())
                    .orElseGet(() -> {
                        log.warn("RecRoleMaster not seeded for '{}' — creating from enum", roleName);
                        return masterRepo.save(RecRoleMaster.builder()
                                .roleName(roleName.toUpperCase())
                                .roleCode(enumCode)
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
                    "Invalid roleType '" + raw + "'. Allowed: INTERNAL, EXTERNAL.");
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

    private void validateExternalFields(RecCreateRoleRequestDTO req) {
        if (isBlank(req.getExternalDepartmentName()))
            throw new IllegalArgumentException(
                    "externalDepartmentName is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorName()))
            throw new IllegalArgumentException(
                    "externalSupervisorName is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorEmail()))
            throw new IllegalArgumentException(
                    "externalSupervisorEmail is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorPhone()))
            throw new IllegalArgumentException(
                    "externalSupervisorPhone is required for EXTERNAL roles");
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}//package com.jpb.reconciliation.reconciliation.service;
//
//import com.jpb.reconciliation.reconciliation.dto.RecCreateRoleRequestDTO;
//import com.jpb.reconciliation.reconciliation.dto.RecPermissionRowDTO;
//import com.jpb.reconciliation.reconciliation.dto.RecRoleResponseDTO;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.entity.RecModule;
//import com.jpb.reconciliation.reconciliation.entity.RecRole;
//import com.jpb.reconciliation.reconciliation.entity.RecRoleMaster;
//import com.jpb.reconciliation.reconciliation.entity.RecRoleModulePermission;
//import com.jpb.reconciliation.reconciliation.enums.RoleStatus;
//import com.jpb.reconciliation.reconciliation.enums.RoleType;
//import com.jpb.reconciliation.reconciliation.enums.StandardRole;
//import com.jpb.reconciliation.reconciliation.mapper.RecRoleMapper;
//import com.jpb.reconciliation.reconciliation.repository.RecModuleRepository;
//import com.jpb.reconciliation.reconciliation.repository.RecRoleMasterRepository;
//import com.jpb.reconciliation.reconciliation.repository.RecRoleRepository;
//import com.jpb.reconciliation.reconciliation.enums.RoleCompatibilityValidator;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
// 
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RecRoleServiceImpl implements RecRoleService {
// 
//    private final RecRoleRepository          roleRepo;
//    private final RecRoleMasterRepository    masterRepo;
//    private final RecModuleRepository        moduleRepo;
//    private final RoleCompatibilityValidator compatibilityValidator;
//    private final RecRoleMapper              roleMapper;
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // CREATE
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    @Override
//    @Transactional
//    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {
//    	try {
// 
//        // 1. Parse and validate enum values
//        RoleType   roleType   = parseRoleType(req.getRoleType());
//        RoleStatus roleStatus = parseRoleStatus(req.getStatus());
// 
//        // 2. Validate role combination rules before any DB work
//        compatibilityValidator.validate(req.getRoleNames());
// 
//        // 3. External fields mandatory when EXTERNAL
//        if (roleType == RoleType.EXTERNAL) {
//            validateExternalFields(req);
//        }
// 
//        // 4. Resolve RecRoleMaster for every name in the list
//        Set<RecRoleMaster> masters = req.getRoleNames().stream()
//                .map(this::resolveRoleMaster)
//                .collect(Collectors.toSet());
// 
//        // 5. Build combined display name e.g. "MAKER + SUPERVISOR"
//        String combinedName = req.getRoleNames().stream()
//                .map(String::toUpperCase)
//                .sorted()
//                .collect(Collectors.joining(" + "));
//        
//     // 6. Generate role code for REC_ROLES_TEST
//        String generatedRoleCode;
//
//        if (masters.size() == 1) {
//
//            // Single role → use its code directly
//            RecRoleMaster master = masters.iterator().next();
//            generatedRoleCode = String.valueOf(master.getRoleCode());
//
//        } else {
//
//            // Multiple roles → combine codes
//            generatedRoleCode = masters.stream()
//                    .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
//                    .map(m -> String.valueOf(m.getRoleCode()))
//                    .collect(Collectors.joining("-"));
//        }
// 
//        // 7. Build RecRole — ROLE_CODE left blank (Oracle trigger writes it)
//        RecRole role = RecRole.builder()
//                .roleName(combinedName)
//                .roleCode(generatedRoleCode)
//                .roleType(roleType.name())
//                .status(roleStatus.name())
//                .description(req.getDescription())
//                .validFrom(req.getValidFrom())
//                .validTo(req.getValidTo())
//                .createdBy(req.getCreatedBy())
//                .externalDepartmentName(
//                        roleType == RoleType.EXTERNAL ? req.getExternalDepartmentName() : null)
//                .externalSupervisorName(
//                        roleType == RoleType.EXTERNAL ? req.getExternalSupervisorName() : null)
//                .externalSupervisorEmail(
//                        roleType == RoleType.EXTERNAL ? req.getExternalSupervisorEmail() : null)
//                .externalSupervisorPhone(
//                        roleType == RoleType.EXTERNAL ? req.getExternalSupervisorPhone() : null)
//                .build();
// 
//        // 7. Wire all masters into join table REC_ROLE_MASTER_MAP
//        masters.forEach(role::addRoleMaster);
// 
//        // 8. Attach module permissions
//        if (req.getPermissions() != null) {
//            req.getPermissions().forEach(p -> role.addPermission(buildPermission(p)));
//        }
// 
//        // 9. Persist → Oracle trigger fires → flush → re-fetch with ROLE_CODE
//        RecRole saved = roleRepo.save(role);
//        roleRepo.flush();
// 
//        RecRole withCode = roleRepo.findByIdWithPermissions(saved.getId())
//                .orElseThrow(() -> new RuntimeException(
//                        "Role not found after save, id=" + saved.getId()));
// 
//        log.info("Role created → id={}, roleCode={}, masters={}",
//                withCode.getId(),
//                withCode.getRoleCode(),
//                masters.stream()
//                       .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
//                       .map(m -> m.getRoleName() + "(" + m.getRoleCode() + ")")
//                       .collect(Collectors.joining(", ")));
// 
//        RecRoleResponseDTO responseDTO = roleMapper.toResponseDTO(withCode);
//        return RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg("Role created successfully")
//                .data(Collections.singletonList(responseDTO))
//                .build();
//        
//    	}catch (Exception e) {
//    		log.error("Unexpected error while creating role: {}", e.getMessage(), e);
//            return RestWithStatusList.builder()
//                    .status("FAILURE")
//                    .statusMsg("An internal error occurred. Please contact support.")
//                    .data(Collections.emptyList())
//                    .build();
//		}
//    }
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET BY ID
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    @Override
//    @Transactional(readOnly = true)
//    public RestWithStatusList getRole(Long id) {
//        RecRole role = roleRepo.findByIdWithPermissions(id)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
//        return RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg("Role fetched successfully")
//                .data(Collections.singletonList(roleMapper.toResponseDTO(role)))
//                .build();
//    }
//    
//    // 
//    
//    @Override
//    public RestWithStatusList getAllRoles() {
//
//        List<RecRole> roles = roleRepo.findAll();
//
//        return RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg("Roles fetched successfully")
//                .data(new ArrayList<Object>(roles))
//                .build();
//    }
//    
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET ALL MODULES
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    @Override
//    @Transactional(readOnly = true)
//    public RestWithStatusList getAllModules() {
//        return RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg("Modules fetched successfully")
//                .data(Collections.unmodifiableList(roleMapper.toModuleDTOList(moduleRepo.findAll())))
//                .build();
//    }
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // UPDATE PERMISSIONS
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    @Override
//    @Transactional
//    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {
//        RecRole role = roleRepo.findByIdWithPermissions(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
// 
//        role.getPermissions().clear();
//        dtos.forEach(p -> role.addPermission(buildPermission(p)));
// 
//        RecRole updated = roleRepo.save(role);
//        roleRepo.flush();
// 
//        return RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg("Permissions updated successfully")
//                .data(Collections.singletonList(roleMapper.toResponseDTO(updated)))
//                .build();
//    }
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // resolveRoleMaster: StandardRole enum → RecRoleMaster entity
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    private RecRoleMaster resolveRoleMaster(String roleName) {
//        Integer enumCode = StandardRole.getCodeByRoleName(roleName);
// 
//        if (enumCode != null) {
//            // Standard role — code comes from StandardRole enum
//            return masterRepo.findByRoleName(roleName.toUpperCase())
//                    .orElseGet(() -> {
//                        log.warn("RecRoleMaster not seeded for '{}' — creating from enum", roleName);
//                        return masterRepo.save(RecRoleMaster.builder()
//                                .roleName(roleName.toUpperCase())
//                                .roleCode(enumCode)
//                                .isSystemRole(true)
//                                .status(RoleStatus.ACTIVE.name())
//                                .build());
//                    });
//        } else {
//            // Custom role — generate next code above 9000
//            String normalized = roleName.trim().toUpperCase().replace(" ", "_");
//            return masterRepo.findByRoleName(normalized)
//                    .orElseGet(() -> {
//                        int nextCode = masterRepo.findMaxCustomRoleCode()
//                                .map(max -> max + 1)
//                                .orElse(9001);
//                        log.info("Custom RecRoleMaster → name={}, code={}", normalized, nextCode);
//                        return masterRepo.save(RecRoleMaster.builder()
//                                .roleName(normalized)
//                                .roleCode(nextCode)
//                                .isSystemRole(false)
//                                .status(RoleStatus.REQUEST.name())
//                                .build());
//                    });
//        }
//    }
// 
//    // ─────────────────────────────────────────────────────────────────────────
//    // Private helpers
//    // ─────────────────────────────────────────────────────────────────────────
// 
//    private RecRoleModulePermission buildPermission(RecPermissionRowDTO p) {
//        RecModule module = moduleRepo.findById(p.getModuleId())
//                .orElseThrow(() -> new RuntimeException(
//                        "Module not found with id: " + p.getModuleId()));
//        return RecRoleModulePermission.builder()
//                .module(module)
//                .hasAccess(p.isHasAccess())
//                .canView(p.isCanView())
//                .canCreate(p.isCanCreate())
//                .canEdit(p.isCanEdit())
//                .canApprove(p.isCanApprove())
//                .canDownload(p.isCanDownload())
//                .build();
//    }
// 
//    private RoleType parseRoleType(String raw) {
//        try {
//            return RoleType.valueOf(raw.toUpperCase());
//        } catch (Exception e) {
//            throw new IllegalArgumentException(
//                    "Invalid roleType '" + raw + "'. Allowed: INTERNAL, EXTERNAL.");
//        }
//    }
// 
//    private RoleStatus parseRoleStatus(String raw) {
//        try {
//            return RoleStatus.valueOf(raw.toUpperCase());
//        } catch (Exception e) {
//            throw new IllegalArgumentException(
//                    "Invalid status '" + raw + "'. Allowed: "
//                    + Arrays.toString(RoleStatus.values()));
//        }
//    }
// 
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
// 
//    private boolean isBlank(String v) {
//        return v == null || v.trim().isEmpty();
//    }
//
//}
//
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import com.jpb.reconciliation.reconciliation.dto.*;
////import com.jpb.reconciliation.reconciliation.entity.*;
////import com.jpb.reconciliation.reconciliation.mapper.RecRoleMapper;
////import com.jpb.reconciliation.reconciliation.repository.*;
////
////import java.util.Collections;
////import java.util.List;
////import java.util.stream.Collectors;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class RecRoleServiceImpl implements RecRoleService {
////
////    private final RecRoleRepository roleRepo;
////    private final RecModuleRepository moduleRepo;
////    private final RecRoleModulePermissionRepository permRepo;
////
////    // ── Get all modules ─────────────────────────────────────────
////    @Override
////    public RestWithStatusList getAllModules() {
////
////        List<Object> modules = moduleRepo.findAllByOrderByDisplayOrderAsc()
////                .stream()
////                .map(m -> RecPermissionRowDTO.builder()
////                        .moduleId(m.getId())
////                        .moduleName(m.getName())
////                        .build())
////                .collect(Collectors.toList());
////
////        return RestWithStatusList.builder()
////                .status("SUCCESS")
////                .statusMsg("Modules fetched successfully")
////                .data(modules)
////                .build();
////    }
////
////    // ── Create Role ─────────────────────────────────────────────
////    @Override
////    @Transactional
////    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {
////
////        // Validate
////        boolean anyAccess = req.getPermissions() != null &&
////                req.getPermissions().stream().anyMatch(RecPermissionRowDTO::isHasAccess);
////
////        if (!anyAccess) {
////            throw new RuntimeException("Please enable access to at least one module.");
////        }
////
////        // Save role
////        RecRole role = RecRole.builder()
////                .roleName(req.getRoleName())
////                .roleType(req.getRoleType())
////                .externalDepartmentName(req.getExternalDepartmentName())
////                .externalSupervisorName(req.getExternalSupervisorName())
////                .externalSupervisorPhone(req.getExternalSupervisorPhone())
////                .externalSupervisorEmail(req.getExternalSupervisorEmail())
////                .status(req.getStatus() != null ? req.getStatus() : "DRAFT")
////                .description(req.getDescription())
////                .validFrom(req.getValidFrom())
////                .validTo(req.getValidTo())
////                .createdBy(req.getCreatedBy())
////                .build();
////
////        RecRole saved = roleRepo.save(role);
////        roleRepo.flush();
////
////        saved = roleRepo.findById(saved.getId())
////                .orElseThrow(() -> new RuntimeException("Role not found after creation"));
////
////        log.info("Created role: {} with code: {}", saved.getRoleName(), saved.getRoleCode());
////
////        // Save permissions
////        savePermissions(saved, req.getPermissions());
////
////        RecRoleResponseDTO responseDTO = buildResponse(saved);
////
////        return RestWithStatusList.builder()
////                .status("SUCCESS")
////                .statusMsg("Role created successfully")
////                .data(Collections.singletonList(responseDTO))
////                .build();
////    }
////
////    // ── Update Permissions ──────────────────────────────────────
////    @Override
////    @Transactional
////    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {
////
////        RecRole role = roleRepo.findById(roleId)
////                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
////
////        permRepo.deleteByRoleId(roleId);
////        savePermissions(role, dtos);
////
////        RecRoleResponseDTO responseDTO = buildResponse(role);
////
////        return RestWithStatusList.builder()
////                .status("SUCCESS")
////                .statusMsg("Permissions updated successfully")
////                .data(Collections.singletonList(responseDTO))
////                .build();
////    }
////
////    // ── Get Role ────────────────────────────────────────────────
////    @Override
////    public RestWithStatusList getRole(Long roleId) {
////
////        RecRole role = roleRepo.findById(roleId)
////                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
////
////        RecRoleResponseDTO responseDTO = buildResponse(role);
////
////        return RestWithStatusList.builder()
////                .status("SUCCESS")
////                .statusMsg("Role fetched successfully")
////                .data(Collections.singletonList(responseDTO))
////                .build();
////    }
////
////    // ── Private Helpers ─────────────────────────────────────────
////
////    private void savePermissions(RecRole role, List<RecPermissionRowDTO> dtos) {
////
////        if (dtos == null) return;
////
////        for (RecPermissionRowDTO dto : dtos) {
////
////            RecModule module = moduleRepo.findById(dto.getModuleId())
////                    .orElseThrow(() ->
////                            new RuntimeException("Invalid Module ID: " + dto.getModuleId()));
////
////            boolean access = dto.isHasAccess();
////
////            RecRoleModulePermission perm = RecRoleModulePermission.builder()
////                    .role(role)
////                    .module(module)
////                    .hasAccess(access)
////                    .canView(access && dto.isCanView())
////                    .canCreate(access && dto.isCanCreate())
////                    .canEdit(access && dto.isCanEdit())
////                    .canApprove(access && dto.isCanApprove())
////                    .canDownload(access && dto.isCanDownload())
////                    .build();
////
////            permRepo.save(perm);
////        }
////    }
////
////    private RecRoleResponseDTO buildResponse(RecRole role) {
////
////        List<RecRoleModulePermission> perms = permRepo.findByRoleId(role.getId());
////
////        return RecRoleMapper.toResponse(role, perms);
////    }
////}