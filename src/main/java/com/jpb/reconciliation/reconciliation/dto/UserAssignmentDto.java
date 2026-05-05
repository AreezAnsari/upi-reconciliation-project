package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class UserAssignmentDto {

    private Long id;
    private Long userId;
    private Long moduleId;
    private String role;
}