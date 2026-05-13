package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.service.ForgotPasswordService;

@RestController
@RequestMapping("/auth")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @PostMapping(value = "/forgot-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        return forgotPasswordService.forgotPassword(request);
    }

    @PostMapping(value = "/reset-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ForgotPasswordResponseDto> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        return forgotPasswordService.resetPassword(request);
    }
}