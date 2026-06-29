package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_DELEGATION")
public class UserDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delegation_seq")
    @SequenceGenerator(name = "delegation_seq", sequenceName = "USER_DELEGATION_SEQ", allocationSize = 1)
    private Long id;

    // User who is delegating (being inactivated)
    @Column(name = "DELEGATOR_USER_ID", nullable = false)
    private Long delegatorUserId;

    // Parent user receiving the delegation
    @Column(name = "DELEGATEE_USER_ID", nullable = false)
    private Long delegateeUserId;

    @Column(name = "REASON", length = 500)
    private String reason;

    // ACTIVE / RESTORED
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "DELEGATED_AT", nullable = false)
    private LocalDateTime delegatedAt;

    @Column(name = "CREATED_BY", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "RESTORED_AT")
    private LocalDateTime restoredAt;

    @Column(name = "RESTORED_BY", length = 100)
    private String restoredBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDelegatorUserId() { return delegatorUserId; }
    public void setDelegatorUserId(Long delegatorUserId) { this.delegatorUserId = delegatorUserId; }

    public Long getDelegateeUserId() { return delegateeUserId; }
    public void setDelegateeUserId(Long delegateeUserId) { this.delegateeUserId = delegateeUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDelegatedAt() { return delegatedAt; }
    public void setDelegatedAt(LocalDateTime delegatedAt) { this.delegatedAt = delegatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getRestoredAt() { return restoredAt; }
    public void setRestoredAt(LocalDateTime restoredAt) { this.restoredAt = restoredAt; }

    public String getRestoredBy() { return restoredBy; }
    public void setRestoredBy(String restoredBy) { this.restoredBy = restoredBy; }
}
