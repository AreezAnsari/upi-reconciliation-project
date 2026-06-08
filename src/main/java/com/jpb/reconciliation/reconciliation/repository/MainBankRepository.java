package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.MainBank;

@Repository
public interface MainBankRepository extends JpaRepository<MainBank, Long> {

    Optional<MainBank> findByInstitutionId(Long institutionId);

    Optional<MainBank> findByInstitutionCode(String institutionCode);

    // Filter by status: ACTIVE / INACTIVE / PENDING / BLOCKED
    List<MainBank> findByStatus(String status);

    // For docx dashboard — show only active institutions
    List<MainBank> findByStatusNot(String status);

    Optional<MainBank> findByVerificationToken(String verificationToken);


    //SuperUser
    Optional<MainBank> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );

    boolean existsByPrimaryEmail(String primaryEmail);

    // Returns true only when a NON-BLOCKED institution already holds this email.
    // Used by checkEmailExists and createInstitution so that a BLOCKED institution's
    // email does NOT block re-onboarding with the same address.
    boolean existsByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    // Uniqueness check for institution name (case-sensitive exact match)
    boolean existsByInstitutionNameFull(String institutionNameFull);

    // All records sharing an email (may be >1 after BLOCKED re-onboarding)
    List<MainBank> findAllByPrimaryEmail(String primaryEmail);

    // Returns the first non-BLOCKED record for a given email — safe when duplicates exist
    Optional<MainBank> findFirstByPrimaryEmailAndStatusNot(String primaryEmail, String status);

    // For auto-block scheduler — finds all BLOCK_PENDING whose window has passed
    List<MainBank> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

    List<MainBank> findByCreatedBy(String createdBy);

    // Sub-institution code generation — find parent institution by superUserId
    Optional<MainBank> findFirstBySuperUserId(String superUserId);
}
