package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconFileDetailsMasterRepository extends JpaRepository<ReconFileDetailsMaster, Long> {

	ReconFileDetailsMaster findByReconFileId(Long processId);

	@Query("SELECT r FROM ReconFileDetailsMaster r WHERE r.rfdTranFileFlag = :flag")
	List<ReconFileDetailsMaster> findByRfdTranFileFlag(@Param("flag") String flag);


	ReconFileDetailsMaster findByReconTemplateDetails_ReconTemplateId(Long reconTemp1);
}

