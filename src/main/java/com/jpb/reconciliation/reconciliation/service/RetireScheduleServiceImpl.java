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
public class RetireScheduleServiceImpl implements RetireScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(RetireScheduleServiceImpl.class);

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

    @Autowired
    private SubTestInstitutionRepository subTestInstitutionRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────
    // SCHEDULE RETIRE — Admin ne "Yes" kiya retire popup mein
    // Status → RETIRE_PENDING (30s ke liye — DEMO MODE)
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleRetire(Long institutionId, String scheduledBy) {

        Optional<TestInstitution> opt = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution inst = opt.get();

        if ("RETIRED".equals(inst.getStatus())) {
            return bad("This institution is already RETIRED.");
        }

        if ("RETIRE_PENDING".equals(inst.getStatus())) {
            return bad("Retire is already scheduled for this institution.");
        }

        // Save current status so we can undo
        inst.setPreRetireStatus(inst.getStatus());
        inst.setStatus("RETIRE_PENDING");
        inst.setRetireScheduledAt(LocalDateTime.now());
        inst.setRetireScheduledBy(scheduledBy);
        inst.setUpdatedAt(LocalDateTime.now());

        testInstitutionRepository.save(inst);
        logger.info("Retire scheduled for institution {} by {} at {}",
                institutionId, scheduledBy, inst.getRetireScheduledAt());

        // ── Formatted retire time for emails ──
        String retireAtFormatted = inst.getRetireScheduledAt()
                .plusHours(24)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // ── Send warning email to Institution Super User ──
        try {
            if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                emailService.sendRetireWarning(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        retireAtFormatted
                );
                logger.info("[RETIRE-WARN] Warning email sent to institution super user: {}", inst.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[RETIRE-WARN] Warning email failed for institution {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        // ── Send warning email to all Sub-Institutes ──
        List<SubTestInstitution> subs = subTestInstitutionRepository.findByParentInstitutionId(institutionId);
        logger.info("[RETIRE-WARN] Sending retirement warning to {} sub-institute(s) under institution {}",
                subs.size(), inst.getInstitutionCode());

        for (SubTestInstitution sub : subs) {
            if ("RETIRED".equals(sub.getStatus())) continue;
            try {
                if (sub.getPrimaryEmail() != null && !sub.getPrimaryEmail().isEmpty()) {
                    emailService.sendSubInstituteRetireWarning(
                            sub.getPrimaryEmail(),
                            sub.getPrimaryFullName() != null ? sub.getPrimaryFullName() : "Super User",
                            sub.getInstitutionNameFull() != null ? sub.getInstitutionNameFull() : sub.getInstitutionCode(),
                            sub.getInstitutionCode(),
                            inst.getInstitutionNameFull(),
                            inst.getInstitutionCode(),
                            retireAtFormatted
                    );
                    logger.info("[RETIRE-WARN] Warning email sent to sub-institute: {} ({})",
                            sub.getInstitutionCode(), sub.getPrimaryEmail());
                }
            } catch (Exception e) {
                logger.warn("[RETIRE-WARN] Warning email failed for sub-institute {} ({}): {}",
                        sub.getInstitutionCode(), sub.getSubInstitutionId(), e.getMessage());
            }
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Retire scheduled. Institution will be permanently retired in 24 hours. You can undo this within 24 hours.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // UNDO RETIRE — Admin ne "Undo" kiya 30s ke andar
    // Status → wapas preRetireStatus
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoRetire(Long institutionId, String undoneBy) {

        Optional<TestInstitution> opt = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution inst = opt.get();

        if (!"RETIRE_PENDING".equals(inst.getStatus())) {
            return bad("No scheduled retire found for this institution.");
        }

        if (inst.getRetireScheduledAt() != null &&
                LocalDateTime.now().isAfter(inst.getRetireScheduledAt().plusHours(24))) {
            return bad("Undo period has expired (24 hours). Institution has been retired.");
        }

        String restoredStatus = inst.getPreRetireStatus() != null ? inst.getPreRetireStatus() : "ACTIVE";
        inst.setStatus(restoredStatus);
        inst.setRetireScheduledAt(null);
        inst.setRetireScheduledBy(null);
        inst.setPreRetireStatus(null);
        inst.setUpdatedAt(LocalDateTime.now());

        testInstitutionRepository.save(inst);
        logger.info("Retire undone for institution {} by {}. Restored to {}", institutionId, undoneBy, restoredStatus);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Retire has been cancelled. Institution status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // AUTO-RETIRE — Runs every 24 hours
    // Retires institutions whose 24hr window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 86400000)   // runs every 24 hours
    @Transactional
    public void autoRetireScheduledInstitutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<TestInstitution> pendingList = testInstitutionRepository
                .findByStatusAndRetireScheduledAtBefore("RETIRE_PENDING", cutoff);

        if (pendingList.isEmpty()) return;

        logger.info("Auto-retire: {} institution(s) to be retired", pendingList.size());

        for (TestInstitution inst : pendingList) {
            inst.setStatus("RETIRED");
            inst.setUpdatedAt(LocalDateTime.now());
            testInstitutionRepository.save(inst);
            logger.info("Auto-retired institution: {} ({})",
                    inst.getInstitutionNameFull(), inst.getInstitutionId());

            // ── Send RETIRED notification email to Super User ──
            try {
                if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                    emailService.sendStatusChangeNotification(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        "RETIRE_PENDING",
                        "RETIRED"
                    );
                }
            } catch (Exception e) {
                logger.warn("Auto-retire email failed for institution {}: {}",
                            inst.getInstitutionCode(), e.getMessage());
            }

            // ── Cascade RETIRED to all sub-institutes ──
            List<SubTestInstitution> subs =
                    subTestInstitutionRepository.findByParentInstitutionId(inst.getInstitutionId());
            logger.info("[CASCADE] Auto-retire: {} sub-institute(s) found under institution {} ({})",
                    subs.size(), inst.getInstitutionNameFull(), inst.getInstitutionCode());

            for (SubTestInstitution sub : subs) {
                if ("RETIRED".equals(sub.getStatus())) continue;   // already retired — skip

                String subOldStatus = sub.getStatus();
                sub.setPreBlockStatus(null);   // RETIRED is permanent — clear any saved state
                sub.setStatus("RETIRED");
                subTestInstitutionRepository.save(sub);
                logger.info("[CASCADE] Auto-retire: sub-institute {} ({}) → RETIRED (was: {})",
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
                            subOldStatus, "RETIRED",
                            inst.getInstitutionNameFull(),
                            inst.getInstitutionCode()
                        );
                    }
                } catch (Exception e) {
                    logger.warn("[CASCADE-EMAIL] Auto-retire email failed for sub-institute {} ({}): {}",
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