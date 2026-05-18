package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.SubInstitution;


@Repository
public interface SubInstitutionRepository extends JpaRepository<SubInstitution, Long> {

    Optional<SubInstitution> findByInstitutionId(Long institutionId);

    Optional<SubInstitution> findByInstitutionCode(String institutionCode);

    // Check if same full name already exists
    Boolean existsByInstitutionNameFull(String institutionNameFull);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<SubInstitution> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<SubInstitution> findByStatusNot(String status);
    
    Optional<SubInstitution> findByVerificationToken(String verificationToken);
    
    
    //SuperUser
    Optional<SubInstitution> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );
    boolean existsByPrimaryEmail(String primaryEmail);
  
}