package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.entity.UserDelegation;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.UserDelegationRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
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

    @Autowired
    private AdminReplacementRepository adminReplacementRepository;

    @Autowired
    private UserDelegationRepository userDelegationRepository;

    @Autowired
    private AdminReplacementService adminReplacementService;

    @Autowired
    private KalAdminRepository kalAdminRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // Set to true when any pending status is scheduled anywhere in the app.
    // The scheduler checks this before touching the DB — 0 queries when idle.
    static volatile boolean hasPendingWork = false;

    public static void flagPendingWork() {
        hasPendingWork = true;
    }

    @javax.annotation.PostConstruct
    public void initPendingFlag() {
        hasPendingWork =
            addUserRepository.existsByStatusIn(Arrays.asList(
                AddUser.UserStatus.INACTIVE_PENDING, AddUser.UserStatus.ACTIVE_PENDING, AddUser.UserStatus.BLOCK_PENDING)) ||
            mainAdminRepository.existsByStatusIn(Arrays.asList("INACTIVE_PENDING", "ACTIVE_PENDING", "BLOCK_PENDING")) ||
            branchAdminRepository.existsByStatusIn(Arrays.asList("INACTIVE_PENDING", "ACTIVE_PENDING", "BLOCK_PENDING"));
    }

    // ─────────────────────────────────────────────
    // SCHEDULE BLOCK — Admin ne "Yes" kiya block popup mein
    // Status → BLOCK_PENDING (24 hrs ke baad permanent BLOCKED)
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, String scheduledBy, String reason) {

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
        bnk.setBlockReason(reason);
        bnk.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(bnk);
        // Sync BLOCK_PENDING to ALL MainAdmins for this bank (current + INACTIVE replaced originals)
        try {
            List<MainAdmin> allBankAdmins = mainAdminRepository.findByBankCode(bnk.getBankCode());
            for (MainAdmin su : allBankAdmins) {
                if (!"BLOCKED".equalsIgnoreCase(su.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(su.getStatus())) {
                    su.setPreBlockStatus(su.getStatus());
                    su.setStatus("BLOCK_PENDING");
                    su.setBlockScheduledAt(LocalDateTime.now());
                    su.setBlockScheduledBy(scheduledBy);
                    su.setBlockReason(reason);
                    su.setUpdatedAt(LocalDateTime.now());
                    su.setUpdatedBy(scheduledBy);
                    mainAdminRepository.save(su);
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock: BANK_ADMIN sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
        }
        // Cascade BLOCK_PENDING to all BranchBanks and their admins
        try {
            List<BranchBank> branches = branchBankRepository.findByParentBankId(bnk.getBankId());
            for (BranchBank branch : branches) {
                if (!"BLOCKED".equalsIgnoreCase(branch.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(branch.getStatus())) {
                    branch.setPreBlockStatus(branch.getStatus());
                    branch.setStatus("BLOCK_PENDING");
                    branch.setBlockScheduledAt(LocalDateTime.now());
                    branch.setBlockScheduledBy(scheduledBy);
                    branch.setBlockReason(reason);
                    branch.setUpdatedAt(LocalDateTime.now());
                    branchBankRepository.save(branch);
                    // Block ALL admins for this branch (current + INACTIVE replaced originals)
                    List<BranchAdmin> allBranchAdmins = branchAdminRepository.findByBranchCode(branch.getBranchCode());
                    for (BranchAdmin ba : allBranchAdmins) {
                        if (!"BLOCKED".equalsIgnoreCase(ba.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(ba.getStatus())) {
                            ba.setPreBlockStatus(ba.getStatus());
                            ba.setStatus("BLOCK_PENDING");
                            ba.setBlockScheduledAt(LocalDateTime.now());
                            ba.setBlockScheduledBy(scheduledBy);
                            ba.setBlockReason(reason);
                            ba.setInactivateScheduledAt(null);
                            ba.setInactivateScheduledBy(null);
                            ba.setReactivateScheduledAt(null);
                            ba.setReactivateScheduledBy(null);
                            ba.setUpdatedAt(LocalDateTime.now());
                            ba.setUpdatedBy(scheduledBy);
                            branchAdminRepository.save(ba);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock: branch cascade failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }
        hasPendingWork = true;
        logger.info("Block scheduled for bank {} by {} at {}",
                bankId, scheduledBy, bnk.getBlockScheduledAt());

        // ── Formatted block time for emails ──
        String blockAtFormatted = bnk.getBlockScheduledAt()
                .plusSeconds(30)   // DEMO: 30s — change to plusHours(24) for production
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // Cascade BLOCK_PENDING to all AddUsers under this bank + send warning emails
        try {
            List<AddUser> bankUsers = addUserRepository.findByBankCode(bnk.getBankCode());
            for (AddUser user : bankUsers) {
                if (user.getStatus() != AddUser.UserStatus.BLOCKED && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
                    user.setPreBlockStatus(user.getStatus().name());
                    user.setStatus(AddUser.UserStatus.BLOCK_PENDING);
                    user.setBlockScheduledAt(LocalDateTime.now());
                    user.setBlockScheduledBy("CASCADE:" + scheduledBy);
                    user.setBlockReason(reason);
                    user.setInactivateScheduledAt(null);
                    user.setInactivateScheduledBy(null);
                    user.setReactivateScheduledAt(null);
                    user.setReactivateScheduledBy(null);
                    addUserRepository.save(user);
                    try {
                        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                            emailService.sendBlockWarning(user.getEmail(),
                                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                    bnk.getBankNameFull(), bnk.getBankCode(), blockAtFormatted);
                        }
                    } catch (Exception emailEx) {
                        logger.warn("scheduleBlock: warning email failed for user {}: {}", user.getUsername(), emailEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock: user cascade failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // ── Send warning email to Bank Admin (routes to replacement if original is inactive) ──
        try {
            String[] bankContact = resolveMainBankContact(bnk);
            if (bankContact[0] != null && !bankContact[0].isEmpty()) {
                emailService.sendBlockWarning(bankContact[0], bankContact[1],
                        bnk.getBankNameFull(), bnk.getBankCode(), blockAtFormatted);
                logger.info("[BLOCK-WARN] Warning email sent to: {}", bankContact[0]);
            }
        } catch (Exception e) {
            logger.warn("[BLOCK-WARN] Warning email failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // ── Send warning emails to all branch admins under this bank ──
        try {
            List<BranchBank> branches = branchBankRepository.findByParentBankId(bnk.getBankId());
            for (BranchBank branch : branches) {
                // Use preBlockStatus for INACTIVE check: by the time email loop runs,
                // cascade loop has already changed INACTIVE branch status to BLOCK_PENDING.
                String priorStatus = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : branch.getStatus();
                if ("BLOCKED".equalsIgnoreCase(priorStatus) || "INACTIVE".equalsIgnoreCase(priorStatus)) continue;
                try {
                    String[] branchContact = resolveBranchBankContact(branch);
                    if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                        emailService.sendBlockWarning(branchContact[0], branchContact[1],
                                branch.getBranchNameFull(), branch.getBranchCode(), blockAtFormatted);
                    }
                } catch (Exception emailEx) {
                    logger.warn("scheduleBlock: warning email failed for branch {}: {}", branch.getBranchCode(), emailEx.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock: branch admin email cascade failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // ── Notify all approved Kal Admins that block was scheduled ──
        try {
            List<KalAdmin> kalAdmins = kalAdminRepository.findByApprovedYn("Y");
            for (KalAdmin ka : kalAdmins) {
                if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                    emailService.sendStatusChangeNotification(ka.getEmailId(),
                            ka.getUserName() != null ? ka.getUserName() : "Kal Admin",
                            bnk.getBankNameFull(), bnk.getBankCode(), "ACTIVE", "BLOCK_PENDING");
                }
            }
        } catch (Exception e) {
            logger.warn("scheduleBlock: Kal Admin notification failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

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
        bnk.setBlockReason(null);
        bnk.setPreBlockStatus(null);
        bnk.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(bnk);
        // Restore ALL MainAdmins for this bank (current + INACTIVE replaced originals)
        final String finalStatus = restoredStatus;
        try {
            List<MainAdmin> allBankAdmins = mainAdminRepository.findByBankCode(bnk.getBankCode());
            for (MainAdmin su : allBankAdmins) {
                if ("BLOCK_PENDING".equalsIgnoreCase(su.getStatus())) {
                    String adminRestored = su.getPreBlockStatus() != null ? su.getPreBlockStatus() : finalStatus;
                    su.setStatus(adminRestored);
                    su.setBlockScheduledAt(null);
                    su.setBlockScheduledBy(null);
                    su.setBlockReason(null);
                    su.setPreBlockStatus(null);
                    su.setUpdatedAt(LocalDateTime.now());
                    su.setUpdatedBy(undoneBy);
                    mainAdminRepository.save(su);
                }
            }
        } catch (Exception e) {
            logger.warn("undoBlock: BANK_ADMIN sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // Restore all BLOCK_PENDING branch banks + branch admins + send cancellation emails
        try {
            List<BranchBank> branches = branchBankRepository.findByParentBankId(bnk.getBankId());
            for (BranchBank branch : branches) {
                if ("BLOCK_PENDING".equalsIgnoreCase(branch.getStatus())) {
                    String branchRestored = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : "ACTIVE";
                    branch.setStatus(branchRestored);
                    branch.setBlockScheduledAt(null);
                    branch.setBlockScheduledBy(null);
                    branch.setBlockReason(null);
                    branch.setPreBlockStatus(null);
                    branch.setUpdatedAt(LocalDateTime.now());
                    branchBankRepository.save(branch);
                    // Restore ALL admins for this branch (current + INACTIVE replaced originals)
                    final String branchRestoredFinal = branchRestored;
                    try {
                        List<BranchAdmin> allBranchAdmins = branchAdminRepository.findByBranchCode(branch.getBranchCode());
                        for (BranchAdmin ba : allBranchAdmins) {
                            if ("BLOCK_PENDING".equalsIgnoreCase(ba.getStatus())) {
                                String baRestored = ba.getPreBlockStatus() != null ? ba.getPreBlockStatus() : branchRestoredFinal;
                                ba.setStatus(baRestored);
                                ba.setBlockScheduledAt(null);
                                ba.setBlockScheduledBy(null);
                                ba.setBlockReason(null);
                                ba.setPreBlockStatus(null);
                                ba.setUpdatedAt(LocalDateTime.now());
                                ba.setUpdatedBy(undoneBy);
                                branchAdminRepository.save(ba);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("undoBlock: branch admin restore failed for {}: {}", branch.getBranchCode(), e.getMessage());
                    }
                    // Send cancellation email to branch admin (routes to replacement if original is inactive)
                    try {
                        String[] branchContact = resolveBranchBankContact(branch);
                        if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                            emailService.sendBlockCancelled(branchContact[0], branchContact[1],
                                    branch.getBranchNameFull(), branch.getBranchCode(), branchRestored);
                        }
                    } catch (Exception emailEx) {
                        logger.warn("undoBlock: cancellation email failed for branch {}: {}", branch.getBranchCode(), emailEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("undoBlock: branch cascade restore failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // Restore all BLOCK_PENDING users under this bank + send cancellation emails
        try {
            List<AddUser> bankUsers = addUserRepository.findByBankCode(bnk.getBankCode());
            for (AddUser user : bankUsers) {
                if (user.getStatus() == AddUser.UserStatus.BLOCK_PENDING) {
                    String userRestored = user.getPreBlockStatus() != null ? user.getPreBlockStatus() : "ACTIVE";
                    user.setStatus(AddUser.UserStatus.valueOf(userRestored));
                    user.setPreBlockStatus(null);
                    user.setBlockScheduledAt(null);
                    user.setBlockScheduledBy(null);
                    user.setBlockReason(null);
                    addUserRepository.save(user);
                    try {
                        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                            emailService.sendBlockCancelled(user.getEmail(),
                                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                    bnk.getBankNameFull(), bnk.getBankCode(), userRestored);
                        }
                    } catch (Exception emailEx) {
                        logger.warn("undoBlock: cancellation email failed for user {}: {}", user.getUsername(), emailEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("undoBlock: user restore failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        logger.info("Block undone for bank {} by {}. Restored to {}", bankId, undoneBy, restoredStatus);

        // ── Send cancellation email to Bank Admin (routes to replacement if original is inactive) ──
        try {
            String[] bankContact = resolveMainBankContact(bnk);
            if (bankContact[0] != null && !bankContact[0].isEmpty()) {
                emailService.sendBlockCancelled(bankContact[0], bankContact[1],
                        bnk.getBankNameFull(), bnk.getBankCode(), restoredStatus);
                logger.info("[UNDO-BLOCK] Cancellation email sent to: {}", bankContact[0]);
            }
        } catch (Exception e) {
            logger.warn("[UNDO-BLOCK] Cancellation email failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        // ── Notify all approved Kal Admins that block was cancelled ──
        try {
            List<KalAdmin> kalAdmins = kalAdminRepository.findByApprovedYn("Y");
            for (KalAdmin ka : kalAdmins) {
                if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                    emailService.sendStatusChangeNotification(ka.getEmailId(),
                            ka.getUserName() != null ? ka.getUserName() : "Kal Admin",
                            bnk.getBankNameFull(), bnk.getBankCode(), "BLOCK_PENDING", restoredStatus);
                }
            }
        } catch (Exception e) {
            logger.warn("undoBlock: Kal Admin notification failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Bank status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────
    // AUTO-BLOCK — Runs every hour, checks 30s/24hr window
    // Permanently blocks banks AND branch banks whose window has passed
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 3600000)   // every 1 hour — change to lower value for demo/testing
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

                // Part 2: sync Main_Bank contact to replacement admin on permanent block
                try {
                    if (bnk.getBankAdminId() != null) {
                        Optional<MainAdmin> origAdminOpt = mainAdminRepository.findByBankCodeAndUsername(
                                bnk.getBankCode(), bnk.getBankAdminId());
                        if (origAdminOpt.isPresent()) {
                            MainAdmin origAdmin = origAdminOpt.get();
                            Optional<AdminReplacement> repOpt = adminReplacementRepository
                                    .findByOriginalEntityIdAndEntityTypeAndStatus(origAdmin.getId(), "MAIN_ADMIN", "ACTIVE");
                            if (repOpt.isPresent() && repOpt.get().getReplacementEntityId() > 0) {
                                Optional<MainAdmin> repAdminOpt = mainAdminRepository.findById(repOpt.get().getReplacementEntityId());
                                if (repAdminOpt.isPresent()) {
                                    MainAdmin repAdmin = repAdminOpt.get();
                                    AdminReplacement rep = repOpt.get();
                                    bnk.setPrimaryEmail(repAdmin.getEmail());
                                    bnk.setPrimaryFullName(rep.getPendingFullName() != null ? rep.getPendingFullName() : repAdmin.getUsername());
                                    bnk.setPrimaryMobile(rep.getPendingMobile());
                                    bnk.setBankAdminId(repAdmin.getUsername());
                                    mainBankRepository.save(bnk);
                                    logger.info("[BLOCK-SYNC] Main_Bank {} contact updated to replacement: {}", bnk.getBankCode(), repAdmin.getUsername());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[BLOCK-SYNC] Main_Bank contact sync failed for {}: {}", bnk.getBankCode(), e.getMessage());
                }
                try {
                    String[] bankContact = resolveMainBankContact(bnk);
                    if (bankContact[0] != null && !bankContact[0].isEmpty()) {
                        emailService.sendStatusChangeNotification(bankContact[0], bankContact[1],
                                bnk.getBankNameFull(), bnk.getBankCode(), "BLOCK_PENDING", "BLOCKED");
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block email failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
                }
                // ── Notify all Kal Admins of permanent main bank block ──
                try {
                    List<KalAdmin> kalAdmins = kalAdminRepository.findByApprovedYn("Y");
                    for (KalAdmin ka : kalAdmins) {
                        if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                            emailService.sendStatusChangeNotification(ka.getEmailId(),
                                    ka.getUserName() != null ? ka.getUserName() : "Kal Admin",
                                    bnk.getBankNameFull(), bnk.getBankCode(), "BLOCK_PENDING", "BLOCKED");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block: Kal Admin notification failed for bank {}: {}", bnk.getBankCode(), e.getMessage());
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

                // Part 2: sync Branch_Bank contact to replacement admin on permanent block
                try {
                    if (bnk.getBranchAdminId() != null) {
                        Optional<BranchAdmin> origAdminOpt = branchAdminRepository.findByBranchCodeAndUsername(
                                bnk.getBranchCode(), bnk.getBranchAdminId());
                        if (origAdminOpt.isPresent()) {
                            BranchAdmin origAdmin = origAdminOpt.get();
                            Optional<AdminReplacement> repOpt = adminReplacementRepository
                                    .findByOriginalEntityIdAndEntityTypeAndStatus(origAdmin.getId(), "BRANCH_ADMIN", "ACTIVE");
                            if (repOpt.isPresent() && repOpt.get().getReplacementEntityId() > 0) {
                                Optional<BranchAdmin> repAdminOpt = branchAdminRepository.findById(repOpt.get().getReplacementEntityId());
                                if (repAdminOpt.isPresent()) {
                                    BranchAdmin repAdmin = repAdminOpt.get();
                                    AdminReplacement rep = repOpt.get();
                                    bnk.setPrimaryEmail(repAdmin.getEmail());
                                    bnk.setPrimaryFullName(rep.getPendingFullName() != null ? rep.getPendingFullName() : repAdmin.getUsername());
                                    bnk.setPrimaryMobile(rep.getPendingMobile());
                                    bnk.setBranchAdminId(repAdmin.getUsername());
                                    branchBankRepository.save(bnk);
                                    logger.info("[BLOCK-SYNC] Branch_Bank {} contact updated to replacement: {}", bnk.getBranchCode(), repAdmin.getUsername());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[BLOCK-SYNC] Branch_Bank contact sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
                }
                try {
                    String[] branchContact = resolveBranchBankContact(bnk);
                    if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                        emailService.sendStatusChangeNotification(branchContact[0], branchContact[1],
                                bnk.getBranchNameFull(), bnk.getBranchCode(), "BLOCK_PENDING", "BLOCKED");
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block email failed for branch bank {}: {}", bnk.getBranchCode(), e.getMessage());
                }
                // ── Notify all Kal Admins of permanent branch bank block ──
                try {
                    List<KalAdmin> kalAdmins = kalAdminRepository.findByApprovedYn("Y");
                    for (KalAdmin ka : kalAdmins) {
                        if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                            emailService.sendStatusChangeNotification(ka.getEmailId(),
                                    ka.getUserName() != null ? ka.getUserName() : "Kal Admin",
                                    bnk.getBranchNameFull(), bnk.getBranchCode(), "BLOCK_PENDING", "BLOCKED");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Auto-block: Kal Admin notification failed for branch {}: {}", bnk.getBranchCode(), e.getMessage());
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
        if (!hasPendingWork) return;

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(30);  // DEMO: 30s window

        // ── AddUser: INACTIVE_PENDING → INACTIVE ──
        List<AddUser> toInactivateUsers = addUserRepository
                .findByStatusAndInactivateScheduledAtBefore(AddUser.UserStatus.INACTIVE_PENDING, cutoff);
        for (AddUser user : toInactivateUsers) {
            user.setStatus(AddUser.UserStatus.INACTIVE);
            addUserRepository.save(user);
            logger.info("Auto-inactivated user: {} ({})", user.getUsername(), user.getId());
            try {
                if (user.getEmail() != null) {
                    String userEntityCode = user.getBranchCode() != null ? user.getBranchCode() : user.getBankCode();
                    String userOrgName = userEntityCode != null ? userEntityCode : user.getUsername();
                    if (user.getBranchCode() != null) {
                        Optional<BranchBank> brOpt = branchBankRepository.findByBranchCode(user.getBranchCode());
                        if (brOpt.isPresent() && brOpt.get().getBranchNameFull() != null) userOrgName = brOpt.get().getBranchNameFull();
                    } else if (user.getBankCode() != null) {
                        Optional<MainBank> bkOpt = mainBankRepository.findByBankCode(user.getBankCode());
                        if (bkOpt.isPresent() && bkOpt.get().getBankNameFull() != null) userOrgName = bkOpt.get().getBankNameFull();
                    }
                    emailService.sendInactivatedNotification(user.getEmail(),
                            user.getFullName(), userOrgName, userEntityCode != null ? userEntityCode : user.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Auto-inactivate email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
            notifyActorFinal(user.getInactivateScheduledBy(), "Inactivated",
                    user.getFullName() != null ? user.getFullName() : user.getUsername(), user.getUsername());
            // Finalize any pending replacement now that user is INACTIVE
            try {
                Optional<AdminReplacement> pendingRep = adminReplacementRepository
                        .findByOriginalEntityIdAndEntityTypeAndStatus(user.getId(), "USER", "PENDING");
                if (pendingRep.isPresent()) {
                    adminReplacementService.finalizeUserPending(pendingRep.get());
                    logger.info("Pending replacement finalized for user: {}", user.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Pending replacement finalization failed for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        // ── AddUser: ACTIVE_PENDING → ACTIVE ──
        List<AddUser> toActivateUsers = addUserRepository
                .findByStatusAndReactivateScheduledAtBefore(AddUser.UserStatus.ACTIVE_PENDING, cutoff);
        for (AddUser user : toActivateUsers) {
            user.setStatus(AddUser.UserStatus.ACTIVE);
            user.setInactivateScheduledAt(null);
            user.setInactivateScheduledBy(null);
            user.setReactivateScheduledAt(null);
            user.setReactivateScheduledBy(null);
            // Existing password is preserved — user logs in with their old password
            addUserRepository.save(user);
            logger.info("Auto-reactivated user: {} ({})", user.getUsername(), user.getId());
            // Auto-INACTIVE the temporary replacement and RESTORE the record
            Optional<AdminReplacement> activeRep = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(user.getId(), "USER", "ACTIVE");
            if (activeRep.isPresent()) {
                AdminReplacement rep = activeRep.get();
                rep.setStatus("RESTORED");
                adminReplacementRepository.save(rep);
                if (rep.getReplacementEntityId() > 0L) {
                    Optional<AddUser> repUserOpt = addUserRepository.findById(rep.getReplacementEntityId());
                    if (repUserOpt.isPresent()) {
                        AddUser repUser = repUserOpt.get();
                        repUser.setStatus(AddUser.UserStatus.INACTIVE_PENDING);
                        repUser.setInactivateScheduledAt(LocalDateTime.now());
                        repUser.setInactivateScheduledBy("SYSTEM");
                        addUserRepository.save(repUser);
                        hasPendingWork = true;
                        logger.info("Scheduled inactivation of replacement user: {} as original {} reactivated", repUser.getUsername(), user.getUsername());
                        // Reassign replacement's children back to original (A)
                        List<AddUser> reactivateChildren = addUserRepository.findByParentId(repUser.getId());
                        for (AddUser child : reactivateChildren) {
                            child.setParentId(user.getId());
                            addUserRepository.save(child);
                        }
                        try {
                            if (repUser.getEmail() != null) {
                                String repDisplayName = repUser.getFullName() != null ? repUser.getFullName() : repUser.getUsername();
                                boolean repIsBranch = repUser.getBranchCode() != null;
                                String repEntityCode = repIsBranch ? repUser.getBranchCode() : repUser.getBankCode();
                                String repOrgName = repEntityCode;
                                if (repIsBranch) {
                                    Optional<BranchBank> brOpt = branchBankRepository.findByBranchCode(repUser.getBranchCode());
                                    if (brOpt.isPresent()) repOrgName = brOpt.get().getBranchNameFull() != null ? brOpt.get().getBranchNameFull() : repEntityCode;
                                } else {
                                    Optional<MainBank> bkOpt = mainBankRepository.findByBankCode(repUser.getBankCode());
                                    if (bkOpt.isPresent()) repOrgName = bkOpt.get().getBankNameFull() != null ? bkOpt.get().getBankNameFull() : repEntityCode;
                                }
                                String inactivateAt = LocalDateTime.now().plusSeconds(30)
                                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                                emailService.sendReplacementTenureEnded(repUser.getEmail(), repDisplayName);
                                emailService.sendInactivatePendingWarning(repUser.getEmail(), repDisplayName, repOrgName, repEntityCode, inactivateAt);
                            }
                        } catch (Exception ex) {
                            logger.warn("Tenure-ended/pending-warning email failed for {}: {}", repUser.getUsername(), ex.getMessage());
                        }
                    }
                }
            }
            // Restore delegation if one was active for this user
            try {
                Optional<UserDelegation> activeDelegation = userDelegationRepository
                        .findByDelegatorUserIdAndStatus(user.getId(), "ACTIVE");
                if (activeDelegation.isPresent()) {
                    UserDelegation delegation = activeDelegation.get();
                    // Return children to delegator (their preDelegationParentId)
                    List<AddUser> delegatedChildren = addUserRepository.findByParentId(delegation.getDelegateeUserId());
                    for (AddUser child : delegatedChildren) {
                        if (child.getPreDelegationParentId() != null
                                && child.getPreDelegationParentId().equals(user.getId())) {
                            child.setParentId(child.getPreDelegationParentId());
                            child.setPreDelegationParentId(null);
                            addUserRepository.save(child);
                        }
                    }
                    // Close delegation record
                    delegation.setStatus("RESTORED");
                    delegation.setRestoredAt(LocalDateTime.now());
                    delegation.setRestoredBy("SYSTEM");
                    userDelegationRepository.save(delegation);
                    // Notify both parties
                    Optional<AddUser> delegateeOpt = addUserRepository.findById(delegation.getDelegateeUserId());
                    if (delegateeOpt.isPresent()) {
                        AddUser delegatee = delegateeOpt.get();
                        String delegatorName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                        String delegateeName = delegatee.getFullName() != null ? delegatee.getFullName() : delegatee.getUsername();
                        try {
                            emailService.sendDelegationRestoredToDelegator(user.getEmail(), delegatorName, delegateeName);
                        } catch (Exception ex) {
                            logger.warn("Delegation restoration email to delegator failed: {}", ex.getMessage());
                        }
                        try {
                            if (delegatee.getEmail() != null) {
                                emailService.sendDelegationRestoredToDelegatee(delegatee.getEmail(), delegateeName, delegatorName);
                            }
                        } catch (Exception ex) {
                            logger.warn("Delegation restoration email to delegatee failed: {}", ex.getMessage());
                        }
                    }
                    logger.info("Delegation RESTORED for user {} (was delegated to {})", user.getUsername(), delegation.getDelegateeUserId());
                }
            } catch (Exception e) {
                logger.warn("Delegation restoration failed for user {}: {}", user.getUsername(), e.getMessage());
            }

            // Send "Final Active" notification + "Credentials" email with new temp password
            try {
                if (user.getEmail() != null) {
                    String userDisplayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                    boolean isBranchUser = user.getBranchCode() != null;
                    String entityCode = isBranchUser ? user.getBranchCode() : user.getBankCode();
                    String codeLabel  = isBranchUser ? "Branch Code" : "Bank Code";
                    String orgName = entityCode; // fallback
                    if (isBranchUser) {
                        Optional<com.jpb.reconciliation.reconciliation.entity.BranchBank> branchOpt =
                                branchBankRepository.findByBranchCode(user.getBranchCode());
                        if (branchOpt.isPresent()) orgName = branchOpt.get().getBranchNameFull() != null
                                ? branchOpt.get().getBranchNameFull() : entityCode;
                    } else {
                        Optional<com.jpb.reconciliation.reconciliation.entity.MainBank> bankOpt =
                                mainBankRepository.findByBankCode(user.getBankCode());
                        if (bankOpt.isPresent()) orgName = bankOpt.get().getBankNameFull() != null
                                ? bankOpt.get().getBankNameFull() : entityCode;
                    }
                    String loginLink = isBranchUser
                            ? frontendUrl + "/user-verify?bankCode=" + user.getBankCode()
                                    + "&branchCode=" + user.getBranchCode()
                                    + "&username=" + user.getUsername()
                                    + "&email=" + user.getEmail() + "&mode=login"
                            : frontendUrl + "/user-verify?bankCode=" + user.getBankCode()
                                    + "&username=" + user.getUsername()
                                    + "&email=" + user.getEmail() + "&mode=login";
                    // Final Active notification
                    emailService.sendReactivatedNotification(user.getEmail(),
                            userDisplayName, orgName, entityCode,
                            user.getUsername(), loginLink);
                    // Credentials reminder — password shown as "--", user logs in with existing password
                    emailService.sendUserWelcome(user.getEmail(), userDisplayName,
                            entityCode, codeLabel, user.getUsername(), "--", loginLink);
                }
            } catch (Exception e) {
                logger.warn("Auto-reactivate email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
            notifyActorFinal(user.getReactivateScheduledBy(), "Reactivated",
                    user.getFullName() != null ? user.getFullName() : user.getUsername(), user.getUsername());
        }

        // ── AddUser: BLOCK_PENDING → BLOCK ──
        List<AddUser> toBlockUsers = addUserRepository
                .findByStatusAndBlockScheduledAtBefore(AddUser.UserStatus.BLOCK_PENDING, cutoff);
        for (AddUser user : toBlockUsers) {
            user.setStatus(AddUser.UserStatus.BLOCKED);
            user.setInactivateScheduledAt(null);
            user.setInactivateScheduledBy(null);
            user.setReactivateScheduledAt(null);
            user.setReactivateScheduledBy(null);
            addUserRepository.save(user);
            logger.info("Auto-blocked user: {} ({})", user.getUsername(), user.getId());
            try {
                if (user.getEmail() != null) {
                    String userEntityCode = user.getBranchCode() != null ? user.getBranchCode() : user.getBankCode();
                    String userOrgName = userEntityCode != null ? userEntityCode : user.getUsername();
                    if (user.getBranchCode() != null) {
                        Optional<BranchBank> brOpt = branchBankRepository.findByBranchCode(user.getBranchCode());
                        if (brOpt.isPresent() && brOpt.get().getBranchNameFull() != null) userOrgName = brOpt.get().getBranchNameFull();
                    } else if (user.getBankCode() != null) {
                        Optional<MainBank> bkOpt = mainBankRepository.findByBankCode(user.getBankCode());
                        if (bkOpt.isPresent() && bkOpt.get().getBankNameFull() != null) userOrgName = bkOpt.get().getBankNameFull();
                    }
                    emailService.sendStatusChangeNotification(user.getEmail(),
                            user.getFullName(), userOrgName, userEntityCode != null ? userEntityCode : user.getUsername(),
                            "BLOCK_PENDING", "BLOCKED");
                }
            } catch (Exception e) {
                logger.warn("Auto-block email failed for user {}: {}", user.getUsername(), e.getMessage());
            }
            boolean isCascadeBlock = user.getBlockScheduledBy() != null && user.getBlockScheduledBy().startsWith("CASCADE:");
            // Only send actor email for direct blocks, not cascade blocks
            if (!isCascadeBlock) {
                notifyActorFinal(user.getBlockScheduledBy(), "Blocked",
                        user.getFullName() != null ? user.getFullName() : user.getUsername(), user.getUsername());
            }
            // Replacement becomes PERMANENT only for direct individual blocks, not cascade
            if (!isCascadeBlock) {
                try {
                    Optional<AdminReplacement> activeRepOpt = adminReplacementRepository
                        .findByOriginalEntityIdAndEntityTypeAndStatus(user.getId(), "USER", "ACTIVE");
                    if (activeRepOpt.isPresent()) {
                        AdminReplacement rep = activeRepOpt.get();
                        rep.setStatus("PERMANENT");
                        adminReplacementRepository.save(rep);
                        logger.info("Replacement for user {} promoted to PERMANENT", user.getUsername());
                        if (rep.getReplacementEntityId() > 0L) {
                            addUserRepository.findById(rep.getReplacementEntityId()).ifPresent(repUser -> {
                                try {
                                    String repName = repUser.getFullName() != null ? repUser.getFullName() : repUser.getUsername();
                                    emailService.sendReplacementBecamePermanent(repUser.getEmail(), repName);
                                } catch (Exception ex) {
                                    logger.warn("Permanent-promotion email failed for user {}: {}", repUser.getUsername(), ex.getMessage());
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Replacement PERMANENT promotion failed for user {}: {}", user.getUsername(), e.getMessage());
                }
            }
            // Notify delegatee if blocked user had an active delegation
            try {
                Optional<UserDelegation> activeDelegation = userDelegationRepository
                        .findByDelegatorUserIdAndStatus(user.getId(), "ACTIVE");
                if (activeDelegation.isPresent()) {
                    UserDelegation delegation = activeDelegation.get();
                    addUserRepository.findById(delegation.getDelegateeUserId()).ifPresent(delegatee -> {
                        if (delegatee.getEmail() != null) {
                            String delegatorName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                            String delegateeName = delegatee.getFullName() != null ? delegatee.getFullName() : delegatee.getUsername();
                            try {
                                emailService.sendDelegatorBlockedToDelegatee(delegatee.getEmail(), delegateeName, delegatorName);
                            } catch (Exception ex) {
                                logger.warn("Delegation block-final email to delegatee failed: {}", ex.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                logger.warn("Delegation block-final notification failed for user {}: {}", user.getUsername(), e.getMessage());
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
            logger.info("Auto-inactivated bank admin: {} ({})", admin.getUsername(), admin.getId());
            try {
                Optional<MainBank> bankOpt = mainBankRepository.findByBankCode(admin.getBankCode());
                String bankName = bankOpt.isPresent() ? bankOpt.get().getBankNameFull() : admin.getBankCode();
                emailService.sendInactivatedNotification(admin.getEmail(), admin.getUsername(), bankName, admin.getBankCode());
            } catch (Exception e) {
                logger.warn("Auto-inactivate email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
            }
            // Finalize any pending replacement now that admin is INACTIVE
            try {
                Optional<AdminReplacement> pendingRep = adminReplacementRepository
                        .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "MAIN_ADMIN", "PENDING");
                if (pendingRep.isPresent()) {
                    adminReplacementService.finalizeMainAdminPending(pendingRep.get());
                    logger.info("Pending replacement finalized for bank admin: {}", admin.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Pending replacement finalization failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // ── MainAdmin (BANK_ADMIN): ACTIVE_PENDING → ACTIVE ──
        List<MainAdmin> toActivateBankAdmins = mainAdminRepository
                .findByStatusAndReactivateScheduledAtBefore("ACTIVE_PENDING", cutoff);
        for (MainAdmin admin : toActivateBankAdmins) {
            admin.setStatus("ACTIVE");
            admin.setInactivateScheduledAt(null);
            admin.setInactivateScheduledBy(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(admin);
            logger.info("Auto-reactivated bank admin: {} ({})", admin.getUsername(), admin.getId());
            // Auto-INACTIVE the temporary replacement and RESTORE the record
            Optional<AdminReplacement> activeRep = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "MAIN_ADMIN", "ACTIVE");
            if (activeRep.isPresent()) {
                AdminReplacement rep = activeRep.get();
                rep.setStatus("RESTORED");
                adminReplacementRepository.save(rep);
                if (rep.getReplacementEntityId() > 0L) {
                    Optional<MainAdmin> repAdminOpt = mainAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        MainAdmin repAdmin = repAdminOpt.get();
                        repAdmin.setStatus("INACTIVE_PENDING");
                        repAdmin.setInactivateScheduledAt(LocalDateTime.now());
                        repAdmin.setInactivateScheduledBy("SYSTEM");
                        repAdmin.setUpdatedAt(LocalDateTime.now());
                        repAdmin.setUpdatedBy("SYSTEM");
                        mainAdminRepository.save(repAdmin);
                        hasPendingWork = true;
                        logger.info("Scheduled inactivation of replacement bank admin: {} as original {} reactivated", repAdmin.getUsername(), admin.getUsername());
                        try {
                            Optional<MainBank> repBankOpt = mainBankRepository.findByBankCode(repAdmin.getBankCode());
                            String repBankName = repBankOpt.isPresent() ? repBankOpt.get().getBankNameFull() : repAdmin.getBankCode();
                            String repAdminDisplay = repAdmin.getUsername();
                            String inactivateAt = LocalDateTime.now().plusSeconds(30)
                                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                            emailService.sendReplacementTenureEnded(repAdmin.getEmail(), repAdminDisplay);
                            emailService.sendInactivatePendingWarning(repAdmin.getEmail(), repAdminDisplay, repBankName, repAdmin.getBankCode(), inactivateAt);
                        } catch (Exception ex) {
                            logger.warn("Tenure-ended/pending-warning email failed for {}: {}", repAdmin.getUsername(), ex.getMessage());
                        }
                    }
                }
            }
            try {
                Optional<MainBank> bankOpt = mainBankRepository.findByBankCode(admin.getBankCode());
                String bankName = bankOpt.isPresent() ? bankOpt.get().getBankNameFull() : admin.getBankCode();
                String bankAdminDisplayName = admin.getFullName() != null ? admin.getFullName() : admin.getUsername();
                emailService.sendReactivatedNotification(admin.getEmail(), bankAdminDisplayName, bankName, admin.getBankCode(),
                        admin.getUsername(), frontendUrl + "/bank-admin-login?bankCode=" + admin.getBankCode() + "&username=" + admin.getUsername());
            } catch (Exception e) {
                logger.warn("Auto-reactivate email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // ── MainAdmin (BANK_ADMIN): BLOCK_PENDING → BLOCKED ──
        List<MainAdmin> toBlockBankAdmins = mainAdminRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);
        for (MainAdmin admin : toBlockBankAdmins) {
            admin.setStatus("BLOCKED");
            admin.setInactivateScheduledAt(null);
            admin.setInactivateScheduledBy(null);
            admin.setReactivateScheduledAt(null);
            admin.setReactivateScheduledBy(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            mainAdminRepository.save(admin);
            logger.info("Auto-blocked bank admin: {} ({})", admin.getUsername(), admin.getId());
            try {
                Optional<MainBank> bankOpt = mainBankRepository.findByBankCode(admin.getBankCode());
                String bankName = bankOpt.isPresent() ? bankOpt.get().getBankNameFull() : admin.getBankCode();
                emailService.sendStatusChangeNotification(admin.getEmail(), admin.getUsername(),
                        bankName, admin.getBankCode(), "BLOCK_PENDING", "BLOCKED");
            } catch (Exception e) {
                logger.warn("Auto-block email failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
            }
            // When original is permanently blocked → replacement becomes PERMANENT admin + update MainBank contact
            try {
                Optional<AdminReplacement> activeRepOpt = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "MAIN_ADMIN", "ACTIVE");
                if (activeRepOpt.isPresent()) {
                    AdminReplacement rep = activeRepOpt.get();
                    rep.setStatus("PERMANENT");
                    adminReplacementRepository.save(rep);
                    logger.info("Replacement for bank admin {} promoted to PERMANENT", admin.getUsername());
                    if (rep.getReplacementEntityId() > 0L) {
                        Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> repAdminOpt =
                            mainAdminRepository.findById(rep.getReplacementEntityId());
                        if (repAdminOpt.isPresent()) {
                            com.jpb.reconciliation.reconciliation.entity.MainAdmin repAdmin = repAdminOpt.get();
                            try {
                                emailService.sendReplacementBecamePermanent(repAdmin.getEmail(), repAdmin.getUsername());
                            } catch (Exception ex) {
                                logger.warn("Permanent-promotion email failed for {}: {}", repAdmin.getUsername(), ex.getMessage());
                            }
                            // Update MainBank contact fields to replacement admin's data
                            try {
                                Optional<MainBank> bankOpt2 = mainBankRepository.findByBankCode(admin.getBankCode());
                                if (bankOpt2.isPresent()) {
                                    MainBank bank = bankOpt2.get();
                                    // Save original admin's full name before overwriting
                                    if (admin.getFullName() == null && bank.getPrimaryFullName() != null) {
                                        admin.setFullName(bank.getPrimaryFullName());
                                        mainAdminRepository.save(admin);
                                    }
                                    bank.setPrimaryEmail(repAdmin.getEmail());
                                    bank.setPrimaryFullName(rep.getPendingFullName() != null ? rep.getPendingFullName() : repAdmin.getUsername());
                                    bank.setPrimaryMobile(rep.getPendingMobile());
                                    bank.setBankAdminId(repAdmin.getUsername());
                                    mainBankRepository.save(bank);
                                    logger.info("[BLOCK-SYNC] Main_Bank {} contact updated to replacement: {}", bank.getBankCode(), repAdmin.getUsername());
                                }
                            } catch (Exception ex) {
                                logger.warn("MainBank contact update failed for bank admin {}: {}", admin.getUsername(), ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Replacement PERMANENT promotion failed for bank admin {}: {}", admin.getUsername(), e.getMessage());
            }
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
            try {
                Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
                emailService.sendInactivatedNotification(admin.getEmail(), admin.getUsername(), branchName, admin.getBranchCode());
            } catch (Exception e) {
                logger.warn("Auto-inactivate email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
            }
            // Finalize any pending replacement now that branch admin is INACTIVE
            try {
                Optional<AdminReplacement> pendingRep = adminReplacementRepository
                        .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "PENDING");
                if (pendingRep.isPresent()) {
                    adminReplacementService.finalizeBranchAdminPending(pendingRep.get());
                    logger.info("Pending replacement finalized for branch admin: {}", admin.getUsername());
                }
            } catch (Exception e) {
                logger.warn("Pending replacement finalization failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // ── BranchAdmin (BRANCH_ADMIN): ACTIVE_PENDING → ACTIVE ──
        List<BranchAdmin> toActivateBranchAdmins = branchAdminRepository
                .findByStatusAndReactivateScheduledAtBefore("ACTIVE_PENDING", cutoff);
        for (BranchAdmin admin : toActivateBranchAdmins) {
            admin.setStatus("ACTIVE");
            admin.setInactivateScheduledAt(null);
            admin.setInactivateScheduledBy(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(admin);
            logger.info("Auto-reactivated branch admin: {} ({})", admin.getUsername(), admin.getId());
            // Auto-INACTIVE the temporary replacement and RESTORE the record
            Optional<AdminReplacement> activeRep = adminReplacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "ACTIVE");
            if (activeRep.isPresent()) {
                AdminReplacement rep = activeRep.get();
                rep.setStatus("RESTORED");
                adminReplacementRepository.save(rep);
                if (rep.getReplacementEntityId() > 0L) {
                    Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> repAdminOpt =
                        branchAdminRepository.findById(rep.getReplacementEntityId());
                    if (repAdminOpt.isPresent()) {
                        com.jpb.reconciliation.reconciliation.entity.BranchAdmin repAdmin = repAdminOpt.get();
                        repAdmin.setStatus("INACTIVE_PENDING");
                        repAdmin.setInactivateScheduledAt(LocalDateTime.now());
                        repAdmin.setInactivateScheduledBy("SYSTEM");
                        repAdmin.setUpdatedAt(LocalDateTime.now());
                        repAdmin.setUpdatedBy("SYSTEM");
                        branchAdminRepository.save(repAdmin);
                        hasPendingWork = true;
                        logger.info("Scheduled inactivation of replacement branch admin: {} as original {} reactivated", repAdmin.getUsername(), admin.getUsername());
                        try {
                            Optional<BranchBank> repBranchOpt = branchBankRepository.findByBranchCode(repAdmin.getBranchCode());
                            String repBranchName = repBranchOpt.isPresent() ? repBranchOpt.get().getBranchNameFull() : repAdmin.getBranchCode();
                            String repAdminDisplay = repAdmin.getUsername();
                            String inactivateAt = LocalDateTime.now().plusSeconds(30)
                                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                            emailService.sendReplacementTenureEnded(repAdmin.getEmail(), repAdminDisplay);
                            emailService.sendInactivatePendingWarning(repAdmin.getEmail(), repAdminDisplay, repBranchName, repAdmin.getBranchCode(), inactivateAt);
                        } catch (Exception ex) {
                            logger.warn("Tenure-ended/pending-warning email failed for {}: {}", repAdmin.getUsername(), ex.getMessage());
                        }
                    }
                }
            }
            try {
                Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
                String branchAdminDisplayName = admin.getFullName() != null ? admin.getFullName() : admin.getUsername();
                emailService.sendReactivatedNotification(admin.getEmail(), branchAdminDisplayName, branchName, admin.getBranchCode(),
                        admin.getUsername(), frontendUrl + "/branch-admin-login?branchCode=" + admin.getBranchCode() + "&username=" + admin.getUsername());
            } catch (Exception e) {
                logger.warn("Auto-reactivate email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // ── BranchAdmin (BRANCH_ADMIN): BLOCK_PENDING → BLOCKED ──
        List<BranchAdmin> toBlockBranchAdmins = branchAdminRepository
                .findByStatusAndBlockScheduledAtBefore("BLOCK_PENDING", cutoff);
        for (BranchAdmin admin : toBlockBranchAdmins) {
            admin.setStatus("BLOCKED");
            admin.setInactivateScheduledAt(null);
            admin.setInactivateScheduledBy(null);
            admin.setReactivateScheduledAt(null);
            admin.setReactivateScheduledBy(null);
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("SYSTEM");
            branchAdminRepository.save(admin);
            logger.info("Auto-blocked branch admin: {} ({})", admin.getUsername(), admin.getId());
            try {
                Optional<BranchBank> branchOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                String branchName = branchOpt.isPresent() ? branchOpt.get().getBranchNameFull() : admin.getBranchCode();
                emailService.sendStatusChangeNotification(admin.getEmail(), admin.getUsername(),
                        branchName, admin.getBranchCode(), "BLOCK_PENDING", "BLOCKED");
            } catch (Exception e) {
                logger.warn("Auto-block email failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
            }
            // When original branch admin is permanently blocked → replacement becomes PERMANENT + update BranchBank contact
            try {
                Optional<AdminReplacement> activeRepOpt = adminReplacementRepository
                    .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "ACTIVE");
                if (activeRepOpt.isPresent()) {
                    AdminReplacement rep = activeRepOpt.get();
                    rep.setStatus("PERMANENT");
                    adminReplacementRepository.save(rep);
                    logger.info("Replacement for branch admin {} promoted to PERMANENT", admin.getUsername());
                    if (rep.getReplacementEntityId() > 0L) {
                        Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> repAdminOpt =
                            branchAdminRepository.findById(rep.getReplacementEntityId());
                        if (repAdminOpt.isPresent()) {
                            com.jpb.reconciliation.reconciliation.entity.BranchAdmin repAdmin = repAdminOpt.get();
                            try {
                                emailService.sendReplacementBecamePermanent(repAdmin.getEmail(), repAdmin.getUsername());
                            } catch (Exception ex) {
                                logger.warn("Permanent-promotion email failed for {}: {}", repAdmin.getUsername(), ex.getMessage());
                            }
                            // Update BranchBank contact fields to replacement admin's data
                            try {
                                Optional<BranchBank> branchBankOpt = branchBankRepository.findByBranchCode(admin.getBranchCode());
                                if (branchBankOpt.isPresent()) {
                                    BranchBank branchBank = branchBankOpt.get();
                                    // Save original admin's full name before overwriting
                                    if (admin.getFullName() == null && branchBank.getPrimaryFullName() != null) {
                                        admin.setFullName(branchBank.getPrimaryFullName());
                                        branchAdminRepository.save(admin);
                                    }
                                    branchBank.setPrimaryEmail(repAdmin.getEmail());
                                    branchBank.setPrimaryFullName(rep.getPendingFullName() != null ? rep.getPendingFullName() : repAdmin.getUsername());
                                    branchBank.setPrimaryMobile(rep.getPendingMobile());
                                    branchBank.setBranchAdminId(repAdmin.getUsername());
                                    branchBankRepository.save(branchBank);
                                    logger.info("[BLOCK-SYNC] Branch_Bank {} contact updated to replacement: {}", branchBank.getBranchCode(), repAdmin.getUsername());
                                }
                            } catch (Exception ex) {
                                logger.warn("BranchBank contact update failed for branch admin {}: {}", admin.getUsername(), ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Replacement PERMANENT promotion failed for branch admin {}: {}", admin.getUsername(), e.getMessage());
            }
        }

        // Reset flag if nothing pending remains — next ticks will be query-free until something is scheduled
        hasPendingWork =
            addUserRepository.existsByStatusIn(Arrays.asList(
                AddUser.UserStatus.INACTIVE_PENDING, AddUser.UserStatus.ACTIVE_PENDING, AddUser.UserStatus.BLOCK_PENDING)) ||
            mainAdminRepository.existsByStatusIn(Arrays.asList("INACTIVE_PENDING", "ACTIVE_PENDING", "BLOCK_PENDING")) ||
            branchAdminRepository.existsByStatusIn(Arrays.asList("INACTIVE_PENDING", "ACTIVE_PENDING", "BLOCK_PENDING"));
    }

    // ─────────────────────────────────────────────
    // Final-phase actor/parent notification helpers
    // Mirror AddUserServiceImpl.notifyActor / notifyParent but used by the scheduler
    // ─────────────────────────────────────────────
    private void notifyActorFinal(String actorBy, String action, String targetName, String targetCode) {
        if (actorBy == null || actorBy.isEmpty()) return;
        if (actorBy.startsWith("CASCADE:")) actorBy = actorBy.substring(8);
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            boolean isEmail = actorBy.contains("@");
            // MainAdmin — try primary lookup (by username or email), then fallback to the other
            Optional<MainAdmin> ma = isEmail
                    ? mainAdminRepository.findFirstByEmail(actorBy)
                    : mainAdminRepository.findFirstByUsername(actorBy);
            if (!ma.isPresent()) ma = isEmail
                    ? mainAdminRepository.findFirstByUsername(actorBy)
                    : mainAdminRepository.findFirstByEmail(actorBy);
            if (ma.isPresent() && ma.get().getEmail() != null && !ma.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ma.get().getEmail(), ma.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            // BranchAdmin — same dual lookup
            Optional<BranchAdmin> ba = isEmail
                    ? branchAdminRepository.findFirstByEmail(actorBy)
                    : branchAdminRepository.findFirstByUsername(actorBy);
            if (!ba.isPresent()) ba = isEmail
                    ? branchAdminRepository.findFirstByUsername(actorBy)
                    : branchAdminRepository.findFirstByEmail(actorBy);
            if (ba.isPresent() && ba.get().getEmail() != null && !ba.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(ba.get().getEmail(), ba.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            // AddUser (actor could be a senior user who scheduled action on sub-user)
            Optional<com.jpb.reconciliation.reconciliation.entity.AddUser> au = isEmail
                    ? addUserRepository.findByEmail(actorBy)
                    : addUserRepository.findByUsername(actorBy);
            if (au.isPresent() && au.get().getEmail() != null && !au.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(au.get().getEmail(), au.get().getUsername(), action, targetName, targetCode, when);
                return;
            }
            // KalAdmin fallback
            kalAdminRepository.findByUserName(actorBy).ifPresent(ka -> {
                if (ka.getEmailId() != null && !ka.getEmailId().isEmpty()) {
                    emailService.sendActorActionConfirmation(ka.getEmailId(), ka.getUserName(), action, targetName, targetCode, when);
                }
            });
        } catch (Exception e) {
            logger.warn("[ACTOR-FINAL-CONFIRM] Email failed for {}: {}", actorBy, e.getMessage());
        }
    }

    private void notifyParentFinal(AddUser user, String action) {
        if (user.getParentId() == null) return;
        try {
            Optional<AddUser> parentOpt = addUserRepository.findById(user.getParentId());
            if (!parentOpt.isPresent()) return;
            AddUser parent = parentOpt.get();
            if (parent.getEmail() == null || parent.getEmail().isEmpty()) return;
            String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            String targetName = user.getFullName() != null ? user.getFullName() : user.getUsername();
            emailService.sendActorActionConfirmation(parent.getEmail(), parent.getUsername(), action, targetName, user.getUsername(), when);
        } catch (Exception e) {
            logger.warn("[PARENT-FINAL-NOTIFY] Failed to notify parent of user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Email routing helpers — route to replacement if original admin is INACTIVE/BLOCKED
    // ─────────────────────────────────────────────
    private String[] resolveMainBankContact(MainBank bank) {
        try {
            if (bank.getBankAdminId() != null) {
                Optional<MainAdmin> origOpt = mainAdminRepository.findByBankCodeAndUsername(
                        bank.getBankCode(), bank.getBankAdminId());
                if (origOpt.isPresent()) {
                    MainAdmin orig = origOpt.get();
                    List<AdminReplacement> reps = adminReplacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                    orig.getId(), "MAIN_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                    if (!reps.isEmpty() && reps.get(0).getReplacementEntityId() > 0) {
                        Optional<MainAdmin> rep = mainAdminRepository.findById(reps.get(0).getReplacementEntityId());
                        if (rep.isPresent() && rep.get().getEmail() != null && !rep.get().getEmail().isEmpty()) {
                            return new String[]{rep.get().getEmail(), rep.get().getUsername()};
                        }
                    }
                    // No active replacement — use the admin's own email directly
                    if (orig.getEmail() != null && !orig.getEmail().isEmpty()) {
                        return new String[]{orig.getEmail(),
                                orig.getUsername() != null ? orig.getUsername() : "Bank Admin"};
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("resolveMainBankContact failed for {}: {}", bank.getBankCode(), e.getMessage());
        }
        return new String[]{bank.getPrimaryEmail(),
                bank.getPrimaryFullName() != null ? bank.getPrimaryFullName() : "Bank Admin"};
    }

    private String[] resolveBranchBankContact(BranchBank branch) {
        try {
            if (branch.getBranchAdminId() != null) {
                Optional<BranchAdmin> origOpt = branchAdminRepository.findByBranchCodeAndUsername(
                        branch.getBranchCode(), branch.getBranchAdminId());
                if (origOpt.isPresent()) {
                    BranchAdmin orig = origOpt.get();
                    List<AdminReplacement> reps = adminReplacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                    orig.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                    if (!reps.isEmpty() && reps.get(0).getReplacementEntityId() > 0) {
                        Optional<BranchAdmin> rep = branchAdminRepository.findById(reps.get(0).getReplacementEntityId());
                        if (rep.isPresent() && rep.get().getEmail() != null && !rep.get().getEmail().isEmpty()) {
                            return new String[]{rep.get().getEmail(), rep.get().getUsername()};
                        }
                    }
                    // No active replacement — use the branch admin's own email directly
                    if (orig.getEmail() != null && !orig.getEmail().isEmpty()) {
                        return new String[]{orig.getEmail(),
                                orig.getUsername() != null ? orig.getUsername() : "Branch Admin"};
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("resolveBranchBankContact failed for {}: {}", branch.getBranchCode(), e.getMessage());
        }
        return new String[]{branch.getPrimaryEmail(),
                branch.getPrimaryFullName() != null ? branch.getPrimaryFullName() : "Branch Admin"};
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestWithStatusList("FAILURE", msg, new ArrayList<>()));
    }
}