package com.jpb.reconciliation.reconciliation.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFileTmpltMast;

@Repository
public interface ReconFileTmpltMastRepository extends JpaRepository<ReconFileTmpltMast, Long> {

    // ── Finders used by service layer ───────────────────────────────────────

    Optional<ReconFileTmpltMast> findByTemplateIdAndIsDeleted(Long templateId, String isDeleted);

    ReconFileTmpltMast findByTemplateName(String templateName);

    Optional<ReconFileTmpltMast> findByTemplateCode(String templateCode);

    long countByTemplateCodeStartingWith(String prefix);

    boolean existsByTemplateNameAndTenantIdAndIsDeleted(String name, Long tenantId, String isDeleted);

    // ── Paginated list queries ───────────────────────────────────────────────

    /** Two-step fetch: first get IDs (count query friendly), then load full graph */
    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.isDeleted = 'N'")
    Page<ReconFileTmpltMast> findTemplates(Pageable pageable);

    @Query("SELECT DISTINCT t FROM ReconFileTmpltMast t " +
            "LEFT JOIN FETCH t.fieldDetails fd " +
            "LEFT JOIN FETCH fd.fieldType " +
            "LEFT JOIN FETCH fd.fieldFormat " +
            "WHERE t IN :templates")
    List<ReconFileTmpltMast> fetchTemplateDetails(@Param("templates") List<ReconFileTmpltMast> templates);

    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.isDeleted = 'N' ORDER BY t.templateName")
    List<ReconFileTmpltMast> findAllTemplates();

    Page<ReconFileTmpltMast> findByTemplateNameContainingIgnoreCase(String templateName, Pageable pageable);

    Page<ReconFileTmpltMast> findByTemplateType(String templateType, Pageable pageable);

    Page<ReconFileTmpltMast> findByTemplateNameContainingIgnoreCaseAndTemplateType(
            String templateName, String templateType, Pageable pageable);

    @Query("SELECT t FROM ReconFileTmpltMast t " +
            "WHERE t.isDeleted = 'N' " +
            "AND (:status IS NULL   OR t.status       = :status) " +
            "AND (:type IS NULL     OR t.templateType = :type) " +
            "AND (:tenantId IS NULL OR t.tenantId     = :tenantId)")
    Page<ReconFileTmpltMast> findAllWithFilters(
            @Param("status")   String status,
            @Param("type")     String type,
            @Param("tenantId") Long tenantId,
            Pageable pageable);
}
