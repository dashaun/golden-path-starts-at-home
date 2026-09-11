#!/usr/bin/env bash
# Creates the service instance every application binds to. This is the whole
# contract: a name, a type, and the credentials the platform owns.
#
# The platform's certificate authority ships with the credentials. A laptop
# that is told to trust a server should be handed what it needs to verify it.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf python3
golden::target

CONFIG_URI="https://config-server.${CF_APPS_DOMAIN}"
CONFIG_INTERNAL_URI="http://config-server.apps.internal:8080"

golden::step "Reading the platform certificate authority"
PLATFORM_CA=""
KIND_CA="${GOLDEN_ROOT}/platform/kind-deployment/temp/certs/ca.crt"
if [[ -s "${KIND_CA}" ]]; then
  PLATFORM_CA="$(cat "${KIND_CA}")"
  echo "  from ${KIND_CA}"
elif command -v kubectl >/dev/null 2>&1; then
  PLATFORM_CA="$(kubectl get secret all-in-one-tls -n cf-system \
      -o jsonpath='{.data.ca\.crt}' 2>/dev/null | base64 -d || true)"
  [[ -n "${PLATFORM_CA}" ]] && echo "  from the cf-system all-in-one-tls secret"
fi
if [[ -z "${PLATFORM_CA}" ]]; then
  echo "  not found; laptops will have to trust the config server some other way" >&2
fi

credentials() {
  CONFIG_URI_VALUE="$1" \
  CONFIG_USERNAME="${CONFIG_CLIENT_USERNAME}" \
  CONFIG_PASSWORD="${CONFIG_CLIENT_PASSWORD}" \
  PLATFORM_CA="${PLATFORM_CA}" \
  python3 -c '
import json, os
payload = {
    "uri": os.environ["CONFIG_URI_VALUE"],
    "username": os.environ["CONFIG_USERNAME"],
    "password": os.environ["CONFIG_PASSWORD"],
}
ca = os.environ.get("PLATFORM_CA", "")
if ca.strip():
    payload["ca.crt"] = ca
print(json.dumps(payload))
'
}

upsert() {
  local name="$1"
  local uri="$2"
  local payload
  payload="$(credentials "${uri}")"
  if cf service "${name}" >/dev/null 2>&1; then
    cf update-user-provided-service "${name}" -p "${payload}" -t "config"
  else
    cf create-user-provided-service "${name}" -p "${payload}" -t "config"
  fi
}

# Applications inside the platform take the internal route; a laptop gets the
# public one. That is the only difference between the two instances.
golden::step "Creating golden-config for applications on the platform"
upsert golden-config "${CONFIG_INTERNAL_URI}"

golden::step "Creating golden-config-external for laptops"
upsert golden-config-external "${CONFIG_URI}"

golden::step "Done"
cf services
