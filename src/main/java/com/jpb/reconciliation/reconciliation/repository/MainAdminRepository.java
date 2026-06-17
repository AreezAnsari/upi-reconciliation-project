package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.MainAdmin;

@Repository
public interface MainAdminRepository
        extends JpaRepository<MainAdmin, Long> {

    // Primary lookup — bankCode+username is the composite identity
    Optional<MainAdmin> findByBankCodeAndUsername(
            String bankCode,
            String username
    );

    // Uniqueness check during username generation (per-bank)
    boolean existsByBankCodeAndUsername(
            String bankCode,
            String username
    );

    // Legacy fallback — only used for old records where bank_code is NULL
    Optional<MainAdmin> findFirstByUsernameAndBankCodeIsNull(String username);

    // Email bridge — used when bankCode in BANK_ADMIN is stale/wrong
    Optional<MainAdmin> findFirstByEmail(String email);
    Optional<MainAdmin> findFirstByEmailOrderByIdAsc(String email);

    // Re-onboarding safe variants — skip BLOCKED records so the active admin is returned.
    // When the same email exists in both a BLOCKED and a fresh record, these return the
    // non-BLOCKED one (newest first = highest ID, which is the re-onboarded record).
    Optional<MainAdmin> findFirstByEmailAndStatusNot(String email, String status);
    Optional<MainAdmin> findFirstByEmailAndStatusNotOrderByIdDesc(String email, String status);

    // JWT validation only — CustomUserDetailService uses this to load UserDetails from token subject
    Optional<MainAdmin> findFirstByUsername(String username);

    // Scheduler: auto-inactivate INACTIVE_PENDING whose window has passed
    List<MainAdmin> findByStatusAndInactivateScheduledAtBefore(String status, LocalDateTime cutoff);

    // Scheduler: auto-reactivate ACTIVE_PENDING whose window has passed
    List<MainAdmin> findByStatusAndReactivateScheduledAtBefore(String status, LocalDateTime cutoff);

    // Scheduler: auto-block BLOCK_PENDING whose window has passed
    List<MainAdmin> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);
}
