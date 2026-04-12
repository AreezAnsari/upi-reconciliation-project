package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SftpServerResponseDTO {
    private Long serverId;
    private String serverName;
    private String host;
    private Integer port;
    private String defaultUsername;
    private String protocol;
    private String authType;
    private String isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Note: password intentionally excluded from response
}