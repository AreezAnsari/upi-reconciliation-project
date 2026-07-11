package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface MenuMasterService {

    /** Updates a menu. Returns "APPLIED" when the change was saved (Admin-direct), "SUBMITTED"
     *  when a Maker's change was held for Checker approval instead, or "FAILED". */
    String updateMenu(ReconMenuMasterDto menuDto, String updatedBy);

    ResponseEntity<RestWithStatusList> getMenus(Long menuId);

    ResponseEntity<RestWithStatusList> getAllMenus();

    ResponseEntity<RestWithStatusList> getMenuByUserId(Long userId);

    /** force=true means the user saw (and accepted) the duplicate / reactivate prompt. */
    ResponseEntity<RestWithStatusList> addMenu(ReconMenuMasterDto menuRequest, UserDetails userDetails, boolean force);

    /** Admin-only: creates a Menu and activates it immediately (status 'Y'), skipping
     *  the DRAFT -> PENDING -> Checker-approval flow. Rejected if the caller isn't
     *  an Admin (KAL_ADMIN / BANK_ADMIN / BRANCH_ADMIN). */
    ResponseEntity<RestWithStatusList> addMenuActive(ReconMenuMasterDto menuRequest, UserDetails userDetails, boolean force);

    /** Menus belonging to a specific Bank/Branch — derived via RCN_RECON_USER's
     *  BANK_ID + ROLE_ID (same technique as ReconRoleMasterService.getRolesByBankId),
     *  then matched against RECON_MENU_MASTER.ROLE_ID (no BANK_ID column on Menu either). */
    ResponseEntity<RestWithStatusList> getMenusByBankId(Long bankId);

    /** Menus the caller may see in Menu List. Admin → their institution; anyone else → only the
     *  menus they or their own subtree created (never a parent's). Scope comes from the JWT. */
    ResponseEntity<RestWithStatusList> getMenusVisibleTo(String username);

    ResponseEntity<ResponseDto> removeMenu(Long menuId);

    ResponseEntity<RestWithStatusList> getMenuByRole(Long roleId);

    /** Menus this role can actually SEE (Sidebar navigation) — driven by C_ROLE_MENU_MAP
     *  (the same table PrivilegesAssign.jsx writes to), not RECON_MENU_MASTER.ROLE_ID
     *  directly. A menu existing in the catalog (Menu List) doesn't mean any role can see
     *  it in their Sidebar until it's explicitly attached here via Privileges. */
    ResponseEntity<RestWithStatusList> getMenusByRolePrivileges(Long roleId);

    Long getVerifiedRoleId(String username);

    ResponseEntity<RestWithStatusList> submitForApproval(Long menuId, String submittedBy);

    ResponseEntity<RestWithStatusList> approveMenu(Long menuId, String approvedBy);

    /** PENDING menus visible to this Checker — same bank/branch-only + product-scope
     *  overlap rule as ReconRoleMasterService.getPendingRolesForChecker. */
    ResponseEntity<RestWithStatusList> getPendingMenusForChecker(String checkerUsername);
}
