package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;

public interface ForgotPasswordService {

    ResponseEntity<ForgotPasswordResponseDto> forgotPassword(ForgotPasswordRequest request);

    ResponseEntity<ForgotPasswordResponseDto> resetPassword(ResetPasswordRequest request);
}