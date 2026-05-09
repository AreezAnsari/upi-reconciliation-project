package com.jpb.reconciliation.reconciliation.controller;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;
import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.entity.Institution;
import com.jpb.reconciliation.reconciliation.service.InstitutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class InstitutionController {

    private final InstitutionService institutionService;

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<RestWithStatusList>
    createInstitution(
            @Valid @RequestBody InstitutionDTO dto) {

        Institution institution =
                institutionService.createInstitution(dto);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institution Created Successfully")
                        .data(
                                Collections.singletonList(
                                        institution))
                        .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<RestWithStatusList>
    getAllInstitutions() {

        List<Institution> institutions =
                institutionService.getAllInstitutions();

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institutions fetched successfully")
                        .data(institutions)
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList>
    getInstitutionById(
            @PathVariable Long id) {

        Institution institution =
                institutionService.getInstitutionById(id);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institution fetched successfully")
                        .data(
                                Collections.singletonList(
                                        institution))
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<RestWithStatusList>
    updateInstitution(
            @PathVariable Long id,
            @RequestBody InstitutionDTO dto) {

        Institution institution =
                institutionService.updateInstitution(
                        id,
                        dto);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institution updated successfully")
                        .data(
                                Collections.singletonList(
                                        institution))
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @PatchMapping("/{id}/status")
    public ResponseEntity<RestWithStatusList>
    updateStatus(
            @PathVariable Long id,
            @RequestParam EnableStatus status) {

        Institution institution =
                institutionService.updateStatus(
                        id,
                        status);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Status updated successfully")
                        .data(
                                Collections.singletonList(
                                        institution))
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList>
    deleteInstitution(
            @PathVariable Long id) {

        institutionService.deleteInstitution(id);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institution deactivated successfully")
                        .data(Collections.emptyList())
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // HARD DELETE
    // =========================================================

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<RestWithStatusList>
    hardDeleteInstitution(
            @PathVariable Long id) {

        institutionService.hardDeleteInstitution(id);

        RestWithStatusList response =
                RestWithStatusList.builder()
                        .status("SUCCESS")
                        .statusMsg(
                                "Institution deleted permanently")
                        .data(Collections.emptyList())
                        .build();

        return ResponseEntity.ok(response);
    }
}