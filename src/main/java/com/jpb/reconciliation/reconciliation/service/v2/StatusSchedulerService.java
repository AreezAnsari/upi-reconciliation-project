package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableScheduling
public class StatusSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(StatusSchedulerService.class);

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private AuditReplacementService replacementService;
    @Autowired private DelegationService delegationService;
    @Autowired private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ── Every 30 seconds: user transitions ──────────────────────────────────────
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processScheduledUserStatusChanges() {
        LocalDateTime now = LocalDateTime.now();

        // INACTIVE_PENDING → INACTIVE (30s after scheduling)
        List<ReconUser> toInactivate = reconUserRepository.findByStatus("INACTIVE_PENDING");
        for (ReconUser user : toInactivate) {
            if (user.getInactivateScheduledAt() == null) continue;
            if (user.getInactivateScheduledAt().plusSeconds(30).isAfter(now)) continue;

            Long userId = user.getUserId();
            String actor = user.getInactivateScheduledBy();
            user.setStatus("INACTIVE");
            user.setInactivateScheduledAt(null);
            user.setInactivateScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
            logger.info("Scheduler: user {} (id={}) → INACTIVE", user.getUsername(), userId);

            try { replacementService.finalizePendingReplacement(userId); }
            catch (Exception e) { logger.warn("finalizePendingReplacement failed for {}: {}", userId, e.getMessage()); }

            try { delegationService.delegateOnInactivate(userId, "SCHEDULER"); }
            catch (Exception e) { logger.warn("delegateOnInactivate failed for {}: {}", userId, e.getMessage()); }

            try { emailService.sendInactivatedNotification(user.getEmail(), user.getFullName(), resolveEntityName(user), resolveEntityCode(user)); }
            catch (Exception e) { logger.warn("sendInactivatedNotification failed: {}", e.getMessage()); }

            notifyActor(actor, "Inactivated", user.getFullName(), user.getUsername());
        }

        // ACTIVE_PENDING (reactivation) → ACTIVE (30s after scheduling)
        // First-login users have reactivateScheduledAt == null — skip those
        List<ReconUser> toReactivate = reconUserRepository.findByStatus("ACTIVE_PENDING");
        for (ReconUser user : toReactivate) {
            if (user.getReactivateScheduledAt() == null) continue; // first-login, not a scheduled reactivation
            if (user.getReactivateScheduledAt().plusSeconds(30).isAfter(now)) continue;

            Long userId = user.getUserId();
            String actor = user.getReactivateScheduledBy();
            user.setStatus("ACTIVE");
            user.setReactivateScheduledAt(null);
            user.setReactivateScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
            logger.info("Scheduler: user {} (id={}) → ACTIVE (reactivation)", user.getUsername(), userId);

            try { delegationService.restoreDelegationOnReactivate(userId, "SCHEDULER"); }
            catch (Exception e) { logger.warn("restoreDelegationOnReactivate failed for {}: {}", userId, e.getMessage()); }

            try { replacementService.onOriginalReactivated(userId); }
            catch (Exception e) { logger.warn("onOriginalReactivated failed for {}: {}", userId, e.getMessage()); }

            try { emailService.sendReactivatedNotification(user.getEmail(), user.getFullName(), resolveEntityName(user), resolveEntityCode(user), user.getUsername(), resolveLoginUrl(user)); }
            catch (Exception e) { logger.warn("sendReactivatedNotification failed: {}", e.getMessage()); }

            notifyActor(actor, "Reactivated", user.getFullName(), user.getUsername());
        }

        // BLOCK_PENDING → BLOCKED (24h after scheduling)
        List<ReconUser> toBlock = reconUserRepository.findByStatus("BLOCK_PENDING");
        for (ReconUser user : toBlock) {
            if (user.getBlockScheduledAt() == null) continue;
            if (user.getBlockScheduledAt().plusHours(24).isAfter(now)) continue;

            Long userId = user.getUserId();
            String actor = user.getBlockScheduledBy();
            user.setStatus("BLOCKED");
            user.setBlockScheduledAt(null);
            user.setBlockScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
            logger.info("Scheduler: user {} (id={}) → BLOCKED", user.getUsername(), userId);

            try { replacementService.onOriginalBlocked(userId); }
            catch (Exception e) { logger.warn("onOriginalBlocked failed for {}: {}", userId, e.getMessage()); }

            try { delegationService.notifyDelegateeBlocked(userId); }
            catch (Exception e) { logger.warn("notifyDelegateeBlocked failed: {}", e.getMessage()); }

            try { emailService.sendBlockedNotification(user.getEmail(), user.getFullName()); }
            catch (Exception e) { logger.warn("sendBlockedNotification failed: {}", e.getMessage()); }

            notifyActor(actor, "Blocked", user.getFullName(), user.getUsername());
        }
    }

    // ── Every 30 seconds: bank/branch transitions ────────────────────────────────
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processScheduledBankStatusChanges() {
        LocalDateTime now = LocalDateTime.now();

        // INACTIVE_PENDING → INACTIVE
        List<ReconBankMaster> toInactivate = reconBankMasterRepository.findByInactivateScheduledAtBefore(now);
        for (ReconBankMaster bank : toInactivate) {
            if ("INACTIVE".equals(bank.getStatus()) || "BLOCKED".equals(bank.getStatus())
                    || "BLOCK_PENDING".equals(bank.getStatus())) continue;
            String actor = bank.getInactivateScheduledBy();
            bank.setStatus("INACTIVE");
            bank.setInactivatedAt(now);
            bank.setInactivateScheduledAt(null);
            bank.setInactivateScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
            logger.info("Scheduler: bank {} → INACTIVE", bank.getBankCode());

            cascadeInactivateUsers(bank.getBankId(), now);

            notifyActor(actor, "Inactivated", bank.getBankName(), bank.getBankCode());
        }

        // ACTIVE_PENDING → ACTIVE
        List<ReconBankMaster> toReactivate = reconBankMasterRepository.findByReactivateScheduledAtBefore(now);
        for (ReconBankMaster bank : toReactivate) {
            if ("ACTIVE".equals(bank.getStatus()) || "BLOCKED".equals(bank.getStatus())
                    || "BLOCK_PENDING".equals(bank.getStatus())) continue;
            String actor = bank.getReactivateScheduledBy();
            bank.setStatus("ACTIVE");
            bank.setReactivateScheduledAt(null);
            bank.setReactivateScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
            logger.info("Scheduler: bank {} → ACTIVE", bank.getBankCode());

            cascadeReactivateUsers(bank.getBankId(), now);

            notifyActor(actor, "Reactivated", bank.getBankName(), bank.getBankCode());
        }

        // BLOCK_PENDING → BLOCKED (after blockScheduledAt)
        List<ReconBankMaster> toBlock = reconBankMasterRepository.findByBlockScheduledAtBefore(now);
        for (ReconBankMaster bank : toBlock) {
            if ("BLOCKED".equals(bank.getStatus())) continue;
            String actor = bank.getBlockScheduledBy();
            // preBlockStatus already saved at schedule time — don't overwrite with BLOCK_PENDING
            if (bank.getPreBlockStatus() == null) bank.setPreBlockStatus(bank.getStatus());
            bank.setStatus("BLOCKED");
            bank.setBlockScheduledAt(null);
            bank.setBlockScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
            logger.info("Scheduler: bank {} → BLOCKED", bank.getBankCode());

            cascadeBlockUsers(bank.getBankId(), now, bank.getBlockReason() != null ? bank.getBlockReason() : "Bank blocked");

            notifyActor(actor, "Blocked", bank.getBankName(), bank.getBankCode());
        }
    }

    // ── Cascade helpers ──────────────────────────────────────────────────────────
    private void cascadeInactivateUsers(Long bankId, LocalDateTime now) {
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if ("INACTIVE".equals(u.getStatus()) || "BLOCKED".equals(u.getStatus())) continue;
            u.setStatus("INACTIVE");
            u.setInactivateScheduledAt(null);
            u.setInactivateScheduledBy(null);
            u.setUpdatedAt(now);
            u.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(u);
            // This cascade is the bank/branch-triggered path (separate from the individual
            // user scheduler above) — a Bank/Branch Admin's own inactivation is scheduled
            // here, so any pending replacement for them must be finalized here too, or it
            // never fires when this scheduler job wins the race against the per-user one.
            try { replacementService.finalizePendingReplacement(u.getUserId()); }
            catch (Exception e) { logger.warn("cascade finalizePendingReplacement failed for {}: {}", u.getUserId(), e.getMessage()); }
            try { emailService.sendInactivatedNotification(u.getEmail(), u.getFullName(), resolveEntityName(u), resolveEntityCode(u)); }
            catch (Exception e) { logger.warn("cascade inactivate email failed: {}", e.getMessage()); }
        }
    }

    private void cascadeReactivateUsers(Long bankId, LocalDateTime now) {
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if (!"ACTIVE_PENDING".equals(u.getStatus()) && !"INACTIVE".equals(u.getStatus())) continue;
            // First-login users (ACTIVE_PENDING without scheduled reactivation) are not touched
            if ("ACTIVE_PENDING".equals(u.getStatus()) && u.getReactivateScheduledAt() == null) continue;
            u.setStatus("ACTIVE");
            u.setReactivateScheduledAt(null);
            u.setReactivateScheduledBy(null);
            u.setUpdatedAt(now);
            u.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(u);
            try { replacementService.onOriginalReactivated(u.getUserId()); }
            catch (Exception e) { logger.warn("cascade onOriginalReactivated failed for {}: {}", u.getUserId(), e.getMessage()); }
            try { emailService.sendReactivatedNotification(u.getEmail(), u.getFullName(), resolveEntityName(u), resolveEntityCode(u), u.getUsername(), resolveLoginUrl(u)); }
            catch (Exception e) { logger.warn("cascade reactivate email failed: {}", e.getMessage()); }
        }
    }

    private void cascadeBlockUsers(Long bankId, LocalDateTime now, String reason) {
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if ("BLOCKED".equals(u.getStatus())) continue;
            // preBlockStatus may already be saved at schedule time — don't overwrite with BLOCK_PENDING
            if (u.getPreBlockStatus() == null) u.setPreBlockStatus(u.getStatus());
            u.setStatus("BLOCKED");
            u.setBlockReason(reason);
            u.setBlockScheduledAt(null);
            u.setBlockScheduledBy(null);
            u.setUpdatedAt(now);
            u.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(u);
            try { replacementService.onOriginalBlocked(u.getUserId()); }
            catch (Exception e) { logger.warn("cascade onOriginalBlocked failed for {}: {}", u.getUserId(), e.getMessage()); }
            try { emailService.sendBlockedNotification(u.getEmail(), u.getFullName()); }
            catch (Exception e) { logger.warn("cascade block email failed: {}", e.getMessage()); }
        }
    }

    // Email to the actor (admin who scheduled the action) once the final status is applied
    private void notifyActor(String actorUsername, String action, String targetName, String targetCode) {
        try {
            if (actorUsername == null || "UNKNOWN".equals(actorUsername) || "SCHEDULER".equals(actorUsername)) return;
            String cleaned = actorUsername.startsWith("CASCADE:") ? actorUsername.substring(8) : actorUsername;
            java.util.Optional<ReconUser> actor = reconUserRepository.findByUsername(cleaned);
            if (!actor.isPresent()) actor = reconUserRepository.findByEmail(cleaned);
            if (actor.isPresent() && actor.get().getEmail() != null && !actor.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(actor.get().getEmail(),
                        actor.get().getFullName() != null ? actor.get().getFullName() : cleaned,
                        action, targetName, targetCode, null);
            }
        } catch (Exception e) {
            logger.warn("Scheduler actor email failed for {}: {}", actorUsername, e.getMessage());
        }
    }

    private String resolveEntityName(ReconUser user) {
        if (user.getBankId() != null) {
            return reconBankMasterRepository.findById(user.getBankId())
                    .map(ReconBankMaster::getBankName).orElse("ReconXpert.Ai");
        }
        return "ReconXpert.Ai";
    }

    private String resolveEntityCode(ReconUser user) {
        if (user.getBankId() != null) {
            return reconBankMasterRepository.findById(user.getBankId())
                    .map(ReconBankMaster::getBankCode).orElse("SYSTEM");
        }
        return "SYSTEM";
    }

    // Builds a real, role-aware login link (was previously sent as an empty string,
    // making the "Login to ReconXpert.Ai" button in the reactivation email a dead link).
    private String resolveLoginUrl(ReconUser user) {
        String code = resolveEntityCode(user);
        String username = user.getUsername() != null ? user.getUsername() : "";
        String enc = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        String userType = user.getUserType() != null ? user.getUserType() : "";
        switch (userType) {
            case "KAL_ADMIN":
                return frontendUrl + "/admin-login";
            case "BANK_ADMIN":
                return frontendUrl + "/bank-admin-login?bankCode=" + encCode + "&username=" + enc + "&mode=login";
            case "BRANCH_ADMIN":
                return frontendUrl + "/branch-admin-login?bankCode=" + encCode + "&username=" + enc + "&mode=login";
            default:
                return frontendUrl + "/user-verify?bankCode=" + encCode + "&username=" + enc + "&mode=login";
        }
    }
}
