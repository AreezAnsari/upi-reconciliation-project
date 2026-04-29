package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TestUserBulkApproveRequest;
import com.jpb.reconciliation.reconciliation.entity.TestPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.TestRole;
import com.jpb.reconciliation.reconciliation.entity.TestUser;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.TestUserMapper;
import com.jpb.reconciliation.reconciliation.repository.TestRoleManageRepository;
import com.jpb.reconciliation.reconciliation.repository.TestUserRepository;

@Service
public class TestUserManageServiceImpl implements TestUserManageService {

    private Logger logger = LoggerFactory.getLogger(TestUserManageServiceImpl.class);

    @Autowired
    TestUserRepository testUserRepository;

    @Autowired
    TestRoleManageRepository testRoleManageRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────
    // CREATE USER
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto) {
        RestWithStatusList restWithStatusList;

        Boolean existsUser = testUserRepository.existsByUserNameAndEmailId(
                reconUserDto.getUserName(), reconUserDto.getEmailId());
        logger.info("Check user present into records :::::::::" + existsUser);

        if (existsUser) {
            restWithStatusList = new RestWithStatusList("FAILURE", "User Already Exists", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        // Fetch role from TEST_RCN_ROLE_MASTER
        TestRole userRole = testRoleManageRepository.findByRoleId(reconUserDto.getRoleId());
        if (userRole == null) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Role not found with ID: " + reconUserDto.getRoleId(), null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        TestUser testUser = TestUserMapper.mapToTestUser(reconUserDto, new TestUser());
        testUser.setRole(userRole);

        // Password setup — same as ReconUserServiceImpl
        TestPasswordManager passwordManager = saveUserPasswordData(reconUserDto.getUserPassword());
        passwordManager.setTestUser(testUser);
        testUser.setPasswordManager(passwordManager);

        testUserRepository.save(testUser);
        testUserRepository.flush();
        logger.info("Test user created: {}", testUser.getUserName());

        restWithStatusList = new RestWithStatusList("SUCCESS", "User Created Successfully", null);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // GET USER BY ID
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getUserByUserId(Long userId) {
        RestWithStatusList restWithStatusList;
        List<Object> userList = new ArrayList<>();

        TestUser testUser = testUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER NOT FOUND"));

        ReconUserResponseDto responseDto = TestUserMapper.mapToTestUserResponseDto(testUser,
                new ReconUserResponseDto());
        logger.info("User found for userId :::" + userId);

        userList.add(responseDto);
        restWithStatusList = new RestWithStatusList("SUCCESS", "User found", userList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // GET ALL USERS
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getAllUsers() {
        RestWithStatusList restWithStatusList;
        List<Object> userList = new ArrayList<>();

        List<TestUser> allUsers = testUserRepository.findAll();
        List<ReconUserResponseDto> users = TestUserMapper.mapToTestUsersResponseDto(allUsers);

        if (!allUsers.isEmpty()) {
            userList.addAll(users);
            restWithStatusList = new RestWithStatusList("SUCCESS", "Users found in the system", userList);
        } else {
            restWithStatusList = new RestWithStatusList("FAILURE", "No users found in the system", userList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // GET USERS BY APPROVED_YN (pending / approved)
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> getApprovedUsers(String approvedYn) {
        RestWithStatusList restWithStatusList;
        List<Object> userDetailsList = new ArrayList<>();

        List<TestUser> allUsersPresent = testUserRepository.findByApprovedYn(approvedYn);
        logger.info("User Details ::::::::" + allUsersPresent);

        if (allUsersPresent.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "User Details Is Not Found", userDetailsList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        List<ReconUserResponseDto> userDetailsListData = TestUserMapper.mapToTestUsersResponseDto(allUsersPresent);
        userDetailsList.addAll(userDetailsListData);
        restWithStatusList = new RestWithStatusList("SUCCESS", "Users Fetched Successfully", userDetailsList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // UPDATE USER
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest) {
        RestWithStatusList restWithStatusList;

        Optional<TestUser> userDetails = testUserRepository.findByUserId(userUpdateRequest.getUserId());
        if (!userDetails.isPresent()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please enter valid user details", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        TestUser user = userDetails.get();
        TestUser updatedUser = TestUserMapper.mapToTestUserUpdate(userUpdateRequest, user);

        // Update role if roleId provided
        if (userUpdateRequest.getRoleId() != null) {
            TestRole role = testRoleManageRepository.findByRoleId(userUpdateRequest.getRoleId());
            if (role != null) {
                updatedUser.setRole(role);
            }
        }

        testUserRepository.save(updatedUser);
        restWithStatusList = new RestWithStatusList("SUCCESS", "User Updated Successfully", null);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // DELETE USER
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<ResponseDto> removeUser(Long userId) {
        TestUser testUser = testUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        testUserRepository.deleteById(testUser.getUserId());
        return new ResponseEntity<>(new ResponseDto("200", "User Successfully Deleted"), HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // SINGLE APPROVE / REJECT
    // ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest,
            UserDetails userDetails) {
        RestWithStatusList restWithStatusList;

        if (approveUserRequest == null) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please select user to approve", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        Optional<TestUser> user = testUserRepository.findByUserId(approveUserRequest.getUserId());
        if (!user.isPresent()) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "User not found with ID: " + approveUserRequest.getUserId(), null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        TestUser getUser = user.get();
        approveUserRequest.setApprovedBy(userDetails.getUsername());
        TestUser approvedOrRejectedUser = TestUserMapper.mapToApproveRejectTestUser(approveUserRequest, getUser);
        logger.info("User approved or rejected :::::::::::" + approvedOrRejectedUser);

        testUserRepository.save(approvedOrRejectedUser);
        restWithStatusList = new RestWithStatusList("SUCCESS", "User Decision Saved Successfully", null);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // BULK APPROVE / REJECT — NEW
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> bulkApproveOrRejectUsers(TestUserBulkApproveRequest request,
            UserDetails userDetails) {
        RestWithStatusList restWithStatusList;
        List<Object> processedUsers = new ArrayList<>();
        List<Object> failedUsers = new ArrayList<>();

        // Validate request
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please select at least one user", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        if (request.getApprovedYn() == null || request.getApprovedYn().trim().isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please provide approval decision (Y/N)", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        logger.info("Bulk {} action on userIds={} by {}", request.getApprovedYn(), request.getUserIds(),
                userDetails.getUsername());

        // Process each userId — same pattern as approveOrRejectUser
        for (Long userId : request.getUserIds()) {
            Optional<TestUser> userOpt = testUserRepository.findByUserId(userId);

            if (!userOpt.isPresent()) {
                logger.warn("User not found with ID: {}", userId);
                failedUsers.add("User ID " + userId + " not found");
                continue;
            }

            TestUser user = userOpt.get();
            user.setApprovedYn(request.getApprovedYn());
            user.setApprovedBy(userDetails.getUsername());
            user.setUserStatus("Y".equals(request.getApprovedYn()) ? "ACTIVE" : "INACTIVE");
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy(userDetails.getUsername());

            testUserRepository.save(user);
            logger.info("Bulk decision applied: userId={}, approvedYn={}", userId, request.getApprovedYn());

            ReconUserResponseDto responseDto = TestUserMapper.mapToTestUserResponseDto(user,
                    new ReconUserResponseDto());
            processedUsers.add(responseDto);
        }

        // All failed
        if (processedUsers.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "No users were updated. Check user IDs.", failedUsers);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        // Partial success
        if (!failedUsers.isEmpty()) {
            restWithStatusList = new RestWithStatusList("PARTIAL_SUCCESS",
                    "Some users updated. " + failedUsers.size() + " user(s) not found.", processedUsers);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
        }

        // Full success
        restWithStatusList = new RestWithStatusList("SUCCESS",
                "Bulk decision applied successfully on " + processedUsers.size() + " user(s)", processedUsers);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────
    private TestPasswordManager saveUserPasswordData(String userPassword) {
        TestPasswordManager passwordManager = new TestPasswordManager();
        passwordManager.setUserPassword(passwordEncoder.encode(userPassword));
        passwordManager.setExpirationDate(LocalDateTime.now());
        passwordManager.setCreatedAt(LocalDateTime.now());
        return passwordManager;
    }
}