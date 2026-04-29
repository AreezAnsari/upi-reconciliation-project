package com.jpb.reconciliation.reconciliation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.entity.*;
import com.jpb.reconciliation.reconciliation.mapper.RecRoleMapper;
import com.jpb.reconciliation.reconciliation.repository.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecRoleServiceImpl implements RecRoleService {

    private final RecRoleRepository roleRepo;
    private final RecModuleRepository moduleRepo;
    private final RecRoleModulePermissionRepository permRepo;

    // ── Get all modules ─────────────────────────────────────────
    @Override
    public RestWithStatusList getAllModules() {

        List<Object> modules = moduleRepo.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(m -> RecPermissionRowDTO.builder()
                        .moduleId(m.getId())
                        .moduleName(m.getName())
                        .build())
                .collect(Collectors.toList());

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Modules fetched successfully")
                .data(modules)
                .build();
    }

    // ── Create Role ─────────────────────────────────────────────
    @Override
    @Transactional
    public RestWithStatusList createRole(RecCreateRoleRequestDTO req) {

        // Validate
        boolean anyAccess = req.getPermissions() != null &&
                req.getPermissions().stream().anyMatch(RecPermissionRowDTO::isHasAccess);

        if (!anyAccess) {
            throw new RuntimeException("Please enable access to at least one module.");
        }

        // Save role
        RecRole role = RecRole.builder()
                .roleName(req.getRoleName())
                .roleType(req.getRoleType())
                .status(req.getStatus() != null ? req.getStatus() : "DRAFT")
                .description(req.getDescription())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .createdBy(req.getCreatedBy())
                .build();

        RecRole saved = roleRepo.save(role);
        roleRepo.flush();

        saved = roleRepo.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Role not found after creation"));

        log.info("Created role: {} with code: {}", saved.getRoleName(), saved.getRoleCode());

        // Save permissions
        savePermissions(saved, req.getPermissions());

        RecRoleResponseDTO responseDTO = buildResponse(saved);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Role created successfully")
                .data(Collections.singletonList(responseDTO))
                .build();
    }

    // ── Update Permissions ──────────────────────────────────────
    @Override
    @Transactional
    public RestWithStatusList updatePermissions(Long roleId, List<RecPermissionRowDTO> dtos) {

        RecRole role = roleRepo.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        permRepo.deleteByRoleId(roleId);
        savePermissions(role, dtos);

        RecRoleResponseDTO responseDTO = buildResponse(role);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Permissions updated successfully")
                .data(Collections.singletonList(responseDTO))
                .build();
    }

    // ── Get Role ────────────────────────────────────────────────
    @Override
    public RestWithStatusList getRole(Long roleId) {

        RecRole role = roleRepo.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        RecRoleResponseDTO responseDTO = buildResponse(role);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Role fetched successfully")
                .data(Collections.singletonList(responseDTO))
                .build();
    }

    // ── Private Helpers ─────────────────────────────────────────

    private void savePermissions(RecRole role, List<RecPermissionRowDTO> dtos) {

        if (dtos == null) return;

        for (RecPermissionRowDTO dto : dtos) {

            RecModule module = moduleRepo.findById(dto.getModuleId())
                    .orElseThrow(() ->
                            new RuntimeException("Invalid Module ID: " + dto.getModuleId()));

            boolean access = dto.isHasAccess();

            RecRoleModulePermission perm = RecRoleModulePermission.builder()
                    .role(role)
                    .module(module)
                    .hasAccess(access)
                    .canView(access && dto.isCanView())
                    .canCreate(access && dto.isCanCreate())
                    .canEdit(access && dto.isCanEdit())
                    .canApprove(access && dto.isCanApprove())
                    .canDownload(access && dto.isCanDownload())
                    .build();

            permRepo.save(perm);
        }
    }

    private RecRoleResponseDTO buildResponse(RecRole role) {

        List<RecRoleModulePermission> perms = permRepo.findByRoleId(role.getId());

        return RecRoleMapper.toResponse(role, perms);
    }
}