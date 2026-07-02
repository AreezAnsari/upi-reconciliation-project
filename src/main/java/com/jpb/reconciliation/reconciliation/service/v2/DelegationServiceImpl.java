package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditUserDelegation;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditUserDelegationRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DelegationServiceImpl implements DelegationService {

    private static final Logger logger = LoggerFactory.getLogger(DelegationServiceImpl.class);

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private AuditUserDelegationRepository delegationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private EmailService emailService;

    // ── Delegate: user goes INACTIVE → transfer children to parent (delegatee) ───
    @Override
    @Transactional
    public void delegateOnInactivate(Long delegatorUserId, String triggeredBy) {
        Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
        if (!delegatorOpt.isPresent()) return;

        ReconUser delegator = delegatorOpt.get();
        Long parentId = delegator.getParentUserId();
        if (parentId == null) {
            logger.debug("delegateOnInactivate: delegatorUserId={} has no parent — skip delegation", delegatorUserId);
            return;
        }

        Optional<ReconUser> delegateeOpt = reconUserRepository.findById(parentId);
        if (!delegateeOpt.isPresent()) return;
        ReconUser delegatee = delegateeOpt.get();

        // Transfer children
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegatorUserId);
        for (ReconUser child : children) {
            child.setPreDelegationParentId(child.getParentUserId());
            child.setParentUserId(parentId);
            child.setUpdatedAt(LocalDateTime.now());
            reconUserRepository.save(child);
        }

        // Record delegation
        AuditUserDelegation delegation = new AuditUserDelegation();
        delegation.setDelegatorUserId(delegatorUserId);
        delegation.setDelegateeUserId(parentId);
        delegation.setBankId(delegator.getBankId());
        delegation.setReason("User inactivated — children delegated to parent");
        delegation.setStatus("ACTIVE");
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setDelegatedBy(triggeredBy);
        delegation.setCreatedAt(LocalDateTime.now());
        delegation.setCreatedBy(triggeredBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "CREATE",
                null, "delegatorUserId=" + delegatorUserId + ",delegateeUserId=" + parentId,
                triggeredBy, delegator.getUserType(), delegator.getBankId(),
                "Delegation: " + delegator.getUsername() + " → " + delegatee.getUsername());

        // Emails
        try {
            String orgName = resolveOrgName(delegator);
            emailService.sendDelegationToDelegate(delegator.getEmail(), delegator.getFullName(),
                    delegatee.getFullName(), "User inactivated", orgName);
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegate failed: {}", e.getMessage());
        }
        try {
            emailService.sendDelegationToDelegatee(delegatee.getEmail(), delegatee.getFullName(),
                    delegator.getFullName(), "User inactivated");
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegatee failed: {}", e.getMessage());
        }

        logger.info("Delegation created: {} → {} ({} children transferred)",
                delegator.getUsername(), delegatee.getUsername(), children.size());
    }

    // ── Restore: user reactivated → children return to original parent ───────────
    @Override
    @Transactional
    public void restoreDelegationOnReactivate(Long delegatorUserId, String triggeredBy) {
        Optional<AuditUserDelegation> activeOpt = delegationRepository
                .findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, "ACTIVE");
        if (!activeOpt.isPresent()) return;

        AuditUserDelegation delegation = activeOpt.get();
        Long delegateeId = delegation.getDelegateeUserId();

        // Restore children: those whose preDelegationParentId = delegatorUserId
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegateeId);
        for (ReconUser child : children) {
            if (delegatorUserId.equals(child.getPreDelegationParentId())) {
                child.setParentUserId(delegatorUserId);
                child.setPreDelegationParentId(null);
                child.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(child);
            }
        }

        delegation.setStatus("RESTORED");
        delegation.setRestoredAt(LocalDateTime.now());
        delegation.setRestoredBy(triggeredBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "UPDATE",
                "status=ACTIVE", "status=RESTORED", triggeredBy, null, delegation.getBankId(),
                "Delegation restored: delegatorUserId=" + delegatorUserId);

        // Emails
        Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
        Optional<ReconUser> delegateeOpt = reconUserRepository.findById(delegateeId);

        delegatorOpt.ifPresent(delegator -> {
            try {
                emailService.sendDelegationRestoredToDelegator(delegator.getEmail(),
                        delegator.getFullName(),
                        delegateeOpt.map(ReconUser::getFullName).orElse("your deputy"));
            } catch (Exception e) {
                logger.warn("sendDelegationRestoredToDelegator failed: {}", e.getMessage());
            }
        });
        delegateeOpt.ifPresent(delegatee -> {
            try {
                emailService.sendDelegationRestoredToDelegatee(delegatee.getEmail(),
                        delegatee.getFullName(),
                        delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
            } catch (Exception e) {
                logger.warn("sendDelegationRestoredToDelegatee failed: {}", e.getMessage());
            }
        });

        logger.info("Delegation restored for delegatorUserId={}", delegatorUserId);
    }

    // ── Notify delegatee: delegator BLOCK_PENDING ────────────────────────────────
    @Override
    public void notifyDelegateeBlockPending(Long delegatorUserId, String blockAt) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, "ACTIVE")
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorBlockPendingToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"),
                                blockAt);
                    } catch (Exception e) {
                        logger.warn("sendDelegatorBlockPendingToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    // ── Notify delegatee: delegator permanently BLOCKED ──────────────────────────
    @Override
    public void notifyDelegateeBlocked(Long delegatorUserId) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, "ACTIVE")
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorBlockedToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
                    } catch (Exception e) {
                        logger.warn("sendDelegatorBlockedToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    // ── Notify delegatee: delegator UNBLOCKED ───────────────────────────────────
    @Override
    public void notifyDelegateeUnblocked(Long delegatorUserId) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, "ACTIVE")
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorUnblockedToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
                    } catch (Exception e) {
                        logger.warn("sendDelegatorUnblockedToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    private String resolveOrgName(ReconUser user) {
        return "ReconXpert.Ai";
    }

    private void auditLog(String table, Long recordId, String op, String oldVal, String newVal,
                           String actor, String actorType, Long bankId, String label) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(table); log.setRecordId(recordId); log.setOperation(op);
            log.setOldValue(oldVal); log.setNewValue(newVal);
            log.setActorUsername(actor); log.setActorType(actorType); log.setBankId(bankId);
            log.setActionLabel(label); log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) { logger.warn("AuditLog save failed: {}", e.getMessage()); }
    }
}
