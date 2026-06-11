package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product date entries for branch banks (Branch Bank).
 * Same pattern as MainBankProduct — stores validFrom / validTo per product.
 * Uses delete-and-reinsert strategy on save (same as admin).
 */
@Entity
@Table(name = "BRANCH_BANK_PRODUCT")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchBankProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BRANCH_BANK_PRODUCT")
    @SequenceGenerator(name = "SEQ_BRANCH_BANK_PRODUCT", sequenceName = "SEQ_BRANCH_BANK_PRODUCT", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
