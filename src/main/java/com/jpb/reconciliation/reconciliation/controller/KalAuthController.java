package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.KalEmployeeDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.KalAuthService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/kalinfotech/auth")
public class KalAuthController {

    private Logger logger = LoggerFactory.getLogger(KalAuthController.class);

    @Autowired
    private KalAuthService kalAuthService;

    // ─────────────────────────────────────────────
    // REGISTER
    // POST /api/kalinfotech/auth/register
    // Called from Create.jsx — no token needed (public endpoint)
    // ─────────────────────────────────────────────
    @Operation(summary = "Register a new KalInfotech employee")
    @PostMapping(value = "/register", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> register(@RequestBody KalEmployeeDto dto) {
        logger.info("Register request received for username: {}", dto.getUsername());
        return kalAuthService.register(dto);
    }
}