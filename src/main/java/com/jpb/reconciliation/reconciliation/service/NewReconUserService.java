package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface NewReconUserService {

    ResponseEntity<RestWithStatusList> createUser(ReconUser user, String createdBy);

    ResponseEntity<RestWithStatusList> getAllUsers();

    ResponseEntity<RestWithStatusList> getUserById(Long userId);

    ResponseEntity<RestWithStatusList> getUserByUsername(String username);

    ResponseEntity<RestWithStatusList> getUsersByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> getUsersByBankCode(String bankCode);

    ResponseEntity<RestWithStatusList> getUsersByRoleId(Long roleId);

    ResponseEntity<RestWithStatusList> getUsersByStatus(String status);

    ResponseEntity<RestWithStatusList> getUsersByType(String userType);

    ResponseEntity<RestWithStatusList> updateUser(Long userId, ReconUser user, String updatedBy);

    ResponseEntity<RestWithStatusList> updateStatus(Long userId, String status, String updatedBy);

    ResponseEntity<RestWithStatusList> approveUser(Long userId, String approvedBy);

    ResponseEntity<RestWithStatusList> deleteUser(Long userId);

    ResponseEntity<RestWithStatusList> checkUsernameExists(String username);

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> blockUser(Long userId, String reason, String updatedBy);

    ResponseEntity<RestWithStatusList> unblockUser(Long userId, String updatedBy);

    ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, LocalDateTime scheduledAt, String scheduledBy, String reason);

    ResponseEntity<RestWithStatusList> cancelSchedule(Long userId, String scheduleType, String updatedBy);
}
