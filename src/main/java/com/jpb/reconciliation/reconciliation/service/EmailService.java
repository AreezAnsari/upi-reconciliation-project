package com.jpb.reconciliation.reconciliation.service;

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
     * e.g. ACTIVE → INACTIVE, BLOCKED, RETIRED, etc.
     */
    void sendStatusChangeNotification(String toEmail, String superUserName,
                                      String institutionName, String institutionCode,
                                      String oldStatus, String newStatus);

    /**
     * Send cascade status change notification to a Sub-Institute's primary contact.
     * Triggered when parent institution is BLOCKED / ACTIVE / RETIRED.
     * Email includes a note referencing the parent institution that caused the cascade.
     */
    void sendSubInstituteStatusNotification(String toEmail, String contactName,
                                            String subInstitutionName, String subInstitutionCode,
                                            String oldStatus, String newStatus,
                                            String parentInstitutionName, String parentInstitutionCode);
}