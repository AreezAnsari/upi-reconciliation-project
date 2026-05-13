package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;

import lombok.*;

@Entity
@Table(name = "SUPERUSER_INSTITUTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "institution_seq_gen"
    )
    @SequenceGenerator(
            name = "institution_seq_gen",
            sequenceName = "institution_seq",
            allocationSize = 1
    )
    @Column(name = "institution_id")
    private Long id;

    // =========================================================
    // BASIC DETAILS
    // =========================================================

    @Column(name = "institution_code", unique = true)
    private String institutionCode;

    @Column(name = "institution_name_full")
    private String institutionNameFull;

    @Column(name = "institution_name_short")
    private String institutionNameShort;

    // =========================================================
    // BANK TYPE
    // =========================================================

    @ElementCollection
    @CollectionTable(
            name = "institution_bank_types",
            joinColumns = @JoinColumn(name = "institution_id")
    )
    @Column(name = "bank_type")
    private List<String> bankType = new ArrayList<>();

    @Column(name = "bank_logo_name")
    private String bankLogoName;

    @Column(name = "bank_logo_path")
    private String bankLogoPath;

    // =========================================================
    // REGISTERED ADDRESS
    // =========================================================

    private String regAddressLine1;
    private String regAddressLine2;
    private String regAddressLine3;

    private String regCity;
    private String regState;
    private String regCountry;

    private String regPhoneCode;
    private String regCityCode;
    private String regPhone;

    // =========================================================
    // COMMUNICATION ADDRESS
    // =========================================================

    private Boolean sameAsRegistered;

    private String commAddressLine1;
    private String commAddressLine2;
    private String commAddressLine3;

    private String commCity;
    private String commState;
    private String commCountry;

    private String commPhoneCode;
    private String commCityCode;
    private String commPhone;

    // =========================================================
    // PRIMARY CONTACT
    // =========================================================

    private String primaryFullName;
    private String primaryEmail;

    private String primaryMobileCode;
    private String primaryMobile;

    private String primaryAltMobileCode;
    private String primaryAltMobile;

    // =========================================================
    // SECONDARY CONTACT
    // =========================================================

    private String secondaryFullName;
    private String secondaryEmail;

    private String secondaryMobileCode;
    private String secondaryMobile;

    private String secondaryAltMobileCode;
    private String secondaryAltMobile;

    // =========================================================
    // PRODUCTS
    // =========================================================

    @ElementCollection
    @CollectionTable(
            name = "institution_products",
            joinColumns = @JoinColumn(name = "institution_id")
    )
    @Column(name = "product_name")
    private List<String> selectedProducts = new ArrayList<>();

    // =========================================================
    // PRODUCT VARIANTS
    // =========================================================

    @OneToMany(
            mappedBy = "institution",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InstitutionProductVariant> selectedVariants =
            new ArrayList<>();

    // =========================================================
    // SECURITY
    // =========================================================

    private Boolean enableMFA;
    private Boolean enableHRMS;
    private Boolean enableOTP;

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    private EnableStatus status;

    // =========================================================
    // CREATED DATE
    // =========================================================

    private LocalDateTime createdDate;
}