package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddUserResponse {

    private Long   id;
    private String fullName;
    private String username;
    private String email;
    private String department;
    private String designation;
    private String mobileNumber;
    private String userType;
    private String role;
//    private String roleType;
    private String status;

    private String externalDepartmentName;
    private String externalSupervisorName;
    private String externalSupervisorEmail;
    private String externalSupervisorPhone;

    private String bankCode;
    private String branchCode;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime inactivateScheduledAt;
    private LocalDateTime blockScheduledAt;
    private LocalDateTime reactivateScheduledAt;

    private String replacementStatus;    // ACTIVE / PERMANENT / null — from ADMIN_REPLACEMENT table
    private String replacedByUsername;   // replacement user's username
    private boolean replacementAdminRow; // true = this row IS the replacement user (not the original)

    // Delegation fields
    private String delegationStatus;   // "DELEGATING" | "DELEGATEE" | null
    private String delegateeUsername;  // set when DELEGATING — who took over
    private String delegatorUsername;  // set when DELEGATEE — who delegated to this user

    // Hierarchy fields
    private Long parentId;
    private List<AddUserResponse> children; // populated in hierarchy fetch, null in flat list responses
}
