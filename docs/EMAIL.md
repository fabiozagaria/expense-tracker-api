# Email di verifica: SMTP locale e Brevo HTTPS

L'implementazione resta nel package `services.mail`, con una sola interfaccia `VerificationEmailSender` e configurazione `app.mail`. Il deploy esistente continua a usare `MAIL_PROVIDER=brevo`, `MAIL_FROM` e `BREVO_API_KEY`: non occorrono nuove variabili obbligatorie né modifiche alla registrazione online.

## Configurazione

| Variabile | SMTP locale | Brevo online | Trasporto disabilitato |
| --- | --- | --- | --- |
| `MAIL_PROVIDER` | `smtp` (default base) | `brevo` | `disabled` (default del profilo demo) |
| `MAIL_FROM` | `noreply@expense-tracker.local` (default) | indirizzo verificato nel proprio account Brevo | default locale sufficiente |
| `MAIL_FROM_NAME` | `Gestionale Spese` | `Gestionale Spese`, modificabile | non usato |
| `BREVO_API_KEY` | non richiesta | chiave API v3 riservata | non richiesta |
| `REGISTRATION_ENABLED` | `true` (default base) | `true`, già configurato nel deploy esistente | deve essere `false` |

Il profilo demo conserva `REGISTRATION_ENABLED=false` come default, ma le variabili esplicite del deploy hanno precedenza e mantengono attiva la registrazione. Non viene modificata la configurazione del servizio pubblico.

Provider sconosciuto, indirizzo mittente non valido o timeout fuori dall'intervallo 100–30000 ms fanno fallire l'avvio. Brevo richiede una chiave non vuota e rifiuta il mittente locale `.local`; la configurazione non può provare che un indirizzo sia stato realmente verificato nel pannello del provider. `disabled` con registrazione abilitata viene rifiutato all'avvio. Non esiste un fallback SMTP automatico se Brevo fallisce.

Con Maven le variabili vanno esportate nella shell o impostate nell'IDE: `.env` non è caricato automaticamente. Il file `.env.example` è un riferimento; il Compose locale configura SMTP/Mailpit come descritto nel README.

## Richiesta e risultato

Brevo riceve un solo `POST https://api.brevo.com/v3/smtp/email`, tramite HTTPS/443. La parola `smtp` nel percorso non indica uso delle porte SMTP. Il payload contiene mittente configurato, destinatario, oggetto, testo e HTML. Username e URL sono escapati nel contenuto HTML; il link contiene un token casuale valido 10 minuti, memorizzato sul database solo come hash SHA-256. SMTP/Mailpit mantiene l'invio del contenuto testuale.

L'accettazione richiede HTTP `201` e un `messageId` non vuoto. Errori HTTP, rete, timeout, risposta non leggibile o conferma mancante producono HTTP `503` con il codice pubblico esistente `EMAIL_DELIVERY_FAILED`. Corpi del provider, chiavi e link di verifica non sono restituiti al client né associati alla causa dell'eccezione applicativa.

La richiesta HTTPS ha timeout di connessione di 5 secondi e lettura di 10 secondi, configurabili tramite `app.mail.connect-timeout-ms` e `app.mail.read-timeout-ms`. Non segue redirect e il codice non ripete automaticamente la POST. SMTP ha timeout di connessione di 5 secondi e lettura/scrittura di 10 secondi.

Il `messageId` dimostra solo l'accettazione della richiesta, non la consegna in inbox. Quote, verifiche mittente, spam e ritardi dipendono dal provider; un link consegnato dopo la scadenza non è utilizzabile. Non sono introdotti webhook, outbox o reinvio automatico dei link.

## Transazioni e verifiche

Prima dell'invio, la registrazione forza il flush dei vincoli MySQL. Un fallimento del trasporto annulla utente e token; un nuovo tentativo manuale può quindi riutilizzare lo stesso username. Invio e commit non sono atomici: un timeout può nascondere un invio già accettato, oppure il commit successivo può fallire, lasciando un link inutilizzabile. Non viene promesso un comportamento exactly-once.

I test del trasporto usano risposte HTTP simulate e un mittente sintetico. I test di registrazione usano MySQL isolato e `JavaMailSender` simulato, senza inviare email reali. Oltre a errori e rollback, verificano che la configurazione Brevo possa avviarsi senza SMTP e che quella disabilitata non annunci una registrazione funzionante.
