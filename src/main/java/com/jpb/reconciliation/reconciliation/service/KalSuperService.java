package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface KalSuperService {

    ResponseEntity<RestWithStatusList>
    verifyCredentials(KalSuperUserVerifyDto dto);

    ResponseEntity<RestWithStatusList>
    setNewPassword(KalSuperUserSetPasswordDto dto);

    // ✅ Sirf yeh add karo
    ResponseEntity<RestWithStatusList>
    login(KalSuperUserVerifyDto dto);

    // ── Called after OTP verified — sets institution status to ACTIVE ──
    ResponseEntity<RestWithStatusList>
    activateInstitution(String email);
}