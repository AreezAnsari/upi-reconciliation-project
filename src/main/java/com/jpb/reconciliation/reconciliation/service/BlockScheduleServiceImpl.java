package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.SubTestInstitution;
import com.jpb.reconciliation.reconciliation.entity.TestInstitution;
import com.jpb.reconciliation.reconciliation.repository.SubTestInstitutionRepository;
import com.jpb.reconciliation.reconciliation.repository.TestInstitutionRepository;

@Service
public class BlockScheduleServiceImpl implements BlockScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(BlockScheduleServiceImpl.class);

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

    @Autowired
    private SubTestInstitutionRepository subTestInstitutionRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK — Admin ne "Yes" kiya block popup mein
    // Status → BLOCK_PENDING (24 hrs ke baad permanent BLOCKED)
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long institutionId, String scheduledBy) {

        Optional<TestInstitution> opt = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution inst = opt.get();

        if ("BLOCKED".equals(inst.getStatus())) {
            return bad("This institution is already permanently BLOCKED.");
        }

        if ("BLOCK_PENDING".equals(inst.getStatus())) {
            return bad("Block is already scheduled for this institution.");
        }

        // Save current status so we can undo within 24 hrs
        inst.setPreBlockStatus(inst.getStatus());
        inst.setStatus("BLOCK_PENDING");
        inst.setBlockScheduledAt(LocalDateTime.now());
        inst.setBlockScheduledBy(scheduledBy);
        inst.setUpdatedAt(LocalDateTime.now());

        testInstitutionRepository.save(inst);
        logger.info("Block scheduled for institution {} by {} at {}",
                institutionId, scheduledBy, inst.getBlockScheduledAt());

        // ── Formatted block time for emails ──
        String blockAtFormatted = inst.getBlockScheduledAt()
                .plusSeconds(30)   // DEMO: 30s — change to plusHours(24) for production
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // ── Send warning email to Institution Super User ──
        try {
            if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                emailService.sendRetireWarning(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        blockAtFormatted
                );
                logger.info("[BLOCK-WARN] Warning email sent to institution super user: {}", inst.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[BLOCK-WARN] Warning email failed for institution {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        // ── Set block schedule data + Send warning email to all Sub-Institutes ──
        List<SubTestInstitution> subs = subTestInstitutionRepository.findByParentInstitutionId(institutionId);
        logger.info("[BLOCK-WARN] Sending block warning to {} sub-institute(s) under institution {}",
                subs.size(), inst.getInstitutionCode());

        for (SubTestInstitution sub : subs) {
            if ("BLOCKED".equals(sub.getStatus())) continue;

            // ── Schedule ke time hi sub-institute mein bhi 3 fields save karo ──
            sub.setPreBlockStatus(sub.getStatus());
            sub.setBlockScheduledAt(inst.getBlockScheduledAt());
            sub.setBlockScheduledBy(scheduledBy);
            subTestInstitutionRepository.save(sub);
            logger.info("[BLOCK-WARN] Block schedule data saved for sub-institute: {} ({})",
                    sub.getInstitutionCode(), sub.getSubInstitutionId());

            try {
                if (sub.getPrimaryEmail() != null && !sub.getPrimaryEmail().isEmpty()) {
                    emailService.sendSubInstituteRetireWarning(
                            sub.getPrimaryEmail(),
                            sub.getPrimaryFullName() != null ? sub.getPrimaryFullName() : "Super User",
                            sub.getInstitutionNameFull() != null ? sub.getInstitutionNameFull() : sub.getInstitutionCode(),
                            sub.getInstitutionCode(),
                            inst.getInstitutionNameFull(),
                            inst.getInstitutionCode(),
                            blockAtFormatted
                    );
                    logger.info("[BLOCK-WARN] Warning email sent to sub-institute: {} ({})",
                            sub.getInstitutionCode(), sub.getPrimaryEmail());
                }
            } catch (Exception e) {
                logger.warn("[BLOCK-WARN] Warning email failed for sub-institute {} ({}): {}",
                        sub.getInstitutionCode(), sub.getSubInstitutionId(), e.getMessage());
            }
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block scheduled. Institution will be permanently blocked in 24 hours. You can undo this within 24 hours.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // UNDO BLOCK — Admin ne "Undo" kiya 24hrs ke andar
    // Status → wapas preRetireStatus
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long institutionId, String undoneBy) {

        Optional<TestInstitution> opt = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution inst = opt.get();

        if (!"BLOCK_PENDING".equals(inst.getStatus())) {
            return bad("No scheduled block found for this institution.");
        }

        if (inst.getBlockScheduledAt() != null &&
                LocalDateTime.now().isAfter(inst.getBlockScheduledAt().plusSeconds(30))) {   // DEMO: 30s — change to plusHours(24) for production
            return bad("Undo period has expired (30 seconds). Institution has been permanently blocked.");
        }

        String restoredStatus = inst.getPreBlockStatus() != null ? inst.getPreBlockStatus() : "ACTIVE";
        inst.setStatus(restoredStatus);
        inst.setBlockScheduledAt(null);
        inst.setBlockScheduledBy(null);
        inst.setPreBlockStatus(null);
        inst.setUpdatedAt(LocalDateTime.now());

        testInstitutionRepository.save(inst);
        logger.info("Block undone for institution {} by {}. Restored to {}", institutionId, undoneBy, restoredStatus);

        // ── Send cancellation email to Institution Super User ──
        try {
            if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                emailService.sendRetireCancelled(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        restoredStatus
                );
                logger.info("[UNDO-BLOCK] Cancellation email sent to super user: {}", inst.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[UNDO-BLOCK] Cancellation email failed for institution {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        // ── Undo: sub-institutes ke schedule fields clear karo + email bhejo ──
        List<SubTestInstitution> subs = subTestInstitutionRepository.findByParentInstitutionId(institutionId);
        for (SubTestInstitution sub : subs) {
            if ("BLOCKED".equals(sub.getStatus())) continue;

            // Schedule data clear karo — block cancel ho gaya
            sub.setBlockScheduledAt(null);
            sub.setBlockScheduledBy(null);
            sub.setPreBlockStatus(null);
            subTestInstitutionRepository.save(sub);
            logger.info("[UNDO-BLOCK] Schedule data cleared for sub-institute: {} ({})",
                    sub.getInstitutionCode(), sub.getSubInstitutionId());

            try {
                if (sub.getPrimaryEmail() != null && !sub.getPrimaryEmail().isEmpty()) {
                    emailService.sendSubInstituteRetireCancelled(
                            sub.getPrimaryEmail(),
                            sub.getPrimaryFullName() != null ? sub.getPrimaryFullName() : "Super User",
                            sub.getInstitutionNameFull() != null ? sub.getInstitutionNameFull() : sub.getInstitutionCode(),
                            sub.getInstitutionCode(),
                            inst.getInstitutionNameFull(),
                            inst.getInstitutionCode()
                    );
                    logger.info("[UNDO-BLOCK] Cancellation email sent to sub-institute: {} ({})",
                            sub.getInstitutionCode(), sub.getPrimaryEmail());
                }
            } catch (Exception e) {
                logger.warn("[UNDO-BLOCK] Cancellation email failed for sub-institute {} ({}): {}",
                        sub.getInstitutionCode(), sub.getSubInstitutionId(), e.getMessage());
            }
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Institution status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // AUTO-BLOCK — Runs every hour, checks 24hr window
    // Permanently blocks institutions whose 24hr window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 5000)   // DEMO: every 5s — change to 3600000 for production (1 hr)
    @Transactional
    public void autoBlockScheduledInstitutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);   // DEMO: 30s — change to minusHours(24) for production

        List<TestInstitution> pendingList = testInstitutionRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);

        if (pendingList.isEmpty()) return;

        logger.info("Auto-block: {} institution(s) to be permanently blocked", pendingList.size());

        for (TestInstitution inst : pendingList) {
            inst.setStatus("BLOCKED");
            // blockScheduledAt, blockScheduledBy, preBlockStatus — null mat karo
            // ye audit trail ke liye DB mein permanently rehenge
            inst.setUpdatedAt(LocalDateTime.now());
            testInstitutionRepository.save(inst);
            logger.info("Auto-blocked institution: {} ({})",
                    inst.getInstitutionNameFull(), inst.getInstitutionId());

            // ── Send BLOCKED notification email to Super User ──
            try {
                if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                    emailService.sendStatusChangeNotification(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        "BLOCK_PENDING",
                        "BLOCKED"
                    );
                }
            } catch (Exception e) {
                logger.warn("Auto-block email failed for institution {}: {}",
                            inst.getInstitutionCode(), e.getMessage());
            }

            // ── Cascade BLOCKED to all sub-institutes (permanent) ──
            List<SubTestInstitution> subs =
                    subTestInstitutionRepository.findByParentInstitutionId(inst.getInstitutionId());
            logger.info("[CASCADE] Auto-block: {} sub-institute(s) found under institution {} ({})",
                    subs.size(), inst.getInstitutionNameFull(), inst.getInstitutionCode());

            for (SubTestInstitution sub : subs) {
                if ("BLOCKED".equals(sub.getStatus())) continue;   // already blocked — skip

                String subOldStatus = sub.getStatus();
                sub.setPreBlockStatus(subOldStatus);
                sub.setStatus("BLOCKED");
                // Sub-institute mein bhi block audit data save karo
                sub.setBlockScheduledAt(inst.getBlockScheduledAt());
                sub.setBlockScheduledBy(inst.getBlockScheduledBy());
                subTestInstitutionRepository.save(sub);
                logger.info("[CASCADE] Auto-block: sub-institute {} ({}) → BLOCKED (was: {})",
                        sub.getInstitutionCode(), sub.getSubInstitutionId(), subOldStatus);

                // Send email to sub-institute Super User
                try {
                    if (sub.getPrimaryEmail() != null && !sub.getPrimaryEmail().isEmpty()) {
                        emailService.sendSubInstituteStatusNotification(
                            sub.getPrimaryEmail(),
                            sub.getPrimaryFullName() != null ? sub.getPrimaryFullName() : "Super User",
                            sub.getInstitutionNameFull() != null
                                    ? sub.getInstitutionNameFull() : sub.getInstitutionCode(),
                            sub.getInstitutionCode(),
                            subOldStatus, "BLOCKED",
                            inst.getInstitutionNameFull(),
                            inst.getInstitutionCode()
                        );
                    }
                } catch (Exception e) {
                    logger.warn("[CASCADE-EMAIL] Auto-block email failed for sub-institute {} ({}): {}",
                            sub.getInstitutionCode(), sub.getSubInstitutionId(), e.getMessage());
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestWithStatusList("FAILURE", msg, new ArrayList<>()));
    }
}