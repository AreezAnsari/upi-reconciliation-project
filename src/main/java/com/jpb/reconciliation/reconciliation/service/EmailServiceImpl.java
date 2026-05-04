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
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Unexpected email error: " + e.getMessage(), e);
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
        } catch (Exception e) {
            logger.error("Failed to send SuperUser welcome email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
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