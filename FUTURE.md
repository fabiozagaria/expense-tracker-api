# FUTURE — Idee per riprendere Expense Tracker

Aggiornato: 22 ottobre 2026.

## Scopo e priorità personali

Questo documento è un promemoria per riprendere il progetto più avanti. Raccoglie possibili miglioramenti di backend e frontend: non è una specifica approvata, una promessa di implementazione o una lista di requisiti obbligatori.

Per il momento la prima versione di Expense Tracker è considerata conclusa. La priorità è completare almeno una prima versione di **Access Desk** e **Job Flow**. Le idee qui raccolte potranno essere ridotte, cambiate, rimandate o scartate alla ripresa.

Il documento è conservato in entrambi i repository:

- Backend: https://github.com/fabiozagaria/expense-tracker-api/blob/master/FUTURE.md
- Frontend: https://github.com/fabiozagaria/expense-tracker-angular/blob/master/FUTURE.md

Se le decisioni cambiano, aggiornare entrambe le copie. Codice, README e documentazione tecnica restano le fonti per ciò che è effettivamente implementato.

## Nota per futuri interventi AI

La presenza di questo file **non autorizza a implementare le idee elencate**. Prima di intervenire, rileggere le istruzioni del repository, esaminare il codice attuale e concordare con il proprietario un singolo obiettivo. Il proprietario vuole implementare personalmente le funzionalità, con spiegazioni e revisione graduali.

Non trasformare automaticamente le proposte in task obbligatori, non riscrivere l'architettura e non dichiarare disponibili funzionalità soltanto descritte qui.

## Punto di partenza da ricontrollare alla ripresa

Al momento della stesura, il backend offre:

- CRUD completo delle spese sotto `/api/expenses`;
- creazione ed elenco delle entrate sotto `/api/incomes`;
- riepilogo complessivo sotto `/api/dashboard/summary`;
- autenticazione JWT, refresh token in cookie HttpOnly e logout;
- registrazione con verifica email tramite Mailpit in locale;
- isolamento dei movimenti per utente autenticato;
- categorie distinte per spese ed entrate, DTO e validazione.

Gli elenchi backend non applicano ancora i filtri e la paginazione descritti sotto. Il riepilogo non limita le somme a un periodo.

Angular dispone di gestione delle spese, inserimento entrate, dashboard e sessione autenticata. Prima di ampliarlo, verificare servizi HTTP, stato con Signals, form, interceptor e componenti esistenti. Conservare lo stile e gli strumenti UI già adottati.

Le verifiche backend del 22 ottobre hanno superato 7 test e i principali flussi HTTP, inclusa la ricezione email in Mailpit. Non equivalgono a una verifica completa del browser Angular. La build Docker originale nell'ambiente remoto è rimasta bloccata da proxy/certificati: non considerarla verificata soltanto sulla base delle prove del JAR.

## Direzione possibile

L'idea è completare prima la gestione quotidiana di entrate e spese, rendendo i due percorsi coerenti. Un ordine ragionevole, da rivalutare, è:

1. Completare dettaglio, modifica ed eliminazione delle entrate.
2. Aggiungere filtri, ordinamento e paginazione alle spese.
3. Applicare lo stesso comportamento alle entrate.
4. Aggiungere un periodo al riepilogo.
5. Consolidare interfaccia, test e documentazione.

Ogni punto può costituire un'attività autonoma. Non è necessario completare tutto in una volta.

## Entrate: completamento del CRUD

Possibili endpoint finali:

| Metodo | Endpoint | Funzione |
| --- | --- | --- |
| GET | `/api/incomes` | Elenco filtrato e paginato |
| GET | `/api/incomes/{id}` | Dettaglio |
| POST | `/api/incomes` | Creazione |
| PUT | `/api/incomes/{id}` | Sostituzione dei campi modificabili |
| PATCH | `/api/incomes/{id}` | Modifica parziale |
| DELETE | `/api/incomes/{id}` | Eliminazione |

Seguire le convenzioni delle spese, verificando prima il codice reale. Conservare i vincoli di dominio e le categorie esistenti. Chiarire campo assente, `null` e stringa vuota nelle PATCH senza introdurre cambiamenti involontari alle spese.

Nelle modifiche per ID, cercare sempre anche per proprietario. Un movimento inesistente o appartenente ad altri deve seguire la convenzione 404 già presente, senza rivelare dati altrui.

## Filtri: contratto proposto

Gli stessi nomi e significati dei parametri potrebbero essere usati per entrambi gli elenchi, mantenendo distinte le rispettive categorie.

```http
GET /api/expenses?category=transport&from=2026-10-01&to=2026-10-31&page=0&size=20&sort=date,desc
```

| Parametro | Significato proposto |
| --- | --- |
| `category` | Una categoria del dominio richiesto |
| `from` | Data iniziale inclusa, formato `YYYY-MM-DD` |
| `to` | Data finale inclusa, formato `YYYY-MM-DD` |
| `minAmount` | Importo minimo incluso |
| `maxAmount` | Importo massimo incluso |
| `q` | Sottostringa cercata nel titolo |
| `page` | Indice pagina a partire da zero |
| `size` | Numero massimo di elementi della pagina |
| `sort` | Campo e direzione consentiti |

Si può partire soltanto da categoria e date, aggiungendo gli altri filtri quando utili. Tutti i filtri sono facoltativi e quelli presenti si combinano con AND.

### Regole da concordare prima di implementare

- Date: estremi inclusivi; un solo estremo è valido; nessun estremo significa nessun limite temporale. `from > to` restituisce 400. Usare la data del movimento come `LocalDate`, senza conversioni di fuso orario.
- Importi: usare `BigDecimal`; limiti maggiori o uguali a zero; `minAmount > maxAmount` restituisce 400. Non confondere questi limiti con i vincoli degli importi in creazione/modifica.
- Testo: trim, ricerca nel titolo senza distinzione maiuscole/minuscole, vuoto dopo trim equivalente a filtro assente. Possibile massimo di 100 caratteri. Trattare `%` e `_` come testo nella ricerca, evitando wildcard involontarie.
- Input: date/importi malformati, categoria non valida e ordinamento non consentito restituiscono 400. Omettere dal client i parametri non valorizzati; definire e testare il comportamento delle stringhe vuote nei parametri tipizzati.
- Nessuna corrispondenza: 200 con elenco vuoto.
- Sicurezza: aggiungere sempre il proprietario autenticato alla query. Non accettare un proprietario scelto dal client.

### Paginazione e ordinamento

Proposta iniziale: `page=0`, `size=20`, `sort=date,desc`. Accettare `page >= 0` e `1 <= size <= 100`; rifiutare gli altri valori con 400. Limitare i campi ordinabili a `date`, `amount` e `title` e le direzioni a `asc`/`desc`.

Aggiungere un criterio secondario stabile, per esempio `date DESC, id DESC`. Una pagina oltre l'ultima può restituire 200 con elementi vuoti.

Possibile DTO di risposta, da concordare prima di modificare il client:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`totalElements` conta tutti i movimenti corrispondenti ai filtri. Il passaggio dall'array attuale a questo oggetto cambia il contratto REST: aggiornare insieme i consumatori Angular e i test interessati.

## Spring Boot: possibile approccio

- Controller: riceve parametri e autenticazione, valida la richiesta e restituisce DTO.
- Service: verifica regole tra campi, ricava l'utente e coordina ricerca e mapping.
- Repository: filtra, ordina e pagina direttamente in MySQL, senza caricare tutti i movimenti in memoria.

Valutare `Specification` e `JpaSpecificationExecutor` di Spring Data JPA per comporre condizioni facoltative. Restare nello stack attuale e scegliere la soluzione più semplice per il numero reale di filtri, senza un framework generico aggiuntivo.

Il vincolo sul proprietario deve essere obbligatorio anche quando tutti gli altri filtri sono assenti.

## Dashboard: possibile riepilogo per periodo

```http
GET /api/dashboard/summary?from=2026-10-01&to=2026-10-31
```

Conservare totale entrate, totale spese e saldo. Applicare le stesse regole temporali degli elenchi; senza date mantenere il riepilogo complessivo, senza movimenti restituire zero.

Le somme devono essere calcolate nel database sull'intero periodo e sul solo proprietario autenticato, mai sulla pagina corrente.

Per una prima versione, il riepilogo potrebbe dipendere soltanto dal periodo. Categoria, testo e importi dei singoli elenchi non modificherebbero il riepilogo: rendere esplicita questa distinzione nella UI. Decidere alla ripresa se usare un selettore di periodo condiviso o controlli separati con etichette chiare.

## Angular: collegare form, query parameter e risultati

Prima di scrivere codice, concordare i contratti REST e adattare l'organizzazione esistente.

- Definire tipi per filtri, risultati paginati e richieste CRUD entrate.
- Costruire le richieste con `HttpParams`, omettendo valori assenti. `HttpParams` è immutabile: conservare il risultato di `set`.
- Per gli importi controllare `null`/`undefined`: zero è un filtro valido e non va scartato con un controllo truthy.
- Inviare date ISO senza conversioni inutili. JWT e refresh restano gestiti dal flusso di autenticazione esistente, mai dai query parameter.
- Applicare filtri sul server; filtrare solo la pagina già scaricata darebbe risultati incompleti.

Possibile UI per entrambi gli elenchi: categoria, intervallo date/importi, testo, ordinamento, pulsanti “Applica filtri” e “Azzera”, comandi di pagina.

Comportamenti da ricordare:

1. Applicare nuovi filtri, cambiare ordine o dimensione pagina riporta a pagina zero.
2. Cambiare pagina conserva i filtri applicati.
3. Azzera ripristina filtri assenti e valori predefiniti di pagina/ordine.
4. Distinguere caricamento, errore e nessun risultato.
5. Evitare che una risposta precedente sovrascriva una ricerca recente; valutare `switchMap` se adatto al flusso esistente.
6. Dopo CRUD ricaricare elenco e riepilogo interessati, conservando i filtri. Un movimento modificato può sparire perché non corrisponde più alla ricerca.
7. Se un'eliminazione svuota l'ultima pagina, tornare a una pagina valida.

La sincronizzazione dei filtri con l'URL del browser è un'idea facoltativa successiva: è distinta dai query parameter inviati all'API.

## Verifiche utili quando si riprenderà il lavoro

Usare MySQL temporaneo o un database test dedicato, mai dati personali, condivisi o della demo pubblica. I test esistenti non predispongono automaticamente un database isolato.

Backend: controllare filtri singoli/combinati, estremi inclusivi, importo zero nei limiti, input non validi, ordinamento stabile, conteggi e pagine vuote. Verificare CRUD entrate e somme del riepilogo. Provare tutto con due utenti e mantenere le regressioni di autenticazione, refresh e logout.

Frontend: verificare costruzione dei parametri, reset e conservazione pagina/filtri, risposta paginata, stati UI e aggiornamenti dopo CRUD.

Prova completa: creare entrate e spese su date/categorie/importi diversi, filtrare e cambiare pagina, modificare/eliminare, controllare i totali del periodo e l'isolamento tra due utenti. Non usare soltanto `/health` per dichiarare verificata l'applicazione.

## Idee da lasciare per un eventuale seguito

Budget mensile, movimenti ricorrenti, esportazioni e nuove statistiche possono aspettare una scelta di scope separata.

Mailpit resta adatto allo sviluppo locale. Un servizio email pubblico richiederà configurazione e verifica dedicate; al momento la registrazione pubblica è disabilitata in attesa del provider HTTPS. Non considerare sufficiente cambiare soltanto l'host SMTP.

## Come ripartire senza perdersi

Alla ripresa rileggere questo file, verificare cosa esiste già nei due repository e scegliere una sola attività. Una possibile prima attività è il CRUD entrate, seguito dai filtri delle spese. Completare backend, collegamento Angular e verifica di quel singolo passaggio prima di ampliare il lavoro.

Aggiornare questo documento per distinguere idee ancora aperte, decisioni prese e funzionalità realmente completate. Non aggiornare lo stato pubblico del README come se tutte le proposte fossero già implementate.
