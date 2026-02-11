package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;

@Repository
public interface ReconFieldDetailsMasterRepository extends JpaRepository<ReconFieldDetailsMaster, Long> {
	
	List<ReconFieldDetailsMaster> findByReconTemplateId(Long templateId);

	List<ReconFieldDetailsMaster> findByReconTemplateIdOrderByReconColumnPosnAsc(Long templateId);

}
