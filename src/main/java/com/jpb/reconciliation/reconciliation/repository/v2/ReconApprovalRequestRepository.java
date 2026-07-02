package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconApprovalRequestRepository extends JpaRepository<ReconApprovalRequest, Long> {

    List<ReconApprovalRequest> findByStatus(String status);

    List<ReconApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<ReconApprovalRequest> findByMakerId(Long makerId);

    List<ReconApprovalRequest> findByCheckerId(Long checkerId);

    List<ReconApprovalRequest> findByEntityTypeAndStatus(String entityType, String status);

    Optional<ReconApprovalRequest> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, String status);
}
