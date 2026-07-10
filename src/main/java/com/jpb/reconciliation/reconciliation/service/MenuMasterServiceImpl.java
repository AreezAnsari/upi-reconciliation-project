package com.jpb.reconciliation.reconciliation.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

@Service
public class MenuMasterServiceImpl implements MenuMasterService {

    // Fixed Master-menu sequence for the Sidebar — same for every role, regardless of which
    // order privileges were granted in. Anything not listed here (custom Masters an Admin
    // creates via Add Menu) sorts after these, via the default(99) fallback.
    private static final Map<String, Integer> MASTER_GROUP_ORDER = new HashMap<>();
    static {
        MASTER_GROUP_ORDER.put("Dashboard", 0);
        MASTER_GROUP_ORDER.put("My Organization", 1);
        MASTER_GROUP_ORDER.put("Administration", 2);
    }

    @Autowired
    MenuMasterRepository menuMasterRepository;

    @Autowired
    AuditLogManagerService auditLogManagerService;

    @Autowired
    com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder approvalAuditRecorder;

    @Autowired
    com.jpb.reconciliation.reconciliation.service.v2.WorkflowNotifier workflowNotifier;

    @Autowired
    ReconUserRepository reconUserRepository;

    @Autowired
    ReconFileDetailsMasterRepository fileDetailsMasterRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    ReconRoleProductMapRepository roleProductMapRepository;

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
            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(userDetails.getUsername());
            if (!userOpt.isPresent()) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "User not found", null), HttpStatus.BAD_REQUEST);
            }
            ReconUser userData = userOpt.get();

            String rejection = validateAgainstCatalog(menuRequest);
            if (rejection != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", rejection, null), HttpStatus.BAD_REQUEST);
            }

            // Scoped to the caller's own bank, taken from their account rather than the request:
            // the client must not be able to name the institution a menu is created under.
            ReconMenuMaster menuWithName = menuMasterRepository.findByMenuNameAndBankIdAndParentMenuCode(
                    menuRequest.getMenuName(), userData.getBankId(), menuRequest.getParentMenuCode());

            if (menuWithName != null &&
                (menuWithName.getParentMenuCode() != null && menuWithName.getParentMenuCode().equalsIgnoreCase(menuRequest.getParentMenuCode())
                || menuWithName.getMenuName().equalsIgnoreCase(menuRequest.getMenuName()))) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu already exists for this bank", null), HttpStatus.BAD_REQUEST);
            }

            materialiseCatalogParents(menuRequest, userData.getBankId(), userData.getUsername());
            ReconMenuMaster createdMenu = createMenu(menuRequest, userData.getUsername(), userData.getBankId());

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
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Exception occurred while creating menu: " + e.getMessage(), null), HttpStatus.BAD_REQUEST);
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

            String rejection = validateAgainstCatalog(menuRequest);
            if (rejection != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", rejection, null), HttpStatus.BAD_REQUEST);
            }

            ReconMenuMaster menuWithName = menuMasterRepository.findByMenuNameAndBankIdAndParentMenuCode(
                    menuRequest.getMenuName(), userData.getBankId(), menuRequest.getParentMenuCode());
            if (menuWithName != null &&
                (menuWithName.getParentMenuCode() != null && menuWithName.getParentMenuCode().equalsIgnoreCase(menuRequest.getParentMenuCode())
                || menuWithName.getMenuName().equalsIgnoreCase(menuRequest.getMenuName()))) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu already exists for this bank", null), HttpStatus.BAD_REQUEST);
            }

            String actor = userDetails.getUsername();
            materialiseCatalogParents(menuRequest, userData.getBankId(), actor);
            ReconMenuMaster createdMenu = createMenu(menuRequest, "Y", actor, actor, actor, userData.getBankId());

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
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Exception occurred while creating menu: " + e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Add Menu may only map a menu the system already knows about.
     *
     * The catalog (BANK_ID IS NULL rows, inserted by sql/menu_catalog_seed.sql) is the register
     * of every menu that may exist. Add Menu appends a bank-owned copy of one of those rows; it
     * does not invent names. Returns the rejection message, or null when the request is valid.
     */
    private String validateAgainstCatalog(ReconMenuMasterDto req) {
        String name = req.getMenuName() == null ? "" : req.getMenuName().trim();
        if (name.isEmpty()) return "Menu name is required.";

        if ("Submenu".equals(req.getMenuType())) {
            boolean registered = menuMasterRepository
                    .findCatalogSubmenu(name, req.getParentMenuCode(), req.getMasterMenuParent())
                    .isPresent();
            if (!registered) {
                return "'" + name + "' is not a registered sub-menu under "
                        + req.getMasterMenuParent() + " > " + req.getParentMenuCode()
                        + ". Contact your administrator to register it first.";
            }
            if (req.getMenuProcessId() == null) {
                return "A sub-menu must be mapped to a process/template before it can be created.";
            }
            return null;
        }

        // Master and Main are seeded per bank at onboarding, or materialised from the catalog by
        // materialiseCatalogParents(). Creating them by hand is still allowed only if registered.
        return menuMasterRepository.findCatalogByNameAndType(name, req.getMenuType()).isPresent()
                ? null
                : "'" + name + "' is not a registered " + req.getMenuType() + " menu. "
                  + "Contact your administrator to register it first.";
    }

    /**
     * Copies the catalog's Master and Main for a submenu into this bank, if the bank has no row
     * for them yet.
     *
     * A bank only receives Dashboard / My Organization / Administration at onboarding. The first
     * time someone maps, say, Configuration > Template Management > Define Extraction, that bank
     * needs its own Configuration and Template Management rows for the submenu to hang off —
     * every nesting lookup in the app matches on the parent's NAME within the same bank.
     */
    private void materialiseCatalogParents(ReconMenuMasterDto req, Long bankId, String createdBy) {
        if (!"Submenu".equals(req.getMenuType()) || bankId == null) return;
        copyCatalogRowIfMissing(req.getMasterMenuParent(), "Master", bankId, createdBy);
        copyCatalogRowIfMissing(req.getParentMenuCode(), "Main", bankId, createdBy);
    }

    private void copyCatalogRowIfMissing(String menuName, String menuType, Long bankId, String createdBy) {
        if (menuName == null || menuMasterRepository.findByMenuNameAndBankId(menuName, bankId) != null) return;
        Optional<ReconMenuMaster> catalogOpt = menuMasterRepository.findCatalogByNameAndType(menuName, menuType);
        if (!catalogOpt.isPresent()) return;
        ReconMenuMaster c = catalogOpt.get();

        ReconMenuMaster copy = new ReconMenuMaster();
        copy.setMenuType(c.getMenuType());
        copy.setMenuName(c.getMenuName());
        copy.setMenuDescription(c.getMenuDescription());
        copy.setParentMenuCode(c.getParentMenuCode());
        copy.setMasterMenuParent(c.getMasterMenuParent());
        copy.setSubMenu("N");
        copy.setMenuUrl(c.getMenuUrl());
        copy.setStatus("Y");
        copy.setBankId(bankId);
        copy.setIsPortalTwin("N");
        copy.setCreatedBy(createdBy);
        copy.setCreatedDate(new Date());
        copy.setInsertDate(new Date());
        menuMasterRepository.save(copy);
        logger.info("Materialised catalog {} '{}' for bankId={}", menuType, menuName, bankId);
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String createdBy, Long bankId) {
        return createMenu(req, "DRAFT", null, null, createdBy, bankId);
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String status, String submittedBy, String approvedBy,
                                       String createdBy, Long bankId) {
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
        // Owner. Taken from the authenticated caller, never from the request body.
        m.setBankId(bankId);
        // Product the mapping belongs to. NULL = platform menu, visible to every product.
        m.setProductId(req.getProductId());
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        menuMasterRepository.save(m);
        return m;
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenuByRole(Long roleId) {
        List<Object> menuList = new ArrayList<>();
        // A role's menus are the ones granted to it in C_ROLE_MENU_MAP. This used to read
        // RECON_MENU_MASTER.ROLE_ID, which held ownership, not grants — the two agreed only
        // because saveMenu() (bootstrap) and savePrivileges() (portal twins) happened to write
        // both. Grants are the real answer to "what may this role see".
        List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        List<ReconMenuMaster> menuByRole = menuIds.isEmpty()
                ? new ArrayList<>()
                : menuMasterRepository.findAllById(menuIds).stream()
                        .filter(m -> "Y".equals(m.getStatus()))
                        .collect(Collectors.toList());
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
    public ResponseEntity<RestWithStatusList> getMenusByRolePrivileges(Long roleId) {
        List<Object> menuList = new ArrayList<>();
        List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        if (menuIds.isEmpty()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "No privileges assigned to this role", menuList), HttpStatus.NOT_FOUND);
        }
        List<ReconMenuMaster> mapped = menuMasterRepository.findAllById(menuIds).stream()
                .filter(m -> "Y".equals(m.getStatus()))
                .collect(Collectors.toList());

        // Sidebar.jsx only nests a Main-type menu under a Master it can find in this same
        // result set — if Privileges only had the child checked (not its parent Master),
        // the child becomes an orphaned, invisible entry. Pull the parent Master in here too,
        // even though it was never explicitly added to C_ROLE_MENU_MAP for this role.
        Map<Long, ReconMenuMaster> byId = new LinkedHashMap<>();
        for (ReconMenuMaster m : mapped) byId.put(m.getMenuId(), m);

        // Same problem one level deeper: granting only a Submenu leaves it orphaned, because the
        // Sidebar nests Submenu -> Main -> Master. Pull in the Submenu's own Main (within the same
        // bank — MENU_NAME is not unique across institutions) so the loop below can then reach the
        // Master through it.
        List<ReconMenuMaster> effective = new ArrayList<>(mapped);
        for (ReconMenuMaster m : mapped) {
            if (!"Submenu".equals(m.getMenuType()) || m.getParentMenuCode() == null) continue;
            ReconMenuMaster main = menuMasterRepository.findByMenuNameAndBankId(m.getParentMenuCode(), m.getBankId());
            if (main != null && "Y".equals(main.getStatus()) && byId.putIfAbsent(main.getMenuId(), main) == null) {
                effective.add(main);
            }
        }

        // Cache the resolved parent PER NAME, resolved once per request — Sidebar.jsx nests by
        // NAME match only, so if two different Main items sharing a parentMenuCode name each
        // independently resolved to a DIFFERENT physical Master row (since MENU_NAME isn't
        // unique — Kal Admin, each Bank Admin, each Branch Admin all have their own
        // "Administration"/"My Organization"), the Sidebar renders one section per row, each
        // duplicating the same children by name. Resolving once and reusing prevents that.
        Map<String, ReconMenuMaster> parentCache = new LinkedHashMap<>();
        for (ReconMenuMaster m : effective) {
            if (!"Main".equals(m.getMenuType()) || m.getParentMenuCode() == null) continue;
            String parentCode = m.getParentMenuCode();
            ReconMenuMaster parent = parentCache.get(parentCode);
            if (parent == null && !parentCache.containsKey(parentCode)) {
                parent = menuMasterRepository.findAllByMenuNameAndMenuType(parentCode, "Master")
                        .stream().filter(p -> "Y".equals(p.getStatus())).findFirst().orElse(null);
                if (parent == null) {
                    try {
                        parent = menuMasterRepository.findByMenuId(Long.parseLong(parentCode)).orElse(null);
                    } catch (NumberFormatException ignored) {
                        // parentMenuCode isn't numeric — not a legacy ID-based row, nothing to fall back to
                    }
                }
                parentCache.put(parentCode, parent);
            }
            if (parent != null && "Y".equals(parent.getStatus())) {
                byId.putIfAbsent(parent.getMenuId(), parent);
            }
        }

        // Global uniqueness guard: MENU_NAME is not unique for Master rows (Kal Admin, each
        // Bank Admin, each Branch Admin all have their own "My Organization"/"Administration")
        // — if TWO different physical Master rows sharing the same name ever both end up in
        // byId (whether directly mapped in C_ROLE_MENU_MAP, e.g. via the backfill migration, or
        // pulled in as an ancestor above), the Sidebar would render one section per row, each
        // duplicating the same children. Collapse to one Master per name here, regardless of
        // how the duplicate got in — this is the single place that guarantees it everywhere.
        Map<String, Long> firstMasterIdByName = new LinkedHashMap<>();
        for (ReconMenuMaster m : byId.values()) {
            if (!"Master".equals(m.getMenuType())) continue;
            firstMasterIdByName.putIfAbsent(m.getMenuName(), m.getMenuId());
        }
        byId.values().removeIf(m -> "Master".equals(m.getMenuType())
                && !m.getMenuId().equals(firstMasterIdByName.get(m.getMenuName())));

        // Sort into the same sequential order the Admin sees. Master-to-Master order is fixed
        // by name (Dashboard, then My Organization, then Administration, anything else last) —
        // NOT by MENU_ID, since the ancestor-Master lookup above can resolve "My Organization"/
        // "Administration" to whichever same-named row it happens to find first across several
        // bootstrap trees, so their relative MENU_ID order isn't reliable. Within each Master's
        // group, items still sort by MENU_ID (a twin's own ID reflects when it was
        // privilege-assigned, not its canonical position, so TWIN_OF_MENU_ID is used instead).
        List<ReconMenuMaster> ordered = new ArrayList<>(byId.values());
        ordered.sort(Comparator
                .comparing((ReconMenuMaster m) -> MASTER_GROUP_ORDER.getOrDefault(
                        "Master".equals(m.getMenuType()) ? m.getMenuName() : m.getParentMenuCode(), 99))
                .thenComparing(m -> m.getTwinOfMenuId() != null ? m.getTwinOfMenuId() : m.getMenuId()));

        for (ReconMenuMaster menu : ordered) {
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
        approvalAuditRecorder.recordSubmission(
                com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_MENU, menuId,
                com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ACTION_CREATE, submittedBy);
        workflowNotifier.notifySubmission("Menu", existing.getMenuName(), String.valueOf(existing.getMenuId()), submittedBy,
                checker -> isVisibleToChecker(checker, existing.getCreatedBy(), existing.getProductId()));
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
        approvalAuditRecorder.recordDecision(
                com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_MENU, menuId,
                approvedBy, "APPROVED", null);
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
    public ResponseEntity<RestWithStatusList> getPendingMenusForChecker(String checkerUsername) {
        Optional<ReconUser> checkerOpt = reconUserRepository.findByUsername(checkerUsername);
        if (!checkerOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Checker not found: " + checkerUsername, null), HttpStatus.NOT_FOUND);
        }
        ReconUser checker = checkerOpt.get();
        List<ReconMenuMaster> visible = menuMasterRepository.findAll().stream()
                .filter(m -> "PENDING".equals(m.getStatus()))
                .filter(m -> isVisibleToChecker(checker, m.getCreatedBy(), m.getProductId()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Pending menus fetched.", visible), HttpStatus.OK);
    }

    // Same scoping rule as ReconRoleMasterServiceImpl.isVisibleToChecker/getPendingRolesForChecker
    // — duplicated here rather than shared, since the two services live in different packages
    // (service vs service.v2) and this is a handful of lines, not worth a shared utility class for.
    // itemProductId is the menu's own PRODUCT_ID. It used to be the owning role's id, from which
    // the product set was looked up in C_ROLE_PRODUCT_MAP — one indirection that now resolves to
    // the same answer, since a menu carries its product directly. NULL still means "unrestricted".
    private boolean isVisibleToChecker(ReconUser checker, String submitterUsername, Long itemProductId) {
        if ("KAL_ADMIN".equals(checker.getUserType())) return true;

        Optional<ReconUser> submitterOpt = reconUserRepository.findByUsername(submitterUsername);
        if (!submitterOpt.isPresent() || !Objects.equals(submitterOpt.get().getBankId(), checker.getBankId())) {
            return false;
        }

        if (UserConstants.isAdminUserType(checker.getUserType())) return true;

        Set<Long> checkerScope = resolveProductScope(checker.getRoleId());
        Set<Long> itemScope = itemProductId == null
                ? Collections.emptySet()
                : Collections.singleton(itemProductId);
        return checkerScope.isEmpty() || itemScope.isEmpty() || !Collections.disjoint(checkerScope, itemScope);
    }

    private Set<Long> resolveProductScope(Long roleId) {
        if (roleId == null) return Collections.emptySet();
        return new HashSet<>(roleProductMapRepository.findProductIdsByRoleId(roleId));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenusByBankId(Long bankId) {
        // Direct now that a menu row names its owner. This used to walk bank -> users -> roleIds
        // (plus roles those users created, minus roles already lent to another branch) purely to
        // reach the bank a menu belonged to. BANK_ID answers that in one hop, and a branch is its
        // own RECON_BANK_MASTER row so branches are covered too.
        List<ReconMenuMaster> menus = menuMasterRepository.findByBankId(bankId);
        // /user-portal twins (see ReconRoleMasterServiceImpl.getOrCreateUserTwinMenu) are an
        // internal implementation detail resolved automatically at privilege-save time — never
        // something an Admin selects or manages directly, so hide them from this listing (used
        // by both the Menu Access Tree and Menu List) to avoid duplicate-looking rows.
        Set<String> hidden = resolveUnmappedHiddenNames(menus);
        List<ReconMenuMaster> visible = menus.stream()
                .filter(m -> !"Y".equals(m.getIsPortalTwin()))
                .filter(m -> !hidden.contains(m.getMenuName()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched for bank.", new ArrayList<>(visible)), HttpStatus.OK);
    }

    /**
     * Case A of the Privileges visibility rule.
     *
     * A Main that the CATALOG declares sub-menus under is useless until at least one of those
     * sub-menus has actually been mapped to a process for this bank (status Y, MENU_PROCESS_ID
     * set). Until then the Main — and its Master, if every one of that Master's Mains is in the
     * same state — is withheld.
     *
     * Case B: a Main the catalog declares no sub-menus under is always visible. That is every
     * Main a bank receives at onboarding, so nothing that works today starts hiding.
     *
     * Returns the menu NAMES to withhold; nesting everywhere in the app is by name within a bank.
     */
    private Set<String> resolveUnmappedHiddenNames(List<ReconMenuMaster> bankMenus) {
        Set<String> mainsThatNeedSubmenus = new HashSet<>(menuMasterRepository.findCatalogMainsWithSubmenus());
        if (mainsThatNeedSubmenus.isEmpty()) return Collections.emptySet();

        Set<String> mainsWithAMappedSubmenu = bankMenus.stream()
                .filter(m -> "Submenu".equals(m.getMenuType()))
                .filter(m -> "Y".equals(m.getStatus()) && m.getMenuProcessId() != null)
                .map(ReconMenuMaster::getParentMenuCode)
                .collect(Collectors.toSet());

        Set<String> hidden = new HashSet<>();
        for (ReconMenuMaster m : bankMenus) {
            if (!"Main".equals(m.getMenuType())) continue;
            if (mainsThatNeedSubmenus.contains(m.getMenuName()) && !mainsWithAMappedSubmenu.contains(m.getMenuName())) {
                hidden.add(m.getMenuName());
            }
        }

        // A Master survives only while it still has at least one visible Main.
        for (ReconMenuMaster master : bankMenus) {
            if (!"Master".equals(master.getMenuType())) continue;
            List<ReconMenuMaster> mains = bankMenus.stream()
                    .filter(x -> "Main".equals(x.getMenuType()) && master.getMenuName().equals(x.getParentMenuCode()))
                    .collect(Collectors.toList());
            if (!mains.isEmpty() && mains.stream().allMatch(x -> hidden.contains(x.getMenuName()))) {
                hidden.add(master.getMenuName());
            }
        }
        return hidden;
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
