package com.jpb.reconciliation.reconciliation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "C_BANK_PRODUCT_MAP")
public class CBankProductMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "BANK_ID", nullable = false)
    private Long bankId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "VALID_FROM", nullable = false)
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "ACTIVE";
}
