package com.jpb.reconciliation.reconciliation.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.ReconMenuMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

@Service
public class MenuMasterServiceImpl implements MenuMasterService {

    @Autowired
    MenuMasterRepository menuMasterRepository;

    @Autowired
    AuditLogManagerService auditLogManagerService;

    @Autowired
    ReconUserRepository reconUserRepository;

    @Autowired
    ReconFileDetailsMasterRepository fileDetailsMasterRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    ReconRoleMasterRepository reconRoleMasterRepository;

    Logger logger = LoggerFactory.getLogger(MenuMasterServiceImpl.class);

    @Override
    public ResponseEntity<RestWithStatusList> getMenus(Long menuId) {
        List<Object> menuData = new ArrayList<>();
        ReconMenuMaster menu = menuMasterRepository.findByMenuId(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found :" + menuId));
        ReconMenuMasterDto menuDto = ReconMenuMasterMapper.mapToMenuDto(menu, new ReconMenuMasterDto());
        menuData.add(menuDto);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu found successfully", menuData), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseDto> removeMenu(Long menuId) {
        ReconMenuMaster menu = menuMasterRepository.findByMenuId(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("MENU NOT FOUND :" + menuId));
        List<ReconMenuMaster> existsParentMenuList = menuMasterRepository.findByParentMenuCode(menu.getMenuType());
        if (existsParentMenuList.isEmpty()) {
            menuMasterRepository.deleteById(menu.getMenuId());
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(MenuConstants.STATUS_417, "Please delete the submenu first."));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto(MenuConstants.STATUS_200, "Menu Removed Successfully"));
    }

    @Override
    public boolean updateMenu(ReconMenuMasterDto menuDto) {
        Optional<ReconMenuMaster> opt = menuMasterRepository.findById(menuDto.getMenuId());
        if (!opt.isPresent()) return false;
        ReconMenuMasterMapper.mapToMenu(menuDto, opt.get());
        menuMasterRepository.save(opt.get());
        return true;
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllMenus() {
        List<ReconMenuMaster> fetchMenuData = menuMasterRepository.findAll();
        List<Object> menuList = new ArrayList<>(fetchMenuData);
        if (!fetchMenuData.isEmpty()) {
            return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu details found successfully", menuList), HttpStatus.OK);
        }
        return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu details not found", null), HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenuByUserId(Long userId) {
        List<Object> menuList = new ArrayList<>();
        List<ReconMenuMaster> menuByUserId = menuMasterRepository.getByInsertUserId(userId);
        if (menuByUserId.isEmpty()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu details not found", menuList), HttpStatus.NOT_FOUND);
        }
        for (ReconMenuMaster menu : menuByUserId) {
            ReconFileDetailsMaster fileData = fileDetailsMasterRepository.findByReconFileId(menu.getMenuProcessId());
            menuList.add(ReconMenuMasterMapper.mapToMenuDtoWithFilePath(new ReconMenuMasterDto(), menu, fileData));
        }
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu details found successfully", menuList), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> addMenu(ReconMenuMasterDto menuRequest, UserDetails userDetails) {
        try {
            ReconMenuMaster menuWithName = menuMasterRepository.findByMenuNameAndRoleIdAndParentMenuCode(
                    menuRequest.getMenuName(), menuRequest.getRoleId(), menuRequest.getParentMenuCode());

            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(userDetails.getUsername());
            if (!userOpt.isPresent()) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "User not found", null), HttpStatus.BAD_REQUEST);
            }
            ReconUser userData = userOpt.get();

            if (menuWithName != null &&
                (menuWithName.getParentMenuCode() != null && menuWithName.getParentMenuCode().equalsIgnoreCase(menuRequest.getParentMenuCode())
                || menuWithName.getMenuName().equalsIgnoreCase(menuRequest.getMenuName()))) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu already exists for this role", null), HttpStatus.BAD_REQUEST);
            }

            ReconMenuMaster createdMenu = createMenu(menuRequest, userData.getUsername());

            ReconFileDetailsMaster getFileData = fileDetailsMasterRepository.findByReconFileId(menuRequest.getMenuProcessId());
            if (getFileData != null) {
                getFileData.setReconExitMenuFlag("Y");
                fileDetailsMasterRepository.save(getFileData);
            }
            auditLogManagerService.commonAudit(userData, "Add MENU", createdMenu);
            return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu Created Successfully",
                    java.util.Collections.singletonList(createdMenu)), HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Exception while creating menu: " + e.getMessage(), e);
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Exception occurred while creating menu", null), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<RestWithStatusList> addMenuActive(ReconMenuMasterDto menuRequest, UserDetails userDetails) {
        try {
            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(userDetails.getUsername());
            if (!userOpt.isPresent()) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "User not found", null), HttpStatus.BAD_REQUEST);
            }
            ReconUser userData = userOpt.get();
            if (!com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(userData.getUserType())) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                        "Only a Bank/Branch/KAL Admin can create a Menu that activates immediately.", null), HttpStatus.FORBIDDEN);
            }

            ReconMenuMaster menuWithName = menuMasterRepository.findByMenuNameAndRoleIdAndParentMenuCode(
                    menuRequest.getMenuName(), menuRequest.getRoleId(), menuRequest.getParentMenuCode());
            if (menuWithName != null &&
                (menuWithName.getParentMenuCode() != null && menuWithName.getParentMenuCode().equalsIgnoreCase(menuRequest.getParentMenuCode())
                || menuWithName.getMenuName().equalsIgnoreCase(menuRequest.getMenuName()))) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu already exists for this role", null), HttpStatus.BAD_REQUEST);
            }

            String actor = userDetails.getUsername();
            ReconMenuMaster createdMenu = createMenu(menuRequest, "Y", actor, actor, actor);

            ReconFileDetailsMaster getFileData = fileDetailsMasterRepository.findByReconFileId(menuRequest.getMenuProcessId());
            if (getFileData != null) {
                getFileData.setReconExitMenuFlag("Y");
                fileDetailsMasterRepository.save(getFileData);
            }
            auditLogManagerService.commonAudit(userData, "Add MENU", createdMenu);
            return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu created and activated.",
                    java.util.Collections.singletonList(createdMenu)), HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Exception while creating active menu: " + e.getMessage(), e);
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Exception occurred while creating menu", null), HttpStatus.BAD_REQUEST);
        }
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String createdBy) {
        return createMenu(req, "DRAFT", null, null, createdBy);
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String status, String submittedBy, String approvedBy, String createdBy) {
        ReconMenuMaster m = new ReconMenuMaster();
        m.setMenuType(req.getMenuType());
        m.setMenuName(req.getMenuName());
        m.setMenuDescription(req.getMenuDescription());
        m.setParentMenuCode(req.getParentMenuCode());
        m.setSubMenu(req.getSubMenuReq());
        m.setMasterMenuParent(req.getMasterMenuParent());
        m.setInsertUserId(req.getUserId());
        m.setMenuProcessId(req.getMenuProcessId());
        m.setMenuUrl(req.getMenuUrl());
        m.setProcessType(req.getProcessType());
        m.setStatus(status);
        m.setSubmittedBy(submittedBy);
        m.setApprovedBy(approvedBy);
        m.setRoleId(req.getRoleId());
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        menuMasterRepository.save(m);
        return m;
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenuByRole(Long roleId) {
        List<Object> menuList = new ArrayList<>();
        List<ReconMenuMaster> menuByRole = menuMasterRepository.getByRoleIdAndStatus(roleId, "Y");
        logger.info("Menu data by role: " + menuByRole);
        if (menuByRole.isEmpty()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu not found for this role", menuList), HttpStatus.NOT_FOUND);
        }
        for (ReconMenuMaster menu : menuByRole) {
            ReconFileDetailsMaster fileData = fileDetailsMasterRepository.findByReconFileId(menu.getMenuProcessId());
            menuList.add(ReconMenuMasterMapper.mapToMenuDtoWithFilePath(new ReconMenuMasterDto(), menu, fileData));
        }
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu found successfully", menuList), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> submitForApproval(Long menuId, String submittedBy) {
        Optional<ReconMenuMaster> opt = menuMasterRepository.findByMenuId(menuId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu not found with ID: " + menuId, null), HttpStatus.NOT_FOUND);
        }
        ReconMenuMaster existing = opt.get();
        existing.setStatus("PENDING");
        existing.setSubmittedBy(submittedBy);
        existing.setModifiedBy(submittedBy);
        existing.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        menuMasterRepository.save(existing);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu submitted for approval.", null), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> approveMenu(Long menuId, String approvedBy) {
        Optional<ReconMenuMaster> opt = menuMasterRepository.findByMenuId(menuId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu not found with ID: " + menuId, null), HttpStatus.NOT_FOUND);
        }
        ReconMenuMaster existing = opt.get();
        existing.setStatus("Y");
        existing.setApprovedBy(approvedBy);
        existing.setModifiedBy(approvedBy);
        existing.setModifiedDate(new Timestamp(System.currentTimeMillis()));
        menuMasterRepository.save(existing);
        if (existing.getSubmittedBy() != null) {
            Optional<ReconUser> maker = reconUserRepository.findByUsername(existing.getSubmittedBy());
            if (maker.isPresent()) {
                emailService.sendWorkflowDecisionNotification(
                        maker.get().getEmail(), maker.get().getFullName(),
                        "Menu", existing.getMenuName(), String.valueOf(existing.getMenuId()), "Approved", approvedBy);
            }
        }
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menu approved and activated.", null), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenusByBankId(Long bankId) {
        List<ReconUser> bankUsers = reconUserRepository.findByBankId(bankId);

        List<Long> roleIdsFromUsers = bankUsers.stream()
                .map(ReconUser::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> usernames = bankUsers.stream()
                .map(ReconUser::getUsername).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<Long> roleIdsFromCreator = usernames.isEmpty() ? new ArrayList<>()
                : reconRoleMasterRepository.findByCreatedByIn(usernames).stream()
                        // A role an admin created can already be assigned to users of a DIFFERENT
                        // bank/branch (e.g. Bank Admin creates a role while onboarding a Branch).
                        // Such roles' menus belong to that branch's view, not this bank's.
                        .filter(r -> reconUserRepository.findByRoleId(r.getRoleId()).stream()
                                .allMatch(u -> bankId.equals(u.getBankId())))
                        .map(ReconRoleMaster::getRoleId).collect(Collectors.toList());

        List<Long> allRoleIds = new ArrayList<>(roleIdsFromUsers);
        roleIdsFromCreator.forEach(id -> { if (!allRoleIds.contains(id)) allRoleIds.add(id); });

        List<ReconMenuMaster> menus = allRoleIds.isEmpty() ? new ArrayList<>() : menuMasterRepository.findByRoleIdIn(allRoleIds);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched for bank.", new ArrayList<>(menus)), HttpStatus.OK);
    }

    @Override
    public Long getVerifiedRoleId(String username) {
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        ReconUser reconUser = userOpt.get();
        if (reconUser.getRoleId() == null) {
            throw new UsernameNotFoundException("No role assigned to user: " + username);
        }
        return reconUser.getRoleId();
    }
}
