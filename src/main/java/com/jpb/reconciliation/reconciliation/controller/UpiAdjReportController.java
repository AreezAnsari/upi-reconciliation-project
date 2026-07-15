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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
     * Stage-wise TTUM Decision -- paginated, raw REC_UPI_ADJ_DATA rows for the
     * given ADJDATE, in natural DB order. page/size now come from the request
     * body (UpiAdjRequestDto) -- single source of truth, default size = 50.
     * Example body: { "adjDate": "13-06-2026", "page": 0, "size": 50 }
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
        log.info("API called: POST /api/v1/upi/upi-adj-ttm-stage for date: {}, page: {}, size: {}",
                requestDto.getAdjDate(), requestDto.getPage(), requestDto.getSize());
        return ResponseEntity.ok(upiAdjTtmStageService.getUpiAdjTtmStage(
                requestDto.getAdjDate(), requestDto.getPage(), requestDto.getSize()));
    }
    /**
     * Download full Stage-wise TTUM Decision data as CSV — NO pagination,
     * poora data ek CSV file mein, direct browser download trigger karega.
     * Example: POST /api/v1/upi/upi-adj-ttm-stage/download
     * Body: { "adjDate": "13-06-2026" }
     */
    @PostMapping(value = "/upi-adj-ttm-stage/download")
    public ResponseEntity<byte[]> downloadUpiAdjTtmStage(@RequestBody UpiAdjRequestDto request) {

        String adjDate = request.getAdjDate();

        if (adjDate == null || adjDate.trim().isEmpty()) {
            log.warn("Download API called without adjDate!");
            return ResponseEntity.badRequest().body("Adjustment Date is required".getBytes());
        }

        log.info("API called: POST /api/v1/upi/upi-adj-ttm-stage/download for date: {}", adjDate);

        byte[] csvBytes = upiAdjTtmStageService.downloadUpiAdjTtmStageCsv(adjDate);

        String fileName = "Stage_Wise_TTUM_" + adjDate.replace("-", "") + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}