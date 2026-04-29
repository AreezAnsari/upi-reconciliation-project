//package com.jpb.reconciliation.reconciliation.controller;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import com.jpb.reconciliation.reconciliation.dto.AccessCheckResponse;
//import com.jpb.reconciliation.reconciliation.dto.JpbModuleDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbPermissionDTO;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleCreateRequest;
//import com.jpb.reconciliation.reconciliation.dto.JpbRoleResponse;
//import com.jpb.reconciliation.reconciliation.service.JpbRoleService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/roles")
//@CrossOrigin(origins = "*")
//@RequiredArgsConstructor
//@Slf4j
//public class JpbRoleController {
//
//    private final JpbRoleService roleService;
//
//    @GetMapping("/modules")
//    public ResponseEntity<List<JpbModuleDTO>> getAllModules() {
//        log.info("GET /api/v1/roles/modules");
//        List<JpbModuleDTO> modules = roleService.getAllModules();
//        return ResponseEntity.ok(modules);
//    }
//
//    @GetMapping("/modules/{id}")
//    public ResponseEntity<JpbModuleDTO> getModuleById(@PathVariable Long id) {
//        log.info("GET /api/v1/roles/modules/{}", id);
//        JpbModuleDTO module = roleService.getModuleById(id);
//        return ResponseEntity.ok(module);
//    }
//
//    @PostMapping
//    public ResponseEntity<JpbRoleResponse> createRole(@RequestBody JpbRoleCreateRequest request) {
//        log.info("POST /api/v1/roles");
//        JpbRoleResponse response = roleService.createRole(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @GetMapping
//    public ResponseEntity<List<JpbRoleResponse>> getAllRoles() {
//        log.info("GET /api/v1/roles");
//        List<JpbRoleResponse> roles = roleService.getAllRoles();
//        return ResponseEntity.ok(roles);
//    }
//
//    @GetMapping("/status/{status}")
//    public ResponseEntity<List<JpbRoleResponse>> getRolesByStatus(@PathVariable String status) {
//        log.info("GET /api/v1/roles/status/{}", status);
//        List<JpbRoleResponse> roles = roleService.getRolesByStatus(status);
//        return ResponseEntity.ok(roles);
//    }
//
//    @GetMapping("/type/{type}")
//    public ResponseEntity<List<JpbRoleResponse>> getRolesByType(@PathVariable String type) {
//        log.info("GET /api/v1/roles/type/{}", type);
//        List<JpbRoleResponse> roles = roleService.getRolesByType(type);
//        return ResponseEntity.ok(roles);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<JpbRoleResponse> getRoleById(@PathVariable Long id) {
//        log.info("GET /api/v1/roles/{}", id);
//        JpbRoleResponse role = roleService.getRoleById(id);
//        return ResponseEntity.ok(role);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<JpbRoleResponse> updateRole(
//        @PathVariable Long id,
//        @RequestBody JpbRoleCreateRequest request) {
//        log.info("PUT /api/v1/roles/{}", id);
//        JpbRoleResponse response = roleService.updateRole(id, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
//        log.info("DELETE /api/v1/roles/{}", id);
//        roleService.deleteRole(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/{id}/permissions")
//    public ResponseEntity<List<JpbPermissionDTO>> getRolePermissions(@PathVariable Long id) {
//        log.info("GET /api/v1/roles/{}/permissions", id);
//        List<JpbPermissionDTO> permissions = roleService.getRolePermissions(id);
//        return ResponseEntity.ok(permissions);
//    }
//
//    @PutMapping("/{id}/permissions")
//    public ResponseEntity<JpbRoleResponse> updatePermissions(
//        @PathVariable Long id,
//        @RequestBody List<JpbPermissionDTO> permissions) {
//        log.info("PUT /api/v1/roles/{}/permissions", id);
//        JpbRoleResponse response = roleService.updatePermissions(id, permissions);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{roleId}/permissions/{moduleId}/check")
//    public ResponseEntity<AccessCheckResponse> hasAccess(
//        @PathVariable Long roleId,
//        @PathVariable Long moduleId) {
//        log.info("GET /api/v1/roles/{}/permissions/{}/check", roleId, moduleId);
//        boolean hasAccess = roleService.hasAccess(roleId, moduleId);
//        return ResponseEntity.ok(
//            AccessCheckResponse.builder()
//                .roleId(roleId)
//                .moduleId(moduleId)
//                .hasAccess(hasAccess)
//                .build()
//        );
//    }
//
//    @PostMapping("/{roleId}/permissions/{moduleId}")
//    public ResponseEntity<JpbPermissionDTO> grantPermission(
//        @PathVariable Long roleId,
//        @PathVariable Long moduleId,
//        @RequestBody JpbPermissionDTO permission) {
//        log.info("POST /api/v1/roles/{}/permissions/{}", roleId, moduleId);
//        JpbPermissionDTO response = roleService.grantPermission(roleId, moduleId, permission);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @DeleteMapping("/{roleId}/permissions/{moduleId}")
//    public ResponseEntity<Void> revokePermission(
//        @PathVariable Long roleId,
//        @PathVariable Long moduleId) {
//        log.info("DELETE /api/v1/roles/{}/permissions/{}", roleId, moduleId);
//        roleService.revokePermission(roleId, moduleId);
//        return ResponseEntity.noContent().build();
//    }
//}