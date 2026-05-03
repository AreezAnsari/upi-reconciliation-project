package com.jpb.reconciliation.reconciliation.controller.v2;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.service.ReconFileTemplateConfigServiceImpl;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v2/template/")
@RequiredArgsConstructor
@Tag(name = "Reconciliation Template", description = "APIs for managing reconciliation templates")
public class ReconFileTemplateConfigController {

    private final ReconFileTemplateConfigServiceImpl reconFileTemplateConfigServiceImpl;

    // ─────────────────────────────────────────────────────────────────────────
    // POST  /api/v1/template/add-template
    // ─────────────────────────────────────────────────────────────────────────
//    @PostMapping(value = "add-template", produces = CommonConstants.APPLICATION_JSON)
//    @Operation(summary = "Add a new template header record")
//    public ResponseEntity<RestWithMapStatusList> addTemplate(
//            @RequestBody ReconTemplateDetailsDto dto) {
//        return reconFileTemplateConfigServiceImpl.addTemplate(dto);
//    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST  /api/v1/template/template-configure
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(value = "template-configure", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Configure a new template with its field definitions")
    public ResponseEntity<RestWithMapStatusList> configureTemplateWithField(
            @RequestBody ReconTemplateConfigRequest request,@AuthenticationPrincipal UserDetails userDetails) {

        if (request.getFieldDetails() == null || request.getFieldDetails().isEmpty()) {
        	return new ResponseEntity<>(
                    ResponseBuilder.failure("fieldDetails must not be null or empty"),
                    HttpStatus.BAD_REQUEST);
        }
        request.setCreatedBy(userDetails.getUsername());
        
        return reconFileTemplateConfigServiceImpl.configureTemplateAndFieldData(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/template/view-template?page=0&size=10
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/view-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "View all templates with pagination")
    public ResponseEntity<RestWithMapStatusList> viewTemplate(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Records per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return reconFileTemplateConfigServiceImpl.viewTemplate(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT   /api/v1/template/update-template/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping(value = "/update-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Update template fields and format")
    public ResponseEntity<RestWithMapStatusList> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody ReconTemplateConfigRequest request) {

        if (request.getFieldDetails() == null || request.getFieldDetails().isEmpty()) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("fieldDetails must not be null or empty"),
                    HttpStatus.BAD_REQUEST);
        }
        return reconFileTemplateConfigServiceImpl.updateTemplate(templateId, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/template/delete-template/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping(value = "/delete-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Delete a template by ID")
    public ResponseEntity<RestWithMapStatusList> deleteTemplate(
            @PathVariable Long templateId) {
        return reconFileTemplateConfigServiceImpl.deleteTemplate(templateId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/template/search-template?templateName=&templateType=
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/search-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Search templates by name or type")
    public ResponseEntity<RestWithMapStatusList> searchTemplate(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reconFileTemplateConfigServiceImpl.searchTemplate(templateName, templateType, page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/template/get-template/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/get-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Get template by ID")
    public ResponseEntity<RestWithMapStatusList> getTemplateById(
            @PathVariable Long templateId) {
        return reconFileTemplateConfigServiceImpl.getTemplateById(templateId);
    }
}
