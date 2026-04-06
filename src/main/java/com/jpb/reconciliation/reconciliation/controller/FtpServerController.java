package com.jpb.reconciliation.reconciliation.controller;


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

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.service.FtpServerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ftp-servers")
@RequiredArgsConstructor
@Tag(name = "SFTP Server Master", description = "APIs for managing SFTP server registry")
public class FtpServerController {

    private final FtpServerService ftpServerService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST  /api/ftp-servers
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Register a new SFTP server")
    public ResponseEntity<RestWithMapStatusList> createFtpServer(
            @RequestBody FtpServerDTO dto) {
        return ftpServerService.createFtpServer(dto);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/ftp-servers
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "Get all registered SFTP servers")
    public ResponseEntity<RestWithMapStatusList> getAllFtpServers() {
        return ftpServerService.getAllFtpServers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/ftp-servers/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Get SFTP server by ID")
    public ResponseEntity<RestWithMapStatusList> getFtpServerById(
            @PathVariable Long id) {
        return ftpServerService.getFtpServerById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET   /api/ftp-servers/search?serverIp=
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/search")
    @Operation(summary = "Search SFTP servers by IP / host")
    public ResponseEntity<RestWithMapStatusList> getFtpServersByIp(
            @RequestParam String serverIp) {
        return ftpServerService.getFtpServersByIp(serverIp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT   /api/ftp-servers/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing SFTP server")
    public ResponseEntity<RestWithMapStatusList> updateFtpServer(
            @PathVariable Long id,
            @RequestBody FtpServerDTO dto) {
        return ftpServerService.updateFtpServer(id, dto);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/ftp-servers/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (deactivate) an SFTP server")
    public ResponseEntity<RestWithMapStatusList> deleteFtpServer(
            @PathVariable Long id) {
        return ftpServerService.deleteFtpServer(id);
    }
}
