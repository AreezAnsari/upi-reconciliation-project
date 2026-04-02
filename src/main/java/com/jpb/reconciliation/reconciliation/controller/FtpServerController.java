package com.jpb.reconciliation.reconciliation.controller;


import java.util.List;

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
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.FtpServerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ftp-servers")
@RequiredArgsConstructor
public class FtpServerController {

    // ── Injecting interface only — implementation is FtpServerServiceImpl ──
    private final FtpServerService ftpServerService;

    @PostMapping
    public ResponseEntity<FtpServerDTO> create(@RequestBody FtpServerDTO dto) {
        return new ResponseEntity<>(ftpServerService.createFtpServer(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FtpServerDTO>> getAll() {
        return ResponseEntity.ok(ftpServerService.getAllFtpServers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FtpServerDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ftpServerService.getFtpServerById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<RestWithStatusList> searchByIp(@RequestParam String serverIp) {
        List<FtpServerDTO> list = ftpServerService.getFtpServersByIp(serverIp);
        if (list.isEmpty()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "No SFTP server found for IP: " + serverIp, null),
                    HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(
                new RestWithStatusList("SUCCESS", "SFTP servers found", List.of(list)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FtpServerDTO> update(@PathVariable Long id, @RequestBody FtpServerDTO dto) {
        return ResponseEntity.ok(ftpServerService.updateFtpServer(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList> delete(@PathVariable Long id) {
        ftpServerService.deleteFtpServer(id);
        return ResponseEntity.ok(
                new RestWithStatusList("SUCCESS", "SFTP server deleted successfully", null));
    }
}
