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
@RequestMapping(path = "/test/api/v1/branch")
@CrossOrigin(origins = "*")
public class BranchBankController {

    Logger logger = LoggerFactory.getLogger(BranchBankController.class);

    @Autowired
    BranchBankService branchBankService;

    // POST /test/api/v1/branchBank/create
    @Operation(summary = "Onboard a new branch bank (branch-bank)")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createBank(
            @RequestBody BranchBankDTO dto,
            Authentication authentication) {

        logger.info("Create branch bank request: {}", dto.getBranchNameFull());

        String createdBy = "UNKNOWN";

        if (authentication != null && authentication.isAuthenticated()) {
            createdBy = authentication.getName();
        }

        logger.info("Created By Username: {}", createdBy);

        return branchBankService.createBank(dto, createdBy);
    }

    // GET /test/api/v1/branchbank/get-all
    @Operation(summary = "Get all branch banks for the logged-in Branch Admin's parent bank")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllBanks(Authentication authentication) {
        String loggedInUsername = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Fetch branch banks for user: {}", loggedInUsername);
        return branchBankService.getAllBanks(loggedInUsername);
    }

    // GET /test/api/v1/branchbank/get/{bankId}
    @Operation(summary = "Get branch bank by ID")
    @GetMapping(value = "/get/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBankById(
            @PathVariable Long bankId) {
        logger.info("Fetch branch bank by ID: {}", bankId);
        return branchBankService.getBankById(bankId);
    }

    // GET /test/api/v1/branchbank/get-by-status?status=ACTIVE
    @Operation(summary = "Get branch banks by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBanksByStatus(
            @RequestParam String status) {
        logger.info("Fetch branch banks by status: {}", status);
        return branchBankService.getBanksByStatus(status);
    }

    // PUT /test/api/v1/branchbank/update/{bankId}
    @Operation(summary = "Full update of branch bank details")
    @PutMapping(value = "/update/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateBank(
            @PathVariable Long bankId,
            @RequestBody BranchBankDTO dto) {
        logger.info("Update branch bank ID: {}", bankId);
        return branchBankService.updateBank(bankId, dto);
    }

    // PATCH /test/api/v1/branchbank/update-status/{bankId}?status=ACTIVE
    @Operation(summary = "Update branch bank status")
    @PatchMapping(value = "/update-status/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long bankId,
            @RequestParam String status) {
        logger.info("Update branch bank {} status → {}", bankId, status);
        return branchBankService.updateStatus(bankId, status);
    }

    // DELETE /test/api/v1/branchbank/delete/{bankId}
    @Operation(summary = "Soft delete branch bank (status → INACTIVE)")
    @DeleteMapping(value = "/delete/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteBank(
            @PathVariable Long bankId) {
        logger.info("Delete branch bank ID: {}", bankId);
        return branchBankService.deleteBank(bankId);
    }

    // POST /test/api/v1/branchbank/upload-logo/{bankId}
    @Operation(summary = "Upload branch bank logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{bankId}", consumes = "multipart/form-data",
            produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long bankId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logo upload for branch bank ID: {}", bankId);
        return branchBankService.uploadLogo(bankId, file, userDetails.getUsername());
    }

    // GET /test/api/v1/branchbank/generate-code
    @Operation(summary = "Pre-generate a unique 8-digit branch bank code for form preview")
    @GetMapping(value = "/generate-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateCode(Authentication authentication) {
        String createdBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Generate branch bank code request by: {}", createdBy);
        return branchBankService.generateCode(createdBy);
    }

    // GET /test/api/v1/branchbank/check-email?email=abc@gmail.com
    @Operation(summary = "Check branch bank email already exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(
            @RequestParam String email) {

        logger.info("Check branch bank email request: {}", email);

        return branchBankService.checkEmailExists(email);
    }

    // GET /test/api/v1/branchbank/check-name?name=SomeName
    @Operation(summary = "Check branch bank name already exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkNameExists(
            @RequestParam String name) {

        logger.info("Check branch bank name request: {}", name);

        return branchBankService.checkNameExists(name);
    }

    // POST /test/api/v1/branchbank/schedule-block/{bankId}
    @Operation(summary = "Schedule permanent block for branch bank (30s countdown)")
    @PostMapping(value = "/schedule-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long bankId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule block for branch bank {} by {}", bankId, scheduledBy);
        return branchBankService.scheduleBlock(bankId, scheduledBy);
    }

    // POST /test/api/v1/branchbank/undo-block/{bankId}
    @Operation(summary = "Undo scheduled block for branch bank (within 30s window)")
    @PostMapping(value = "/undo-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoBlock(
            @PathVariable Long bankId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo block for branch bank {} by {}", bankId, undoneBy);
        return branchBankService.undoBlock(bankId, undoneBy);
    }

    // GET /test/api/v1/branchbank/export/excel
    @Operation(summary = "Export all branch banks as Excel (.xlsx)")
    @GetMapping(value = "/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        logger.info("Export branch banks as Excel");
        return branchBankService.exportToExcel();
    }

    // GET /test/api/v1/branchbank/export/csv
    @Operation(summary = "Export all branch banks as CSV")
    @GetMapping(value = "/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        logger.info("Export branch banks as CSV");
        return branchBankService.exportToCsv();
    }

    // POST /test/api/v1/branchbank/schedule-inactivate/{bankId}
    @Operation(summary = "Schedule inactivation for branch bank (30s demo / 30min production)")
    @PostMapping(value = "/schedule-inactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long bankId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule inactivate for branch bank {} by {}", bankId, scheduledBy);
        return branchBankService.scheduleInactivate(bankId, scheduledBy);
    }

    // POST /test/api/v1/branchbank/undo-inactivate/{bankId}
    @Operation(summary = "Undo scheduled inactivation for branch bank")
    @PostMapping(value = "/undo-inactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoInactivate(
            @PathVariable Long bankId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo inactivate for branch bank {} by {}", bankId, undoneBy);
        return branchBankService.undoInactivate(bankId, undoneBy);
    }

    // POST /test/api/v1/branchbank/schedule-reactivate/{bankId}
    @Operation(summary = "Schedule reactivation for branch bank (30s demo / 1hr production)")
    @PostMapping(value = "/schedule-reactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long bankId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule reactivate for branch bank {} by {}", bankId, scheduledBy);
        return branchBankService.scheduleReactivate(bankId, scheduledBy);
    }

    // POST /test/api/v1/branchbank/undo-reactivate/{bankId}
    @Operation(summary = "Undo scheduled reactivation for branch bank")
    @PostMapping(value = "/undo-reactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoReactivate(
            @PathVariable Long bankId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo reactivate for branch bank {} by {}", bankId, undoneBy);
        return branchBankService.undoReactivate(bankId, undoneBy);
    }

    // GET /test/api/v1/branchbank/get-by-code/{bankCode}
    // Used by BranchAdmin sidebar to display bank logo + short name
    @Operation(summary = "Get branch bank by bank code")
    @GetMapping(value = "/get-by-code/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getByCode(@PathVariable String bankCode) {
        logger.info("Get branch bank by code: {}", bankCode);
        return branchBankService.getBankByCode(bankCode);
    }

    // GET /test/api/v1/branchbank/get-by-email?email=...
    // Used by BranchAdmin sidebar — email is always in sync with BRANCH_BANK.primary_email
    @Operation(summary = "Get branch bank by admin email")
    @GetMapping(value = "/get-by-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getByEmail(@RequestParam String email) {
        logger.info("Get branch bank by email: {}", email);
        return branchBankService.getBankByEmail(email);
    }

    // GET /test/api/v1/branchbank/logo/{bankCode}
    @Operation(summary = "Serve branch bank logo image by bank code")
    @GetMapping(value = "/logo/{bankCode}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable String bankCode) {
        logger.info("Serve logo for branch bank code: {}", bankCode);
        return branchBankService.getLogoImage(bankCode);
    }
}
