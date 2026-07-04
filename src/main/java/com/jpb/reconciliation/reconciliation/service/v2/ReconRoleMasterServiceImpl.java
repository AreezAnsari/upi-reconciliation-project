package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleProductMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReconRoleMasterServiceImpl implements ReconRoleMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconRoleMasterServiceImpl.class);

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private RoleCodeGeneratorService roleCodeGeneratorService;

    @Autowired
    private ReconRoleProductMapRepository roleProductMapRepository;

    @Autowired
    private EmailService emailService;

    private void notifyMakerOfDecision(String submittedByUsername, String itemName, String itemCode, String decision, String decidedBy) {
        if (submittedByUsername == null) return;
        Optional<ReconUser> maker = reconUserRepository.findByUsername(submittedByUsername);
        if (!maker.isPresent()) return;
        emailService.sendWorkflowDecisionNotification(
                maker.get().getEmail(), maker.get().getFullName(),
                "Role", itemName, itemCode, decision, decidedBy);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRole(ReconRoleMaster role, String createdBy) {
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Role name is required.", null));
        }
        if (reconRoleMasterRepository.existsByRoleName(role.getRoleName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Role name already exists.", null));
        }
        // Role code is never typed by hand — auto-generated from ROLE_CODE_SEQ/ROLE_CODE_CUSTOM_SEQ
        // (same as the old backend). Reuses the code the Add Role form already reserved/showed the
        // user, unless it's gone stale (taken by another role since then).
        role.setRoleCode(resolveRoleCode(role.getRoleCode(), role.getRoleName()));
        role.setStatus("DRAFT");
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(createdBy);
        ReconRoleMaster saved = reconRoleMasterRepository.save(role);
        logger.info("ReconRoleMaster created: {} by {}", saved.getRoleCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Role created successfully.", Collections.singletonList(saved)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRoleActive(ReconRoleMaster role, String createdBy) {
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(createdBy);
        if (!actorOpt.isPresent() || !com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(actorOpt.get().getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("FAILURE", "Only a Bank/Branch/KAL Admin can create a Role that activates immediately.", null));
        }
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Role name is required.", null));
        }
        if (reconRoleMasterRepository.existsByRoleName(role.getRoleName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Role name already exists.", null));
        }
        role.setRoleCode(resolveRoleCode(role.getRoleCode(), role.getRoleName()));
        // Not ACTIVE yet — a role with zero privileges is unusable. It only flips to ACTIVE
        // once the Admin actually assigns at least one privilege (see savePrivileges below).
        role.setStatus("DRAFT");
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(createdBy);
        ReconRoleMaster saved = reconRoleMasterRepository.save(role);
        logger.info("ReconRoleMaster created by Admin (awaiting privileges): {} by {}", saved.getRoleCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Role created and activated.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleById(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByStatus(String status) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched by status.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByType(String roleType) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByRoleType(roleType);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched by type.", roles));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateRole(Long roleId, ReconRoleMaster role, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        if (role.getRoleName() != null) existing.setRoleName(role.getRoleName());
        if (role.getRoleDesc() != null) existing.setRoleDesc(role.getRoleDesc());
        if (role.getRoleType() != null) existing.setRoleType(role.getRoleType());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> submitForApproval(Long roleId, String submittedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        if (roleMenuMapRepository.findMenuIdsByRoleId(roleId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE",
                            "Assign at least one privilege to this role before submitting for approval.", null));
        }

        ReconRoleMaster existing = opt.get();
        existing.setStatus("PENDING");
        existing.setSubmittedBy(submittedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(submittedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role submitted for approval.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveRole(Long roleId, String approvedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus("ACTIVE");
        existing.setApprovedBy(approvedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(approvedBy);
        reconRoleMasterRepository.save(existing);
        notifyMakerOfDecision(existing.getSubmittedBy(), existing.getRoleName(), existing.getRoleCode(), "Approved", approvedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role approved and activated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long roleId, String status, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        String submittedBy = existing.getSubmittedBy();
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        if ("REJECTED".equalsIgnoreCase(status)) {
            notifyMakerOfDecision(submittedBy, existing.getRoleName(), existing.getRoleCode(), "Rejected", updatedBy);
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteRole(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkRoleCodeExists(String roleCode) {
        boolean exists = reconRoleMasterRepository.existsByRoleCode(roleCode);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPrivileges(Long roleId) {
        List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Privileges fetched.", menuIds));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> savePrivileges(Long roleId, List<Long> menuIds, String updatedBy) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }

        // Checker Dashboard and Maker-side menus (Maker Dashboard / Add Role / Add Menu / Add User)
        // are mutually exclusive — same SOD rule as product capability: a Checker can never also
        // be able to create records. Enforced here as the server-side source of truth; the
        // Privileges-assign screen also disables the opposing checkboxes for UX.
        if (menuIds != null) {
            Set<String> requestedNames = menuIds.stream()
                    .map(menuMasterRepository::findById)
                    .filter(Optional::isPresent)
                    .map(o -> o.get().getMenuName())
                    .collect(Collectors.toSet());
            boolean hasChecker = requestedNames.contains("Checker Dashboard");
            boolean hasMaker = requestedNames.contains("Maker Dashboard") || requestedNames.contains("Add Role")
                    || requestedNames.contains("Add Menu") || requestedNames.contains("Add User");
            if (hasChecker && hasMaker) {
                return ResponseEntity.badRequest().body(new RestWithStatusList("FAILURE",
                        "A role cannot have both Checker Dashboard and Maker-side privileges (Maker Dashboard / Add Role / Add Menu / Add User).",
                        null));
            }
        }

        roleMenuMapRepository.deleteByRoleId(roleId);

        if (menuIds != null) {
            for (Long menuId : menuIds) {
                Optional<ReconMenuMaster> menuOpt = menuMasterRepository.findById(menuId);
                if (!menuOpt.isPresent()) continue;
                CRoleMenuMap map = new CRoleMenuMap();
                // @EmbeddedId is never auto-instantiated by Hibernate on insert in this setup —
                // leaving it null makes CompositeNestedGeneratedValueGenerator NPE while trying to
                // reflectively populate id.menuId from the @MapsId association. Set it explicitly.
                map.setId(new CRoleMenuMap.RoleMenuMapId(roleId, menuId));
                map.setRole(roleOpt.get());
                map.setMenu(menuOpt.get());
                map.setCreatedAt(LocalDateTime.now());
                map.setCreatedBy(updatedBy);
                roleMenuMapRepository.save(map);
            }
        }

        // A Bank/Branch/KAL Admin never goes through maker-checker approval — the moment they
        // assign at least one privilege to a role they created, it goes straight ACTIVE. Until
        // then it stays DRAFT (a role with zero privileges is unusable, so it can't be ACTIVE).
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(updatedBy);
        boolean actorIsAdmin = actorOpt.isPresent()
                && com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(actorOpt.get().getUserType());
        if (actorIsAdmin && menuIds != null && !menuIds.isEmpty()) {
            ReconRoleMaster r = roleOpt.get();
            r.setStatus("ACTIVE");
            r.setSubmittedBy(updatedBy);
            r.setApprovedBy(updatedBy);
            r.setUpdatedAt(LocalDateTime.now());
            r.setUpdatedBy(updatedBy);
            reconRoleMasterRepository.save(r);
        }

        logger.info("Privileges saved for roleId={}: {} menu(s) by {}", roleId, menuIds == null ? 0 : menuIds.size(), updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Privileges saved.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByBankId(Long bankId) {
        List<ReconUser> bankUsers = reconUserRepository.findByBankId(bankId);

        List<Long> roleIds = bankUsers.stream()
                .map(ReconUser::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> usernames = bankUsers.stream()
                .map(ReconUser::getUsername).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<ReconRoleMaster> byUser = roleIds.isEmpty() ? Collections.emptyList() : reconRoleMasterRepository.findByRoleIdIn(roleIds);
        List<ReconRoleMaster> byCreator = usernames.isEmpty() ? Collections.emptyList() : reconRoleMasterRepository.findByCreatedByIn(usernames);

        // A role an admin created can already be assigned to users of a DIFFERENT bank/branch
        // (e.g. Bank Admin creates a role while onboarding a Branch). Such roles belong to that
        // branch's view, not this bank's — so drop anything from byCreator that is in use elsewhere.
        byCreator = byCreator.stream()
                .filter(r -> reconUserRepository.findByRoleId(r.getRoleId()).stream()
                        .allMatch(u -> bankId.equals(u.getBankId())))
                .collect(Collectors.toList());

        Map<Long, ReconRoleMaster> merged = new LinkedHashMap<>();
        byUser.forEach(r -> merged.put(r.getRoleId(), r));
        byCreator.forEach(r -> merged.put(r.getRoleId(), r));

        // Bootstrap admin roles (KAL_ADMIN, BANK_ADMIN_<code>, BRANCH_ADMIN_<code>) are system-level
        // and not something a Bank/Branch Admin manages from this screen — hide them from the list.
        List<ReconRoleMaster> visible = merged.values().stream()
                .filter(r -> !isSystemAdminRoleCode(r.getRoleCode()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched for bank.", visible));
    }

    // KAL_ADMIN (exact) and BANK_ADMIN_<code>/BRANCH_ADMIN_<code> (bootstrap prefix from
    // ReconBankMasterServiceImpl.createDefaultAdminMenus) are system roles, never user-created.
    private boolean isSystemAdminRoleCode(String roleCode) {
        if (roleCode == null) return false;
        return roleCode.equals("KAL_ADMIN")
                || roleCode.startsWith("BANK_ADMIN_")
                || roleCode.startsWith("BRANCH_ADMIN_");
    }

    // Reuses the code the Add Role form already reserved (via /generate-code) and displayed to
    // the user as a disabled field, as long as it's still unused. If it went stale (someone else
    // took it, or none was sent), a fresh one is generated — mirrors the old backend's
    // peekNextCode()/generateNextCode() fallback pattern.
    private String resolveRoleCode(String reservedCode, String roleName) {
        if (reservedCode != null && !reservedCode.trim().isEmpty()
                && !reconRoleMasterRepository.existsByRoleCode(reservedCode.trim())) {
            return reservedCode.trim();
        }
        return roleCodeGeneratorService.generateNextCode(roleName);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleProducts(Long roleId) {
        List<Long> productIds = roleProductMapRepository.findProductIdsByRoleId(roleId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role products fetched.", productIds));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> saveRoleProducts(Long roleId, List<Long> productIds, String updatedBy) {
        if (!reconRoleMasterRepository.existsById(roleId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        roleProductMapRepository.deleteByRoleId(roleId);
        if (productIds != null) {
            for (Long productId : productIds) {
                ReconRoleProductMap map = new ReconRoleProductMap();
                map.setRoleId(roleId);
                map.setProductId(productId);
                map.setCreatedAt(LocalDateTime.now());
                map.setCreatedBy(updatedBy);
                roleProductMapRepository.save(map);
            }
        }
        logger.info("Products saved for roleId={}: {} product(s) by {}", roleId, productIds == null ? 0 : productIds.size(), updatedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Products saved.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> saveRoleBankTypeScope(Long roleId, String bankTypeScope, String updatedBy) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster role = roleOpt.get();
        role.setBankTypeScope(bankTypeScope != null && !bankTypeScope.trim().isEmpty() ? bankTypeScope.trim() : null);
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(role);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank type scope saved.", null));
    }
}
