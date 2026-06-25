package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;

@Repository
public interface BranchAdminRepository extends JpaRepository<BranchAdmin, Long> {

    Optional<BranchAdmin> findByBranchCodeAndUsername(String branchCode, String username);

    boolean existsByBranchCodeAndUsername(String branchCode, String username);

    // Email uniqueness check — used before creating a replacement admin
    boolean existsByBranchCodeAndEmail(String branchCode, String email);

    // Global email check — used to prevent replacement with an already-registered admin email
    boolean existsByEmail(String email);

    Optional<BranchAdmin> findFirstByEmail(String email);

    Optional<BranchAdmin> findFirstByEmailOrderByIdAsc(String email);

    // Re-onboarding safe variants — skip BLOCKED records so the active admin is returned.
    // When the same email exists in both a BLOCKED and a fresh record, these return the
    // non-BLOCKED one (newest first = highest ID, which is the re-onboarded record).
    Optional<BranchAdmin> findFirstByEmailAndStatusNot(String email, String status);
    Optional<BranchAdmin> findFirstByEmailAndStatusNotOrderByIdDesc(String email, String status);

    Optional<BranchAdmin> findFirstByUsername(String username);
    Optional<BranchAdmin> findFirstByUsernameAndStatusNotOrderByIdDesc(String username, String status);

    // Scheduler: auto-inactivate INACTIVE_PENDING whose window has passed
    List<BranchAdmin> findByStatusAndInactivateScheduledAtBefore(String status, LocalDateTime cutoff);

    // Scheduler: auto-reactivate ACTIVE_PENDING whose window has passed
    List<BranchAdmin> findByStatusAndReactivateScheduledAtBefore(String status, LocalDateTime cutoff);

    // Scheduler: auto-block BLOCK_PENDING whose window has passed
    List<BranchAdmin> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

    // Quick existence check — used as scheduler pre-check to avoid full queries when nothing is pending
    boolean existsByStatusIn(Collection<String> statuses);

    // All admins for a branch (current + INACTIVE replaced originals)
    List<BranchAdmin> findByBranchCode(String branchCode);

    // Fetch all branch admins for a set of branch codes (used by getAllBranchAdmins)
    List<BranchAdmin> findAllByBranchCodeIn(List<String> branchCodes);
}
