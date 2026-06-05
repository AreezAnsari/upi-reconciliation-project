package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.BranchBankProduct;

/**
 * Repository for branch bank (sub-institution) product date entries.
 * Same pattern as MainBankProductRepository.
 */
@Repository
public interface BranchBankProductRepository extends JpaRepository<BranchBankProduct, Long> {

    List<BranchBankProduct> findByInstitutionId(Long institutionId);

    @Transactional
    void deleteByInstitutionId(Long institutionId);
}
