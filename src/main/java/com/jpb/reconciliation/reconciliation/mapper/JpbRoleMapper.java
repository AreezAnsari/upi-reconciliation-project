//package com.jpb.reconciliation.reconciliation.mapper;
//
//import org.springframework.stereotype.Component;
//
//import com.jpb.reconciliation.reconciliation.dto.JpbModuleDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbPermissionDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleCreateRequest;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleResponse;
//import com.jpb.reconciliation.reconciliation.entity.JpbModule;
//import com.jpb.reconciliation.reconciliation.entity.JpbRole;
//import com.jpb.reconciliation.reconciliation.entity.JpbRolePermission;
//
//import java.util.List;
//import java.util.stream.Collectors;
// 
///**
// * Mapper for converting between Entity and DTO classes
// * Handles all conversions: Entity → DTO and DTO → Entity
// */
//@Component
//public class JpbRoleMapper {
// 
//    // ── Module Mappings ────────────────────────────────────────
// 
//    /**
//     * Convert JpbModule entity to JpbModuleDTO
//     * @param module Module entity
//     * @return Module DTO
//     */
//    public JpbModuleDTO toModuleDTO(JpbModule module) {
//        if (module == null) {
//            return null;
//        }
// 
//        return JpbModuleDTO.builder()
//            .id(module.getId())
//            .name(module.getName())
//            .description(module.getDescription())
//            .displayOrder(module.getDisplayOrder())
//            .build();
//    }
// 
//    /**
//     * Convert JpbModuleDTO to JpbModule entity
//     * @param dto Module DTO
//     * @return Module entity
//     */
//    public JpbModule toModuleEntity(JpbModuleDTO dto) {
//        if (dto == null) {
//            return null;
//        }
// 
//        return JpbModule.builder()
//            .id(dto.getId())
//            .name(dto.getName())
//            .description(dto.getDescription())
//            .displayOrder(dto.getDisplayOrder())
//            .build();
//    }
// 
//    /**
//     * Convert list of modules to DTOs
//     * @param modules List of module entities
//     * @return List of module DTOs
//     */
//    public List<JpbModuleDTO> toModuleDTOList(List<JpbModule> modules) {
//        if (modules == null) {
//            return null;
//        }
// 
//        return modules.stream()
//            .map(this::toModuleDTO)
//            .collect(Collectors.toList());
//    }
// 
//    // ── Permission Mappings ────────────────────────────────────
// 
//    /**
//     * Convert JpbRolePermission entity to JpbPermissionDTO
//     * @param permission Permission entity
//     * @return Permission DTO
//     */
//    public JpbPermissionDTO toPermissionDTO(JpbRolePermission permission) {
//        if (permission == null) {
//            return null;
//        }
// 
//        return JpbPermissionDTO.builder()
//            .moduleId(permission.getModule().getId())
//            .moduleName(permission.getModule().getName())
//            .hasAccess(permission.isHasAccess())
//            .canView(permission.isCanView())
//            .canCreate(permission.isCanCreate())
//            .canEdit(permission.isCanEdit())
//            .canApprove(permission.isCanApprove())
//            .canDownload(permission.isCanDownload())
//            .build();
//    }
// 
//    /**
//     * Convert list of permissions to DTOs
//     * @param permissions List of permission entities
//     * @return List of permission DTOs
//     */
//    public List<JpbPermissionDTO> toPermissionDTOList(List<JpbRolePermission> permissions) {
//        if (permissions == null) {
//            return null;
//        }
// 
//        return permissions.stream()
//            .map(this::toPermissionDTO)
//            .collect(Collectors.toList());
//    }
// 
//    // ── Role Mappings ────────────────────────────────────────
// 
//    /**
//     * Convert JpbRole entity to JpbRoleResponse DTO
//     * Includes permissions if available
//     * @param role Role entity
//     * @param permissions List of permissions for the role
//     * @return Role response DTO
//     */
//    public JpbRoleResponse toRoleResponse(JpbRole role, List<JpbRolePermission> permissions) {
//        if (role == null) {
//            return null;
//        }
// 
//        List<JpbPermissionDTO> permissionDTOs = permissions != null 
//            ? toPermissionDTOList(permissions) 
//            : null;
// 
//        return JpbRoleResponse.builder()
//            .id(role.getId())
//            .roleName(role.getRoleName())
//            .roleType(role.getRoleType())
//            .status(role.getStatus())
//            .description(role.getDescription())
//            .createdBy(role.getCreatedBy())
//            .createdAt(role.getCreatedAt())
//            .updatedBy(role.getUpdatedBy())
//            .updatedAt(role.getUpdatedAt())
//            .permissions(permissionDTOs)
//            .build();
//    }
// 
//    /**
//     * Convert JpbRole entity to JpbRoleResponse DTO (without permissions)
//     * @param role Role entity
//     * @return Role response DTO
//     */
//    public JpbRoleResponse toRoleResponse(JpbRole role) {
//        return toRoleResponse(role, null);
//    }
// 
//    /**
//     * Convert JpbRoleCreateRequest DTO to JpbRole entity
//     * @param request Create role request
//     * @return Role entity
//     */
//    public JpbRole toRoleEntity(JpbRoleCreateRequest request) {
//        if (request == null) {
//            return null;
//        }
// 
//        return JpbRole.builder()
//            .roleName(request.getRoleName())
//            .roleType(request.getRoleType())
//            .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
//            .description(request.getDescription())
//            .createdBy(request.getCreatedBy())
//            .build();
//    }
// 
//    /**
//     * Convert list of roles to response DTOs
//     * @param roles List of role entities
//     * @param permissionsByRoleId Map of role ID to permissions
//     * @return List of role response DTOs
//     */
//    public List<JpbRoleResponse> toRoleResponseList(
//        List<JpbRole> roles, 
//        java.util.Map<Long, List<JpbRolePermission>> permissionsByRoleId) {
//        
//        if (roles == null) {
//            return null;
//        }
// 
//        return roles.stream()
//            .map(role -> toRoleResponse(
//                role,
//                permissionsByRoleId != null ? permissionsByRoleId.get(role.getId()) : null
//            ))
//            .collect(Collectors.toList());
//    }
// 
//    /**
//     * Convert list of roles to response DTOs (without permissions)
//     * @param roles List of role entities
//     * @return List of role response DTOs
//     */
//    public List<JpbRoleResponse> toRoleResponseList(List<JpbRole> roles) {
//        return toRoleResponseList(roles, null);
//    }
// 
//    // ── Permission Entity Mappings ─────────────────────────────
// 
//    /**
//     * Convert JpbPermissionDTO to JpbRolePermission entity
//     * Used for creating/updating permissions
//     * @param permissionDTO Permission DTO
//     * @param role Role entity
//     * @param module Module entity
//     * @return Permission entity
//     */
//    public JpbRolePermission toPermissionEntity(
//        JpbPermissionDTO permissionDTO,
//        JpbRole role,
//        JpbModule module) {
//        
//        if (permissionDTO == null || role == null || module == null) {
//            return null;
//        }
// 
//        boolean hasAccess = permissionDTO.isHasAccess();
// 
//        return JpbRolePermission.builder()
//            .role(role)
//            .module(module)
//            .hasAccess(hasAccess)
//            .canView(hasAccess && permissionDTO.isCanView())
//            .canCreate(hasAccess && permissionDTO.isCanCreate())
//            .canEdit(hasAccess && permissionDTO.isCanEdit())
//            .canApprove(hasAccess && permissionDTO.isCanApprove())
//            .canDownload(hasAccess && permissionDTO.isCanDownload())
//            .build();
//    }
// 
//    /**
//     * Convert list of permission DTOs to entities
//     * @param permissionDTOs List of permission DTOs
//     * @param role Role entity
//     * @param moduleMap Map of module ID to module entity
//     * @return List of permission entities
//     */
//    public List<JpbRolePermission> toPermissionEntityList(
//        List<JpbPermissionDTO> permissionDTOs,
//        JpbRole role,
//        java.util.Map<Long, JpbModule> moduleMap) {
//        
//        if (permissionDTOs == null || role == null || moduleMap == null) {
//            return null;
//        }
// 
//        return permissionDTOs.stream()
//            .map(dto -> toPermissionEntity(dto, role, moduleMap.get(dto.getModuleId())))
//            .filter(entity -> entity != null)
//            .collect(Collectors.toList());
//    }
// 
//    // ── Utility Methods ────────────────────────────────────────
// 
//    /**
//     * Check if two permission DTOs are equal in terms of permissions
//     * @param dto1 First permission DTO
//     * @param dto2 Second permission DTO
//     * @return true if permissions are identical
//     */
//    public boolean arePermissionsEqual(JpbPermissionDTO dto1, JpbPermissionDTO dto2) {
//        if (dto1 == null || dto2 == null) {
//            return dto1 == dto2;
//        }
// 
//        return dto1.isHasAccess() == dto2.isHasAccess()
//            && dto1.isCanView() == dto2.isCanView()
//            && dto1.isCanCreate() == dto2.isCanCreate()
//            && dto1.isCanEdit() == dto2.isCanEdit()
//            && dto1.isCanApprove() == dto2.isCanApprove()
//            && dto1.isCanDownload() == dto2.isCanDownload();
//    }
// 
//    /**
//     * Update permission entity from DTO
//     * @param permissionDTO Source DTO
//     * @param permission Target permission entity to update
//     */
//    public void updatePermissionFromDTO(JpbPermissionDTO permissionDTO, JpbRolePermission permission) {
//        if (permissionDTO == null || permission == null) {
//            return;
//        }
// 
//        boolean hasAccess = permissionDTO.isHasAccess();
// 
//        permission.setHasAccess(hasAccess);
//        permission.setCanView(hasAccess && permissionDTO.isCanView());
//        permission.setCanCreate(hasAccess && permissionDTO.isCanCreate());
//        permission.setCanEdit(hasAccess && permissionDTO.isCanEdit());
//        permission.setCanApprove(hasAccess && permissionDTO.isCanApprove());
//        permission.setCanDownload(hasAccess && permissionDTO.isCanDownload());
//    }
//}