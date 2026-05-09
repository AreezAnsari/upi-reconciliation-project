package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.KalSuperUserSetPasswordDto;
import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.KalSuperService;

@RestController
@RequestMapping("/test/api/v1/institution") // ✅ Same mapping rakho — Spring handle karega
public class KalSuperUserController {

    @Autowired
    private KalSuperService kalSuperService;

    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(
            @RequestBody KalSuperUserVerifyDto dto) {
        return kalSuperService.verifyCredentials(dto);
    }

    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(
            @RequestBody KalSuperUserSetPasswordDto dto) {
        return kalSuperService.setNewPassword(dto);
    }

    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(
            @RequestBody KalSuperUserVerifyDto dto) {
        return kalSuperService.login(dto);
    }
}