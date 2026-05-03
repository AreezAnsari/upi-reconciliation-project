package com.jpb.reconciliation.reconciliation.service;

import javax.validation.Valid;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface AddUserService {

    RestWithStatusList createUser(AddUserRequest request,String createdBy, String instCode);
    
    RestWithStatusList getUsersByInstitution(String instCode);

    RestWithStatusList getUserById(Long id);
    
    RestWithStatusList updateUser(Long id, AddUserRequest request);

    RestWithStatusList deactivateUser(Long id);

    RestWithStatusList searchUsers(String instCode, String term);

    RestWithStatusList getAllUsers();

}