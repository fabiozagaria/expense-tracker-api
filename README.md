# Gestionale Spese — Backend API

![Versione](https://img.shields.io/badge/versione-0.3.0--SNAPSHOT-blue)
![Stato](https://img.shields.io/badge/stato-in%20sviluppo-orange)

**Versione attuale: 0.3.0-SNAPSHOT — API spese, entrate e riepilogo in consolidamento.**

Backend Spring Boot del progetto full stack **Gestionale Spese**. La versione corrente fornisce API persistenti per le spese personali, la creazione e lettura delle entrate e un riepilogo dei totali per utente.

- [Repository frontend](https://github.com/fabiozagaria/expense-tracker-angular)
- [Demo full stack](https://gestionale-spese.vercel.app/)

## Stato del progetto

**Demo pubblica su Render con MySQL Aiven/TLS; CRUD spese e autenticazione coperti da test di integrazione. Restano da ampliare i test di entrate, dashboard e casi limite.**

Il dominio `Expense` espone le operazioni di lettura, creazione, sostituzione, aggiornamento parziale ed eliminazione per l'utente autenticato. Registrazione con verifica email, BCrypt, login JWT, refresh token ruotato in cookie HttpOnly e logout sono coperti da un test di integrazione con MySQL e invio email simulato. Le verifiche pubbliche documentate del 12 ottobre 2026 coprono health, login demo, lettura spese/entrate/dashboard, refresh e logout. Il frontend Vercel è collegato all'API; il relativo README documenta anche la verifica browser del CRUD spese. La registrazione pubblica resta disabilitata in attesa di un provider email HTTPS; la verifica completa della registrazione locale con Mailpit resta aperta.

## Scopo e confine della versione

Expense Tracker è il prodotto full stack di riferimento per API MVC, DTO, validazione, transazioni, autenticazione e isolamento dei dati. La demo distribuita è disponibile; il consolidamento riguarda regressioni, isolamento dei dati, registrazione locale e casi limite. Nessuna nuova funzionalità è necessaria per documentare correttamente lo scope corrente.

Le prove manuali documentate non equivalgono a una copertura automatica completa o a una certificazione per uso in produzione.

## Implementato

- progetto Spring Boot 4.1 con Java 21;
- entità JPA `Expense` con importo `BigDecimal`, descrizione, categoria e data;
- categorie di spesa tramite `ExpenseCategory` salvate come stringhe;
- persistenza con Spring Data JPA, `ExpenseRepository` ed `EntityManager`;
- DTO separati per creazione, PUT, PATCH e risposta;
- CRUD REST completo sotto `/api/expenses`;
- validazione dei payload con Jakarta Validation;
- metodi transazionali per creazione, modifica ed eliminazione;
- gestione dell'assenza di una spesa tramite eccezione dedicata;
- connessione MySQL configurabile tramite variabile d'ambiente;
- serializzazione delle date ISO con Jackson;
- test di avvio del contesto Spring;
- registrazione, verifica email, login, refresh e logout sotto `/auth`;
- JWT Bearer per `/api/expenses`, isolamento delle spese per proprietario e CORS per il frontend locale;
- test di integrazione del flusso autenticazione e spese personali;
- `GET` e `POST /api/incomes` per entrate associate all'utente autenticato;
- `GET /api/dashboard/summary` per totale entrate, totale spese e saldo.

## Modello attuale

| Campo | Tipo | Note |
| --- | --- | --- |
| `id` | `Long` | Chiave primaria generata dal database |
| `title` | `String` | Obbligatorio e validato |
| `amount` | `BigDecimal` | Importo positivo |
| `description` | `String` | Descrizione facoltativa con lunghezza limitata |
| `category` | `ExpenseCategory` | Enum persistito come stringa |
| `date` | `LocalDate` | Data del movimento |

## Tecnologie

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Jakarta Validation
- Jackson
- MySQL
- Maven Wrapper

## Architettura attuale

```mermaid
flowchart LR
    Client --> Controller
    Controller --> Service
    Service --> Repository
    Repository --> MySQL
```

## Stato degli endpoint REST

| Metodo | Endpoint | Stato |
| --- | --- | --- |
| `GET` | `/api/expenses` | Implementato |
| `GET` | `/api/expenses/{id}` | Implementato |
| `POST` | `/api/expenses` | Implementato |
| `PUT` | `/api/expenses/{id}` | Implementato |
| `PATCH` | `/api/expenses/{id}` | Implementato |
| `DELETE` | `/api/expenses/{id}` | Implementato |

### Entrate e riepilogo

| Metodo | Endpoint | Comportamento presente |
| --- | --- | --- |
| `GET` | `/api/incomes` | Elenco entrate del proprietario |
| `POST` | `/api/incomes` | Creazione entrata |
| `GET` | `/api/dashboard/summary` | Totali e saldo del proprietario |

Il dominio entrate non espone ancora un CRUD completo. Questi endpoint richiedono autenticazione Bearer e non sono coperti dal test `AuthExpenseFlowTests` dedicato alle spese.

Gli endpoint `/api/expenses` richiedono `Authorization: Bearer <access token>` e restituiscono solo le spese dell'utente autenticato. Gli endpoint pubblici `POST /auth/register`, `/auth/verify-email`, `/auth/login`, `/auth/refresh` e `/auth/logout` gestiscono la sessione; il refresh token viaggia solo nel cookie HttpOnly.

## Configurazione locale

### Requisiti

- JDK 21 per l'avvio senza Docker
- MySQL per l'avvio senza Docker

Il progetto usa il database `expense-tracker`. La password viene letta dalla variabile d'ambiente `DB_PASSWORD`.

Esempio di preparazione del database:

```sql
CREATE DATABASE `expense-tracker`;
```

Linux e macOS:

```bash
export DB_PASSWORD="la-tua-password"
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:DB_PASSWORD="la-tua-password"
./mvnw.cmd spring-boot:run
```

Per l'autenticazione serve anche `SECRET_KEY` (chiave casuale di almeno 32 byte, da impostare nell'ambiente e mai versionare). Il servizio userà `http://localhost:8080` e accetta il frontend locale su `http://localhost:4200`.

### Avvio con Docker Compose

1. Copiare `.env.example` in `.env` e compilare `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` e `SECRET_KEY` con valori propri. `.env` è ignorato da Git e dal contesto Docker.
2. Avviare lo stack con `docker compose up --build -d`.
3. Aprire Mailpit su `http://localhost:8025` per leggere il link di verifica email; avviare Angular su `http://localhost:4200`.

Compose avvia backend, MySQL 8 e Mailpit. Il database usa un volume persistente e non espone la porta 3306 sull'host. Backend e Mailpit sono accessibili solo da `localhost`. Per fermare i container: `docker compose down` (il volume MySQL resta disponibile).

## Verifiche

```bash
./mvnw test
```

Sono presenti il test di avvio del contesto e un test di integrazione con MySQL per registrazione, verifica email, login, refresh, Bearer e isolamento delle spese. L'invio email è simulato nel test.

## Limiti attuali

- API demo pubblicata su Render Free; registrazione pubblica disabilitata in attesa del provider email HTTPS;
- CORS configurabile tramite `FRONTEND_URL` e origine delle POST auth verificata nel profilo demo;
- entrate e dashboard presenti e letture verificate sull'API pubblica; mancano test automatizzati dedicati e prove complete dei casi limite;
- il percorso completo nel browser con Mailpit resta da verificare;
- restano da ampliare i test dei casi limite; la configurazione demo non equivale a una configurazione per dati personali o uso produttivo.

## Consolidamento successivo

1. completare la verifica della registrazione locale con Angular e Mailpit;
2. ampliare regressioni, isolamento fra utenti e casi limite di spese, entrate e dashboard;
3. integrare e verificare un provider email HTTPS prima di abilitare la registrazione pubblica;
4. scegliere eventuali nuove funzionalità soltanto con una decisione di scope separata.

La demo usa il proxy Vercel, un refresh cookie HttpOnly/Secure con `SameSite=Strict` e verifica dell'origine delle POST auth. Vedere [deploy e verifiche](docs/DEPLOYMENT.md) per ambienti, cookie e limiti. Il logout revoca il refresh; un access token già emesso resta valido fino alla scadenza.

## Versioning

Il backend segue la stessa versione funzionale del frontend:

- `0.3.0-SNAPSHOT` identifica lo sviluppo della release Expense Tracker `0.3.0`;
- `PATCH` per correzioni compatibili;
- `MINOR` per nuove funzionalità durante la fase `0.x`;
- rimozione del suffisso `SNAPSHOT` quando viene prodotto un artefatto di release.

## Autore

Fabio Zagaria — Junior Backend Developer con competenze full stack.


## Demo pubblica

Frontend su [Vercel](https://gestionale-spese.vercel.app/), backend su [Render](https://expense-tracker-api-demo-plb9.onrender.com) e database MySQL Aiven con TLS. Il pulsante **Prova la demo** usa un account condiviso: inserire soltanto dati fittizi. Render Free può richiedere 2–3 minuti al primo avvio dopo la sospensione. Le verifiche pubbliche sono registrate al 12 ottobre 2026 in [deploy e verifiche](docs/DEPLOYMENT.md); non costituiscono un controllo di disponibilità in tempo reale.

## Controllo registrazione

POST /auth/register salva User con password BCrypt, ruolo USER ed email non verificata. Prima del salvataggio controlla username/email già presenti e restituisce 409 con errorName specifico. In locale invia il link tramite SMTP/Mailpit; nella demo pubblica la registrazione resta disabilitata fino alla configurazione di un provider HTTPS.
