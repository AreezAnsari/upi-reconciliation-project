package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.MainBankProduct;

@Repository
public interface MainBankProductRepository extends JpaRepository<MainBankProduct, Long> {

    List<MainBankProduct> findByBankId(Long bankId);

    @Transactional
    void deleteByBankId(Long bankId);

    @Query("SELECT p.productName FROM MainBankProduct p " +
           "JOIN MainBank mb ON mb.bankId = p.bankId " +
           "WHERE mb.bankCode = :bankCode")
    List<String> findProductNamesByBankCode(@Param("bankCode") String bankCode);
}
