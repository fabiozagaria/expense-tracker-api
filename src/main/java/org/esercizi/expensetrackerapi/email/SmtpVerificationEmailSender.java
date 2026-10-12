package org.esercizi.expensetrackerapi.email;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpVerificationEmailSender implements VerificationEmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpVerificationEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from.isBlank() ? "noreply@expense-tracker.local" : from;
    }

    @Override
    public void send(String recipient, String subject, String textContent, String htmlContent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(textContent);
        try {
            mailSender.send(message);
        } catch (MailException ignored) {
            throw new EmailDeliveryException();
        }
    }
}
