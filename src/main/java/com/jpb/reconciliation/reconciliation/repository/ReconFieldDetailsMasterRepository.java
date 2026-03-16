package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;

@Repository
public interface ReconFieldDetailsMasterRepository extends JpaRepository<ReconFieldDetailsMaster, Long> {

	@Query("SELECT f " + "FROM ReconFieldDetailsMaster f " + "JOIN FETCH f.reconFieldTypeMaster "
			+ "JOIN FETCH f.reconFieldFormatMaster " + "JOIN FETCH f.reconTemplateDetails t "
			+ "WHERE t.reconTemplateId = :templateId " + "ORDER BY f.reconColumnPosn")
	List<ReconFieldDetailsMaster> findFullFieldDetailsByTemplateId(@Param("templateId") Long templateId);

//	List<ReconFieldDetailsMaster> findByReconTemplateDetails_ReconTemplateIdOrderByReconColumnPosnAsc(Long templateId);

//	List<ReconFieldDetailsMaster> findByReconTemplateId(Long templateId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
//	@Transactional
	@Query("DELETE FROM ReconFieldDetailsMaster r WHERE r.reconTemplateDetails.reconTemplateId = :templateId")
	void deleteByTemplateId(@Param("templateId") Long templateId);

//	@Query(value = "SELECT * FROM RCN_FIELD_DTL_MAST WHERE RFM_TEMPLATE_ID = :templateId ORDER BY RFM_COL_POSN ASC", 
//	           nativeQuery = true)
//	    List<ReconFieldDetailsMaster> findByReconTemplateIdOrderByReconColumnPosnAsc(@Param("templateId") Long templateId);
}
