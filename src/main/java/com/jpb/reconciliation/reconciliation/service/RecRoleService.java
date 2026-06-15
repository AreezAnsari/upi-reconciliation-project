package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface RecRoleService {

    RestWithStatusList createRole(RecCreateRoleRequestDTO req, Authentication authentication);

    RestWithStatusList getRole(Long roleId, Authentication authentication);

    RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos);

    RestWithStatusList getAllModules();

    RestWithStatusList getAllRolesByCreator(Authentication authentication);
}
