package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADMIN_REPLACEMENT")
public class AdminReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MAIN_ADMIN / BRANCH_ADMIN / USER
    @Column(name = "ENTITY_TYPE", nullable = false, length = 20)
    private String entityType;

    @Column(name = "BANK_CODE", length = 255)
    private String bankCode;

    @Column(name = "BRANCH_CODE", length = 255)
    private String branchCode;

    @Column(name = "ORIGINAL_ENTITY_ID", nullable = false)
    private Long originalEntityId;

    @Column(name = "REPLACEMENT_ENTITY_ID", nullable = false)
    private Long replacementEntityId;

    @Column(name = "REASON", length = 500)
    private String reason;

    // ACTIVE / RESTORED / PERMANENT
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "REPLACED_AT", nullable = false)
    private LocalDateTime replacedAt;

    @Column(name = "REPLACED_BY", nullable = false, length = 255)
    private String replacedBy;

    @Column(name = "FINALIZED_AT")
    private LocalDateTime finalizedAt;

    @Column(name = "FINALIZED_BY", length = 255)
    private String finalizedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public Long getOriginalEntityId() { return originalEntityId; }
    public void setOriginalEntityId(Long originalEntityId) { this.originalEntityId = originalEntityId; }

    public Long getReplacementEntityId() { return replacementEntityId; }
    public void setReplacementEntityId(Long replacementEntityId) { this.replacementEntityId = replacementEntityId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getReplacedAt() { return replacedAt; }
    public void setReplacedAt(LocalDateTime replacedAt) { this.replacedAt = replacedAt; }

    public String getReplacedBy() { return replacedBy; }
    public void setReplacedBy(String replacedBy) { this.replacedBy = replacedBy; }

    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }

    public String getFinalizedBy() { return finalizedBy; }
    public void setFinalizedBy(String finalizedBy) { this.finalizedBy = finalizedBy; }
}
