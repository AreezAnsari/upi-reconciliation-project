package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContextResolver {

    private final MainAdminRepository   mainAdminRepository;
    private final BranchAdminRepository branchAdminRepository;
    private final BranchBankRepository  branchBankRepository;
    private final MainBankRepository    mainBankRepository;
    private final AddUserRepository     addUserRepository;

    public boolean isAllowed(Authentication auth) {
        String role = extractRole(auth);
        if (role != null) {
            return "USER".equals(role) || "BANK_ADMIN".equals(role) || "BRANCH_ADMIN".equals(role);
        }
        // No role claim (old token / refresh / OAuth) — fall back to DB check
        String principal = auth.getName();
        Optional<AddUser> asUser = addUserRepository.findByUsername(principal);
        if (!asUser.isPresent()) asUser = addUserRepository.findByEmail(principal);
        if (asUser.isPresent() && asUser.get().getStatus() == AddUser.UserStatus.ACTIVE) return true;
        if (findMainAdmin(principal) != null) return true;
        if (branchAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(principal, "BLOCKED").isPresent()) return true;
        if (branchAdminRepository.findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED").isPresent()) return true;
        return false;
    }

    /**
     * Resolves bankCode, branchCode, and username for the logged-in principal.
     * JWT role claim routes to the exact table — no cross-table fallbacks.
     * Old tokens without a role claim use a sequential DB lookup (refresh / OAuth flows).
     */
    public AdminContext resolve(Authentication auth) {
        String principal = auth.getName();
        String role      = extractRole(auth);

        if ("USER".equals(role)) {
            Optional<AddUser> opt = addUserRepository.findByUsername(principal);
            if (!opt.isPresent()) {
                throw new IllegalStateException("REC_USER not found or inactive: " + principal);
            }
            AddUser.UserStatus st = opt.get().getStatus();
            // Allow transitional statuses — only hard-block INACTIVE / BLOCKED
            if (st == AddUser.UserStatus.INACTIVE || st == AddUser.UserStatus.BLOCKED) {
                throw new IllegalStateException("REC_USER not found or inactive: " + principal);
            }
            AddUser u = opt.get();
            log.info("[RESOLVE] USER {} (id={}) — bank: {}, branch: {}", u.getUsername(), u.getId(), u.getBankCode(), u.getBranchCode());
            return new AdminContext(u.getUsername(), u.getBankCode(), u.getBranchCode(), u.getId());
        }

        if ("BRANCH_ADMIN".equals(role)) {
            Optional<BranchAdmin> opt = branchAdminRepository
                    .findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED");
            if (!opt.isPresent()) {
                throw new IllegalStateException("BRANCH_ADMIN not found: " + principal);
            }
            BranchAdmin ba = opt.get();
            String bankCode = resolveBankCodeFromBranch(ba.getBranchCode());
            return new AdminContext(ba.getUsername(), bankCode, ba.getBranchCode());
        }

        if ("BANK_ADMIN".equals(role)) {
            // JWT subject may be email (OTP flow) or username (direct login)
            Optional<MainAdmin> byEmail = mainAdminRepository.findFirstByEmail(principal);
            if (byEmail.isPresent()) {
                MainAdmin ba = byEmail.get();
                return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
            }
            Optional<MainAdmin> byUsername = mainAdminRepository.findFirstByUsername(principal);
            if (byUsername.isPresent()) {
                MainAdmin ba = byUsername.get();
                return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
            }
            throw new IllegalStateException("BANK_ADMIN not found: " + principal);
        }

        // No role claim — old token (refresh / OAuth / KalAdmin). Use sequential DB lookup.
        log.warn("[RESOLVE] No role claim for principal='{}', falling back to DB lookup", principal);

        Optional<AddUser> userOpt = addUserRepository.findByUsername(principal);
        if (userOpt.isPresent() && userOpt.get().getStatus() == AddUser.UserStatus.ACTIVE) {
            AddUser u = userOpt.get();
            return new AdminContext(u.getUsername(), u.getBankCode(), u.getBranchCode(), u.getId());
        }
        Optional<MainAdmin> bankByEmail = mainAdminRepository.findFirstByEmail(principal);
        if (bankByEmail.isPresent()) {
            MainAdmin ba = bankByEmail.get();
            return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
        }
        Optional<BranchAdmin> branchByUsername = branchAdminRepository
                .findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED");
        if (branchByUsername.isPresent()) {
            BranchAdmin ba = branchByUsername.get();
            String bankCode = resolveBankCodeFromBranch(ba.getBranchCode());
            return new AdminContext(ba.getUsername(), bankCode, ba.getBranchCode());
        }
        Optional<MainAdmin> bankByUsername = mainAdminRepository.findFirstByUsername(principal);
        if (bankByUsername.isPresent()) {
            MainAdmin ba = bankByUsername.get();
            return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
        }

        throw new IllegalStateException(
                "Principal is not a Bank Admin, Branch Admin, or active User: " + principal);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private String extractRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return null;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String authority = a.getAuthority();
            if (authority != null && authority.startsWith("ROLE_")) {
                return authority.substring(5); // strip "ROLE_" prefix
            }
        }
        return null;
    }

    private MainAdmin findMainAdmin(String principal) {
        Optional<MainAdmin> byEmail = mainAdminRepository.findFirstByEmail(principal);
        if (byEmail.isPresent()) return byEmail.get();
        return mainAdminRepository.findFirstByUsername(principal).orElse(null);
    }

    private String resolveBankCodeFromBranch(String branchCode) {
        if (branchCode == null) return null;
        Optional<BranchBank> branch = branchBankRepository.findByBranchCode(branchCode);
        if (!branch.isPresent()) {
            log.warn("No BranchBank record found for branchCode={}", branchCode);
            return null;
        }
        Long parentBankId = branch.get().getParentBankId();
        if (parentBankId == null) {
            log.warn("BranchBank {} has null parentBankId", branchCode);
            return null;
        }
        return mainBankRepository.findById(parentBankId)
                .map(bank -> bank.getBankCode())
                .orElse(null);
    }
}
