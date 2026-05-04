package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RoleApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.TestRoleBulkApproveRequest;
import com.jpb.reconciliation.reconciliation.dto.RoleCreateRequest;
import com.jpb.reconciliation.reconciliation.dto.RoleMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.TestRole;
import com.jpb.reconciliation.reconciliation.mapper.TestRoleMenuMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.TestRoleManageRepository;

@Service
public class TestRoleManageServiceImpl implements TestRoleManageService {

    Logger logger = LoggerFactory.getLogger(TestRoleManageServiceImpl.class);

    @Autowired
    TestRoleManageRepository roleManageRepository;

    @Autowired
    MenuMasterRepository menuMasterRepository;
    
    @Autowired
    DecisionHistoryService decisionHistoryService;

    @Override
    public ResponseEntity<RestWithStatusList> getAllRoleDetails() {
        RestWithStatusList restWithStatusList;
        List<Object> roleWithMenuList = new ArrayList<>();
        List<TestRole> getAllRole = roleManageRepository.findAll();
        List<ReconMenuMaster> getAllMenu = menuMasterRepository.findAll();

        if (!getAllRole.isEmpty()) {
            for (TestRole role : getAllRole) {
                List<ReconMenuMaster> menuWithRoleList = new ArrayList<>();
                for (ReconMenuMaster menu : getAllMenu) {
                    if (menu.getRoleId() != null && menu.getRoleId().equals(role.getRoleId())) {
                        menuWithRoleList.add(menu);
                    }
                }
                RoleMasterDto roleMenu = TestRoleMenuMapper.mapRoleWithMenu(role, menuWithRoleList, new RoleMasterDto());
                roleWithMenuList.add(roleMenu);
            }
            restWithStatusList = new RestWithStatusList("SUCCESS", "Role Found Successfully", roleWithMenuList);
        } else {
            restWithStatusList = new RestWithStatusList("FAILURE", "Role Not Found", roleWithMenuList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleByUserLogin(Long verifiedRoleId) {
        RestWithStatusList restWithStatusList;
        List<Object> roleData = new ArrayList<>();
        TestRole getLoginRole = roleManageRepository.findByRoleId(verifiedRoleId);
        roleData.add(getLoginRole);
        restWithStatusList = new RestWithStatusList("SUCCESS", "Login role user found.", roleData);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRole(String roleCode, String roleName, List<Long> menuIds,
            String createdBy) {
        logger.info("Creating role: code={}, name={}, menus={}", roleCode, roleName, menuIds);

        if (roleManageRepository.existsByRoleCode(roleCode)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestWithStatusList("FAILURE",
                    "Role code '" + roleCode + "' already exists. Please use a unique code.", null));
        }

        TestRole newRole = new TestRole();
        newRole.setRoleCode(roleCode.toUpperCase().trim());
        newRole.setRoleName(roleName.trim());
        newRole.setCreatedAt(LocalDateTime.now());
        newRole.setCreatedBy(createdBy);
        newRole.setApprovedYn("N");
        TestRole savedRole = roleManageRepository.save(newRole);
        logger.info("Role saved with ID: {}", savedRole.getRoleId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Role created successfully", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> updateRoleDetails(RoleCreateRequest request, String username) {
        TestRole existingRole = roleManageRepository.findById(request.getRoleId()).orElse(null);
        if (existingRole == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new RestWithStatusList("FAILURE", "Role with ID '" + request.getRoleId() + "' not found.", null));
        }

        if (request.getRoleCode() != null) {
            String newCode = request.getRoleCode().toUpperCase().trim();
            if (roleManageRepository.existsByRoleCodeAndRoleIdNot(newCode, existingRole.getRoleId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestWithStatusList("FAILURE",
                        "Role code '" + newCode + "' is already used by another role.", null));
            }
            existingRole.setRoleCode(newCode);
        }

        if (request.getRoleName() != null) {
            existingRole.setRoleName(request.getRoleName().trim());
        }

        existingRole.setUpdatedAt(LocalDateTime.now());
        existingRole.setUpdatedBy(username);
        roleManageRepository.save(existingRole);
        logger.info("Role updated successfully: roleId={}", existingRole.getRoleId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(new RestWithStatusList("SUCCESS", "Role updated successfully", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPendingRoles(String approvedYn) {
        RestWithStatusList restWithStatusList;
        List<Object> roleList = new ArrayList<>();

        List<TestRole> roles = roleManageRepository.findByApprovedYn(approvedYn);
        logger.info("Roles with approvedYn={} : {}", approvedYn, roles);

        if (roles.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No roles found", roleList);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        roles.stream().map(r -> (Object) r).forEach(roleList::add);
        restWithStatusList = new RestWithStatusList("SUCCESS", "Roles fetched successfully", roleList);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> approveOrRejectRole(RoleApproveRequest request,
            UserDetails userDetails) {
        RestWithStatusList restWithStatusList;

        if (request == null || request.getRoleId() == null) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please select a role to approve", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        TestRole role = roleManageRepository.findByRoleId(request.getRoleId());
        if (role == null) {
            restWithStatusList = new RestWithStatusList("FAILURE",
                    "Role not found with ID: " + request.getRoleId(), null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        role.setApprovedYn(request.getApprovedYn());
        role.setApprovedBy(userDetails.getUsername());
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(userDetails.getUsername());

        roleManageRepository.save(role);
        logger.info("Role {} by {} | roleId={}", request.getApprovedYn(), userDetails.getUsername(),
                request.getRoleId());

        // — audit log save hoga
        decisionHistoryService.saveCheckerDecisionAudit(
            "CHECKER_ROLE",               // module
            role.getRoleName(),           // item name
            role.getRoleId(),             // item id
            request.getApprovedYn(),      // "Y" / "N" / "I"
            userDetails.getUsername(),    // checker ka username
            null,                         // userId — optional
            request.getRoleStatus()       // remarks field use kar lo
        );

        restWithStatusList = new RestWithStatusList("SUCCESS", "Role decision saved successfully", null);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }

    // NEW — Bulk approve / reject multiple roles at once
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> bulkApproveOrRejectRoles(TestRoleBulkApproveRequest request,
            UserDetails userDetails) {
        RestWithStatusList restWithStatusList;
        List<Object> processedRoles = new ArrayList<>();
        List<Object> failedRoles = new ArrayList<>();

        // Validate request
        if (request == null || request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please select at least one role", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        if (request.getApprovedYn() == null || request.getApprovedYn().trim().isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "Please provide approval decision (Y/N/I)", null);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
        }

        logger.info("Bulk {} action on roleIds={} by {}", request.getApprovedYn(), request.getRoleIds(),
                userDetails.getUsername());

        // Process each roleId — same pattern as approveOrRejectRole
        for (Long roleId : request.getRoleIds()) {
            TestRole role = roleManageRepository.findByRoleId(roleId);

            if (role == null) {
                logger.warn("Role not found with ID: {}", roleId);
                failedRoles.add("Role ID " + roleId + " not found");
                continue;
            }

            role.setApprovedYn(request.getApprovedYn());
            role.setApprovedBy(userDetails.getUsername());
            role.setUpdatedAt(LocalDateTime.now());
            role.setUpdatedBy(userDetails.getUsername());

            roleManageRepository.save(role);

            decisionHistoryService.saveCheckerDecisionAudit(
                "CHECKER_ROLE",
                role.getRoleName(),
                role.getRoleId(),
                request.getApprovedYn(),
                userDetails.getUsername(),
                null,
                request.getRemarks()
            );
            logger.info("Bulk decision applied: roleId={}, approvedYn={}", roleId, request.getApprovedYn());

            RoleMasterDto roleDto = TestRoleMenuMapper.mapRoleToDto(role, new RoleMasterDto());
            processedRoles.add(roleDto);
        }

        // All failed
        if (processedRoles.isEmpty()) {
            restWithStatusList = new RestWithStatusList("FAILURE", "No roles were updated. Check role IDs.", failedRoles);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
        }

        // Partial success
        if (!failedRoles.isEmpty()) {
            restWithStatusList = new RestWithStatusList("PARTIAL_SUCCESS",
                    "Some roles updated. " + failedRoles.size() + " role(s) not found.", processedRoles);
            return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
        }

        // Full success
        restWithStatusList = new RestWithStatusList("SUCCESS",
                "Bulk decision applied successfully on " + processedRoles.size() + " role(s)", processedRoles);
        return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
    }
}