package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.*;

import lombok.Data;

@Entity
@Table(name = "C_PARAM", uniqueConstraints = {
        @UniqueConstraint(columnNames = "PARAM_NAME")
})
@Data
public class CParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PARAM_NAME", unique = true, nullable = false)
    private String paramName;

    @Column(name = "PARAM_VALUE")
    private String paramValue;

    @Column(name = "ACTIVE_YN")
    private String activeYn = "Y";
}