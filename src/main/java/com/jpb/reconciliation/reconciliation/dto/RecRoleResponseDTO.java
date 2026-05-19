package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecRoleResponseDTO {
    private Long      id;
    private String    roleName;
    private String    roleCode;
    // All master rows assigned to this role (each has its enum-based int code)
    private List<RecRoleMasterDTO> assignedRoles;
    private String    roleType;
    private String    externalDepartmentName;
    private String    externalSupervisorName;
    private String    externalSupervisorEmail;
    private String    externalSupervisorPhone;
    private String    status;
    private String    description;
    private Long   assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String    createdBy;
    private LocalDateTime createdAt;
    private List<RecPermissionRowDTO> permissions;
}

