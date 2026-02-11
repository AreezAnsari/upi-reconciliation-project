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
		menuDto.setOperations(menu.getOperations());
		menuDto.setUserId(menu.getInsertUserId());
		menuDto.setMenuProcessId(menu.getMenuProcessId());
		menuDto.setMasterMenuParent(menu.getMasterMenuParent());
		return menuDto;
	}

	public static ReconMenuMaster mapToMenu(ReconMenuMasterDto menuDto, ReconMenuMaster menu) {
		menu.setMenuId(menuDto.getMenuId());
		menu.setMenuType(menuDto.getMenuType());
		menuDto.setMenuDescription(menu.getMenuDescription());
		menu.setParentMenuCode(menuDto.getParentMenuCode());
		menu.setMenuUrl(menuDto.getMenuUrl());
		menuDto.setMenuName(menu.getMenuName());
		menu.setOperations(menuDto.getOperations());
		menu.setSubMenu(menuDto.getSubMenuReq());
		menu.setInsertUserId(menuDto.getUserId());
		menu.setMenuProcessId(menuDto.getMenuProcessId());
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
		reconMenuMasterDto.setOperations(menu.getOperations());
		reconMenuMasterDto.setUserId(menu.getInsertUserId());
		reconMenuMasterDto.setMenuProcessId(menu.getMenuProcessId());
		reconMenuMasterDto.setMasterMenuParent(menu.getMasterMenuParent());
		reconMenuMasterDto.setRoleId(menu.getRoleId());

		if (fileData == null) {
			reconMenuMasterDto.setReconFilePath(null);
		}else {
			reconMenuMasterDto.setReconFilePath(fileData.getReconFileDestinationPath());
		}
		return reconMenuMasterDto;
	}

}
