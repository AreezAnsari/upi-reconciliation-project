//package com.jpb.reconciliation.reconciliation.service;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.dto.JpbModuleDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbPermissionDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleCreateRequest;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleResponse;
//import com.jpb.reconciliation.reconciliation.entity.JpbModule;
//import com.jpb.reconciliation.reconciliation.entity.JpbRole;
//import com.jpb.reconciliation.reconciliation.entity.JpbRolePermission;
//import com.jpb.reconciliation.reconciliation.mapper.JpbRoleMapper;
//import com.jpb.reconciliation.reconciliation.repository.JpbModuleRepository;
//import com.jpb.reconciliation.reconciliation.repository.JpbRolePermissionRepository;
//import com.jpb.reconciliation.reconciliation.repository.JpbRoleRepository;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Implementation of JpbRoleService
// * Handles all business logic for role and permission management
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class JpbRoleServiceImpl implements JpbRoleService {
//
//    private final JpbRoleRepository roleRepository;
//    private final JpbModuleRepository moduleRepository;
//    private final JpbRolePermissionRepository permissionRepository;
//    private final JpbRoleMapper roleMapper;
//
//    // ── Module Operations ──────────────────────────────────────
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<JpbModuleDTO> getAllModules() {
//        log.info("Fetching all modules");
//        try {
//            List<JpbModule> modules = moduleRepository.findAllByOrderByDisplayOrderAsc();
//            log.info("Successfully fetched {} modules", modules.size());
//            return roleMapper.toModuleDTOList(modules);
//        } catch (Exception e) {
//            log.error("Error fetching modules", e);
//            throw new RuntimeException("Failed to fetch modules: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public JpbModuleDTO getModuleById(Long moduleId) {
//        log.info("Fetching module with ID: {}", moduleId);
//        
//        if (moduleId == null || moduleId <= 0) {
//            throw new IllegalArgumentException("Invalid module ID: " + moduleId);
//        }
//
//        try {
//            JpbModule module = moduleRepository.findById(moduleId)
//                .orElseThrow(() -> {
//                    log.error("Module not found: {}", moduleId);
//                    return new RuntimeException("Module not found: " + moduleId);
//                });
//
//            log.info("Module found: {}", module.getName());
//            return roleMapper.toModuleDTO(module);
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error fetching module", e);
//            throw new RuntimeException("Failed to fetch module: " + e.getMessage(), e);
//        }
//    }
//
//    // ── Role CRUD Operations ───────────────────────────────────
//
//    @Override
//    @Transactional
//    public JpbRoleResponse createRole(JpbRoleCreateRequest request) {
//        log.info("========== CREATING NEW ROLE ==========");
//        log.info("Role Name: {}, Type: {}, Status: {}", 
//            request.getRoleName(), request.getRoleType(), request.getStatus());
//
//        try {
//            // Validate request
//            validateCreateRoleRequest(request);
//
//            // Check for duplicate role name
//            if (roleRepository.existsByRoleName(request.getRoleName())) {
//                String msg = "Role with name '" + request.getRoleName() + "' already exists";
//                log.error("Role creation failed: {}", msg);
//                throw new IllegalArgumentException(msg);
//            }
//
//            // Validate at least one permission has access
//            boolean anyAccess = request.getPermissions().stream()
//                .anyMatch(JpbPermissionDTO::isHasAccess);
//            if (!anyAccess) {
//                String msg = "At least one module must have access enabled";
//                log.error("Role creation failed: {}", msg);
//                throw new IllegalArgumentException(msg);
//            }
//
//            // Verify all modules exist
//            List<JpbModule> modules = new ArrayList<>();
//            for (JpbPermissionDTO permDTO : request.getPermissions()) {
//                JpbModule module = moduleRepository.findById(permDTO.getModuleId())
//                    .orElseThrow(() -> {
//                        log.error("Module not found: {}", permDTO.getModuleId());
//                        return new RuntimeException("Module not found: " + permDTO.getModuleId());
//                    });
//                modules.add(module);
//            }
//
//            // Create and save role
//            JpbRole role = roleMapper.toRoleEntity(request);
//            JpbRole savedRole = roleRepository.save(role);
//            log.info("Role saved with ID: {}", savedRole.getId());
//
//            // Create permissions map for easy access
//            Map<Long, JpbModule> moduleMap = modules.stream()
//                .collect(Collectors.toMap(JpbModule::getId, m -> m));
//
//            // Save permissions
//            savePermissions(savedRole, request.getPermissions(), moduleMap);
//
//            log.info("========== ROLE CREATED SUCCESSFULLY ==========");
//            log.info("Role ID: {}, Name: {}", savedRole.getId(), savedRole.getRoleName());
//
//            // Fetch permissions for response
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(savedRole.getId());
//            return roleMapper.toRoleResponse(savedRole, permissions);
//
//        } catch (IllegalArgumentException e) {
//            log.error("Validation error: {}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("Error creating role", e);
//            throw new RuntimeException("Failed to create role: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<JpbRoleResponse> getAllRoles() {
//        log.info("Fetching all roles");
//        try {
//            List<JpbRole> roles = roleRepository.findAll();
//            log.info("Found {} roles", roles.size());
//
//            // Fetch permissions for all roles
//            Map<Long, List<JpbRolePermission>> permissionsByRoleId = new HashMap<>();
//            for (JpbRole role : roles) {
//                permissionsByRoleId.put(role.getId(), permissionRepository.findByRoleId(role.getId()));
//            }
//
//            return roleMapper.toRoleResponseList(roles, permissionsByRoleId);
//
//        } catch (Exception e) {
//            log.error("Error fetching all roles", e);
//            throw new RuntimeException("Failed to fetch roles: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<JpbRoleResponse> getRolesByStatus(String status) {
//        log.info("Fetching roles with status: {}", status);
//
//        if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
//            throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE");
//        }
//
//        try {
//            List<JpbRole> roles = roleRepository.findByStatus(status);
//            log.info("Found {} roles with status: {}", roles.size(), status);
//
//            // Fetch permissions for all roles
//            Map<Long, List<JpbRolePermission>> permissionsByRoleId = new HashMap<>();
//            for (JpbRole role : roles) {
//                permissionsByRoleId.put(role.getId(), permissionRepository.findByRoleId(role.getId()));
//            }
//
//            return roleMapper.toRoleResponseList(roles, permissionsByRoleId);
//
//        } catch (Exception e) {
//            log.error("Error fetching roles by status", e);
//            throw new RuntimeException("Failed to fetch roles: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<JpbRoleResponse> getRolesByType(String roleType) {
//        log.info("Fetching roles with type: {}", roleType);
//
//        if (roleType == null || (!roleType.equals("INTERNAL") && !roleType.equals("EXTERNAL"))) {
//            throw new IllegalArgumentException("Role type must be INTERNAL or EXTERNAL");
//        }
//
//        try {
//            List<JpbRole> roles = roleRepository.findByRoleType(roleType);
//            log.info("Found {} roles with type: {}", roles.size(), roleType);
//
//            // Fetch permissions for all roles
//            Map<Long, List<JpbRolePermission>> permissionsByRoleId = new HashMap<>();
//            for (JpbRole role : roles) {
//                permissionsByRoleId.put(role.getId(), permissionRepository.findByRoleId(role.getId()));
//            }
//
//            return roleMapper.toRoleResponseList(roles, permissionsByRoleId);
//
//        } catch (Exception e) {
//            log.error("Error fetching roles by type", e);
//            throw new RuntimeException("Failed to fetch roles: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public JpbRoleResponse getRoleById(Long roleId) {
//        log.info("Fetching role with ID: {}", roleId);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        try {
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> {
//                    log.error("Role not found: {}", roleId);
//                    return new RuntimeException("Role not found: " + roleId);
//                });
//
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(roleId);
//            log.info("Role found: {}", role.getRoleName());
//
//            return roleMapper.toRoleResponse(role, permissions);
//
//        } catch (Exception e) {
//            log.error("Error fetching role", e);
//            throw new RuntimeException("Failed to fetch role: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional
//    public JpbRoleResponse updateRole(Long roleId, JpbRoleCreateRequest request) {
//        log.info("========== UPDATING ROLE ==========");
//        log.info("Role ID: {}", roleId);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        try {
//            // Fetch existing role
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            log.info("Updating role: {}", role.getRoleName());
//
//            // Validate request
//            validateCreateRoleRequest(request);
//
//            // Check for duplicate (if name changed)
//            if (!role.getRoleName().equals(request.getRoleName()) 
//                && roleRepository.existsByRoleName(request.getRoleName())) {
//                throw new IllegalArgumentException("Role name already exists");
//            }
//
//            // Update role fields
//            role.setRoleName(request.getRoleName());
//            role.setRoleType(request.getRoleType());
//            role.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
//            role.setDescription(request.getDescription());
//            role.setUpdatedBy(request.getCreatedBy());
//            role.setUpdatedAt(LocalDateTime.now());
//
//            JpbRole updatedRole = roleRepository.save(role);
//            log.info("Role updated successfully");
//
//            // Delete old permissions and save new ones
//            permissionRepository.deleteByRoleId(roleId);
//            
//            // Get modules for permission creation
//            List<JpbModule> modules = request.getPermissions().stream()
//                .map(perm -> moduleRepository.findById(perm.getModuleId()).orElse(null))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//            
//            Map<Long, JpbModule> moduleMap = modules.stream()
//                .collect(Collectors.toMap(JpbModule::getId, m -> m));
//
//            savePermissions(updatedRole, request.getPermissions(), moduleMap);
//
//            log.info("========== ROLE UPDATED SUCCESSFULLY ==========");
//
//            // Fetch permissions for response
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(updatedRole.getId());
//            return roleMapper.toRoleResponse(updatedRole, permissions);
//
//        } catch (Exception e) {
//            log.error("Error updating role", e);
//            throw new RuntimeException("Failed to update role: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional
//    public JpbRoleResponse updateRoleStatus(Long roleId, String status) {
//        log.info("Updating role status - Role ID: {}, New Status: {}", roleId, status);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
//            throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE");
//        }
//
//        try {
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            role.setStatus(status);
//            role.setUpdatedAt(LocalDateTime.now());
//
//            JpbRole updatedRole = roleRepository.save(role);
//            log.info("Role status updated successfully");
//
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(roleId);
//            return roleMapper.toRoleResponse(updatedRole, permissions);
//
//        } catch (Exception e) {
//            log.error("Error updating role status", e);
//            throw new RuntimeException("Failed to update role status: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void deleteRole(Long roleId) {
//        log.info("========== DELETING ROLE ==========");
//        log.info("Role ID: {}", roleId);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        try {
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            log.info("Deleting role: {}", role.getRoleName());
//
//            // Delete permissions first (cascade handles this, but explicit for clarity)
//            permissionRepository.deleteByRoleId(roleId);
//
//            // Delete role
//            roleRepository.deleteById(roleId);
//            log.info("========== ROLE DELETED SUCCESSFULLY ==========");
//
//        } catch (Exception e) {
//            log.error("Error deleting role", e);
//            throw new RuntimeException("Failed to delete role: " + e.getMessage(), e);
//        }
//    }
//
//    // ── Permission Operations ──────────────────────────────────
//
//    @Override
//    @Transactional
//    public JpbRoleResponse updatePermissions(Long roleId, List<JpbPermissionDTO> permissions) {
//        log.info("Updating permissions for role ID: {}", roleId);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        try {
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            // Delete old permissions
//            permissionRepository.deleteByRoleId(roleId);
//
//            // Get modules
//            List<JpbModule> modules = permissions.stream()
//                .map(perm -> moduleRepository.findById(perm.getModuleId()).orElse(null))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//
//            Map<Long, JpbModule> moduleMap = modules.stream()
//                .collect(Collectors.toMap(JpbModule::getId, m -> m));
//
//            // Save new permissions
//            savePermissions(role, permissions, moduleMap);
//
//            log.info("Permissions updated successfully");
//
//            List<JpbRolePermission> savedPermissions = permissionRepository.findByRoleId(roleId);
//            return roleMapper.toRoleResponse(role, savedPermissions);
//
//        } catch (Exception e) {
//            log.error("Error updating permissions", e);
//            throw new RuntimeException("Failed to update permissions: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<JpbPermissionDTO> getRolePermissions(Long roleId) {
//        log.info("Fetching permissions for role ID: {}", roleId);
//
//        if (roleId == null || roleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID: " + roleId);
//        }
//
//        try {
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(roleId);
//            log.info("Found {} permissions", permissions.size());
//            return roleMapper.toPermissionDTOList(permissions);
//
//        } catch (Exception e) {
//            log.error("Error fetching permissions", e);
//            throw new RuntimeException("Failed to fetch permissions: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean hasAccess(Long roleId, Long moduleId) {
//        log.debug("Checking access - Role ID: {}, Module ID: {}", roleId, moduleId);
//
//        if (roleId == null || roleId <= 0 || moduleId == null || moduleId <= 0) {
//            return false;
//        }
//
//        try {
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(roleId);
//            return permissions.stream()
//                .anyMatch(p -> p.getModule().getId().equals(moduleId) && p.isHasAccess());
//
//        } catch (Exception e) {
//            log.error("Error checking access", e);
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional
//    public JpbPermissionDTO grantPermission(Long roleId, Long moduleId, JpbPermissionDTO permission) {
//        log.info("Granting permission - Role ID: {}, Module ID: {}", roleId, moduleId);
//
//        if (roleId == null || roleId <= 0 || moduleId == null || moduleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID or module ID");
//        }
//
//        try {
//            JpbRole role = roleRepository.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//            JpbModule module = moduleRepository.findById(moduleId)
//                .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
//
//            JpbRolePermission rolePermission = roleMapper.toPermissionEntity(permission, role, module);
//            JpbRolePermission savedPermission = permissionRepository.save(rolePermission);
//
//            log.info("Permission granted successfully");
//            return roleMapper.toPermissionDTO(savedPermission);
//
//        } catch (Exception e) {
//            log.error("Error granting permission", e);
//            throw new RuntimeException("Failed to grant permission: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void revokePermission(Long roleId, Long moduleId) {
//        log.info("Revoking permission - Role ID: {}, Module ID: {}", roleId, moduleId);
//
//        if (roleId == null || roleId <= 0 || moduleId == null || moduleId <= 0) {
//            throw new IllegalArgumentException("Invalid role ID or module ID");
//        }
//
//        try {
//            // Get all permissions and delete the specific one
//            List<JpbRolePermission> permissions = permissionRepository.findByRoleId(roleId);
//            
//            JpbRolePermission permToRevoke = permissions.stream()
//                .filter(p -> p.getModule().getId().equals(moduleId))
//                .findFirst()
//                .orElse(null);
//
//            if (permToRevoke != null) {
//                permissionRepository.delete(permToRevoke);
//                log.info("Permission revoked successfully");
//            } else {
//                log.warn("Permission not found to revoke");
//            }
//
//        } catch (Exception e) {
//            log.error("Error revoking permission", e);
//            throw new RuntimeException("Failed to revoke permission: " + e.getMessage(), e);
//        }
//    }
//
//    // ── Private Helper Methods ────────────────────────────────
//
//    /**
//     * Validate create role request
//     * @param request The request to validate
//     */
//    private void validateCreateRoleRequest(JpbRoleCreateRequest request) {
//        if (request == null) {
//            throw new IllegalArgumentException("Request cannot be null");
//        }
//
//        if (request.getRoleName() == null || request.getRoleName().trim().isEmpty()) {
//            throw new IllegalArgumentException("Role name is required");
//        }
//
//        if (request.getRoleType() == null 
//            || (!request.getRoleType().equals("INTERNAL") && !request.getRoleType().equals("EXTERNAL"))) {
//            throw new IllegalArgumentException("Role type must be INTERNAL or EXTERNAL");
//        }
//
//        if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
//            throw new IllegalArgumentException("At least one permission is required");
//        }
//    }
//
//    /**
//     * Save permissions for a role
//     * @param role The role entity
//     * @param permissionDTOs List of permission DTOs
//     * @param moduleMap Map of module ID to module entity
//     */
//    private void savePermissions(
//        JpbRole role,
//        List<JpbPermissionDTO> permissionDTOs,
//        Map<Long, JpbModule> moduleMap) {
//        
//        log.debug("Saving {} permissions for role: {}", permissionDTOs.size(), role.getRoleName());
//
//        for (JpbPermissionDTO permDTO : permissionDTOs) {
//            JpbModule module = moduleMap.get(permDTO.getModuleId());
//            if (module == null) {
//                log.warn("Module not found for permission, skipping: {}", permDTO.getModuleId());
//                continue;
//            }
//
//            JpbRolePermission permission = roleMapper.toPermissionEntity(permDTO, role, module);
//            permissionRepository.save(permission);
//            
//            log.debug("Permission saved - Module: {}, Access: {}", 
//                module.getName(), permDTO.isHasAccess());
//        }
//    }
//}