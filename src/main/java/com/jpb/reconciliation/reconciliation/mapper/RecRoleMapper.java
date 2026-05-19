package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.entity.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RecRoleMapper {
	
	// ── Primary method used by service ────────────────────────────────────────
    // Builds the full response from a RecRole that already has permissions loaded.
    // Called after findByIdWithPermissions() which eager-fetches everything.
    public RecRoleResponseDTO toResponseDTO(RecRole role) {
        return RecRoleResponseDTO.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .assignedRoles(toMasterDTOList(role))   // all masters from join table
                .roleType(role.getRoleType())
                .status(role.getStatus())
                .externalDepartmentName(role.getExternalDepartmentName())
                .externalSupervisorName(role.getExternalSupervisorName())
                .externalSupervisorEmail(role.getExternalSupervisorEmail())
                .externalSupervisorPhone(role.getExternalSupervisorPhone())
                .assignedUserId(role.getAssignedUserId())
                .assignedUserName(role.getAssignedUserName())
                .assignedUserEmail(role.getAssignedUserEmail())
                .description(role.getDescription())
                .validFrom(role.getValidFrom())
                .validTo(role.getValidTo())
                .createdBy(role.getCreatedBy())
                .createdAt(role.getCreatedAt())
                .permissions(toPermissionDTOList(role))
                .build();
    }

    public static RecRoleResponseDTO toResponse(
            RecRole role,
            List<RecRoleModulePermission> perms) {

        List<RecPermissionRowDTO> permissionList = perms.stream()
                .map(p -> RecPermissionRowDTO.builder()
                        .moduleId(p.getModule().getId())
                        .moduleName(p.getModule().getName())
                        .hasAccess(p.isHasAccess())
                        .canView(p.isCanView())
                        .canCreate(p.isCanCreate())
                        .canEdit(p.isCanEdit())
                        .canApprove(p.isCanApprove())
                        .canDownload(p.isCanDownload())
                        .build())
                .collect(Collectors.toList());

        return RecRoleResponseDTO.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .roleType(role.getRoleType())
                .status(role.getStatus())
                .externalDepartmentName(role.getExternalDepartmentName())
                .externalSupervisorName(role.getExternalSupervisorName())
                .externalSupervisorEmail(role.getExternalSupervisorEmail())
                .externalSupervisorPhone(role.getExternalSupervisorPhone())
                .assignedUserId(role.getAssignedUserId())
                .assignedUserName(role.getAssignedUserName())
                .assignedUserEmail(role.getAssignedUserEmail())
                .description(role.getDescription())
                .validFrom(role.getValidFrom())
                .validTo(role.getValidTo())
                .createdBy(role.getCreatedBy())
                .createdAt(role.getCreatedAt())
                .permissions(permissionList)
                .build();
    }
    
    // ── RecRoleMaster → RecRoleMasterDTO ──────────────────────────────────────
    
    public RecRoleMasterDTO toMasterDTO(RecRoleMaster master) {
        return RecRoleMasterDTO.builder()
                .id(master.getId())
                .roleName(master.getRoleName())
                .roleCode(master.getRoleCode())
                .isSystemRole(master.getIsSystemRole())
                .status(master.getStatus())
                .build();
    }
 
    // ── RecModule → RecModuleDTO ───────────────────────────────────────────────
 
    public RecModuleDTO toModuleDTO(RecModule module) {
        return RecModuleDTO.builder()
                .id(module.getId())
                .moduleName(module.getName())
                .build();
    }
 
    public List<RecModuleDTO> toModuleDTOList(List<RecModule> modules) {
        if (modules == null) return Collections.emptyList();
        return modules.stream()
                .map(this::toModuleDTO)
                .collect(Collectors.toList());
    }
 
    // ── Private helpers ───────────────────────────────────────────────────────
 
    private List<RecRoleMasterDTO> toMasterDTOList(RecRole role) {
        if (role.getRoleMasters() == null || role.getRoleMasters().isEmpty()) {
            return Collections.emptyList();
        }
        return role.getRoleMasters().stream()
                .sorted(Comparator.comparing(RecRoleMaster::getRoleCode))
                .map(this::toMasterDTO)
                .collect(Collectors.toList());
    }
 
    private List<RecPermissionRowDTO> toPermissionDTOList(RecRole role) {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            return Collections.emptyList();
        }
        return role.getPermissions().stream()
                .map(this::toPermissionDTO)
                .collect(Collectors.toList());
    }
 
    private RecPermissionRowDTO toPermissionDTO(RecRoleModulePermission p) {
        return RecPermissionRowDTO.builder()
                .moduleId(p.getModule()    != null ? p.getModule().getId()         : null)
                .moduleName(p.getModule()  != null ? p.getModule().getName() : null)
                .hasAccess(p.isHasAccess())
                .canView(p.isCanView())
                .canCreate(p.isCanCreate())
                .canEdit(p.isCanEdit())
                .canApprove(p.isCanApprove())
                .canDownload(p.isCanDownload())
                .build();
    }
}