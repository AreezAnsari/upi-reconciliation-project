package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.BranchBank;


@Repository
public interface BranchBankRepository extends JpaRepository<BranchBank, Long> {

    Optional<BranchBank> findByBranchId(Long branchId);

    List<BranchBank> findByParentBankId(Long parentBankId);

    Optional<BranchBank> findFirstByBranchAdminId(String superUserId);

    Optional<BranchBank> findByBranchCode(String branchCode);

    // Check if same full name already exists
    Boolean existsByBranchNameFull(String branchNameFull);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<BranchBank> findByStatus(String status);

    // For docx dashboard — show only active banks
    List<BranchBank> findByStatusNot(String status);

    Optional<BranchBank> findByVerificationToken(String verificationToken);


    //SuperUser
    Optional<BranchBank> findByBranchCodeAndBranchAdminId(
            String branchCode,
            String branchAdminId
    );
    boolean existsByPrimaryEmail(String primaryEmail);

    // Returns true only when a NON-BLOCKED branch bank already holds this email.
    // Used by checkEmailExists and createBank so that a BLOCKED record's
    // email does NOT block re-onboarding with the same address.
    boolean existsByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    // All records sharing an email (may be >1 after BLOCKED re-onboarding)
    List<BranchBank> findAllByPrimaryEmail(String primaryEmail);

    // Returns the first non-BLOCKED record for a given email — safe when duplicates exist
    Optional<BranchBank> findFirstByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    boolean existsByBranchCode(String branchCode);

    // Auto-block scheduler: find BLOCK_PENDING whose 30s window has passed
    List<BranchBank> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

}
