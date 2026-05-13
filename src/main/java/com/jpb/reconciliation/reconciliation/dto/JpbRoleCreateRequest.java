package com.jpb.reconciliation.reconciliation.dto;


import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpbRoleCreateRequest {
    private String roleName;
    private String roleType;
    private String status;
    private String description;
    private String createdBy;
    private List<JpbPermissionDTO> permissions;
}