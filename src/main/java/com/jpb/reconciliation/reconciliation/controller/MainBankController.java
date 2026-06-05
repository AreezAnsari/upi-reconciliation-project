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
@RequestMapping(path = "/test/api/v1/institution")
@CrossOrigin(origins = "*")
public class MainBankController {

    Logger logger = LoggerFactory.getLogger(MainBankController.class);

    @Autowired
    MainBankService mainBankService;

    @Autowired
    BlockScheduleService blockScheduleService;

    // ─────────────────────────────────────────────
    // CREATE
    // POST /test/api/v1/institution/create
    // ─────────────────────────────────────────────
    @Operation(summary = "Onboard a new institution")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createInstitution(
            @RequestBody MainBankDTO dto,
            Authentication authentication) {
        logger.info("Create institution request received: {}", dto.getInstitutionNameFull());
        String createdBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.createInstitution(dto, createdBy);
    }

    // ─────────────────────────────────────────────
    // GET ALL
    // GET /test/api/v1/institution/get-all
    // ─────────────────────────────────────────────
    @Operation(summary = "Get all institutions")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        logger.info("Fetch all institutions request received");
        return mainBankService.getAllInstitutions();
    }

    // ─────────────────────────────────────────────
    // GET BY ID
    // GET /test/api/v1/institution/get/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Get institution by ID")
    @GetMapping(value = "/get/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionById(
            @PathVariable Long institutionId) {
        logger.info("Fetch institution by ID: {}", institutionId);
        return mainBankService.getInstitutionById(institutionId);
    }

    // ─────────────────────────────────────────────
    // GET BY STATUS
    // GET /test/api/v1/institution/get-by-status?status=ACTIVE
    // ─────────────────────────────────────────────
    @Operation(summary = "Get institutions by status (ACTIVE / INACTIVE / PENDING / BLOCKED)")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionsByStatus(
            @RequestParam String status) {
        logger.info("Fetch institutions by status: {}", status);
        return mainBankService.getInstitutionsByStatus(status);
    }

    // ─────────────────────────────────────────────
    // FULL UPDATE
    // PUT /test/api/v1/institution/update/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Full update of institution details")
    @PutMapping(value = "/update/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateInstitution(
            @PathVariable Long institutionId,
            @RequestBody MainBankDTO dto) {
        logger.info("Update institution request for ID: {}", institutionId);
        return mainBankService.updateInstitution(institutionId, dto);
    }

    // ─────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // PATCH /test/api/v1/institution/update-status/{institutionId}?status=ACTIVE
    // ─────────────────────────────────────────────
    @Operation(summary = "Update institution status only (ACTIVE / INACTIVE / PENDING / BLOCKED)")
    @PatchMapping(value = "/update-status/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long institutionId,
            @RequestParam String status) {
        logger.info("Update status request for institution ID: {} → {}", institutionId, status);
        return mainBankService.updateStatus(institutionId, status);
    }

    // ─────────────────────────────────────────────
    // SOFT DELETE
    // DELETE /test/api/v1/institution/delete/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Soft delete institution (sets status to INACTIVE)")
    @DeleteMapping(value = "/delete/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteInstitution(
            @PathVariable Long institutionId) {
        logger.info("Delete institution request for ID: {}", institutionId);
        return mainBankService.deleteInstitution(institutionId);
    }

    // ─────────────────────────────────────────────
    // LOGO UPLOAD
    // POST /test/api/v1/institution/upload-logo/{institutionId}
    // Content-Type: multipart/form-data
    // ─────────────────────────────────────────────
    @Operation(summary = "Upload institution logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{institutionId}", consumes = "multipart/form-data",
            produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long institutionId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        logger.info("Logo upload request for institution ID: {} | file: {} | size: {} bytes",
                institutionId,
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);
        String uploadedBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.uploadLogo(institutionId, file, uploadedBy);
    }

    @GetMapping(value = "/verify-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> verifyEmail(@RequestParam String token) {
        return mainBankService.verifyEmail(token);
    }

    // ─────────────────────────────────────────────
    // GENERATE INSTITUTION CODE
    // GET /test/api/v1/institution/generate-code
    // ─────────────────────────────────────────────
    @Operation(summary = "Generate a unique 8-digit institution code")
    @GetMapping(value = "/generate-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateCode() {
        logger.info("Generate institution code request received");
        return mainBankService.generateCode();
    }

    // ─────────────────────────────────────────────
    // CHECK NAME EXISTS
    // GET /test/api/v1/institution/check-name?name=State Bank of India
    // ─────────────────────────────────────────────
    @Operation(summary = "Check if institution name already exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkNameExists(@RequestParam String name) {
        logger.info("Check institution name request: {}", name);
        return mainBankService.checkNameExists(name);
    }

    // ─────────────────────────────────────────────
    // CHECK EMAIL EXISTS
    // GET /test/api/v1/institution/check-email?email=rajesh@sbi.co.in
    // ─────────────────────────────────────────────
    @Operation(summary = "Check if primary contact email already exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(@RequestParam String email) {
        logger.info("Check institution email request: {}", email);
        return mainBankService.checkEmailExists(email);
    }

    // ─────────────────────────────────────────────
    // EXPORT EXCEL
    // GET /test/api/v1/institution/export/excel
    // ─────────────────────────────────────────────
    @Operation(summary = "Export all institutions as Excel (.xlsx)")
    @GetMapping(value = "/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        logger.info("Export institutions as Excel request received");
        return mainBankService.exportToExcel();
    }

    // ─────────────────────────────────────────────
    // EXPORT CSV
    // GET /test/api/v1/institution/export/csv
    // ─────────────────────────────────────────────
    @Operation(summary = "Export all institutions as CSV")
    @GetMapping(value = "/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        logger.info("Export institutions as CSV request received");
        return mainBankService.exportToCsv();
    }

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK
    // POST /test/api/v1/institution/schedule-block/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Schedule institution permanent block — auto-blocks after 30s (demo)")
    @PostMapping(value = "/schedule-block/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long institutionId,
            Authentication authentication) {
        String scheduledBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Schedule block request for institution ID: {} by {}", institutionId, scheduledBy);
        return blockScheduleService.scheduleBlock(institutionId, scheduledBy);
    }

    // ─────────────────────────────────────────────
    // UNDO BLOCK
    // POST /test/api/v1/institution/undo-block/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Undo scheduled block — only within 30s window (demo)")
    @PostMapping(value = "/undo-block/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> undoBlock(
            @PathVariable Long institutionId,
            Authentication authentication) {
        String undoneBy = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        logger.info("Undo block request for institution ID: {} by {}", institutionId, undoneBy);
        return blockScheduleService.undoBlock(institutionId, undoneBy);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERVE LOGO IMAGE
    // GET /test/api/v1/institution/logo/{institutionCode}
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(summary = "Serve institution logo image by institution code")
    @GetMapping(value = "/logo/{institutionCode}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable String institutionCode) {
        logger.info("Logo image request for institution code: {}", institutionCode);
        return mainBankService.getLogoImage(institutionCode);
    }

    @GetMapping(value = "/get-my-institutions", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMyInstitutions(Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return mainBankService.getInstitutionsByCreatedBy(username);
    }

    @GetMapping(value = "/{institutionId}/sub-institutes", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getSubInstitutes(@PathVariable Long institutionId) {
        logger.info("Fetching sub-institutes for parent institution ID: {}", institutionId);
        return mainBankService.getSubInstitutes(institutionId);
    }

    // GET /test/api/v1/institution/get-by-code/{institutionCode}
    // Used by SuperUser sidebar to display bank logo + short name
    @Operation(summary = "Get institution by institution code")
    @GetMapping(value = "/get-by-code/{institutionCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getByCode(@PathVariable String institutionCode) {
        logger.info("Get institution by code: {}", institutionCode);
        return mainBankService.getInstitutionByCode(institutionCode);
    }

}
