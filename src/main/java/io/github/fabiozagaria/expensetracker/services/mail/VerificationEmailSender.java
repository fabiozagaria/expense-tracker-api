package io.github.fabiozagaria.expensetracker.services.mail;

public interface VerificationEmailSender {
    void send(String recipient, String subject, String text, String html);
}

