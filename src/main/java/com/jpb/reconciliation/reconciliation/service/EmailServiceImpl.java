package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    // ─────────────────────────────────────────────────────────────────────
    // SEND FORGOT PASSWORD OTP EMAIL
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void sendForgotPasswordOtp(String toEmail, String userName,
                                      String otpCode, int expiryMins) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Your Password Reset OTP");
            helper.setText(buildOtpEmailHtml(userName, otpCode, expiryMins), true);

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP email — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email (messaging error): " + e.getMessage(), toEmail, e);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP email — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email (unexpected error): " + e.getMessage(), toEmail, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND Bank Admin WELCOME EMAIL
    // Includes: Bank Code, User ID, Default Password, Verify Link
    // @Async — runs in background, won't block the API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBankAdminWelcome(String toEmail, String superUserName,
                                     String bankName, String bankCode,
                                     String superUserId, String defaultPassword,
                                     String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — You have been added as Bank Admin of " + bankName);
            helper.setText(buildSuperUserWelcomeHtml(
                superUserName, bankName, bankCode,
                superUserId, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("SuperUser welcome email sent to: {} | userId: {}", toEmail, superUserId);
        } catch (MessagingException e) {
            // @Async — exception won't reach caller; bankmust NOT be rolled back on email failure
            logger.error("[EMAIL-DELIVERY-FAIL] SuperUser welcome — recipient: {} | userId: {} | reason: {}", toEmail, superUserId, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] SuperUser welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — OTP Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildOtpEmailHtml(String userName, String otpCode, int expiryMins) {
        return "<!DOCTYPE html>"
            + "<html lang='en'>"
            + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Password Reset OTP</title></head>"
            + "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,Helvetica,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px 40px 20px 40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px 0;'>Dear <strong>" + sanitize(userName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 28px 0;'>We received a request to reset your password on ReconXpert.Ai. Use the OTP below to proceed.</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:8px 0 28px 0;'>"
            + "<div style='background:#f8fafc;border:2px dashed #d4a843;border-radius:12px;padding:24px 32px;display:inline-block;text-align:center;'>"
            + "<p style='margin:0 0 8px 0;font-size:12px;color:#94a3b8;text-transform:uppercase;letter-spacing:2px;'>Your One-Time Password</p>"
            + "<p style='margin:0;font-size:40px;font-weight:bold;color:#1a1a2e;letter-spacing:10px;'>" + sanitize(otpCode) + "</p>"
            + "<p style='margin:8px 0 0 0;font-size:12px;color:#ef4444;'>Valid for " + expiryMins + " minutes only</p>"
            + "</div></td></tr></table>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px 0;'>To reset your password:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 24px 0;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter this OTP on the password reset page</li>"
            + "<li>Set your new password (min 8 chars, 1 uppercase, 1 number, 1 special character)</li>"
            + "<li>Log in with your new credentials</li>"
            + "</ol>"
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:12px 16px;margin-bottom:24px;'>"
            + "<p style='margin:0;font-size:12px;color:#991b1b;'><strong>Security Notice:</strong> Never share this OTP with anyone. KalInfotech will never ask for your OTP. If you did not request this, please ignore this email — your account is safe.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Admin Welcome Email
    // Contains: Bank Code, User ID, Default Password, Verify button
    // ─────────────────────────────────────────────────────────────────────
    private String buildSuperUserWelcomeHtml(String name, String bankName,
                                              String bankCode, String superUserId,
                                              String defaultPassword, String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "You have been added as the <strong>Bank Admin</strong> of "
            + "<strong>" + sanitize(bankName) + "</strong> on ReconXpert.Ai by KalInfotech Admin."
            + "</p>"

            // Credentials box — Bank Code, User ID, Default Password
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>Bank Code:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(bankCode) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>User ID:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(superUserId) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Kindly click the button below to verify your email and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#1a1a2e,#0f3460);color:#d4a843;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Email &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the login page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>Bank Code:</strong> " + sanitize(bankCode) + "</li>"
            + "<li>Enter your <strong>User ID:</strong> " + sanitize(superUserId) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete email verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This link expires in 48 hours. Do not share it with anyone.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND STATUS CHANGE NOTIFICATION EMAIL
    // Sent when Admin changes bank status: INACTIVE / BLOCKED / ACTIVE
    // @Async — fire and forget, won't block API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendStatusChangeNotification(String toEmail, String superUserName,
                                              String bankName, String bankCode,
                                              String oldStatus, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Bank Status Update: " + bankName);
            helper.setText(buildStatusChangeHtml(superUserName, bankName, bankCode, oldStatus, newStatus), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Status change — recipient: {} | bank: {} | {} → {}", toEmail, bankCode, oldStatus, newStatus);
        } catch (MessagingException e) {
            // @Async — status update must NOT be blocked by email failure
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — recipient: {} | bank: {} | {} → {} | reason: {}", toEmail, bankCode, oldStatus, newStatus, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — unexpected error — recipient: {} | bank: {} | reason: {}", toEmail, bankCode, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Status Change Notification Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildStatusChangeHtml(String name, String bankName,
                                          String bankCode,
                                          String oldStatus, String newStatus) {

        // Color + icon per status
        String statusColor, statusBg, statusIcon, statusMessage;
        switch (newStatus.toUpperCase()) {
            case "INACTIVE":
                statusColor  = "#6366f1";
                statusBg     = "rgba(99,102,241,0.1)";
                statusIcon   = "⏸";
                statusMessage = "Your account has been marked <strong>Inactive</strong>. "
                    + "You will not be able to access the platform until it is reactivated. "
                    + "Please contact KalInfotech Admin for assistance.";
                break;
            case "BLOCKED":
                statusColor  = "#ef4444";
                statusBg     = "rgba(239,68,68,0.1)";
                statusIcon   = "🚫";
                statusMessage = "Your account has been <strong>Blocked</strong> by KalInfotech Admin. "
                    + "Access to the ReconXpert.Ai platform has been permanently restricted. "
                    + "Please contact KalInfotech Admin immediately for clarification.";
                break;
            case "ACTIVE":
                statusColor  = "#22c55e";
                statusBg     = "rgba(34,197,94,0.1)";
                statusIcon   = "✅";
                statusMessage = "Your account has been <strong>Activated</strong>. "
                    + "You can now access the ReconXpert.Ai platform using your credentials.";
                break;
            default:
                statusColor  = "#d4a843";
                statusBg     = "rgba(212,168,67,0.1)";
                statusIcon   = "ℹ";
                statusMessage = "Your bank status has been updated to <strong>" + sanitize(newStatus) + "</strong>.";
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + statusColor + ";padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>" + statusIcon + "</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:" + statusColor + ";'>Status Changed: " + sanitize(oldStatus) + " → " + sanitize(newStatus) + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;line-height:1.7;'>" + statusMessage + "</p>"

            // Bank details box
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:13px;color:#475569;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:600;padding:4px 0;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:4px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>New Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:4px 0;color:" + statusColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "</table></div>"

            // Contact note
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> This is an automated notification from KalInfotech Admin. "
            + "If you have any questions, please contact us at <a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUB-INSTITUTE CASCADE STATUS NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankStatusNotification(String toEmail, String contactName,
                                                   String branchBankName, String branchBankCode,
                                                   String oldStatus, String newStatus,
                                                   String parentBankName, String parentBankCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Sub-Institute Status Update: " + branchBankName);
            helper.setText(buildSubInstituteStatusHtml(
                    contactName, branchBankName, branchBankCode,
                    oldStatus, newStatus, parentBankName, parentBankCode), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub-bnkitute cascade — recipient: {} | branch: {} | {} → {} | parent: {}",
                    toEmail, branchBankCode, oldStatus, newStatus, parentBankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-bnkitute cascade — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-bnkitute cascade — unexpected — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        }
    }

    private String buildSubInstituteStatusHtml(String contactName,
                                                String branchBankName, String branchBankCode,
                                                String oldStatus, String newStatus,
                                                String parentBankName, String parentBankCode) {

        String statusColor, statusBg, statusIcon, statusHeading, statusMessage, actionNote;

        switch (newStatus.toUpperCase()) {
            case "BLOCKED":
                statusColor   = "#ef4444";
                statusBg      = "rgba(239,68,68,0.08)";
                statusIcon    = "🚫";
                statusHeading = "Platform Access Suspended";
                statusMessage = "We wish to inform you that your bank's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>suspended</strong> "
                    + "with immediate effect, in accordance with a compliance directive issued by "
                    + "<strong>KalInfotech Administration</strong> applicable to <strong>"
                    + sanitize(parentBankName) + "</strong> and all associated banks. "
                    + "During this period, platform login and all reconciliation operations will be unavailable.";
                actionNote    = "To understand the reason for this action or to request rebnkatement, "
                    + "please reach out to your designated KalInfotech Relationship Manager or write to us at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>. "
                    + "Please quote your Bank Code when contacting support.";
                break;
            case "ACTIVE":
                statusColor   = "#22c55e";
                statusBg      = "rgba(34,197,94,0.08)";
                statusIcon    = "✅";
                statusHeading = "Platform Access Rebnkated";
                statusMessage = "We are pleased to inform you that your bank's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>rebnkated</strong>. "
                    + "This follows the reactivation of <strong>" + sanitize(parentBankName) + "</strong> "
                    + "by KalInfotech Administration. You may now resume normal platform operations "
                    + "using your existing Bank Admin credentials.";
                actionNote    = "If you experience any difficulty accessing the platform or require assistance, "
                    + "please contact our support team at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>.";
                break;
            default:
                statusColor   = "#d4a843";
                statusBg      = "rgba(212,168,67,0.08)";
                statusIcon    = "ℹ";
                statusHeading = "Bank Status Update";
                statusMessage = "Your bank's status on the ReconXpert.Ai platform has been updated "
                    + "to <strong>" + sanitize(newStatus) + "</strong> by KalInfotech Administration.";
                actionNote    = "For queries, contact us at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>.";
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + statusColor + ";padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:36px;'>" + statusIcon + "</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:" + statusColor + ";'>" + statusHeading + "</p>"
            + "<p style='margin:0;font-size:13px;color:#64748b;'>"
            + sanitize(oldStatus) + " &nbsp;&#8594;&nbsp; <strong style='color:" + statusColor + ";'>" + sanitize(newStatus) + "</strong>"
            + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 28px;line-height:1.8;'>" + statusMessage + "</p>"

            // Bank details card
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#94a3b8;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Your Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;width:170px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:5px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Current Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:5px 0;color:" + statusColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Effective</td>"
            + "<td style='font-size:13px;color:#1e293b;padding:5px 0;'>Immediately</td></tr>"
            + "</table></div>"

            // Issued by / reference
            + "<div style='background:#f0f4ff;border:1px solid #c7d2fe;border-radius:8px;padding:16px 20px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 10px;font-size:11px;color:#4338ca;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Issued By</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:170px;'>Issuing Authority</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table>"
            + "</div>"

            // Action note
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0 0 4px;font-size:12px;color:#92400e;font-weight:700;'>Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#92400e;line-height:1.7;'>" + actionNote + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK WARNING — Bank Admin
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBlockWarning(String toEmail, String superUserName,
                                 String bankName, String bankCode,
                                 String blockAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ ReconXpert.Ai — Block Notice: " + bankName);
            helper.setText(buildBlockWarningHtml(superUserName, bankName, bankCode, blockAt), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Block warning — recipient: {} | bank: {} | blockAt: {}",
                    toEmail, bankCode, blockAt);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block warning — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block warning — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK WARNING — Sub-Institute Primary Contact
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankBlockWarning(String toEmail, String contactName,
                                             String branchBankName, String branchBankCode,
                                             String parentBankName, String parentBankCode,
                                             String blockAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ ReconXpert.Ai — Block Notice: " + branchBankName);
            helper.setText(buildSubBlockWarningHtml(contactName, branchBankName, branchBankCode,
                    parentBankName, parentBankCode, blockAt), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub block warning — recipient: {} | branch: {} | parent: {} | blockAt: {}",
                    toEmail, branchBankCode, parentBankCode, blockAt);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block warning — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block warning — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK CANCELLED — Bank Admin
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBlockCancelled(String toEmail, String superUserName,
                                   String bankName, String bankCode,
                                   String restoredStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("✅ ReconXpert.Ai — Block Cancelled: " + bankName);
            helper.setText(buildBlockCancelledHtml(superUserName, bankName, bankCode, restoredStatus), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Block cancelled — recipient: {} | bank: {}", toEmail, bankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block cancelled — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block cancelled — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK CANCELLED — Sub-Institute Primary Contact
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankBlockCancelled(String toEmail, String contactName,
                                               String branchBankName, String branchBankCode,
                                               String parentBankName, String parentBankCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("✅ ReconXpert.Ai — Block Cancelled: " + branchBankName);
            helper.setText(buildSubBlockCancelledHtml(contactName, branchBankName, branchBankCode,
                    parentBankName, parentBankCode), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub block cancelled — recipient: {} | branch: {} | parent: {}",
                    toEmail, branchBankCode, parentBankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block cancelled — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block cancelled — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Block Cancelled (Bank)
    // ─────────────────────────────────────────────────────────────────────
    private String buildBlockCancelledHtml(String name, String bankName,
                                             String bankCode, String restoredStatus) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Success banner
            + "<tr><td style='background:rgba(34,197,94,0.1);border-bottom:3px solid #22c55e;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>✅</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#15803d;'>Block Successfully Cancelled</p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>Your account has been restored to <strong>" + sanitize(restoredStatus) + "</strong> status</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We are pleased to inform you that the scheduled block of your bank's account on "
            + "<strong>ReconXpert.Ai</strong> has been <strong>successfully cancelled</strong> by KalInfotech Administration. "
            + "Your account is now fully restored and you may continue using the platform as normal."
            + "</p>"

            // Details box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#166534;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Account Restoration Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#4ade80;padding:5px 0;width:170px;color:#166534;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Restored Status</td>"
            + "<td style='font-size:13px;font-weight:700;color:#15803d;padding:5px 0;'>" + sanitize(restoredStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Action By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>KalInfotech Administration</td></tr>"
            + "</table></div>"

            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> No further action is required from your side. "
            + "If you have any questions, please contact us at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Block Cancelled (Sub-Institute)
    // ─────────────────────────────────────────────────────────────────────
    private String buildSubBlockCancelledHtml(String contactName,
                                                String branchBankName, String branchBankCode,
                                                String parentBankName, String parentBankCode) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Success banner
            + "<tr><td style='background:rgba(34,197,94,0.1);border-bottom:3px solid #22c55e;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>✅</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#15803d;'>Block Successfully Cancelled</p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>Your account block has been called off — no action required</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We are pleased to inform you that the scheduled block of your bank on "
            + "<strong>ReconXpert.Ai</strong> has been <strong>successfully cancelled</strong>. "
            + "This follows the cancellation of the block of your parent bank, "
            + "<strong>" + sanitize(parentBankName) + "</strong>, by KalInfotech Administration. "
            + "Your platform access and all reconciliation services remain fully active."
            + "</p>"

            // Details box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#166534;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Your Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;width:170px;'>Your Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Your Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table></div>"

            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>No action required.</strong> You may continue using the platform normally. "
            + "For any queries, contact us at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Block Warning
    // ─────────────────────────────────────────────────────────────────────
    private String buildBlockWarningHtml(String name, String bankName,
                                         String bankCode, String blockAt) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Warning banner
            + "<tr><td style='background:rgba(239,68,68,0.08);border-bottom:3px solid #ef4444;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>🚫</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#991b1b;'>Account Block Scheduled</p>"
            + "<p style='margin:0;font-size:13px;color:#7f1d1d;'>Your account will be permanently blocked on <strong>" + sanitize(blockAt) + "</strong></p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "This is an urgent notice that your bank's account on <strong>ReconXpert.Ai</strong> has been "
            + "<strong>scheduled for permanent blocking</strong> by KalInfotech Administration. "
            + "Once blocked, all platform access and reconciliation services will be <strong>permanently suspended</strong> "
            + "and cannot be reversed."
            + "</p>"

            // Details box
            + "<div style='background:#fef2f2;border:1px solid #fecaca;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#991b1b;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Block Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;width:170px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Scheduled By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Block Time</td>"
            + "<td style='font-size:13px;font-weight:700;color:#dc2626;padding:5px 0;'>" + sanitize(blockAt) + "</td></tr>"
            + "</table></div>"

            // Urgent action
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:16px 18px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 6px;font-size:13px;color:#991b1b;font-weight:700;'>⚡ Immediate Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#991b1b;line-height:1.7;'>"
            + "If you believe this is an error or wish to cancel the block, please contact KalInfotech Administration "
            + "<strong>immediately</strong> at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#dc2626;font-weight:600;'>support@kalinfotech.com</a>. "
            + "This action can only be cancelled before the scheduled block time."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Sub-Institute Block Warning
    // ─────────────────────────────────────────────────────────────────────
    private String buildSubBlockWarningHtml(String contactName,
                                            String branchBankName, String branchBankCode,
                                            String parentBankName, String parentBankCode,
                                            String blockAt) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Warning banner
            + "<tr><td style='background:rgba(239,68,68,0.08);border-bottom:3px solid #ef4444;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>🚫</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#991b1b;'>Account Block Scheduled</p>"
            + "<p style='margin:0;font-size:13px;color:#7f1d1d;'>Your account will be permanently blocked on <strong>" + sanitize(blockAt) + "</strong></p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "This is an urgent notice that your bank's account on <strong>ReconXpert.Ai</strong> has been "
            + "<strong>scheduled for permanent blocking</strong>. This action has been triggered by the blocking "
            + "of your parent bank, <strong>" + sanitize(parentBankName) + "</strong>, by KalInfotech Administration."
            + "</p>"

            // Details box
            + "<div style='background:#fef2f2;border:1px solid #fecaca;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#991b1b;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Block Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;width:170px;'>Your Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Your Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Block Time</td>"
            + "<td style='font-size:13px;font-weight:700;color:#dc2626;padding:5px 0;'>" + sanitize(blockAt) + "</td></tr>"
            + "</table></div>"

            // Issued by
            + "<div style='background:#f0f4ff;border:1px solid #c7d2fe;border-radius:8px;padding:16px 20px;margin-bottom:20px;'>"
            + "<p style='margin:0 0 10px;font-size:11px;color:#4338ca;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Issued By</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:170px;'>Issuing Authority</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table></div>"

            // Urgent action
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:16px 18px;'>"
            + "<p style='margin:0 0 6px;font-size:13px;color:#991b1b;font-weight:700;'>⚡ Immediate Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#991b1b;line-height:1.7;'>"
            + "If you wish to raise an objection or seek clarification, please contact KalInfotech Administration "
            + "<strong>before</strong> the scheduled block time at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#dc2626;font-weight:600;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // BANK PROFILE UPDATE NOTIFICATION
    // Sent to primary contact whenever Admin updates the bankprofile.
    // changesBySections maps section name → list of "Field: old → new" strings.
    // Only called when the map is non-empty (i.e. something actually changed).
    // @Async — fire and forget, update must NOT be blocked by email failure
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBankUpdateNotification(String toEmail, String contactName,
                                                   String bankName, String bankCode,
                                                   String updatedAt,
                                                   Map<String, List<String>> changesBySections) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Bank Profile Updated: " + bankName);
            helper.setText(buildBankUpdateHtml(contactName, bankName, bankCode,
                    updatedAt, changesBySections), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Bank update notification — recipient: {} | bank: {} | sections: {}",
                    toEmail, bankCode, changesBySections.keySet());
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Bank update — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Bank update — unexpected — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Profile Update Notification
    // Renders only the sections that actually changed, with before → after values
    // ─────────────────────────────────────────────────────────────────────
    private String buildBankUpdateHtml(String contactName, String bankName,
                                               String bankCode, String updatedAt,
                                               Map<String, List<String>> changesBySections) {

        // Build the change-details block: one coloured card per section
        StringBuilder changesHtml = new StringBuilder();
        String[] sectionColors = { "#0d9488", "#6366f1", "#f59e0b", "#ef4444" };
        String[] sectionBgs    = { "rgba(13,148,136,0.07)", "rgba(99,102,241,0.07)",
                                    "rgba(245,158,11,0.07)", "rgba(239,68,68,0.07)" };
        int colorIdx = 0;
        for (Map.Entry<String, List<String>> section : changesBySections.entrySet()) {
            String color = sectionColors[colorIdx % sectionColors.length];
            String bg    = sectionBgs[colorIdx % sectionBgs.length];
            colorIdx++;

            changesHtml.append("<div style='background:").append(bg)
                .append(";border:1px solid ").append(color)
                .append("33;border-left:4px solid ").append(color)
                .append(";border-radius:8px;padding:16px 20px;margin-bottom:14px;'>")
                .append("<p style='margin:0 0 10px;font-size:12px;color:").append(color)
                .append(";font-weight:700;text-transform:uppercase;letter-spacing:1px;'>")
                .append(sanitize(section.getKey())).append("</p>");

            for (String change : section.getValue()) {
                // Split at " → " to bold the two sides
                String sanitized = sanitize(change);
                String formatted = sanitized.replace(" &#x2192; ", "</span>"
                        + "<span style='color:#94a3b8;'> &#x2192; </span>"
                        + "<span style='color:#1e293b;font-weight:600;'>");
                // If it was a removal/addition line (starts with Added/Removed) give it a badge
                String badge = "";
                if (change.startsWith("Added:")) {
                    badge = "<span style='background:#dcfce7;color:#166534;font-size:10px;font-weight:700;"
                          + "padding:2px 7px;border-radius:20px;margin-right:8px;'>NEW</span>";
                } else if (change.startsWith("Removed:")) {
                    badge = "<span style='background:#fee2e2;color:#991b1b;font-size:10px;font-weight:700;"
                          + "padding:2px 7px;border-radius:20px;margin-right:8px;'>REMOVED</span>";
                }
                changesHtml.append("<p style='margin:0 0 6px;font-size:13px;color:#475569;'>")
                    .append(badge)
                    .append("<span style='color:#475569;'>").append(formatted).append("</span>")
                    .append("</p>");
            }
            changesHtml.append("</div>");
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Info banner
            + "<tr><td style='background:rgba(13,148,136,0.08);border-bottom:3px solid #0d9488;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>🔔</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:#0f766e;'>Bank Profile Updated</p>"
            + "<p style='margin:6px 0 0;font-size:13px;color:#0d9488;'>The following changes have been applied to your account</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We wish to inform you that the profile of <strong>" + sanitize(bankName) + "</strong> "
            + "on the <strong>ReconXpert.Ai</strong> platform has been updated by KalInfotech Administration. "
            + "The specific changes are detailed below."
            + "</p>"

            // Bank meta card
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #0d9488;border-radius:8px;padding:16px 20px;margin-bottom:24px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:140px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:4px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Updated On</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(updatedAt) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Updated By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "</table></div>"

            // Change sections (dynamic)
            + "<p style='font-size:13px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:1px;margin:0 0 12px;'>Changes Made</p>"
            + changesHtml.toString()

            // Security note
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:14px 18px;margin-top:10px;'>"
            + "<p style='margin:0 0 4px;font-size:12px;color:#92400e;font-weight:700;'>Important Notice</p>"
            + "<p style='margin:0;font-size:13px;color:#92400e;line-height:1.7;'>"
            + "If you were not informed of this update or believe it was made in error, "
            + "please contact KalInfotech Administration immediately at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;font-weight:600;'>support@kalinfotech.com</a> "
            + "and quote your Bank Code: <strong>" + sanitize(bankCode) + "</strong>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATE PENDING WARNING
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivatePendingWarning(String toEmail, String contactName,
                                              String entityName, String entityCode,
                                              String inactivateAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Inactivation Scheduled: " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "ACTIVE", "INACTIVE_PENDING",
                    "Your account has been scheduled for inactivation. It will be marked <strong>Inactive</strong> on "
                    + sanitize(inactivateAt) + ". If this was not authorized, please contact your administrator immediately to cancel.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivate-pending warning sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivate-pending warning — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATE CANCELLED
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivateCancelled(String toEmail, String contactName,
                                         String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Inactivation Cancelled: " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "INACTIVE_PENDING", "ACTIVE",
                    "The scheduled inactivation of your account has been successfully cancelled by the administrator. "
                    + "Your account has been restored to <strong>Active</strong> status — no further action is required.",
                    "#22c55e", "✅");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivate-cancelled sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivate-cancelled — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATED NOTIFICATION (auto-process fired)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivatedNotification(String toEmail, String contactName,
                                             String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Account Inactivated: " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "INACTIVE_PENDING", "INACTIVE",
                    "Your account has been marked <strong>Inactive</strong>. Access to the ReconXpert.Ai platform has been suspended. "
                    + "Please contact your administrator if you require reactivation.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivated notification sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivated notification — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATE PENDING NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivatePendingNotification(String toEmail, String contactName,
                                                   String entityName, String entityCode,
                                                   String reactivateAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Reactivation Scheduled: " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "INACTIVE", "ACTIVE_PENDING",
                    "Your account has been scheduled for reactivation. It will be marked <strong>Active</strong> on "
                    + sanitize(reactivateAt) + ". No action is required on your part.",
                    "#22c55e", "🔄");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivate-pending sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivate-pending — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATE CANCELLED
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivateCancelled(String toEmail, String contactName,
                                         String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Reactivation Cancelled: " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "ACTIVE_PENDING", "INACTIVE",
                    "The scheduled reactivation of your account has been cancelled by the administrator. "
                    + "Your account status remains <strong>Inactive</strong>. Please contact your administrator if you require assistance.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivate-cancelled sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivate-cancelled — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATED NOTIFICATION (auto-process fired)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivatedNotification(String toEmail, String contactName,
                                             String entityName, String entityCode,
                                             String username, String loginUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Account Reactivated: " + entityName);
            String safe     = sanitize(contactName);
            String safeOrg  = sanitize(entityName);
            String safeCode = sanitize(entityCode);
            String safeUser = sanitize(username);
            String safeUrl  = sanitize(loginUrl);
            String body = "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

                // Header
                + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
                + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
                + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
                + "</td></tr>"

                // Body
                + "<tr><td style='padding:40px;'>"

                // Greeting
                + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + safe + "</strong>,</p>"
                + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
                + "Your account on <strong>ReconXpert.Ai</strong> has been successfully <strong>Reactivated</strong>. "
                + "You can now log in using your existing credentials below."
                + "</p>"

                // Credentials box — green like onboarding
                + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
                + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>✅ Your Login Credentials</p>"
                + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>ID / Code:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + safeCode + "</span></p>"
                + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + safeUser + "</span></p>"
                + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Password:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;color:#94a3b8;'>--</span>"
                + "<span style='font-size:11px;color:#64748b;margin-left:8px;'>(use your existing password)</span></p>"
                + "</div>"

                // Login button
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
                + "<a href='" + safeUrl + "' style='background:linear-gradient(135deg,#1a1a2e,#0f3460);color:#d4a843;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Login to ReconXpert.Ai</a>"
                + "</td></tr></table>"

                // Steps note
                + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the button, on the login page:</p>"
                + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
                + "<li>Your <strong>ID / Code</strong> and <strong>Username</strong> will be pre-filled</li>"
                + "<li>Enter your <strong>existing password</strong> to continue</li>"
                + "</ol>"

                // Warning box
                + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
                + "<p style='margin:0;font-size:12px;color:#92400e;'>"
                + "<strong>Important:</strong> Please do not share your credentials with anyone. "
                + "Contact your administrator if you need assistance."
                + "</p>"
                + "</div>"

                + "</td></tr>"

                // Footer
                + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
                + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
                + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivated notification sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivated notification — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Generic pending/transition email
    // ─────────────────────────────────────────────────────────────────────
    private String buildStatusPendingHtml(String contactName, String entityName, String entityCode,
                                           String oldStatus, String newStatus, String message,
                                           String accentColor, String icon) {
        String statusBg;
        switch (newStatus != null ? newStatus.toUpperCase() : "") {
            case "INACTIVE_PENDING": statusBg = "rgba(99,102,241,0.1)";  break;
            case "INACTIVE":         statusBg = "rgba(99,102,241,0.1)";  break;
            case "ACTIVE_PENDING":   statusBg = "rgba(34,197,94,0.1)";   break;
            case "ACTIVE":           statusBg = "rgba(34,197,94,0.1)";   break;
            case "BLOCK_PENDING":    statusBg = "rgba(239,68,68,0.08)";  break;
            case "BLOCKED":          statusBg = "rgba(239,68,68,0.08)";  break;
            default:                 statusBg = "rgba(212,168,67,0.1)";  break;
        }
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + accentColor + ";padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>" + icon + "</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:" + accentColor + ";'>Status Changed: " + sanitize(oldStatus) + " → " + sanitize(newStatus) + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;line-height:1.7;'>" + message + "</p>"

            // Account details table
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + accentColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:13px;color:#475569;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Account Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(entityName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:600;padding:4px 0;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:4px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>New Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:4px 0;color:" + accentColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "</table></div>"

            // Note box
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> This is an automated notification from KalInfotech Admin. "
            + "If you have any questions, please contact us at <a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REPLACEMENT OUTGOING NOTIFICATION (to old admin/user)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementOutgoingNotification(String toEmail, String contactName,
                                                     String entityCode, String replacedBy, String reason,
                                                     String replacementFullName, String replacementEmail,
                                                     String orderedBy) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Your Account Has Been Replaced: " + sanitize(entityCode));
            helper.setText(buildReplacementOutgoingHtml(contactName, entityCode, replacedBy, reason,
                    replacementFullName, replacementEmail, orderedBy), true);
            mailSender.send(message);
            logger.info("Replacement outgoing email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement outgoing — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REPLACEMENT WELCOME (to new admin/user with credentials)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementWelcome(String toEmail, String contactName,
                                        String entityCode, String username, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — You Have Been Assigned as Replacement: " + sanitize(entityCode));
            helper.setText(buildReplacementWelcomeHtml(contactName, entityCode, username, tempPassword), true);
            mailSender.send(message);
            logger.info("Replacement welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement welcome — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Outgoing (old admin/user)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementOutgoingHtml(String contactName, String entityCode,
                                                 String replacedBy, String reason,
                                                 String replacementFullName, String replacementEmail,
                                                 String orderedBy) {
        String safeReason   = (reason != null && !reason.trim().isEmpty()) ? sanitize(reason) : "No reason provided";
        String repName      = (replacementFullName != null && !replacementFullName.trim().isEmpty()) ? sanitize(replacementFullName) : "—";
        String repEmail     = (replacementEmail != null && !replacementEmail.trim().isEmpty()) ? sanitize(replacementEmail) : "—";
        String safeOrdered  = (orderedBy != null && !orderedBy.trim().isEmpty()) ? sanitize(orderedBy) : "—";
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>This is to inform you that your account on ReconXpert.Ai has been handed over to a replacement. Your account is now <strong>INACTIVE</strong>.</p>"
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin:0 0 24px;'>"
            + "<p style='margin:0 0 10px;font-size:15px;font-weight:bold;color:#b91c1c;'>&#x26A0; Account Replaced</p>"
            + "<table cellpadding='0' cellspacing='0' width='100%'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Entity Code:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Actioned By:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(replacedBy) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Ordered By:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + safeOrdered + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Replacement Name:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + repName + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Replacement Email:</td><td style='font-size:13px;color:#1e293b;'>" + repEmail + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Reason:</td><td style='font-size:13px;color:#1e293b;'>" + safeReason + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 16px;'>If you believe this is an error, please contact your KalInfotech administrator immediately.</p>"
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'><strong>Note:</strong> This is an automated notification. Do not reply to this email.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Welcome (new admin/user with credentials)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementWelcomeHtml(String contactName, String entityCode,
                                                String username, String tempPassword) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>You have been assigned as a replacement on ReconXpert.Ai. Your account is now <strong>ACTIVE</strong>. Please find your login credentials below.</p>"
            + "<div style='background:#f0fdf4;border-left:4px solid #16a34a;border-radius:8px;padding:24px;margin:0 0 24px;'>"
            + "<p style='margin:0 0 16px;font-size:15px;font-weight:bold;color:#15803d;'>&#x2705; Your Login Credentials</p>"
            + "<table cellpadding='0' cellspacing='0' width='100%'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;width:140px;'>Entity Code:</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;'>Username:</td>"
            + "<td style='font-size:14px;color:#1e293b;font-weight:bold;letter-spacing:1px;'>" + sanitize(username) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;'>Temporary Password:</td>"
            + "<td><span style='background:#1a1a2e;color:#d4a843;font-size:15px;font-weight:bold;letter-spacing:2px;padding:4px 12px;border-radius:6px;display:inline-block;'>" + sanitize(tempPassword) + "</span></td></tr>"
            + "</table></div>"
            + "<div style='background:#eff6ff;border-left:4px solid #3b82f6;border-radius:6px;padding:12px 16px;margin:0 0 16px;'>"
            + "<p style='margin:0;font-size:13px;color:#1e40af;'><strong>&#128274; Security:</strong> Please change your password immediately after your first login using the Forgot Password option.</p>"
            + "</div>"
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'><strong>Note:</strong> This is an automated notification. Do not share your credentials with anyone. Do not reply to this email.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND Replacement Admin Welcome (onboarding-style with verify link)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementAdminWelcome(String toEmail, String contactName,
                                             String bankCode, String userId,
                                             String defaultPassword, String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — You Have Been Assigned as Replacement Admin");
            helper.setText(buildReplacementAdminWelcomeHtml(contactName, bankCode, userId, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("Replacement admin welcome email sent to: {} | userId: {}", toEmail, userId);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement admin welcome — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Admin Welcome (same format as onboarding, replacement message)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementAdminWelcomeHtml(String name, String bankCode,
                                                     String userId, String defaultPassword,
                                                     String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "You have been designated as a <strong>Replacement Admin</strong> on ReconXpert.Ai. "
            + "Please find your login credentials below and complete email verification to activate your account."
            + "</p>"
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>Bank Code:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(bankCode) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>User ID:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(userId) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Kindly click the button below to verify your email and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#1a1a2e,#0f3460);color:#d4a843;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Email &amp; Set Password</a>"
            + "</td></tr></table>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the login page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>Bank Code:</strong> " + sanitize(bankCode) + "</li>"
            + "<li>Enter your <strong>User ID:</strong> " + sanitize(userId) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete email verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Override
    public void sendReplacementTenureEnded(String toEmail, String contactName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Your Temporary Assignment Has Ended — ReconXpert.Ai");
            helper.setText(buildReplacementTenureEndedHtml(contactName), true);
            mailSender.send(message);
            logger.info("Replacement tenure-ended email sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send tenure-ended email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildReplacementTenureEndedHtml(String contactName) {
        String safe = sanitize(contactName);
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 16px;'>Dear <strong>" + safe + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;line-height:1.7;margin:0 0 20px;'>"
            + "Your temporary assignment on <strong>ReconXpert.Ai</strong> has now ended. "
            + "The original administrator has been reactivated and your account has been set to <strong>Inactive</strong>."
            + "</p>"
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0;font-size:13px;color:#92400e;'>"
            + "If you believe this is an error, please contact your KalInfotech administrator."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Override
    public void sendReplacementBecamePermanent(String toEmail, String contactName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("You Are Now a Permanent Administrator — ReconXpert.Ai");
            helper.setText(buildReplacementBecamePermanentHtml(contactName), true);
            mailSender.send(message);
            logger.info("Replacement became-permanent email sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send became-permanent email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildReplacementBecamePermanentHtml(String contactName) {
        String safe = sanitize(contactName);
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 16px;'>Dear <strong>" + safe + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;line-height:1.7;margin:0 0 20px;'>"
            + "The previous administrator has been permanently removed. You are now the <strong>Permanent Administrator</strong> on <strong>ReconXpert.Ai</strong>."
            + "</p>"
            + "<div style='background:#f0fdf4;border-left:4px solid #16a34a;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>"
            + "Your account now has full permanent admin access. Please log in to continue managing your institution."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND USER WELCOME EMAIL
    // Sent to a newly created REC_USER after an admin adds them via AddUser.
    // Same template as Bank Admin welcome but with "User" description.
    // @Async — fire and forget, won't block API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendUserWelcome(String toEmail, String fullName,
                                String code, String codeLabel, String username,
                                String defaultPassword, String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Welcome! Your User Account Has Been Created");
            helper.setText(buildUserWelcomeHtml(fullName, code, codeLabel, username, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("User welcome email sent to: {} | username: {}", toEmail, username);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] User welcome — recipient: {} | username: {} | reason: {}", toEmail, username, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] User welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendUserWelcomeReplacement(String toEmail, String fullName,
                                           String code, String codeLabel, String username,
                                           String defaultPassword, String verifyLink,
                                           String replacementDescription) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — You’ve Been Added as a Replacement User");
            helper.setText(buildUserWelcomeReplacementHtml(fullName, code, codeLabel, username, defaultPassword, verifyLink, replacementDescription), true);
            mailSender.send(message);
            logger.info("Replacement user welcome email sent to: {} | username: {}", toEmail, username);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement user welcome — recipient: {} | username: {} | reason: {}", toEmail, username, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement user welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    private String buildUserWelcomeReplacementHtml(String fullName, String code, String codeLabel,
                                                    String username, String defaultPassword,
                                                    String verifyLink, String replacementDescription) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(fullName) + "</strong>,</p>"

            // Replacement banner
            + "<div style='background:#eff6ff;border:1px solid #bfdbfe;border-left:4px solid #2563eb;border-radius:8px;padding:14px 18px;margin-bottom:20px;'>"
            + "<p style='margin:0;font-size:14px;color:#1e40af;'>"
            + "<strong>Replacement Assignment:</strong> " + sanitize(replacementDescription)
            + "</p>"
            + "</div>"

            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "Please use the credentials below to verify your account and set your password."
            + "</p>"

            // Credentials box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>" + sanitize(codeLabel) + ":</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(code) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(username) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Click the button below to verify your account and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#1a1a2e,#0f3460);color:#d4a843;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Account &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the verification page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>" + sanitize(codeLabel) + ":</strong> " + sanitize(code) + "</li>"
            + "<li>Enter your <strong>Username:</strong> " + sanitize(username) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    private String buildUserWelcomeHtml(String fullName, String code, String codeLabel,
                                        String username, String defaultPassword,
                                        String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(fullName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "You have been added as a <strong>User</strong> on ReconXpert.Ai. "
            + "Please use the credentials below to verify your account and set your password."
            + "</p>"

            // Credentials box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>" + sanitize(codeLabel) + ":</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(code) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(username) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Click the button below to verify your account and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#1a1a2e,#0f3460);color:#d4a843;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Account &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the verification page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>" + sanitize(codeLabel) + ":</strong> " + sanitize(code) + "</li>"
            + "<li>Enter your <strong>Username:</strong> " + sanitize(username) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #d4a843;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACTOR ACTION CONFIRMATION — sent to parent/actor after every child action
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendActorActionConfirmation(String toEmail, String actorName,
                                             String action, String targetName, String targetCode,
                                             String scheduledAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Action Confirmation: " + action);
            helper.setText(buildActorConfirmationHtml(actorName, action, targetName, targetCode, scheduledAt), true);
            mailSender.send(message);
            logger.info("[ACTOR-CONFIRM] Sent to {} — action: {} on {}", toEmail, action, targetCode);
        } catch (Exception e) {
            logger.warn("[ACTOR-CONFIRM] Email failed for {} — action: {}: {}", toEmail, action, e.getMessage());
        }
    }

    private String buildActorConfirmationHtml(String actorName, String action,
                                               String targetName, String targetCode,
                                               String scheduledAt) {
        String actionColor = action.toLowerCase().contains("block") ? "#ef4444"
                           : action.toLowerCase().contains("inact") ? "#f97316"
                           : action.toLowerCase().contains("react") || action.toLowerCase().contains("cancel") ? "#22c55e"
                           : "#6366f1";
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#d4a843;margin:0;font-size:20px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Action Confirmation</p>"
            + "</td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(actorName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 24px;line-height:1.6;'>This confirms that you have successfully performed the following action on the ReconXpert.Ai platform.</p>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + actionColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;width:40%;font-size:13px;color:#64748b;'>Action</td>"
            + "<td style='padding:6px 0;font-size:13px;font-weight:700;color:" + actionColor + ";'>" + sanitize(action) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Target</td>"
            + "<td style='padding:6px 0;font-size:13px;font-weight:600;color:#1e293b;'>" + sanitize(targetName) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Code</td>"
            + "<td style='padding:6px 0;font-size:13px;color:#475569;font-family:monospace;'>" + sanitize(targetCode) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>When</td>"
            + "<td style='padding:6px 0;font-size:13px;color:#475569;'>" + sanitize(scheduledAt) + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:12px;color:#94a3b8;margin:0;line-height:1.6;'>If you did not perform this action, please contact <strong>support@kalinfotech.com</strong> immediately.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:4px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    // Prevent XSS — sanitize user input before putting in HTML
    private String sanitize(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
}