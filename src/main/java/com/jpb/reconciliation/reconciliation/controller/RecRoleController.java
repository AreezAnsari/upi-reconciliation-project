package com.jpb.reconciliation.reconciliation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.service.RecRoleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles") 
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RecRoleController {

    private final RecRoleService roleService; // use interface, not impl

    //  GET Modules
    @GetMapping("/modules")
    public ResponseEntity<RestWithStatusList> getModules() {
        return ResponseEntity.ok(roleService.getAllModules());
    }

    // CREATE Role
    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(@RequestBody RecCreateRoleRequestDTO req) {
        log.info("Creating role: {} with status: {}", req.getRoleName(), req.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(req));
    }

    // GET Role
    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRole(id));
    }

    // UPDATE Permissions
    @PutMapping("/{id}/permissions")
    public ResponseEntity<RestWithStatusList> updatePermissions(
            @PathVariable Long id,
            @RequestBody List<RecPermissionRowDTO> dtos) {

        return ResponseEntity.ok(roleService.updatePermissions(id, dtos));
    }
}