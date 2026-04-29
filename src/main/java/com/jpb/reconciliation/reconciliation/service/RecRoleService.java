package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.*;

import java.util.List;

public interface RecRoleService {

    RestWithStatusList createRole(RecCreateRoleRequestDTO req);

    RestWithStatusList getRole(Long roleId);

    RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos);

    RestWithStatusList getAllModules();
}