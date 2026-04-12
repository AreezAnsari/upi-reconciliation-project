package com.jpb.reconciliation.reconciliation.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.jpb.reconciliation.reconciliation.dto.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
import com.jpb.reconciliation.reconciliation.service.ReconSftpServerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/recon/sftp-servers")
@RequiredArgsConstructor
public class ReconSftpServerController {

    private final ReconSftpServerService sftpServerService;

    @PostMapping
    public ResponseEntity<RestWithMapStatusList> create(
            @RequestBody SftpServerRequestDTO request,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId,@AuthenticationPrincipal UserDetails userDetails) {
        return sftpServerService.createServer(request, userDetails.getUsername());
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<RestWithMapStatusList> update(
            @PathVariable Long serverId,
            @RequestBody SftpServerRequestDTO request,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        return sftpServerService.updateServer(serverId, request, userId);
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<RestWithMapStatusList> getById(@PathVariable Long serverId) {
        return sftpServerService.getServerById(serverId);
    }

    @GetMapping
    public ResponseEntity<RestWithMapStatusList> getAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return sftpServerService.getAllServers(activeOnly);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<RestWithMapStatusList> delete(
            @PathVariable Long serverId,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        return sftpServerService.deleteServer(serverId, userId);
    }

    @PostMapping("/test-connection")
    public ResponseEntity<RestWithMapStatusList> testConnection(
            @RequestBody SftpTestConnectionRequestDTO request) {
        return sftpServerService.testConnection(request);
    }
}