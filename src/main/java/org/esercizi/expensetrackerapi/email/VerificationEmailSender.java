package org.esercizi.expensetrackerapi.email;

public interface VerificationEmailSender {
    void send(String recipient, String subject, String textContent, String htmlContent);
}
