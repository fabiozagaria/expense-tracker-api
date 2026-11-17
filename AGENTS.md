# Istruzioni per assistenti AI

## Scopo
Backend Spring Boot del Gestionale Spese full stack. Il repository è in sviluppo attivo e deve restare coerente con il frontend `expense-tracker-angular`.

## Regole operative
- Leggere `README.md` e `docs/CONTEXT.md` prima di modifiche ampie.
- Preservare separazione controller/service/repository e DTO dedicati.
- Verificare transazioni, validazione e semantica HTTP quando si modifica il CRUD.
- Non introdurre autenticazione, nuovi domini o infrastruttura non richiesta solo perché sono previsti in futuro.
- Prima di dichiarare completata una modifica, eseguire/verificare i test pertinenti quando disponibili.
- **Documentazione sempre allineata.** Ogni modifica (codice, struttura, stato, decisione, versione) aggiorna nella stessa PR tutti i documenti che la riguardano: `README.md`, `docs/CONTEXT.md`, `docs/DEPLOYMENT.md`, `docs/EMAIL.md`, `FUTURE.md`, questo file e la scheda Notion del progetto. Se cambiano versione, stato, descrizione o schema del progetto, controllare anche la sua scheda nel portfolio (`fabio-zagaria-portfolio`: dati in `portfolio-angular/src/app/data`, `README.md` e `docs/CONTEXT.md`). Prima di dichiarare finito un lavoro, passare in rassegna l'elenco e dire per ciascun documento se è stato aggiornato o perché non serve. Una modifica senza documenti allineati non è Done.

## Fonti di verità
- Il codice backend è la verità sugli endpoint realmente disponibili.
- Il frontend collegato deve adeguarsi al contratto REST reale.
- `README.md` presenta lo stato pubblico; `docs/CONTEXT.md` sintetizza lo stato tecnico corrente.
- Roadmap didattica, voti e ripassi restano in Notion.

## Sicurezza
Non inserire password database, token, chiavi o altri segreti nel repository.