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
@Table(name = "RCN_RECON_USER")
public class ReconUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "BANK_ID")
    private Long bankId;

    @Column(name = "USERNAME", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "FULL_NAME", length = 200, nullable = false)
    private String fullName;

    @Column(name = "EMAIL", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;

    @Column(name = "USER_TYPE", length = 30, nullable = false)
    private String userType;

    @Column(name = "CONTACT_RANK", length = 10)
    private String contactRank;

    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "PASSWORD_HASH", length = 255)
    private String passwordHash;

    @Column(name = "PASSWORD_SET", nullable = false)
    private Integer passwordSet = 0;

    @Column(name = "PASSWORD_UPDATED_AT")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "DESIGNATION", length = 100)
    private String designation;

    @Column(name = "DEPARTMENT", length = 150)
    private String department;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "ACTIVE_PENDING";

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

    @Column(name = "PARENT_USER_ID")
    private Long parentUserId;

    @Column(name = "PRE_DELEGATION_PARENT_ID")
    private Long preDelegationParentId;

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    @Column(name = "APPROVED_YN", length = 1, nullable = false)
    private String approvedYn = "N";

    @Column(name = "APPROVED_BY", length = 100)
    private String approvedBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
