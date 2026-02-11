package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface MenuMasterService {

	boolean updateMenu(ReconMenuMasterDto menuDto);

	ResponseEntity<RestWithStatusList> getMenus(Long menuId);

	ResponseEntity<RestWithStatusList> getAllMenus();

	ResponseEntity<RestWithStatusList> getMenuByUserId(Long userId);

	ResponseEntity<RestWithStatusList> addMenu(ReconMenuMasterDto menuRequest, UserDetails userDetails);

	ResponseEntity<ResponseDto> removeMenu(Long menuId);

	ResponseEntity<RestWithStatusList> getMenuByRole(Long roleId);

	Long getVerifiedRoleId(String username);

}
