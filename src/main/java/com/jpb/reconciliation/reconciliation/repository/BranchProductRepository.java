package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.BranchBankProduct;

import java.util.List;

/**
 * Repository for BRANCH_BANK_PRODUCT table.
 *
 * We only need one query: give me all product names
 * for a given branch. The frontend privilege tree will
 * use these to decide what to pre-tick and what to grey-out.
 */
@Repository
public interface BranchProductRepository extends JpaRepository<BranchBankProduct, Long> {

    /**
     * Fetch all product names purchased by a specific branch.
     *
     * Returns: ["UPI", "NEFT", "RTGS"] — just the names, nothing else.
     * Frontend maps these to MODULE_TREE_DATA nodes by matching label.
     *
     * @param branchId  The BRANCH_ID from BRANCH_BANK table
     */
    @Query("SELECT p.productName FROM BranchBankProduct p WHERE p.branchId = :branchId")
    List<String> findProductNamesByBranchId(@Param("branchId") Long branchId);

    /**
     * Check if a specific product is purchased by the branch.
     * Useful for validation before saving privileges.
     */
    @Query("SELECT COUNT(p) > 0 FROM BranchBankProduct p " +
           "WHERE p.branchId = :branchId AND UPPER(p.productName) = UPPER(:productName)")
    boolean existsByBranchIdAndProductName(
            @Param("branchId")    Long branchId,
            @Param("productName") String productName);
}