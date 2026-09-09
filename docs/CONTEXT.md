# Contesto tecnico — Gestionale Spese Backend

Aggiornato: 2026-09-09

## Obiettivo corrente
Integrare gradualmente Spring Security nel Gestionale Spese senza perdere il verticale Expense già verificato, mantenendo WIP = 1 sulla catena di autenticazione backend.

## Stato osservato
- Versione dichiarata: 0.3.0-SNAPSHOT.
- Java 21, Spring Boot 4.1, Spring Data JPA, Jakarta Validation e MySQL.
- CRUD Expense implementato: GET, GET by id, POST, PUT, PATCH e DELETE.
- POST, DELETE e PATCH erano stati verificati manualmente end-to-end con Angular e persistenza MySQL prima delle modifiche Security/owner.
- PATCH descrizione corretto: `null` significa campo non modificato; stringa vuota è accettata per svuotare la descrizione.
- Lombok introdotto sulle entity.
- `User` contiene username, password, ruolo e relazione `@OneToMany` verso Expense; `Expense` contiene `owner` con `@ManyToOne`.
- `UserRepository.findByUsername(...)` e `CustomUserDetailsService` sono stati introdotti.
- `AuthController` crea un `UsernamePasswordAuthenticationToken` e delega la verifica credenziali a `AuthenticationManager`.
- `SecurityConfig` è stateless, espone `AuthenticationManager` e `PasswordEncoder` BCrypt e permette `POST /auth/login`.
- Aggiunta e verificata su `master` la dipendenza `spring-boot-starter-security-oauth2-resource-server` per il supporto Resource Server/JWT.
- Presente `jwt.secret=${SECRET_KEY}` in configurazione; nessun segreto reale è versionato.

## WIP / blocco corrente
La base Security è ancora incompleta e non è Done:
- `CustomUserDetailsService` implementa correttamente `UserDetailsService`, ma nello stato verificato non è ancora registrato come bean Spring (`@Service`/`@Component` o `@Bean`);
- la configurazione Resource Server/JWT è solo scaffold: manca ancora la strategia effettiva di decoding/verifica token;
- `ExpenseCreateRequest` accetta attualmente un intero `User owner`: con autenticazione reale l'owner non dovrà essere scelto dal client, ma derivato dal principal autenticato;
- `ExpenseResponse` restituisce attualmente l'entity `User`, scelta da correggere per evitare esposizione di dati sensibili e problemi di serializzazione;
- il contratto POST Expense non è più allineato all'Angular attuale perché il frontend non invia `owner`;
- nessun nuovo test comportamentale o avvio completo del backend è stato verificato dopo queste modifiche.

## Prossima azione
Registrare `CustomUserDetailsService` come bean Spring e verificare che il backend si avvii con una catena `AuthenticationManager` → `UserDetailsService` realmente collegata al database. Non generare ancora JWT prima di questa verifica.

## Priorità tecniche successive
- spostare l'assegnazione di `Expense.owner` dal payload client al principal autenticato;
- evitare di esporre direttamente l'entity `User` nei DTO Expense;
- completare login e generazione/verifica JWT solo dopo la catena di autenticazione base;
- aggiungere test JUnit/Mockito e test di integrazione Security/Expense;
- consolidare validazione, error handling, CORS e ambienti.

## Decisioni tecniche
- Spring Security viene introdotto direttamente nel progetto Expense Tracker; Task Manager resta solo riferimento didattico occasionale.
- Il flusso ricostruito è: `UserRepository` → `UserDetailsService` → `AuthenticationManager` → endpoint login → JWT.
- L'utente proprietario di una Expense dovrà essere determinato lato server dall'identità autenticata, non accettato come autorità dal client.
- Password persistite e confrontate tramite `PasswordEncoder` BCrypt; nessuna password in chiaro deve essere considerata accettabile.

## Test / verifiche della sessione 09/09/2026
- Verificato su GitHub `master` il commit Security iniziale.
- Verificati struttura `User`, repository, `CustomUserDetailsService`, `SecurityConfig`, `AuthController` e mapping owner.
- Verificata presenza della dipendenza Resource Server nel `pom.xml`.
- Non risultano verificati in questa sessione compilazione completa, startup applicazione, login reale o JWT.

## Regola di sincronizzazione
Il contratto REST realmente implementato qui è la fonte di verità per `expense-tracker-angular`. Non documentare sicurezza, utenti o autenticazione come completati finché non compilano, non si avviano e non sono verificati. A fine sessione significativa aggiornare questo file insieme alla scheda Notion del progetto.