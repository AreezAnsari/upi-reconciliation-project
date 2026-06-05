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

    // Uniqueness check for institution name (case-sensitive exact match)
    boolean existsByInstitutionNameFull(String institutionNameFull);

    Optional<MainBank> findByPrimaryEmail(String primaryEmail);

    // For auto-block scheduler — finds all BLOCK_PENDING whose window has passed
    List<MainBank> findByStatusAndBlockScheduledAtBefore(String status, LocalDateTime cutoff);

    List<MainBank> findByCreatedBy(String createdBy);

    // Sub-institution code generation — find parent institution by superUserId
    Optional<MainBank> findFirstBySuperUserId(String superUserId);
}
