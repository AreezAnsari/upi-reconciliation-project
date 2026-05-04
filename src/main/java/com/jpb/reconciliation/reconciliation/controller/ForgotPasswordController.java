package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.service.ForgotPasswordService;

@RestController
@RequestMapping("/auth")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    /**
     * STEP 1 — Forgot Password
     * User submits their registered email ID.
     * System generates OTP and sends it to the email.
     *
     * POST /auth/forgot-password
     * Request Body: { "emailId": "john.doe@kalinfotech.com" }
     */
    @PostMapping(value = "/forgot-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ResponseDto> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return forgotPasswordService.forgotPassword(request);
    }

    /**
     * STEP 2 — Reset Password
     * User submits email + OTP received + new password + confirm password.
     * System validates OTP and updates password.
     *
     * POST /auth/reset-password
     * Request Body: {
     *   "emailId": "john.doe@kalinfotech.com",
     *   "otpCode": "482910",
     *   "newPassword": "NewPass@123",
     *   "confirmNewPassword": "NewPass@123"
     * }
     */
    @PostMapping(value = "/reset-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ResponseDto> resetPassword(@RequestBody ResetPasswordRequest request) {
        return forgotPasswordService.resetPassword(request);
    }
}