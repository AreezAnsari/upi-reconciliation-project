package com.jpb.reconciliation.reconciliation.mapper;

import org.springframework.stereotype.Component;
import com.jpb.reconciliation.reconciliation.dto.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.SftpServerResponseDTO;
import com.jpb.reconciliation.reconciliation.entity.ReconSftpServerMast;

@Component
public class ReconSftpServerMapper {

    /**
     * Convert RequestDTO → Entity (for CREATE)
     */
    public ReconSftpServerMast toEntity(SftpServerRequestDTO dto) {
        if (dto == null) return null;
        return ReconSftpServerMast.builder()
                .serverName(dto.getServerName())
                .host(dto.getHost())
                .port(dto.getPort() != null ? dto.getPort() : 22)
                .defaultUsername(dto.getDefaultUsername())
                .password(dto.getPassword())
                .protocol(dto.getProtocol())
                .authType(dto.getAuthType() != null ? dto.getAuthType() : "PASSWORD")
                .isActive("Y")
                // createdBy and createdAt set in service
                .build();
    }

    /**
     * Convert Entity → ResponseDTO
     */
    public SftpServerResponseDTO toResponseDTO(ReconSftpServerMast entity) {
        if (entity == null) return null;
        return SftpServerResponseDTO.builder()
                .serverId(entity.getServerId())
                .serverName(entity.getServerName())
                .host(entity.getHost())
                .port(entity.getPort())
                .defaultUsername(entity.getDefaultUsername())
                .protocol(entity.getProtocol())
                .authType(entity.getAuthType())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // password intentionally excluded from response
                .build();
    }

    /**
     * Update existing Entity from RequestDTO (for UPDATE / PUT)
     * Skips: serverId, isActive, createdBy, createdAt — must not change on update
     * Skips password if blank — preserves existing password
     */
    public void updateEntityFromRequest(SftpServerRequestDTO dto, ReconSftpServerMast entity) {
        if (dto == null || entity == null) return;
        entity.setServerName(dto.getServerName());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort() != null ? dto.getPort() : 22);
        entity.setDefaultUsername(dto.getDefaultUsername());
        entity.setProtocol(dto.getProtocol());
        entity.setAuthType(dto.getAuthType() != null ? dto.getAuthType() : "PASSWORD");
        // FIX: isBlank() → null check + trim().isEmpty() (Java 8 compatible)
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            entity.setPassword(dto.getPassword());
        }
        // updatedBy set in service via request header
        // updatedAt handled by @UpdateTimestamp
    }
}