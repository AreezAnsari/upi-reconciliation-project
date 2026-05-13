package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.*;
import com.jpb.reconciliation.reconciliation.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class RecRoleMapper {

    public static RecRoleResponseDTO toResponse(
            RecRole role,
            List<RecRoleModulePermission> perms) {

        List<RecPermissionRowDTO> permissionList = perms.stream()
                .map(p -> RecPermissionRowDTO.builder()
                        .moduleId(p.getModule().getId())
                        .moduleName(p.getModule().getName())
                        .hasAccess(p.isHasAccess())
                        .canView(p.isCanView())
                        .canCreate(p.isCanCreate())
                        .canEdit(p.isCanEdit())
                        .canApprove(p.isCanApprove())
                        .canDownload(p.isCanDownload())
                        .build())
                .collect(Collectors.toList());

        return RecRoleResponseDTO.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .roleType(role.getRoleType())
                .status(role.getStatus())
                .description(role.getDescription())
                .validFrom(role.getValidFrom())
                .validTo(role.getValidTo())
                .createdBy(role.getCreatedBy())
                .createdAt(role.getCreatedAt())
                .permissions(permissionList)
                .build();
    }
}