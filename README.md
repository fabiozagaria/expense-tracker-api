# Gestionale Spese — Backend API

![Versione](https://img.shields.io/badge/versione-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green)
![Stato](https://img.shields.io/badge/perimetro_funzionale-completato-success)

Backend del progetto full stack **Gestionale Spese**: autenticazione con verifica email, gestione delle spese e delle entrate personali, ricerca paginata e riepilogo finanziario.

**Progetto concluso al 25 ottobre 2026 — versione 1.0.0.**

Il perimetro funzionale è completato, salvo correzioni di bug, refactoring e funzionalità future da gestire con nuove versioni. Il progetto resta una demo portfolio, con limiti e verifiche residue dichiarati sotto.

- [Applicazione online](https://gestionale-spese.vercel.app/)
- [Frontend Angular](https://github.com/fabiozagaria/expense-tracker-angular)
- [Health backend](https://expense-tracker-api-demo-plb9.onrender.com/health)
- [Portfolio di Fabio Zagaria](https://fabio-zagaria-portfolio.vercel.app/)

## Funzionalità

- CRUD completo di spese ed entrate: creazione, lettura, sostituzione, modifica parziale ed eliminazione.
- Risorse associate all'utente autenticato; lettura, modifica ed eliminazione di risorse altrui restituiscono 404.
- Liste paginate nel database con filtro mese/anno, ricerca e ordine per data e ID.
- Dashboard con totale entrate, totale spese e saldo per proprietario.
- Registrazione, verifica email, BCrypt, JWT Bearer, rotazione del refresh token e logout.
- SMTP/Mailpit in locale e Brevo HTTPS online.
- DTO dedicati, Jakarta Validation, transazioni sui metodi di scrittura e gestione centralizzata degli errori.

## Architettura e struttura

Spring Boot 4.1, Java 21, Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA/Hibernate, MySQL, Lombok e Maven Wrapper.

Il controller gestisce HTTP e validazione del DTO; il service applica le regole e i confini transazionali; repository ed EntityManager gestiscono la persistenza. L'identità del proprietario deriva dal principal autenticato, non dal payload del client.

| Percorso | Responsabilità |
| --- | --- |
| `src/main/java/org/esercizi/expensetrackerapi/controllers/` | Endpoint auth, spese, entrate, dashboard, demo e health |
| `services/` nello stesso package | Logica applicativa, mapping DTO e ricerca tramite `MovementSearch` |
| `services/mail/` | `VerificationEmailSender`, implementazioni SMTP e Brevo |
| `repository/` | Repository JPA, query per proprietario e aggregazioni |
| `model/` | Entità utenti, spese, entrate e token; categorie e ruoli |
| `dto/` | Richieste, risposte e contratto stabile `MovementPage` |
| `security/` | Configurazione Security, JWT, refresh e controllo Origin |
| `exceptions/` | Eccezioni applicative, `GlobalExceptionHandler` e `APIError` |
| `src/main/resources/` | Configurazione base e profilo `demo` |
| `src/test/java/` | Test unitari, trasporto email e integrazione con MySQL |
| `docker-compose.yaml` | API, MySQL 8.0 e Mailpit locali |
| `Dockerfile`, `docker-entrypoint.sh` | Build Java 21 e truststore Aiven per il deploy |
| `render.yaml` | Blueprint Render, configurato per `master` |
| `docs/CONTEXT.md`, `docs/DEPLOYMENT.md` | Contesto tecnico e dettagli/storia delle verifiche |

Spese ed entrate hanno ID `Long`, titolo, importo positivo `BigDecimal`, descrizione facoltativa, categoria enum, data `LocalDate` e proprietario. Titolo: 3–15 caratteri; descrizione: massimo 30; data odierna o passata. Le categorie dei due domini sono distinte.

## Contratto REST

Per entrambi i prefissi `/api/expenses` e `/api/incomes`:

| Metodo | Percorso relativo | Risultato |
| --- | --- | --- |
| GET | prefisso base | Array dei movimenti del proprietario; usato anche dalla dashboard |
| GET | `/search` | Pagina filtrata del proprietario |
| GET | `/{id}` | Dettaglio, oppure 404 |
| POST | prefisso base | Creazione, 201 con risposta del movimento |
| PUT | `/{id}` | Sostituzione dei campi modificabili, 200 |
| PATCH | `/{id}` | Modifica dei campi presenti e non null, 200 |
| DELETE | `/{id}` | Eliminazione, 200 con risposta del movimento eliminato |

`GET /api/dashboard/summary` restituisce `totalIncome`, `totalExpense` e `balance`. Gli endpoint `/api/**` richiedono `Authorization: Bearer <access token>`.

### Ricerca e paginazione

Esempio: `GET /api/incomes/search?page=0&size=10&month=2026-10&order=newest&query=stipendio`.

| Parametro | Comportamento |
| --- | --- |
| `page` | Indice da 0; default 0 |
| `size` | Da 1 a 100; default 10 |
| `month` | Facoltativo, formato `yyyy-MM` |
| `order` | `newest` (default) oppure `oldest`; data e ID nella stessa direzione |
| `query` | Massimo 100 caratteri; testo parziale su titolo/descrizione, senza distinzione maiuscole; oppure importo esatto con punto/virgola o data esatta ISO/`gg/mm/aaaa` |

Mese, ricerca e proprietario sono combinati. Il testo è cercato letteralmente, con escape dei caratteri wildcard SQL. La risposta contiene `content`, `number`, `size`, `totalElements` e `totalPages`. Gli endpoint array preesistenti restano compatibili; i totali della dashboard non dipendono dalla pagina visualizzata.

### Autenticazione

| Metodo | Endpoint | Utilizzo |
| --- | --- | --- |
| POST | `/auth/register` | Crea utente non verificato e invia email |
| POST | `/auth/verify-email?token=...` | Conferma email |
| POST | `/auth/login` | Access token e cookie refresh |
| POST | `/auth/refresh` | Rotazione del refresh e nuovo access token |
| POST | `/auth/logout` | Revoca del refresh e rimozione cookie |
| POST | `/auth/demo` | Accesso condiviso, disponibile nel profilo demo |

JWT di accesso: 15 minuti. Refresh: 7 giorni, cookie HttpOnly con Path `/auth`. Online è Secure e SameSite=Strict. Il logout revoca il refresh; un access token già emesso resta valido fino alla scadenza.

Duplicati username/email: 409. Fallimento dell'invio email: 503 `EMAIL_DELIVERY_FAILED` e rollback della registrazione. La verifica email è necessaria prima del login.

## Deploy attuale

| Componente | Configurazione |
| --- | --- |
| Frontend | Vercel: `https://gestionale-spese.vercel.app` |
| Backend | Render: `expense-tracker-api-demo`, profilo `demo` |
| Database | MySQL Aiven, TLS con verifica CA e hostname |
| Email online | Brevo via HTTPS; registrazione abilitata |
| Proxy | Vercel inoltra `/health`, `/auth/*` e `/api/*` a Render |

Il servizio Render esistente segue `demo/render-aiven-setup`, allineato al merge funzionale di `master` il 25 ottobre 2026. Il Blueprint versionato indica invece `master`: un commit sul solo `master` non prova che il servizio esistente sia aggiornato.

Variabili principali del deploy: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SECRET_KEY`, `DEMO_PASSWORD`, `SPRING_PROFILES_ACTIVE=demo`, `FRONTEND_URL=https://gestionale-spese.vercel.app`, `MAIL_PROVIDER=brevo`, `BREVO_API_KEY`, `MAIL_FROM` e `REGISTRATION_ENABLED=true`. La CA Aiven viene fornita con `AIVEN_CA_CERT` o secret file: vedere [deploy](docs/DEPLOYMENT.md). Segreti esclusivamente nell'ambiente.

Il profilo demo mantiene la registrazione disabilitata come default; il deploy la abilita esplicitamente. Il pulsante **Prova la demo** usa un account condiviso: usare dati fittizi. Gli account personali restano separati tramite i controlli del backend. Render può richiedere un'attesa al risveglio; `/health` conferma HTTP, non tutte le dipendenze.

## Avvio da un clone pulito

Eseguire i comandi dalla radice del repository. Scegliere uno dei due percorsi: il backend usa la porta 8080 e non va avviato contemporaneamente con Compose e Maven.

### A. Ambiente completo con Docker Compose

Prerequisiti: Docker Engine/Docker Desktop avviato, Docker Compose v2 e accesso HTTPS ai registry delle immagini e a Maven Central. Non servono JDK o MySQL installati sull'host.

1. Creare `.env` a partire dal modello, soltanto se il file non esiste già:

   ```bash
   cp .env.example .env
   ```

   In PowerShell usare `Copy-Item .env.example .env`.

2. Compilare tutte le variabili prima dell'avvio:

   | Variabile | Utilizzo |
   | --- | --- |
   | `DB_PASSWORD` | Password dell'utente MySQL applicativo `expense_tracker` |
   | `MYSQL_ROOT_PASSWORD` | Password dell'amministratore MySQL, distinta da quella applicativa |
   | `SECRET_KEY` | Chiave JWT casuale di almeno 32 byte, da mantenere stabile tra i riavvii |

   Usare valori propri, senza commettere `.env`, già ignorato da Git e dal contesto Docker. Se disponibile, `openssl rand -hex 32` genera una chiave casuale adatta a `SECRET_KEY`. Le variabili già esportate nella shell hanno precedenza sui valori di `.env`.

3. Validare, costruire e avviare:

   ```bash
   docker compose config --quiet
   docker compose build
   docker compose up -d
   ```

Compose include già backend, MySQL 8 e Mailpit. Configura il backend verso `mysql:3306` e `mailpit:1025`; MySQL non espone la porta 3306 sull'host. Il backend attende l'healthcheck MySQL, mentre per Mailpit attende l'avvio del container.

- Backend: `http://localhost:8080`; controllo HTTP: `http://localhost:8080/health`.
- Mailpit: `http://localhost:8025`, per leggere le email di verifica.
- Frontend Angular, avviato separatamente: `http://localhost:4200`, origine e destinazione dei link di verifica configurate nello stack.

Controllare stato e log:

```bash
docker compose ps
docker compose logs --tail=100 backend mysql mailpit
```

`/health` conferma che il processo HTTP risponde; verificare anche login e lettura dati per controllare il database. Un errore di download legato a proxy o certificati va risolto configurando l'ambiente, mantenendo attiva la verifica TLS.

Arrestare senza eliminare i volumi:

```bash
docker compose down
```

Il volume MySQL conserva i dati. Le password di inizializzazione vengono applicate al primo avvio di un database vuoto: cambiare `.env` non aggiorna le credenziali di un volume già inizializzato. In quel caso occorre riallineare le credenziali con una procedura amministrativa, conservando i dati.

### B. Backend nativo con Maven

Prerequisiti: **JDK 21 completo** (`java` e `javac`, con `JAVA_HOME` coerente), MySQL raggiungibile e accesso HTTPS a Maven Central. Il Maven Wrapper incluso scarica Maven; non serve installarlo separatamente. Per registrare utenti serve anche SMTP compatibile con la configurazione locale: porta 1025, senza autenticazione e senza TLS.

Predisporre su un'istanza MySQL di sviluppo due database separati e un utente dedicato. Esempio da eseguire come amministratore, sostituendo la password segnaposto e adattando l'host dell'utente se MySQL è remoto:

```sql
CREATE DATABASE `expense-tracker`;
CREATE DATABASE `expense-tracker-test`;
CREATE USER 'expense_tracker'@'localhost' IDENTIFIED BY 'SOSTITUIRE_CON_PASSWORD_LOCALE';
GRANT ALL PRIVILEGES ON `expense-tracker`.* TO 'expense_tracker'@'localhost';
GRANT ALL PRIVILEGES ON `expense-tracker-test`.* TO 'expense_tracker'@'localhost';
```

Se database o utente esistono già, verificarne configurazione e permessi senza ricrearli. Hibernate usa `ddl-auto=update`: l'applicazione può creare o aggiornare tabelle nel database indicato.

Per Mailpit si può usare un'installazione locale oppure, se Docker è disponibile, avviare solo un container SMTP temporaneo in un altro terminale:

```bash
docker run --rm --name expense-tracker-mailpit-local -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit
```

Il comando non monta volumi; `Ctrl+C` lo arresta. Non avviarlo se Mailpit del percorso Compose occupa già le stesse porte. L'interfaccia è `http://localhost:8025`. Senza un servizio SMTP raggiungibile la registrazione non può completarsi; i link ricevuti puntano al frontend Angular locale.

**Il file `.env` viene letto da Docker Compose, non automaticamente da Spring Boot avviato con Maven.** Impostare le variabili nella stessa shell che esegue Maven. Sostituire i segnaposto prima di procedere:

Linux/macOS:

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/expense-tracker'
export DB_USERNAME='expense_tracker'
export DB_PASSWORD='SOSTITUIRE_CON_PASSWORD_LOCALE'
export SECRET_KEY='SOSTITUIRE_CON_CHIAVE_CASUALE_DI_ALMENO_32_BYTE'
export MAIL_HOST='localhost'
```

Windows PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/expense-tracker'
$env:DB_USERNAME='expense_tracker'
$env:DB_PASSWORD='SOSTITUIRE_CON_PASSWORD_LOCALE'
$env:SECRET_KEY='SOSTITUIRE_CON_CHIAVE_CASUALE_DI_ALMENO_32_BYTE'
$env:MAIL_HOST='localhost'
```

`DB_PASSWORD` e `SECRET_KEY` sono obbligatorie. I default sono `DB_URL=jdbc:mysql://127.0.0.1:3306/expense-tracker`, `DB_USERNAME=root` e `MAIL_HOST=localhost`: gli esempi scelgono esplicitamente un utente dedicato. `MYSQL_ROOT_PASSWORD` serve al servizio MySQL di Compose, non al backend nativo. La porta SMTP predefinita è 1025; un servizio con autenticazione/TLS richiede configurazione aggiuntiva. Il MySQL dello stack Compose non è raggiungibile dall'host tramite porta 3306 con il Compose attuale.

### Compilazione, test e avvio nativo

I test **usano MySQL reale e non creano automaticamente un database isolato**. Possono aggiornare lo schema e inizializzare dati demo: indirizzarli esclusivamente a un database dedicato ai test, senza dati personali o condivisi. Non usare il database della demo pubblica. L'invio email nei test di autenticazione è simulato.

Con le variabili precedenti già impostate, compilare, eseguire i test sul database dedicato e produrre il JAR:

```bash
(DB_URL='jdbc:mysql://127.0.0.1:3306/expense-tracker-test' ./mvnw clean verify) &&
  ./mvnw spring-boot:run
```

La subshell limita il cambio di `DB_URL` ai test; l'avvio successivo usa il database di sviluppo. In PowerShell:

```powershell
$developmentDbUrl = $env:DB_URL
try {
    $env:DB_URL='jdbc:mysql://127.0.0.1:3306/expense-tracker-test'
    ./mvnw.cmd clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Build Maven non riuscita.' }
} finally {
    $env:DB_URL=$developmentDbUrl
}
# Avviare solo dopo aver verificato che la build sia riuscita.
./mvnw.cmd spring-boot:run
```

Il backend risponde su `http://localhost:8080`; arrestarlo con `Ctrl+C`. `FRONTEND_URL` ha default `http://localhost:4200` e controlla CORS e link di verifica. Per provare registrazione e verifica dal browser avviare anche il frontend Angular.


## Verifiche e limiti

Verifiche del **25 ottobre 2026**:

| Verifica | Esito |
| --- | --- |
| Suite backend completa, Java 21.0.13 e MySQL 8.0.46 temporaneo | 14 test passati, zero fallimenti/errori/skipped |
| Suite Angular | 62 test passati |
| Build Angular production | Riuscita; avvisi budget iniziale e CSS login/dashboard |
| Registrazione online con email reale | Confermata dal proprietario |
| Caricamento delle nuove liste online | Confermato dal proprietario |
| Correzione link entrate dalla dashboard | Deploy Vercel confermato |

La suite include autenticazione/sessione, CRUD e isolamento spese, CRUD entrate e accessi altrui respinti, ricerca mese/testo/importo/data, contesto applicativo, flusso demo e trasporto Brevo simulato. I test MySQL hanno usato un database temporaneo locale, senza dati online; non attestano copertura di ogni caso limite o un audit di sicurezza.

Restano aperti il controllo visivo sistematico desktop/mobile nei due temi, il percorso completo nel browser locale con Angular e Mailpit e la build/avvio integrali del Compose originale in un ambiente senza i problemi TLS del proxy della precedente prova. La versione funzionale è conclusa; nuove funzionalità sono rinviate a versioni future.


## Versioning e manutenzione

Prima versione funzionalmente conclusa: **1.0.0**, riferimento **25 ottobre 2026**. POM e nome JAR nel Dockerfile sono allineati; il cambio dei manifest non crea automaticamente un tag Git o una GitHub Release.

- **PATCH** (`1.0.1`): correzioni compatibili e manutenzione/refactoring senza cambiare il contratto pubblico.
- **MINOR** (`1.1.0`): nuove funzionalità compatibili.
- **MAJOR** (`2.0.0`): modifiche incompatibili del contratto pubblico.

## Autore

**Fabio Zagaria · fabiozagariadev** — Junior Backend Developer con competenze full stack.

[Portfolio](https://fabio-zagaria-portfolio.vercel.app/) · [GitHub](https://github.com/fabiozagaria)
