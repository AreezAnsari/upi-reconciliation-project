package com.jpb.reconciliation.reconciliation.controller;


import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;
import com.jpb.reconciliation.reconciliation.dto.InstitutionDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.InstitutionService;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for FSS Payment Gateway – Institution Management
 *
 * Base URL: /api/v1/institutions
 *
 * All responses use RestWithStatusList wrapper:
 * {
 *   "status"    : "SUCCESS" | "FAILURE",
 *   "statusMsg" : "...",
 *   "data"      : [ ... ]
 * }
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  Method  │  Endpoint                              │  Description     │
 * ├──────────┼────────────────────────────────────────┼──────────────────┤
 * │  POST    │  /api/v1/institutions                  │  Create          │
 * │  GET     │  /api/v1/institutions                  │  Get All         │
 * │  GET     │  /api/v1/institutions/paged            │  Get Paged       │
 * │  GET     │  /api/v1/institutions/{id}             │  Get By ID       │
 * │  GET     │  /api/v1/institutions/status/{status}  │  Get By Status   │
 * │  PUT     │  /api/v1/institutions/{id}             │  Full Update     │
 * │  PATCH   │  /api/v1/institutions/{id}/status      │  Status Update   │
 * │  DELETE  │  /api/v1/institutions/{id}             │  Soft Delete     │
 * │  DELETE  │  /api/v1/institutions/{id}/hard        │  Hard Delete     │
 * └──────────┴────────────────────────────────────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/institutions
     * Create a new institution record.
     *
     * Response 201:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution created successfully",
     *   "data"      : [ { ...institutionDTO... } ]
     * }
     */
    @PostMapping
    public ResponseEntity<RestWithStatusList> createInstitution(
            @Valid @RequestBody InstitutionDTO institutionDTO) {

        InstitutionDTO created = institutionService.createInstitution(institutionDTO);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution created successfully")
                .data(Collections.singletonList(created))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/institutions
     * Retrieve all institutions.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institutions fetched successfully",
     *   "data"      : [ { ...institution1... }, { ...institution2... } ]
     * }
     */
    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {

        List<InstitutionDTO> list = institutionService.getAllInstitutions();

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institutions fetched successfully")
                .data(Collections.unmodifiableList(list))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/institutions/paged?page=0&size=10&sort=institutionName,asc
     * Retrieve paginated list of institutions.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institutions fetched successfully",
     *   "data"      : [ { ...Page<InstitutionDTO>... } ]
     * }
     */
    @GetMapping("/paged")
    public ResponseEntity<RestWithStatusList> getAllInstitutionsPaged(
            @PageableDefault(size = 10, sort = "institutionName") Pageable pageable) {

        Page<InstitutionDTO> page = institutionService.getAllInstitutionsPaged(pageable);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institutions fetched successfully")
                .data(Collections.singletonList(page))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/institutions/{id}
     * Retrieve a single institution by its ID.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution fetched successfully",
     *   "data"      : [ { ...institutionDTO... } ]
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getInstitutionById(@PathVariable Long id) {

        InstitutionDTO dto = institutionService.getInstitutionById(id);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution fetched successfully")
                .data(Collections.singletonList(dto))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/institutions/status/{status}
     * Retrieve institutions filtered by status (ACTIVE / INACTIVE).
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institutions fetched by status: ACTIVE",
     *   "data"      : [ { ...institution1... }, { ...institution2... } ]
     * }
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<RestWithStatusList> getByStatus(@PathVariable EnableStatus status) {

        List<InstitutionDTO> list = institutionService.getInstitutionsByStatus(status);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institutions fetched by status: " + status)
                .data(Collections.unmodifiableList(list))
                .build();

        return ResponseEntity.ok(response);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * PUT /api/v1/institutions/{id}
     * Full update of an institution record.
     * Note: institutionId, institutionUserId, userRole, dataEncryptionKey
     *       are read-only per FSS manual — ignored during update.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution updated successfully",
     *   "data"      : [ { ...updatedInstitutionDTO... } ]
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestWithStatusList> updateInstitution(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionDTO institutionDTO) {

        InstitutionDTO updated = institutionService.updateInstitution(id, institutionDTO);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution updated successfully")
                .data(Collections.singletonList(updated))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/v1/institutions/{id}/status?status=ACTIVE
     * Change only the Enable Status (ACTIVE / INACTIVE).
     * Per the FSS manual: institution records cannot be deleted — use status change instead.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution status updated to ACTIVE",
     *   "data"      : [ { ...updatedInstitutionDTO... } ]
     * }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long id,
            @RequestParam EnableStatus status) {

        InstitutionDTO updated = institutionService.updateStatus(id, status);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution status updated to " + status)
                .data(Collections.singletonList(updated))
                .build();

        return ResponseEntity.ok(response);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/institutions/{id}
     * Soft delete: sets institution status to INACTIVE.
     * Per FSS manual: you cannot delete an institution record.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution deactivated successfully",
     *   "data"      : []
     * }
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList> deleteInstitution(@PathVariable Long id) {

        institutionService.deleteInstitution(id);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution deactivated successfully")
                .data(Collections.emptyList())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/institutions/{id}/hard
     * Hard delete: permanently removes the institution record.
     * Use only when absolutely necessary.
     *
     * Response 200:
     * {
     *   "status"    : "SUCCESS",
     *   "statusMsg" : "Institution permanently deleted",
     *   "data"      : []
     * }
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<RestWithStatusList> hardDeleteInstitution(@PathVariable Long id) {

        institutionService.hardDeleteInstitution(id);

        RestWithStatusList response = RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Institution permanently deleted")
                .data(Collections.emptyList())
                .build();

        return ResponseEntity.ok(response);
    }
}