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
     * Send welcome email to Institution's Primary Contact (Super User)
     * after KalInfotech Admin successfully onboards the institution.
     * Includes: Institution Code, Super User ID, Default Password, Verify Link.
     */
    void sendSuperUserWelcome(String toEmail, String superUserName,
                              String institutionName, String institutionCode,
                              String superUserId, String defaultPassword,
                              String verifyLink);

    /**
     * Send status change notification email to Institution's Super User
     * whenever KalInfotech Admin changes the institution status.
     * e.g. ACTIVE → INACTIVE, BLOCKED, etc.
     */
    void sendStatusChangeNotification(String toEmail, String superUserName,
                                      String institutionName, String institutionCode,
                                      String oldStatus, String newStatus);

    /**
     * Send cascade status change notification to a Sub-Institute's primary contact.
     * Triggered when parent institution is BLOCKED / ACTIVE.
     * Email includes a note referencing the parent institution that caused the cascade.
     */
    void sendSubInstituteStatusNotification(String toEmail, String contactName,
                                            String subInstitutionName, String subInstitutionCode,
                                            String oldStatus, String newStatus,
                                            String parentInstitutionName, String parentInstitutionCode);

    /**
     * Send block warning email to Institution's Super User when admin schedules permanent block.
     * Tells them: account will be permanently blocked in 24 hours, contact admin to cancel.
     */
    void sendBlockWarning(String toEmail, String superUserName,
                          String institutionName, String institutionCode,
                          String blockAt);

    /**
     * Send block cancelled email to Institution's Super User when admin undoes block.
     */
    void sendBlockCancelled(String toEmail, String superUserName,
                            String institutionName, String institutionCode,
                            String restoredStatus);

    /**
     * Send block cancelled email to a Sub-Institute when parent's block is undone.
     */
    void sendSubInstituteBlockCancelled(String toEmail, String contactName,
                                        String subInstitutionName, String subInstitutionCode,
                                        String parentInstitutionName, String parentInstitutionCode);

    /**
     * Send block warning email to a Sub-Institute's primary contact.
     * Triggered at same time as parent institution block warning.
     */
    void sendSubInstituteBlockWarning(String toEmail, String contactName,
                                      String subInstitutionName, String subInstitutionCode,
                                      String parentInstitutionName, String parentInstitutionCode,
                                      String blockAt);

    /**
     * Send profile update notification to the institution's primary contact
     * whenever KalInfotech Admin updates the institution profile.
     * changesBySections: section name → list of "Field: old → new" strings.
     * Only sections that actually changed are included; empty map means no email is sent.
     */
    void sendInstitutionUpdateNotification(String toEmail, String contactName,
                                           String institutionName, String institutionCode,
                                           String updatedAt,
                                           Map<String, List<String>> changesBySections);
}