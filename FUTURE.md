# Roadmap — Expense Tracker API

Aggiornato il 10 novembre 2026.

## Stato attuale

La versione **1.1.0** completa il perimetro funzionale attuale. L'API offre il CRUD di spese ed entrate, ricerca con filtri e paginazione, riepilogo finanziario e isolamento dei dati per utente. L'autenticazione include verifica email, recupero password, rinnovo della sessione e logout.

Per il contratto REST e i dettagli tecnici, consulta il [README](README.md) e il [contesto tecnico](docs/CONTEXT.md).

## Prossimo aggiornamento proposto

La prossima evoluzione da valutare riguarda la **gestione dell'account**. L'idea è introdurre impostazioni per l'utente autenticato e consentirgli di eliminare il proprio account.

Prima di avviare lo sviluppo, resta da decidere quali altre impostazioni includere e come gestire i dati associati all'account e le sessioni attive in caso di eliminazione. Il perimetro e i tempi sono ancora da definire.
