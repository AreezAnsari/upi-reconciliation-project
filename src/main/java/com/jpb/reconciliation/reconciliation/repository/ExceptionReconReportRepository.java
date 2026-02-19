package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;

@Repository
public interface ExceptionReconReportRepository extends JpaRepository<ExceptionReconReportEntity, Long> {

	List<ExceptionReconReportEntity> findByProcessId(Long long1);
	
}
