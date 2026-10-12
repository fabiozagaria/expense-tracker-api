#!/bin/sh
set -eu

# Aiven's CA is public; database credentials must remain in environment variables.
if [ "${SPRING_PROFILES_ACTIVE:-}" = "demo" ]; then
  ca_file=/etc/secrets/aiven-ca.pem
  if [ -n "${AIVEN_CA_CERT:-}" ]; then
    ca_file=/tmp/aiven-ca.pem
    printf '%s\n' "$AIVEN_CA_CERT" > "$ca_file"
  fi
  test -r "$ca_file" || { echo 'Missing Aiven CA certificate' >&2; exit 1; }
  rm -f /tmp/aiven-truststore.p12
  keytool -importcert -noprompt -alias aiven -file "$ca_file" \
    -keystore /tmp/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
fi
exec java -jar /app/app.jar
