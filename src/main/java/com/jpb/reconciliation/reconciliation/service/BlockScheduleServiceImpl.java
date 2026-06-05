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
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;

@Service
public class BlockScheduleServiceImpl implements BlockScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(BlockScheduleServiceImpl.class);

    @Autowired
    private MainBankRepository mainBankRepository;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private BranchBankRepository branchBankRepository;

    @Autowired
    private BranchAdminRepository branchAdminRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK — Admin ne "Yes" kiya block popup mein
    // Status → BLOCK_PENDING (24 hrs ke baad permanent BLOCKED)
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long institutionId, String scheduledBy) {

        Optional<MainBank> opt = mainBankRepository.findById(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBank inst = opt.get();

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

        mainBankRepository.save(inst);
        // Sync BLOCK_PENDING to KAL_SUPER_USER
        try {
            mainAdminRepository.findByInstitutionCodeAndUsername(
                    inst.getInstitutionCode(), inst.getSuperUserId())
                .ifPresent(su -> { su.setStatus("BLOCK_PENDING"); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy(scheduledBy); mainAdminRepository.save(su); });
        } catch (Exception e) {
            logger.warn("scheduleBlock: KAL_SUPER_USER sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
        }
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

        // Individual block only — sub-institutes are NOT affected

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

        Optional<MainBank> opt = mainBankRepository.findById(institutionId);
        if (!opt.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBank inst = opt.get();

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

        mainBankRepository.save(inst);
        // Sync restored status to KAL_SUPER_USER
        final String finalStatus = restoredStatus;
        try {
            mainAdminRepository.findByInstitutionCodeAndUsername(
                    inst.getInstitutionCode(), inst.getSuperUserId())
                .ifPresent(su -> { su.setStatus(finalStatus); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy(undoneBy); mainAdminRepository.save(su); });
        } catch (Exception e) {
            logger.warn("undoBlock: KAL_SUPER_USER sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
        }
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

        // Individual undo only — sub-institutes are NOT affected

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Institution status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // AUTO-BLOCK — Runs every hour, checks 30s/24hr window
    // Permanently blocks institutions AND sub-institutions whose window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 3600000)   // DEMO: every 5s — change to 3600000 for production (1 hr)
    @Transactional
    public void autoBlockScheduledInstitutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);   // DEMO: 30s — change to minusHours(24) for production

        // ── Auto-block main institutions ──
        List<MainBank> pendingList = mainBankRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);

        if (!pendingList.isEmpty()) {
            logger.info("Auto-block: {} main institution(s) to be permanently blocked", pendingList.size());
            for (MainBank inst : pendingList) {
                inst.setStatus("BLOCKED");
                inst.setUpdatedAt(LocalDateTime.now());
                mainBankRepository.save(inst);
                // Sync BLOCKED to KAL_SUPER_USER
                try {
                    mainAdminRepository.findByInstitutionCodeAndUsername(
                            inst.getInstitutionCode(), inst.getSuperUserId())
                        .ifPresent(su -> { su.setStatus("BLOCKED"); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy("SYSTEM"); mainAdminRepository.save(su); });
                } catch (Exception e) {
                    logger.warn("autoBlock: KAL_SUPER_USER sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
                }
                logger.info("Auto-blocked institution: {} ({})",
                        inst.getInstitutionNameFull(), inst.getInstitutionId());

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
            }
        }

        // ── Auto-block sub-institutions ──
        List<BranchBank> pendingSubList = branchBankRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);

        if (!pendingSubList.isEmpty()) {
            logger.info("Auto-block: {} sub-institution(s) to be permanently blocked", pendingSubList.size());
            for (BranchBank inst : pendingSubList) {
                inst.setStatus("BLOCKED");
                inst.setUpdatedAt(LocalDateTime.now());
                branchBankRepository.save(inst);
                // Sync BLOCKED to BRANCH_ADMIN
                try {
                    branchAdminRepository.findByInstitutionCodeAndUsername(
                            inst.getInstitutionCode(), inst.getSuperUserId())
                        .ifPresent(ba -> { ba.setStatus("BLOCKED"); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy("SYSTEM"); branchAdminRepository.save(ba); });
                } catch (Exception e) {
                    logger.warn("autoBlock: BRANCH_ADMIN sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
                }
                logger.info("Auto-blocked sub-institution: {} ({})",
                        inst.getInstitutionNameFull(), inst.getInstitutionId());

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
                    logger.warn("Auto-block email failed for sub-institution {}: {}",
                                inst.getInstitutionCode(), e.getMessage());
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