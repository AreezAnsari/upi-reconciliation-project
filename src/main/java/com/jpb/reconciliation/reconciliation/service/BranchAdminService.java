package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.BranchAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BranchAdminService {
    ResponseEntity<RestWithStatusList> verifyEmail(String institutionCode, String username);
    ResponseEntity<RestWithStatusList> checkUserStatus(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> verifyCredentials(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> setNewPassword(BranchAdminSetPasswordDto dto);
    ResponseEntity<RestWithStatusList> login(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> directLogin(BranchAdminVerifyDto dto);
    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request);
    ResponseEntity<RestWithStatusList> verifyForgotOtp(ForgotPasswordRequest request);
    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);
    ResponseEntity<RestWithStatusList> activateBranchAdmin(String email);
}
