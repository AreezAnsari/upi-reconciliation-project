package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditReplacement;
import org.springframework.http.ResponseEntity;

public interface AuditReplacementService {

    ResponseEntity<RestWithStatusList> createReplacement(AuditReplacement replacement, String replacedBy);

    ResponseEntity<RestWithStatusList> getReplacementsByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> getReplacementsByOriginalUser(Long originalUserId);

    ResponseEntity<RestWithStatusList> finalizeReplacement(Long replacementId, String finalizedBy);

    ResponseEntity<RestWithStatusList> restoreReplacement(Long replacementId, String restoredBy);

    ResponseEntity<RestWithStatusList> getActiveReplacementForUser(Long originalUserId);

    ResponseEntity<RestWithStatusList> schedulePendingReplacement(
            Long originalUserId, String pendingEmail, String pendingFullName,
            String pendingMobile, String orderedBy, String reason, String scheduledBy);

    void cancelPendingReplacement(Long originalUserId);

    void finalizePendingReplacement(Long originalUserId);

    void onOriginalBlocked(Long originalUserId);

    void onOriginalReactivated(Long originalUserId);
}
