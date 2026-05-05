package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.UserAssignmentDto;
import com.jpb.reconciliation.reconciliation.entity.UserAssignment;
import org.springframework.stereotype.Component;

// package: mapper
@Component
public class UserAssignmentMapper {

    public UserAssignment toEntity(UserAssignmentDto dto) {
        UserAssignment entity = new UserAssignment();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setModuleId(dto.getModuleId());
        entity.setRole(dto.getRole());
        return entity;
    }

    public UserAssignmentDto toDto(UserAssignment entity) {
        UserAssignmentDto dto = new UserAssignmentDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setModuleId(entity.getModuleId());
        dto.setRole(entity.getRole());
        return dto;
    }
}