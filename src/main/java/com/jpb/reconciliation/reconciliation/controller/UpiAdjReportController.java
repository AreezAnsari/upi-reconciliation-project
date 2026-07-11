package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UpiAdjRequestDto;
import com.jpb.reconciliation.reconciliation.service.Upiadjttmstageservice;
import com.jpb.reconciliation.reconciliation.service.UpiAdjSummaryService;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/upi")
public class UpiAdjReportController {

    @Autowired
    private UpiAdjSummaryService upiAdjSummaryService;

    @Autowired
    private Upiadjttmstageservice upiAdjTtmStageService;

    @PostMapping(value = "/adj-summary", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAdjSummary(@RequestBody UpiAdjRequestDto requestDto) {

        if (requestDto == null || requestDto.getAdjDate() == null || requestDto.getAdjDate().trim().isEmpty()) {
            log.warn("API called without adjDate!");
            return ResponseEntity.ok(RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Error: Adjustment Date is required (format: dd-MM-yyyy)")
                    .data(Collections.emptyList())
                    .build());
        }
        log.info("API called: POST /api/v1/upi/adj-summary for Adjustment Date: {}", requestDto.getAdjDate());
        return upiAdjSummaryService.getAdjSummaryByType(requestDto.getAdjDate());
    }

    /**
     * Stage-wise TTUM Decision -- DR/CR account codes resolved from real
     * REC_UPI_ADJ_DATA rows for the given ADJDATE (REMITTER/BENEFICIERY
     * tell us Jio's actual role, TRANSACTION_TYPE tells us U2 vs U3).
     */
    @PostMapping(value = "/upi-adj-ttm-stage", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUpiAdjTtmStage(@RequestBody UpiAdjRequestDto requestDto) {

        if (requestDto == null || requestDto.getAdjDate() == null || requestDto.getAdjDate().trim().isEmpty()) {
            log.warn("API called without adjDate!");
            return ResponseEntity.ok(RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Error: Adjustment Date is required (format: dd-MM-yyyy)")
                    .data(Collections.emptyList())
                    .build());
        }
        log.info("API called: POST /api/v1/upi/upi-adj-ttm-stage for Adjustment Date: {}", requestDto.getAdjDate());
        return ResponseEntity.ok(upiAdjTtmStageService.getUpiAdjTtmStage(requestDto.getAdjDate()));
    }
}