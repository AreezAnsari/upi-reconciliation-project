package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.UserAssignmentDto;

import java.util.List;

// package: service
public interface UserAssignmentService {

    UserAssignmentDto create(UserAssignmentDto dto);

    UserAssignmentDto getById(Long id);

    List<UserAssignmentDto> getAll();

    UserAssignmentDto update(Long id, UserAssignmentDto dto);

    void delete(Long id);
}
