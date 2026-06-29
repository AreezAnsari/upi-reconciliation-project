package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminReplacementRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import org.springframework.http.ResponseEntity;

public interface AdminReplacementService {

    /** Immediate replacement — only used for USER type still (admin types go through schedule-pending). */
    ResponseEntity<RestWithStatusList> replace(AdminReplacementRequest request, String replacedBy);

    /** Validates replacement email without writing to DB — use before scheduling inactivation. */
    ResponseEntity<RestWithStatusList> validateReplacementEmail(AdminReplacementRequest request);

    /** Stores a PENDING replacement record; actual admin creation happens after INACTIVE transition. */
    ResponseEntity<RestWithStatusList> schedulePendingReplacement(AdminReplacementRequest request, String scheduledBy);

    /** Called by scheduler after INACTIVE_PENDING → INACTIVE for MainAdmin. */
    void finalizeMainAdminPending(AdminReplacement pending);

    /** Called by scheduler after INACTIVE_PENDING → INACTIVE for BranchAdmin. */
    void finalizeBranchAdminPending(AdminReplacement pending);

    /** Called by scheduler after INACTIVE_PENDING → INACTIVE for User. */
    void finalizeUserPending(AdminReplacement pending);
}
