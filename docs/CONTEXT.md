# Contesto tecnico — Gestionale Spese Backend

## Stato corrente — 19 novembre 2026

**Versione corrente 1.3.0** (frontend compatibile 1.2.0). La 1.3.0 del 19 novembre 2026 porta il database a PostgreSQL con il profilo `neon`; il contratto REST non cambia. Questa sezione e il README descrivono lo stato attuale; le note successive conservano la cronologia e possono descrivere stati superati.

- **Progetto Neon collegato il 19 novembre 2026:** `neon.ts`, `package.json` (solo `@neon/config` e `@neon/env` per la CLI Neon, fuori dalla build Java) e `.env.local` escluso da Git. Progetto `calm-fire-76971235`, branch `production`, PostgreSQL 18.7. JAR verificato con `demo,neon` su PostgreSQL 18.7 locale; connessione al database Neon reale ancora da provare su Render (dettagli in `docs/DEPLOYMENT.md`).
- **Database migrato a PostgreSQL il 19 novembre 2026, per usare Neon (piano Free).** Driver, Flyway (`flyway-database-postgresql`), Docker Compose (PostgreSQL 17) e Testcontainers (PostgreSQL 17) passano da MySQL a PostgreSQL. Le migrazioni V1 e V2 MySQL sono unite in un unico `V1__initial_schema.sql` PostgreSQL, senza baseline: il database Aiven non esiste più, quindi non ci sono dati MySQL da convertire. Username ed email restano unici senza distinguere maiuscole e minuscole (su MySQL lo garantiva la collation `*_ci`): indici unici su `lower()` e ricerche con `lower()` in `UserRepository`; dopo il login il token porta lo username esatto salvato. Il profilo `aiven` è sostituito dal profilo `neon` (`SPRING_PROFILES_ACTIVE=demo,neon`): TLS `verify-full` con il truststore di Java, `channelBinding=require`, pool di massimo 3 connessioni e nessuna connessione a riposo, così Neon può spegnersi e non consuma ore di calcolo. Verifiche: suite completa 124 test, zero fallimenti/errori/skipped (Java 21, PostgreSQL 17 Testcontainers); JAR avviato con `demo,neon` su PostgreSQL 17 locale (migrazione applicata, login demo, spesa creata e ritrovata dopo il riavvio, registrazione chiusa con `REGISTRATION_ENABLED=false`) e rifiuto dell'avvio contro un server senza TLS. Non ancora verificato: connessione a un database Neon reale e deploy su Render con il profilo `neon`.
- **Demo online ripristinata il 15 novembre 2026, con un database H2 in memoria.** Il 12 novembre il database MySQL Aiven della demo si era spento (piano gratuito ritirato; log Render: `UnknownHostException` sul DNS del database, poi crash loop). Il profilo `demo` ora usa H2 (nessun database esterno, nessun costo; PR #28): `DemoDataInitializer` ricrea l'account condiviso e i dati di esempio a ogni avvio, i dati aggiunti dai visitatori si perdono al riavvio e la registrazione resta chiusa anche con `REGISTRATION_ENABLED=true`. Il database esterno è opzionale: oggi PostgreSQL su Neon col profilo `neon` (prima MySQL col profilo `aiven`, rimosso). Verificati sul backend live (deploy del commit `1d1caa4`): `/health` (anche attraverso Vercel), login demo, `/api/me`, dashboard, ricerca di spese ed entrate e registrazione chiusa (503). Il frontend ha spento l'avviso di sospensione.
- Frontend Vercel e backend Render; finché su Render non si attiva il profilo `neon`, la demo online usa H2 in memoria.
- Novità 1.2.0, retrocompatibile: `GET /api/me` restituisce `{ username, email }` dell'utente del token (nessun altro campo, mai la password). Serve alla pagina Impostazioni del frontend per inviare il link di cambio password con l'email dell'account. `SecurityConfig` invariato: la rotta è protetta da `anyRequest().authenticated()`.
- Dalla 1.1.0: reinvio della verifica email, reset password, password fino a 72 byte UTF-8, rate limiting per IP sulle POST pubbliche di `/auth` (429 `RATE_LIMITED` con `Retry-After`), reuse detection dei refresh token, errori in formato `APIError`, elenchi array limitati a 1000 elementi (`X-Result-Truncated`), Flyway, Swagger UI, Testcontainers e CI su GitHub Actions.
- Suite backend completa il 10 novembre 2026: 122 test passati, zero fallimenti, con Java 21 e MySQL 8.0 Testcontainers.
- Il percorso reale Impostazioni → email → nuova password → login è stato provato dal proprietario sul sito pubblico il 10 novembre 2026 con un'email vera e funziona: la 1.2.0 con `GET /api/me` è quindi online sul servizio pubblico (render.yaml indica master e lo storico dei deploy Render mostra Auto-Deploy dei commit di master).
- POM e nome JAR allineati a 1.3.0. Il cambio dei manifest non crea da solo un tag Git o una GitHub Release.
- Il proprietario ha dichiarato il progetto concluso (10 novembre 2026). Unica idea futura, rinviata e a suo carico: eliminazione dell'account (vedi `FUTURE.md`).

## Cronologia e dettagli tecnici

Aggiornato: 2026-10-22

## Obiettivo corrente

Verificare il verticale registrazione → verifica email → login → spese personali → refresh/logout insieme al frontend Angular.

## Stato osservato

- Spring Boot 4.1, Java 21 come target, JPA/PostgreSQL (MySQL fino al 19 novembre 2026), Spring Security Resource Server e BCrypt.
- `POST /auth/register` crea un utente non verificato e invia un link tramite Mailpit; `POST /auth/verify-email` lo abilita.
- La verifica dello stesso token risponde ancora con successo se l'account risulta già confermato; i token sconosciuti o scaduti per utenti non verificati restano rifiutati.
- `POST /auth/login` restituisce un access token JWT di 15 minuti e imposta un refresh token di 7 giorni in cookie HttpOnly.
- `POST /auth/refresh` ruota il refresh token; `POST /auth/logout` lo revoca.
- Le spese richiedono Bearer JWT; il server limita lettura, modifica ed eliminazione al proprietario. Restano sia GET `/api/expenses` sia `/api/expenses/search` paginato. Entrambe le risposte usano Redis con TTL due minuti; cache distinta per proprietario e parametri, invalidata dopo il commit delle scritture. Redis indisponibile: fallback sul database; invalidazioni fallite e letture concorrenti possono lasciare dati precedenti fino al TTL.
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

Controllo server di username/email duplicati prima di salvare User e inviare email: HTTP 409 con errorName USERNAME_ALREADY_EXISTS o EMAIL_ALREADY_EXISTS. I vincoli UNIQUE del database (indici su `lower()` per username ed email) restano attivi per richieste concorrenti; il controllo applicativo usa lo stesso confronto senza maiuscole. Email vuota respinta. Gli errori 400 escono sempre nel formato `APIError`: `VALIDATION_ERROR` (`details` elenca `campo: messaggio`), `MALFORMED_REQUEST` (JSON illeggibile, senza dettagli del parser), `INVALID_PARAMETER` (tipo errato, es. id non numerico) e `ILLEGAL_ARGUMENT` per paginazione/ricerca non valide. Un `IllegalArgumentException` imprevisto non viene più convertito in 400: è un errore del server e il suo messaggio non arriva al client. Il dispatch ERROR è permesso nel security chain: un errore inviato con `sendError` (es. il 403 di `AuthOriginFilter`) mantiene il suo stato invece di diventare 401 a corpo vuoto; una richiesta diretta a `/error` resta protetta. Rate limiting per IP sulle POST pubbliche di `/auth` (Bucket4j in memoria con Caffeine per l'espulsione, `RateLimitFilter` prima dell'autenticazione, 429 con `Retry-After`; soglie in `RateLimitPolicy`, `RATE_LIMIT_ENABLED`); nei test è spento di default tramite `PostgresTestContainerInitializer`. Documentazione OpenAPI con springdoc 3.2 (`/swagger-ui.html`, `/v3/api-docs`, pubbliche, `SWAGGER_ENABLED`; con la UI attiva `app.api-public-url`/`API_PUBLIC_URL` è ammessa come Origin per le POST /auth e per CORS, nel profilo demo vale l'URL Render, così il "Try it out" di `/auth/demo` funziona); i tag stanno sui controller e gli endpoint pubblici azzerano il requisito bearer con `@SecurityRequirements`. Refresh token: il riuso di un token già ruotato oltre 10 secondi dalla rotazione chiude tutte le sessioni dell'utente (entro 10 secondi è trattato come gara tra due richieste e respinge solo quella in ritardo); un job giornaliero (`app.token-cleanup.cron`, default 03:30) elimina refresh token, token di verifica e di reset scaduti da oltre un giorno. La password resta BCrypt e il ruolo viene assegnato come USER dal server. Nuovi utenti non verificati non possono fare login. Schema del database gestito da Flyway: dal 19 novembre 2026 un unico `V1__initial_schema.sql` PostgreSQL (prima V1 baseline MySQL di Hibernate e V2 `password_reset_tokens`, con baseline per i database esistenti); `ddl-auto=validate`. Se il link scade, `POST /auth/resend-verification` (body `{email}`) sostituisce il token e rinvia l'email: risposta sempre 202 (non rivela se l'email esiste), cooldown di 60 secondi, disabilitato come la registrazione nel profilo demo. Il profilo demo conserva la registrazione pubblica disabilitata: il provider email HTTPS sarà configurato successivamente.

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

## Verifica MySQL completata — 2026-10-25

Supera la precedente nota sulla suite database non eseguita. Suite Maven completa: 14 test, zero fallimenti/errori/skipped, Java 21.0.13 e MySQL Community 8.0.46 locale temporaneo. Database dedicato `expense_tracker_verification`, connessione su loopback, processo arrestato a fine prova; nessun dato online usato. Incluso `incomeCrudSearchAndOwnership`: GET/PUT/PATCH/DELETE altrui respinti con 404, CRUD proprietario, mese, titolo/descrizione, importo con virgola e date ISO/italiane. Test esistenti auth/spese/demo e 3 test Brevo inclusi. Frontend: build e 62 test passati; link entrate dalla dashboard corretto nel commit b12238f0f2ec953d8a2f31280a8601dc266493c6, stato Vercel success. Il proprietario ha confermato il caricamento delle liste. Resta il controllo browser completo desktop/mobile e temi.

## Integrazione compatibile della manutenzione email — 2026-10-26

Recuperati gli incrementi utili della precedente PR email nella sola architettura `services.mail`/`app.mail`, mantenendo `MAIL_FROM`, `EMAIL_DELIVERY_FAILED`, versione 1.0.0 e configurazione della registrazione online. Aggiunti HTML escapato, conferma Brevo `201` con `messageId`, timeout, rifiuto di redirect/configurazioni incoerenti e flush dei vincoli prima dell'invio. Dettagli e limite tra invio e commit in [EMAIL.md](EMAIL.md).

`clean verify` completato con JDK 21.0.13 e MySQL 8.4 temporaneo su loopback: 39 test, zero fallimenti/errori/skipped e JAR 1.0.0 prodotto. Inclusi i flussi auth/spese/entrate/demo esistenti, conferme HTTP simulate, escaping, configurazione e rollback reale di utente/token con successivo tentativo riuscito. Nessuna chiamata Brevo, email reale, modifica al servizio pubblico o dato online. Il database di prova è effimero e separato; queste prove non sostituiscono il controllo browser o la consegna reale già documentata.
