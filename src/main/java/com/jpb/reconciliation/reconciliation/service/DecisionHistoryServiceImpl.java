package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.DecisionHistoryResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditLogManager;
import com.jpb.reconciliation.reconciliation.repository.TestAuditLogManagerRepository;

@Service
public class DecisionHistoryServiceImpl implements DecisionHistoryService {

    Logger logger = LoggerFactory.getLogger(DecisionHistoryServiceImpl.class);

    // Module constants — same pattern as existing AuditLogManagerServiceImpl
    private static final String MODULE_ROLE = "CHECKER_ROLE";
    private static final String MODULE_USER = "CHECKER_USER";

    @Autowired
    private TestAuditLogManagerRepository auditLogManagerRepository;

    // ─────────────────────────────────────────────
    // Tab 3 — ALL history (roles + users combined)
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getAllDecisionHistory() {
        RestWithStatusList restWithStatusList;
        List<Object> historyList = new ArrayList<>();

        List<AuditLogManager> allHistory = auditLogManagerRepository
                .findByModuleInOrderByAuditDateTimeDesc(Arrays.asList(MODULE_ROLE, MODULE_USER));

        logger.info("Decision History fetched — total records: {}", allHistory.size());

        if (allHistory.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No decision history found", historyList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        List<DecisionHistoryResponseDto> mapped = mapToHistoryDto(allHistory);
        historyList.addAll(mapped);
        restWithStatusList = new RestWithStatusList("SUCCESS", "Decision history fetched successfully", historyList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // Only ROLE decisions
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getRoleDecisionHistory() {
        RestWithStatusList restWithStatusList;
        List<Object> historyList = new ArrayList<>();

        List<AuditLogManager> roleHistory = auditLogManagerRepository
                .findByModuleOrderByAuditDateTimeDesc(MODULE_ROLE);

        logger.info("Role Decision History — total records: {}", roleHistory.size());

        if (roleHistory.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No role decision history found", historyList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        historyList.addAll(mapToHistoryDto(roleHistory));
        restWithStatusList = new RestWithStatusList("SUCCESS", "Role history fetched successfully", historyList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // Only USER decisions
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getUserDecisionHistory() {
        RestWithStatusList restWithStatusList;
        List<Object> historyList = new ArrayList<>();

        List<AuditLogManager> userHistory = auditLogManagerRepository
                .findByModuleOrderByAuditDateTimeDesc(MODULE_USER);

        logger.info("User Decision History — total records: {}", userHistory.size());

        if (userHistory.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No user decision history found", historyList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        historyList.addAll(mapToHistoryDto(userHistory));
        restWithStatusList = new RestWithStatusList("SUCCESS", "User history fetched successfully", historyList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // Filter by decision — "Approved" / "Disapproved"
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getHistoryByDecision(String module, String decision) {
        RestWithStatusList restWithStatusList;
        List<Object> historyList = new ArrayList<>();

        // module param → convert to internal constant
        String internalModule = "ROLE".equalsIgnoreCase(module) ? MODULE_ROLE : MODULE_USER;

        List<AuditLogManager> filtered = auditLogManagerRepository
                .findByModuleAndEventOrderByAuditDateTimeDesc(internalModule, decision);

        logger.info("History by module={} decision={} — records: {}", module, decision, filtered.size());

        if (filtered.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No history found for given filter", historyList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        historyList.addAll(mapToHistoryDto(filtered));
        restWithStatusList = new RestWithStatusList("SUCCESS", "History fetched successfully", historyList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // Save audit — called from ServiceImpl on every checker decision
    // Same style as AuditLogManagerServiceImpl.loginAudit()
    // ─────────────────────────────────────────────
    @Override
    public void saveCheckerDecisionAudit(String module, String itemName, Long itemId,
            String decision, String decidedByUsername, Long decidedByUserId, String remarks) {

        AuditLogManager audit = new AuditLogManager();

        // module = "CHECKER_ROLE" or "CHECKER_USER"
        audit.setModule(module);

        // subModule = role/user name
        audit.setSubModule(itemName);

        // event = "Approved" / "Disapproved" / "Inactive"
        audit.setEvent(decision);

        // eventData = detailed info
        audit.setEventData("ItemName:" + itemName + "|ItemId:" + itemId
                + "|DecidedBy:" + decidedByUsername);

        audit.setEventStatus("SUCCESS");
        audit.setUserId(decidedByUserId);
        audit.setUserIp("-");
        audit.setAuditDateTime(new Date());

        // remarks stored in oldData — same field existing code uses
        audit.setOldData(remarks != null ? remarks : "-");
        audit.setRoleId(itemId);

        logger.info("Checker Decision Audit saved — module={}, item={}, decision={}",
                module, itemName, decision);

        auditLogManagerRepository.save(audit);
    }

    // ─────────────────────────────────────────────
    // Private helper — AuditLogManager → DTO
    // ─────────────────────────────────────────────
    private List<DecisionHistoryResponseDto> mapToHistoryDto(List<AuditLogManager> auditList) {
        List<DecisionHistoryResponseDto> dtoList = new ArrayList<>();
        for (AuditLogManager audit : auditList) {
            DecisionHistoryResponseDto dto = new DecisionHistoryResponseDto();
            dto.setAuditLogId(audit.getAuditLogId());
            dto.setModule(audit.getModule());
            dto.setSubModule(audit.getSubModule());
            dto.setEvent(audit.getEvent());
            dto.setEventData(audit.getEventData());
            dto.setEventStatus(audit.getEventStatus());
            dto.setDecidedByUserId(audit.getUserId());
            dto.setDecidedByUsername(audit.getUserIp()); // stored in userIp for checker
            dto.setAuditDateTime(audit.getAuditDateTime());
            dto.setRemarks(audit.getOldData());
            dto.setRoleId(audit.getRoleId());
            dtoList.add(dto);
        }
        return dtoList;
    }
}