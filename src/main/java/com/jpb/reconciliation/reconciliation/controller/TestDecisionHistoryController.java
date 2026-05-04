package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.DecisionHistoryService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/history")
public class TestDecisionHistoryController {

    Logger logger = LoggerFactory.getLogger(TestDecisionHistoryController.class);

    @Autowired
    DecisionHistoryService decisionHistoryService;

    // Tab 3 — sabhi checker decisions (roles + users combined)
    @Operation(summary = "Get all checker decision history (roles + users combined)")
    @GetMapping(value = "/all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllDecisionHistory() {
        logger.info("Fetching all decision history");
        return decisionHistoryService.getAllDecisionHistory();
    }

    // Sirf Role decisions
    @Operation(summary = "Get checker decision history for roles only")
    @GetMapping(value = "/roles", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRoleDecisionHistory() {
        logger.info("Fetching role decision history");
        return decisionHistoryService.getRoleDecisionHistory();
    }

    // Sirf User decisions
    @Operation(summary = "Get checker decision history for users only")
    @GetMapping(value = "/users", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUserDecisionHistory() {
        logger.info("Fetching user decision history");
        return decisionHistoryService.getUserDecisionHistory();
    }

    // Filter by module + decision — e.g. ROLE + Approved
    @Operation(summary = "Filter history by module (ROLE/USER) and decision (Approved/Disapproved/Inactive)")
    @GetMapping(value = "/filter", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getHistoryByDecision(
            @RequestParam String module,
            @RequestParam String decision) {
        logger.info("Fetching history — module={}, decision={}", module, decision);
        return decisionHistoryService.getHistoryByDecision(module, decision);
    }
}