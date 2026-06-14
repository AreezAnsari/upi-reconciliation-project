package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RecCreateRoleRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.RecPermissionRowDTO;
import com.jpb.reconciliation.reconciliation.dto.RecRoleResponseDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.RecModule;
import com.jpb.reconciliation.reconciliation.entity.RecRole;
import com.jpb.reconciliation.reconciliation.entity.RecRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.RecRoleModulePermission;
import com.jpb.reconciliation.reconciliation.enums.RoleCompatibilityValidator;
import com.jpb.reconciliation.reconciliation.enums.RoleStatus;
import com.jpb.reconciliation.reconciliation.enums.RoleType;
import com.jpb.reconciliation.reconciliation.enums.StandardRole;
import com.jpb.reconciliation.reconciliation.mapper.RecRoleMapper;
import com.jpb.reconciliation.reconciliation.repository.RecModuleRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RecRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    @Override
    @Transactional(noRollbackFor = {Exception.class})
    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {
        try {
            if (req.getRoleNames() == null || req.getRoleNames().isEmpty()) {
                return RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Please select at least one role name")
                        .data(Collections.emptyList())
                        .build();
            }

            RoleType   roleType   = parseRoleType(req.getRoleType());
            RoleStatus roleStatus = parseRoleStatus(req.getStatus());

            compatibilityValidator.validate(req.getRoleNames());

            if (roleType == RoleType.EXTERNAL) {
                validateExternalFields(req);
            }

            Set<RecRoleMaster> masters = req.getRoleNames().stream()
                    .map(this::resolveRoleMaster)
                    .collect(Collectors.toSet());

            String combinedName = req.getRoleNames().stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.joining(" + "));

            if (roleRepo.existsByRoleName(combinedName)) {
                return RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Role '" + combinedName + "' already exists")
                        .data(Collections.emptyList())
                        .build();
            }

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

            RecRole role = RecRole.builder()
                    .roleName(combinedName)
                    .roleCode(generatedRoleCode)
                    .roleType(roleType.name())
                    .status(roleStatus.name())
                    .description(req.getDescription())
                    .department(req.getDepartment())
                    .sessionTimeout(req.getSessionTimeout())
                    .validFrom(req.getValidFrom())
                    .validTo(req.getValidTo())
                    .createdBy(req.getCreatedBy())
                    .assignedUserId(req.getAssignedUserId())
                    .assignedUserName(req.getAssignedUserName())
                    .assignedUserEmail(req.getAssignedUserEmail())
                    .externalDepartmentName(roleType == RoleType.EXTERNAL ? req.getExternalDepartmentName() : null)
                    .externalSupervisorName(roleType == RoleType.EXTERNAL ? req.getExternalSupervisorName() : null)
                    .externalSupervisorEmail(roleType == RoleType.EXTERNAL ? req.getExternalSupervisorEmail() : null)
                    .externalSupervisorPhone(roleType == RoleType.EXTERNAL ? req.getExternalSupervisorPhone() : null)
                    .build();

            masters.forEach(role::addRoleMaster);

            if (req.getPermissions() != null) {
                req.getPermissions().forEach(p -> role.addPermission(buildPermission(p)));
            }

            RecRole saved = roleRepo.save(role);
            roleRepo.flush();

            RecRole withCode = roleRepo.findByIdWithPermissions(saved.getId())
                    .orElseThrow(() -> new RuntimeException("Role not found after save, id=" + saved.getId()));

            log.info("Role created → id={}, roleCode={}", withCode.getId(), withCode.getRoleCode());

            RecRoleResponseDTO responseDTO = roleMapper.toResponseDTO(withCode);
            return RestWithStatusList.builder()
                    .status("SUCCESS")
                    .statusMsg("Role created successfully")
                    .data(Collections.singletonList(responseDTO))
                    .build();

        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate role constraint violation: {}", e.getMessage());
            return RestWithStatusList.builder()
                    .status("FAILURE")
                    .statusMsg("Role already exists — duplicate entry detected")
                    .data(Collections.emptyList())
                    .build();

        } catch (IllegalArgumentException e) {
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

    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getAllRoles() {
        List<RecRole> roles = roleRepo.findAll();
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Roles fetched successfully")
                .data(new ArrayList<>(roles))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RestWithStatusList getAllModules() {
        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Modules fetched successfully")
                .data(Collections.unmodifiableList(roleMapper.toModuleDTOList(moduleRepo.findAll())))
                .build();
    }

    @Override
    @Transactional(noRollbackFor = {Exception.class})
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

    private RecRoleModulePermission buildPermission(RecPermissionRowDTO p) {
        RecModule module = moduleRepo.findById(p.getModuleId())
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + p.getModuleId()));
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
            throw new IllegalArgumentException("Invalid roleType '" + raw + "'. Allowed: INTERNAL, EXTERNAL.");
        }
    }

    private RoleStatus parseRoleStatus(String raw) {
        try {
            return RoleStatus.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status '" + raw + "'. Allowed: " + Arrays.toString(RoleStatus.values()));
        }
    }

    private void validateExternalFields(RecCreateRoleRequestDTO req) {
        if (isBlank(req.getExternalDepartmentName()))
            throw new IllegalArgumentException("externalDepartmentName is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorName()))
            throw new IllegalArgumentException("externalSupervisorName is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorEmail()))
            throw new IllegalArgumentException("externalSupervisorEmail is required for EXTERNAL roles");
        if (isBlank(req.getExternalSupervisorPhone()))
            throw new IllegalArgumentException("externalSupervisorPhone is required for EXTERNAL roles");
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}
