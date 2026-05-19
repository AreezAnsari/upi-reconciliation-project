package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.SubInstitutionDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface SubInstitutionService {

    // Create new institution
    ResponseEntity<RestWithStatusList> createInstitution(SubInstitutionDTO dto);

    // Get all institutions
    ResponseEntity<RestWithStatusList> getAllInstitutions();

    // Get by ID
    ResponseEntity<RestWithStatusList> getInstitutionById(Long institutionId);

    // Get by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    ResponseEntity<RestWithStatusList> getInstitutionsByStatus(String status);

    // Full update (edit form submit)
    ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, SubInstitutionDTO dto);

    // Status-only update (toggle ACTIVE/INACTIVE/BLOCKED)
    ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status);

    // Soft delete → sets status to INACTIVE
    ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId);

    // Logo file upload → saves file to disk, updates logo_path in DB
    ResponseEntity<RestWithStatusList> uploadLogo(Long institutionId, MultipartFile file, String logoUploader);

    ResponseEntity<RestWithStatusList> verifyEmail(String token);
    ResponseEntity<RestWithStatusList> checkEmailExists(String email);
    ResponseEntity<byte[]> exportToExcel() throws java.io.IOException;

    // Export institutions as CSV
    ResponseEntity<byte[]> exportToCsv();
}