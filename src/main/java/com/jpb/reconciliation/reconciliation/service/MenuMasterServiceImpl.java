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
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.ReconMenuMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductMasterRepository;
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
    ReconProcessDefMasterRepository processDefMasterRepository;

    @Autowired
    ReconProductMasterRepository productMasterRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    ReconRoleProductMapRepository roleProductMapRepository;

    @Autowired
    com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository bankProductMapRepository;

    @Autowired
    com.jpb.reconciliation.reconciliation.service.v2.HierarchyScopeService hierarchyScopeService;

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
        if (isCatalogRow(menu)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto(MenuConstants.STATUS_417, CATALOG_IMMUTABLE));
        }
        // Was: findByParentMenuCode(menu.getMenuType()) — compared the menu's own TYPE STRING
        // ("Master"/"Main"/"Submenu") against every row's MENU_PARENT, which no real row's value
        // ever equals, so this silently returned empty regardless of real children. Fixed to use
        // the reliable PARENT_MENU_ID pointer (see sql/menu_parent_id_migration.sql) — this can
        // only make deletion MORE cautious (blocks what should never have been allowed), never
        // reject a delete that correctly succeeds today.
        List<ReconMenuMaster> existsParentMenuList = menuMasterRepository.findByParentMenuId(menu.getMenuId());
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
    public String updateMenu(ReconMenuMasterDto menuDto, String updatedBy) {
        Optional<ReconMenuMaster> opt = menuMasterRepository.findById(menuDto.getMenuId());
        if (!opt.isPresent()) return "FAILED";
        // Catalog rows are system metadata — never editable from any screen.
        if (isCatalogRow(opt.get())) return "CATALOG_IMMUTABLE";

        // Maker-checker on UPDATE: Admin applies immediately; a Maker's edit is held as a PENDING
        // approval request (proposed changes stashed, live menu untouched) until a Checker approves.
        boolean isAdmin = updatedBy != null && reconUserRepository.findByUsername(updatedBy)
                .map(a -> UserConstants.isAdminUserType(a.getUserType())).orElse(false);
        if (!isAdmin && updatedBy != null) {
            java.util.Map<String, Object> changes = new java.util.LinkedHashMap<>();
            if (menuDto.getMenuName() != null) changes.put("menuName", menuDto.getMenuName());
            if (menuDto.getMenuDescription() != null) changes.put("menuDescription", menuDto.getMenuDescription());
            if (menuDto.getMenuUrl() != null) changes.put("menuUrl", menuDto.getMenuUrl());
            approvalAuditRecorder.recordSubmission(
                    com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_MENU, menuDto.getMenuId(),
                    com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ACTION_UPDATE, updatedBy,
                    com.jpb.reconciliation.reconciliation.service.v2.ApprovalJson.write(changes));
            return "SUBMITTED";
        }

        ReconMenuMaster m = opt.get();
        ReconMenuMasterMapper.mapToMenu(menuDto, m);
        // isClickable is always backend-derived, never trusted from the request — keep it synced
        // whenever menuUrl changes via Edit Menu, same as at creation time.
        m.setIsClickable(m.getMenuUrl() != null && !m.getMenuUrl().trim().isEmpty() ? "Y" : "N");
        menuMasterRepository.save(m);
        return "APPLIED";
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
    public ResponseEntity<RestWithStatusList> addMenu(ReconMenuMasterDto menuRequest, UserDetails userDetails, boolean force) {
        try {
            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(userDetails.getUsername());
            if (!userOpt.isPresent()) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", "User not found", null), HttpStatus.BAD_REQUEST);
            }
            ReconUser userData = userOpt.get();

            // An appended menu row is always bank-scoped. A caller with no institution (e.g. a
            // Kal Admin) has nothing to append to — reject cleanly here instead of letting the
            // insert collide with the catalog's unique code index and surface a raw DB error.
            if (userData.getBankId() == null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                        "Menus can only be added by a Bank or Branch administrator, not from a Kal Admin account.",
                        null), HttpStatus.BAD_REQUEST);
            }

            // The new custom Master/Main/Submenu path — entirely separate validation/creation,
            // no catalog lookup at all. Everything below this block (today's catalog-driven
            // sequence) is unreached and unchanged when this flag is absent/false.
            if (Boolean.TRUE.equals(menuRequest.getCustom())) {
                return addCustomMenu(menuRequest, userData, "DRAFT", null, null);
            }

            String rejection = validateAgainstCatalog(menuRequest);
            if (rejection != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", rejection, null), HttpStatus.BAD_REQUEST);
            }

            // Product is never user input — auto-resolved from the catalog hierarchy the request
            // already selected (Extraction/Reconciliation only; every other module stays NULL).
            // Overwriting here means every downstream step (bank-ownership check, duplicate-key
            // resolution, persistence) reads the correct value with no further changes needed.
            menuRequest.setProductId(resolveAutoProductId(menuRequest));

            // One Process id may only ever back one live menu, system-wide.
            String dupProcess = validateProcessIdUnique(menuRequest.getMenuProcessId());
            if (dupProcess != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", dupProcess, null), HttpStatus.BAD_REQUEST);
            }

            // Bank is taken from the caller's account, never from the request body — a client must
            // not be able to name the institution a menu is mapped under.
            String badProduct = validateProductForBank(menuRequest.getProductId(), userData.getBankId());
            if (badProduct != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", badProduct, null), HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<RestWithStatusList> existing =
                    resolveExistingMapping(menuRequest, userData.getBankId(), userData.getUsername(), "DRAFT", force);
            if (existing != null) return existing;

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
    public ResponseEntity<RestWithStatusList> addMenuActive(ReconMenuMasterDto menuRequest, UserDetails userDetails, boolean force) {
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
            // An appended menu row is bank-scoped; a caller with no institution (Kal Admin) has
            // nothing to append to. Reject cleanly instead of hitting the catalog unique-index.
            if (userData.getBankId() == null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                        "Menus can only be added by a Bank or Branch administrator, not from a Kal Admin account.",
                        null), HttpStatus.BAD_REQUEST);
            }

            if (Boolean.TRUE.equals(menuRequest.getCustom())) {
                String actorName = userDetails.getUsername();
                return addCustomMenu(menuRequest, userData, "Y", actorName, actorName);
            }

            String rejection = validateAgainstCatalog(menuRequest);
            if (rejection != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", rejection, null), HttpStatus.BAD_REQUEST);
            }

            // Product is never user input — see the matching comment in addMenu().
            menuRequest.setProductId(resolveAutoProductId(menuRequest));

            String dupProcess = validateProcessIdUnique(menuRequest.getMenuProcessId());
            if (dupProcess != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", dupProcess, null), HttpStatus.BAD_REQUEST);
            }

            String badProduct = validateProductForBank(menuRequest.getProductId(), userData.getBankId());
            if (badProduct != null) {
                return new ResponseEntity<>(new RestWithStatusList("FAILURE", badProduct, null), HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<RestWithStatusList> existing =
                    resolveExistingMapping(menuRequest, userData.getBankId(), userData.getUsername(), "Y", force);
            if (existing != null) return existing;

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

    /**
     * Custom Master/Main/Submenu creation — completely separate from the catalog-driven path
     * above. No catalog lookup at all: the row is built directly from the request. Reached only
     * when {@code menuRequest.getCustom() == true} (addMenu/addMenuActive dispatch), so today's
     * catalog flow is never touched by this method existing.
     */
    private ResponseEntity<RestWithStatusList> addCustomMenu(ReconMenuMasterDto req, ReconUser userData,
                                                              String status, String submittedBy, String approvedBy) {
        String rejection = validateCustomMenu(req, userData.getBankId());
        if (rejection != null) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", rejection, null), HttpStatus.BAD_REQUEST);
        }
        String badProduct = validateProductForBank(req.getProductId(), userData.getBankId());
        if (badProduct != null) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", badProduct, null), HttpStatus.BAD_REQUEST);
        }
        ReconMenuMaster createdMenu = createCustomMenu(req, status, submittedBy, approvedBy,
                userData.getUsername(), userData.getBankId());
        auditLogManagerService.commonAudit(userData, "Add MENU", createdMenu);
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS",
                "Y".equals(status) ? "Menu created and activated." : "Menu Created Successfully",
                java.util.Collections.singletonList(createdMenu)), HttpStatus.CREATED);
    }

    /**
     * Validates a custom Master/Main/Submenu request. Returns the rejection message, or null when
     * valid. Mirrors validateAgainstCatalog's shape but with its own rules — no catalog row is
     * ever consulted here.
     */
    private String validateCustomMenu(ReconMenuMasterDto req, Long bankId) {
        String name = req.getMenuName() == null ? "" : req.getMenuName().trim();
        if (name.isEmpty()) return "Menu name is required.";

        String menuType = req.getMenuType();
        Long parentId = req.getParentMenuId();
        Long productId = req.getProductId();

        if ("Master".equals(menuType)) {
            // Root of the hierarchy — no parent, never product-scoped (matches the existing
            // catalog convention: Master/Main rows always carry PRODUCT_ID = NULL).
            if (productId != null) return "A Master menu cannot be mapped to a specific product.";
        } else if ("Main".equals(menuType)) {
            if (parentId == null) return "A parent Master is required.";
            if (productId != null) return "A Main menu cannot be mapped to a specific product.";
            Optional<ReconMenuMaster> parentOpt = menuMasterRepository.findByMenuId(parentId);
            if (!parentOpt.isPresent() || !Objects.equals(parentOpt.get().getBankId(), bankId)) {
                return "Parent menu not found.";
            }
            if (!"Master".equals(parentOpt.get().getMenuType())) {
                return "Parent must be a Master menu.";
            }
        } else if ("Submenu".equals(menuType)) {
            if (parentId == null) return "A parent Main is required.";
            Optional<ReconMenuMaster> parentOpt = menuMasterRepository.findByMenuId(parentId);
            if (!parentOpt.isPresent() || !Objects.equals(parentOpt.get().getBankId(), bankId)) {
                return "Parent menu not found.";
            }
            if (!"Main".equals(parentOpt.get().getMenuType())) {
                return "Parent must be a Main menu.";
            }
            // A custom Submenu under an Extraction/Reconciliation Main is process-driven, exactly
            // like its catalog counterpart — the Admin picks a real RFD_FILE_ID/RPM_PROCESS_ID, and
            // the fixed-prefix URL is composed internally (see createCustomMenu), never typed. Every
            // other module still has no process mapping at all, so the URL IS its identity there.
            String customModule = resolveModuleByMasterId(parentOpt.get().getParentMenuId());
            if (customModule != null) {
                if (req.getMenuProcessId() == null) {
                    return "Select a Process for this custom " + customModule + " submenu.";
                }
                String dupProcess = validateProcessIdUnique(req.getMenuProcessId());
                if (dupProcess != null) return dupProcess;
            } else if (req.getMenuUrl() == null || req.getMenuUrl().trim().isEmpty()) {
                return "Custom sub menus must have a URL — they have no process mapping to derive one from.";
            }
            // Product is never user input, anywhere — a custom submenu has no catalog/process
            // hierarchy to auto-resolve one from (unlike a catalog Submenu, see resolveAutoProductId),
            // so it is always rejected here rather than silently dropped in createCustomMenu.
            if (productId != null) {
                return "Custom sub menus cannot be mapped to a product.";
            }
        } else {
            return "Unknown menu type.";
        }

        if (req.getMenuUrl() != null && !req.getMenuUrl().trim().isEmpty() && !isValidCustomUrl(req.getMenuUrl())) {
            return "URL must start with '/' and contain no spaces (e.g. /reports).";
        }

        // Duplicate guard — same parent + name (case-insensitive) + bank + product, regardless of
        // the existing row's status (ACTIVE/DRAFT/REJECTED/INACTIVE all count), so nobody ends up
        // with three "Reports" under the same parent.
        if (!menuMasterRepository.findByParentMenuIdAndMenuNameIgnoreCaseAndBankIdAndProductId(
                parentId, name, bankId, productId).isEmpty()) {
            return "A menu named '" + name + "' already exists under this parent.";
        }
        return null;
    }

    /**
     * Builds and saves a custom menu row directly from the request — no catalog lookup. Assigns a
     * generated SYSTEM_MENU_CODE once the row's MENU_ID is known (post-insert), so a custom menu
     * still carries a permanent identity, just not a catalog-shared one.
     */
    private ReconMenuMaster createCustomMenu(ReconMenuMasterDto req, String status, String submittedBy,
                                              String approvedBy, String createdBy, Long bankId) {
        ReconMenuMaster m = new ReconMenuMaster();
        m.setMenuType(req.getMenuType());
        m.setMenuName(req.getMenuName().trim());
        m.setMenuDescription(req.getMenuDescription());
        m.setParentMenuId(req.getParentMenuId());
        m.setSubMenu("N");

        // A custom Submenu under an Extraction/Reconciliation Main is process-driven — the fixed
        // prefix + the chosen id is composed here, server-side, exactly like the catalog path;
        // req.getMenuUrl() is never trusted for this case (validateCustomMenu already required
        // menuProcessId instead of a manual URL for it). Every other module keeps the existing
        // free-URL behavior untouched.
        String customModule = "Submenu".equals(req.getMenuType())
                ? resolveModuleByMasterId(menuMasterRepository.findByMenuId(req.getParentMenuId())
                        .map(ReconMenuMaster::getParentMenuId).orElse(null))
                : null;
        if (customModule != null) {
            Long processId = req.getMenuProcessId();
            String prefix = "Extraction".equals(customModule) ? EXTRACTION_URL_PREFIX : RECONCILIATION_URL_PREFIX;
            m.setMenuUrl(prefix + processId);
            m.setMenuProcessId(processId);
            m.setProcessType(customModule.toUpperCase());
        } else {
            m.setMenuUrl(req.getMenuUrl() != null ? req.getMenuUrl().trim() : null);
        }
        m.setStatus(status);
        m.setSubmittedBy(submittedBy);
        m.setApprovedBy(approvedBy);
        m.setBankId(bankId);
        // Product is never user input for a custom menu — validateCustomMenu already rejects a
        // non-null req.getProductId() before this is reached, so this is always null in practice;
        // set explicitly (rather than relying on that) so the row's intent reads clearly here too.
        m.setProductId(null);
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        m.setMenuSource(MENU_SOURCE_CUSTOM);
        m.setIsClickable(m.getMenuUrl() != null && !m.getMenuUrl().trim().isEmpty() ? "Y" : "N");
        // SYSTEM_MENU_CODE is NOT NULL at the DB level, so the first insert needs a placeholder —
        // MENU_ID isn't known until after this save (sequence-generated). Overwritten with the
        // real, permanent "CUS_<bankId>_<menuId>" code immediately below.
        m.setSystemMenuCode("CUS_PENDING_" + System.nanoTime());
        menuMasterRepository.save(m);

        m.setSystemMenuCode("CUS_" + bankId + "_" + m.getMenuId());
        menuMasterRepository.save(m);

        logger.info("Custom {} '{}' created for bankId={}: {}", m.getMenuType(), m.getMenuName(), bankId, m.getSystemMenuCode());
        return m;
    }

    private String validateAgainstCatalog(ReconMenuMasterDto req) {
        String name = req.getMenuName() == null ? "" : req.getMenuName().trim();
        if (name.isEmpty()) return "Menu name is required.";

        if ("Submenu".equals(req.getMenuType())) {
            Optional<ReconMenuMaster> catalogOpt = menuMasterRepository
                    .findCatalogSubmenu(name, req.getParentMenuCode(), req.getMasterMenuParent());
            if (!catalogOpt.isPresent()) {
                return "'" + name + "' is not a registered sub-menu under "
                        + req.getMasterMenuParent() + " > " + req.getParentMenuCode()
                        + ". Contact your administrator to register it first.";
            }
            // A catalog sub-menu that already carries a URL is a fixed-route page: the route is
            // owned by the catalog, so no process/template mapping is required. One without a URL
            // is process-driven — its URL can only be derived from a process, so that is mandatory.
            String catalogUrl = catalogOpt.get().getMenuUrl();
            boolean fixedUrl = catalogUrl != null && !catalogUrl.trim().isEmpty();
            if (!fixedUrl && req.getMenuProcessId() == null) {
                return "A sub-menu must be mapped to a process/template before it can be created.";
            }
            // Product is never client input (see resolveAutoProductId, called after this method
            // returns) — no guard needed here; the resolver itself only ever returns non-null for
            // Extraction/Reconciliation.
            return null;
        }

        // Master and Main are never product-scoped, in any module — matches the existing catalog
        // convention (Master/Main rows always carry PRODUCT_ID = NULL) and the equivalent guard on
        // the custom-menu path (validateCustomMenu).
        if (req.getProductId() != null) {
            return "A " + req.getMenuType() + " menu cannot be mapped to a specific product.";
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
    /**
     * B3 — duplicate / reactivate check, keyed on the PERMANENT identity, never on the display name.
     *
     * A logical menu may legitimately be mapped many times over — once per product, and for a
     * process-driven menu once per process. So the key is:
     *      process-driven : BANK_ID + PRODUCT_ID + SYSTEM_MENU_CODE + PROCESS_ID
     *      fixed-URL      : BANK_ID + PRODUCT_ID + SYSTEM_MENU_CODE      (PROCESS_ID is null)
     *
     * Outcomes:
     *   live mapping exists      -> DUPLICATE_MENU_MAPPED (the UI asks to confirm; force=true proceeds)
     *   only a dead mapping      -> REUSABLE_MENU_MAPPED  (offer to reactivate rather than pile up
     *                               another row); force=true reactivates it in place
     *   nothing                  -> null, meaning "go ahead and append"
     *
     * @param statusOnReactivate the status a reactivated row should return to — DRAFT for a Maker's
     *                           append (it must go through the Checker again), 'Y' for an Admin's.
     */
    private ResponseEntity<RestWithStatusList> resolveExistingMapping(ReconMenuMasterDto req, Long bankId,
                                                                      String actor, String statusOnReactivate,
                                                                      boolean force) {
        String code = resolveCatalogCode(req);
        if (code == null || bankId == null) return null;

        List<ReconMenuMaster> sameKey = menuMasterRepository.findBankMappingsByCode(bankId, code).stream()
                .filter(m -> Objects.equals(m.getProductId(), req.getProductId()))
                .filter(m -> Objects.equals(m.getMenuProcessId(), req.getMenuProcessId()))
                .collect(Collectors.toList());
        if (sameKey.isEmpty()) return null;

        ReconMenuMaster live = sameKey.stream().filter(m -> !isDeadStatus(m.getStatus())).findFirst().orElse(null);
        ReconMenuMaster dead = sameKey.stream().filter(m -> isDeadStatus(m.getStatus())).findFirst().orElse(null);

        if (live != null) {
            if (!force) {
                return ResponseEntity.ok(new RestWithStatusList("DUPLICATE_MENU_MAPPED",
                        "This submenu is already mapped for this Product and Process. Do you still want to continue?",
                        Collections.singletonList(live)));
            }
            // Confirmed — but there is nothing to add. This exact mapping (bank + product + code +
            // process) already exists and is live, and UX_MENU_BANK_PRODUCT_CODE rightly forbids a
            // second copy of it. So confirming is a no-op that hands back the mapping they already
            // have, rather than an insert that would die on the unique index.
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                    "This submenu is already mapped for this Product and Process — nothing to add.",
                    Collections.singletonList(live)));
        }

        // Only a dead (inactive / rejected) mapping: reuse it instead of creating duplicate history.
        if (dead != null) {
            if (!force) {
                return ResponseEntity.ok(new RestWithStatusList("REUSABLE_MENU_MAPPED",
                        "An inactive mapping already exists for this Product and Process. Reactivate it instead of creating a new one?",
                        Collections.singletonList(dead)));
            }
            dead.setStatus(statusOnReactivate);
            dead.setModifiedBy(actor);
            dead.setModifiedDate(new java.sql.Timestamp(System.currentTimeMillis()));
            menuMasterRepository.save(dead);
            logger.info("Reactivated existing menu mapping {} (code={}, bank={})", dead.getMenuId(), code, bankId);
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Existing mapping reactivated.",
                    Collections.singletonList(dead)));
        }
        return null;
    }

    static final String CATALOG_IMMUTABLE =
            "This is a system catalog menu. It is validation metadata and cannot be edited, deleted or approved.";

    // MENU_SOURCE values — avoids stringly-typed literals scattered across custom-menu logic.
    private static final String MENU_SOURCE_CATALOG = "CATALOG";
    private static final String MENU_SOURCE_CUSTOM = "CUSTOM";

    // Relaxed on purpose: only "starts with / and contains no whitespace" — a stricter charset
    // regex would reject legitimate routes with params or query strings (/reports/view/:id,
    // /reports?type=daily).
    private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile("^/\\S*$");

    private boolean isValidCustomUrl(String url) {
        return url != null && URL_PATTERN.matcher(url.trim()).matches();
    }

    // Same fixed route pattern the catalog Extraction/Reconciliation submenus already use
    // (previously composed client-side) — the Process id is never user-typed into this, it's
    // always appended internally once a real RFD_FILE_ID / RPM_PROCESS_ID has been picked.
    private static final String EXTRACTION_URL_PREFIX = "/extraction/fileProcessing.extr?processid=";
    private static final String RECONCILIATION_URL_PREFIX = "/reconciliation/fileProcessing.extr?processid=";

    /**
     * Resolves a custom Main's module ("Extraction"/"Reconciliation"/null) by walking up to its
     * own parent Master's name — a custom Main never carries masterMenuParent (that's catalog-only
     * wire), so this is the only way to know which module a custom Submenu's parent belongs to.
     */
    private String resolveModuleByMasterId(Long masterId) {
        if (masterId == null) return null;
        return menuMasterRepository.findByMenuId(masterId)
                .map(ReconMenuMaster::getMenuName)
                .filter(nm -> "Extraction".equals(nm) || "Reconciliation".equals(nm))
                .orElse(null);
    }

    /**
     * B5-style write-side guard, but system-wide rather than per-bank: one MENU_PROCESS_ID
     * (RFD_FILE_ID / RPM_PROCESS_ID) may back only one live menu — catalog or custom. Returns the
     * rejection message, or null when the id is free.
     */
    private String validateProcessIdUnique(Long menuProcessId) {
        if (menuProcessId == null) return null;
        boolean clash = menuMasterRepository.findByMenuProcessId(menuProcessId).stream()
                .anyMatch(m -> !isDeadStatus(m.getStatus()));
        return clash ? "This Process is already mapped to another menu. Please select a different Process." : null;
    }

    /** Catalog rows own no institution. They are the register Add Menu validates against — never
     *  business data, so no screen may edit / delete / approve / reject them. */
    private boolean isCatalogRow(ReconMenuMaster m) {
        return m != null && m.getBankId() == null && "CATALOG".equals(m.getCreatedBy());
    }

    /**
     * B5, write side — a menu may only ever be mapped to a product the institution actually holds.
     *
     * The read side already hides an out-of-scope mapping, but letting one be created at all leaves
     * a row that nobody can see and nothing will ever clean up. Reject it at the door instead.
     * Returns the rejection message, or null when the product is fine.
     */
    private String validateProductForBank(Long productId, Long bankId) {
        if (productId == null) return null; // platform menu — no product restriction
        if (bankId == null) return null;    // KAL_ADMIN has no institution to check against
        if (!resolveBankProductScope(bankId).contains(productId)) {
            return "Your institution has not purchased this product (or its subscription is no longer active), "
                 + "so a menu cannot be mapped to it.";
        }
        return null;
    }

    /** B5 — the products a bank has actually purchased and that are still active. */
    private Set<Long> resolveBankProductScope(Long bankId) {
        if (bankId == null) return Collections.emptySet();
        return bankProductMapRepository.findByBankIdAndStatus(bankId, "ACTIVE").stream()
                .map(com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap::getProductId)
                .collect(Collectors.toSet());
    }

    /**
     * Auto-resolves PRODUCT_ID for an Extraction/Reconciliation Submenu — Product is never asked
     * of the Admin; it is derived from the file/process the request already selected (today's
     * unchanged Process Type + Template Name pickers). Matches that file/process's own NAME
     * against RECON_PRODUCT_MASTER (longest product name wins on overlap, e.g. a name containing
     * both "NEFT" and a longer product name). Deliberately reads only the existing NAME column on
     * RCN_FILE_DTL_MAST / RCN_PROCESS_DEF_MAST — no schema change to either table.
     * Every other module (Configuration, Reports, Process, Dispute, UPI/NEFT dashboards, My
     * Organization, Administration) always resolves to null here, since none of those pass the
     * masterName check below.
     */
    private Long resolveAutoProductId(ReconMenuMasterDto req) {
        if (!"Submenu".equals(req.getMenuType())) return null;
        String masterName = req.getMasterMenuParent();
        if (!"Extraction".equals(masterName) && !"Reconciliation".equals(masterName)) return null;
        if (req.getMenuProcessId() == null) return null;

        String subjectName;
        if ("EXTRACTION".equals(req.getProcessType())) {
            ReconFileDetailsMaster f = fileDetailsMasterRepository.findByReconFileId(req.getMenuProcessId());
            subjectName = f != null ? f.getReconFileName() : null;
        } else if ("RECONCILIATION".equals(req.getProcessType())) {
            ReconProcessDefMaster p = processDefMasterRepository.findByReconProcessId(req.getMenuProcessId());
            subjectName = p != null ? p.getReconProcessName() : null;
        } else {
            return null;
        }
        if (subjectName == null || subjectName.trim().isEmpty()) return null;

        String upper = subjectName.toUpperCase();
        return productMasterRepository.findByStatus("ACTIVE").stream()
                .filter(pm -> upper.contains(pm.getProductName().toUpperCase()))
                .max(Comparator.comparingInt(pm -> pm.getProductName().length()))
                .map(ReconProductMaster::getProductId)
                .orElse(null);
    }

    /** B5 — a menu mapped to a product the scope doesn't include must never be returned. A NULL
     *  PRODUCT_ID is a platform menu (My Organization / Administration): unrestricted. */
    private boolean isInProductScope(ReconMenuMaster m, Set<Long> scope) {
        return m.getProductId() == null || scope.contains(m.getProductId());
    }

    /** A mapping that no longer counts as in-use — safe to reactivate rather than duplicate. */
    private boolean isDeadStatus(String status) {
        return "N".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status);
    }

    /** The catalog identity behind a request: trust the code when the client sent it, otherwise
     *  resolve it once from the catalog by hierarchy. Never trust a display name as identity. */
    private String resolveCatalogCode(ReconMenuMasterDto req) {
        if (req.getSystemMenuCode() != null && !req.getSystemMenuCode().trim().isEmpty()) {
            return req.getSystemMenuCode().trim();
        }
        Optional<ReconMenuMaster> catalog = "Submenu".equals(req.getMenuType())
                ? menuMasterRepository.findCatalogSubmenu(req.getMenuName(), req.getParentMenuCode(), req.getMasterMenuParent())
                : menuMasterRepository.findCatalogByNameAndType(req.getMenuName(), req.getMenuType());
        return catalog.map(ReconMenuMaster::getSystemMenuCode).orElse(null);
    }

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
        copy.setSystemMenuCode(c.getSystemMenuCode());
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
        // Resolved against the NEW bank's scope, not the catalog's — materialiseCatalogParents
        // copies the Master before the Main, so by the time the Main is copied, this bank's own
        // just-copied Master row already exists and is what this should point at.
        copy.setParentMenuId(resolveParentMenuId(c.getMenuType(), c.getParentMenuCode(), bankId));
        copy.setMenuSource(MENU_SOURCE_CATALOG);
        copy.setIsClickable(copy.getMenuUrl() != null && !copy.getMenuUrl().trim().isEmpty() ? "Y" : "N");
        menuMasterRepository.save(copy);
        logger.info("Materialised catalog {} '{}' for bankId={}", menuType, menuName, bankId);
    }

    // Internal, additive mirror of parentMenuCode — resolves the DIRECT parent's MENU_ID (one
    // level up only; masterMenuParent, the grandparent's name, is not an input here, matching
    // TWIN_OF_MENU_ID's single-level precedent). Never throws; returns null if unresolvable —
    // parentMenuCode/masterMenuParent remain authoritative, this is a best-effort mirror only.
    // See sql/menu_parent_id_migration.sql.
    private Long resolveParentMenuId(String menuType, String parentMenuCode, Long bankId) {
        if ("Master".equals(menuType) || parentMenuCode == null) return null;
        String parentType = "Main".equals(menuType) ? "Master" : "Main";
        Optional<ReconMenuMaster> byName = menuMasterRepository
                .findAllByMenuNameAndMenuType(parentMenuCode, parentType).stream()
                .filter(p -> "Y".equals(p.getStatus()))
                .filter(p -> Objects.equals(p.getBankId(), bankId))
                .filter(p -> bankId != null || !"CATALOG".equals(p.getCreatedBy()))
                .findFirst();
        if (byName.isPresent()) return byName.get().getMenuId();
        if ("Main".equals(menuType)) {
            try { return Long.parseLong(parentMenuCode); } catch (NumberFormatException ignored) {
                // parentMenuCode isn't numeric — not a legacy ID-based row, nothing to fall back to
            }
        }
        return null;
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String createdBy, Long bankId) {
        return createMenu(req, "DRAFT", null, null, createdBy, bankId);
    }

    private ReconMenuMaster createMenu(ReconMenuMasterDto req, String status, String submittedBy, String approvedBy,
                                       String createdBy, Long bankId) {
        // The catalog row this append is derived from. It is the authoritative source of the
        // immutable metadata (identity code, and the route of a fixed-URL page) — never the client.
        ReconMenuMaster catalog = "Submenu".equals(req.getMenuType())
                ? menuMasterRepository.findCatalogSubmenu(req.getMenuName(), req.getParentMenuCode(), req.getMasterMenuParent()).orElse(null)
                : menuMasterRepository.findCatalogByNameAndType(req.getMenuName(), req.getMenuType()).orElse(null);

        ReconMenuMaster m = new ReconMenuMaster();
        m.setMenuType(req.getMenuType());
        m.setMenuName(req.getMenuName());
        m.setMenuDescription(req.getMenuDescription());
        m.setParentMenuCode(req.getParentMenuCode());
        m.setSubMenu(req.getSubMenuReq());
        m.setMasterMenuParent(req.getMasterMenuParent());
        m.setParentMenuId(resolveParentMenuId(req.getMenuType(), req.getParentMenuCode(), bankId));
        m.setMenuProcessId(req.getMenuProcessId());
        // Identity is inherited from the catalog, never taken from the request.
        if (catalog != null) m.setSystemMenuCode(catalog.getSystemMenuCode());
        // A fixed-route page takes its URL from the catalog; a process-driven one from the request
        // (where it was derived from the chosen template).
        String catalogUrl = catalog != null ? catalog.getMenuUrl() : null;
        boolean fixedUrl = catalogUrl != null && !catalogUrl.trim().isEmpty();
        m.setMenuUrl(fixedUrl ? catalogUrl : req.getMenuUrl());
        m.setProcessType(fixedUrl ? catalog.getProcessType() : req.getProcessType());
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
        m.setMenuSource(MENU_SOURCE_CATALOG);
        m.setIsClickable(m.getMenuUrl() != null && !m.getMenuUrl().trim().isEmpty() ? "Y" : "N");
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
        // B5 — product isolation on the sidebar. A granted menu still only reaches the user if its
        // product is one the institution actually holds AND (when the role itself is product-scoped)
        // one the role is scoped to. A menu with no product is a platform menu: unrestricted.
        // Granting alone is never enough — this is what stops, say, NEFT menus surfacing for a bank
        // that only bought UPI.
        Set<Long> roleScope = resolveProductScope(roleId);
        List<ReconMenuMaster> mapped = menuMasterRepository.findAllById(menuIds).stream()
                .filter(m -> "Y".equals(m.getStatus()))
                .filter(m -> isInProductScope(m, resolveBankProductScope(m.getBankId())))
                .filter(m -> m.getProductId() == null || roleScope.isEmpty() || roleScope.contains(m.getProductId()))
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
            // A custom Submenu (Add Menu's custom path) never sets parentMenuCode at all — only
            // parentMenuId — so the gate must accept either, not just parentMenuCode, or a custom
            // Submenu's Main is never pulled in and it stays orphaned.
            if (!"Submenu".equals(m.getMenuType()) || (m.getParentMenuCode() == null && m.getParentMenuId() == null)) continue;
            // Prefer the numeric pointer when present — an exact PK lookup, strictly more
            // precise than name+bankId, and returns the same row that lookup would for any
            // correctly-backfilled row (see sql/menu_parent_id_migration.sql). Falls back to the
            // original name-based lookup, unchanged, when parentMenuId happens to be absent.
            ReconMenuMaster main = m.getParentMenuId() != null
                    ? menuMasterRepository.findByMenuId(m.getParentMenuId()).orElse(null)
                    : menuMasterRepository.findByMenuNameAndBankId(m.getParentMenuCode(), m.getBankId());
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
        // Separate cache keyed by MENU_ID for custom Mains (parentMenuCode null) — a custom
        // Main's parent Master is an exact PK lookup, no name ambiguity possible, so it doesn't
        // need the same per-name duplicate-collapsing treatment as the catalog path below.
        Map<Long, ReconMenuMaster> parentCacheById = new LinkedHashMap<>();
        for (ReconMenuMaster m : effective) {
            if (!"Main".equals(m.getMenuType())) continue;
            if (m.getParentMenuCode() == null) {
                // Custom Main — only the numeric pointer was ever set.
                if (m.getParentMenuId() == null) continue;
                ReconMenuMaster parent = parentCacheById.get(m.getParentMenuId());
                if (parent == null && !parentCacheById.containsKey(m.getParentMenuId())) {
                    parent = menuMasterRepository.findByMenuId(m.getParentMenuId()).orElse(null);
                    parentCacheById.put(m.getParentMenuId(), parent);
                }
                if (parent != null && "Y".equals(parent.getStatus())) byId.putIfAbsent(parent.getMenuId(), parent);
                continue;
            }
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
        if (isCatalogRow(existing)) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", CATALOG_IMMUTABLE, null), HttpStatus.FORBIDDEN);
        }
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
    @Transactional
    public ResponseEntity<RestWithStatusList> approveMenu(Long menuId, String approvedBy) {
        Optional<ReconMenuMaster> opt = menuMasterRepository.findByMenuId(menuId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", "Menu not found with ID: " + menuId, null), HttpStatus.NOT_FOUND);
        }
        ReconMenuMaster existing = opt.get();
        // B6 — a Checker only ever decides on an appended bank row; catalog rows are metadata.
        if (isCatalogRow(existing)) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE", CATALOG_IMMUTABLE, null), HttpStatus.FORBIDDEN);
        }
        // B7 — the mapping is only meaningful while the catalog entry it was derived from still
        // exists. If it was removed/migrated away, activating this row would create an orphan.
        if (existing.getSystemMenuCode() != null
                && !menuMasterRepository.findCatalogByCode(existing.getSystemMenuCode()).isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                    "This menu references a system catalog entry (" + existing.getSystemMenuCode()
                    + ") that no longer exists. It cannot be approved — re-register it in the catalog first.",
                    null), HttpStatus.CONFLICT);
        }
        // Only a menu still awaiting a Checker's decision can be approved. Guards the sequential
        // race (a second Checker approving something already decided) as a clear rejection instead
        // of a silent duplicate approval/email.
        if (!"PENDING".equals(existing.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                    "This request has already been processed.", null), HttpStatus.CONFLICT);
        }
        // Atomic claim: if another Checker's decision landed between our read above and this write,
        // rowsUpdated is 0 and we must not proceed (first completed decision always wins).
        int rowsUpdated = menuMasterRepository.compareAndSetStatus(menuId, "PENDING", "Y");
        if (rowsUpdated == 0) {
            return new ResponseEntity<>(new RestWithStatusList("FAILURE",
                    "This request has already been processed.", null), HttpStatus.CONFLICT);
        }
        existing = menuMasterRepository.findByMenuId(menuId).orElse(existing);
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
    public ResponseEntity<RestWithStatusList> getMenusVisibleTo(String username) {
        Optional<ReconUser> callerOpt = hierarchyScopeService.caller(username);
        if (!callerOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched.", new ArrayList<>()), HttpStatus.OK);
        }
        ReconUser caller = callerOpt.get();

        // An Admin owns the institution's menu tree.
        if (hierarchyScopeService.isAdmin(caller)) {
            return getMenusByBankId(caller.getBankId());
        }

        // Everyone else sees only what they, or their own subtree, registered. A parent's menus are
        // never exposed — Menu List offers edit/submit actions on what it shows.
        Set<String> mine = new HashSet<>();
        mine.add(caller.getUsername());
        for (ReconUser d : reconUserRepository.findAllById(hierarchyScopeService.descendantUserIds(caller.getUserId()))) {
            if (d.getUsername() != null) mine.add(d.getUsername());
        }

        ResponseEntity<RestWithStatusList> bankMenus = getMenusByBankId(caller.getBankId());
        List<?> data = bankMenus.getBody() != null && bankMenus.getBody().getData() != null
                ? bankMenus.getBody().getData() : Collections.emptyList();
        List<Object> owned = data.stream()
                .filter(ReconMenuMaster.class::isInstance)
                .map(ReconMenuMaster.class::cast)
                .filter(m -> mine.contains(m.getCreatedBy()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched.", owned), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllMenusByBankId(Long bankId) {
        // Menu List's own view: every appended Master/Main/Submenu this bank has ever created,
        // regardless of whether it's been mapped to a URL/process yet. Deliberately does NOT call
        // getMenusByBankId or reuse its hide-until-mapped rule (resolveUnmappedHiddenNames /
        // resolveCustomHiddenIds) — that rule exists for the Privilege tree and Sidebar, which are
        // both untouched here. Portal-twin rows are still excluded: those are an internal
        // implementation detail no Admin ever manages directly, not a "no mapping yet" case.
        // Bootstrap admin menus (Dashboard / My Organization / Administration and everything under
        // them — see SystemMenuCodes) are auto-created at onboarding, not something an Admin ever
        // manages from Menu List either, so they're excluded the same way.
        List<ReconMenuMaster> menus = menuMasterRepository.findByBankId(bankId);
        List<ReconMenuMaster> visible = menus.stream()
                .filter(m -> !"Y".equals(m.getIsPortalTwin()))
                .filter(m -> !com.jpb.reconciliation.reconciliation.constants.SystemMenuCodes.isBootstrapCode(m.getSystemMenuCode()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched for bank.", new ArrayList<>(visible)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllMenusVisibleTo(String username) {
        Optional<ReconUser> callerOpt = hierarchyScopeService.caller(username);
        if (!callerOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched.", new ArrayList<>()), HttpStatus.OK);
        }
        ReconUser caller = callerOpt.get();

        if (hierarchyScopeService.isAdmin(caller)) {
            return getAllMenusByBankId(caller.getBankId());
        }

        Set<String> mine = new HashSet<>();
        mine.add(caller.getUsername());
        for (ReconUser d : reconUserRepository.findAllById(hierarchyScopeService.descendantUserIds(caller.getUserId()))) {
            if (d.getUsername() != null) mine.add(d.getUsername());
        }

        ResponseEntity<RestWithStatusList> bankMenus = getAllMenusByBankId(caller.getBankId());
        List<?> data = bankMenus.getBody() != null && bankMenus.getBody().getData() != null
                ? bankMenus.getBody().getData() : Collections.emptyList();
        List<Object> owned = data.stream()
                .filter(ReconMenuMaster.class::isInstance)
                .map(ReconMenuMaster.class::cast)
                .filter(m -> mine.contains(m.getCreatedBy()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched.", owned), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMenusByBankId(Long bankId) {
        // Direct now that a menu row names its owner. This used to walk bank -> users -> roleIds
        // (plus roles those users created, minus roles already lent to another branch) purely to
        // reach the bank a menu belonged to. BANK_ID answers that in one hop, and a branch is its
        // own RECON_BANK_MASTER row so branches are covered too.
        // Only appended, bank-owned rows — catalog rows (BANK_ID NULL) can never reach the
        // privilege tree, so a catalog entry alone never makes anything visible.
        List<ReconMenuMaster> menus = menuMasterRepository.findByBankId(bankId);

        // B5 — product isolation: a mapping for a product this bank hasn't purchased (or whose
        // subscription is no longer ACTIVE) must never be offered. NULL product = platform menu.
        Set<Long> productScope = resolveBankProductScope(bankId);
        menus = menus.stream().filter(m -> isInProductScope(m, productScope)).collect(Collectors.toList());

        // /user-portal twins (see ReconRoleMasterServiceImpl.getOrCreateUserTwinMenu) are an
        // internal implementation detail resolved automatically at privilege-save time — never
        // something an Admin selects or manages directly, so hide them from this listing (used
        // by both the Menu Access Tree and Menu List) to avoid duplicate-looking rows.
        Set<String> hidden = resolveUnmappedHiddenNames(menus);
        // Custom rows (MENU_SOURCE='CUSTOM') are never touched by the name-based rule above —
        // they use their own recursive, URL/process-driven visibility rule instead. The two
        // operate on disjoint row populations (menuSource partitions them), so unioning the
        // results carries no risk to the existing catalog rule.
        Set<Long> hiddenCustomIds = resolveCustomHiddenIds(menus);
        List<ReconMenuMaster> visible = menus.stream()
                .filter(m -> !"Y".equals(m.getIsPortalTwin()))
                .filter(m -> !hidden.contains(m.getMenuName()))
                .filter(m -> !hiddenCustomIds.contains(m.getMenuId()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Menus fetched for bank.", new ArrayList<>(visible)), HttpStatus.OK);
    }

    /**
     * Recursive visibility rule for custom (MENU_SOURCE='CUSTOM') Master/Main/Submenu rows —
     * parallel to, never replacing, resolveUnmappedHiddenNames's catalog-only "hide until mapped"
     * rule above. A custom node is visible if it is itself clickable/process-bound, OR any of its
     * descendants (by PARENT_MENU_ID) is visible. Memoized so a Master with several Main children
     * is only ever evaluated once per node.
     */
    private Set<Long> resolveCustomHiddenIds(List<ReconMenuMaster> bankMenus) {
        List<ReconMenuMaster> customMenus = bankMenus.stream()
                .filter(m -> MENU_SOURCE_CUSTOM.equals(m.getMenuSource()))
                .collect(Collectors.toList());
        if (customMenus.isEmpty()) return Collections.emptySet();

        Map<Long, List<ReconMenuMaster>> childrenByParent = new HashMap<>();
        for (ReconMenuMaster m : customMenus) {
            if (m.getParentMenuId() != null) {
                childrenByParent.computeIfAbsent(m.getParentMenuId(), k -> new ArrayList<>()).add(m);
            }
        }

        Map<Long, Boolean> memo = new HashMap<>();
        Set<Long> hiddenIds = new HashSet<>();
        for (ReconMenuMaster m : customMenus) {
            if (!isCustomNodeVisible(m, childrenByParent, memo)) hiddenIds.add(m.getMenuId());
        }
        return hiddenIds;
    }

    private boolean isCustomNodeVisible(ReconMenuMaster m, Map<Long, List<ReconMenuMaster>> childrenByParent,
                                         Map<Long, Boolean> memo) {
        Boolean cached = memo.get(m.getMenuId());
        if (cached != null) return cached;
        boolean selfVisible = "Y".equals(m.getIsClickable()) || m.getMenuProcessId() != null;
        boolean visible = selfVisible || childrenByParent.getOrDefault(m.getMenuId(), Collections.emptyList())
                .stream().anyMatch(child -> isCustomNodeVisible(child, childrenByParent, memo));
        memo.put(m.getMenuId(), visible);
        return visible;
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

        // A sub-menu counts as live once its APPENDED bank row is actually usable — either it is
        // bound to a process (process-driven) or it carries a route (fixed-URL page). Note this is
        // evaluated over bank rows only: a catalog row, even one with a URL, never counts.
        Set<String> mainsWithAMappedSubmenu = bankMenus.stream()
                .filter(m -> "Submenu".equals(m.getMenuType()))
                .filter(m -> "Y".equals(m.getStatus()))
                .filter(m -> m.getMenuProcessId() != null
                          || (m.getMenuUrl() != null && !m.getMenuUrl().trim().isEmpty()))
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
