package com.jpb.reconciliation.reconciliation.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping to BRANCH_BANK_PRODUCT table.
 *
 * Key columns used:
 *   BRANCH_ID    — which branch purchased this product
 *   PRODUCT_NAME — e.g. "UPI", "NEFT", "RTGS", "AEPS"
 *   VALID_FROM   — product active from this date
 *   VALID_TO     — product expires on this date
 *
 * We check VALID_TO >= SYSDATE to only show currently active products.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "BRANCH_BANK_PRODUCT")
public class BranchBankProduct {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "BRANCH_ID", nullable = false)
    private Long branchId;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;
}