package com.jpb.reconciliation.reconciliation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.CustomRoleRequestDto;
import com.jpb.reconciliation.reconciliation.dto.CustomRoleResponseDto;
import com.jpb.reconciliation.reconciliation.entity.TestRole;
import com.jpb.reconciliation.reconciliation.repository.TestRoleRepo;

@Service
public class CustomRoleService {

    private final TestRoleRepo repo;

    public CustomRoleService(TestRoleRepo repo) {
        this.repo = repo;
    }

    @Transactional
    public CustomRoleResponseDto createRole(CustomRoleRequestDto dto) {

        validate(dto);

        TestRole role = new TestRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleType(dto.getRoleType());
        role.setRoleStatus(dto.getRoleStatus());
        role.setRoleDesc(dto.getRoleDesc());
        role.setValidFrom(dto.getValidFrom());
        role.setValidTo(dto.getValidTo());

        TestRole saved = repo.save(role);

        CustomRoleResponseDto res = new CustomRoleResponseDto();
        res.setId(saved.getId());
        res.setRoleName(saved.getRoleName());
        res.setRoleCode(saved.getRoleCode());
        res.setCreatedAt(saved.getCreatedAt());

        return res;
    }

    private void validate(CustomRoleRequestDto dto) {

        if (dto.getRoleName() == null || dto.getRoleName().isEmpty())
            throw new RuntimeException("Role Name required");

        if (!dto.getRoleType().equalsIgnoreCase("Internal") &&
            !dto.getRoleType().equalsIgnoreCase("External"))
            throw new RuntimeException("Invalid Role Type");

        if (!dto.getRoleStatus().equalsIgnoreCase("Active") &&
            !dto.getRoleStatus().equalsIgnoreCase("Inactive") &&
            !dto.getRoleStatus().equalsIgnoreCase("Draft"))
            throw new RuntimeException("Invalid Role Status");
    }
}