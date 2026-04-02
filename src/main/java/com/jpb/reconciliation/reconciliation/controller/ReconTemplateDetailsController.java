package com.jpb.reconciliation.reconciliation.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusListPagination;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.service.ReconTemplateDetailsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/template/")
@RequiredArgsConstructor
@Tag(name = "Reconciliation Template", description = "APIs for managing reconciliation templates")
public class ReconTemplateDetailsController {

    // ── Injecting interface only — implementation is ReconTemplateDetailsServiceImpl ──
    private final ReconTemplateDetailsService reconTemplateDetailsService;

    @PostMapping(value = "add-template", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> addTemplate(@RequestBody ReconTemplateDetailsDto dto) {
        return reconTemplateDetailsService.addTemplate(dto);
    }

    @PostMapping(value = "template-configure", produces = CommonConstants.APPLICATION_JSON)
    ResponseEntity<RestWithStatusList> configureTemplateWithField(@RequestBody TemplateFieldDto request) {
        if (request.getFieldDetails() == null || request.getFieldDetails().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "fieldDetails must not be null or empty", null),
                    HttpStatus.BAD_REQUEST);
        }
        return reconTemplateDetailsService.configureTemplateAndFieldData(request);
    }

    @GetMapping(value = "/view-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "View all templates with pagination")
    public ResponseEntity<RestWithStatusListPagination> viewTemplate(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reconTemplateDetailsService.viewTemplate(page, size);
    }

    @PutMapping(value = "/update-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Update template fields and format only")
    public ResponseEntity<RestWithStatusList> updateTemplate(
            @PathVariable Long templateId, @RequestBody TemplateFieldDto request) {
        if (request.getFieldDetails() == null || request.getFieldDetails().isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "fieldDetails must not be null or empty", null),
                    HttpStatus.BAD_REQUEST);
        }
        return reconTemplateDetailsService.updateTemplate(templateId, request);
    }

    @DeleteMapping(value = "/delete-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Delete a template by ID")
    public ResponseEntity<RestWithStatusList> deleteTemplate(@PathVariable Long templateId) {
        return reconTemplateDetailsService.deleteTemplate(templateId);
    }

    @GetMapping(value = "/search-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Search templates by name or type")
    public ResponseEntity<RestWithStatusListPagination> searchTemplate(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reconTemplateDetailsService.searchTemplate(templateName, templateType, page, size);
    }

    @GetMapping(value = "/get-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Get template by ID")
    public ResponseEntity<?> getTemplateById(@PathVariable Long templateId) {
        return reconTemplateDetailsService.getTemplateById(templateId);
    }
}
