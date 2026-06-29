//package com.jpb.reconciliation.reconciliation.service.v2;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import java.util.stream.Collectors;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.Session;
//import com.jpb.reconciliation.reconciliation.constants.v2.SftpConstants;
//import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
//import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;
//import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;
//import com.jpb.reconciliation.reconciliation.repository.v2.ReconSftpServerMastRepository;
//import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class ReconSftpServerServiceImpl implements ReconSftpServerService {
//
//    private static final Logger logger = LoggerFactory.getLogger(ReconSftpServerServiceImpl.class);
//
//    private final ReconSftpServerMastRepository sftpServerRepo;
//
//    // ─── CREATE ───────────────────────────────────────────────────────────────
//
//    @Override
//    @Transactional
//    public ResponseEntity<RestWithMapStatusList> createServer(SftpServerRequestDTO request, String createdBy) {
//
//        if (sftpServerRepo.existsByServerName(request.getServerName())) {
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure("SFTP Server name already exists: " + request.getServerName()),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        // ── Auth type validation ──────────────────────────────────────────────
//        String authType = null != request.getAuthType()
//                ? request.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
//        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
//            if (null == request.getPrivateKeyPath() || request.getPrivateKeyPath().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("privateKeyPath is required when authType is SSH_KEY. Do not send password for SSH_KEY auth."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            // PASSWORD (default)
//            if (null == request.getPassword() || request.getPassword().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("password is required when authType is PASSWORD. Do not send privateKeyPath for PASSWORD auth."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        }
//
//        ReconSftpServerMast saved;
//        try {
//            ReconSftpServerMast entity = ReconSftpServerMast.builder()
//                    .serverName(request.getServerName())
//                    .host(request.getHost())
//                    .port(null != request.getPort() ? request.getPort() : 22)
//                    .defaultUsername(request.getDefaultUsername())
//                    .password(request.getPassword())
//                    .privateKeyPath(request.getPrivateKeyPath())
//                    .passphrase(request.getPassphrase())
//                    .protocol(request.getProtocol())
//                    .authType(authType)
//                    .remotePath(request.getRemotePath())
//                    .filePattern(request.getFilePattern())
//                    .archivePath(request.getArchivePath())
//                    .isActive(SftpConstants.ACTIVE)
//                    .createdBy(null != createdBy ? createdBy : SftpConstants.DEFAULT_ACTOR)
//                    .updatedBy(null != createdBy ? createdBy : SftpConstants.DEFAULT_ACTOR)
//                    .build();
//
//            saved = sftpServerRepo.save(entity);
//            logger.info("SFTP Server created. ID: {}, Name: {}",
//                    saved.getServerId(), saved.getServerName());
//
//        } catch (IllegalArgumentException e) {
//            logger.error("Validation error while creating SFTP server: {}", e.getMessage(), e);
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure(e.getMessage()),
//                    HttpStatus.BAD_REQUEST);
//        } catch (Exception e) {
//            logger.error("Error creating SFTP server: {}", e.getMessage(), e);
//            return new ResponseEntity<>(
//                    ResponseBuilder.error("Failed to create SFTP server: " + e.getMessage()),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        Map<String, Object> row = buildServerRow(saved);
//        return ResponseEntity.ok(
//                ResponseBuilder.ok("SFTP Server created successfully.", "sftpServer", Collections.singletonList(row)));
//    }
//
//    // ─── UPDATE ───────────────────────────────────────────────────────────────
//
//    @Override
//    @Transactional
//    public ResponseEntity<RestWithMapStatusList> updateServer(Long serverId, SftpServerRequestDTO request, String updatedBy) {
//
//        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
//                .orElse(null);
//
//        if (null == entity) {
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        if (sftpServerRepo.existsByServerNameAndServerIdNot(request.getServerName(), serverId)) {
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure("Server name already in use: " + request.getServerName()),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        // ── Auth type switch validation ───────────────────────────────────────
//        String authType = null != request.getAuthType()
//                ? request.getAuthType().toUpperCase() : entity.getAuthType();
//        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
//            // privateKeyPath must exist (new in request OR already in DB)
//            boolean hasKeyPath = (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty())
//                    || (null != entity.getPrivateKeyPath() && !entity.getPrivateKeyPath().trim().isEmpty());
//            if (!hasKeyPath) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("privateKeyPath is required when authType is SSH_KEY."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            // PASSWORD — must have password (new in request OR already in DB)
//            boolean hasPassword = (null != request.getPassword() && !request.getPassword().trim().isEmpty())
//                    || (null != entity.getPassword() && !entity.getPassword().trim().isEmpty());
//            if (!hasPassword) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("password is required when authType is PASSWORD."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.failure("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        }
//
//        ReconSftpServerMast saved;
//        try {
//            entity.setServerName(request.getServerName());
//            entity.setHost(request.getHost());
//            entity.setPort(null != request.getPort() ? request.getPort() : 22);
//            entity.setDefaultUsername(request.getDefaultUsername());
//            entity.setAuthType(authType);
//            entity.setProtocol(request.getProtocol());
//            // Preserve existing credential if new one not sent
//            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
//                entity.setPassword(request.getPassword());
//            }
//            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
//                entity.setPrivateKeyPath(request.getPrivateKeyPath());
//            }
//            if (null != request.getPassphrase() && !request.getPassphrase().trim().isEmpty()) {
//                entity.setPassphrase(request.getPassphrase());
//            }
//            entity.setUpdatedBy(updatedBy);
//
//            saved = sftpServerRepo.save(entity);
//            logger.info("SFTP Server updated. ID: {}, Name: {}", saved.getServerId(), saved.getServerName());
//
//        } catch (Exception e) {
//            logger.error("Error updating SFTP server ID {}: {}", serverId, e.getMessage(), e);
//            return new ResponseEntity<>(
//                    ResponseBuilder.error("Failed to update SFTP server: " + e.getMessage()),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        Map<String, Object> row = buildServerRow(saved);
//        return ResponseEntity.ok(
//                ResponseBuilder.ok("SFTP Server updated successfully.", "sftpServer", Collections.singletonList(row)));
//    }
//
//    // ─── GET BY ID ────────────────────────────────────────────────────────────
//
//    @Override
//    public ResponseEntity<RestWithMapStatusList> getServerById(Long serverId) {
//
//        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
//                .orElse(null);
//
//        if (null == entity) {
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        Map<String, Object> row = buildServerRow(entity);
//        return ResponseEntity.ok(
//                ResponseBuilder.ok("SFTP Server fetched successfully.", "sftpServer", Collections.singletonList(row)));
//    }
//
//    // ─── GET ALL ──────────────────────────────────────────────────────────────
//
//    @Override
//    public ResponseEntity<RestWithStatusList> getAllServers(boolean activeOnly) {
//
//        List<ReconSftpServerMast> list = activeOnly
//                ? sftpServerRepo.findByIsActive(SftpConstants.ACTIVE)
//                : sftpServerRepo.findAll();
//
//        logger.info("Fetched {} SFTP server(s). activeOnly={}", list.size(), activeOnly);
//
//        // Map to safe rows — password intentionally excluded (same as buildServerRow)
//        List<Map<String, Object>> rows = new ArrayList<>();
//        for (ReconSftpServerMast s : list) {
//            rows.add(buildServerRow(s));
//        }
//
//        return ResponseEntity.ok(
//                ResponseBuilder.okList("SFTP Servers fetched successfully.", rows));
//    }
//
//    // ─── SOFT DELETE ──────────────────────────────────────────────────────────
//
//    @Override
//    @Transactional
//    public ResponseEntity<RestWithMapStatusList> deleteServer(Long serverId, String updatedBy) {
//
//        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
//                .orElse(null);
//
//        if (null == entity) {
//            return new ResponseEntity<>(
//                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
//                    HttpStatus.BAD_REQUEST);
//        }
//
//        try {
//            entity.setIsActive(SftpConstants.INACTIVE);
//            entity.setUpdatedBy(updatedBy);
//            sftpServerRepo.save(entity);
//            logger.info("SFTP Server soft-deleted. ID: {}", serverId);
//
//        } catch (Exception e) {
//            logger.error("Error deleting SFTP server ID {}: {}", serverId, e.getMessage(), e);
//            return new ResponseEntity<>(
//                    ResponseBuilder.error("Failed to delete SFTP server: " + e.getMessage()),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        Map<String, Object> row = new LinkedHashMap<>();
//        row.put("serverId",   entity.getServerId());
//        row.put("serverName", entity.getServerName());
//        row.put("isActive",   entity.getIsActive());
//
//        return ResponseEntity.ok(
//                ResponseBuilder.ok("SFTP Server deleted successfully.", "sftpServer", Collections.singletonList(row)));
//    }
//
//    // ─── TEST CONNECTION ──────────────────────────────────────────────────────
//
//    @Override
//    public ResponseEntity<RestWithStatusList> testConnection(SftpTestConnectionRequestDTO request) {
//
//        Session session = null;
//        ChannelSftp channel = null;
//
//        // ── Validate auth fields before attempting connection ─────────────────
//        String authType = null != request.getAuthType()
//                ? request.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
//        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
//            if (null == request.getPrivateKeyPath() || request.getPrivateKeyPath().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.errorList("privateKeyPath is required when authType is SSH_KEY."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.errorList("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            if (null == request.getPassword() || request.getPassword().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.errorList("password is required when authType is PASSWORD."),
//                        HttpStatus.BAD_REQUEST);
//            }
//            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
//                return new ResponseEntity<>(
//                        ResponseBuilder.errorList("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
//                        HttpStatus.BAD_REQUEST);
//            }
//        }
//
//        try {
//            JSch jsch = new JSch();
//
//            if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
//                String passphrase = request.getPassphrase();
//                if (null != passphrase && !passphrase.trim().isEmpty()) {
//                    jsch.addIdentity(request.getPrivateKeyPath(), passphrase);
//                } else {
//                    jsch.addIdentity(request.getPrivateKeyPath());
//                }
//            }
//
//            session = jsch.getSession(request.getUsername(), request.getHost(), request.getPort());
//
//            if (!SftpConstants.AUTH_SSH_KEY.equals(authType)) {
//                session.setPassword(request.getPassword());
//            }
//
//            Properties config = new Properties();
//            config.put("StrictHostKeyChecking", "no");
//            session.setConfig(config);
//            session.setTimeout(10000);
//            session.connect();
//
//            channel = (ChannelSftp) session.openChannel("sftp");
//            channel.connect();
//
//            if (null != request.getRemotePath() && !request.getRemotePath().trim().isEmpty()) {
//                channel.ls(request.getRemotePath());
//            }
//
//            logger.info("SFTP Test Connection SUCCESS -> {}:{}", request.getHost(), request.getPort());
//
//            return ResponseEntity.ok(
//                    ResponseBuilder.okEmpty("Connection successful! Remote path is accessible."));
//
//        } catch (Exception e) {
//            logger.warn("SFTP Test Connection FAILED -> {}:{} | {}", request.getHost(), request.getPort(), e.getMessage());
//
//            Map<String, Object> row = new LinkedHashMap<>();
//            row.put("host",       request.getHost());
//            row.put("port",       request.getPort());
//            row.put("remotePath", request.getRemotePath());
//            row.put("status",     "FAILED");
//            row.put("reason",     e.getMessage());
//
//            return new ResponseEntity<>(
//                    ResponseBuilder.errorList("Connection failed: " + e.getMessage()),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//
//        } finally {
//            if (null != channel && channel.isConnected()) channel.disconnect();
//            if (null != session && session.isConnected()) session.disconnect();
//        }
//    }
//
//    // ─── PRIVATE MAPPER ───────────────────────────────────────────────────────
//
//    private Map<String, Object> buildServerRow(ReconSftpServerMast e) {
//        Map<String, Object> row = new LinkedHashMap<>();
//        row.put("serverId",        e.getServerId());
//        row.put("serverName",      e.getServerName());
//        row.put("host",            e.getHost());
//        row.put("port",            e.getPort());
//        row.put("defaultUsername", e.getDefaultUsername());
//        row.put("authType",        e.getAuthType());
//        row.put("privateKeyPath",  e.getPrivateKeyPath());  // shown only when SSH_KEY
//        row.put("protocol",        e.getProtocol());
//        row.put("remotePath",      e.getRemotePath());
//        row.put("filePattern",     e.getFilePattern());
//        row.put("archivePath",     e.getArchivePath());
//        row.put("isActive",        e.getIsActive());
//        row.put("createdBy",       e.getCreatedBy());
//        row.put("createdAt",       e.getCreatedAt());
//        row.put("updatedBy",       e.getUpdatedBy());
//        row.put("updatedAt",       e.getUpdatedAt());
//        // password + passphrase intentionally excluded from response
//        return row;
//    }
//}