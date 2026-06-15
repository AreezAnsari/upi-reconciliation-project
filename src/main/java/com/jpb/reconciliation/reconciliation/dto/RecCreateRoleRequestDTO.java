package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecCreateRoleRequestDTO {

    private List<String> roleNames;
    private String    roleType;
    private String    status;
    private String    externalDepartmentName;
    private String    externalSupervisorName;
    private String    externalSupervisorEmail;
    private String    externalSupervisorPhone;
    private Long      assignedUserId;
    private String    assignedUserName;
    private String    assignedUserEmail;
    private String    description;
    private String    department;
    private Integer   sessionTimeout;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String    createdBy;
    private String    bankCode;
    private String    branchCode;

    private List<RecPermissionRowDTO> permissions;
}
