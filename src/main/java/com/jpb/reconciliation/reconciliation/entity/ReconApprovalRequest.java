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
@Table(name = "RECON_APPROVAL_REQUEST")
public class ReconApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REQUEST_ID")
    private Long requestId;

    @Column(name = "ENTITY_TYPE", length = 20, nullable = false)
    private String entityType;

    @Column(name = "ENTITY_ID", nullable = false)
    private Long entityId;

    @Column(name = "ACTION_TYPE", length = 20, nullable = false)
    private String actionType;

    @Column(name = "MAKER_ID", nullable = false)
    private Long makerId;

    @Column(name = "SUBMITTED_AT")
    private LocalDateTime submittedAt;

    @Column(name = "CHECKER_ID")
    private Long checkerId;

    @Column(name = "CHECKED_AT")
    private LocalDateTime checkedAt;

    @Column(name = "DECISION", length = 20)
    private String decision;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "DRAFT";
}
