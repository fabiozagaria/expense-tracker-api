# Demo portfolio: Render + Aiven + Vercel

Preparazione del codice; pubblicazione e verifica nel browser ancora da completare.

## Backend

1. Creare un database Aiven MySQL sul piano Free, verificando disponibilità e limiti nell'account. Usare un database dedicato alla demo, senza dati personali.
2. Configurare il certificato CA pubblico fornito da Aiven nella variabile Render `AIVEN_CA_CERT`, oppure come secret file `aiven-ca.pem` (montato in `/etc/secrets/aiven-ca.pem`). L'entrypoint crea un truststore temporaneo; Connector/J verifica CA e hostname con `VERIFY_IDENTITY`.
3. Il Blueprint `render.yaml` usa soltanto un web service Docker Free. Impostare `DB_URL` nel formato `jdbc:mysql://HOST:PORT/defaultdb` (senza parametri che disabilitano TLS), `DB_USERNAME` e `DB_PASSWORD`. Render genera `SECRET_KEY` e `DEMO_PASSWORD`. Non copiare i segreti nel repository.
4. Il profilo `demo` disabilita la registrazione e crea un account USER verificato e dati fittizi solo alla prima inizializzazione. Ai riavvii aggiorna la password demo ma conserva i dati. Non usare questo profilo su un database personale o di produzione.
5. `GET /health` indica che il processo HTTP è avviato; non certifica la disponibilità del database. Controllare anche un login demo e una lettura dati.

## Frontend

La build production usa URL relativi. `vercel.json` inoltra `/auth/*` e `/api/*` al backend Render; entrambe le destinazioni devono usare l'URL realmente assegnato al servizio. Il servizio Render creato usa `https://expense-tracker-api-demo-plb9.onrender.com`; le rewrites sono aggiornate a questo URL.

Il cookie resta HttpOnly, Secure e SameSite=Strict, senza Domain, con Path=/auth. Non aggiungere un prefisso diverso al proxy senza aggiornare anche il Path del cookie. Le risposte API non devono essere memorizzate nella CDN. Verificare che Vercel inoltri Origin, Cookie e Set-Cookie.

`FRONTEND_URL` deve essere esattamente `https://gestionale-spese.vercel.app`, senza slash finale. Nel profilo demo tutte le POST /auth richiedono questa Origin: protegge anche refresh, logout e login da richieste cross-site. Le preview Vercel non sono automaticamente autorizzate.

La demo usa un account condiviso: visitatori diversi possono cambiare gli stessi dati. Il pulsante non espone la password; avvia il normale flusso JWT/refresh. Non inserire dati personali. Non è previsto un reset automatico né una difesa completa contro abuso/quote: se necessario sospendere la demo o aggiungere limiti mirati.

## Email

In locale resta SMTP/Mailpit. La demo non invia email e blocca la registrazione. La futura integrazione HTTPS richiede un provider e un mittente verificato; non basta cambiare host o porta SMTP.

## Verifica prima della pubblicazione

- Test backend con MySQL e test Angular, più build production.
- Render avviato senza errori, TLS MySQL verificato e nome pubblico corretto nelle rewrites.
- Nel browser: Prova la demo, elenco/modifica/eliminazione spese, inserimento entrate, riepilogo.
- Reload pagina, refresh dopo scadenza JWT, logout, riutilizzo del refresh revocato.
- POST auth da origine non autorizzata o senza Origin rifiutata; /users e altri repository non esposti tramite Spring Data REST.
- Primo accesso dopo sospensione: messaggio di attesa, nessun retry automatico dei POST; se il proxy scade prima del risveglio riprovare manualmente.
- Controllare le condizioni gratuite e le eventuali sospensioni del database; nessun pagamento o keep-alive artificiale è previsto.

## Verifiche locali eseguite

2026-10-12: 5 test backend passati con MySQL 8.4 temporaneo; 25 test Angular passati e build production riuscita (warning sul budget iniziale già superiore a 500 kB). TLS Aiven e proxy Vercel richiedono la prova con le risorse pubbliche.

## Servizio Render creato

Servizio `expense-tracker-api-demo`, piano Free, regione Frankfurt, branch `demo/render-aiven-setup`. Certificato e variabili preparati; manca l'inserimento di `DB_PASSWORD` da parte del proprietario e la verifica del deploy. Il frontend di produzione non è ancora aggiornato.
