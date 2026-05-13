package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.KalSuperUser;

@Repository
public interface KalSuperUserRepository
        extends JpaRepository<KalSuperUser, Long> {

    Optional<KalSuperUser>
    findBySuperUserId(String superUserId);

    Optional<KalSuperUser>
    findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );

    // Used by OtpController to activate institution after first login
    Optional<KalSuperUser>
    findByEmail(String email);    
}