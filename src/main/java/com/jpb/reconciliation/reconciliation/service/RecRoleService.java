package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.*;

import java.util.List;

import org.springframework.http.ResponseEntity;

public interface RecRoleService {

    RestWithStatusList createRole(RecCreateRoleRequestDTO req);

    RestWithStatusList getRole(Long roleId);

    RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos);
    
    RestWithStatusList updateRole(Long roleId, RecCreateRoleRequestDTO req);

    RestWithStatusList getAllModules();
    
    RestWithStatusList getAllRoles();
}