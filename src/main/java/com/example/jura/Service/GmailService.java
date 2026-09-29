package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${spring.mail.password:}")
    private String appPassword;

    public boolean isConfigured() {
        return validAddress(senderEmail) && appPassword != null && !appPassword.isBlank();
    }

    public void sendEmail(String recipient, String subject, String message) {
        sendEmail(recipient, subject, message, null);
    }

    public void sendEmail(String recipient, String subject, String message, String htmlMessage) {
        if (!isConfigured()) {
            log.warn("Gmail SMTP is not configured; set GMAIL_SENDER_EMAIL and GMAIL_APP_PASSWORD");
            throw new ApiException("Gmail SMTP is not configured");
        }
        if (!validAddress(recipient) || subject == null || subject.isBlank()
                || subject.contains("\r") || subject.contains("\n") || message == null) {
            log.warn("Gmail email fields are invalid; email was not sent");
            throw new ApiException("Email fields are invalid");
        }

        try {
            MimeMessage email = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(email, htmlMessage != null, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            if (htmlMessage == null) {
                helper.setText(message, false);
            } else {
                helper.setText(message, htmlMessage);
                helper.addInline("jura-logo", new ClassPathResource("static/logo.png"), "image/png");
            }
            mailSender.send(email);
            return;
        } catch (MailAuthenticationException exception) {
            log.warn("Gmail SMTP authentication failed; check sender email and Google App Password");
            throw new ApiException("Gmail SMTP authentication failed");
        } catch (MailException | MessagingException exception) {
            // Do not log provider responses, passwords, recipients or email contents.
            log.warn("Gmail SMTP email failed ({})", exception.getClass().getSimpleName());
        }
        throw new ApiException("Gmail SMTP could not send the email");
    }

    private boolean validAddress(String address) {
        // Accept a single plain email address, not a display name or recipient list.
        return address != null && address.length() <= 254
                && address.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");
    }
}
