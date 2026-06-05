package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.BranchBank;


@Repository
public interface BranchBankRepository extends JpaRepository<BranchBank, Long> {

    Optional<BranchBank> findByInstitutionId(Long institutionId);

    List<BranchBank> findByParentInstitutionId(Long parentInstitutionId);

    Optional<BranchBank> findFirstBySuperUserId(String superUserId);

    Optional<BranchBank> findByInstitutionCode(String institutionCode);

    // Check if same full name already exists
    Boolean existsByInstitutionNameFull(String institutionNameFull);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<BranchBank> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<BranchBank> findByStatusNot(String status);

    Optional<BranchBank> findByVerificationToken(String verificationToken);


    //SuperUser
    Optional<BranchBank> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );
    boolean existsByPrimaryEmail(String primaryEmail);

    boolean existsByInstitutionCode(String institutionCode);

    // Auto-block scheduler: find BLOCK_PENDING whose 30s window has passed
    List<BranchBank> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

}
