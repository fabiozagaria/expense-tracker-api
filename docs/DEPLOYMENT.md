# Demo portfolio: Render + Vercel (database H2 in memoria o PostgreSQL su Neon)

## Stato corrente — 19 novembre 2026

**Versione corrente 1.2.0** (frontend compatibile 1.2.0). Questa sezione e il README descrivono lo stato attuale; le note successive conservano la cronologia e possono descrivere stati superati.

- **Database migrato a PostgreSQL il 19 novembre 2026, per usare Neon (piano Free).** Driver, Flyway (`flyway-database-postgresql`), Docker Compose (PostgreSQL 17) e Testcontainers (PostgreSQL 17) passano da MySQL a PostgreSQL. Le migrazioni V1 e V2 MySQL sono unite in un unico `V1__initial_schema.sql` PostgreSQL, senza baseline: il database Aiven non esiste più, quindi non ci sono dati MySQL da convertire. Username ed email restano unici senza distinguere maiuscole e minuscole (su MySQL lo garantiva la collation `*_ci`): indici unici su `lower()` e ricerche con `lower()` in `UserRepository`; dopo il login il token porta lo username esatto salvato. Il profilo `aiven` è sostituito dal profilo `neon` (`SPRING_PROFILES_ACTIVE=demo,neon`): TLS `verify-full` con il truststore di Java, `channelBinding=require`, pool di massimo 3 connessioni e nessuna connessione a riposo, così Neon può spegnersi e non consuma ore di calcolo. Verifiche: suite completa 124 test, zero fallimenti/errori/skipped (Java 21, PostgreSQL 17 Testcontainers); JAR avviato con `demo,neon` su PostgreSQL 17 locale (migrazione applicata, login demo, spesa creata e ritrovata dopo il riavvio, registrazione chiusa con `REGISTRATION_ENABLED=false`) e rifiuto dell'avvio contro un server senza TLS. Non ancora verificato: connessione a un database Neon reale e deploy su Render con il profilo `neon`.
- **Demo online ripristinata il 15 novembre 2026, con un database H2 in memoria.** Il 12 novembre il database MySQL Aiven della demo si era spento (piano gratuito ritirato; log Render: `UnknownHostException` sul DNS del database, poi crash loop). Il profilo `demo` ora usa H2 (nessun database esterno, nessun costo; PR #28): `DemoDataInitializer` ricrea l'account condiviso e i dati di esempio a ogni avvio, i dati aggiunti dai visitatori si perdono al riavvio e la registrazione resta chiusa anche con `REGISTRATION_ENABLED=true`. Il database esterno è opzionale: oggi PostgreSQL su Neon col profilo `neon` (prima MySQL col profilo `aiven`, rimosso). Verificati sul backend live (deploy del commit `1d1caa4`): `/health` (anche attraverso Vercel), login demo, `/api/me`, dashboard, ricerca di spese ed entrate e registrazione chiusa (503). Il frontend ha spento l'avviso di sospensione.
- Frontend Vercel e backend Render; finché su Render non si attiva il profilo `neon`, la demo online usa H2 in memoria.
- Novità 1.2.0, retrocompatibile: `GET /api/me` restituisce `{ username, email }` dell'utente del token (nessun altro campo, mai la password). Serve alla pagina Impostazioni del frontend per inviare il link di cambio password con l'email dell'account. `SecurityConfig` invariato: la rotta è protetta da `anyRequest().authenticated()`.
- Dalla 1.1.0: reinvio della verifica email, reset password, password fino a 72 byte UTF-8, rate limiting per IP sulle POST pubbliche di `/auth` (429 `RATE_LIMITED` con `Retry-After`), reuse detection dei refresh token, errori in formato `APIError`, elenchi array limitati a 1000 elementi (`X-Result-Truncated`), Flyway, Swagger UI, Testcontainers e CI su GitHub Actions.
- Suite backend completa il 10 novembre 2026: 122 test passati, zero fallimenti, con Java 21 e MySQL 8.0 Testcontainers.
- Il percorso reale Impostazioni → email → nuova password → login è stato provato dal proprietario sul sito pubblico il 10 novembre 2026 con un'email vera e funziona: la 1.2.0 con `GET /api/me` è quindi online sul servizio pubblico (render.yaml indica master e lo storico dei deploy Render mostra Auto-Deploy dei commit di master).
- POM e nome JAR allineati a 1.2.0. Il cambio dei manifest non crea da solo un tag Git o una GitHub Release.
- Il proprietario ha dichiarato il progetto concluso (10 novembre 2026). Unica idea futura, rinviata e a suo carico: eliminazione dell'account (vedi `FUTURE.md`).

## Cronologia e dettagli tecnici

Demo pubblicata; le verifiche storiche sono riportate sotto con la relativa data e non attestano la disponibilità attuale. Per lo sviluppo locale seguire i due percorsi nel [README](../README.md#avvio-da-un-clone-pulito).

## Backend

1. Dal 15 novembre 2026 il profilo `demo` usa un database H2 in memoria: nessun database esterno e nessun costo. Lo schema lo crea Hibernate all'avvio (Flyway è spento, le migrazioni sono per PostgreSQL) e `DemoDataInitializer` ricrea l'account USER verificato con i dati fittizi. I dati aggiunti dai visitatori si perdono al riavvio del servizio (Render Free si spegne dopo circa 15 minuti di inattività).
2. Il Blueprint `render.yaml` usa un web service Docker Free e un Key Value Free per la cache. Render genera `SECRET_KEY` e `DEMO_PASSWORD`. Non copiare i segreti nel repository. Con il solo profilo `demo` `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` non servono; `AIVEN_CA_CERT` e il secret file `aiven-ca.pem` non servono più in nessun caso e si possono togliere dal servizio.
3. La registrazione è chiusa nel profilo `demo` anche se `REGISTRATION_ENABLED=true` è ancora impostata: con un database in memoria gli account sparirebbero al riavvio.
4. **Passaggio a Neon (PostgreSQL, piano Free).** Passi da fare a mano, nell'ordine:
   1. Su [console.neon.tech](https://console.neon.tech) creare un progetto (regione europea, vicina a Render Frankfurt, es. AWS Frankfurt `eu-central-1`) e usare il database predefinito `neondb`, dedicato alla demo e senza dati personali.
   2. In **Connect** scegliere la connessione **diretta** (pooled connection spenta: l'host non deve contenere `-pooler`, perché Neon sconsiglia PgBouncer per le migrazioni Flyway). Dalla stringa ricavare host, database, utente e password.
   3. Su Render impostare `DB_URL=jdbc:postgresql://HOST/neondb` (senza credenziali e senza parametri: TLS `verify-full` e `channelBinding=require` sono fissati da `application-neon.properties`), `DB_USERNAME`, `DB_PASSWORD` e infine `SPRING_PROFILES_ACTIVE=demo,neon`. Segreti solo su Render.
   4. Al primo avvio Flyway crea lo schema e `DemoDataInitializer` crea l'account demo con i dati di esempio; ai riavvii aggiorna la password demo ma conserva i dati. Con `neon` la registrazione segue `REGISTRATION_ENABLED`: con `true` serve anche `MAIL_PROVIDER=brevo` configurato (con `disabled` l'avvio fallisce apposta).
   5. Verificare `/health`, login demo, una scrittura e la sua lettura dopo un riavvio del servizio.

   Limiti del piano Free di Neon (documentazione ufficiale, novembre 2026): 100 ore di calcolo (CU-ore) al mese per progetto, 1 GB di spazio per progetto, nessun addebito oltre i limiti (superato lo spazio le scritture falliscono). Il calcolo si spegne dopo qualche minuto senza connessioni: per questo il profilo `neon` non tiene connessioni aperte a riposo (`minimum-idle=0`, `idle-timeout` 60 secondi), e Render Free a sua volta si spegne dopo circa 15 minuti senza traffico. Il risveglio di Neon aggiunge poche centinaia di millisecondi alla prima query. Gratuito, ma non illimitato: se Neon ritira il piano come ha fatto Aiven, basta tornare a `SPRING_PROFILES_ACTIVE=demo` (H2). Non usare il profilo `demo` su un database personale o di produzione.
5. `GET /health` indica che il processo HTTP è avviato; non certifica la disponibilità del database. Controllare anche un login demo e una lettura dati.

## Frontend

La build production usa URL relativi. `vercel.json` inoltra `/auth/*` e `/api/*` al backend Render; entrambe le destinazioni devono usare l'URL realmente assegnato al servizio. Il servizio Render creato usa `https://expense-tracker-api-demo-plb9.onrender.com`; le rewrites sono aggiornate a questo URL.

Il cookie resta HttpOnly, Secure e SameSite=Strict, senza Domain, con Path=/auth. Non aggiungere un prefisso diverso al proxy senza aggiornare anche il Path del cookie. Le risposte API non devono essere memorizzate nella CDN. Verificare che Vercel inoltri Origin, Cookie e Set-Cookie.

`FRONTEND_URL` deve essere esattamente `https://gestionale-spese.vercel.app`, senza slash finale. Nel profilo demo tutte le POST /auth richiedono questa Origin: protegge anche refresh, logout e login da richieste cross-site. Le preview Vercel non sono automaticamente autorizzate.

La demo usa un account condiviso: visitatori diversi possono cambiare gli stessi dati. Il pulsante non espone la password; avvia il normale flusso JWT/refresh. Non inserire dati personali. Non è previsto un reset automatico né una difesa completa contro abuso/quote: se necessario sospendere la demo o aggiungere limiti mirati.

## Email

In locale resta SMTP/Mailpit (`MAIL_PROVIDER=smtp`, default). Per inviare online tramite Brevo HTTPS configurare nelle variabili d'ambiente Render:

| Variabile | Valore |
| --- | --- |
| `MAIL_PROVIDER` | `brevo` |
| `BREVO_API_KEY` | Chiave API Brevo, non chiave SMTP; segreto solo su Render |
| `MAIL_FROM` | Indirizzo del mittente verificato in Brevo |
| `FRONTEND_URL` | `https://gestionale-spese.vercel.app` |
| `REGISTRATION_ENABLED` | `true` per abilitare l'endpoint dopo la configurazione |

Il frontend deve avere anche il blocco del form online rimosso: il solo flag backend non abilita il pulsante Angular. Il profilo demo resta disabilitato per default. Non cambiare `SPRING_PROFILES_ACTIVE` per abilitare le email.

Brevo usa una POST HTTPS con timeout di connessione di 5 secondi e lettura di 10 secondi. Un errore HTTP/rete produce 503 `EMAIL_DELIVERY_FAILED`, senza esporre la risposta del provider. L'eccezione runtime provoca il rollback della transazione di registrazione: l'utente può riprovare. Non ci sono retry automatici dell'invio. L'accettazione da Brevo non garantisce la consegna: controllare i log transazionali e la posta indesiderata.

Dopo il deploy provare con un indirizzo reale: registrazione, ricezione email, link su `/verify-email`, login e isolamento dati. Provare anche login prima della verifica, token scaduto e username/email duplicati. Non dichiarare la registrazione pubblica verificata prima di questa prova.

Test del trasporto senza database o email reali: `./mvnw -Dtest=BrevoVerificationEmailSenderTests test` con JDK 21. Il test controlla payload/autenticazione, errori del provider e configurazione mancante. Le regressioni esistenti usano un PostgreSQL usa-e-getta di Testcontainers (serve Docker).

## Checklist per nuovi deploy (non equivale a verifiche completate)

- Test backend con PostgreSQL (Testcontainers) e test Angular, più build production.
- Render avviato senza errori, TLS verso Neon verificato (`verify-full`) e nome pubblico corretto nelle rewrites.
- Nel browser: Prova la demo, elenco/modifica/eliminazione spese, inserimento entrate, riepilogo.
- Reload pagina, refresh dopo scadenza JWT, logout, riutilizzo del refresh revocato.
- POST auth da origine non autorizzata o senza Origin rifiutata; /users e altri repository non esposti tramite Spring Data REST.
- Primo accesso dopo sospensione: messaggio di attesa, nessun retry automatico dei POST; se il proxy scade prima del risveglio riprovare manualmente.
- Controllare le condizioni gratuite e le eventuali sospensioni del database; nessun pagamento o keep-alive artificiale è previsto.

## Verifiche locali eseguite

2026-10-12: 5 test backend passati con MySQL 8.4 temporaneo; 25 test Angular passati e build production riuscita (warning sul budget iniziale già superiore a 500 kB). In quella prova locale TLS Aiven e proxy Vercel non erano verificati; vedere la successiva verifica pubblica del 12 ottobre.

## Servizio Render creato

Servizio `expense-tracker-api-demo`, piano Free, regione Frankfurt, inizialmente dal branch `demo/render-aiven-setup` (il Blueprint attuale indica `master`). Certificato e variabili configurati; `DB_PASSWORD` inserita dal proprietario. Deploy Render e API verificati il 12 ottobre 2026. Il frontend è pubblicato su `https://gestionale-spese.vercel.app`; questa nota non costituisce una nuova verifica browser.

## Verifica backend pubblico — 2026-10-12

Deploy Render `dep-db6g54ad0e5s73akjl20` live. `/health`, login demo, lettura spese/entrate/dashboard e refresh restituiscono 200; logout restituisce 204 e il refresh dopo logout 401. Cookie HttpOnly, Secure, SameSite=Strict e Path=/auth verificati. Connessione MySQL riuscita con la configurazione VERIFY_IDENTITY e il certificato Aiven. Avvio dell'app circa 143 secondi su Render Free; la UI deve considerare 2–3 minuti.

## Verifica infrastruttura locale — 2026-10-22

L'analisi del commit `9c5a681` ha validato Compose e verificato compilazione, packaging e 7 test Maven con JDK 21 e MySQL temporaneo in RAM. In un container di prova sono riusciti registrazione con ricezione email in Mailpit, verifica email, login, CRUD spese, lettura entrate/dashboard, refresh e logout. Nessun dato persistente è stato usato o alterato.

La build Docker originale è stata tentata ma bloccata al download Maven da un certificato non riconosciuto del proxy remoto. La prova del JAR con configurazione proxy/trust esterna al progetto non certifica la build integrale del Dockerfile originale. Nessuna verifica TLS è stata disabilitata.

Restano da verificare build e avvio integrali del Compose originale in un ambiente HTTPS funzionante e registrazione completa nel browser locale con Angular e Mailpit. Le prove locali non aggiornano le verifiche della demo pubblica del 12 ottobre.


## Verifica deploy registrazione — 2026-10-25

Backend Render live: deploy `dep-dbf1v8ek1f9s73fptho0`, commit `76fec7db0f4f86fb9764ca087836e12df7c094c7`. Il servizio segue ancora `demo/render-aiven-setup`, avanzato senza force dal vecchio commit alla versione integrata in master. Profilo demo conservato; MAIL_PROVIDER=brevo e REGISTRATION_ENABLED=true configurati su Render preservando i segreti esistenti.

Verifiche completate: 3 test Brevo con Java 21 passati; 56 test Angular passati e build production riuscita con gli avvisi sui budget già presenti. Browser pubblico /register: pulsante Registrati abilitato. API pubblica: health 200, username portfolio-demo già esistente respinto con 409 USERNAME_ALREADY_EXISTS (anche tramite proxy Vercel), token verifica inesistente 400, accesso anonimo alle spese 401, login demo 200 e lettura spese autenticata 200. La prova del duplicato non crea utenti e non invia email.

Resta da verificare con un indirizzo reale: registrazione nuova, accettazione/consegna Brevo, ricezione del link, conferma e login del nuovo account. Nessuna email reale inviata durante questi controlli. Le precedenti note su test non eseguibili sono superate per i test mirati citati; non è stata rieseguita l'intera suite backend con MySQL.
