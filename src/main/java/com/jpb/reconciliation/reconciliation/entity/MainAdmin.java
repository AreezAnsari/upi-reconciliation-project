package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
    name = "BANK_ADMIN",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_kal_super_user_bnk_username",
        columnNames = { "bank_code", "username" }
    )
)
public class MainAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "password_set", nullable = false)
    private Integer passwordSet = 0;

    @Column(name = "forgot_otp")
    private String forgotOtp;

    @Column(name = "forgot_otp_expiry")
    private LocalDateTime forgotOtpExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "INACTIVATE_SCHEDULED_AT")
    private LocalDateTime inactivateScheduledAt;

    @Column(name = "PRE_INACTIVATE_STATUS", length = 20)
    private String preInactivateStatus;

    @Column(name = "REACTIVATE_SCHEDULED_AT")
    private LocalDateTime reactivateScheduledAt;

    @Column(name = "PRE_REACTIVATE_STATUS", length = 20)
    private String preReactivateStatus;

    @Column(name = "BLOCK_SCHEDULED_AT")
    private LocalDateTime blockScheduledAt;

    @Column(name = "PRE_BLOCK_STATUS", length = 20)
    private String preBlockStatus;

    @Column(name = "INACTIVATED_BY", length = 100)
    private String inactivatedBy;

    @Column(name = "BLOCKED_BY", length = 100)
    private String blockedBy;
}
