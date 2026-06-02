package com.jpb.reconciliation.reconciliation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendForgotPasswordOtp(
            String email,
            String username,
            String otp,
            int expiryMinutes) {

        try {

            // EMAIL MESSAGE
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);

            message.setSubject(
                    "Sub Institute Forgot Password OTP"
            );

            message.setText(

                    "Hello " + username + ",\n\n"

                            + "Your OTP for password reset is: "
                            + otp

                            + "\n\nThis OTP is valid for "
                            + expiryMinutes
                            + " minutes."

                            + "\n\nDo not share this OTP with anyone."

                            + "\n\nRegards,"
                            + "\nKal Recon Team"
            );

            // SEND MAIL
            mailSender.send(message);

            logger.info(
                    "Forgot Password OTP sent successfully to {}",
                    email
            );

        } catch (Exception e) {

            logger.error(
                    "Failed to send OTP email to {} : {}",
                    email,
                    e.getMessage()
            );
        }
    }
}