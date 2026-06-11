package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;

@Repository
public interface BranchAdminRepository extends JpaRepository<BranchAdmin, Long> {

    Optional<BranchAdmin> findByBranchCodeAndUsername(String branchCode, String username);

    boolean existsByBranchCodeAndUsername(String branchCode, String username);

    Optional<BranchAdmin> findFirstByEmail(String email);

    Optional<BranchAdmin> findFirstByEmailOrderByIdAsc(String email);

    // Re-onboarding safe variants — skip BLOCKED records so the active admin is returned.
    // When the same email exists in both a BLOCKED and a fresh record, these return the
    // non-BLOCKED one (newest first = highest ID, which is the re-onboarded record).
    Optional<BranchAdmin> findFirstByEmailAndStatusNot(String email, String status);
    Optional<BranchAdmin> findFirstByEmailAndStatusNotOrderByIdDesc(String email, String status);

    Optional<BranchAdmin> findFirstByUsername(String username);
}
