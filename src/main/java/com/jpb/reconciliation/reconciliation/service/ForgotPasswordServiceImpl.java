package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    @Override
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmailId();

        // TODO: generate OTP + send email

        return ResponseEntity.ok(
                new ForgotPasswordResponseDto("OTP sent successfully to " + email)
        );
    }

    @Override
    public ResponseEntity<ForgotPasswordResponseDto> resetPassword(ResetPasswordRequest request) {

        // TODO: validate OTP + update password

        return ResponseEntity.ok(
                new ForgotPasswordResponseDto("Password reset successful")
        );
    }
}