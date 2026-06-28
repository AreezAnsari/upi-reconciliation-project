package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.service.ReconBankMasterService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v2/bank")
@CrossOrigin(origins = "*")
public class ReconBankMasterController {

    private static final Logger logger = LoggerFactory.getLogger(ReconBankMasterController.class);

    @Autowired
    private ReconBankMasterService reconBankMasterService;

    @Operation(summary = "Create a new bank")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createBank(
            @RequestBody ReconBankMaster bank,
            Authentication authentication) {
        String createdBy = resolveUser(authentication);
        logger.info("Create bank request: {} by {}", bank.getBankCode(), createdBy);
        return reconBankMasterService.createBank(bank, createdBy);
    }

    @Operation(summary = "Get all banks")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        logger.info("Get all banks request received");
        return reconBankMasterService.getAllBanks();
    }

    @Operation(summary = "Get bank by ID")
    @GetMapping(value = "/get/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBankById(@PathVariable Long bankId) {
        logger.info("Get bank by ID: {}", bankId);
        return reconBankMasterService.getBankById(bankId);
    }

    @Operation(summary = "Get bank by code")
    @GetMapping(value = "/get-by-code/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBankByCode(@PathVariable String bankCode) {
        logger.info("Get bank by code: {}", bankCode);
        return reconBankMasterService.getBankByCode(bankCode);
    }

    @Operation(summary = "Get banks by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBanksByStatus(@RequestParam String status) {
        logger.info("Get banks by status: {}", status);
        return reconBankMasterService.getBanksByStatus(status);
    }

    @Operation(summary = "Get banks by type")
    @GetMapping(value = "/get-by-type", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBanksByType(@RequestParam String bankType) {
        logger.info("Get banks by type: {}", bankType);
        return reconBankMasterService.getBanksByType(bankType);
    }

    @Operation(summary = "Get branch banks by parent bank ID")
    @GetMapping(value = "/{parentBankId}/branch-banks", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBranchBanks(@PathVariable Long parentBankId) {
        logger.info("Get branch banks for parent: {}", parentBankId);
        return reconBankMasterService.getBranchBanks(parentBankId);
    }

    @Operation(summary = "Update bank details")
    @PutMapping(value = "/update/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateBank(
            @PathVariable Long bankId,
            @RequestBody ReconBankMaster bank,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.updateBank(bankId, bank, updatedBy);
    }

    @Operation(summary = "Update bank status")
    @PatchMapping(value = "/update-status/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long bankId,
            @RequestParam String status,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update bank status for ID: {} to {} by {}", bankId, status, updatedBy);
        return reconBankMasterService.updateStatus(bankId, status, updatedBy);
    }

    @Operation(summary = "Soft delete bank (sets INACTIVE)")
    @DeleteMapping(value = "/delete/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteBank(@PathVariable Long bankId) {
        logger.info("Delete bank request for ID: {}", bankId);
        return reconBankMasterService.deleteBank(bankId);
    }

    @Operation(summary = "Check if bank code exists")
    @GetMapping(value = "/check-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkBankCodeExists(@RequestParam String bankCode) {
        return reconBankMasterService.checkBankCodeExists(bankCode);
    }

    @Operation(summary = "Check if bank name exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkBankNameExists(@RequestParam String bankName) {
        return reconBankMasterService.checkBankNameExists(bankName);
    }

    @Operation(summary = "Block a bank (stores pre-block status)")
    @PostMapping(value = "/block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> blockBank(
            @PathVariable Long bankId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Block bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.blockBank(bankId, reason, updatedBy);
    }

    @Operation(summary = "Unblock a bank (restores pre-block status)")
    @PostMapping(value = "/unblock/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> unblockBank(
            @PathVariable Long bankId,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Unblock bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.unblockBank(bankId, updatedBy);
    }

    @Operation(summary = "Schedule bank inactivation at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-inactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long bankId,
            @RequestParam String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule inactivate for bankId: {} at {} by {}", bankId, scheduledAt, scheduledBy);
        return reconBankMasterService.scheduleInactivate(bankId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule bank reactivation at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-reactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long bankId,
            @RequestParam String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule reactivate for bankId: {} at {} by {}", bankId, scheduledAt, scheduledBy);
        return reconBankMasterService.scheduleReactivate(bankId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule bank block at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long bankId,
            @RequestParam String scheduledAt,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule block for bankId: {} at {} by {}", bankId, scheduledAt, scheduledBy);
        return reconBankMasterService.scheduleBlock(bankId, dateTime, scheduledBy, reason);
    }

    @Operation(summary = "Cancel a scheduled bank status change (scheduleType: INACTIVATE / REACTIVATE / BLOCK)")
    @PostMapping(value = "/cancel-schedule/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> cancelSchedule(
            @PathVariable Long bankId,
            @RequestParam String scheduleType,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Cancel {} schedule for bankId: {} by {}", scheduleType, bankId, updatedBy);
        return reconBankMasterService.cancelSchedule(bankId, scheduleType, updatedBy);
    }

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }
}
