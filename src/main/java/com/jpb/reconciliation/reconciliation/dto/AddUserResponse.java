package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;

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
    private String roleType;
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
}
