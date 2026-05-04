package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TEST_INSTITUTION")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestInstitution {

    // ─── Primary Key ──────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TEST_INSTITUTION")
    @SequenceGenerator(name = "SEQ_TEST_INSTITUTION", sequenceName = "SEQ_TEST_INSTITUTION", allocationSize = 1)
    @Column(name = "institution_id")
    private Long institutionId;

    // Docx: "Institution id will be system generated (Total 8 Character —
    //        bank spell and 4 chars are unique mathematical numbers)"
    // e.g.  "STAT4821" for State Bank of India
    @Column(name = "institution_code", length = 8, unique = true)
    private String institutionCode;

    // ─── Step 1: Institution Details ──────────────────────────────────────────
    @Column(name = "institution_name_full", nullable = false, length = 150)
    private String institutionNameFull;

    @Column(name = "institution_name_short", length = 20)
    private String institutionNameShort;

    // Stored as comma-separated: "Issuer,Acquirer"
    @Column(name = "bank_type", length = 100)
    private String bankType;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    // ─── Step 2: Registered Office Address ───────────────────────────────────
    @Column(name = "reg_address_line1", nullable = false, length = 255)
    private String regAddressLine1;

    @Column(name = "reg_address_line2", length = 255)
    private String regAddressLine2;

    @Column(name = "reg_address_line3", length = 255)
    private String regAddressLine3;

    @Column(name = "reg_city", nullable = false, length = 100)
    private String regCity;

    @Column(name = "reg_state", nullable = false, length = 100)
    private String regState;

    @Column(name = "reg_country", nullable = false, length = 100)
    private String regCountry;

    @Column(name = "reg_phone_code", length = 10)
    private String regPhoneCode;

    @Column(name = "reg_city_code", length = 10)
    private String regCityCode;

    @Column(name = "reg_phone", length = 20)
    private String regPhone;

    // ─── Step 2: Communication Address ───────────────────────────────────────
    // Y = same as registered address
    @Column(name = "same_as_registered", length = 1)
    private String sameAsRegistered = "N";

    @Column(name = "comm_address_line1", length = 255)
    private String commAddressLine1;

    @Column(name = "comm_address_line2", length = 255)
    private String commAddressLine2;

    @Column(name = "comm_address_line3", length = 255)
    private String commAddressLine3;

    @Column(name = "comm_city", length = 100)
    private String commCity;

    @Column(name = "comm_state", length = 100)
    private String commState;

    @Column(name = "comm_country", length = 100)
    private String commCountry;

    @Column(name = "comm_phone_code", length = 10)
    private String commPhoneCode;

    @Column(name = "comm_city_code", length = 10)
    private String commCityCode;

    @Column(name = "comm_phone", length = 20)
    private String commPhone;

    // ─── Step 3: Primary Contact ──────────────────────────────────────────────
    @Column(name = "primary_full_name", nullable = false, length = 100)
    private String primaryFullName;

    @Column(name = "primary_email", nullable = false, length = 150)
    private String primaryEmail;

    @Column(name = "primary_mobile_code", length = 10)
    private String primaryMobileCode;

    @Column(name = "primary_mobile", nullable = false, length = 15)
    private String primaryMobile;

    @Column(name = "primary_alt_mobile_code", length = 10)
    private String primaryAltMobileCode;

    @Column(name = "primary_alt_mobile", length = 15)
    private String primaryAltMobile;

    // ─── Step 3: Secondary Contact ────────────────────────────────────────────
    @Column(name = "secondary_full_name", length = 100)
    private String secondaryFullName;

    @Column(name = "secondary_email", length = 150)
    private String secondaryEmail;

    @Column(name = "secondary_mobile_code", length = 10)
    private String secondaryMobileCode;

    @Column(name = "secondary_mobile", length = 15)
    private String secondaryMobile;

    @Column(name = "secondary_alt_mobile_code", length = 10)
    private String secondaryAltMobileCode;

    @Column(name = "secondary_alt_mobile", length = 15)
    private String secondaryAltMobile;

    // ─── Step 4: Products ─────────────────────────────────────────────────────
    // Stored as comma-separated: "UPI,NEFT,RTGS,Credit Cards"
    @Column(name = "selected_products", length = 500)
    private String selectedProducts;

    // ─── Step 5: Security & Compliance ───────────────────────────────────────
    @Column(name = "enable_mfa", length = 1)
    private String enableMfa = "N";

    @Column(name = "enable_hrms", length = 1)
    private String enableHrms = "N";

    @Column(name = "enable_otp", length = 1)
    private String enableOtp = "Y";

    // ─── Status ───────────────────────────────────────────────────────────────
    // ACTIVE / INACTIVE / PENDING / BLOCKED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";
    
    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;
    
 // ── Super User Credentials ────────────────────────────────────────────────
    @Column(name = "super_user_id", length = 100)
    private String superUserId;

    @Column(name = "default_password", length = 100)
    private String defaultPassword;

    // ─── Audit Columns (same pattern as all entities in this project) ─────────
    @CreatedDate
    @Column(updatable = false, name = "crated_at")
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false, name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(insertable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(insertable = false, name = "updated_by")
    private String updatedBy;
}