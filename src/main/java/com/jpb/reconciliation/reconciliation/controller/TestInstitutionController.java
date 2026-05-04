package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.jpb.reconciliation.reconciliation.dto.TestInstitutionDTO;
import com.jpb.reconciliation.reconciliation.service.TestInstitutionService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/test/api/v1/institution")
@CrossOrigin(origins = "*")
public class TestInstitutionController {

    Logger logger = LoggerFactory.getLogger(TestInstitutionController.class);

    @Autowired
    TestInstitutionService testInstitutionService;

    // ─────────────────────────────────────────────
    // CREATE
    // POST /test/api/v1/institution/create
    // ─────────────────────────────────────────────
    @Operation(summary = "Onboard a new institution")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createInstitution(
            @RequestBody TestInstitutionDTO dto) {
        logger.info("Create institution request received: {}", dto.getInstitutionNameFull());
        return testInstitutionService.createInstitution(dto);
    }

    // ─────────────────────────────────────────────
    // GET ALL
    // GET /test/api/v1/institution/get-all
    // ─────────────────────────────────────────────
    @Operation(summary = "Get all institutions")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        logger.info("Fetch all institutions request received");
        return testInstitutionService.getAllInstitutions();
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
        return testInstitutionService.getInstitutionById(institutionId);
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
        return testInstitutionService.getInstitutionsByStatus(status);
    }

    // ─────────────────────────────────────────────
    // FULL UPDATE
    // PUT /test/api/v1/institution/update/{institutionId}
    // ─────────────────────────────────────────────
    @Operation(summary = "Full update of institution details")
    @PutMapping(value = "/update/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateInstitution(
            @PathVariable Long institutionId,
            @RequestBody TestInstitutionDTO dto) {
        logger.info("Update institution request for ID: {}", institutionId);
        return testInstitutionService.updateInstitution(institutionId, dto);
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
        return testInstitutionService.updateStatus(institutionId, status);
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
        return testInstitutionService.deleteInstitution(institutionId);
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
            @RequestPart("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logo upload request for institution ID: {} | file: {} | size: {} bytes",
                institutionId,
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);
        return testInstitutionService.uploadLogo(institutionId, file, userDetails.getUsername());
    }
    
    @GetMapping(value = "/verify-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> verifyEmail(@RequestParam String token) {
        return testInstitutionService.verifyEmail(token);
    }
}