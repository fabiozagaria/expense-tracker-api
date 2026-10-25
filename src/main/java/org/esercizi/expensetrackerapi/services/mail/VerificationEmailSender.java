package org.esercizi.expensetrackerapi.services.mail;

public interface VerificationEmailSender {
    void send(String recipient, String subject, String text);
}

