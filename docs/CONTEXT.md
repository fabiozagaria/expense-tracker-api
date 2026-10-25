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


## Brevo HTTPS — 2026-10-25

Trasporto email separato tramite VerificationEmailSender: SMTP/Mailpit predefinito, Brevo selezionato con MAIL_PROVIDER=brevo. MAIL_FROM e BREVO_API_KEY sono configurazione esterna. Il profilo demo consente REGISTRATION_ENABLED mantenendo false come default. Errori Brevo: 503 EMAIL_DELIVERY_FAILED e rollback della registrazione. Aggiunti test HTTP simulati; in questo ambiente non eseguiti perché Maven Central non è raggiungibile e il JDK disponibile è 17 anziché 21. Invio reale e deploy non verificati.

## Verifica deploy registrazione — 2026-10-25

Backend Render live: deploy `dep-dbf1v8ek1f9s73fptho0`, commit `76fec7db0f4f86fb9764ca087836e12df7c094c7`. Il servizio segue ancora `demo/render-aiven-setup`, avanzato senza force dal vecchio commit alla versione integrata in master. Profilo demo conservato; MAIL_PROVIDER=brevo e REGISTRATION_ENABLED=true configurati su Render preservando i segreti esistenti.

Verifiche completate: 3 test Brevo con Java 21 passati; 56 test Angular passati e build production riuscita con gli avvisi sui budget già presenti. Browser pubblico /register: pulsante Registrati abilitato. API pubblica: health 200, username portfolio-demo già esistente respinto con 409 USERNAME_ALREADY_EXISTS (anche tramite proxy Vercel), token verifica inesistente 400, accesso anonimo alle spese 401, login demo 200 e lettura spese autenticata 200. La prova del duplicato non crea utenti e non invia email.

Resta da verificare con un indirizzo reale: registrazione nuova, accettazione/consegna Brevo, ricezione del link, conferma e login del nuovo account. Nessuna email reale inviata durante questi controlli. Le precedenti note su test non eseguibili sono superate per i test mirati citati; non è stata rieseguita l'intera suite backend con MySQL.

## CRUD entrate e liste — 2026-10-25, branch in verifica

Scope autorizzato: CRUD completo entrate, liste paginate dal database, ordinamento data/ID, mese/anno, ricerca titolo/descrizione/importo/data e firma fabiozagariadev con portfolio e GitHub. Nessuna modifica agli endpoint array usati dai grafici. Nuovi endpoint /api/{expenses,incomes}/search. Angular /summary e /incomes usano un componente condiviso con client HTTP dedicato, gestione errori, modifica completa e conferma cancellazione.

Verifiche: build frontend e 62 test Angular passati; 6 test backend mirati passati con Java 21 e Mockito agent esterno. Nuova regressione MySQL aggiunta ad AuthExpenseFlowTests, ancora da eseguire su database isolato. Nessun dato o configurazione del deploy alterato. Prima del merge: suite con MySQL e controllo browser desktop/mobile, chiaro/scuro; poi deploy backend e frontend coordinati.
