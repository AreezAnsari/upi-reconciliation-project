package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;

public interface ForgotPasswordService {

    // Step 1: Validate email → generate OTP → send email
    ResponseEntity<ResponseDto> forgotPassword(ForgotPasswordRequest request);

    // Step 2: Validate OTP + new password → update password in DB
    ResponseEntity<ResponseDto> resetPassword(ResetPasswordRequest request);
}