package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

/**
 * EmailService — sends transactional emails from ReconXpert.Ai
 *
 * All methods are fire-and-forget — they throw RuntimeException on failure
 * so callers can decide whether to propagate or swallow.
 */
public interface EmailService {

    /**
     * Send OTP email for Forgot Password flow.
     */
    void sendForgotPasswordOtp(String toEmail, String userName, String otpCode, int expiryMins);

    /**
     * Send welcome email to Bank's Primary Contact (Super User)
     * after KalInfotech Admin successfully onboards the bank.
     * Includes: Bank Code, Super User ID, Default Password, Verify Link.
     */
    void sendBankAdminWelcome(String toEmail, String superUserName,
                              String bankName, String bankCode,
                              String superUserId, String defaultPassword,
                              String verifyLink);

    /**
     * Send status change notification email to Bank's Super User
     * whenever KalInfotech Admin changes the bank status.
     * e.g. ACTIVE → INACTIVE, BLOCKED, etc.
     */
    void sendStatusChangeNotification(String toEmail, String superUserName,
                                      String bankName, String bankCode,
                                      String oldStatus, String newStatus);

    /**
     * Send cascade status change notification to a Sub-Institute's primary contact.
     * Triggered when parent bank is BLOCKED / ACTIVE.
     * Email includes a note referencing the parent bank that caused the cascade.
     */
    void sendBranchBankStatusNotification(String toEmail, String contactName,
                                            String branchBankName, String branchBankCode,
                                            String oldStatus, String newStatus,
                                            String parentBankName, String parentBankCode);

    /**
     * Send block warning email to Bank's Super User when admin schedules permanent block.
     * Tells them: account will be permanently blocked in 24 hours, contact admin to cancel.
     */
    void sendBlockWarning(String toEmail, String superUserName,
                          String bankName, String bankCode,
                          String blockAt);

    /**
     * Send block cancelled email to Bank's Super User when admin undoes block.
     */
    void sendBlockCancelled(String toEmail, String superUserName,
                            String bankName, String bankCode,
                            String restoredStatus);

    /**
     * Send block cancelled email to a Sub-Institute when parent's block is undone.
     */
    void sendBranchBankBlockCancelled(String toEmail, String contactName,
                                        String branchBankName, String branchBankCode,
                                        String parentBankName, String parentBankCode);

    /**
     * Send block warning email to a Sub-Institute's primary contact.
     * Triggered at same time as parent bank block warning.
     */
    void sendBranchBankBlockWarning(String toEmail, String contactName,
                                      String branchBankName, String branchBankCode,
                                      String parentBankName, String parentBankCode,
                                      String blockAt);

    /**
     * Send profile update notification to the bank's primary contact
     * whenever KalInfotech Admin updates the bank profile.
     * changesBySections: section name → list of "Field: old → new" strings.
     * Only sections that actually changed are included; empty map means no email is sent.
     */
    void sendBankUpdateNotification(String toEmail, String contactName,
                                           String bankName, String bankCode,
                                           String updatedAt,
                                           Map<String, List<String>> changesBySections);

    /**
     * Sent when an admin schedules inactivation (INACTIVE_PENDING).
     * Tells the contact: your account will be inactivated at inactivateAt unless cancelled.
     */
    void sendInactivatePendingWarning(String toEmail, String contactName,
                                      String entityName, String entityCode,
                                      String inactivateAt);

    /**
     * Sent when admin undoes a pending inactivation.
     * Account restored to its previous (ACTIVE) status.
     */
    void sendInactivateCancelled(String toEmail, String contactName,
                                 String entityName, String entityCode);

    /**
     * Sent when the inactivation window expires and status flips to INACTIVE automatically.
     */
    void sendInactivatedNotification(String toEmail, String contactName,
                                     String entityName, String entityCode);

    /**
     * Sent when an admin schedules reactivation (ACTIVE_PENDING).
     * Tells the contact: your account will be reactivated at reactivateAt.
     */
    void sendReactivatePendingNotification(String toEmail, String contactName,
                                           String entityName, String entityCode,
                                           String reactivateAt);

    /**
     * Sent when admin undoes a pending reactivation.
     * Account restored to INACTIVE.
     */
    void sendReactivateCancelled(String toEmail, String contactName,
                                 String entityName, String entityCode);

    /**
     * Sent when the reactivation window expires and status flips to ACTIVE automatically.
     */
    void sendReactivatedNotification(String toEmail, String contactName,
                                     String entityName, String entityCode);
}