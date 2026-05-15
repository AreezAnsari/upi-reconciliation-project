package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TestInstitution;

@Repository
public interface TestInstitutionRepository extends JpaRepository<TestInstitution, Long> {

    Optional<TestInstitution> findByInstitutionId(Long institutionId);

    Optional<TestInstitution> findByInstitutionCode(String institutionCode);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<TestInstitution> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<TestInstitution> findByStatusNot(String status);
    
    Optional<TestInstitution> findByVerificationToken(String verificationToken);
    
    
    //SuperUser
    Optional<TestInstitution> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );
    
    boolean existsByPrimaryEmail(String primaryEmail);
    
    // For auto-retire scheduler — finds all RETIRE_PENDING whose 24hr window passed
    List<TestInstitution> findByStatusAndRetireScheduledAtBefore(String status, LocalDateTime cutoff);
    
    List<TestInstitution> findByCreatedBy(String createdBy);
}