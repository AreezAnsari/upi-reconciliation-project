package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Who a caller is allowed to see in the Administration screens.
 *
 * A Bank/Branch Admin owns their whole institution, so they see all of it. Anyone else (a Maker,
 * a plain user) may only ever see the part of the tree BELOW them — their own descendants.
 *
 * This matters for more than tidiness: the Administration screens can replace, delegate and
 * re-assign users. If a child could see their own parent (or an Admin) in those lists, they could
 * swap that parent out and take their place — a privilege-escalation path. So a caller never sees
 * their ancestors, their siblings, or anyone else's branch of the tree.
 *
 * Scope is always derived from the authenticated caller, never from a request parameter, so it
 * cannot be widened by the client.
 */
@Service
public class HierarchyScopeService {

    private static final String CHECKER_MENU = "Checker Dashboard";

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private CRoleMenuMapRepository roleMenuMapRepository;
    @Autowired private MenuMasterRepository menuMasterRepository;

    public Optional<ReconUser> caller(String username) {
        return username == null ? Optional.empty() : reconUserRepository.findByUsername(username);
    }

    public boolean isAdmin(ReconUser user) {
        return user != null && UserConstants.isAdminUserType(user.getUserType());
    }

    /**
     * Every user below this one in the reporting tree (children, grandchildren, …). Excludes the
     * caller themselves — a user is not their own subordinate and must not be able to act on their
     * own record from these screens.
     */
    public Set<Long> descendantUserIds(Long rootUserId) {
        Set<Long> out = new HashSet<>();
        if (rootUserId == null) return out;

        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootUserId);
        while (!queue.isEmpty()) {
            Long parent = queue.poll();
            for (ReconUser child : reconUserRepository.findByParentUserId(parent)) {
                // Guards against a cycle in the data ever hanging the request.
                if (child.getUserId() != null && out.add(child.getUserId())) {
                    queue.add(child.getUserId());
                }
            }
        }
        return out;
    }

    /** The users a caller may see: their whole institution if an Admin, otherwise only their own subtree. */
    public List<ReconUser> visibleUsers(ReconUser caller) {
        if (caller == null) return Collections.emptyList();
        if (isAdmin(caller)) {
            return caller.getBankId() == null
                    ? reconUserRepository.findAll()                       // KAL_ADMIN — platform wide
                    : reconUserRepository.findByBankId(caller.getBankId());
        }
        Set<Long> ids = descendantUserIds(caller.getUserId());
        return ids.isEmpty() ? Collections.emptyList() : reconUserRepository.findAllById(ids);
    }

    /**
     * A Checker role is one that holds the Checker Dashboard privilege.
     *
     * A Maker must never even see a Checker role — not in a list, not in a role dropdown. Hiding it
     * is what keeps the segregation of duties real: a Maker who cannot see a Checker role cannot
     * assign one, hand one out, or hand themselves one.
     */
    public boolean isCheckerRole(Long roleId) {
        if (roleId == null) return false;
        List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        if (menuIds.isEmpty()) return false;
        return menuMasterRepository.findAllById(menuIds).stream()
                .map(ReconMenuMaster::getMenuName)
                .anyMatch(CHECKER_MENU::equals);
    }
}
