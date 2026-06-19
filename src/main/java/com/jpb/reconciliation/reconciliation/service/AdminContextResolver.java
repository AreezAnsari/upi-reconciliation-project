package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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

    /**
     * Returns true if the authenticated principal belongs to either the
     * BANK_ADMIN or BRANCH_ADMIN table.
     *
     * NOTE: We do NOT rely on Spring Security authorities here because the
     * JWT filter may resolve the principal via KalAdmin (which has null
     * authorities) before reaching the BranchAdmin lookup. Direct DB check
     * is the only reliable gate.
     */
    public boolean isAllowed(Authentication auth) {
        String principal = auth.getName();
        return findMainAdmin(principal) != null
            || branchAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(principal, "BLOCKED").isPresent()
            || branchAdminRepository.findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED").isPresent();
    }

    /**
     * Resolves bankCode, branchCode, and username for the logged-in admin.
     * Checks BANK_ADMIN table first, then BRANCH_ADMIN.
     * Throws if the principal cannot be matched to either table.
     */
    public AdminContext resolve(Authentication auth) {
        String principal = auth.getName();

        // Step 1: Bank Admin JWT subject = email — check MainAdmin by email first.
        // This must come before any BranchAdmin lookup to prevent a bank admin's
        // email from accidentally matching a BranchAdmin email record.
        Optional<MainAdmin> bankAdminByEmail = mainAdminRepository.findFirstByEmail(principal);
        if (bankAdminByEmail.isPresent()) {
            MainAdmin ba = bankAdminByEmail.get();
            return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
        }

        // Step 2: Branch Admin JWT subject = username — check BranchAdmin by username.
        Optional<BranchAdmin> branchByUsername = branchAdminRepository
                .findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED");
        if (branchByUsername.isPresent()) {
            BranchAdmin ba = branchByUsername.get();
            String bankCode = resolveBankCodeFromBranch(ba.getBranchCode());
            return new AdminContext(ba.getUsername(), bankCode, ba.getBranchCode());
        }

        // Step 3: MainAdmin by username (fallback)
        Optional<MainAdmin> bankAdminByUsername = mainAdminRepository.findFirstByUsername(principal);
        if (bankAdminByUsername.isPresent()) {
            MainAdmin ba = bankAdminByUsername.get();
            return new AdminContext(ba.getUsername(), ba.getBankCode(), null);
        }

        // Step 4: BranchAdmin by email (edge case fallback)
        Optional<BranchAdmin> branchByEmail = branchAdminRepository
                .findFirstByEmailAndStatusNotOrderByIdDesc(principal, "BLOCKED");
        if (branchByEmail.isPresent()) {
            BranchAdmin ba = branchByEmail.get();
            String bankCode = resolveBankCodeFromBranch(ba.getBranchCode());
            return new AdminContext(ba.getUsername(), bankCode, ba.getBranchCode());
        }

        throw new IllegalStateException(
                "Principal is not a Bank Admin or Branch Admin: " + principal);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private MainAdmin findMainAdmin(String principal) {
        Optional<MainAdmin> byEmail = mainAdminRepository.findFirstByEmail(principal);
        if (byEmail.isPresent()) return byEmail.get();
        return mainAdminRepository.findFirstByUsername(principal).orElse(null);
    }

    private BranchAdmin findBranchAdmin(String principal) {
        Optional<BranchAdmin> byEmail = branchAdminRepository.findFirstByEmailAndStatusNotOrderByIdDesc(principal, "BLOCKED");
        if (byEmail.isPresent()) return byEmail.get();
        return branchAdminRepository.findFirstByUsernameAndStatusNotOrderByIdDesc(principal, "BLOCKED").orElse(null);
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
