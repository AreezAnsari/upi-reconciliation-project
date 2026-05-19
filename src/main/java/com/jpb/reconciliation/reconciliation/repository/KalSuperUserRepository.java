package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.SubSuperUser;

@Repository
public interface KalSuperUserRepository
        extends JpaRepository<SubSuperUser, Long> {

    // Main lookup
    Optional<SubSuperUser> findByInstitutionCodeAndUsername(
            String institutionCode,
            String username
    );

    // Forgot password
    Optional<SubSuperUser> findFirstByEmail(String email);

    // Username lookup
    Optional<SubSuperUser> findByUsername(String username);

    // Email lookup
    Optional<SubSuperUser> findByEmail(String email);
}