package com.jpb.reconciliation.reconciliation.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.MainBankDTO;
import com.jpb.reconciliation.reconciliation.service.BlockScheduleService;
import com.jpb.reconciliation.reconciliation.service.MainBankService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/bank")
@CrossOrigin(origins = "*")
public class MainBankController {

    Logger logger = LoggerFactory.getLogger(MainBankController.class);

    @Autowired
    MainBankService mainBankService;

    @Autowired
    BlockScheduleService blockScheduleService;

    // ─────────────────────────────────────────────
    // CREATE
    // POST /test/api/v1/bank/create
    // ─────────────────────────────────────────────
    @Operation(summary = "Onboard a new bank")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createbank(
            @RequestBody MainBankDTO dto,
            Authentication authentication) {
        logger.info("Create bank request received: {}", dto.getBankNameFull());
        String createdBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.createbank(dto, createdBy);
    }

    // ─────────────────────────────────────────────
    // GET ALL
    // GET /test/api/v1/bank/get-all
    // ─────────────────────────────────────────────
    @Operation(summary = "Get all banks")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllbanks() {
        logger.info("Fetch all banks request received");
        return mainBankService.getAllBanks();
    }

    // ─────────────────────────────────────────────
    // GET BY ID
    // GET /test/api/v1/bank/get/{bankId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Get bank by ID")
    @GetMapping(value = "/get/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getbankById(
            @PathVariable Long bankId) {
        logger.info("Fetch bank by ID: {}", bankId);
        return mainBankService.getBankById(bankId);
    }

    // ─────────────────────────────────────────────
    // GET BY STATUS
    // GET /test/api/v1/bank/get-by-status?status=ACTIVE
    // ─────────────────────────────────────────────
    @Operation(summary = "Get banks by status (ACTIVE / INACTIVE / PENDING / BLOCKED)")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getbanksByStatus(
            @RequestParam String status) {
        logger.info("Fetch banks by status: {}", status);
        return mainBankService.getBanksByStatus(status);
    }

    // ─────────────────────────────────────────────
    // FULL UPDATE
    // PUT /test/api/v1/bank/update/{bankId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Full update of bank details")
    @PutMapping(value = "/update/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updatebank(
            @PathVariable Long bankId,
            @RequestBody MainBankDTO dto) {
        logger.info("Update bank request for ID: {}", bankId);
        return mainBankService.updateBank(bankId, dto);
    }

    // ─────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // PATCH /test/api/v1/bank/update-status/{bankId}?status=ACTIVE
    // ─────────────────────────────────────────────
    @Operation(summary = "Update bank status only (ACTIVE / INACTIVE / PENDING / BLOCKED)")
    @PatchMapping(value = "/update-status/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long bankId,
            @RequestParam String status) {
        logger.info("Update status request for bank ID: {} → {}", bankId, status);
        return mainBankService.updateStatus(bankId, status);
    }

    // ─────────────────────────────────────────────
    // SOFT DELETE
    // DELETE /test/api/v1/bank/delete/{bankId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Soft delete bank (sets status to INACTIVE)")
    @DeleteMapping(value = "/delete/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deletebank(
            @PathVariable Long bankId) {
        logger.info("Delete bank request for ID: {}", bankId);
        return mainBankService.deleteBank(bankId);
    }

    // ─────────────────────────────────────────────
    // LOGO UPLOAD
    // POST /test/api/v1/bank/upload-logo/{bankId}
    // Content-Type: multipart/form-data
    // ─────────────────────────────────────────────
    @Operation(summary = "Upload bank logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{bankId}", consumes = "multipart/form-data",
            produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long bankId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        logger.info("Logo upload request for bank ID: {} | file: {} | size: {} bytes",
                bankId,
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);
        String uploadedBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.uploadLogo(bankId, file, uploadedBy);
    }

    // ─────────────────────────────────────────────
    // GENERATE BankCODE
    // GET /test/api/v1/bank/generate-code
    // ─────────────────────────────────────────────
    @Operation(summary = "Generate a unique 8-digit bank code")
    @GetMapping(value = "/generate-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateCode() {
        logger.info("Generate bank code request received");
        return mainBankService.generateCode();
    }

    // ─────────────────────────────────────────────
    // CHECK NAME EXISTS
    // GET /test/api/v1/bank/check-name?name=State Bank of India
    // ─────────────────────────────────────────────
    @Operation(summary = "Check if bank name already exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkNameExists(@RequestParam String name) {
        logger.info("Check bank name request: {}", name);
        return mainBankService.checkNameExists(name);
    }

    // ─────────────────────────────────────────────
    // CHECK EMAIL EXISTS
    // GET /test/api/v1/bank/check-email?email=rajesh@sbi.co.in
    // ─────────────────────────────────────────────
    @Operation(summary = "Check if primary contact email already exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(@RequestParam String email) {
        logger.info("Check bank email request: {}", email);
        return mainBankService.checkEmailExists(email);
    }

    // ─────────────────────────────────────────────
    // EXPORT EXCEL
    // GET /test/api/v1/bank/export/excel
    // ─────────────────────────────────────────────
    @Operation(summary = "Export all banks as Excel (.xlsx)")
    @GetMapping(value = "/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        logger.info("Export banks as Excel request received");
        return mainBankService.exportToExcel();
    }

    // ─────────────────────────────────────────────
    // EXPORT CSV
    // GET /test/api/v1/bank/export/csv
    // ─────────────────────────────────────────────
    @Operation(summary = "Export all banks as CSV")
    @GetMapping(value = "/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        logger.info("Export banks as CSV request received");
        return mainBankService.exportToCsv();
    }

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK
    // POST /test/api/v1/bank/schedule-block/{bankId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Schedule bank permanent block — auto-blocks after 30s (demo)")
    @PostMapping(value = "/schedule-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long bankId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule block request for bank ID: {} by {}", bankId, scheduledBy);
        return blockScheduleService.scheduleBlock(bankId, scheduledBy);
    }

    // ─────────────────────────────────────────────
    // UNDO BLOCK
    // POST /test/api/v1/bank/undo-block/{bankId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Undo scheduled block — only within 30s window (demo)")
    @PostMapping(value = "/undo-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoBlock(
            @PathVariable Long bankId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo block request for bank ID: {} by {}", bankId, undoneBy);
        return blockScheduleService.undoBlock(bankId, undoneBy);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERVE LOGO IMAGE
    // GET /test/api/v1/bank/logo/{bankCode}
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Serve bank logo image by bank code")
    @GetMapping(value = "/logo/{bankCode}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable String bankCode) {
        logger.info("Logo image request for bank code: {}", bankCode);
        return mainBankService.getLogoImage(bankCode);
    }

    @GetMapping(value = "/get-my-banks", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMybanks(Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.getBanksByCreatedBy(username);
    }

    @GetMapping(value = "/{bankId}/branch-banks", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getSubInstitutes(@PathVariable Long bankId) {
        logger.info("Fetching branch bank for parent bank ID: {}", bankId);
        return mainBankService.getBranchBank(bankId);
    }

    // GET /test/api/v1/bank/get-by-code/{bankCode}
    // Used by Bank Admin sidebar to display bank logo + short name
    @Operation(summary = "Get bank by bank code")
    @GetMapping(value = "/get-by-code/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getByCode(@PathVariable String bankCode) {
        logger.info("Get bank by code: {}", bankCode);
        return mainBankService.getBankByCode(bankCode);
    }

}
