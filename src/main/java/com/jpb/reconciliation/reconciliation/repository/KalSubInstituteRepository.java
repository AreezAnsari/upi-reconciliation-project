package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.KalSubInstitute;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface KalSubInstituteRepository
        extends JpaRepository<KalSubInstitute, Long> {

    // Main lookup
    @Query(
            value = "SELECT * FROM KAL_SUB_INSTITUTE " +
                    "WHERE TRIM(INSTITUTION_CODE) = TRIM(:institutionCode) " +
                    "AND TRIM(USERNAME) = TRIM(:username)",
            nativeQuery = true
    )
    Optional<KalSubInstitute> findByInstitutionCodeAndUsername(
            @Param("institutionCode") String institutionCode,
            @Param("username") String username
    );

    // Forgot password
    Optional<KalSubInstitute> findFirstByEmail(String email);

    // Username lookup
    Optional<KalSubInstitute> findByUsername(String username);

    // Email lookup
    Optional<KalSubInstitute> findByEmail(String email);
}