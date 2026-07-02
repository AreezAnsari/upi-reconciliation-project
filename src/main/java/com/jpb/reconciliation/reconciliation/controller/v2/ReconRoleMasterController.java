package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.service.v2.ReconRoleMasterService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recon-roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReconRoleMasterController {

    private final ReconRoleMasterService reconRoleMasterService;

    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(
            @RequestBody ReconRoleMaster role,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.createRole(role, userDetails.getUsername());
    }

    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        return reconRoleMasterService.getAllRoles();
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> getRoleById(@PathVariable Long roleId) {
        return reconRoleMasterService.getRoleById(roleId);
    }

    @GetMapping("/by-status")
    public ResponseEntity<RestWithStatusList> getRolesByStatus(@RequestParam String status) {
        return reconRoleMasterService.getRolesByStatus(status);
    }

    @GetMapping("/by-type")
    public ResponseEntity<RestWithStatusList> getRolesByType(@RequestParam String roleType) {
        return reconRoleMasterService.getRolesByType(roleType);
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> updateRole(
            @PathVariable Long roleId,
            @RequestBody ReconRoleMaster role,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.updateRole(roleId, role, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/submit")
    public ResponseEntity<RestWithStatusList> submitForApproval(
            @PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.submitForApproval(roleId, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/approve")
    public ResponseEntity<RestWithStatusList> approveRole(
            @PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.approveRole(roleId, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/status")
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long roleId,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.updateStatus(roleId, status, userDetails.getUsername());
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> deleteRole(@PathVariable Long roleId) {
        return reconRoleMasterService.deleteRole(roleId);
    }

    @GetMapping("/check-code")
    public ResponseEntity<RestWithStatusList> checkRoleCodeExists(@RequestParam String roleCode) {
        return reconRoleMasterService.checkRoleCodeExists(roleCode);
    }
}
