package com.jpb.reconciliation.reconciliation.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.service.FileConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/file/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Configuration", description = "APIs for managing file ingestion configurations")
public class FileConfigController {

    private final FileConfigService fileConfigService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/file/templates
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/templates")
    @Operation(summary = "Get all configured templates for dropdown selection")
    public ResponseEntity<RestWithMapStatusList> getAllTemplates() {
        return fileConfigService.getAllTemplates();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/file/templates/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get template details by ID")
    public ResponseEntity<RestWithMapStatusList> getTemplateById(
            @PathVariable Long templateId) {
        return fileConfigService.getTemplateById(templateId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/file/file-configurations?page=0&size=10&templateId=&fileName=
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/file-configurations")
    @Operation(summary = "Get all file configurations with pagination and filters")
    public ResponseEntity<RestWithMapStatusList> getAllFileConfigs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String fileName) {
        return fileConfigService.getAllFileConfigs(page, size, templateId, fileName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/v1/file/file-configurations/{fileId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/file-configurations/{fileId}")
    @Operation(summary = "Get file configuration by ID")
    public ResponseEntity<RestWithMapStatusList> getFileConfigById(
            @PathVariable Long fileId) {
        return fileConfigService.getFileConfigById(fileId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST  /api/v1/file/file-configurations
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/file-configurations")
    @Operation(summary = "Create a new file ingestion configuration")
    public ResponseEntity<RestWithMapStatusList> createFileConfig(
            @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return fileConfigService.createFileConfig(request, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT   /api/v1/file/file-configurations/{fileId}
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/file-configurations/{fileId}")
    @Operation(summary = "Update an existing file ingestion configuration")
    public ResponseEntity<RestWithMapStatusList> updateFileConfig(
            @PathVariable Long fileId,
            @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return fileConfigService.updateFileConfig(fileId, request, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/file/file-configurations/{fileId}
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/file-configurations/{fileId}")
    @Operation(summary = "Delete a file ingestion configuration")
    public ResponseEntity<RestWithMapStatusList> deleteFileConfig(
            @PathVariable Long fileId) {
        return fileConfigService.deleteFileConfig(fileId);
    }
}
