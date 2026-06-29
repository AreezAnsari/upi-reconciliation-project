package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.MainBankDTO;

public interface MainBankService {

    // Create new bank
    ResponseEntity<RestWithStatusList> createbank(MainBankDTO dto, String createdBy);

    // Get all banks
    ResponseEntity<RestWithStatusList> getAllBanks();

    // Get by ID
    ResponseEntity<RestWithStatusList> getBankById(Long bankId);

    // Get by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    ResponseEntity<RestWithStatusList> getBanksByStatus(String status);

    // Full update (edit form submit)
    ResponseEntity<RestWithStatusList> updateBank(Long bankId, MainBankDTO dto);

    // Status-only update (toggle ACTIVE/INACTIVE/BLOCKED)
    ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status);

    // Soft delete → sets status to INACTIVE
    ResponseEntity<RestWithStatusList> deleteBank(Long bankId);

    // Logo file upload → saves file to disk, updates logo_path in DB
    ResponseEntity<RestWithStatusList> uploadLogo(Long bankId, MultipartFile file, String logoUploader);

    ResponseEntity<RestWithStatusList> verifyEmail(String token);

    // Check if bank name already exists — used for Step 1 real-time validation
    ResponseEntity<RestWithStatusList> checkNameExists(String name);

    // Export banks as Excel
    ResponseEntity<byte[]> exportToExcel() throws java.io.IOException;

    // Export banks as CSV
    ResponseEntity<byte[]> exportToCsv();

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> getBanksByCreatedBy(String username);

    ResponseEntity<RestWithStatusList> getBranchBank(Long parentBankId);

    // Generate a unique 8-digit bank code (epoch-based)
    ResponseEntity<RestWithStatusList> generateCode();

    // Serve bank logo image by bank code
    ResponseEntity<byte[]> getLogoImage(String bankCode);

    // Get bank by code (used by SuperUser sidebar to display bank info)
    ResponseEntity<RestWithStatusList> getBankByCode(String bankCode);

}
