# Contesto tecnico — Gestionale Spese Backend

Aggiornato: 2026-09-21

## Obiettivo corrente

Verificare il verticale registrazione → verifica email → login → spese personali → refresh/logout insieme al frontend Angular.

## Stato osservato

- Spring Boot 4.1, Java 21 come target, JPA/MySQL, Spring Security Resource Server e BCrypt.
- `POST /auth/register` crea un utente non verificato e invia un link tramite Mailpit; `POST /auth/verify-email` lo abilita.
- La verifica dello stesso token risponde ancora con successo se l'account risulta già confermato; i token sconosciuti o scaduti per utenti non verificati restano rifiutati.
- `POST /auth/login` restituisce un access token JWT di 15 minuti e imposta un refresh token di 7 giorni in cookie HttpOnly.
- `POST /auth/refresh` ruota il refresh token; `POST /auth/logout` lo revoca.
- `/api/expenses` richiede Bearer JWT; il server ricava l'owner dal principal e filtra lettura, modifica ed eliminazione per proprietario.
- CORS locale centralizzato in `SecurityConfig` per `http://localhost:4200` con credenziali.
- Maven configura Lombok come annotation processor anche con il JDK 25 installato.
- Dockerfile multi-stage con JDK/JRE 21 e Compose per API, MySQL 8 persistente e Mailpit; password e chiave JWT arrivano dall'ambiente.
- `mvnw.cmd test` passa; il test di integrazione usa MySQL reale, email simulata e verifica registrazione, verifica email ripetuta, login, Bearer, refresh e isolamento fra utenti.
- Docker Compose è stato avviato in uno stack temporaneo: MySQL healthy, API HTTP 401 senza Bearer e Mailpit HTTP 200. Lo stack di prova è stato rimosso.

## Limiti e prossima verifica

- Resta la prova manuale del link nel browser con Angular e lo stack Docker avviati insieme.
- Cookie `Secure=false`, URL di verifica e CORS sono configurati per sviluppo locale. La pubblicazione dell'API richiede configurazione esplicita degli ambienti.
- Servono altri test sui casi limite del CRUD e della sessione.
- Dashboard ed entrate restano fuori dal verticale attuale.

## Regola di sincronizzazione

Il contratto REST qui implementato è la fonte di verità per `expense-tracker-angular`. Aggiornare questo file e la documentazione pubblica quando cambia il contratto.
