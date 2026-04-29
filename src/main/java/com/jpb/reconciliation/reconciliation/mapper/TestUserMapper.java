package com.jpb.reconciliation.reconciliation.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RoleDto;
import com.jpb.reconciliation.reconciliation.entity.TestRole;
import com.jpb.reconciliation.reconciliation.entity.TestUser;

public class TestUserMapper {

    // Map DTO → new TestUser (for create)
    public static TestUser mapToTestUser(ReconUserDto reconUserDto, TestUser testUser) {
        testUser.setUserName(reconUserDto.getUserName());
        testUser.setUserStatus("INACTIVE");
        testUser.setDesignation(reconUserDto.getDesignation());
        testUser.setEmailId(reconUserDto.getEmailId());
        testUser.setInstitution(reconUserDto.getInstitution());
        testUser.setMobileNumber(reconUserDto.getMobileNumber());
        testUser.setType(reconUserDto.getType());
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setCreatedBy(reconUserDto.getCreatedBy());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setUpdatedBy(reconUserDto.getUpdatedBy());
        return testUser;
    }

    // Map single TestUser → ReconUserResponseDto (for get by id / get current user)
    public static ReconUserResponseDto mapToTestUserResponseDto(TestUser testUser,
            ReconUserResponseDto reconUserResponseDto) {
        reconUserResponseDto.setUserId(testUser.getUserId());
        reconUserResponseDto.setUserName(testUser.getUserName());
        reconUserResponseDto.setUserStatus(testUser.getUserStatus());
        reconUserResponseDto.setDesignation(testUser.getDesignation());
        reconUserResponseDto.setEmailId(testUser.getEmailId());
        reconUserResponseDto.setInstitution(testUser.getInstitution());
        reconUserResponseDto.setMobileNumber(testUser.getMobileNumber());
        reconUserResponseDto.setType(testUser.getType());
        reconUserResponseDto.setCreatedAt(testUser.getCreatedAt());
        reconUserResponseDto.setCreatedBy(testUser.getCreatedBy());

        RoleDto roleDto = mapToRoleDto(testUser.getRole());
        reconUserResponseDto.setRole(roleDto);
        return reconUserResponseDto;
    }

    // Map List<TestUser> → List<ReconUserResponseDto> (for get all / get pending)
    public static List<ReconUserResponseDto> mapToTestUsersResponseDto(List<TestUser> userList) {
        List<ReconUserResponseDto> userDataList = new ArrayList<>();
        for (TestUser user : userList) {
            ReconUserResponseDto mapUser = new ReconUserResponseDto();
            mapUser.setUserId(user.getUserId());
            mapUser.setUserName(user.getUserName());
            mapUser.setUserStatus(user.getUserStatus());
            mapUser.setDesignation(user.getDesignation());
            mapUser.setEmailId(user.getEmailId());
            mapUser.setInstitution(user.getInstitution());
            mapUser.setMobileNumber(user.getMobileNumber());
            mapUser.setType(user.getType());
            mapUser.setCreatedAt(user.getCreatedAt());
            mapUser.setCreatedBy(user.getCreatedBy());
            mapUser.setUpdatedAt(user.getUpdatedAt());
            mapUser.setUpdatedBy(user.getUpdatedBy());

            RoleDto roleDto = mapToRoleDto(user.getRole());
            mapUser.setRole(roleDto);
            userDataList.add(mapUser);
        }
        return userDataList;
    }

    // Map DTO → TestUser (for approve / reject)
    public static TestUser mapToApproveRejectTestUser(ReconUserDto reconUserDto, TestUser testUser) {
        testUser.setApprovedYn(reconUserDto.getApprovedYn());
        testUser.setApprovedBy(reconUserDto.getApprovedBy());
        testUser.setUserStatus("ACTIVE");
        return testUser;
    }

    // Map DTO → TestUser (for update)
    public static TestUser mapToTestUserUpdate(ReconUserDto reconUserDto, TestUser testUser) {
        testUser.setUserName(reconUserDto.getUserName());
        testUser.setUserStatus(reconUserDto.getUserStatus());
        testUser.setDesignation(reconUserDto.getDesignation());
        testUser.setEmailId(reconUserDto.getEmailId());
        testUser.setInstitution(reconUserDto.getInstitution());
        testUser.setMobileNumber(reconUserDto.getMobileNumber());
        testUser.setType(reconUserDto.getType());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setUpdatedBy(reconUserDto.getUpdatedBy());
        return testUser;
    }

    // Private helper — TestRole → RoleDto
    private static RoleDto mapToRoleDto(TestRole role) {
        if (role == null) {
            return null;
        }
        RoleDto roleDto = new RoleDto();
        roleDto.setRoleId(role.getRoleId());
        roleDto.setRoleName(role.getRoleName());
        roleDto.setRoleCode(role.getRoleCode());
        roleDto.setCreatedAt(LocalDateTime.now());
        roleDto.setCreatedBy(role.getRoleName());
        return roleDto;
    }
}