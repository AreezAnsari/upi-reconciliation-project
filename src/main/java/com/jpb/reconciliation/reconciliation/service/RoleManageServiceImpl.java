package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RoleMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.mapper.RoleMenuMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;

@Service
public class RoleManageServiceImpl implements RoleManageService {

	@Autowired
	RoleManageRepository roleManageRepository;

	@Autowired
	MenuMasterRepository menuMasterRepository;

	@Override
	public ResponseEntity<RestWithStatusList> getAllRoleDetails() {
		RestWithStatusList restWithStatusList;

		List<Object> roleWithMenuList = new ArrayList<>();
		List<Role> getAllRole = roleManageRepository.findAll();

		List<ReconMenuMaster> getAllMenu = menuMasterRepository.findAll();
		if (!getAllRole.isEmpty()) {
//			getAllRole.stream().map(role -> (Object) role).forEach(roleList::add);
			for (Role role : getAllRole) {
				List<ReconMenuMaster> menuWithRoleList = new ArrayList<>();
				for (ReconMenuMaster menu : getAllMenu) {
					if (menu.getRoleId().equals(role.getRoleId())) {
						menuWithRoleList.add(menu);
					}
				}
				RoleMasterDto roleMenu = RoleMenuMapper.mapRoleWithMenu(role, menuWithRoleList, new RoleMasterDto());
				roleWithMenuList.add(roleMenu);
			}

			restWithStatusList = new RestWithStatusList("SUCCESS", "Role Found Successfully", roleWithMenuList);

		} else {
			restWithStatusList = new RestWithStatusList("FAILURE", "Role Not Found ", roleWithMenuList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getRoleByUserLogin(Long verifiedRoleId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> roleData = new ArrayList<>();
		Role getLoginRole = roleManageRepository.findByRoleId(verifiedRoleId);
		roleData.add(getLoginRole);
		
		restWithStatusList = new RestWithStatusList("SUCCESS", "Login role user found.", roleData);
		return new ResponseEntity<RestWithStatusList>(restWithStatusList,HttpStatus.OK);
	}

}
