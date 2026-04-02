package com.jpb.reconciliation.reconciliation.controller;


import java.util.List;

import org.springframework.data.domain.Page;
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

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigDTO;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.TemplateDTO;
import com.jpb.reconciliation.reconciliation.service.FileConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/file/")
@RequiredArgsConstructor
@Slf4j
public class FileConfigController {

    // ── Injecting interface only — implementation is FileConfigServiceImpl ──
    private final FileConfigService fileConfigService;

    @GetMapping("/templates")
    public ResponseEntity<List<TemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(fileConfigService.getAllTemplates());
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<TemplateDTO> getTemplateById(@PathVariable Long templateId) {
        return ResponseEntity.ok(fileConfigService.getTemplateById(templateId));
    }

    @GetMapping("/file-configurations")
    public ResponseEntity<Page<FileConfigDTO>> getAllFileConfigs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String fileName) {
        return ResponseEntity.ok(fileConfigService.getAllFileConfigs(page, size, templateId, fileName));
    }

    @GetMapping("/file-configurations/{fileId}")
    public ResponseEntity<FileConfigDTO> getFileConfigById(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileConfigService.getFileConfigById(fileId));
    }

    @PostMapping("/file-configurations")
    public ResponseEntity<RestWithStatusList> createFileConfig(
            @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return fileConfigService.createFileConfig(request, userId);
    }

    @PutMapping("/file-configurations/{fileId}")
    public ResponseEntity<FileConfigDTO> updateFileConfig(
            @PathVariable Long fileId,
            @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(fileConfigService.updateFileConfig(fileId, request, userId));
    }

    @DeleteMapping("/file-configurations/{fileId}")
    public ResponseEntity<RestWithStatusList> deleteFileConfig(@PathVariable Long fileId) {
        return fileConfigService.deleteFileConfig(fileId);
    }
}
