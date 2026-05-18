package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.TestInstitutionProduct;

@Repository
public interface TestInstitutionProductRepository extends JpaRepository<TestInstitutionProduct, Long> {

    List<TestInstitutionProduct> findByInstitutionId(Long institutionId);

    @Transactional
    void deleteByInstitutionId(Long institutionId);
}
