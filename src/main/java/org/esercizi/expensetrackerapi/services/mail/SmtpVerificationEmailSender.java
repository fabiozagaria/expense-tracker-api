package org.esercizi.expensetrackerapi.services.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpVerificationEmailSender implements VerificationEmailSender {
    private final JavaMailSender sender;
    private final String from;

    public SmtpVerificationEmailSender(JavaMailSender sender,
            @Value("${app.mail.from:noreply@expense-tracker.local}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        sender.send(message);
    }
}

