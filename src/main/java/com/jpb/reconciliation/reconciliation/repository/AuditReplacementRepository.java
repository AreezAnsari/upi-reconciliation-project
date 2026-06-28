package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.AuditReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditReplacementRepository extends JpaRepository<AuditReplacement, Long> {

    List<AuditReplacement> findByBankId(Long bankId);

    List<AuditReplacement> findByOriginalUserId(Long originalUserId);

    List<AuditReplacement> findByReplacementUserId(Long replacementUserId);

    List<AuditReplacement> findByStatus(String status);

    List<AuditReplacement> findByBankIdAndStatus(Long bankId, String status);

    Optional<AuditReplacement> findByOriginalUserIdAndStatus(Long originalUserId, String status);

    List<AuditReplacement> findByEntityType(String entityType);

    boolean existsByOriginalUserIdAndStatus(Long originalUserId, String status);

    boolean existsByReplacementUserIdAndStatus(Long replacementUserId, String status);

    Optional<AuditReplacement> findByReplacementUserIdAndStatus(Long replacementUserId, String status);
}
