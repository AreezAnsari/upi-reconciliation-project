package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
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
import com.jpb.reconciliation.reconciliation.entity.TestInstitution;
import com.jpb.reconciliation.reconciliation.repository.TestInstitutionRepository;

@Service
public class RetireScheduleServiceImpl implements RetireScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(RetireScheduleServiceImpl.class);

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

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

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Retire scheduled. Institution will be permanently retired in 30 seconds. You can undo this within 30 seconds.",
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
                LocalDateTime.now().isAfter(inst.getRetireScheduledAt().plusSeconds(30))) {
            return bad("Undo period has expired (30 seconds). Institution has been retired.");
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
    // AUTO-RETIRE — Runs every 5 seconds (DEMO MODE)
    // Retires institutions whose 30s window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoRetireScheduledInstitutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);

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