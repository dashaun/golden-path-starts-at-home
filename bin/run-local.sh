#!/usr/bin/env bash
# Run one application on this laptop against the platform's config server.
#
#   bin/run-local.sh greeting-service

set -euo pipefail
GOLDEN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="${1:-greeting-service}"

source "${GOLDEN_ROOT}/bin/lib-java.sh"
golden::use_pinned_java "${GOLDEN_ROOT}"

if [[ ! -d "${GOLDEN_ROOT}/bindings/golden-config" ]]; then
  echo "No binding yet. Run bin/dev-bind.sh first." >&2
  exit 1
fi

export SERVICE_BINDING_ROOT="${GOLDEN_ROOT}/bindings"

# Trust the platform's certificate authority, and nothing else new.
CA="${SERVICE_BINDING_ROOT}/golden-config/ca.crt"
TRUSTSTORE="${SERVICE_BINDING_ROOT}/platform-truststore.p12"
if [[ -s "${CA}" ]]; then
  if [[ ! -s "${TRUSTSTORE}" || "${CA}" -nt "${TRUSTSTORE}" ]]; then
    rm -f "${TRUSTSTORE}"
    keytool -importcert -noprompt -trustcacerts \
      -alias golden-platform -file "${CA}" \
      -keystore "${TRUSTSTORE}" -storetype PKCS12 -storepass changeit >/dev/null
    chmod 600 "${TRUSTSTORE}"
  fi
  export SPRING_APPLICATION_JSON="{\"spring.ssl.bundle.pem.platform.truststore.certificate\":\"file:${CA}\"}"
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=${TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=changeit"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=${TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=changeit"
fi

exec "${GOLDEN_ROOT}/mvnw" -f "${GOLDEN_ROOT}/apps/${APP}/pom.xml" spring-boot:run
