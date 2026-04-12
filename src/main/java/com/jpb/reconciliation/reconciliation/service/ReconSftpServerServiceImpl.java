package com.jpb.reconciliation.reconciliation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
import com.jpb.reconciliation.reconciliation.entity.ReconSftpServerMast;
import com.jpb.reconciliation.reconciliation.repository.ReconSftpServerMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReconSftpServerServiceImpl implements ReconSftpServerService {

    private static final Logger logger = LoggerFactory.getLogger(ReconSftpServerServiceImpl.class);

    private final ReconSftpServerMastRepository sftpServerRepo;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> createServer(SftpServerRequestDTO request, String createdBy) {

        if (sftpServerRepo.existsByServerName(request.getServerName())) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server name already exists: " + request.getServerName()),
                    HttpStatus.BAD_REQUEST);
        }

        ReconSftpServerMast saved;
        try {
            ReconSftpServerMast entity = ReconSftpServerMast.builder()
                    .serverName(request.getServerName())
                    .host(request.getHost())
                    .port(request.getPort() != null ? request.getPort() : 22)
                    .defaultUsername(request.getDefaultUsername())
                    .password(request.getPassword())
                    .protocol(request.getProtocol())
                    .authType(request.getAuthType() != null ? request.getAuthType() : "PASSWORD")
                    .isActive("Y")
                    .createdBy(createdBy != null ? createdBy : "SYSTEM")
                    .build();

            saved = sftpServerRepo.save(entity);
            logger.info("SFTP Server created. ID: {}, Name: {}", saved.getServerId(), saved.getServerName());

        } catch (IllegalArgumentException e) {
            logger.error("Validation error while creating SFTP server: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.failure(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error creating SFTP server: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to create SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = buildServerRow(saved);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server created successfully.", "sftpServer", List.of(row)));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> updateServer(Long serverId, SftpServerRequestDTO request, String updatedBy) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, "Y")
                .orElse(null);

        if (entity == null) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        if (sftpServerRepo.existsByServerNameAndServerIdNot(request.getServerName(), serverId)) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Server name already in use: " + request.getServerName()),
                    HttpStatus.BAD_REQUEST);
        }

        ReconSftpServerMast saved;
        try {
            entity.setServerName(request.getServerName());
            entity.setHost(request.getHost());
            entity.setPort(request.getPort() != null ? request.getPort() : 22);
            entity.setDefaultUsername(request.getDefaultUsername());
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                entity.setPassword(request.getPassword());
            }
            entity.setProtocol(request.getProtocol());
            entity.setAuthType(request.getAuthType());
            entity.setUpdatedBy(updatedBy);

            saved = sftpServerRepo.save(entity);
            logger.info("SFTP Server updated. ID: {}, Name: {}", saved.getServerId(), saved.getServerName());

        } catch (Exception e) {
            logger.error("Error updating SFTP server ID {}: {}", serverId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to update SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = buildServerRow(saved);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server updated successfully.", "sftpServer", List.of(row)));
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithMapStatusList> getServerById(Long serverId) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, "Y")
                .orElse(null);

        if (entity == null) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> row = buildServerRow(entity);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server fetched successfully.", "sftpServer", List.of(row)));
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithMapStatusList> getAllServers(boolean activeOnly) {

        List<ReconSftpServerMast> list = activeOnly
                ? sftpServerRepo.findByIsActive("Y")
                : sftpServerRepo.findAll();

        List<Map<String, Object>> rows = list.stream()
                .map(this::buildServerRow)
                .collect(Collectors.toList());

        logger.info("Fetched {} SFTP server(s). activeOnly={}", rows.size(), activeOnly);

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Servers fetched successfully.", "sftpServers", rows));
    }

    // ─── SOFT DELETE ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> deleteServer(Long serverId, String updatedBy) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, "Y")
                .orElse(null);

        if (entity == null) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            entity.setIsActive("N");
            entity.setUpdatedBy(updatedBy);
            sftpServerRepo.save(entity);
            logger.info("SFTP Server soft-deleted. ID: {}", serverId);

        } catch (Exception e) {
            logger.error("Error deleting SFTP server ID {}: {}", serverId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to delete SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("serverId", entity.getServerId());
        row.put("serverName", entity.getServerName());
        row.put("isActive", entity.getIsActive());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server deleted successfully.", "sftpServer", List.of(row)));
    }

    // ─── TEST CONNECTION ──────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithMapStatusList> testConnection(SftpTestConnectionRequestDTO request) {

        Session session = null;
        ChannelSftp channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(request.getUsername(), request.getHost(), request.getPort());
            session.setPassword(request.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000);
            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            if (request.getRemotePath() != null && !request.getRemotePath().isBlank()) {
                channel.ls(request.getRemotePath());
            }

            logger.info("SFTP Test Connection SUCCESS -> {}:{}", request.getHost(), request.getPort());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("host", request.getHost());
            row.put("port", request.getPort());
            row.put("remotePath", request.getRemotePath());
            row.put("status", "CONNECTED");

            return ResponseEntity.ok(
                    ResponseBuilder.ok("Connection successful! Remote path is accessible.", "connectionResult", List.of(row)));

        } catch (Exception e) {
            logger.warn("SFTP Test Connection FAILED -> {}:{} | {}", request.getHost(), request.getPort(), e.getMessage());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("host", request.getHost());
            row.put("port", request.getPort());
            row.put("remotePath", request.getRemotePath());
            row.put("status", "FAILED");
            row.put("reason", e.getMessage());

            return new ResponseEntity<>(
                    ResponseBuilder.error("Connection failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // ─── PRIVATE MAPPER ───────────────────────────────────────────────────────

    private Map<String, Object> buildServerRow(ReconSftpServerMast e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("serverId",        e.getServerId());
        row.put("serverName",      e.getServerName());
        row.put("host",            e.getHost());
        row.put("port",            e.getPort());
        row.put("defaultUsername", e.getDefaultUsername());
        row.put("protocol",        e.getProtocol());
        row.put("authType",        e.getAuthType());
        row.put("isActive",        e.getIsActive());
        row.put("createdBy",       e.getCreatedBy());
        row.put("createdAt",       e.getCreatedAt());
        row.put("updatedBy",       e.getUpdatedBy());
        row.put("updatedAt",       e.getUpdatedAt());
        // password intentionally excluded
        return row;
    }
}