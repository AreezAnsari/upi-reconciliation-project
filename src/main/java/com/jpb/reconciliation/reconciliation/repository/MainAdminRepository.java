package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.MainAdmin;

@Repository
public interface MainAdminRepository
        extends JpaRepository<MainAdmin, Long> {

    // Primary lookup — institutionCode+username is the composite identity
    Optional<MainAdmin> findByInstitutionCodeAndUsername(
            String institutionCode,
            String username
    );

    // Uniqueness check during username generation (per-institution)
    boolean existsByInstitutionCodeAndUsername(
            String institutionCode,
            String username
    );

    // Legacy fallback — only used for old records where institution_code is NULL
    Optional<MainAdmin> findFirstByUsernameAndInstitutionCodeIsNull(String username);

    // Email bridge — used when institutionCode in KAL_SUPER_USER is stale/wrong
    Optional<MainAdmin> findFirstByEmail(String email);
    Optional<MainAdmin> findFirstByEmailOrderByIdAsc(String email);

    // Re-onboarding safe variants — skip BLOCKED records so the active admin is returned.
    // When the same email exists in both a BLOCKED and a fresh record, these return the
    // non-BLOCKED one (newest first = highest ID, which is the re-onboarded record).
    Optional<MainAdmin> findFirstByEmailAndStatusNot(String email, String status);
    Optional<MainAdmin> findFirstByEmailAndStatusNotOrderByIdDesc(String email, String status);

    // JWT validation only — CustomUserDetailService uses this to load UserDetails from token subject
    Optional<MainAdmin> findFirstByUsername(String username);
}
