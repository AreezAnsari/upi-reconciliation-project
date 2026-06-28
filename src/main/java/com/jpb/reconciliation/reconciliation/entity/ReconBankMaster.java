package com.jpb.reconciliation.reconciliation.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RECON_BANK_MASTER")
public class ReconBankMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BANK_ID")
    private Long bankId;

    @Column(name = "BANK_CODE", length = 20, unique = true, nullable = false)
    private String bankCode;

    @Column(name = "BANK_TYPE", length = 100, nullable = false)
    private String bankType;

    @Column(name = "PARENT_BANK_ID")
    private Long parentBankId;

    @JsonProperty("bankNameFull")
    @JsonAlias("bankName")
    @Column(name = "BANK_NAME", length = 150, nullable = false)
    private String bankName;

    @Column(name = "BANK_NAME_SHORT", length = 50)
    private String bankNameShort;

    @Column(name = "BANK_CATEGORY", length = 100)
    private String bankCategory;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "REQUEST";

    @Column(name = "PRE_BLOCK_STATUS", length = 20)
    private String preBlockStatus;

    @Column(name = "INACTIVATE_SCHEDULED_AT")
    private LocalDateTime inactivateScheduledAt;

    @Column(name = "INACTIVATE_SCHEDULED_BY", length = 100)
    private String inactivateScheduledBy;

    @Column(name = "REACTIVATE_SCHEDULED_AT")
    private LocalDateTime reactivateScheduledAt;

    @Column(name = "REACTIVATE_SCHEDULED_BY", length = 100)
    private String reactivateScheduledBy;

    @Column(name = "BLOCK_SCHEDULED_AT")
    private LocalDateTime blockScheduledAt;

    @Column(name = "BLOCK_SCHEDULED_BY", length = 100)
    private String blockScheduledBy;

    @Column(name = "BLOCK_REASON", length = 500)
    private String blockReason;

    @Column(name = "REG_ADDRESS", length = 500)
    private String regAddress;

    @Column(name = "REG_CITY", length = 100)
    private String regCity;

    @Column(name = "REG_STATE", length = 100)
    private String regState;

    @Column(name = "REG_COUNTRY", length = 100)
    private String regCountry;

    @Column(name = "REG_PHONE", length = 20)
    private String regPhone;

    @Column(name = "ENABLE_MFA")
    private Boolean enableMfa;

    @Column(name = "ENABLE_OTP")
    private Boolean enableOtp;

    @Column(name = "ENABLE_HRMS")
    private Boolean enableHrms;

    @Column(name = "PRIMARY_FULL_NAME", length = 200)
    private String primaryFullName;

    @Column(name = "PRIMARY_EMAIL", length = 150)
    private String primaryEmail;

    @Column(name = "PRIMARY_MOBILE", length = 20)
    private String primaryMobile;

    @Column(name = "SECONDARY_FULL_NAME", length = 200)
    private String secondaryFullName;

    @Column(name = "SECONDARY_EMAIL", length = 150)
    private String secondaryEmail;

    @Column(name = "SECONDARY_MOBILE", length = 20)
    private String secondaryMobile;

    @Column(name = "BANK_ADMIN_USERNAME", length = 100)
    private String bankAdminUsername;

    // Plaintext default password — cleared after admin sets their own password
    @Column(name = "DEFAULT_PASSWORD", length = 255)
    private String defaultPassword;

    @Column(name = "VERIFICATION_TOKEN", length = 255)
    private String verificationToken;

    @Column(name = "TOKEN_EXPIRY")
    private LocalDateTime tokenExpiry;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Transient — not persisted; populated on read / consumed on write via C_BANK_PRODUCT_MAP
    @Transient
    private List<String> selectedProducts;

    @Transient
    private Map<String, ProductDateEntry> productDates;

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductDateEntry {
        private LocalDate validFrom;
        private LocalDate validTo;
    }
}
