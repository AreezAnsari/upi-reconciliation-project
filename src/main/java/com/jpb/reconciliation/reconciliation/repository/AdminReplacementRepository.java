package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminReplacementRepository extends JpaRepository<AdminReplacement, Long> {

    boolean existsByOriginalEntityIdAndEntityTypeAndStatus(
            Long originalEntityId, String entityType, String status);

    Optional<AdminReplacement> findByOriginalEntityIdAndEntityTypeAndStatus(
            Long originalEntityId, String entityType, String status);

    Optional<AdminReplacement> findByReplacementEntityIdAndEntityTypeAndStatus(
            Long replacementEntityId, String entityType, String status);
}
