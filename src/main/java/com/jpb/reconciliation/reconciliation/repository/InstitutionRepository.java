package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByInstitutionId(Long institutionId);

    Optional<Institution> findByInstitutionCode(String institutionCode);

    // Check if same full name already exists
    Boolean existsByInstitutionNameFull(String institutionNameFull);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<Institution> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<Institution> findByStatusNot(String status);
    
    Optional<Institution> findByVerificationToken(String verificationToken);
    
    
    //SuperUser
    Optional<Institution> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );
}