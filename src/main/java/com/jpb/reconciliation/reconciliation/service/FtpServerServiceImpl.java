package com.jpb.reconciliation.reconciliation.service;


import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconSftpServerMast;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.ReconSftpServerMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FtpServerServiceImpl implements FtpServerService {

    private final ReconSftpServerMastRepository sftpServerRepository;

    // =========================================================================
    // CREATE SFTP SERVER
    // data key: "sftpServer"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> createFtpServer(FtpServerDTO dto) {
        ReconSftpServerMast entity = mapToEntity(dto);
        entity.setCreatedBy("SYSTEM");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIsActive("Y");

        ReconSftpServerMast saved = sftpServerRepository.save(entity);
        log.info("SFTP server created. ID: {}", saved.getSftpServerId());

        return new ResponseEntity<>(
                ResponseBuilder.ok("SFTP server created successfully.",
                        "sftpServer", List.of(toRowMap(saved))),
                HttpStatus.CREATED);
    }

    // =========================================================================
    // GET ALL SFTP SERVERS
    // data key: "sftpServers"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getAllFtpServers() {
        List<Map<String, Object>> rows = sftpServerRepository.findAll()
                .stream().map(this::toRowMap).collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP servers fetched successfully.",
                        "sftpServers", rows));
    }

    // =========================================================================
    // GET SFTP SERVER BY ID
    // data key: "sftpServer"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getFtpServerById(Long id) {
        ReconSftpServerMast entity = sftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SFTP server not found: " + id));

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP server fetched successfully.",
                        "sftpServer", List.of(toRowMap(entity))));
    }

    // =========================================================================
    // SEARCH BY IP / HOST
    // data key: "sftpServers"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getFtpServersByIp(String serverIp) {
        List<ReconSftpServerMast> list = sftpServerRepository.findByServerIp(serverIp);

        if (list.isEmpty()) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("No SFTP server found for IP: " + serverIp),
                    HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> rows = list.stream()
                .map(this::toRowMap).collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP servers found.", "sftpServers", rows));
    }

    // =========================================================================
    // UPDATE SFTP SERVER
    // data key: "sftpServer"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> updateFtpServer(Long id, FtpServerDTO dto) {
        ReconSftpServerMast existing = sftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SFTP server not found: " + id));

        existing.setServerName(dto.getFtpServerName());
        existing.setHost(dto.getServerIp());
        existing.setPort(dto.getPort());
        existing.setDefaultUsername(dto.getUserName());
        existing.setUpdatedAt(LocalDateTime.now());

        ReconSftpServerMast saved = sftpServerRepository.save(existing);
        log.info("SFTP server updated. ID: {}", saved.getSftpServerId());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP server updated successfully.",
                        "sftpServer", List.of(toRowMap(saved))));
    }

    // =========================================================================
    // DELETE SFTP SERVER (soft delete — marks inactive)
    // data key: "deleted"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> deleteFtpServer(Long id) {
        ReconSftpServerMast existing = sftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SFTP server not found: " + id));

        existing.setIsActive("N");
        existing.setUpdatedAt(LocalDateTime.now());
        sftpServerRepository.save(existing);
        log.info("SFTP server deactivated. ID: {}", id);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deletedSftpServerId", id);
        row.put("serverName",          existing.getServerName());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP server deleted successfully.", "deleted", List.of(row)));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private ReconSftpServerMast mapToEntity(FtpServerDTO dto) {
        ReconSftpServerMast e = new ReconSftpServerMast();
        e.setServerName(dto.getFtpServerName());
        e.setHost(dto.getServerIp());
        e.setPort(dto.getPort());
        e.setDefaultUsername(dto.getUserName());
        e.setAuthType("PASSWORD");
        return e;
    }

    /** Flat row map — password intentionally excluded (stored in vault) */
    private Map<String, Object> toRowMap(ReconSftpServerMast e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sftpServerId",     e.getSftpServerId());
        m.put("serverName",       e.getServerName());
        m.put("host",             e.getHost());
        m.put("port",             e.getPort());
        m.put("defaultUsername",  e.getDefaultUsername());
        m.put("authType",         e.getAuthType());
        m.put("isActive",         e.getIsActive());
        m.put("createdAt",        e.getCreatedAt());
        m.put("updatedAt",        e.getUpdatedAt());
        return m;
    }
}
