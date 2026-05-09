//package com.jpb.reconciliation.reconciliation.service;
//
//
//import java.util.List;
//
//import com.jpb.reconciliation.reconciliation.dto.JpbModuleDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbPermissionDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleCreateRequest;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleResponse;
//
//public interface JpbRoleService {
//
//    List<JpbModuleDTO> getAllModules();
//    JpbModuleDTO getModuleById(Long moduleId);
//    
//    JpbRoleResponse createRole(JpbRoleCreateRequest request);
//    List<JpbRoleResponse> getAllRoles();
//    List<JpbRoleResponse> getRolesByStatus(String status);
//    List<JpbRoleResponse> getRolesByType(String roleType);
//    JpbRoleResponse getRoleById(Long roleId);
//    JpbRoleResponse updateRole(Long roleId, JpbRoleCreateRequest request);
//    JpbRoleResponse updateRoleStatus(Long roleId, String status);
//    void deleteRole(Long roleId);
//    
//    JpbRoleResponse updatePermissions(Long roleId, List<JpbPermissionDTO> permissions);
//    List<JpbPermissionDTO> getRolePermissions(Long roleId);
//    boolean hasAccess(Long roleId, Long moduleId);
//    JpbPermissionDTO grantPermission(Long roleId, Long moduleId, JpbPermissionDTO permission);
//    void revokePermission(Long roleId, Long moduleId);
//}