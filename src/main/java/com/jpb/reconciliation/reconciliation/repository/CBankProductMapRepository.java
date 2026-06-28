package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.CBankProductMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CBankProductMapRepository extends JpaRepository<CBankProductMap, Long> {

    List<CBankProductMap> findByBankId(Long bankId);

    List<CBankProductMap> findByProductId(Long productId);

    List<CBankProductMap> findByBankIdAndStatus(Long bankId, String status);

    Optional<CBankProductMap> findByBankIdAndProductId(Long bankId, Long productId);

    boolean existsByBankIdAndProductId(Long bankId, Long productId);

    void deleteByBankId(Long bankId);

    @Query("SELECT pm.productName FROM CBankProductMap bpm " +
           "JOIN ReconProductMaster pm ON pm.productId = bpm.productId " +
           "WHERE bpm.bankId = :bankId AND bpm.status = 'ACTIVE'")
    List<String> findActiveProductNamesByBankId(@Param("bankId") Long bankId);
}
