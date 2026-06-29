package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.security.core.Authentication;

public interface AddUserService {

    RestWithStatusList createUser(AddUserRequest request, Authentication authentication);

    RestWithStatusList getUsersByCreator(Authentication authentication);
    RestWithStatusList getUsersByCreatorUsername(String creatorUsername);

    RestWithStatusList getUserById(Long id);

    RestWithStatusList updateUser(Long id, AddUserRequest request);

    RestWithStatusList deactivateUser(Long id);

    RestWithStatusList searchByCreator(Authentication authentication, String term);

    RestWithStatusList getAllUsers();

    RestWithStatusList getUsersByBankCode(String bankCode);

    RestWithStatusList getUsersByBranchCode(String branchCode);

    RestWithStatusList scheduleInactivateUser(Long id, String scheduledBy);

    RestWithStatusList undoInactivateUser(Long id, String undoneBy);

    RestWithStatusList scheduleReactivateUser(Long id, String scheduledBy);

    RestWithStatusList undoReactivateUser(Long id, String undoneBy);

    RestWithStatusList scheduleBlockUser(Long id, String scheduledBy, String reason);

    RestWithStatusList undoBlockUser(Long id, String undoneBy);

    RestWithStatusList getUserHierarchy(Authentication authentication);

    RestWithStatusList getUserHierarchyByBankCode(String bankCode);

    RestWithStatusList getUserHierarchyByBranchCode(String branchCode);

    RestWithStatusList getUserHierarchyByBankDirect(String bankCode);

    RestWithStatusList delegateUser(Long userId, Long delegateeId, String reason, String delegatedBy);

    RestWithStatusList getUserAncestors(Long userId);
}
