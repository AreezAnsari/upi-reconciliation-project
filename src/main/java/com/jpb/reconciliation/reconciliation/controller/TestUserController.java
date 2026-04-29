package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TestUserBulkApproveRequest;
import com.jpb.reconciliation.reconciliation.service.TestUserManageService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/user")
public class TestUserController {

    Logger logger = LoggerFactory.getLogger(TestUserController.class);

    @Autowired
    TestUserManageService testUserManageService;

    @Operation(summary = "Create a new test user")
    @PostMapping(value = "/create-user", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createUser(@RequestBody ReconUserDto reconUserDto) {
        return testUserManageService.createUser(reconUserDto);
    }

    @Operation(summary = "Get test user by userId")
    @GetMapping(value = "/getuserby-userid/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUser(@PathVariable Long userId) {
        return testUserManageService.getUserByUserId(userId);
    }

    @Operation(summary = "Get all test users")
    @GetMapping(value = "/getallusers", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllUserDetails() {
        return testUserManageService.getAllUsers();
    }

    @Operation(summary = "Get users by approval status (N = pending, Y = approved)")
    @GetMapping(value = "/get-approved-users", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getApprovedUsers(@RequestParam String approvedYN) {
        return testUserManageService.getApprovedUsers(approvedYN);
    }

    @Operation(summary = "Update test user details")
    @PutMapping(path = "/update-user", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateUser(@RequestBody ReconUserDto userUpdateRequest) {
        return testUserManageService.updateUser(userUpdateRequest);
    }

    @Operation(summary = "Delete test user by userId")
    @DeleteMapping(value = "/remove-user/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ResponseDto> removedUser(@PathVariable Long userId) {
        return testUserManageService.removeUser(userId);
    }

    @Operation(summary = "Single approve or reject a user")
    @PostMapping(value = "/approve-reject-user", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveOrRejectUser(
            @RequestBody ReconUserDto approveUserRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        return testUserManageService.approveOrRejectUser(approveUserRequest, userDetails);
    }

    // NEW — Bulk approve / reject multiple users at once
    @Operation(summary = "Bulk approve or reject multiple users")
    @PostMapping(value = "/bulk-approve-reject", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> bulkApproveOrRejectUsers(
            @RequestBody TestUserBulkApproveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return testUserManageService.bulkApproveOrRejectUsers(request, userDetails);
    }
}