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
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
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
    private AddUserRepository addUserRepository;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK — Admin ne "Yes" kiya block popup mein
    // Status → BLOCK_PENDING (24 hrs ke baad permanent BLOCKED)
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, String scheduledBy) {

        Optional<MainBank> opt = mainBankRepository.findById(bankId);
        if (!opt.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        MainBank bnk = opt.get();

        if ("BLOCKED".equals(bnk.getStatus())) {
            return bad("This bank is already permanently BLOCKED.");
        }

        if ("BLOCK_PENDING".equals(bnk.getStatus())) {
            return bad("Block is already scheduled for this bank.");
        }

        // Save current status so we can undo within 24 hrs
        bnk.setPreBlockStatus(bnk.getStatus());
        bnk.setStatus("BLOCK_PENDING");
        bnk.setBlockScheduledAt(LocalDateTime.now());
        bnk.setBlockScheduledBy(scheduledBy);
        bnk.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(bnk);
        // Sync BLOCK_PENDING to BANK_ADMIN
        try {
            mainAdminRepository.findByBankCodeAndUsername(
                    bnk.getBankCode(), bnk.getBankAdminId())
                .ifPresent(su -> { su.setStatus("BLOCK_PENDING"); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy(scheduledBy); mainAdminRepository.save(su); });
        } catch (Exception e) {
            logger.warn("scheduleBlock: BANK_ADMIN sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
        }
        logger.info("Block scheduled for bank {} by {} at {}",
                bankId, scheduledBy, bnk.getBlockScheduledAt());

        // ── Formatted block time for emails ──
        String blockAtFormatted = bnk.getBlockScheduledAt()
                .plusSeconds(30)   // DEMO: 30s — change to plusHours(24) for production
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // ── Send warning email to Bank Super User ──
        try {
            if (bnk.getPrimaryEmail() != null && !bnk.getPrimaryEmail().isEmpty()) {
                emailService.sendBlockWarning(
                        bnk.getPrimaryEmail(),
                        bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Super User",
                        bnk.getBankNameFull(),
                        bnk.getBankCode(),
                        blockAtFormatted
                );
                logger.info("[BLOCK-WARN] Warning email sent to bank super user: {}", bnk.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[BLOCK-WARN] Warning email failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // Individual block only — branch banks are NOT affected

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block scheduled. Bank will be permanently blocked in 24 hours. You can undo this within 24 hours.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // UNDO BLOCK — Admin ne "Undo" kiya 24hrs ke andar
    // Status → wapas preBlockStatus
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long bankId, String undoneBy) {

        Optional<MainBank> opt = mainBankRepository.findById(bankId);
        if (!opt.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        MainBank bnk = opt.get();

        if (!"BLOCK_PENDING".equals(bnk.getStatus())) {
            return bad("No scheduled block found for this bank.");
        }

        if (bnk.getBlockScheduledAt() != null &&
                LocalDateTime.now().isAfter(bnk.getBlockScheduledAt().plusSeconds(30))) {   // DEMO: 30s — change to plusHours(24) for production
            return bad("Undo period has expired (30 seconds). Bank has been permanently blocked.");
        }

        String restoredStatus = bnk.getPreBlockStatus() != null ? bnk.getPreBlockStatus() : "ACTIVE";
        bnk.setStatus(restoredStatus);
        bnk.setBlockScheduledAt(null);
        bnk.setBlockScheduledBy(null);
        bnk.setPreBlockStatus(null);
        bnk.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(bnk);
        // Sync restored status to BANK_ADMIN
        final String finalStatus = restoredStatus;
        try {
            mainAdminRepository.findByBankCodeAndUsername(
                    bnk.getBankCode(), bnk.getBankAdminId())
                .ifPresent(su -> { su.setStatus(finalStatus); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy(undoneBy); mainAdminRepository.save(su); });
        } catch (Exception e) {
            logger.warn("undoBlock: BANK_ADMIN sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
        }
        logger.info("Block undone for bank {} by {}. Restored to {}", bankId, undoneBy, restoredStatus);

        // ── Send cancellation email to Bank Super User ──
        try {
            if (bnk.getPrimaryEmail() != null && !bnk.getPrimaryEmail().isEmpty()) {
                emailService.sendBlockCancelled(
                        bnk.getPrimaryEmail(),
                        bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Super User",
                        bnk.getBankNameFull(),
                        bnk.getBankCode(),
                        restoredStatus
                );
                logger.info("[UNDO-BLOCK] Cancellation email sent to super user: {}", bnk.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[UNDO-BLOCK] Cancellation email failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // Individual undo only — branch banks are NOT affected

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Bank status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // AUTO-BLOCK — Runs every hour, checks 30s/24hr window
    // Permanently blocks banks AND branch banks whose window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 3600000)   // DEMO: every 5s — change to 3600000 for production (1 hr)
    @Transactional
    public void autoBlockScheduledBanks() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);   // DEMO: 30s — change to minusHours(24) for production

        // ── Auto-block main banks ──
        List<MainBank> pendingList = mainBankRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);

        if (!pendingList.isEmpty()) {
            logger.info("Auto-block: {} main bank(s) to be permanently blocked", pendingList.size());
            for (MainBank bnk : pendingList) {
                bnk.setStatus("BLOCKED");
                bnk.setUpdatedAt(LocalDateTime.now());
                mainBankRepository.save(bnk);
                // Sync BLOCKED to BANK_ADMIN
                try {
                    mainAdminRepository.findByBankCodeAndUsername(
                            bnk.getBankCode(), bnk.getBankAdminId())
                        .ifPresent(su -> { su.setStatus("BLOCKED"); su.setUpdatedAt(LocalDateTime.now()); su.setUpdatedBy("SYSTEM"); mainAdminRepository.save(su); });
                } catch (Exception e) {
                    logger.warn("autoBlock: BANK_ADMIN sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
                }
                logger.info("Auto-blocked bank: {} ({})",
                        bnk.getBankNameFull(), bnk.getBankId());

                try {
                    if (bnk.getPrimaryEmail() != null && !bnk.getPrimaryEmail().isEmpty()) {
                        emailService.sendStatusChangeNotification(
                            bnk.getPrimaryEmail(),
                            bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Super User",
                            bnk.getBankNameFull(),
                            bnk.getBankCode(),
                            "BLOCK_PENDING",
                            "BLOCKED"
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block email failed for bank {}: {}",
                                bnk.getBankCode(), e.getMessage());
                }
            }
        }

        // ── Auto-block branch banks ──
        List<BranchBank> pendingSubList = branchBankRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);

        if (!pendingSubList.isEmpty()) {
            logger.info("Auto-block: {} branch bank(s) to be permanently blocked", pendingSubList.size());
            for (BranchBank bnk : pendingSubList) {
                bnk.setStatus("BLOCKED");
                bnk.setUpdatedAt(LocalDateTime.now());
                branchBankRepository.save(bnk);
                // Sync BLOCKED to BRANCH_ADMIN
                try {
                    branchAdminRepository.findByBranchCodeAndUsername(
                            bnk.getBranchCode(), bnk.getBranchAdminId())
                        .ifPresent(ba -> { ba.setStatus("BLOCKED"); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy("SYSTEM"); branchAdminRepository.save(ba); });
                } catch (Exception e) {
                    logger.warn("autoBlock: BRANCH_ADMIN sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
                }
                logger.info("Auto-blocked branch bank: {} ({})",
                        bnk.getBranchNameFull(), bnk.getBranchId());

                try {
                    if (bnk.getPrimaryEmail() != null && !bnk.getPrimaryEmail().isEmpty()) {
                        emailService.sendStatusChangeNotification(
                            bnk.getPrimaryEmail(),
                            bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Super User",
                            bnk.getBranchNameFull(),
                            bnk.getBranchCode(),
                            "BLOCK_PENDING",
                            "BLOCKED"
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block email failed for branch bank {}: {}",
                                bnk.getBranchCode(), e.getMessage());
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // AUTO-INACTIVATE / REACTIVATE — BranchBank + AddUser
    // Runs every 5 seconds (DEMO); production: increase fixedRate
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void autoProcessPendingStatuses() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);  // DEMO: 30s window

        // ── BranchBank: INACTIVE_PENDING → INACTIVE ──
        List<BranchBank> toInactivateBranch = branchBankRepository
                .findByStatusAndInactivateScheduledAtBefore("INACTIVE_PENDING", cutoff);
        for (BranchBank bnk : toInactivateBranch) {
            bnk.setStatus("INACTIVE");
            bnk.setInactivatedAt(LocalDateTime.now());
            bnk.setUpdatedAt(LocalDateTime.now());
            branchBankRepository.save(bnk);
            try {
                branchAdminRepository.findByBranchCodeAndUsername(bnk.getBranchCode(), bnk.getBranchAdminId())
                    .ifPresent(ba -> { ba.setStatus("INACTIVE"); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy("SYSTEM"); branchAdminRepository.save(ba); });
            } catch (Exception e) {
                logger.warn("autoInactivate BRANCH_ADMIN sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
            }
            logger.info("Auto-inactivated branch bank: {} ({})", bnk.getBranchNameFull(), bnk.getBranchId());
            try {
                if (bnk.getPrimaryEmail() != null) {
                    emailService.sendInactivatedNotification(bnk.getPrimaryEmail(),
                            bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Branch Admin",
                            bnk.getBranchNameFull(), bnk.getBranchCode());
                }
            } catch (Exception e) {
                logger.warn("Auto-inactivate email failed for branch bank {}: {}", bnk.getBranchCode(), e.getMessage());
            }
        }

        // ── BranchBank: ACTIVE_PENDING → ACTIVE ──
        List<BranchBank> toActivateBranch = branchBankRepository
                .findByStatusAndReactivateScheduledAtBefore("ACTIVE_PENDING", cutoff);
        for (BranchBank bnk : toActivateBranch) {
            bnk.setStatus("ACTIVE");
            bnk.setInactivateScheduledAt(null);
            bnk.setPreInactivateStatus(null);
            bnk.setUpdatedAt(LocalDateTime.now());
            branchBankRepository.save(bnk);
            try {
                branchAdminRepository.findByBranchCodeAndUsername(bnk.getBranchCode(), bnk.getBranchAdminId())
                    .ifPresent(ba -> { ba.setStatus("ACTIVE"); ba.setInactivateScheduledAt(null); ba.setPreInactivateStatus(null); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy("SYSTEM"); branchAdminRepository.save(ba); });
            } catch (Exception e) {
                logger.warn("autoReactivate BRANCH_ADMIN sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
            }
            logger.info("Auto-reactivated branch bank: {} ({})", bnk.getBranchNameFull(), bnk.getBranchId());
            try {
                if (bnk.getPrimaryEmail() != null) {
                    emailService.sendReactivatedNotification(bnk.getPrimaryEmail(),
                            bnk.getPrimaryFullName() != null ? bnk.getPrimaryFullName() : "Branch Admin",
                            bnk.getBranchNameFull(), bnk.getBranchCode());
                }
            } catch (Exception e) {
                logger.warn("Auto-reactivate email failed for branch bank {}: {}", bnk.getBranchCode(), e.getMessage());
            }
        }

        // ── AddUser: INACTIVE_PENDING → INACTIVE ──
        List<AddUser> toInactivateUsers = addUserRepository
                .findByStatusAndInactivateScheduledAtBefore(AddUser.UserStatus.INACTIVE_PENDING, cutoff);
        for (AddUser user : toInactivateUsers) {
            user.setStatus(AddUser.UserStatus.INACTIVE);
            addUserRepository.save(user);
            logger.info("Auto-inactivated user: {} ({})", user.getUsername(), user.getId());
            try {
                if (user.getEmail() != null) {
                    emailService.sendInactivatedNotification(user.getEmail(),
                            user.getFullName(), user.getUsername(), user.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Auto-inactivate email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        // ── AddUser: ACTIVE_PENDING → ACTIVE ──
        List<AddUser> toActivateUsers = addUserRepository
                .findByStatusAndReactivateScheduledAtBefore(AddUser.UserStatus.ACTIVE_PENDING, cutoff);
        for (AddUser user : toActivateUsers) {
            user.setStatus(AddUser.UserStatus.ACTIVE);
            user.setInactivateScheduledAt(null);
            user.setPreInactivateStatus(null);
            addUserRepository.save(user);
            logger.info("Auto-reactivated user: {} ({})", user.getUsername(), user.getId());
            try {
                if (user.getEmail() != null) {
                    emailService.sendReactivatedNotification(user.getEmail(),
                            user.getFullName(), user.getUsername(), user.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Auto-reactivate email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        // ── AddUser: BLOCK_PENDING → BLOCK ──
        List<AddUser> toBlockUsers = addUserRepository
                .findByStatusAndBlockScheduledAtBefore(AddUser.UserStatus.BLOCK_PENDING, cutoff);
        for (AddUser user : toBlockUsers) {
            user.setStatus(AddUser.UserStatus.BLOCK);
            user.setInactivateScheduledAt(null);
            user.setPreInactivateStatus(null);
            user.setReactivateScheduledAt(null);
            user.setPreReactivateStatus(null);
            addUserRepository.save(user);
            logger.info("Auto-blocked user: {} ({})", user.getUsername(), user.getId());
            try {
                if (user.getEmail() != null) {
                    emailService.sendStatusChangeNotification(user.getEmail(),
                            user.getFullName(), user.getUsername(), user.getUsername(),
                            "BLOCK_PENDING", "BLOCK");
                }
            } catch (Exception e) {
                logger.warn("Auto-block email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        // ── MainAdmin (BANK_ADMIN): INACTIVE_PENDING → INACTIVE ──
        List<MainAdmin> toInactivateBankAdmins = mainAdminRepository
                .findByStatusAndInactivateScheduledAtBefore("INACTIVE_PENDING", cutoff);
        for (MainAdmin admin : toInactivateBankAdmins) {
            admin.setStatus("INACTIVE");
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(admin);
            try {
                mainBankRepository.findByBankCode(admin.getBankCode())
                    .ifPresent(b -> { b.setStatus("INACTIVE"); mainBankRepository.save(b); });
            } catch (Exception e) {
                logger.warn("Auto-inactivate MainBank sync failed for {}: {}", admin.getBankCode(), e.getMessage());
            }
            logger.info("Auto-inactivated bank admin: {} ({})", admin.getUsername(), admin.getId());
        }

        // ── MainAdmin (BANK_ADMIN): ACTIVE_PENDING → ACTIVE ──
        List<MainAdmin> toActivateBankAdmins = mainAdminRepository
                .findByStatusAndReactivateScheduledAtBefore("ACTIVE_PENDING", cutoff);
        for (MainAdmin admin : toActivateBankAdmins) {
            admin.setStatus("ACTIVE");
            admin.setInactivateScheduledAt(null);
            admin.setPreInactivateStatus(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(admin);
            try {
                mainBankRepository.findByBankCode(admin.getBankCode())
                    .ifPresent(b -> { b.setStatus("ACTIVE"); mainBankRepository.save(b); });
            } catch (Exception e) {
                logger.warn("Auto-reactivate MainBank sync failed for {}: {}", admin.getBankCode(), e.getMessage());
            }
            logger.info("Auto-reactivated bank admin: {} ({})", admin.getUsername(), admin.getId());
        }

        // ── MainAdmin (BANK_ADMIN): BLOCK_PENDING → BLOCKED ──
        List<MainAdmin> toBlockBankAdmins = mainAdminRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);
        for (MainAdmin admin : toBlockBankAdmins) {
            admin.setStatus("BLOCKED");
            admin.setInactivateScheduledAt(null);
            admin.setPreInactivateStatus(null);
            admin.setReactivateScheduledAt(null);
            admin.setPreReactivateStatus(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(admin);
            logger.info("Auto-blocked bank admin: {} ({})", admin.getUsername(), admin.getId());
        }

        // ── BranchAdmin (BRANCH_ADMIN): INACTIVE_PENDING → INACTIVE ──
        List<BranchAdmin> toInactivateBranchAdmins = branchAdminRepository
                .findByStatusAndInactivateScheduledAtBefore("INACTIVE_PENDING", cutoff);
        for (BranchAdmin admin : toInactivateBranchAdmins) {
            admin.setStatus("INACTIVE");
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(admin);
            logger.info("Auto-inactivated branch admin: {} ({})", admin.getUsername(), admin.getId());
        }

        // ── BranchAdmin (BRANCH_ADMIN): ACTIVE_PENDING → ACTIVE ──
        List<BranchAdmin> toActivateBranchAdmins = branchAdminRepository
                .findByStatusAndReactivateScheduledAtBefore("ACTIVE_PENDING", cutoff);
        for (BranchAdmin admin : toActivateBranchAdmins) {
            admin.setStatus("ACTIVE");
            admin.setInactivateScheduledAt(null);
            admin.setPreInactivateStatus(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(admin);
            logger.info("Auto-reactivated branch admin: {} ({})", admin.getUsername(), admin.getId());
        }

        // ── BranchAdmin (BRANCH_ADMIN): BLOCK_PENDING → BLOCKED ──
        List<BranchAdmin> toBlockBranchAdmins = branchAdminRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);
        for (BranchAdmin admin : toBlockBranchAdmins) {
            admin.setStatus("BLOCKED");
            admin.setInactivateScheduledAt(null);
            admin.setPreInactivateStatus(null);
            admin.setReactivateScheduledAt(null);
            admin.setPreReactivateStatus(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(admin);
            logger.info("Auto-blocked branch admin: {} ({})", admin.getUsername(), admin.getId());
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