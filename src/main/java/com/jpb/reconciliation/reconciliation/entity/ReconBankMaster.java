package com.jpb.reconciliation.reconciliation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "BANK_NAME", length = 150, nullable = false)
    private String bankName;

    @Column(name = "BANK_CATEGORY", length = 100)
    private String bankCategory;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "ACTIVE";

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

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Transient — received from frontend for Bank Admin user creation, not persisted
    @Transient private String primaryFullName;
    @Transient private String primaryEmail;
    @Transient private String primaryMobile;
    @Transient private String secondaryFullName;
    @Transient private String secondaryEmail;
    @Transient private String secondaryMobile;
}
