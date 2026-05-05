package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.ReconMenuMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;

@Service
public class MenuMasterServiceImpl implements MenuMasterService {
	@Autowired
	MenuMasterRepository menuMasterRepository;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	ReconUserRepository reconUserRepository;

	Logger logger = LoggerFactory.getLogger(MenuMasterServiceImpl.class);

	@Autowired
	ReconFileDetailsMasterRepository fileDetailsMasterRepository;

	@Override
	public ResponseEntity<RestWithStatusList> getMenus(Long menuId) {
		RestWithStatusList restWithStatusList;

		List<Object> menuData = new ArrayList<>();
		ReconMenuMaster menu = menuMasterRepository.findByMenuId(menuId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu not found :" + menuId));
		ReconMenuMasterDto menuDto = ReconMenuMasterMapper.mapToMenuDto(menu, new ReconMenuMasterDto());
		menuData.add(menuDto);

		restWithStatusList = new RestWithStatusList("SUCCESS", "Menu found successfully", menuData);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);

	}

	@Override
	public ResponseEntity<ResponseDto> removeMenu(Long menuId) {
		ReconMenuMaster menu = menuMasterRepository.findByMenuId(menuId)
				.orElseThrow(() -> new ResourceNotFoundException("MENU NOT FOUND :" + menuId));
		logger.info("MENU BY MENU ID :::::::::::::::::::::" + menu);
		if (menu != null) {

			List<ReconMenuMaster> existsParentMenuList = menuMasterRepository.findByParentMenuCode(menu.getMenuType());
			logger.info("EXISTS MENU BY PARENT CODE ::::::::::::::::::::::" + existsParentMenuList);

			if (existsParentMenuList.isEmpty()) {
				menuMasterRepository.deleteById(menu.getMenuId());
			} else {
				return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
						.body(new ResponseDto(MenuConstants.STATUS_417, "Please delete the submenu !!!"));
			}
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(new ResponseDto(MenuConstants.STATUS_200, "Menu Removed Successfully"));
	}

	@Override
	public boolean updateMenu(ReconMenuMasterDto menuDto) {
		boolean isUpdated = false;

		Long menuId = menuDto.getMenuId();
		ReconMenuMaster menu = menuMasterRepository.findById(menuId).get();
		if (menu != null) {
			ReconMenuMasterMapper.mapToMenu(menuDto, menu);
			menuMasterRepository.save(menu);
			isUpdated = true;
		}
		return isUpdated;

	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllMenus() {

		RestWithStatusList restWithStatusList;

		List<ReconMenuMaster> fetchMenuData = menuMasterRepository.findAll();
		List<Object> menuList = new ArrayList<>();
		for (ReconMenuMaster menu : fetchMenuData) {
			menuList.add(menu);
		}

		if (fetchMenuData != null) {
			restWithStatusList = new RestWithStatusList("SUCCESS", "Menu details found succcessfully", menuList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
		}
		restWithStatusList = new RestWithStatusList("FAILURE", "Menu details not found", null);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getMenuByUserId(Long userId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> menuList = new ArrayList<>();
		List<ReconMenuMaster> menuByUserId = menuMasterRepository.getByInsertUserId(userId);
		if (menuByUserId.isEmpty()) {
			restWithStatusList = new RestWithStatusList("FAILURE", "Menu details not found successfully", menuList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		} else {
			for (ReconMenuMaster menu : menuByUserId) {
				ReconFileDetailsMaster fileData = fileDetailsMasterRepository
						.findByReconFileId(menu.getMenuProcessId());
				ReconMenuMasterDto menuMaster = ReconMenuMasterMapper.mapToMenuDtoWithFilePath(new ReconMenuMasterDto(),
						menu, fileData);
				menuList.add(menuMaster);
			}
			restWithStatusList = new RestWithStatusList("SUCCESS", "Menu details found successfully", menuList);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> addMenu(ReconMenuMasterDto menuRequest, UserDetails userDetails) {
		RestWithStatusList restWithStatusList = null;
		try {
		ReconMenuMaster menuWithName = menuMasterRepository.findByMenuNameAndRoleIdAndParentMenuCode(
				menuRequest.getMenuName(), menuRequest.getRoleId(), menuRequest.getParentMenuCode());
		ReconUser userData = reconUserRepository.findByUserName(userDetails.getUsername()).get();
		ReconMenuMaster createdUser = null;
		if (menuWithName == null) {
			createdUser = createMenu(menuRequest);
		} else if (menuWithName.getParentMenuCode().equalsIgnoreCase(menuRequest.getParentMenuCode())
				|| menuWithName.getMenuName().equalsIgnoreCase(menuRequest.getMenuName())) {
			restWithStatusList = new RestWithStatusList("FAILURE", "Menu already exists for this role", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		} else {
			createdUser = createMenu(menuRequest);
		}

		ReconFileDetailsMaster getFileData = fileDetailsMasterRepository
				.findByReconFileId(menuRequest.getMenuProcessId());
		if (getFileData != null) {
			getFileData.setReconExitMenuFlag("Y");
			fileDetailsMasterRepository.save(getFileData);
		}
		auditLogManagerService.commonAudit(userData, "Add MENU", createdUser);
		restWithStatusList = new RestWithStatusList("SUCCESS", "Menu Created Successfully", null);
		
		}catch (Exception e) {
			// TODO: handle exception
			restWithStatusList = new RestWithStatusList("FAILURE", "Exception occured while creating menu", null);
			logger.error("Exception occured while creating menu "+e.getMessage(),e);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.CREATED);
	}

	private ReconMenuMaster createMenu(ReconMenuMasterDto menuRequest) {
		ReconMenuMaster addNewMenu = new ReconMenuMaster();
		if (menuRequest.getMenuType().equalsIgnoreCase("MASTER")) {
			addNewMenu.setMenuType(menuRequest.getMenuType());
			addNewMenu.setMenuName(menuRequest.getMenuName());
			addNewMenu.setMenuDescription(menuRequest.getMenuDescription());
			addNewMenu.setInsertUserId(menuRequest.getUserId());
			addNewMenu.setMenuProcessId(menuRequest.getMenuProcessId());
			addNewMenu.setMenuUrl(menuRequest.getMenuUrl());
			addNewMenu.setCreatedBy(null);
			addNewMenu.setCreatedDate(new Date());
			addNewMenu.setInsertDate(new Date());
			addNewMenu.setProcessType(menuRequest.getProcessType());
			addNewMenu.setStatus("Y");
			addNewMenu.setRoleId(menuRequest.getRoleId());
		} else if (menuRequest.getMenuType().equalsIgnoreCase("MAIN")) {
			addNewMenu.setMenuType(menuRequest.getMenuType());
			addNewMenu.setMenuName(menuRequest.getMenuName());
			addNewMenu.setMenuDescription(menuRequest.getMenuDescription());
			addNewMenu.setSubMenu(menuRequest.getSubMenuReq());
			addNewMenu.setParentMenuCode(menuRequest.getParentMenuCode());
			addNewMenu.setInsertUserId(menuRequest.getUserId());
			addNewMenu.setMenuProcessId(menuRequest.getMenuProcessId());
			addNewMenu.setMenuUrl(menuRequest.getMenuUrl());
			addNewMenu.setCreatedBy(null);
			addNewMenu.setCreatedDate(new Date());
			addNewMenu.setInsertDate(new Date());
			addNewMenu.setProcessType(menuRequest.getProcessType());
			addNewMenu.setStatus("Y");
			addNewMenu.setRoleId(menuRequest.getRoleId());
		} else {
			addNewMenu.setMenuType(menuRequest.getMenuType());
			addNewMenu.setMenuName(menuRequest.getMenuName());
			addNewMenu.setMenuDescription(menuRequest.getMenuDescription());
			addNewMenu.setParentMenuCode(menuRequest.getParentMenuCode());
			addNewMenu.setInsertUserId(menuRequest.getUserId());
			addNewMenu.setMenuProcessId(menuRequest.getMenuProcessId());
			addNewMenu.setMenuUrl(menuRequest.getMenuUrl());
			addNewMenu.setMasterMenuParent(menuRequest.getMasterMenuParent());
			addNewMenu.setCreatedBy(null);
			addNewMenu.setCreatedDate(new Date());
			addNewMenu.setInsertDate(new Date());
			addNewMenu.setProcessType(menuRequest.getProcessType());
			addNewMenu.setStatus("Y");
			addNewMenu.setRoleId(menuRequest.getRoleId());
		}
		menuMasterRepository.save(addNewMenu);
		return addNewMenu;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getMenuByRole(Long roleId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> menuList = new ArrayList<>();
		List<ReconMenuMaster> menuByRole = menuMasterRepository.getByRoleId(roleId);
		logger.info("Menu data by role :::::::::::::" + menuByRole);

		if (menuByRole.isEmpty()) {
			restWithStatusList = new RestWithStatusList("FAILURE", "Menu not found for this role", menuList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		} else {
			for (ReconMenuMaster menu : menuByRole) {
				ReconFileDetailsMaster fileData = fileDetailsMasterRepository
						.findByReconFileId(menu.getMenuProcessId());
				ReconMenuMasterDto menuMaster = ReconMenuMasterMapper.mapToMenuDtoWithFilePath(new ReconMenuMasterDto(),
						menu, fileData);
				menuList.add(menuMaster);
			}
			restWithStatusList = new RestWithStatusList("SUCCESS", "Menu found successfully", menuList);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public Long getVerifiedRoleId(String username) {
		Optional<ReconUser> user = reconUserRepository.findByUserName(username);
		ReconUser reconUserData = user.get();
		if (reconUserData == null || reconUserData.getRole() == null) {
			throw new UsernameNotFoundException("User or role not found for: " + username);
		}

		return reconUserData.getRole().getRoleId();
	}

}
