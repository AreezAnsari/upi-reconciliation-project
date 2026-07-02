package com.jpb.reconciliation.reconciliation.repository.v2;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;

@Repository
public interface ReconTmpltFieldDtlsRepository extends JpaRepository<ReconTmpltFieldDtls, Long> {

    /** Full graph load — used after template fetch to avoid N+1 */
    @Query("SELECT f FROM ReconTmpltFieldDtls f " +
              "JOIN FETCH f.fieldType " +
              "JOIN FETCH f.fieldFormat " +
              "JOIN FETCH f.template t " +
              "WHERE t.templateId = :templateId " +
              "ORDER BY f.sequenceOrder")
    List<ReconTmpltFieldDtls> findActiveFieldsByTemplateId(@Param("templateId") Long templateId);

    @Query("SELECT MAX(f.sequenceOrder) FROM ReconTmpltFieldDtls f " +
           "WHERE f.template.templateId = :templateId")
    Long findMaxSequenceByTemplateId(@Param("templateId") Long templateId);

    //my
    Optional<ReconTmpltFieldDtls> findFirstByTemplate_TemplateId(Long templateId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReconTmpltFieldDtls f SET f.sequenceOrder = :order WHERE f.fieldId = :fieldId")
    void updateSequenceOrder(@Param("fieldId") Long fieldId, @Param("order") Long order);

    /** Hard delete — used for both normal removal and compensating rollback */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReconTmpltFieldDtls f WHERE f.template.templateId = :templateId")
    void deleteByTemplateId(@Param("templateId") Long templateId);

    // Legacy alias method name — keeps old service code compiling
    default List<ReconTmpltFieldDtls> findFullFieldDetailsByTemplateId(Long templateId) {
        return findActiveFieldsByTemplateId(templateId);
        
        

    }
}
