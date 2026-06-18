package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BranchAdminService {
    ResponseEntity<RestWithStatusList> verifyEmail(String bankCode, String username);
    ResponseEntity<RestWithStatusList> checkUserStatus(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> verifyCredentials(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> setNewPassword(BranchAdminSetPasswordDto dto);
    ResponseEntity<RestWithStatusList> login(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> directLogin(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequestDto request);
    ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequestDto request);
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);
    ResponseEntity<RestWithStatusList> activateBranchAdmin(String email);

    ResponseEntity<RestWithStatusList> scheduleInactivate(Long id, String scheduledBy);
    ResponseEntity<RestWithStatusList> undoInactivate(Long id, String undoneBy);
    ResponseEntity<RestWithStatusList> scheduleReactivate(Long id, String scheduledBy);
    ResponseEntity<RestWithStatusList> undoReactivate(Long id, String undoneBy);
    ResponseEntity<RestWithStatusList> scheduleBlock(Long id, String scheduledBy, String reason);
    ResponseEntity<RestWithStatusList> undoBlock(Long id, String undoneBy);

    ResponseEntity<RestWithStatusList> scheduleInactivateByBranchBankId(Long branchBankId, String scheduledBy);
    ResponseEntity<RestWithStatusList> undoInactivateByBranchBankId(Long branchBankId, String undoneBy);
    ResponseEntity<RestWithStatusList> scheduleReactivateByBranchBankId(Long branchBankId, String scheduledBy);
    ResponseEntity<RestWithStatusList> undoReactivateByBranchBankId(Long branchBankId, String undoneBy);
    ResponseEntity<RestWithStatusList> scheduleBlockByBranchBankId(Long branchBankId, String scheduledBy, String reason);
    ResponseEntity<RestWithStatusList> undoBlockByBranchBankId(Long branchBankId, String undoneBy);
}
