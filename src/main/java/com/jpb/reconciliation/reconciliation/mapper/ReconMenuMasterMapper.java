package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

public class ReconMenuMasterMapper {

    public static ReconMenuMasterDto mapToMenuDto(ReconMenuMaster menu, ReconMenuMasterDto menuDto) {
        menuDto.setMenuId(menu.getMenuId());
        menuDto.setMenuType(menu.getMenuType());
        menuDto.setMenuName(menu.getMenuName());
        menuDto.setMenuDescription(menu.getMenuDescription());
        menuDto.setParentMenuCode(menu.getParentMenuCode());
        menuDto.setSubMenuReq(menu.getSubMenu());
        menuDto.setMenuUrl(menu.getMenuUrl());
        menuDto.setMenuProcessId(menu.getMenuProcessId());
        menuDto.setMasterMenuParent(menu.getMasterMenuParent());
        menuDto.setProductId(menu.getProductId());
        // A custom Main/Submenu never sets parentMenuCode/masterMenuParent (only this numeric
        // pointer) — frontend tree-building (Sidebar/Navbar/PrivilegesAssign) needs it to nest
        // custom rows at all, so every raw-entity response must carry it through.
        menuDto.setParentMenuId(menu.getParentMenuId());
        return menuDto;
    }

    public static ReconMenuMaster mapToMenu(ReconMenuMasterDto menuDto, ReconMenuMaster menu) {
        menu.setMenuId(menuDto.getMenuId());
        menu.setMenuType(menuDto.getMenuType());
        menu.setMenuDescription(menuDto.getMenuDescription());
        menu.setParentMenuCode(menuDto.getParentMenuCode());
        menu.setMenuUrl(menuDto.getMenuUrl());
        menu.setMenuName(menuDto.getMenuName());
        menu.setSubMenu(menuDto.getSubMenuReq());
        menu.setMenuProcessId(menuDto.getMenuProcessId());
        menu.setProductId(menuDto.getProductId());
        return menu;
    }

    public static ReconMenuMasterDto mapToMenuDtoWithFilePath(ReconMenuMasterDto reconMenuMasterDto,
            ReconMenuMaster menu, ReconFileDetailsMaster fileData) {
        reconMenuMasterDto.setMenuId(menu.getMenuId());
        reconMenuMasterDto.setMenuType(menu.getMenuType());
        reconMenuMasterDto.setMenuName(menu.getMenuName());
        reconMenuMasterDto.setMenuDescription(menu.getMenuDescription());
        reconMenuMasterDto.setParentMenuCode(menu.getParentMenuCode());
        reconMenuMasterDto.setSubMenuReq(menu.getSubMenu());
        reconMenuMasterDto.setMenuUrl(menu.getMenuUrl());
        reconMenuMasterDto.setMenuProcessId(menu.getMenuProcessId());
        reconMenuMasterDto.setMasterMenuParent(menu.getMasterMenuParent());
        reconMenuMasterDto.setProductId(menu.getProductId());
        reconMenuMasterDto.setParentMenuId(menu.getParentMenuId());
        if (fileData == null) {
            reconMenuMasterDto.setReconFilePath(null);
        } else {
            reconMenuMasterDto.setReconFilePath(fileData.getReconFileDestinationPath());
        }
        return reconMenuMasterDto;
    }
}
