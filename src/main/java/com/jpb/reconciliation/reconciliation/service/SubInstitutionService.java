package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.SubInstitutionDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface SubInstitutionService {

    ResponseEntity<RestWithStatusList> createInstitution(
            SubInstitutionDTO dto,
            String createdBy);

    ResponseEntity<RestWithStatusList> getAllInstitutions();

    ResponseEntity<RestWithStatusList> getInstitutionById(Long institutionId);

    ResponseEntity<RestWithStatusList> getInstitutionsByStatus(String status);

    ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, SubInstitutionDTO dto);

    ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status);

    ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId);

    ResponseEntity<RestWithStatusList> uploadLogo(Long institutionId, MultipartFile file, String logoUploader);

    ResponseEntity<RestWithStatusList> verifyEmail(String token);

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> checkNameExists(String name);

    ResponseEntity<byte[]> exportToExcel() throws java.io.IOException;

    ResponseEntity<byte[]> exportToCsv();
}