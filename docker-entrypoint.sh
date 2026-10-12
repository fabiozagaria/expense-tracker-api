#!/bin/sh
set -eu

# Aiven's CA is public; do not put database credentials in this file.
if [ "${SPRING_PROFILES_ACTIVE:-}" = "demo" ]; then
  test -r /etc/secrets/aiven-ca.pem || { echo 'Missing Aiven CA certificate' >&2; exit 1; }
  rm -f /tmp/aiven-truststore.p12
  keytool -importcert -noprompt -alias aiven -file /etc/secrets/aiven-ca.pem \
    -keystore /tmp/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
fi
exec java -jar /app/app.jar
