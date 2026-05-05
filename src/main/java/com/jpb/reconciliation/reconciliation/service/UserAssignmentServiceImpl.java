package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.UserAssignmentDto;
import com.jpb.reconciliation.reconciliation.entity.UserAssignment;
import com.jpb.reconciliation.reconciliation.mapper.UserAssignmentMapper;
import com.jpb.reconciliation.reconciliation.repository.UserAssignmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAssignmentServiceImpl implements UserAssignmentService {

    @Autowired
    private UserAssignmentRepository repository;

    @Autowired
    private UserAssignmentMapper mapper;

    @Override
    public UserAssignmentDto create(UserAssignmentDto dto) {
        UserAssignment entity = mapper.toEntity(dto);
        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public UserAssignmentDto getById(Long id) {
        UserAssignment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Data not found"));
        return mapper.toDto(entity);
    }

    @Override
    public List<UserAssignmentDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserAssignmentDto update(Long id, UserAssignmentDto dto) {
        UserAssignment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Data not found"));

        entity.setUserId(dto.getUserId());
        entity.setModuleId(dto.getModuleId());
        entity.setRole(dto.getRole());

        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}