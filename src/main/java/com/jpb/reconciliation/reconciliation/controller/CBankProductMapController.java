package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.repository.CBankProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.service.CBankProductMapService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/bank-product-map")
@CrossOrigin(origins = "*")
public class CBankProductMapController {

    private static final Logger logger = LoggerFactory.getLogger(CBankProductMapController.class);

    @Autowired
    private CBankProductMapService cBankProductMapService;

    @Autowired
    private CBankProductMapRepository cBankProductMapRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    @Operation(summary = "Map a product to a bank")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> mapBankProduct(@RequestBody CBankProductMap mapping) {
        logger.info("Map bank-product request: bankId={}, productId={}", mapping.getBankId(), mapping.getProductId());
        return cBankProductMapService.mapBankProduct(mapping);
    }

    @Operation(summary = "Get all product mappings for a bank")
    @GetMapping(value = "/get-by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMappingsByBankId(@PathVariable Long bankId) {
        return cBankProductMapService.getMappingsByBankId(bankId);
    }

    @Operation(summary = "Get all bank mappings for a product")
    @GetMapping(value = "/get-by-product/{productId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMappingsByProductId(@PathVariable Long productId) {
        return cBankProductMapService.getMappingsByProductId(productId);
    }

    @Operation(summary = "Remove all product mappings for a bank")
    @DeleteMapping(value = "/remove-by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> removeMappingsByBankId(@PathVariable Long bankId) {
        logger.info("Remove bank-product mappings for bankId={}", bankId);
        return cBankProductMapService.removeMappingsByBankId(bankId);
    }

    @Operation(summary = "Update mapping status")
    @PatchMapping(value = "/update-status/{id}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateMappingStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return cBankProductMapService.updateMappingStatus(id, status);
    }

    @Operation(summary = "Get purchased products for a branch or bank by code")
    @GetMapping("/branch-products")
    public ResponseEntity<Map<String, Object>> getBranchProducts(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String bankCode) {

        logger.info("Fetching products: branchCode={}, bankCode={}", branchCode, bankCode);

        Map<String, Object> response = new HashMap<>();

        try {
            List<String> rawProducts;
            String resolvedBankCode = bankCode;

            if (branchCode != null && !branchCode.trim().isEmpty()) {
                java.util.Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findByBankCode(branchCode.trim());
                if (!branchOpt.isPresent()) {
                    rawProducts = Collections.emptyList();
                } else {
                    rawProducts = cBankProductMapRepository.findActiveProductNamesByBankId(branchOpt.get().getBankId());
                }
                response.put("branchCode", branchCode);
            } else if (bankCode != null && !bankCode.trim().isEmpty()) {
                java.util.Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findByBankCode(bankCode.trim());
                if (!bankOpt.isPresent()) {
                    rawProducts = Collections.emptyList();
                } else {
                    rawProducts = cBankProductMapRepository.findActiveProductNamesByBankId(bankOpt.get().getBankId());
                }
                response.put("bankCode", bankCode);
            } else {
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
                isIssuer   = reconBankMasterRepository.findByBankCode(resolvedBankCode.trim())
                        .map(b -> parseBankCategory(b.getBankCategory()).contains("ISSUER"))
                        .orElse(false);
                isAcquirer = reconBankMasterRepository.findByBankCode(resolvedBankCode.trim())
                        .map(b -> parseBankCategory(b.getBankCategory()).contains("ACQUIRER"))
                        .orElse(false);
            }

            response.put("status",            "SUCCESS");
            response.put("purchasedProducts", purchasedProducts);
            response.put("isIssuer",          isIssuer);
            response.put("isAcquirer",        isAcquirer);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            Map<String, Object> err = new HashMap<>();
            err.put("status",            "FAILURE");
            err.put("statusMsg",         "Could not fetch products: " + e.getMessage());
            err.put("purchasedProducts", Collections.emptyList());
            err.put("isIssuer",          false);
            err.put("isAcquirer",        false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    private List<String> parseBankCategory(String bankCategory) {
        if (bankCategory == null || bankCategory.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(bankCategory.split(","))
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toList());
    }
}
