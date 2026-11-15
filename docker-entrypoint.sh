#!/bin/sh
set -eu

# Il profilo "demo" usa H2 in memoria e non ha bisogno di certificati. Il profilo "aiven"
# (SPRING_PROFILES_ACTIVE=demo,aiven) punta a un MySQL esterno: la CA di Aiven è pubblica,
# le credenziali del database restano nelle variabili d'ambiente.
case ",${SPRING_PROFILES_ACTIVE:-}," in *,aiven,*)
  ca_file=/etc/secrets/aiven-ca.pem
  if [ -n "${AIVEN_CA_CERT:-}" ]; then
    ca_file=/tmp/aiven-ca.pem
    printf '%s\n' "$AIVEN_CA_CERT" > "$ca_file"
  fi
  test -r "$ca_file" || { echo 'Missing Aiven CA certificate' >&2; exit 1; }
  rm -f /tmp/aiven-truststore.p12
  keytool -importcert -noprompt -alias aiven -file "$ca_file" \
    -keystore /tmp/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
  ;;
esac
exec java -jar /app/app.jar
