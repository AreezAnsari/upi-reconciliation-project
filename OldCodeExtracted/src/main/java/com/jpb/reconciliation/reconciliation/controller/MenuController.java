package com.jpb.reconciliation.reconciliation.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.MenuMasterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "CRUD REST APIs for Menu", description = "CRUD REST APIs for CREATE menu, DELETE menu, UPDATE menu and GET menu details")
@RestController
@RequestMapping(path = "/api/v1")
@CrossOrigin
public class MenuController {

	@Autowired
	MenuMasterService menuMasterService;

	@Operation(summary = "Create Menu REST API", description = "Create Rest API for create menu")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Http Status Created"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@PostMapping(value = "/addmenu", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> addMasterMenu(@RequestBody ReconMenuMasterDto menuRequest,
			@AuthenticationPrincipal UserDetails userDetails ) {
		return menuMasterService.addMenu(menuRequest, userDetails);
	}

	@Operation(summary = "Fetch Menu REST API", description = "Fetch Rest API for Menu Details")
	@GetMapping(value = "/getmenu/{menuId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getMenu(@PathVariable Long menuId) {
		return menuMasterService.getMenus(menuId);
	}

	@GetMapping(value = "/getMenuBy-userId/{userId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getMenuByUserId(@PathVariable Long userId) {
		return menuMasterService.getMenuByUserId(userId);
	}
	
	@GetMapping(value = "/getMenuBy-Role/{roleId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getMenuByRole(@PathVariable Long roleId,
			@AuthenticationPrincipal UserDetails userDetails) {
		
		Long verifiedRoleId = menuMasterService.getVerifiedRoleId(userDetails.getUsername());
		
		if (!roleId.equals(verifiedRoleId)) {
	        return ResponseEntity
	            .status(HttpStatus.FORBIDDEN)
	            .body(new RestWithStatusList("UNAUTHORIZED_ACCESS", "Requested menu role does not match user's verified role.",null));
	    }
		return menuMasterService.getMenuByRole(verifiedRoleId);
	}
    
	@Operation(summary = "Delete Menu REST API", description = "Delete Rest Api for Menu")
	@DeleteMapping(value = "/removemenu/{menuId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> removeMenu(@PathVariable Long menuId) {
		return menuMasterService.removeMenu(menuId);
	}
    
	@Operation(summary = "Update Menu REST API", description = "Update Rest API for Menu")
	@PutMapping(value = "/editmenu", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> updateMenu(@RequestBody ReconMenuMasterDto menuDto) {
		boolean isUpdated = menuMasterService.updateMenu(menuDto);
		if (isUpdated) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ResponseDto(MenuConstants.STATUS_200, MenuConstants.MESSAGE_200));
		} else {
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(new ResponseDto(MenuConstants.STATUS_417, MenuConstants.MESSAGE_417));
		}

	}

	@GetMapping(value = "/getallmenu", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllMenus() {
		return menuMasterService.getAllMenus();	
	}

	@GetMapping(value = "/current-user", produces = CommonConstants.APPLICATION_JSON)
	public String getLoggedInUser(Principal principal) {
		return principal.getName();
	}
}
