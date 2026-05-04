package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface DecisionHistoryService {

    // Tab 3 — fetch all checker decisions (roles + users combined)
    ResponseEntity<RestWithStatusList> getAllDecisionHistory();

    // Fetch only Role decisions
    ResponseEntity<RestWithStatusList> getRoleDecisionHistory();

    // Fetch only User decisions
    ResponseEntity<RestWithStatusList> getUserDecisionHistory();

    // Fetch by decision type — "Approved" / "Disapproved" / "Inactive"
    ResponseEntity<RestWithStatusList> getHistoryByDecision(String module, String decision);

    // Save audit — called internally from TestRoleManageServiceImpl & TestUserManageServiceImpl
    void saveCheckerDecisionAudit(String module, String itemName, Long itemId,
            String decision, String decidedByUsername, Long decidedByUserId, String remarks);
}