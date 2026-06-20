package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminReplacementRepository extends JpaRepository<AdminReplacement, Long> {

    boolean existsByOriginalEntityIdAndEntityTypeAndStatus(
            Long originalEntityId, String entityType, String status);

    Optional<AdminReplacement> findByOriginalEntityIdAndEntityTypeAndStatus(
            Long originalEntityId, String entityType, String status);

    Optional<AdminReplacement> findByReplacementEntityIdAndEntityTypeAndStatus(
            Long replacementEntityId, String entityType, String status);

    boolean existsByReplacementEntityIdAndEntityTypeAndStatus(
            Long replacementEntityId, String entityType, String status);

    // Returns any ACTIVE or PERMANENT replacement record for the given original entity
    List<AdminReplacement> findByOriginalEntityIdAndEntityTypeAndStatusIn(
            Long originalEntityId, String entityType, Collection<String> statuses);

    // Returns any ACTIVE or PERMANENT replacement record for the given replacement entity
    List<AdminReplacement> findByReplacementEntityIdAndEntityTypeAndStatusIn(
            Long replacementEntityId, String entityType, Collection<String> statuses);
}
