package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;

@Repository
public interface ReconBatchProcessEntityRepository extends JpaRepository<ReconBatchProcessEntity, Long> {

	ReconBatchProcessEntity findByProcessIdAndSequenceNo(Long processId, Long sequenceId);

	List<ReconBatchProcessEntity> findByProcessTypeAndProcessIdAndInsertDate(String reportType, Long processId,
			LocalDate reportDate);

	List<ReconBatchProcessEntity> findByProcessIdAndStatus(Long processId, String string);

}
