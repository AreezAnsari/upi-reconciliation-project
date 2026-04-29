package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.*;

import lombok.Data;

@Entity
@Table(name = "M_LOOKUP")
@Data
public class MLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lookup_seq_gen")
    @SequenceGenerator(name = "lookup_seq_gen", sequenceName = "M_LOOKUP_SEQ", allocationSize = 1)
    @Column(name = "LOOKUP_ID")
    private Long id;

    @Column(name = "LOOKUP_NAME", nullable = false)
    private String lookupName;

    @Column(name = "LOOKUP_VALUE")
    private String lookupValue;

    @Column(name = "LOOKUP_CODE", nullable = false)
    private String lookupCode;

    @Column(name = "LOOKUP_DESC")
    private String lookupDesc;

    @Column(name = "SHORT_NAME")
    private String shortName;

    @Column(name = "LONG_NAME")
    private String longName;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "ACTIVE_YN", nullable = false)
    private String activeYn;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "CREATED_ON", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "UPDATED_ON")
    private LocalDateTime updatedOn;

    @ManyToOne
    @JoinColumn(name = "PARENT_LOOKUP_ID")
    private MLookup parentLookup;
}