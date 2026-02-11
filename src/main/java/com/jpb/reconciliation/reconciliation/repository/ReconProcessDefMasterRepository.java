package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconProcessDefMasterRepository extends JpaRepository<ReconProcessDefMaster, Long> {
    ReconProcessDefMaster findByReconProcessId(Long processId);
}
