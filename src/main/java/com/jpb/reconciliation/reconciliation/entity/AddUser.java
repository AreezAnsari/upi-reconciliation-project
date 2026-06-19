package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REC_USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AddUser {

    public enum Role {
        MAKER, CHECKER, WORKER, AUDITOR, IT_OPS, SUPERVISOR, RCC_CXO, DEFAULT
    }

    public enum UserType {
        INTERNAL, EXTERNAL
    }

    public enum UserStatus {
        REQUEST, VERIFIED, ACTIVE, INACTIVE_PENDING, INACTIVE, ACTIVE_PENDING, BLOCK_PENDING, BLOCK, RETIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "APP_USER_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "FULL_NAME", nullable = false, length = 200)
    private String fullName;

    @Column(name = "USERNAME", unique = true, nullable = false, length = 100)
    private String username;

    @Column(name = "EMAIL", unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "DEPARTMENT", length = 150)
    private String department;

    @Column(name = "DESIGNATION", length = 100)
    private String designation;

    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_TYPE", nullable = false, length = 20)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.REQUEST;

    @Column(name = "EXTERNAL_DEPARTMENT_NAME", length = 200)
    private String externalDepartmentName;

    @Column(name = "EXTERNAL_SUPERVISOR_NAME", length = 100)
    private String externalSupervisorName;

    @Column(name = "EXTERNAL_SUPERVISOR_EMAIL", length = 150)
    private String externalSupervisorEmail;

    @Column(name = "EXTERNAL_SUPERVISOR_PHONE", length = 20)
    private String externalSupervisorPhone;

    @Column(name = "ROLE_TYPE", nullable = false, length = 20)
    private String roleType;

    @Column(name = "PASSWORD_SET", nullable = false)
    @Builder.Default
    private Integer passwordSet = 0;

    @Column(name = "BANK_CODE", length = 50)
    private String bankCode;

    @Column(name = "BRANCH_CODE", length = 50)
    private String branchCode;

    @Column(name = "DEFAULT_PASSWORD", length = 100)
    private String defaultPassword; // stores BCrypt hash, same as MainBank

    // ─── Inactivate Schedule ──
    @Column(name = "INACTIVATE_SCHEDULED_AT")
    private LocalDateTime inactivateScheduledAt;

    @Column(name = "INACTIVATE_SCHEDULED_BY", length = 100)
    private String inactivateScheduledBy;

    // ─── Reactivate Schedule ──
    @Column(name = "REACTIVATE_SCHEDULED_AT")
    private LocalDateTime reactivateScheduledAt;

    @Column(name = "REACTIVATE_SCHEDULED_BY", length = 100)
    private String reactivateScheduledBy;

    // ─── Block Schedule ──
    @Column(name = "BLOCK_SCHEDULED_AT")
    private LocalDateTime blockScheduledAt;

    @Column(name = "BLOCK_SCHEDULED_BY", length = 100)
    private String blockScheduledBy;

    @Column(name = "BLOCK_REASON", length = 500)
    private String blockReason;

    @Column(name = "PRE_BLOCK_STATUS", length = 20)
    private String preBlockStatus;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
