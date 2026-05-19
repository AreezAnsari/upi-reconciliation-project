package com.jpb.reconciliation.reconciliation.service;

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
    // SEND SUPER USER WELCOME EMAIL
    // Includes: Institution Code, User ID, Default Password, Verify Link
    // @Async — runs in background, won't block the API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendSuperUserWelcome(String toEmail, String superUserName,
                                     String institutionName, String institutionCode,
                                     String superUserId, String defaultPassword,
                                     String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — You have been added as Super User of " + institutionName);
            helper.setText(buildSuperUserWelcomeHtml(
                superUserName, institutionName, institutionCode,
                superUserId, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("SuperUser welcome email sent to: {} | userId: {}", toEmail, superUserId);
        } catch (MessagingException e) {
            // @Async — exception won't reach caller; institution must NOT be rolled back on email failure
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
    // HTML TEMPLATE — Super User Welcome Email
    // Contains: Institution Code, User ID, Default Password, Verify button
    // ─────────────────────────────────────────────────────────────────────
    private String buildSuperUserWelcomeHtml(String name, String institutionName,
                                              String institutionCode, String superUserId,
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
            + "You have been added as the <strong>Super User</strong> of "
            + "<strong>" + sanitize(institutionName) + "</strong> on ReconXpert.Ai by KalInfotech Admin."
            + "</p>"

            // Credentials box — Institution Code, User ID, Default Password
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>Institution Code:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(institutionCode) + "</span></p>"
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
            + "<li>Enter your <strong>Institution Code:</strong> " + sanitize(institutionCode) + "</li>"
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
    // Sent when Admin changes institution status: INACTIVE / BLOCKED / RETIRED / ACTIVE
    // @Async — fire and forget, won't block API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendStatusChangeNotification(String toEmail, String superUserName,
                                              String institutionName, String institutionCode,
                                              String oldStatus, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Institution Status Update: " + institutionName);
            helper.setText(buildStatusChangeHtml(superUserName, institutionName, institutionCode, oldStatus, newStatus), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Status change — recipient: {} | institution: {} | {} → {}", toEmail, institutionCode, oldStatus, newStatus);
        } catch (MessagingException e) {
            // @Async — status update must NOT be blocked by email failure
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — recipient: {} | institution: {} | {} → {} | reason: {}", toEmail, institutionCode, oldStatus, newStatus, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — unexpected error — recipient: {} | institution: {} | reason: {}", toEmail, institutionCode, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Status Change Notification Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildStatusChangeHtml(String name, String institutionName,
                                          String institutionCode,
                                          String oldStatus, String newStatus) {

        // Color + icon per status
        String statusColor, statusBg, statusIcon, statusMessage;
        switch (newStatus.toUpperCase()) {
            case "INACTIVE":
                statusColor  = "#6366f1";
                statusBg     = "rgba(99,102,241,0.1)";
                statusIcon   = "⏸";
                statusMessage = "Your institution account has been marked <strong>Inactive</strong>. "
                    + "You will not be able to access the platform until it is reactivated. "
                    + "Please contact KalInfotech Admin for assistance.";
                break;
            case "BLOCKED":
                statusColor  = "#ef4444";
                statusBg     = "rgba(239,68,68,0.1)";
                statusIcon   = "🚫";
                statusMessage = "Your institution account has been <strong>Blocked</strong> by KalInfotech Admin. "
                    + "Access to the ReconXpert.Ai platform has been restricted. "
                    + "Please contact KalInfotech Admin immediately for clarification.";
                break;
            case "RETIRED":
                statusColor  = "#a855f7";
                statusBg     = "rgba(168,85,247,0.1)";
                statusIcon   = "🔒";
                statusMessage = "Your institution account has been <strong>Retired</strong>. "
                    + "This is a permanent action. Access to ReconXpert.Ai has been permanently closed. "
                    + "Please contact KalInfotech Admin if you believe this is an error.";
                break;
            case "ACTIVE":
                statusColor  = "#22c55e";
                statusBg     = "rgba(34,197,94,0.1)";
                statusIcon   = "✅";
                statusMessage = "Your institution account has been <strong>Activated</strong>. "
                    + "You can now access the ReconXpert.Ai platform using your credentials.";
                break;
            default:
                statusColor  = "#d4a843";
                statusBg     = "rgba(212,168,67,0.1)";
                statusIcon   = "ℹ";
                statusMessage = "Your institution status has been updated to <strong>" + sanitize(newStatus) + "</strong>.";
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

            // Institution details box
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:13px;color:#475569;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Institution Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Institution Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(institutionName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Institution Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:600;padding:4px 0;'>" + sanitize(institutionCode) + "</td></tr>"
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
    public void sendSubInstituteStatusNotification(String toEmail, String contactName,
                                                   String subInstitutionName, String subInstitutionCode,
                                                   String oldStatus, String newStatus,
                                                   String parentInstitutionName, String parentInstitutionCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Sub-Institute Status Update: " + subInstitutionName);
            helper.setText(buildSubInstituteStatusHtml(
                    contactName, subInstitutionName, subInstitutionCode,
                    oldStatus, newStatus, parentInstitutionName, parentInstitutionCode), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub-institute cascade — recipient: {} | sub: {} | {} → {} | parent: {}",
                    toEmail, subInstitutionCode, oldStatus, newStatus, parentInstitutionCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-institute cascade — recipient: {} | sub: {} | reason: {}",
                    toEmail, subInstitutionCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-institute cascade — unexpected — recipient: {} | sub: {} | reason: {}",
                    toEmail, subInstitutionCode, e.getMessage());
        }
    }

    private String buildSubInstituteStatusHtml(String contactName,
                                                String subInstitutionName, String subInstitutionCode,
                                                String oldStatus, String newStatus,
                                                String parentInstitutionName, String parentInstitutionCode) {

        String statusColor, statusBg, statusIcon, statusHeading, statusMessage, actionNote;

        switch (newStatus.toUpperCase()) {
            case "BLOCKED":
                statusColor   = "#ef4444";
                statusBg      = "rgba(239,68,68,0.08)";
                statusIcon    = "🚫";
                statusHeading = "Platform Access Suspended";
                statusMessage = "We wish to inform you that your institution's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>suspended</strong> "
                    + "with immediate effect, in accordance with a compliance directive issued by "
                    + "<strong>KalInfotech Administration</strong> applicable to <strong>"
                    + sanitize(parentInstitutionName) + "</strong> and all associated institutions. "
                    + "During this period, platform login and all reconciliation operations will be unavailable.";
                actionNote    = "To understand the reason for this action or to request reinstatement, "
                    + "please reach out to your designated KalInfotech Relationship Manager or write to us at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>. "
                    + "Please quote your Institution Code when contacting support.";
                break;
            case "RETIRED":
                statusColor   = "#a855f7";
                statusBg      = "rgba(168,85,247,0.08)";
                statusIcon    = "🔒";
                statusHeading = "Institution Permanently Decommissioned";
                statusMessage = "We regret to inform you that your institution has been <strong>permanently "
                    + "decommissioned</strong> from the <strong>ReconXpert.Ai</strong> platform, effective immediately. "
                    + "This action has been taken in connection with the decommissioning of "
                    + "<strong>" + sanitize(parentInstitutionName) + "</strong> from our platform network. "
                    + "All platform access, data operations, and reconciliation services for your institution have been permanently closed.";
                actionNote    = "If you believe this action has been taken in error, or if you require a formal "
                    + "explanation, please contact KalInfotech Administration at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>. "
                    + "Please note that this is a permanent action and cannot be reversed once confirmed.";
                break;
            case "ACTIVE":
                statusColor   = "#22c55e";
                statusBg      = "rgba(34,197,94,0.08)";
                statusIcon    = "✅";
                statusHeading = "Platform Access Reinstated";
                statusMessage = "We are pleased to inform you that your institution's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>reinstated</strong>. "
                    + "This follows the reactivation of <strong>" + sanitize(parentInstitutionName) + "</strong> "
                    + "by KalInfotech Administration. You may now resume normal platform operations "
                    + "using your existing Super User credentials.";
                actionNote    = "If you experience any difficulty accessing the platform or require assistance, "
                    + "please contact our support team at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#d4a843;'>support@kalinfotech.com</a>.";
                break;
            default:
                statusColor   = "#d4a843";
                statusBg      = "rgba(212,168,67,0.08)";
                statusIcon    = "ℹ";
                statusHeading = "Institution Status Update";
                statusMessage = "Your institution's status on the ReconXpert.Ai platform has been updated "
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
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Super User, " + sanitize(subInstitutionName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 28px;line-height:1.8;'>" + statusMessage + "</p>"

            // Institution details card
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#94a3b8;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Your Institution Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;width:170px;'>Institution Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(subInstitutionName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Institution Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(subInstitutionCode) + "</td></tr>"
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
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Network Institution</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(parentInstitutionName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentInstitutionCode) + ")</span></td></tr>"
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