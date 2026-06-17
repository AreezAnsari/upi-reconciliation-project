package com.jpb.reconciliation.reconciliation.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RoleDto;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.Role;

public class ReconUserMapper {

	public static KalAdmin mapToReconUser(ReconUserDto reconUserDto, KalAdmin KalAdmin) {
		KalAdmin.setUserName(reconUserDto.getUserName());
		KalAdmin.setUserStatus("INACTIVE");
		KalAdmin.setDesignation(reconUserDto.getDesignation());
		KalAdmin.setEmailId(reconUserDto.getEmailId());
		KalAdmin.setBank(reconUserDto.getBank());
		KalAdmin.setMobileNumber(reconUserDto.getMobileNumber());
		KalAdmin.setType(reconUserDto.getType());
		KalAdmin.setCreatedAt(LocalDateTime.now());
		KalAdmin.setCreatedBy(reconUserDto.getCreatedBy());
		KalAdmin.setUpdatedAt(LocalDateTime.now());
		KalAdmin.setUpdatedBy(reconUserDto.getUpdatedBy());
		return KalAdmin;
	}

	public static ReconUserResponseDto mapToReconUserResponseDto(KalAdmin KalAdmin,
			ReconUserResponseDto reconUserResponseDto) {
		reconUserResponseDto.setUserId(KalAdmin.getUserId());
		reconUserResponseDto.setUserName(KalAdmin.getUserName());
		reconUserResponseDto.setUserStatus(KalAdmin.getUserStatus());
		reconUserResponseDto.setDesignation(KalAdmin.getDesignation());
		reconUserResponseDto.setEmailId(KalAdmin.getEmailId());
		reconUserResponseDto.setBank(KalAdmin.getBank());
		reconUserResponseDto.setMobileNumber(KalAdmin.getMobileNumber());
		reconUserResponseDto.setType(KalAdmin.getType());
		reconUserResponseDto.setCreatedAt(LocalDateTime.now());
		reconUserResponseDto.setCreatedBy(KalAdmin.getUserName());

		RoleDto roleDto = mapToRole(KalAdmin.getRole());
		reconUserResponseDto.setRole(roleDto);
		return reconUserResponseDto;
	}

	private static RoleDto mapToRole(Role role) {
		if (role == null) {
			return null;
		}

		RoleDto roleDto = new RoleDto();
		roleDto.setRoleName(role.getRoleName());
		roleDto.setRoleId(role.getRoleId());
		roleDto.setRoleCode(role.getRoleCode());
		roleDto.setCreatedAt(LocalDateTime.now());
		roleDto.setCreatedBy(role.getCreatedBy());
		return roleDto;
	}

	public static List<ReconUserResponseDto> mapToReconUsersResponseDto(List<KalAdmin> allUsersIsPresent) {
		List<ReconUserResponseDto> userDataList = new ArrayList<>();
		for (KalAdmin user : allUsersIsPresent) {
			ReconUserResponseDto mapUser = new ReconUserResponseDto();

			mapUser.setUserId(user.getUserId());
			mapUser.setUserName(user.getUserName());
			mapUser.setUserStatus(user.getUserStatus());
			mapUser.setDesignation(user.getDesignation());
			mapUser.setEmailId(user.getEmailId());
			mapUser.setBank(user.getBank());
			mapUser.setMobileNumber(user.getMobileNumber());
			mapUser.setType(user.getType());
			mapUser.setCreatedAt(user.getCreatedAt());
			mapUser.setCreatedBy(user.getCreatedBy());
			mapUser.setUpdatedAt(user.getUpdatedAt());
			mapUser.setUpdatedBy(user.getUpdatedBy());

			RoleDto roleDto = mapToRole(user.getRole());
			mapUser.setRole(roleDto);
			userDataList.add(mapUser);
		}
		return userDataList;
	}

	public static KalAdmin mapToApproveRejectReconUser(ReconUserDto reconUserDto, KalAdmin KalAdmin) {
		KalAdmin.setApprovedYn(reconUserDto.getApprovedYn());
		KalAdmin.setApprovedBy(reconUserDto.getApprovedBy());
		KalAdmin.setUserStatus("ACTIVE");
		return KalAdmin;
	}

	public static KalAdmin mapToReconUserUpdate(ReconUserDto reconUserDto, KalAdmin KalAdmin) {
//		KalAdmin.setUserId(reconUserDto.getUserId());
		KalAdmin.setUserName(reconUserDto.getUserName());
		KalAdmin.setUserStatus(reconUserDto.getUserStatus());
		KalAdmin.setDesignation(reconUserDto.getDesignation());
		KalAdmin.setEmailId(reconUserDto.getEmailId());
		KalAdmin.setBank(reconUserDto.getBank());
		KalAdmin.setMobileNumber(reconUserDto.getMobileNumber());
		KalAdmin.setType(reconUserDto.getType());
		KalAdmin.setCreatedAt(LocalDateTime.now());
		KalAdmin.setCreatedBy(reconUserDto.getCreatedBy());
		KalAdmin.setUpdatedAt(LocalDateTime.now());
		KalAdmin.setUpdatedBy(reconUserDto.getUpdatedBy());
		return KalAdmin;
	}

}
