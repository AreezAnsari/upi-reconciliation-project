package com.jpb.reconciliation.reconciliation.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.BranchBankService;
import org.springframework.security.core.Authentication;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/subinstitution")
@CrossOrigin(origins = "*")
public class BranchBankController {

    Logger logger = LoggerFactory.getLogger(BranchBankController.class);

    @Autowired
    BranchBankService branchBankService;

    // POST /test/api/v1/subinstitution/create
    @Operation(summary = "Onboard a new branch bank (sub-institution)")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createInstitution(
            @RequestBody BranchBankDTO dto,
            Authentication authentication) {

        logger.info("Create branch bank request: {}", dto.getInstitutionNameFull());

        String createdBy = "UNKNOWN";

        if (authentication != null && authentication.isAuthenticated()) {
            createdBy = authentication.getName();
        }

        logger.info("Created By Username: {}", createdBy);

        return branchBankService.createInstitution(dto, createdBy);
    }

    // GET /test/api/v1/subinstitution/get-all
    @Operation(summary = "Get all branch banks")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        logger.info("Fetch all branch banks");
        return branchBankService.getAllInstitutions();
    }

    // GET /test/api/v1/subinstitution/get/{institutionId}
    @Operation(summary = "Get branch bank by ID")
    @GetMapping(value = "/get/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionById(
            @PathVariable Long institutionId) {
        logger.info("Fetch branch bank by ID: {}", institutionId);
        return branchBankService.getInstitutionById(institutionId);
    }

    // GET /test/api/v1/subinstitution/get-by-status?status=ACTIVE
    @Operation(summary = "Get branch banks by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionsByStatus(
            @RequestParam String status) {
        logger.info("Fetch branch banks by status: {}", status);
        return branchBankService.getInstitutionsByStatus(status);
    }

    // PUT /test/api/v1/subinstitution/update/{institutionId}
    @Operation(summary = "Full update of branch bank details")
    @PutMapping(value = "/update/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateInstitution(
            @PathVariable Long institutionId,
            @RequestBody BranchBankDTO dto) {
        logger.info("Update branch bank ID: {}", institutionId);
        return branchBankService.updateInstitution(institutionId, dto);
    }

    // PATCH /test/api/v1/subinstitution/update-status/{institutionId}?status=ACTIVE
    @Operation(summary = "Update branch bank status")
    @PatchMapping(value = "/update-status/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long institutionId,
            @RequestParam String status) {
        logger.info("Update branch bank {} status → {}", institutionId, status);
        return branchBankService.updateStatus(institutionId, status);
    }

    // DELETE /test/api/v1/subinstitution/delete/{institutionId}
    @Operation(summary = "Soft delete branch bank (status → INACTIVE)")
    @DeleteMapping(value = "/delete/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteInstitution(
            @PathVariable Long institutionId) {
        logger.info("Delete branch bank ID: {}", institutionId);
        return branchBankService.deleteInstitution(institutionId);
    }

    // POST /test/api/v1/subinstitution/upload-logo/{institutionId}
    @Operation(summary = "Upload branch bank logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{institutionId}", consumes = "multipart/form-data",
            produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long institutionId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logo upload for branch bank ID: {}", institutionId);
        return branchBankService.uploadLogo(institutionId, file, userDetails.getUsername());
    }

    // GET /test/api/v1/subinstitution/verify-email?token=xxx
    @GetMapping(value = "/verify-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> verifyEmail(@RequestParam String token) {
        return branchBankService.verifyEmail(token);
    }

    // GET /test/api/v1/subinstitution/generate-code
    @Operation(summary = "Pre-generate a unique 8-digit branch bank code for form preview")
    @GetMapping(value = "/generate-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateCode(Authentication authentication) {
        String createdBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Generate branch bank code request by: {}", createdBy);
        return branchBankService.generateCode(createdBy);
    }

    // GET /test/api/v1/subinstitution/check-email?email=abc@gmail.com
    @Operation(summary = "Check branch bank email already exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(
            @RequestParam String email) {

        logger.info("Check branch bank email request: {}", email);

        return branchBankService.checkEmailExists(email);
    }

    // GET /test/api/v1/subinstitution/check-name?name=SomeName
    @Operation(summary = "Check branch bank name already exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkNameExists(
            @RequestParam String name) {

        logger.info("Check branch bank name request: {}", name);

        return branchBankService.checkNameExists(name);
    }

    // POST /test/api/v1/subinstitution/schedule-block/{institutionId}
    @Operation(summary = "Schedule permanent block for branch bank (30s countdown)")
    @PostMapping(value = "/schedule-block/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long institutionId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule block for branch bank {} by {}", institutionId, scheduledBy);
        return branchBankService.scheduleBlock(institutionId, scheduledBy);
    }

    // POST /test/api/v1/subinstitution/undo-block/{institutionId}
    @Operation(summary = "Undo scheduled block for branch bank (within 30s window)")
    @PostMapping(value = "/undo-block/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoBlock(
            @PathVariable Long institutionId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo block for branch bank {} by {}", institutionId, undoneBy);
        return branchBankService.undoBlock(institutionId, undoneBy);
    }

    // GET /test/api/v1/subinstitution/export/excel
    @Operation(summary = "Export all branch banks as Excel (.xlsx)")
    @GetMapping(value = "/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        logger.info("Export branch banks as Excel");
        return branchBankService.exportToExcel();
    }

    // GET /test/api/v1/subinstitution/export/csv
    @Operation(summary = "Export all branch banks as CSV")
    @GetMapping(value = "/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        logger.info("Export branch banks as CSV");
        return branchBankService.exportToCsv();
    }

    // GET /test/api/v1/subinstitution/logo/{institutionCode}
    @Operation(summary = "Serve branch bank logo image by institution code")
    @GetMapping(value = "/logo/{institutionCode}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable String institutionCode) {
        logger.info("Serve logo for branch bank code: {}", institutionCode);
        return branchBankService.getLogoImage(institutionCode);
    }
}
