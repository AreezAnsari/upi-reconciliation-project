package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditReplacement;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.AuditReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class AuditReplacementServiceImpl implements AuditReplacementService {

    private static final Logger logger = LoggerFactory.getLogger(AuditReplacementServiceImpl.class);

    @Autowired
    private AuditReplacementRepository auditReplacementRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createReplacement(AuditReplacement replacement, String replacedBy) {
        if (replacement.getBankId() == null || replacement.getOriginalUserId() == null
                || replacement.getReplacementUserId() == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank ID, original user ID, and replacement user ID are required.", null));
        }

        // Validate both users exist
        Optional<ReconUser> originalOpt = reconUserRepository.findById(replacement.getOriginalUserId());
        if (!originalOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Original user not found: " + replacement.getOriginalUserId(), null));
        }
        Optional<ReconUser> replacementOpt = reconUserRepository.findById(replacement.getReplacementUserId());
        if (!replacementOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Replacement user not found: " + replacement.getReplacementUserId(), null));
        }

        // Ensure no active replacement already exists for original user
        Optional<AuditReplacement> existingActive = auditReplacementRepository
                .findByOriginalUserIdAndStatus(replacement.getOriginalUserId(), "ACTIVE");
        if (existingActive.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "An active replacement already exists for original user: " + replacement.getOriginalUserId(), null));
        }

        // Set original user INACTIVE, replacement user ACTIVE
        ReconUser originalUser = originalOpt.get();
        ReconUser replacementUser = replacementOpt.get();

        originalUser.setStatus("INACTIVE");
        originalUser.setUpdatedAt(LocalDateTime.now());
        originalUser.setUpdatedBy(replacedBy);
        reconUserRepository.save(originalUser);

        replacementUser.setStatus("ACTIVE");
        replacementUser.setUpdatedAt(LocalDateTime.now());
        replacementUser.setUpdatedBy(replacedBy);
        reconUserRepository.save(replacementUser);

        replacement.setStatus("ACTIVE");
        replacement.setReplacedAt(LocalDateTime.now());
        replacement.setReplacedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        AuditReplacement saved = auditReplacementRepository.save(replacement);

        logger.info("AuditReplacement initiated: bankId={}, originalUserId={} -> INACTIVE, replacementUserId={} -> ACTIVE by {}",
                saved.getBankId(), saved.getOriginalUserId(), saved.getReplacementUserId(), replacedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Replacement initiated. Original user set INACTIVE, replacement user set ACTIVE.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getReplacementsByBankId(Long bankId) {
        List<AuditReplacement> replacements = auditReplacementRepository.findByBankId(bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Replacements fetched.", replacements));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getReplacementsByOriginalUser(Long originalUserId) {
        List<AuditReplacement> replacements = auditReplacementRepository.findByOriginalUserId(originalUserId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Replacements fetched.", replacements));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> finalizeReplacement(Long replacementId, String finalizedBy) {
        Optional<AuditReplacement> opt = auditReplacementRepository.findById(replacementId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Replacement not found with ID: " + replacementId, null));
        }
        AuditReplacement existing = opt.get();
        if (!"ACTIVE".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Only ACTIVE replacements can be finalized. Current status: " + existing.getStatus(), null));
        }
        // Finalize: original user stays INACTIVE permanently, replacement user stays ACTIVE
        existing.setStatus("FINALIZED");
        existing.setFinalizedAt(LocalDateTime.now());
        existing.setFinalizedBy(finalizedBy);
        auditReplacementRepository.save(existing);
        logger.info("AuditReplacement FINALIZED: {} by {}. Original user stays INACTIVE permanently.", replacementId, finalizedBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Replacement finalized. Original user remains INACTIVE permanently.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> restoreReplacement(Long replacementId, String restoredBy) {
        Optional<AuditReplacement> opt = auditReplacementRepository.findById(replacementId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Replacement not found with ID: " + replacementId, null));
        }
        AuditReplacement existing = opt.get();
        if (!"ACTIVE".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Only ACTIVE replacements can be restored. Current status: " + existing.getStatus(), null));
        }

        // Restore original user to ACTIVE, set replacement user back to INACTIVE
        Optional<ReconUser> originalOpt = reconUserRepository.findById(existing.getOriginalUserId());
        Optional<ReconUser> replacementOpt = reconUserRepository.findById(existing.getReplacementUserId());

        if (originalOpt.isPresent()) {
            ReconUser originalUser = originalOpt.get();
            originalUser.setStatus("ACTIVE");
            originalUser.setUpdatedAt(LocalDateTime.now());
            originalUser.setUpdatedBy(restoredBy);
            reconUserRepository.save(originalUser);
        }
        if (replacementOpt.isPresent()) {
            ReconUser replacementUser = replacementOpt.get();
            replacementUser.setStatus("INACTIVE");
            replacementUser.setUpdatedAt(LocalDateTime.now());
            replacementUser.setUpdatedBy(restoredBy);
            reconUserRepository.save(replacementUser);
        }

        existing.setStatus("RESTORED");
        existing.setRestoredAt(LocalDateTime.now());
        existing.setRestoredBy(restoredBy);
        auditReplacementRepository.save(existing);

        logger.info("AuditReplacement RESTORED: {} by {}. Original user -> ACTIVE, replacement user -> INACTIVE.", replacementId, restoredBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Replacement restored. Original user set ACTIVE, replacement user set INACTIVE.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getActiveReplacementForUser(Long originalUserId) {
        Optional<AuditReplacement> opt = auditReplacementRepository.findByOriginalUserIdAndStatus(originalUserId, "ACTIVE");
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "No active replacement found for user: " + originalUserId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Active replacement found.", Collections.singletonList(opt.get())));
    }
}



