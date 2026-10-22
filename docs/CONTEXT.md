# Contesto tecnico — Gestionale Spese Backend

Aggiornato: 2026-10-22

## Obiettivo corrente

Verificare il verticale registrazione → verifica email → login → spese personali → refresh/logout insieme al frontend Angular.

## Stato osservato

- Spring Boot 4.1, Java 21 come target, JPA/MySQL, Spring Security Resource Server e BCrypt.
- `POST /auth/register` crea un utente non verificato e invia un link tramite Mailpit; `POST /auth/verify-email` lo abilita.
- La verifica dello stesso token risponde ancora con successo se l'account risulta già confermato; i token sconosciuti o scaduti per utenti non verificati restano rifiutati.
- `POST /auth/login` restituisce un access token JWT di 15 minuti e imposta un refresh token di 7 giorni in cookie HttpOnly.
- `POST /auth/refresh` ruota il refresh token; `POST /auth/logout` lo revoca.
- `/api/expenses` richiede Bearer JWT; il server ricava l'owner dal principal e filtra lettura, modifica ed eliminazione per proprietario.
- CORS locale centralizzato in `SecurityConfig` per `http://localhost:4200` con credenziali.
- JDK 21 e Maven Wrapper 3.9.16 sono il percorso di riferimento; Lombok è configurato come annotation processor.
- Dockerfile multi-stage con JDK/JRE 21 e Compose per API, MySQL 8 persistente e Mailpit; password e chiave JWT arrivano dall'ambiente.
- I test Maven verificati con JDK 21 usano MySQL reale, email simulata e coprono registrazione, verifica email ripetuta, login, Bearer, refresh e isolamento fra utenti.
- Le verifiche del 22 ottobre hanno usato MySQL e Mailpit temporanei e un container di prova per il JAR. Non attestano la build completa del Dockerfile originale, bloccata dal proxy; dettagli sotto.

## Limiti e prossima verifica

- Resta la prova manuale del link nel browser con Angular e lo stack Docker avviati insieme.
- Cookie `Secure=false`, URL di verifica e CORS sono configurati per sviluppo locale. La pubblicazione dell'API richiede configurazione esplicita degli ambienti.
- Servono altri test sui casi limite del CRUD e della sessione.
- Entrate e dashboard sono implementate e coperte da letture smoke; restano da ampliare i test dedicati e i casi limite.

## Regola di sincronizzazione

Il contratto REST qui implementato è la fonte di verità per `expense-tracker-angular`. Aggiornare questo file e la documentazione pubblica quando cambia il contratto.


## Preparazione demo — 2026-10-12

Profilo backend demo, login condiviso e proxy Vercel pubblicati; verifiche API pubbliche registrate il 12 ottobre 2026 in DEPLOYMENT.md. SMTP resta locale e la registrazione pubblica è disabilitata. La registrazione completa nel browser locale con Angular e Mailpit resta da verificare.

## Registrazione — 2026-10-12

Controllo server di username/email duplicati prima di salvare User e inviare email: HTTP 409 con errorName USERNAME_ALREADY_EXISTS o EMAIL_ALREADY_EXISTS. I vincoli UNIQUE MySQL restano attivi per richieste concorrenti. Email vuota respinta. La password resta BCrypt e il ruolo viene assegnato come USER dal server. Nuovi utenti non verificati non possono fare login. Il profilo demo conserva la registrazione pubblica disabilitata: il provider email HTTPS sarà configurato successivamente.

## Riproducibilità — 2026-10-22

- L'analisi del commit `9c5a681` ha superato 7 test con MySQL 8.0 temporaneo in RAM, compilazione e packaging del JAR con JDK 21. I sorgenti erano montati in sola lettura; nessun database persistente è stato usato.
- Smoke HTTP completati: health, accesso anonimo rifiutato, registrazione con email ricevuta in Mailpit, verifica email, login, CRUD spese (inclusi PUT e PATCH), lettura entrate e dashboard, refresh, logout e refresh revocato rifiutato. Questa prova non include il browser Angular.
- La configurazione Compose è stata validata. La build Docker originale si è fermata al download Maven per il certificato del proxy dell'ambiente remoto; non è dichiarata riuscita. Il JAR è stato verificato in un container di prova con configurazione proxy/trust esterna al repository.
- README distingue ambiente Compose e avvio Maven. `.env` è letto da Compose; Maven richiede variabili esportate. I test richiedono un database MySQL dedicato e isolato.
- Restano la verifica integrale della build/avvio Compose originale in un ambiente HTTPS funzionante e la registrazione locale nel browser con Angular.
