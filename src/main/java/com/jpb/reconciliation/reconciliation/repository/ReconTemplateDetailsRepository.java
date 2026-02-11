package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconTemplateDetailsRepository extends JpaRepository<ReconTemplateDetails, Long> {
    ReconTemplateDetails findByReconTemplateId(Long reconTemplateId);
}
