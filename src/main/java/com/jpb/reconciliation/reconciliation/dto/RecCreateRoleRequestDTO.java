package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecCreateRoleRequestDTO {

    /**
     * One or more role category names from the StandardRole enum.
     * e.g. ["MAKER"], ["MAKER", "CHECKER"], ["OTHER"]
     * For a custom role the name can be anything; it will map to OTHER.
     */
    @NotEmpty(message = "At least one role name is required")
    private List<
        @NotBlank(message = "Role name must not be blank")
        @Size(max = 25, message = "Role name must be ≤ 25 characters")
        String> roleNames;
//    private String    status;         // DRAFT / PENDING
    private String    roleType;        // RECON_USER / BANK_USER / BRANCH_USER
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
    private String    createdBy;      // set from JWT context in backend
    private String    bankCode;       // set from JWT context in backend
    private String    branchCode;     // set from JWT context in backend (null for bank admin)
    private boolean forceCreate; // default false; frontend sets true after user confirms "create anyway"
    private String reservedRoleCode; // pre-generated via GET /preview-code; createRole()
    // reuses this instead of calling generateNextCode() again

    private List<RecPermissionRowDTO> permissions; // the checkbox matrix
}
