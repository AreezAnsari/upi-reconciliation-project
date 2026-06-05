package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BranchBankService {

    ResponseEntity<RestWithStatusList> createInstitution(
            BranchBankDTO dto,
            String createdBy);

    ResponseEntity<RestWithStatusList> getAllInstitutions();

    ResponseEntity<RestWithStatusList> getInstitutionById(Long institutionId);

    ResponseEntity<RestWithStatusList> getInstitutionsByStatus(String status);

    ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, BranchBankDTO dto);

    ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status);

    ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId);

    ResponseEntity<RestWithStatusList> uploadLogo(Long institutionId, MultipartFile file, String logoUploader);

    ResponseEntity<RestWithStatusList> verifyEmail(String token);

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> checkNameExists(String name);

    ResponseEntity<RestWithStatusList> generateCode(String createdBy);

    ResponseEntity<byte[]> exportToExcel() throws java.io.IOException;

    ResponseEntity<byte[]> exportToCsv();

    ResponseEntity<RestWithStatusList> scheduleBlock(Long institutionId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoBlock(Long institutionId, String undoneBy);

    ResponseEntity<byte[]> getLogoImage(String institutionCode);
}
