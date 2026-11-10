# Roadmap — Expense Tracker API

Aggiornato il 10 novembre 2026.

## Stato attuale

La versione **1.2.0** completa il perimetro funzionale attuale: il progetto è concluso. L'API offre il CRUD di spese ed entrate, ricerca con filtri e paginazione, riepilogo finanziario e isolamento dei dati per utente. L'autenticazione include verifica email, recupero password, rinnovo della sessione e logout. `GET /api/me` restituisce username ed email dell'utente autenticato, usati dalla pagina Impostazioni del frontend.

Per il contratto REST e i dettagli tecnici, consulta il [README](README.md) e il [contesto tecnico](docs/CONTEXT.md).

## Sviluppi futuri

Le impostazioni dell'utente autenticato ci sono già (`GET /api/me` e il link per cambiare la password). L'unica idea rimasta aperta è l'**eliminazione dell'account**: l'utente potrebbe eliminare il proprio account dalle impostazioni.

È rinviata a molto più avanti e verrà eventualmente sviluppata dal proprietario del progetto. Prima di avviarla resta da decidere come gestire i dati associati all'account (spese, entrate, token) e le sessioni attive.
