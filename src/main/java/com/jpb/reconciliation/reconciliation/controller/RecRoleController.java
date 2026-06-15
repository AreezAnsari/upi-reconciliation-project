package com.jpb.reconciliation.reconciliation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.service.AdminContextResolver;
import com.jpb.reconciliation.reconciliation.service.RecRoleService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RecRoleController {

    private final RecRoleService       roleService;
    private final AdminContextResolver contextResolver;

    @GetMapping("/modules")
    public ResponseEntity<RestWithStatusList> getModules() {
        return ResponseEntity.ok(roleService.getAllModules());
    }

    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(
            @RequestBody RecCreateRoleRequestDTO req,
            Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        log.info("Creating role: {} by user: {}", req.getRoleNames(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(req, authentication));
    }

    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllRoles(Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        log.info("Fetching roles for user: {}", authentication.getName());
        return ResponseEntity.ok(roleService.getAllRolesByCreator(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getRole(
            @PathVariable Long id,
            Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        return ResponseEntity.ok(roleService.getRole(id, authentication));
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

    private ResponseEntity<RestWithStatusList> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Access denied: only Bank Admin or Branch Admin can perform this action")
                        .data(Collections.emptyList())
                        .build());
    }
}
