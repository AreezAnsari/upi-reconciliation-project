package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.UpiAdjSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/upi")
public class UpiAdjReportController {

    @Autowired
    private UpiAdjSummaryService upiAdjSummaryService;
    
    @GetMapping(value = "/adj-summary", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAdjSummary(@RequestParam("adjDate") String adjDate) {
        // YAHAN CHANGE HUA HAI: @RequestParam add kiya aur service ko pass kiya
        log.info("API called: GET /api/v1/upi/adj-summary for Adjustment Date: {}", adjDate);
        return upiAdjSummaryService.getAdjSummaryByType(adjDate);
    }
}