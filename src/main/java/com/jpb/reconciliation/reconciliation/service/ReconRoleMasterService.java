package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconRoleMaster;
import org.springframework.http.ResponseEntity;

public interface ReconRoleMasterService {

    ResponseEntity<RestWithStatusList> createRole(ReconRoleMaster role, String createdBy);

    ResponseEntity<RestWithStatusList> getAllRoles();

    ResponseEntity<RestWithStatusList> getRoleById(Long roleId);

    ResponseEntity<RestWithStatusList> getRolesByStatus(String status);

    ResponseEntity<RestWithStatusList> getRolesByType(String roleType);

    ResponseEntity<RestWithStatusList> updateRole(Long roleId, ReconRoleMaster role, String updatedBy);

    ResponseEntity<RestWithStatusList> submitForApproval(Long roleId, String submittedBy);

    ResponseEntity<RestWithStatusList> approveRole(Long roleId, String approvedBy);

    ResponseEntity<RestWithStatusList> updateStatus(Long roleId, String status, String updatedBy);

    ResponseEntity<RestWithStatusList> deleteRole(Long roleId);

    ResponseEntity<RestWithStatusList> checkRoleCodeExists(String roleCode);
}
