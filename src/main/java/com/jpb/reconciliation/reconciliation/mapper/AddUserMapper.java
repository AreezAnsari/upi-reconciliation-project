package com.jpb.reconciliation.reconciliation.mapper;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.entity.AddUser;

public class AddUserMapper {

    /**
     * Convert CreateUserRequest → User Entity
     */
    public static AddUser toEntity(AddUserRequest req,
                                PasswordEncoder passwordEncoder,
                                String createdBy,
                                String institutionCode) {

        return AddUser.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .department(req.getDepartment())
                .designation(req.getDesignation())
                .mobileNumber(req.getMobileNumber())
                .userType(AddUser.UserType.valueOf(req.getUserType().toUpperCase()))
                .role(AddUser.Role.valueOf(req.getRole().toUpperCase()))
                .status(AddUser.UserStatus.ACTIVE) // default
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .createdBy(createdBy)
                .institutionCode(institutionCode)
                .build();
    }

    /**
     * Convert User Entity → UserResponse DTO
     */
    public static AddUserResponse toResponse(AddUser user) {

        return AddUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .department(user.getDepartment())
                .designation(user.getDesignation())
                .mobileNumber(user.getMobileNumber())
                .userType(user.getUserType() != null ? user.getUserType().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .institutionCode(user.getInstitutionCode())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}