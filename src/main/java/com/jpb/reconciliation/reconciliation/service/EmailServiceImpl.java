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

    // Reads from application-sit.yml → app.mail.from
    @Value("${app.mail.from}")
    private String fromEmail;

    // Reads from application-sit.yml → app.mail.from-name
    @Value("${app.mail.from-name}")
    private String fromName;

    // ─────────────────────────────────────────────────────────────────────
    // SEND FORGOT PASSWORD OTP EMAIL
    // @Async — runs in background thread, won't block the API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void sendForgotPasswordOtp(String toEmail, String userName,
                                      String otpCode, int expiryMins) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // true = multipart (needed for HTML), false = not inline
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai — Your Password Reset OTP");
            helper.setText(buildOtpEmailHtml(userName, otpCode, expiryMins), true); // true = isHtml

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            // Throw so ForgotPasswordServiceImpl can return proper error to user
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);

        } catch (Exception e) {
            logger.error("Unexpected error while sending OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Unexpected email error: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML EMAIL TEMPLATE
    // Professional, clean design — matches ReconXpert.Ai branding
    // ─────────────────────────────────────────────────────────────────────
    private String buildOtpEmailHtml(String userName, String otpCode, int expiryMins) {
        return "<!DOCTYPE html>"
            + "<html lang='en'>"
            + "<head>"
            + "  <meta charset='UTF-8'>"
            + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "  <title>Password Reset OTP</title>"
            + "</head>"
            + "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,Helvetica,sans-serif;'>"

            // ── Outer wrapper ──
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f9;padding:40px 0;'>"
            + "<tr><td align='center'>"

            // ── Email card ──
            + "<table width='600' cellpadding='0' cellspacing='0' style='"
            + "  background-color:#ffffff;"
            + "  border-radius:12px;"
            + "  overflow:hidden;"
            + "  box-shadow:0 4px 20px rgba(0,0,0,0.08);"
            + "'>"

            // ── Header ──
            + "<tr>"
            + "  <td style='"
            + "    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);"
            + "    padding:32px 40px;"
            + "    text-align:center;"
            + "  '>"
            + "    <h1 style='color:#d4a843;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "    <p style='color:#94a3b8;margin:6px 0 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "  </td>"
            + "</tr>"

            // ── Body ──
            + "<tr>"
            + "  <td style='padding:40px 40px 20px 40px;'>"

            // Greeting
            + "    <p style='font-size:16px;color:#1e293b;margin:0 0 8px 0;'>Dear <strong>" + sanitize(userName) + "</strong>,</p>"
            + "    <p style='font-size:14px;color:#64748b;margin:0 0 28px 0;'>We received a request to reset your password on ReconXpert.Ai. Use the OTP below to proceed.</p>"

            // OTP Box
            + "    <table width='100%' cellpadding='0' cellspacing='0'>"
            + "    <tr><td align='center' style='padding:8px 0 28px 0;'>"
            + "      <div style='"
            + "        background:#f8fafc;"
            + "        border:2px dashed #d4a843;"
            + "        border-radius:12px;"
            + "        padding:24px 32px;"
            + "        display:inline-block;"
            + "        text-align:center;"
            + "      '>"
            + "        <p style='margin:0 0 8px 0;font-size:12px;color:#94a3b8;text-transform:uppercase;letter-spacing:2px;'>Your One-Time Password</p>"
            + "        <p style='margin:0;font-size:40px;font-weight:bold;color:#1a1a2e;letter-spacing:10px;'>" + sanitize(otpCode) + "</p>"
            + "        <p style='margin:8px 0 0 0;font-size:12px;color:#ef4444;'>Valid for " + expiryMins + " minutes only</p>"
            + "      </div>"
            + "    </td></tr>"
            + "    </table>"

            // Instructions
            + "    <p style='font-size:13px;color:#64748b;margin:0 0 8px 0;'>To reset your password:</p>"
            + "    <ol style='font-size:13px;color:#64748b;margin:0 0 24px 0;padding-left:20px;line-height:1.8;'>"
            + "      <li>Enter this OTP on the password reset page</li>"
            + "      <li>Set your new password (min 8 chars, 1 uppercase, 1 number, 1 special character)</li>"
            + "      <li>Log in with your new credentials</li>"
            + "    </ol>"

            // Warning box
            + "    <div style='"
            + "      background:#fef2f2;"
            + "      border-left:4px solid #ef4444;"
            + "      border-radius:6px;"
            + "      padding:12px 16px;"
            + "      margin-bottom:24px;"
            + "    '>"
            + "      <p style='margin:0;font-size:12px;color:#991b1b;'>"
            + "        <strong>Security Notice:</strong> Never share this OTP with anyone. "
            + "        KalInfotech will never ask for your OTP. "
            + "        If you did not request this, please ignore this email — your account is safe."
            + "      </p>"
            + "    </div>"

            + "  </td>"
            + "</tr>"

            // ── Footer ──
            + "<tr>"
            + "  <td style='"
            + "    background:#f8fafc;"
            + "    border-top:1px solid #e2e8f0;"
            + "    padding:20px 40px;"
            + "    text-align:center;"
            + "  '>"
            + "    <p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "    <p style='margin:6px 0 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "  </td>"
            + "</tr>"

            + "</table>"  // end card
            + "</td></tr>"
            + "</table>"  // end outer
            + "</body></html>";
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