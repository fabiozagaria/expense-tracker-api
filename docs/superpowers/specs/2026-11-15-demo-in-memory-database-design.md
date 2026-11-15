# Demo con database in memoria — design breve

Data: 15 novembre 2026. Repository: `expense-tracker-api`. Dimensione: piccola modifica di configurazione.

## Intento e perché
Il database MySQL Aiven della demo è stato spento (piano gratuito ritirato) e il proprietario non vuole pagare per una demo. La demo deve tornare online a costo zero e senza dipendere da un database esterno che un provider possa ritirare.

## Cosa sappiamo
- La demo usa un solo account condiviso (`portfolio-demo`) ricreato con tre movimenti di esempio da `DemoDataInitializer` a ogni avvio.
- La registrazione sulla demo è disattivata (`REGISTRATION_ENABLED=false`): non esistono altri utenti da conservare.

## Obiettivi
- Il profilo `demo` usa H2 in memoria: nessun database esterno, nessun costo.
- Nessuna modifica obbligatoria su Render: il merge riavvia il deploy e la demo parte.
- Il percorso MySQL/Aiven resta disponibile con il profilo `aiven` (`SPRING_PROFILES_ACTIVE=demo,aiven`).

## Non obiettivi / anti-obiettivi
- Non cambiare lo schema di produzione né le migrazioni Flyway MySQL.
- Non indebolire i test: restano su MySQL Testcontainers, tranne i test del profilo `demo`, che girano sulla configurazione vera (H2).
- Non promettere dati persistenti: i movimenti aggiunti dai visitatori si perdono al riavvio di Render.

## Approcci considerati
1. **H2 in memoria per il profilo `demo` (scelto):** zero costi, nessuna dipendenza esterna; i dati si azzerano al riavvio, accettabile perché la demo è ri-seminata.
2. PostgreSQL su un piano gratuito: richiede nuove migrazioni, driver e configurazione, e il piano può essere ritirato come quello Aiven.
3. Piano Developer Aiven (~5 $/mese): scartato, il proprietario non vuole pagare.

## Design
- `pom.xml`: dipendenza `com.h2database:h2` (runtime).
- `application-demo.properties`: URL `jdbc:h2:mem:demo`, Flyway spento, `ddl-auto=create` (schema generato da Hibernate). Le proprietà SSL/truststore Aiven passano in `application-aiven.properties` (con Flyway acceso e `ddl-auto=validate`).
- `docker-entrypoint.sh`: crea il truststore Aiven solo se il profilo `aiven` è attivo.
- `MySqlTestContainerInitializer`: nei contesti con profilo `demo` non sovrascrive il datasource (e il container MySQL parte solo quando serve).
- `render.yaml`: tolte `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` del servizio demo.
- Documentazione e avvisi aggiornati a modifica verificata.

## Lasciato al builder
Dettagli di configurazione H2; se alcune query non funzionano su H2 si corregge o si segnala, senza cambiare MySQL.
