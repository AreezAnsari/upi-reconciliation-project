package com.jpb.reconciliation.reconciliation.dto;


import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpbRoleResponse {
    private Long id;
    private String roleName;
    private String roleType;
    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private List<JpbPermissionDTO> permissions;
}