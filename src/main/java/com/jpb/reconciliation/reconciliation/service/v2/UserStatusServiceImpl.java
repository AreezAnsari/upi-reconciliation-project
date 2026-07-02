package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

@Service
public class UserStatusServiceImpl implements UserStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UserStatusServiceImpl.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AuditReplacementService replacementService;
    @Autowired private DelegationService delegationService;
    @Autowired private EmailService emailService;

    // ── Schedule Inactivate ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, String scheduledBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"ACTIVE".equals(user.getStatus())) {
            return fail("User must be ACTIVE to schedule inactivation. Current: " + user.getStatus());
        }

        String old = user.getStatus();
        user.setStatus("INACTIVE_PENDING");
        user.setInactivateScheduledAt(LocalDateTime.now());
        user.setInactivateScheduledBy(scheduledBy);
        user.setReactivateScheduledAt(null);
        user.setReactivateScheduledBy(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(scheduledBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", old, "INACTIVE_PENDING",
                scheduledBy, user.getUserType(), user.getBankId(), "Inactivation scheduled");

        String inactivateAt = LocalDateTime.now().plusSeconds(30).format(FMT);
        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendInactivatePendingWarning(user.getEmail(), user.getFullName(),
                    orgInfo[0], orgInfo[1], inactivateAt);
        } catch (Exception e) {
            logger.warn("sendInactivatePendingWarning failed for {}: {}", user.getUsername(), e.getMessage());
        }

        notifyActor(scheduledBy, "Inactivation Scheduled", user.getFullName(), user.getUsername(), inactivateAt, user);

        logger.info("User {} scheduled for INACTIVE by {}", user.getUsername(), scheduledBy);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Inactivation scheduled. User will go INACTIVE in ~30 seconds.", null));
    }

    // ── Undo Inactivate ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoInactivate(Long userId, String undoneBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"INACTIVE_PENDING".equals(user.getStatus())) {
            return fail("No pending inactivation found for this user.");
        }

        user.setStatus("ACTIVE");
        user.setInactivateScheduledAt(null);
        user.setInactivateScheduledBy(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(undoneBy);
        reconUserRepository.save(user);

        replacementService.cancelPendingReplacement(userId);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", "INACTIVE_PENDING", "ACTIVE",
                undoneBy, user.getUserType(), user.getBankId(), "Inactivation cancelled");

        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendInactivateCancelled(user.getEmail(), user.getFullName(), orgInfo[0], orgInfo[1]);
        } catch (Exception e) {
            logger.warn("sendInactivateCancelled failed: {}", e.getMessage());
        }

        notifyActor(undoneBy, "Inactivation Cancelled", user.getFullName(), user.getUsername(),
                LocalDateTime.now().format(FMT), user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Inactivation cancelled. User restored to ACTIVE.", null));
    }

    // ── Schedule Reactivate ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, String scheduledBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"INACTIVE".equals(user.getStatus())) {
            return fail("User must be INACTIVE to schedule reactivation. Current: " + user.getStatus());
        }

        String old = user.getStatus();
        user.setStatus("ACTIVE_PENDING");
        user.setReactivateScheduledAt(LocalDateTime.now());
        user.setReactivateScheduledBy(scheduledBy);
        user.setInactivateScheduledAt(null);
        user.setInactivateScheduledBy(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(scheduledBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", old, "ACTIVE_PENDING",
                scheduledBy, user.getUserType(), user.getBankId(), "Reactivation scheduled");

        String reactivateAt = LocalDateTime.now().plusSeconds(30).format(FMT);
        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendReactivatePendingNotification(user.getEmail(), user.getFullName(),
                    orgInfo[0], orgInfo[1], reactivateAt);
        } catch (Exception e) {
            logger.warn("sendReactivatePendingNotification failed: {}", e.getMessage());
        }

        notifyActor(scheduledBy, "Reactivation Scheduled", user.getFullName(), user.getUsername(), reactivateAt, user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Reactivation scheduled. User will become ACTIVE in ~30 seconds.", null));
    }

    // ── Undo Reactivate ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoReactivate(Long userId, String undoneBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"ACTIVE_PENDING".equals(user.getStatus()) || user.getReactivateScheduledAt() == null) {
            return fail("No pending reactivation found for this user.");
        }

        user.setStatus("INACTIVE");
        user.setReactivateScheduledAt(null);
        user.setReactivateScheduledBy(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(undoneBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", "ACTIVE_PENDING", "INACTIVE",
                undoneBy, user.getUserType(), user.getBankId(), "Reactivation cancelled");

        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendReactivateCancelled(user.getEmail(), user.getFullName(), orgInfo[0], orgInfo[1]);
        } catch (Exception e) {
            logger.warn("sendReactivateCancelled failed: {}", e.getMessage());
        }

        notifyActor(undoneBy, "Reactivation Cancelled", user.getFullName(), user.getUsername(),
                LocalDateTime.now().format(FMT), user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Reactivation cancelled. User remains INACTIVE.", null));
    }

    // ── Schedule Block ───────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, String reason, String scheduledBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if ("BLOCKED".equals(user.getStatus()) || "BLOCK_PENDING".equals(user.getStatus())) {
            return fail("User is already BLOCKED or BLOCK_PENDING.");
        }

        String old = user.getStatus();
        user.setPreBlockStatus(old);
        user.setStatus("BLOCK_PENDING");
        user.setBlockReason(reason);
        user.setBlockScheduledAt(LocalDateTime.now());
        user.setBlockScheduledBy(scheduledBy);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(scheduledBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", old, "BLOCK_PENDING",
                scheduledBy, user.getUserType(), user.getBankId(), "Block scheduled");

        String blockAt = LocalDateTime.now().plusHours(24).format(FMT);
        try {
            emailService.sendBlockWarning(user.getEmail(), user.getFullName(),
                    resolveOrgInfo(user)[0], resolveOrgInfo(user)[1], blockAt);
        } catch (Exception e) {
            logger.warn("sendBlockWarning failed: {}", e.getMessage());
        }

        // Notify delegatee if user has active delegation
        notifyDelegateeBlockPending(user, blockAt);

        notifyActor(scheduledBy, "Block Scheduled", user.getFullName(), user.getUsername(), blockAt, user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block scheduled. User will be BLOCKED in 24 hours.", null));
    }

    // ── Undo Block ───────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long userId, String undoneBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"BLOCK_PENDING".equals(user.getStatus())) {
            return fail("No pending block found for this user.");
        }

        String restore = user.getPreBlockStatus() != null ? user.getPreBlockStatus() : "ACTIVE";
        user.setStatus(restore);
        user.setPreBlockStatus(null);
        user.setBlockReason(null);
        user.setBlockScheduledAt(null);
        user.setBlockScheduledBy(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(undoneBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", "BLOCK_PENDING", restore,
                undoneBy, user.getUserType(), user.getBankId(), "Block cancelled");

        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendBlockCancelled(user.getEmail(), user.getFullName(),
                    orgInfo[0], orgInfo[1], restore);
        } catch (Exception e) {
            logger.warn("sendBlockCancelled failed: {}", e.getMessage());
        }

        notifyActor(undoneBy, "Block Cancelled", user.getFullName(), user.getUsername(),
                LocalDateTime.now().format(FMT), user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block cancelled. User restored to " + restore + ".", null));
    }

    // ── Immediate Block ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> blockImmediate(Long userId, String reason, String blockedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if ("BLOCKED".equals(user.getStatus())) {
            return fail("User is already BLOCKED.");
        }

        String old = user.getStatus();
        user.setPreBlockStatus(old);
        user.setStatus("BLOCKED");
        user.setBlockReason(reason);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(blockedBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", old, "BLOCKED",
                blockedBy, user.getUserType(), user.getBankId(), "Immediate block");

        replacementService.onOriginalBlocked(userId);
        try { delegationService.notifyDelegateeBlocked(userId); } catch (Exception e) { logger.warn("notifyDelegateeBlocked: {}", e.getMessage()); }

        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendBlockedNotification(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            logger.warn("sendBlockedNotification failed: {}", e.getMessage());
        }

        notifyActor(blockedBy, "Blocked", user.getFullName(), user.getUsername(),
                LocalDateTime.now().format(FMT), user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "User BLOCKED.", null));
    }

    // ── Unblock ──────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> unblock(Long userId, String unblockedBy) {
        Optional<ReconUser> opt = reconUserRepository.findById(userId);
        if (!opt.isPresent()) return notFound(userId);
        ReconUser user = opt.get();

        if (!"BLOCKED".equals(user.getStatus())) {
            return fail("User is not BLOCKED.");
        }

        String restore = user.getPreBlockStatus() != null ? user.getPreBlockStatus() : "ACTIVE";
        user.setStatus(restore);
        user.setPreBlockStatus(null);
        user.setBlockReason(null);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(unblockedBy);
        reconUserRepository.save(user);

        auditLog("RCN_RECON_USER", userId, "STATUS_CHANGE", "BLOCKED", restore,
                unblockedBy, user.getUserType(), user.getBankId(), "User unblocked");
        try { delegationService.notifyDelegateeUnblocked(userId); } catch (Exception e) { logger.warn("notifyDelegateeUnblocked: {}", e.getMessage()); }

        try {
            String[] orgInfo = resolveOrgInfo(user);
            emailService.sendBlockCancelled(user.getEmail(), user.getFullName(), orgInfo[0], orgInfo[1], restore);
        } catch (Exception e) {
            logger.warn("sendBlockCancelled failed: {}", e.getMessage());
        }

        notifyActor(unblockedBy, "Unblocked", user.getFullName(), user.getUsername(),
                LocalDateTime.now().format(FMT), user);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "User unblocked. Status restored to " + restore + ".", null));
    }

    // ── Private Helpers ──────────────────────────────────────────────────────────
    private String[] resolveOrgInfo(ReconUser user) {
        if (user.getBankId() != null) {
            Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findById(user.getBankId());
            if (bankOpt.isPresent()) {
                ReconBankMaster bank = bankOpt.get();
                return new String[]{bank.getBankName(), bank.getBankCode()};
            }
        }
        return new String[]{"ReconXpert.Ai", "SYSTEM"};
    }

    private void notifyActor(String actorUsername, String action, String targetName,
                              String targetCode, String scheduledAt, ReconUser context) {
        try {
            Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(actorUsername);
            actorOpt.ifPresent(actor -> {
                if (actor.getEmail() != null) {
                    emailService.sendActorActionConfirmation(actor.getEmail(), actor.getFullName(),
                            action, targetName, targetCode, scheduledAt);
                }
            });
        } catch (Exception e) {
            logger.warn("notifyActor email failed: {}", e.getMessage());
        }
    }

    private void notifyDelegateeBlockPending(ReconUser user, String blockAt) {
        try {
            delegationService.notifyDelegateeBlockPending(user.getUserId(), blockAt);
        } catch (Exception e) {
            logger.warn("notifyDelegateeBlockPending failed: {}", e.getMessage());
        }
    }

    private void auditLog(String table, Long recordId, String op, String oldVal, String newVal,
                           String actor, String actorType, Long bankId, String label) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(table); log.setRecordId(recordId);
            log.setOperation(op != null && op.length() > 10 ? op.substring(0, 10) : op);
            log.setOldValue(oldVal); log.setNewValue(newVal);
            log.setActorUsername(actor); log.setActorType(actorType); log.setBankId(bankId);
            log.setActionLabel(label); log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) { logger.warn("AuditLog save failed: {}", e.getMessage()); }
    }

    private ResponseEntity<RestWithStatusList> notFound(Long userId) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RestWithStatusList("FAILURE", "User not found: " + userId, null));
    }

    private ResponseEntity<RestWithStatusList> fail(String msg) {
        return ResponseEntity.ok(new RestWithStatusList("FAILURE", msg, null));
    }
}
