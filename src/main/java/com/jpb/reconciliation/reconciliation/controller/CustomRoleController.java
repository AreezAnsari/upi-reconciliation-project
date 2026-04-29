package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.CustomRoleRequestDto;
import com.jpb.reconciliation.reconciliation.dto.CustomRoleResponseDto;
import com.jpb.reconciliation.reconciliation.service.CustomRoleService;

@RestController
@RequestMapping("/api/v1/roles")
public class CustomRoleController {

    private final CustomRoleService service;

    public CustomRoleController(CustomRoleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CustomRoleResponseDto> create(@RequestBody CustomRoleRequestDto dto) {
        return new ResponseEntity<>(service.createRole(dto), HttpStatus.CREATED);
    }
}