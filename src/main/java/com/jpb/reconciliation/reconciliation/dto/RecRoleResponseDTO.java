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
    private String    roleType;
    private String    status;
    private String    description;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String    createdBy;
    private LocalDateTime createdAt;
    private List<RecPermissionRowDTO> permissions;
}

