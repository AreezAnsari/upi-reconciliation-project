package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TestInstitution;

@Repository
public interface TestInstitutionRepository extends JpaRepository<TestInstitution, Long> {

    Optional<TestInstitution> findByInstitutionId(Long institutionId);

    Optional<TestInstitution> findByInstitutionCode(String institutionCode);

    // Check if same full name already exists
    Boolean existsByInstitutionNameFull(String institutionNameFull);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<TestInstitution> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<TestInstitution> findByStatusNot(String status);
    
    Optional<TestInstitution> findByVerificationToken(String verificationToken);
}