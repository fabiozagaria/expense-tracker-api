# Istruzioni per assistenti AI

## Scopo
Backend Spring Boot del Gestionale Spese full stack. Il repository è in sviluppo attivo e deve restare coerente con il frontend `expense-tracker-angular`.

## Regole operative
- Leggere `README.md` e `docs/CONTEXT.md` prima di modifiche ampie.
- Preservare separazione controller/service/repository e DTO dedicati.
- Verificare transazioni, validazione e semantica HTTP quando si modifica il CRUD.
- Non introdurre autenticazione, nuovi domini o infrastruttura non richiesta solo perché sono previsti in futuro.
- Prima di dichiarare completata una modifica, eseguire/verificare i test pertinenti quando disponibili.

## Fonti di verità
- Il codice backend è la verità sugli endpoint realmente disponibili.
- Il frontend collegato deve adeguarsi al contratto REST reale.
- `README.md` presenta lo stato pubblico; `docs/CONTEXT.md` sintetizza lo stato tecnico corrente.
- Roadmap didattica, voti e ripassi restano in Notion.

## Sicurezza
Non inserire password database, token, chiavi o altri segreti nel repository.