package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface RoleManageService {

	ResponseEntity<RestWithStatusList> getAllRoleDetails();

	ResponseEntity<RestWithStatusList> getRoleByUserLogin(Long verifiedRoleId);

	ResponseEntity<RestWithStatusList> createRole(String roleCode, String roleName, List<Long> menuIds,
			String username);

}
