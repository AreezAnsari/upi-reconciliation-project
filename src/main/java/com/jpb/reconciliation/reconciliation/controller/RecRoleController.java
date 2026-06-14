package com.jpb.reconciliation.reconciliation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.service.RecRoleService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RecRoleController {

    private final RecRoleService roleService;

    @GetMapping("/modules")
    public ResponseEntity<RestWithStatusList> getModules() {
        return ResponseEntity.ok(roleService.getAllModules());
    }

    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(@RequestBody RecCreateRoleRequestDTO req) {
        log.info("Creating role: {} with status: {}", req.getRoleNames(), req.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(req));
    }

    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        log.info("Fetching all roles");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRole(id));
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RestWithStatusList> updatePermissions(
            @PathVariable Long id,
            @RequestBody List<RecPermissionRowDTO> dtos) {
        return ResponseEntity.ok(roleService.updatePermissions(id, dtos));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestWithStatusList> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg(ex.getMessage())
                        .data(Collections.emptyList())
                        .build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestWithStatusList> handleServerError(RuntimeException ex) {
        log.error("Server error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Unexpected error: " + ex.getMessage())
                        .data(Collections.emptyList())
                        .build());
    }
}
