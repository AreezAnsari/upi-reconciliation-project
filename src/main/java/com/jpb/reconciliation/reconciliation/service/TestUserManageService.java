package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TestUserBulkApproveRequest;

@Service
public interface TestUserManageService {

    // Create new user
    ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto);

    // Get user by ID
    ResponseEntity<RestWithStatusList> getUserByUserId(Long userId);

    // Get all users
    ResponseEntity<RestWithStatusList> getAllUsers();

    // Get users by approvedYn (N = pending, Y = approved)
    ResponseEntity<RestWithStatusList> getApprovedUsers(String approvedYn);

    // Update user
    ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest);

    // Delete user
    ResponseEntity<ResponseDto> removeUser(Long userId);

    // Single approve / reject
    ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest, UserDetails userDetails);

    // NEW — Bulk approve / reject multiple users at once
    ResponseEntity<RestWithStatusList> bulkApproveOrRejectUsers(TestUserBulkApproveRequest request, UserDetails userDetails);
}