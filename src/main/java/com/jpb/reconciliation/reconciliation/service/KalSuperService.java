package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface KalSuperService {

    ResponseEntity<RestWithStatusList> verifyCredentials(KalSuperUserVerifyDto dto);

    ResponseEntity<RestWithStatusList> setNewPassword(KalSuperUserSetPasswordDto dto);

<<<<<<< HEAD
    ResponseEntity<RestWithStatusList> login(KalSuperUserVerifyDto dto);

    ResponseEntity<RestWithStatusList> forgotPassword(ForgotPasswordRequest request);

    ResponseEntity<RestWithStatusList> resetPassword(ResetPasswordRequest request);
=======
    // ✅ Sirf yeh add karo
    ResponseEntity<RestWithStatusList>
    login(KalSuperUserVerifyDto dto);

    // ── Called after OTP verified — sets institution status to ACTIVE ──
    ResponseEntity<RestWithStatusList>
    activateInstitution(String email);
>>>>>>> origin/feature/areez-ui
}