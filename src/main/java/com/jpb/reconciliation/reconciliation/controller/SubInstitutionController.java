package com.jpb.reconciliation.reconciliation.controller;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
 
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.SubInstitutionDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.SubInstitutionService;
 
import io.swagger.v3.oas.annotations.Operation;
 
// ✅ FIX: Class name changed to SubInstitutionController
//         (was InstitutionController — conflict with the main InstitutionController)
@RestController
@RequestMapping(path = "/test/api/v1/subinstitution")
@CrossOrigin(origins = "*")
public class SubInstitutionController {
 
    Logger logger = LoggerFactory.getLogger(SubInstitutionController.class);
 
    @Autowired
    SubInstitutionService institutionService;
 
    // POST /test/api/v1/subinstitution/create
    @Operation(summary = "Onboard a new sub-institution")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createInstitution(
            @RequestBody SubInstitutionDTO dto) {
        logger.info("Create sub-institution request: {}", dto.getInstitutionNameFull());
        return institutionService.createInstitution(dto);
    }
 
    // GET /test/api/v1/subinstitution/get-all
    @Operation(summary = "Get all sub-institutions")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        logger.info("Fetch all sub-institutions");
        return institutionService.getAllInstitutions();
    }
 
    // GET /test/api/v1/subinstitution/get/{institutionId}
    @Operation(summary = "Get sub-institution by ID")
    @GetMapping(value = "/get/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionById(
            @PathVariable Long institutionId) {
        logger.info("Fetch sub-institution by ID: {}", institutionId);
        return institutionService.getInstitutionById(institutionId);
    }
 
    // GET /test/api/v1/subinstitution/get-by-status?status=ACTIVE
    @Operation(summary = "Get sub-institutions by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getInstitutionsByStatus(
            @RequestParam String status) {
        logger.info("Fetch sub-institutions by status: {}", status);
        return institutionService.getInstitutionsByStatus(status);
    }
 
    // PUT /test/api/v1/subinstitution/update/{institutionId}
    @Operation(summary = "Full update of sub-institution details")
    @PutMapping(value = "/update/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateInstitution(
            @PathVariable Long institutionId,
            @RequestBody SubInstitutionDTO dto) {
        logger.info("Update sub-institution ID: {}", institutionId);
        return institutionService.updateInstitution(institutionId, dto);
    }
 
    // PATCH /test/api/v1/subinstitution/update-status/{institutionId}?status=ACTIVE
    @Operation(summary = "Update sub-institution status")
    @PatchMapping(value = "/update-status/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long institutionId,
            @RequestParam String status) {
        logger.info("Update sub-institution {} status → {}", institutionId, status);
        return institutionService.updateStatus(institutionId, status);
    }
 
    // DELETE /test/api/v1/subinstitution/delete/{institutionId}
    @Operation(summary = "Soft delete sub-institution (status → INACTIVE)")
    @DeleteMapping(value = "/delete/{institutionId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteInstitution(
            @PathVariable Long institutionId) {
        logger.info("Delete sub-institution ID: {}", institutionId);
        return institutionService.deleteInstitution(institutionId);
    }
 
    // POST /test/api/v1/subinstitution/upload-logo/{institutionId}
    @Operation(summary = "Upload sub-institution logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{institutionId}", consumes = "multipart/form-data",
            produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long institutionId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logo upload for sub-institution ID: {}", institutionId);
        return institutionService.uploadLogo(institutionId, file, userDetails.getUsername());
    }
 
    // GET /test/api/v1/subinstitution/verify-email?token=xxx
    @GetMapping(value = "/verify-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> verifyEmail(@RequestParam String token) {
        return institutionService.verifyEmail(token);
    }
 // GET /test/api/v1/subinstitution/check-email?email=abc@gmail.com
    @Operation(summary = "Check sub institution email already exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(
            @RequestParam String email) {

        logger.info("Check sub institution email request: {}", email);

        return institutionService.checkEmailExists(email);
    }
}
