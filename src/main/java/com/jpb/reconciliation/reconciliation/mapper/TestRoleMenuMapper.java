package com.jpb.reconciliation.reconciliation.mapper;

import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.RoleMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.TestRole;

public class TestRoleMenuMapper {

    // Existing method — DO NOT CHANGE
    public static RoleMasterDto mapRoleWithMenu(TestRole role, List<ReconMenuMaster> menuWithRoleList,
            RoleMasterDto roleMasterDto) {
        roleMasterDto.setRoleId(role.getRoleId());
        roleMasterDto.setRoleName(role.getRoleName());
        roleMasterDto.setRoleCode(role.getRoleCode());
        roleMasterDto.setMenu(menuWithRoleList);
        return roleMasterDto;
    }

    // NEW — map single TestRole to RoleMasterDto (no menu needed for bulk response)
    public static RoleMasterDto mapRoleToDto(TestRole role, RoleMasterDto roleMasterDto) {
        roleMasterDto.setRoleId(role.getRoleId());
        roleMasterDto.setRoleName(role.getRoleName());
        roleMasterDto.setRoleCode(role.getRoleCode());
        return roleMasterDto;
    }
}