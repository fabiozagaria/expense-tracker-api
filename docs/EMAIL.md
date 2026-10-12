# Email di verifica: Brevo HTTPS

## Trasporto e piano gratuito

Il backend invia le email di verifica tramite `POST https://api.brevo.com/v3/smtp/email`, su HTTPS/porta 443. La parola `smtp` nel percorso API non significa che il backend usi il protocollo SMTP: le porte SMTP bloccate da Render Free non sono coinvolte. Non serve un SDK aggiuntivo.

Il piano Brevo Free offre 300 invii al giorno, con branding del provider. Non è un servizio illimitato: oltre la quota, le email transazionali possono essere accodate o non consegnate. I token dell'app scadono dopo 10 minuti, quindi una consegna ritardata può rendere il link inutilizzabile. Il codice verifica l'accettazione della richiesta, non la consegna in inbox.

Non avendo un dominio proprio, è possibile verificare un indirizzo gratuito come mittente con il codice ricevuto per email. Brevo documenta che riscrive questi mittenti con un proprio dominio (per email transazionali, `t-sender-sib.com`). È una misura temporanea del provider: non garantisce la consegna futura e non equivale ad autenticare un dominio proprio. Non si possono configurare i DNS di `vercel.app` o `gmail.com`.

## Configurazione

| Variabile | Locale | Demo Render iniziale | Brevo configurato |
| --- | --- | --- | --- |
| `MAIL_PROVIDER` | `smtp` | `disabled` | `brevo` |
| `REGISTRATION_ENABLED` | `true` | `false` | `true`, solo dopo la verifica |
| `EMAIL_FROM` | facoltativo; fallback Mailpit | vuoto | indirizzo esattamente verificato in Brevo |
| `EMAIL_FROM_NAME` | `Expense Tracker` | `Expense Tracker` | `Expense Tracker` |
| `BREVO_API_KEY` | non serve | vuoto | chiave segreta API v3 |
| `FRONTEND_URL` | `http://localhost:4200` | `https://gestionale-spese.vercel.app` | stesso URL pubblico |

Il profilo `demo` mantiene le protezioni dei cookie e dell'origine anche quando viene abilitata la registrazione. L'accesso «Prova la demo» continua a funzionare. Brevo richiede mittente e chiave configurati per avviare l'app; `disabled` con registrazione abilitata viene rifiutato all'avvio. Non esiste un fallback SMTP automatico quando Brevo fallisce.

## Passaggi che richiedono il proprietario

1. Creare/accedere a un account **Free** su <https://www.brevo.com>; completare le eventuali verifiche richieste dal provider per abilitare le email transazionali.
2. In <https://app.brevo.com/senders>, aggiungere il nome `Expense Tracker` e il proprio indirizzo. Confermare il codice ricevuto nella casella.
3. Nella sezione **SMTP & API → API keys**, generare una chiave API v3 dedicata. Non usare la chiave SMTP. Non copiarla in chat, GitHub, frontend o screenshot.
4. Inserire la chiave in `BREVO_API_KEY` nel pannello **Environment** del servizio Render <https://dashboard.render.com/web/srv-db6g1rrl550s73akepag>. Inserire anche `EMAIL_FROM`, `EMAIL_FROM_NAME` e `MAIL_PROVIDER=brevo`. Tenere `REGISTRATION_ENABLED=false` durante la preparazione.
5. Pubblicare questo branch sul servizio scelto, verificando che il branch effettivamente seguito da Render contenga il commit email. Il servizio esistente segue `demo/render-aiven-setup`; aggiornare soltanto `master` non basta.
6. Concordare una casella per il test reale, aprire temporaneamente la registrazione con `REGISTRATION_ENABLED=true` e verificare: registrazione → email ricevuta (anche Spam) → link entro 10 minuti → login. Chi esegue la prova deve sapere che saranno salvati username e indirizzo.
7. Solo dopo la prova, abilitare il form Angular con `registrationEnabled=true` nell'environment production, mantenendo `demo=true` per conservare il pulsante demo. Prima della prova, la produzione conserva `registrationEnabled=false`.

La chiave API e i codici di conferma richiedono l'accesso del proprietario all'account: l'assistente non può crearli senza quell'accesso. Il branch da solo non abilita email o registrazione sulla demo online.

## Errori e transazioni

La registrazione controlla username/email duplicati e forza il flush dei vincoli MySQL prima di chiamare il provider. Su rifiuto HTTP, timeout o risposta priva di `messageId`, restituisce HTTP 503 con `errorName=EMAIL_DELIVERY_UNAVAILABLE`; la transazione annulla utente e token. Non vengono restituiti corpo provider, API key o link segreto.

L'API invia una sola richiesta, con timeout di connessione 5 secondi e lettura 10 secondi. Non segue redirect e non ripete automaticamente POST. Un timeout è ambiguo: Brevo potrebbe aver accettato l'email. In quel caso la transazione annullata rende quel link inutilizzabile; un nuovo tentativo manuale può produrre un'altra email. L'invio e il commit MySQL non sono atomici: anche un errore del commit dopo l'accettazione può lasciare un link inutilizzabile.

SMTP resta solo per Mailpit/sviluppo locale, con timeout e lo stesso errore applicativo. Il token casuale rimane memorizzato esclusivamente come hash SHA-256, con validità di 10 minuti. Non sono introdotti webhook di consegna, outbox, reinvio di link scaduti o sistemi completi contro l'abuso: se le quote impediscono la prova, richiudere la registrazione e lasciare disponibile l'accesso demo.

## Fonti ufficiali verificate il 12 ottobre 2026

- [Limiti Brevo Free](https://help.brevo.com/hc/en-us/articles/208580669)
- [Creazione e verifica del mittente](https://help.brevo.com/hc/en-us/articles/208836149)
- [Riscrittura degli indirizzi gratuiti e requisiti dei mittenti](https://help.brevo.com/hc/en-us/articles/14925263522578)
- [API email transazionale](https://developers.brevo.com/reference/send-transac-email)
