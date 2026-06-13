package com.jpb.reconciliation.reconciliation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Central email service.
 *
 * Methods:
 *  1. sendUserWelcomeEmail     — called when user is created + role mapped
 *  2. sendForgotPasswordOtp    — called from forgotPassword flow (existing KalSuperUser + new AddUser)
 *  3. sendLoginOtp             — called from login flow (existing KalSuperUser + new AddUser)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ── Sender address — match your application.properties spring.mail.username ──
    private static final String FROM = "noreply@jpbreconciliation.com";

    // =========================================================================
    // 1. WELCOME EMAIL — sent when user is created and role is mapped
    //    Contains: institutionCode, email (username), defaultPassword, Verify button
    // =========================================================================

    /**
     * Sends a welcome / credential email to the newly created user.
     *
     * Contains ONLY 3 things:
     *   1. Institution Code
     *   2. Email (shown so user knows which email to use as reference)
     *   3. Default Password
     *
     * User visits the login page directly and enters these credentials.
     * Since passwordSet=0, the login page shows the Set New Password form inline.
     *
     * @param toEmail         user's email address (also the destination)
     * @param fullName        user's full name (for greeting)
     * @param institutionCode institution code to enter at login
     * @param username        username (separate from email — e.g. rajesh.kumar)
     * @param defaultPassword auto-generated plain-text default password
     * @param loginPageUrl    URL of the login page (shown as a plain link, not a button)
     */
    public void sendUserWelcomeEmail(String toEmail,
                                     String fullName,
                                     String institutionCode,
                                     String username,
                                     String defaultPassword,
                                     String loginPageUrl) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM);
        helper.setTo(toEmail);
        helper.setSubject("Your ReconXpert.Ai Account Credentials");

        String html = buildWelcomeEmailHtml(fullName, institutionCode, username, defaultPassword, loginPageUrl);

        helper.setText(html, true);
        mailSender.send(message);

        log.info("[EMAIL] Welcome email sent to: {} (username={})", toEmail, username);
    }

    // =========================================================================
    // 2. FORGOT PASSWORD OTP EMAIL
    //    Reused by both KalSuperUser flow and new AddUser flow
    // =========================================================================

    /**
     * Sends a forgot-password OTP email.
     *
     * @param toEmail         user's email
     * @param username        user's username (for greeting)
     * @param otp             6-digit OTP
     * @param expiryMinutes   OTP validity in minutes (typically 10)
     */
    public void sendForgotPasswordOtp(String toEmail,
                                      String username,
                                      String otp,
                                      int expiryMinutes) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM);
        helper.setTo(toEmail);
        helper.setSubject("Password Reset OTP");

        String html = buildForgotPasswordOtpHtml(username, otp, expiryMinutes);
        helper.setText(html, true);
        mailSender.send(message);

        log.info("[EMAIL] Forgot-password OTP sent to: {}", toEmail);
    }

    // =========================================================================
    // 3. LOGIN OTP EMAIL
    //    Sent after successful password verification at login
    // =========================================================================

    /**
     * Sends the login OTP email.
     *
     * @param toEmail  user's email
     * @param username user's username (for greeting)
     * @param otp      6-digit OTP
     */
    public void sendLoginOtp(String toEmail,
                             String username,
                             String otp) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM);
        helper.setTo(toEmail);
        helper.setSubject("Your Login OTP");

        String html = buildLoginOtpHtml(username, otp);
        helper.setText(html, true);
        mailSender.send(message);

        log.info("[EMAIL] Login OTP sent to: {}", toEmail);
    }

    // =========================================================================
    // HTML Builders
    // =========================================================================

    /**
     * Builds the welcome email HTML.
     *
     * Shows ONLY 3 credentials:
     *   Institution Code | Username | Default Password
     *
     * Steps shown to user:
     *   1. Visit login page
     *   2. Enter the 3 credentials
     *   3. Set a new password (shown inline on login page)
     *   4. Verify OTP → account active
     */
    private String buildWelcomeEmailHtml(String fullName,
                                          String institutionCode,
                                          String username,
                                          String defaultPassword,
                                          String loginPageUrl) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "  * { box-sizing: border-box; margin: 0; padding: 0; }" +
               "  body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5;" +
               "         margin: 0; padding: 24px 0; }" +
               "  .wrapper { max-width: 560px; margin: 0 auto; }" +

               // Header — dark navy matching ReconXpert brand
               "  .header { background: #0d1b3e; border-radius: 10px 10px 0 0;" +
               "            padding: 28px 36px; }" +
               "  .header-top { display: flex; align-items: center; margin-bottom: 6px; }" +
               "  .brand { font-size: 20px; font-weight: 700; color: #ffffff; }" +
               "  .brand span { color: #f5a623; }" +          // orange accent
               "  .tagline { font-size: 12px; color: #7ecfcf; margin-top: 4px; }" +

               // Body card
               "  .card { background: #ffffff; padding: 36px; border-radius: 0 0 10px 10px;" +
               "          box-shadow: 0 4px 16px rgba(0,0,0,0.08); }" +
               "  .greeting { font-size: 15px; color: #333; margin-bottom: 12px; }" +
               "  .intro { font-size: 14px; color: #555; line-height: 1.6; margin-bottom: 24px; }" +

               // Credential boxes — 3 individual cards
               "  .cred-section { margin-bottom: 28px; }" +
               "  .cred-label { font-size: 11px; font-weight: 700; text-transform: uppercase;" +
               "                letter-spacing: 0.8px; color: #888; margin-bottom: 6px; }" +
               "  .cred-value { background: #f7f8fc; border: 1.5px solid #e3e6f0;" +
               "                border-radius: 8px; padding: 14px 18px;" +
               "                font-size: 18px; font-weight: 700; color: #0d1b3e;" +
               "                letter-spacing: 1px; word-break: break-all; }" +
               "  .cred-value.password { color: #d4380d; background: #fff2e8;" +
               "                         border-color: #ffbb96; }" +

               // Steps
               "  .steps-title { font-size: 13px; font-weight: 700; color: #333;" +
               "                 margin-bottom: 12px; }" +
               "  .steps { list-style: none; padding: 0; }" +
               "  .steps li { font-size: 13px; color: #555; padding: 7px 0 7px 28px;" +
               "              position: relative; border-bottom: 1px solid #f0f0f0; }" +
               "  .steps li:last-child { border-bottom: none; }" +
               "  .steps li::before { content: attr(data-step); position: absolute; left: 0;" +
               "                      top: 7px; width: 18px; height: 18px; background: #0d1b3e;" +
               "                      color: #fff; font-size: 10px; font-weight: 700;" +
               "                      border-radius: 50%; display: flex; align-items: center;" +
               "                      justify-content: center; text-align: center;" +
               "                      line-height: 18px; }" +

               // Login button
               "  .btn-wrap { text-align: center; margin: 28px 0 20px; }" +
               "  .btn { display: inline-block; padding: 13px 40px;" +
               "         background: #f5a623; color: #ffffff !important;" +
               "         text-decoration: none; border-radius: 8px;" +
               "         font-size: 15px; font-weight: 700; letter-spacing: 0.3px; }" +

               // Warning
               "  .warning { background: #fff8e1; border-left: 4px solid #f5a623;" +
               "             border-radius: 6px; padding: 12px 16px; font-size: 12px;" +
               "             color: #7a5c00; line-height: 1.5; }" +

               // Footer
               "  .footer { text-align: center; margin-top: 20px; font-size: 11px; color: #aaa; }" +
               "</style></head><body>" +
               "<div class='wrapper'>" +

               // ── Header ────────────────────────────────────────────────────
               "  <div class='header'>" +
               "    <div class='brand'>Recon<span>Xpert</span>.Ai</div>" +
               "    <div class='tagline'>Institution Management · Kal Infotech</div>" +
               "  </div>" +

               // ── Card ──────────────────────────────────────────────────────
               "  <div class='card'>" +
               "    <p class='greeting'>Dear <strong>" + escapeHtml(fullName) + "</strong>,</p>" +
               "    <p class='intro'>Your account has been created on <strong>ReconXpert.Ai</strong>. " +
               "      Use the credentials below to log in and set your new password.</p>" +

               // Credential 1 — Institution Code
               "    <div class='cred-section'>" +
               "      <div class='cred-label'>Institution Code</div>" +
               "      <div class='cred-value'>" + escapeHtml(institutionCode) + "</div>" +
               "    </div>" +

               // Credential 2 — Username
               "    <div class='cred-section'>" +
               "      <div class='cred-label'>Username</div>" +
               "      <div class='cred-value'>" + escapeHtml(username) + "</div>" +
               "    </div>" +

               // Credential 3 — Default Password (highlighted in orange-red)
               "    <div class='cred-section'>" +
               "      <div class='cred-label'>Default Password (temporary)</div>" +
               "      <div class='cred-value password'>" + escapeHtml(defaultPassword) + "</div>" +
               "    </div>" +

               // Steps
               "    <p class='steps-title'>What to do next:</p>" +
               "    <ol class='steps'>" +
               "      <li data-step='1'>Visit the login page and enter the credentials above</li>" +
               "      <li data-step='2'>You will be prompted to set a new password on the same page</li>" +
               "      <li data-step='3'>After setting your password, an OTP will be sent to this email</li>" +
               "      <li data-step='4'>Enter the OTP — your account will become active</li>" +
               "    </ol>" +

               // Login button
               "    <div class='btn-wrap'>" +
               "      <a href='" + escapeHtml(loginPageUrl) + "' class='btn'>Go to Login Page &rarr;</a>" +
               "    </div>" +

               // Warning
               "    <div class='warning'>" +
               "      &#9888;&nbsp; <strong>Keep these credentials safe.</strong> " +
               "      Do not share your password with anyone. " +
               "      This default password will be invalidated once you set a new one." +
               "    </div>" +
               "  </div>" +

               // Footer
               "  <div class='footer'>This is an automated email from ReconXpert.Ai &mdash; " +
               "please do not reply.</div>" +
               "</div>" +
               "</body></html>";
    }

    private String buildForgotPasswordOtpHtml(String username, String otp, int expiryMinutes) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }" +
               "  .container { max-width: 600px; margin: 30px auto; background: #ffffff;" +
               "               border-radius: 8px; overflow: hidden;" +
               "               box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background: #1a237e; color: #ffffff; padding: 28px 32px; }" +
               "  .header h1 { margin: 0; font-size: 22px; }" +
               "  .body { padding: 32px; color: #333333; text-align: center; }" +
               "  .otp-box { font-size: 36px; font-weight: bold; letter-spacing: 10px;" +
               "             color: #1a237e; background: #e8eaf6; padding: 16px 32px;" +
               "             border-radius: 8px; display: inline-block; margin: 20px 0; }" +
               "  .footer { background: #f5f5f5; padding: 18px 32px; font-size: 12px;" +
               "            color: #888; border-top: 1px solid #e0e0e0; text-align: center; }" +
               "</style></head><body>" +
               "<div class='container'>" +
               "  <div class='header'><h1>Password Reset Request</h1></div>" +
               "  <div class='body'>" +
               "    <p>Hello <strong>" + escapeHtml(username) + "</strong>,</p>" +
               "    <p>Use the OTP below to reset your password:</p>" +
               "    <div class='otp-box'>" + escapeHtml(otp) + "</div>" +
               "    <p>This OTP is valid for <strong>" + expiryMinutes + " minutes</strong>.</p>" +
               "    <p style='color:#888; font-size:13px;'>If you did not request a password reset, ignore this email.</p>" +
               "  </div>" +
               "  <div class='footer'>This is an automated email. Please do not reply.</div>" +
               "</div>" +
               "</body></html>";
    }

    private String buildLoginOtpHtml(String username, String otp) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }" +
               "  .container { max-width: 600px; margin: 30px auto; background: #ffffff;" +
               "               border-radius: 8px; overflow: hidden;" +
               "               box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background: #1a237e; color: #ffffff; padding: 28px 32px; }" +
               "  .header h1 { margin: 0; font-size: 22px; }" +
               "  .body { padding: 32px; color: #333333; text-align: center; }" +
               "  .otp-box { font-size: 36px; font-weight: bold; letter-spacing: 10px;" +
               "             color: #1a237e; background: #e8eaf6; padding: 16px 32px;" +
               "             border-radius: 8px; display: inline-block; margin: 20px 0; }" +
               "  .footer { background: #f5f5f5; padding: 18px 32px; font-size: 12px;" +
               "            color: #888; border-top: 1px solid #e0e0e0; text-align: center; }" +
               "</style></head><body>" +
               "<div class='container'>" +
               "  <div class='header'><h1>Login OTP</h1></div>" +
               "  <div class='body'>" +
               "    <p>Hello <strong>" + escapeHtml(username) + "</strong>,</p>" +
               "    <p>Your one-time login OTP is:</p>" +
               "    <div class='otp-box'>" + escapeHtml(otp) + "</div>" +
               "    <p>This OTP is valid for <strong>5 minutes</strong>.</p>" +
               "    <p style='color:#888; font-size:13px;'>Do not share this OTP with anyone.</p>" +
               "  </div>" +
               "  <div class='footer'>This is an automated email. Please do not reply.</div>" +
               "</div>" +
               "</body></html>";
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}