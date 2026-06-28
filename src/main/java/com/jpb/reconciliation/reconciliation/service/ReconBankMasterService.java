package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface ReconBankMasterService {

    ResponseEntity<RestWithStatusList> createBank(ReconBankMaster bank, String createdBy);

    ResponseEntity<RestWithStatusList> getAllBanks();

    ResponseEntity<RestWithStatusList> getBankById(Long bankId);

    ResponseEntity<RestWithStatusList> getBankByCode(String bankCode);

    ResponseEntity<RestWithStatusList> getBanksByStatus(String status);

    ResponseEntity<RestWithStatusList> getBanksByType(String bankType);

    ResponseEntity<RestWithStatusList> getBranchBanks(Long parentBankId);

    ResponseEntity<RestWithStatusList> updateBank(Long bankId, ReconBankMaster bank, String updatedBy);

    ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status, String updatedBy);

    ResponseEntity<RestWithStatusList> deleteBank(Long bankId);

    ResponseEntity<RestWithStatusList> checkBankCodeExists(String bankCode);

    ResponseEntity<RestWithStatusList> checkBankNameExists(String bankName);

    ResponseEntity<RestWithStatusList> blockBank(Long bankId, String reason, String updatedBy);

    ResponseEntity<RestWithStatusList> unblockBank(Long bankId, String updatedBy);

    ResponseEntity<RestWithStatusList> scheduleInactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleReactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, LocalDateTime scheduledAt, String scheduledBy, String reason);

    ResponseEntity<RestWithStatusList> cancelSchedule(Long bankId, String scheduleType, String updatedBy);
}
