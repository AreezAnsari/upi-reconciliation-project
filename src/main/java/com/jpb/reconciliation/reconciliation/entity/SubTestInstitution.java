package com.jpb.reconciliation.reconciliation.entity;

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

@Entity
@Table(name = "SUB_TEST_INSTITUTION")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTestInstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SUB_INSTITUTION")
    @SequenceGenerator(name = "SEQ_SUB_INSTITUTION", sequenceName = "SEQ_TEST_INSTITUTION", allocationSize = 1)
    @Column(name = "institution_id")
    private Long subInstitutionId;

    @Column(name = "institution_code", length = 20)
    private String institutionCode;

    @Column(name = "institution_name_full", length = 150)
    private String institutionNameFull;

    @Column(name = "institution_type", length = 50)
    private String institutionType;

    @Column(name = "parent_institution_id")
    private Long parentInstitutionId;

    @Column(name = "reg_city", length = 100)
    private String regCity;

    @Column(name = "reg_state", length = 100)
    private String regState;

    @Column(name = "reg_country", length = 100)
    private String regCountry;

    @Column(name = "primary_full_name", length = 100)
    private String primaryFullName;

    @Column(name = "primary_designation", length = 100)
    private String primaryDesignation;

    @Column(name = "primary_email", length = 150)
    private String primaryEmail;

    @Column(name = "primary_phone", length = 20)
    private String primaryPhone;

    @Column(name = "status", length = 20)
    private String status;

    // Stores the status before a parent-cascade BLOCK — used to restore on ACTIVE
    @Column(name = "pre_block_status", length = 20)
    private String preBlockStatus;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
