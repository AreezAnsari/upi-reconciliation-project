package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RoleApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.TestRoleBulkApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.RoleCreateRequest;
import com.jpb.reconciliation.reconciliation.service.MenuMasterService;
import com.jpb.reconciliation.reconciliation.service.TestRoleManageService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/")
public class TestRoleController {

    @Autowired
    TestRoleManageService roleManageService;

    @Autowired
    MenuMasterService menuMasterService;

    @GetMapping(value = "get-all-role", produces = CommonConstants.APPLICATION_JSON)
    ResponseEntity<RestWithStatusList> getAllRole() {
        return roleManageService.getAllRoleDetails();
    }

    @GetMapping(value = "/getrole-loginuser/{roleId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenuByRole(@PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long verifiedRoleId = menuMasterService.getVerifiedRoleId(userDetails.getUsername());
        if (!roleId.equals(verifiedRoleId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestWithStatusList("UNAUTHORIZED_ACCESS",
                    "Requested role does not match user's verified role.", null));
        }
        return roleManageService.getRoleByUserLogin(verifiedRoleId);
    }

    @Operation(summary = "Create new role with menu assignment")
    @PostMapping(value = "/role/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createRole(@RequestBody RoleCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return roleManageService.createRole(request.getRoleCode(), request.getRoleName(), request.getMenuIds(),
                userDetails.getUsername());
    }

    @Operation(summary = "Update existing role")
    @PutMapping(value = "/role/update", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateRole(@RequestBody RoleCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return roleManageService.updateRoleDetails(request, userDetails.getUsername());
    }

    @Operation(summary = "Get roles by approval status (N = pending, Y = approved)")
    @GetMapping(value = "/role/get-pending-roles", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingRoles(@RequestParam String approvedYN) {
        return roleManageService.getPendingRoles(approvedYN);
    }

    @Operation(summary = "Approve or reject a single role")
    @PostMapping(value = "/role/approve-reject-role", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveOrRejectRole(@RequestBody RoleApproveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return roleManageService.approveOrRejectRole(request, userDetails);
    }

    // NEW — Bulk approve / reject multiple roles at once
    @Operation(summary = "Bulk approve or reject multiple roles")
    @PostMapping(value = "/role/bulk-approve-reject", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> bulkApproveOrRejectRoles(@RequestBody TestRoleBulkApproveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return roleManageService.bulkApproveOrRejectRoles(request, userDetails);
    }
}