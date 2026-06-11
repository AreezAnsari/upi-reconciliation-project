package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.*;

import lombok.*;

@Entity
@Table(name = "BANK_PRODUCT_VARIANTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankProductVariant {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "variant_seq_gen"
    )
    @SequenceGenerator(
            name = "variant_seq_gen",
            sequenceName = "variant_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchBank branchBank;
}
