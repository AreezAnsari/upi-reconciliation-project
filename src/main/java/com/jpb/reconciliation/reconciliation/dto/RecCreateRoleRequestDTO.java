package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecCreateRoleRequestDTO {

    private List<String>   roleNames;       // mandatory
//    private String    status;         // DRAFT / PENDING
    private String    roleType;       // INTERNAL / EXTERNAL (mandatory)
//    private String    externalDepartmentName;
//    private String    externalSupervisorName;
//    private String    externalSupervisorEmail;
//    private String    externalSupervisorPhone;
    @NotNull(message = "Session timeout is required")
    @Min(value = 1, message = "Session timeout must be at least 1 minute")
    @Max(value = 120, message = "Session timeout cannot exceed 120 minutes (2 hours)")
    private Integer sessionTimeout;
    private Long   assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private String department;
    private String    description;    // optional
    private LocalDate validFrom;      // optional
    private LocalDate validTo;        // optional
    private String    createdBy;      // set from session in real app

    private List<RecPermissionRowDTO> permissions; // the checkbox matrix
}
