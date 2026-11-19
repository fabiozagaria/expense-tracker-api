#!/bin/sh
set -eu

# Il profilo "demo" usa H2 in memoria; il profilo "neon" (SPRING_PROFILES_ACTIVE=demo,neon) usa
# PostgreSQL su Neon con TLS verificato dal driver (sslmode=require nell'URL, CA pubblica).
# Le credenziali del database restano nelle variabili d'ambiente.
exec java -jar /app/app.jar
