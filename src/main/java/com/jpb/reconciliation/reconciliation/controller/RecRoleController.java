package com.jpb.reconciliation.reconciliation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.repository.BranchProductRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankProductRepository;
import com.jpb.reconciliation.reconciliation.service.RecRoleCodeGeneratorService;
import com.jpb.reconciliation.reconciliation.service.RecRoleService;

import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/roles") 
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RecRoleController {

    private final RecRoleService roleService;
    private final RecRoleCodeGeneratorService codeGenerator;
    private final BranchProductRepository     branchProductRepo;
    private final MainBankProductRepository   bankProductRepo;
    private final MainBankRepository mainBankRepository;

    //  GET Modules
    @GetMapping("/modules")
    public ResponseEntity<RestWithStatusList> getAllModules() {
        return ResponseEntity.ok(roleService.getAllModules());
    }

    // CREATE Role
    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(@RequestBody RecCreateRoleRequestDTO req) {
        log.info("Creating role: {} with status: {}", req.getRoleNames()); //req.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(req));
    }
    
    //UPDATE ROLE BY ID 
    @PutMapping("/update/{id}")
    public ResponseEntity<RestWithStatusList> updateRole(
            @PathVariable Long id,
            @RequestBody RecCreateRoleRequestDTO request) {

        return ResponseEntity.ok(
                roleService.updateRole(id, request));
    }
    
    // SOFT DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList> deleteRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.deleteRole(id));
    }
    
    // ── GET Branch Purchased Products ──────────────────────────────────────────
    /**
     * GET /api/v1/roles/branch-products?branchCode=87822385
     *
     * Fetches all products purchased by a branch.
     * Accepts branchCode (string from auth store) instead of numeric branchId.
     * Bank Admin (no branchCode) → returns empty list (no products locked).
     */
    @GetMapping("/branch-products")
    public ResponseEntity<Map<String, Object>> getBranchProducts(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String bankCode) {

        log.info("Fetching products: branchCode={}, bankCode={}", branchCode, bankCode);

        Map<String, Object> response = new HashMap<>();

        try {
            List<String> rawProducts;
            String resolvedBankCode = bankCode;

            if (branchCode != null && !branchCode.trim().isEmpty()) {
                // Branch Admin — products purchased by this specific branch
                rawProducts = branchProductRepo.findProductNamesByBranchCode(branchCode.trim());
                response.put("branchCode", branchCode);
            } else if (bankCode != null && !bankCode.trim().isEmpty()) {
                // Bank Admin — products from MAIN_BANK_PRODUCT (bank-level table)
                rawProducts = bankProductRepo.findProductNamesByBankCode(bankCode.trim());
                response.put("bankCode", bankCode);
            } else {
                // KAL Super Admin — no scope, return empty
                response.put("status",            "SUCCESS");
                response.put("purchasedProducts", Collections.emptyList());
                response.put("isIssuer",          false);
                response.put("isAcquirer",        false);
                return ResponseEntity.ok(response);
            }

            List<String> purchasedProducts = new ArrayList<>();
            for (String p : rawProducts) {
                if (p != null && !p.trim().isEmpty()) {
                    purchasedProducts.add(p.trim().toUpperCase());
                }
            }
            
            boolean isIssuer   = false;
            boolean isAcquirer = false;
            if (resolvedBankCode != null && !resolvedBankCode.trim().isEmpty()) {
                isIssuer   = mainBankRepository.findByBankCode(resolvedBankCode.trim())
                        .map(this::hasIssuerFlag)
                        .orElse(false);
                isAcquirer = mainBankRepository.findByBankCode(resolvedBankCode.trim())
                        .map(this::hasAcquirerFlag)
                        .orElse(false);
            }

            log.info("Products found: {}, isIssuer={}, isAcquirer={} ", purchasedProducts , isIssuer, isAcquirer);

            response.put("status",            "SUCCESS");
            response.put("purchasedProducts", purchasedProducts);
            response.put("isIssuer",          isIssuer);
            response.put("isAcquirer",        isAcquirer);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching products: {}", e.getMessage(), e);

            Map<String, Object> err = new HashMap<>();
            err.put("status",            "FAILURE");
            err.put("statusMsg",         "Could not fetch products: " + e.getMessage());
            err.put("purchasedProducts", Collections.emptyList());
            err.put("isIssuer",          false);
            err.put("isAcquirer",        false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
    
    // ── PREVIEW Role Code (estimate only — does NOT consume sequence) ─────────
    
    @GetMapping("/preview-code")
    public ResponseEntity<Map<String, Object>> previewRoleCode(
            @RequestParam String roleName) {
 
        log.info("Preview code requested for roleName={}", roleName);
 
        try {
            String previewCode = codeGenerator.peekNextCode(roleName);
 
            Map<String, Object> response = new HashMap<>();
            response.put("status",      "SUCCESS");
            response.put("previewCode", previewCode);
            response.put("isEstimate",  true);
            response.put("roleName",    roleName.trim().toUpperCase());
 
            log.info("Preview code for roleName={} → {}", roleName, previewCode);
            return ResponseEntity.ok(response);
 
        } catch (Exception e) {
            log.error("Error previewing role code for roleName={}: {}", roleName, e.getMessage());
 
            Map<String, Object> err = new HashMap<>();
            err.put("status",      "FAILURE");
            err.put("statusMsg",   "Could not preview code: " + e.getMessage());
            err.put("previewCode", "----");
            err.put("isEstimate",  true);
 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
    
    //  Load privileges for a role ───────────────────────────────────────
 
    /* GET /api/v1/roles/{id}/privileges*/
    
    @GetMapping("/{id}/privileges")
    public ResponseEntity<RestWithStatusList> getPrivileges(@PathVariable Long id) {
        log.info("GET privileges for roleId={}", id);
        return ResponseEntity.ok(roleService.getPrivileges(id));
    }
    
    
    // ── PUT: Save (replace) privileges for a role ─────────────────────────────
    /**
     * PUT /api/v1/roles/{id}/privileges
     * *
     * Full replace: clears existing permissions for this role,
     * then inserts all rows from the request body.
     *
     * Request body (List<RecPermissionRowDTO>):
     * [
     *   { "moduleId": 1, "hasAccess": true,  "canView": true,
     *     "canCreate": true, "canEdit": false, "canApprove": false, "canDownload": false },
     *   { "moduleId": 2, "hasAccess": false, "canView": false,
     *     "canCreate": false, "canEdit": false, "canApprove": false, "canDownload": false },
     *   ...
     * ]
     *
     * Note: Send ALL modules (not just enabled ones).
     * hasAccess=false rows are stored so the modal can restore state on reopen.
     */
    @PutMapping("/{id}/privileges")
    public ResponseEntity<RestWithStatusList> savePrivileges(
            @PathVariable Long id,
            @RequestBody List<RecPermissionRowDTO> dtos) {
 
        log.info("PUT privileges for roleId={}, rows={}", id, dtos.size());
        return ResponseEntity.ok(roleService.updatePermissions(id, dtos));
    }
    
    
 // GET ALL ROLES
    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllRoles() {

        log.info("Fetching all roles");

        return ResponseEntity.ok(roleService.getAllRoles());
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
    
 // ── Helpers: parse the comma-separated bankType string ─────────────────────
    private boolean hasIssuerFlag(MainBank bank) {
        return parseBankType(bank.getBankType()).contains("ISSUER");
    }
    private boolean hasAcquirerFlag(MainBank bank) {
        return parseBankType(bank.getBankType()).contains("ACQUIRER");
    }
    private List<String> parseBankType(String bankType) {
        if (bankType == null || bankType.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(bankType.split(","))
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toList());
    }
    
 // ── Global handler: IllegalArgumentException → 400 with clear message ──────
    // Catches validation failures from RoleCompatibilityValidator and parseRoleType etc.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestWithStatusList> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg(ex.getMessage())
                        .data(Collections.emptyList())
                        .build());
    }
 
    // ── Global handler: RuntimeException → 500 ────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestWithStatusList> handleServerError(RuntimeException ex) {
        log.error("Server error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestWithStatusList.builder()
                        .status("FAILURE")
                        .statusMsg("Unexpected error: " + ex.getMessage())
                        .data(Collections.emptyList())
                        .build());
    }
}