package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;

@Repository
public interface ReconTemplateDetailsRepository extends JpaRepository<ReconTemplateDetails, Long> {
	ReconTemplateDetails findByReconTemplateId(Long reconTemplateId);

	ReconTemplateDetails findByTemplateName(String templateName);

	// Fetch ONLY template and field details (excluding file details)
	@Query(value = "SELECT DISTINCT t FROM ReconTemplateDetails t " + "LEFT JOIN FETCH t.fieldDetails fd "
			+ "LEFT JOIN FETCH fd.reconFieldTypeMaster "
			+ "LEFT JOIN FETCH fd.reconFieldFormatMaster", countQuery = "SELECT COUNT(DISTINCT t) FROM ReconTemplateDetails t")
	Page<ReconTemplateDetails> findAllWithDetails(Pageable pageable);

	@Query("SELECT t FROM ReconTemplateDetails t ORDER BY t.templateName")
	List<ReconTemplateDetails> findAllTemplates();

	Page<ReconTemplateDetails> findByTemplateNameContainingIgnoreCase(String templateName, Pageable pageable);

	Page<ReconTemplateDetails> findByTemplateType(String templateType, Pageable pageable);

	Page<ReconTemplateDetails> findByTemplateNameContainingIgnoreCaseAndTemplateType(String templateName,
			String templateType, Pageable pageable);
}
