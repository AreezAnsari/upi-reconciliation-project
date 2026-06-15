package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.security.core.Authentication;

public interface AddUserService {

    RestWithStatusList createUser(AddUserRequest request, Authentication authentication);

    RestWithStatusList getUsersByCreator(Authentication authentication);

    RestWithStatusList getUserById(Long id);

    RestWithStatusList updateUser(Long id, AddUserRequest request);

    RestWithStatusList deactivateUser(Long id);

    RestWithStatusList searchByCreator(Authentication authentication, String term);

    RestWithStatusList getAllUsers();
}
