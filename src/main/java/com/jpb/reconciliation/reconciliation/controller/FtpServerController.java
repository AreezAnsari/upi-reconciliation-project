package com.jpb.reconciliation.reconciliation.controller;


import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.service.FtpServerService;

@RestController
@RequestMapping("/api/ftp-servers")
public class FtpServerController {

    @Autowired
    private FtpServerService ftpServerService;

    // ─── CREATE ───────────────────────────────────────────────
    // POST /api/ftp-servers
    @PostMapping
    public ResponseEntity<FtpServerDTO> createFtpServer(@Valid @RequestBody FtpServerDTO dto) {
        FtpServerDTO created = ftpServerService.createFtpServer(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ─── READ ALL ─────────────────────────────────────────────
    // GET /api/ftp-servers
    @GetMapping
    public ResponseEntity<List<FtpServerDTO>> getAllFtpServers() {
        List<FtpServerDTO> list = ftpServerService.getAllFtpServers();
        return ResponseEntity.ok(list);
    }

    // ─── READ BY ID ───────────────────────────────────────────
    // GET /api/ftp-servers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<FtpServerDTO> getFtpServerById(@PathVariable Long id) {
        FtpServerDTO dto = ftpServerService.getFtpServerById(id);
        return ResponseEntity.ok(dto);
    }

    // ─── READ BY SERVER IP ────────────────────────────────────
    // GET /api/ftp-servers/search?serverIp=10.44.75.35
    @GetMapping("/search")
    public ResponseEntity<List<FtpServerDTO>> getFtpServersByIp(@RequestParam String serverIp) {
        List<FtpServerDTO> list = ftpServerService.getFtpServersByIp(serverIp);
        return ResponseEntity.ok(list);
    }

    // ─── UPDATE (Full) ────────────────────────────────────────
    // PUT /api/ftp-servers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<FtpServerDTO> updateFtpServer(
            @PathVariable Long id,
            @Valid @RequestBody FtpServerDTO dto) {
        FtpServerDTO updated = ftpServerService.updateFtpServer(id, dto);
        return ResponseEntity.ok(updated);
    }

    // ─── DELETE ───────────────────────────────────────────────
    // DELETE /api/ftp-servers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFtpServer(@PathVariable Long id) {
        ftpServerService.deleteFtpServer(id);
        return ResponseEntity.noContent().build();
    }
}
