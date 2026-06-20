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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //  GET Modules
    @GetMapping("/modules")
    public ResponseEntity<RestWithStatusList> getModules() {
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
                return ResponseEntity.ok(response);
            }

            List<String> purchasedProducts = new ArrayList<>();
            for (String p : rawProducts) {
                if (p != null && !p.trim().isEmpty()) {
                    purchasedProducts.add(p.trim().toUpperCase());
                }
            }

            log.info("Products found: {}", purchasedProducts);

            response.put("status",            "SUCCESS");
            response.put("purchasedProducts", purchasedProducts);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching products: {}", e.getMessage(), e);

            Map<String, Object> err = new HashMap<>();
            err.put("status",            "FAILURE");
            err.put("statusMsg",         "Could not fetch products: " + e.getMessage());
            err.put("purchasedProducts", Collections.emptyList());
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