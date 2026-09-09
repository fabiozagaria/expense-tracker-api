# Contesto tecnico — Gestionale Spese Backend

Aggiornato: 2026-09-09

## Obiettivo corrente
Mantenere la REST API Expense coerente e ripristinare il backend in stato compilabile/avviabile dopo l'introduzione sperimentale di `User`, relazione `owner`, Lombok e Spring Security.

## Stato osservato
- Versione dichiarata: 0.3.0-SNAPSHOT.
- Java 21, Spring Boot 4.1, Spring Data JPA, Jakarta Validation e MySQL.
- CRUD Expense implementato: GET, GET by id, POST, PUT, PATCH e DELETE.
- DTO separati, metodi transazionali, enum `ExpenseCategory`, `EntityManager` e gestione not-found dedicata.
- POST, DELETE e PATCH sono stati verificati manualmente end-to-end con Angular e persistenza MySQL prima delle ultime modifiche strutturali.
- PATCH descrizione corretto: `null` significa campo non modificato; stringa vuota è accettata per svuotare la descrizione.
- Lombok introdotto sulle entity.
- Introdotta una base non ancora completata per `User`, relazione `Expense.owner` e configurazione Spring Security stateless/JWT.

## WIP / blocco corrente
Il backend non è considerato Done dopo le ultime modifiche perché lo stato attuale richiede riallineamento prima di proseguire:
- `Expense` ora contiene anche `owner`, ma il mapper `toExpense(...)` usa ancora la vecchia costruzione dell'entity senza owner;
- `User.expenseList` è una relazione `@OneToMany` e non deve essere trattata come una normale colonna con `@Column`;
- la configurazione Security/JWT è solo uno scaffold e va verificata insieme alle dipendenze effettive prima di essere considerata funzionante;
- i test automatici restano insufficienti e non c'è CI backend verificata.

## Prossima azione
Ripristinare prima il backend in stato compilabile e avviabile: correggere il mapping `Expense` ↔ `User` e il mapping di creazione della spesa. Solo dopo rieseguire i test del verticale Expense e continuare con Spring Security.

## Priorità tecniche successive
- consolidare la modellazione `User`/owner senza rompere il CRUD esistente;
- introdurre gradualmente `UserDetails`/principal e autenticazione solo dopo il ripristino del verticale;
- aggiungere test JUnit/Mockito e test di integrazione;
- consolidare validazione ed error handling;
- configurare correttamente CORS e ambienti.

## Regola di sincronizzazione
Il contratto REST realmente implementato qui è la fonte di verità per `expense-tracker-angular`. Non documentare sicurezza, utenti o autenticazione come completati finché non compilano, non si avviano e non sono verificati. A fine sessione significativa aggiornare questo file insieme alla scheda Notion del progetto.