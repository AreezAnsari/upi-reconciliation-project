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
public class AddUserResponse{
 
    private Long   id;
    private String username;
    private String email;
    private String department;
    private String designation;
    private String mobileNumber;
    private String userType;        // "INTERNAL" | "EXTERNAL"
    private String role;            // "MAKER" | "CHECKER"
    private String status;          // "ACTIVE" | "INACTIVE"
    private String institutionCode;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}