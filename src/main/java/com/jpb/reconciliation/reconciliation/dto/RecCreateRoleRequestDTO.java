package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecCreateRoleRequestDTO {

    private List<String>   roleNames;       // mandatory
    private String    roleType;       // INTERNAL / EXTERNAL (mandatory)
    private String    status;         // DRAFT / PENDING
    private String    externalDepartmentName;
    private String    externalSupervisorName;
    private String    externalSupervisorEmail;
    private String    externalSupervisorPhone;
    private Long   assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private String    description;    // optional
    private LocalDate validFrom;      // optional
    private LocalDate validTo;        // optional
    private String    createdBy;      // set from session in real app

    private List<RecPermissionRowDTO> permissions; // the checkbox matrix
}
