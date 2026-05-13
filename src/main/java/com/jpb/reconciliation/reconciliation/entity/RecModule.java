package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "REC_MODULES_TEST")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;
}
