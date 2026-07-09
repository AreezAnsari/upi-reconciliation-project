package com.jpb.reconciliation.reconciliation.controller.v2;

import java.util.Collections; 
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;
import com.jpb.reconciliation.reconciliation.service.v2.ReconSourceSystemMastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Recon Source System API", description = "Operations for Recon Source System master data")
@RestController
@RequestMapping("/api/v2/source-systems")
public class ReconSourceSystemMastController {

    private final ReconSourceSystemMastService service;

    public ReconSourceSystemMastController(ReconSourceSystemMastService service) {
        this.service = service;
    }

    // ─── GET ALL ACTIVE ───────────────────────────────────────────────
    @Operation(summary = "Get all active Recon Source Systems",
               description = "Returns all source systems where isActive = 'Y'")
    @ApiResponse(responseCode = "200", description = "Active source systems fetched successfully")
    @GetMapping("/active")
    public ResponseEntity<RestWithStatusList> getActiveSources() {
        List<ReconSourceSystemMast> list = service.getActiveSources();

        if (list.isEmpty()) {
            return ResponseEntity.ok(
                RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("No active source system records found")
                    .data(Collections.emptyList())
                    .build()
            );
        }

        return ResponseEntity.ok(
            RestWithStatusList.builder()
                .status(CommonConstants.SUCCESS)
                .statusMsg("Active source systems fetched successfully")
                .data(Collections.unmodifiableList(list))
                .build()
        );
    }

}