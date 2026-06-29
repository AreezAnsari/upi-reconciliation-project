package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.MainBank;

@Repository
public interface MainBankRepository extends JpaRepository<MainBank, Long> {

    Optional<MainBank> findByBankId(Long bankId);

    Optional<MainBank> findByBankCode(String bankCode);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<MainBank> findByStatus(String status);

    // For docx dashboard — show only active banks
    List<MainBank> findByStatusNot(String status);

    Optional<MainBank> findByVerificationToken(String verificationToken);


    //SuperUser
    Optional<MainBank> findByBankCodeAndBankAdminId(
            String bankCode,
            String superUserId
    );

    boolean existsByPrimaryEmail(String primaryEmail);

    // Returns true only when a NON-BLOCKED bank already holds this email.
    // Used by checkEmailExists and createbank so that a BLOCKED bank's
    // email does NOT block re-onboarding with the same address.
    boolean existsByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    // Uniqueness check for bank name (case-sensitive exact match)
    boolean existsByBankNameFull(String bankNameFull);

    // All records sharing an email (may be >1 after BLOCKED re-onboarding)
    List<MainBank> findAllByPrimaryEmail(String primaryEmail);

    // Returns the first non-BLOCKED record for a given email — safe when duplicates exist
    Optional<MainBank> findFirstByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    // For auto-block scheduler — finds all BLOCK_PENDING whose window has passed
    List<MainBank> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

    List<MainBank> findByCreatedBy(String createdBy);

    // Branch bank code generation — find parent bank by superUserId
    Optional<MainBank> findFirstByBankAdminId(String superUserId);
}
