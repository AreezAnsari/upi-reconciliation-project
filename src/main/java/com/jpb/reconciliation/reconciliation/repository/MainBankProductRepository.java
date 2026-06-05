package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.MainBankProduct;

@Repository
public interface MainBankProductRepository extends JpaRepository<MainBankProduct, Long> {

    List<MainBankProduct> findByInstitutionId(Long institutionId);

    @Transactional
    void deleteByInstitutionId(Long institutionId);
}
