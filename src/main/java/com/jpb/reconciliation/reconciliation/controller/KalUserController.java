package com.jpb.reconciliation.reconciliation.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.KalLoginDto;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.service.KalUserService;

@RestController
@RequestMapping("/api/kalinfotech/auth")
public class KalUserController {

    @Autowired
    private KalUserService service;

    /**
     * POST /api/kalinfotech/auth/register
     * Registers a new KalInfotech employee.
     * Public endpoint — no token required (configured in SecurityConfig).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody KalUserDto dto
    ) {
        return ResponseEntity.ok(service.register(dto));
    }

    /**
     * POST /api/kalinfotech/auth/login
     * Logs in a KalInfotech employee.
     * Public endpoint — no token required (configured in SecurityConfig).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody KalLoginDto dto) {
    	 System.out.println("LOGIN CONTROLLER HIT");
        return ResponseEntity.ok(
                service.login(dto.getUsername(), dto.getPassword())
        );
    
    }
}
