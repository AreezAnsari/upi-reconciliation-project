package com.jpb.reconciliation.reconciliation.dto;

public class AdminReplacementResponse {

    private Long replacementId;
    private String entityType;
    private Long originalEntityId;
    private Long replacementEntityId;
    private String status;
    private String replacedAt;
    private String replacedBy;

    public Long getReplacementId() { return replacementId; }
    public void setReplacementId(Long replacementId) { this.replacementId = replacementId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getOriginalEntityId() { return originalEntityId; }
    public void setOriginalEntityId(Long originalEntityId) { this.originalEntityId = originalEntityId; }

    public Long getReplacementEntityId() { return replacementEntityId; }
    public void setReplacementEntityId(Long replacementEntityId) { this.replacementEntityId = replacementEntityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReplacedAt() { return replacedAt; }
    public void setReplacedAt(String replacedAt) { this.replacedAt = replacedAt; }

    public String getReplacedBy() { return replacedBy; }
    public void setReplacedBy(String replacedBy) { this.replacedBy = replacedBy; }
}
