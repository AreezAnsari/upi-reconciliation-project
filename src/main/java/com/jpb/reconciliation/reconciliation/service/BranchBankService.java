package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BranchBankService {

    ResponseEntity<RestWithStatusList> createBank(
            BranchBankDTO dto,
            String createdBy);

    ResponseEntity<RestWithStatusList> getAllBanks(String loggedInUsername);

    ResponseEntity<RestWithStatusList> getBankById(Long bankId);

    ResponseEntity<RestWithStatusList> getBanksByStatus(String status);

    ResponseEntity<RestWithStatusList> updateBank(Long bankId, BranchBankDTO dto);

    ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status);

    ResponseEntity<RestWithStatusList> deleteBank(Long bankId);

    ResponseEntity<RestWithStatusList> uploadLogo(Long bankId, MultipartFile file, String logoUploader);

    ResponseEntity<RestWithStatusList> verifyEmail(String bankCode, String username);

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> checkNameExists(String name);

    ResponseEntity<RestWithStatusList> generateCode(String createdBy);

    ResponseEntity<byte[]> exportToExcel() throws java.io.IOException;

    ResponseEntity<byte[]> exportToCsv();

    ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoBlock(Long bankId, String undoneBy);

    ResponseEntity<byte[]> getLogoImage(String bankCode);

    ResponseEntity<RestWithStatusList> getBankByCode(String bankCode);

    ResponseEntity<RestWithStatusList> getBankByEmail(String email);
}
