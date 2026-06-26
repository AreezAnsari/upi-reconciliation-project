package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;

@Service
public class PasswordandSecurityExpiryService {

    private static final Logger logger =
            LoggerFactory.getLogger(PasswordandSecurityExpiryService.class);

    @Autowired private BranchAdminRepository   branchRepo;
    @Autowired private KalAdminRepository      kalrepo;
    @Autowired private MainAdminRepository     mainRepo;

    // ✅ Use PasswordaAndSecurityMailService — jo already project mein hai
    @Autowired private PasswordaAndSecurityMailService mailService;

    // ── Entry point — called by scheduler daily at 01:00 AM ──
    public void runExpiryCheck() {
        logger.info("=== Password Expiry Check Started ===");
        checkBranchAdmins();
        checkMainAdmins();
        checkKalAdmins();
        logger.info("=== Password Expiry Check Finished ===");
    }

    // ── Calculate how many days ago password was updated ──
    private long calculateDays(LocalDateTime updatedAt) {
        if (updatedAt == null) return 0;
        return ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());
    }

    //Testing
 // ✅ TEST ONLY — directly simulate daysUsed
    public void testSingleUserMail(String email, String name, long days) {
        process(email, name, days, new Object(), "TEST");
    }
    // ── Branch Admins ──
    private void checkBranchAdmins() {
        for (BranchAdmin u : branchRepo.findAll()) {
            try {
                // ✅ FIX: createdAt fallback if passwordUpdatedAt is null
                LocalDateTime ref = u.getPasswordUpdatedAt() != null
                        ? u.getPasswordUpdatedAt()
                        : u.getCreatedAt();
                if (ref == null) continue;

                long days = calculateDays(ref);
                process(u.getEmail(), u.getUsername(), days, u, "BRANCH");
            } catch (Exception e) {
                logger.error("BranchAdmin check failed for {}: {}",
                        u.getUsername(), e.getMessage());
            }
        }
    }

    // ── Main (Bank) Admins ──
    private void checkMainAdmins() {
        for (MainAdmin u : mainRepo.findAll()) {
            try {
                // ✅ FIX: createdAt fallback if passwordUpdatedAt is null
                LocalDateTime ref = u.getPasswordUpdatedAt() != null
                        ? u.getPasswordUpdatedAt()
                        : u.getCreatedAt();
                if (ref == null) continue;

                long days = calculateDays(ref);
                process(u.getEmail(), u.getUsername(), days, u, "MAIN");
            } catch (Exception e) {
                logger.error("MainAdmin check failed for {}: {}",
                        u.getUsername(), e.getMessage());
            }
        }
    }

    // ── Kal Admins ──
    private void checkKalAdmins() {
        for (KalAdmin u : kalrepo.findAll()) {
            try {
                if (u.getPasswordUpdatedAt() == null) continue;

                long days = calculateDays(u.getPasswordUpdatedAt());
                process(u.getEmailId(), u.getUserName(), days, u, "KAL");
            } catch (Exception e) {
                logger.error("KalAdmin check failed for {}: {}",
                        u.getUserName(), e.getMessage());
            }
        }
    }

    // ── Core logic: send mail or block ──
    private void process(String email, String name,
                         long days, Object user, String type) {

        if (email == null || email.trim().isEmpty()) {
            logger.warn("[{}] Skipping — email is null for user: {}", type, name);
            return;
        }

        logger.debug("[{}] user={} daysUsed={}", type, name, days);

        if (days == 80) {
            // ── Reminder: 10 days left ──
            String subject = "⚠ ReconXpert.Ai — Password Expiring in 10 Days";
            String body = buildReminderBody(name, 10);
            sendSafely(email, subject, body, name, type);

        } else if (days == 85) {
            // ── Reminder: 5 days left ──
            String subject = "🚨 ReconXpert.Ai — Password Expiring in 5 Days (URGENT)";
            String body = buildReminderBody(name, 5);
            sendSafely(email, subject, body, name, type);

        } else if (days == 90) {
            // ── Exactly 90 days: expired — send warning mail ──
            String subject = "⛔ ReconXpert.Ai — Password Expired";
            String body = buildExpiredBody(name);
            sendSafely(email, subject, body, name, type);

        } else if (days > 90) {
            // ── More than 90 days: block account ──
            blockUser(user, name, type);

            String subject = "⛔ ReconXpert.Ai — Account Blocked: Password Expired";
            String body = buildBlockedBody(name);
            sendSafely(email, subject, body, name, type);
        }
    }

    // ── Block user based on type ──
    private void blockUser(Object user, String name, String type) {
        try {
            if (user instanceof BranchAdmin) {
                BranchAdmin b = (BranchAdmin) user;
                // ✅ Only block if not already blocked
                if (!"BLOCKED".equalsIgnoreCase(b.getStatus())) {
                    b.setStatus("BLOCKED");
                    branchRepo.save(b);
                    logger.info("[{}] BLOCKED — user={}", type, name);
                }
            } else if (user instanceof MainAdmin) {
                MainAdmin m = (MainAdmin) user;
                if (!"BLOCKED".equalsIgnoreCase(m.getStatus())) {
                    m.setStatus("BLOCKED");
                    mainRepo.save(m);
                    logger.info("[{}] BLOCKED — user={}", type, name);
                }
            } else if (user instanceof KalAdmin) {
                KalAdmin k = (KalAdmin) user;
                if (!"BLOCKED".equalsIgnoreCase(k.getUserStatus())) {
                    k.setUserStatus("BLOCKED");
                    kalrepo.save(k);
                    logger.info("[{}] BLOCKED — user={}", type, name);
                }
            }
        } catch (Exception e) {
            logger.error("[{}] Block failed for {}: {}", type, name, e.getMessage());
        }
    }

    // ── Send mail safely — never throw, just log ──
    private void sendSafely(String email, String subject,
                            String body, String name, String type) {
        try {
            mailService.sendMail(email, subject, body);
            logger.info("[{}] Mail sent — user={} subject={}", type, name, subject);
        } catch (Exception e) {
            logger.error("[{}] Mail failed — user={} reason={}", type, name, e.getMessage());
        }
    }

    // ────────────────────────────────────────────
    // Email body builders (plain text)
    // ────────────────────────────────────────────

    private String buildReminderBody(String name, int daysLeft) {
        return "Dear " + name + ",\n\n"
            + "As part of our security policy, your ReconXpert.Ai password must be "
            + "changed every 90 days.\n\n"
            + "Your password will expire in " + daysLeft + " day(s).\n\n"
            + "Please login immediately and update your password to avoid losing access.\n\n"
            + "Steps to update:\n"
            + "1. Login to ReconXpert.Ai\n"
            + "2. Go to Security → Change Password\n"
            + "3. Enter your current password and set a strong new password\n\n"
            + "For help, contact: support@kalinfotech.com\n\n"
            + "Regards,\n"
            + "KalInfotech Security Team\n"
            + "ReconXpert.Ai";
    }

    private String buildExpiredBody(String name) {
        return "Dear " + name + ",\n\n"
            + "Your ReconXpert.Ai password has EXPIRED (90-day policy).\n\n"
            + "Please use the 'Forgot Password' option on the login page to reset "
            + "your password and regain access.\n\n"
            + "If you need help, contact: support@kalinfotech.com\n\n"
            + "Regards,\n"
            + "KalInfotech Security Team\n"
            + "ReconXpert.Ai";
    }

    private String buildBlockedBody(String name) {
        return "Dear " + name + ",\n\n"
            + "Your ReconXpert.Ai account has been BLOCKED because your password "
            + "expired more than 90 days ago.\n\n"
            + "To restore access, please contact your administrator or write to: "
            + "support@kalinfotech.com\n\n"
            + "Regards,\n"
            + "KalInfotech Security Team\n"
            + "ReconXpert.Ai";
    }
}