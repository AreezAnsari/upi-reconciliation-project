package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RoleApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.TestRoleBulkApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.RoleCreateRequest;

@Service
public interface TestRoleManageService {

    ResponseEntity<RestWithStatusList> getAllRoleDetails();

    ResponseEntity<RestWithStatusList> getRoleByUserLogin(Long verifiedRoleId);

    ResponseEntity<RestWithStatusList> createRole(String roleCode, String roleName, List<Long> menuIds,
            String username);

    ResponseEntity<RestWithStatusList> updateRoleDetails(RoleCreateRequest request, String username);

    ResponseEntity<RestWithStatusList> getPendingRoles(String approvedYn);

    ResponseEntity<RestWithStatusList> approveOrRejectRole(RoleApproveRequest request, UserDetails userDetails);

    // NEW — bulk approve / reject multiple roles at once
    ResponseEntity<RestWithStatusList> bulkApproveOrRejectRoles(TestRoleBulkApproveRequest request,
            UserDetails userDetails);
}