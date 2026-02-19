package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.MenuMasterService;
import com.jpb.reconciliation.reconciliation.service.RoleManageService;

@RestController
@RequestMapping(path = "/api/v1/")
public class RoleController {

	@Autowired
	RoleManageService roleManageService;
	
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
	        return ResponseEntity
	            .status(HttpStatus.FORBIDDEN)
	            .body(new RestWithStatusList("UNAUTHORIZED_ACCESS", "Requested role does not match user's verified role.",null));
	    }
		return roleManageService.getRoleByUserLogin(verifiedRoleId);
	}
}
