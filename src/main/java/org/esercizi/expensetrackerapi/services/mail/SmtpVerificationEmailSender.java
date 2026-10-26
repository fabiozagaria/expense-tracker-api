package org.esercizi.expensetrackerapi.services.mail;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpVerificationEmailSender implements VerificationEmailSender {
    private final JavaMailSender sender;
    private final String from;

    public SmtpVerificationEmailSender(JavaMailSender sender,
            String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String recipient, String subject, String text, String html) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException ignored) {
            throw new EmailDeliveryException();
        }
    }
}

