# Contesto tecnico — Gestionale Spese Backend

Aggiornato: 2026-09-08

## Obiettivo corrente
Fornire la REST API persistente del verticale Expense e mantenerla allineata al frontend Angular.

## Stato osservato
- Versione dichiarata: 0.3.0-SNAPSHOT.
- Java 21, Spring Boot 4.1, Spring Data JPA, Jakarta Validation e MySQL.
- CRUD Expense completo: GET, GET by id, POST, PUT, PATCH e DELETE.
- DTO separati, metodi transazionali, enum `ExpenseCategory` e gestione dell'assenza tramite eccezione dedicata.
- Test attuali concentrati soprattutto sull'avvio del contesto.

## Priorità tecniche dichiarate
- aggiungere test JUnit/Mockito e test di integrazione;
- consolidare validazione ed error handling;
- configurare correttamente CORS/ambienti;
- verificare l'intero verticale con il frontend;
- introdurre più avanti entrate, utenti e autenticazione.

## Regola di sincronizzazione
Il contratto REST realmente implementato qui è la fonte di verità per `expense-tracker-angular`. Non documentare come completati domini o sicurezza finché non sono presenti nel codice.