#!/usr/bin/env bash
# The point of the whole exercise: change a secret in one place and watch it
# reach a running application, with no redeploy and no developer involvement.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf curl
golden::target

VAULT_PUBLIC="golden-vault.${CF_APPS_DOMAIN}"
NEW_KEY="pk_live_$(LC_ALL=C tr -dc 'a-f0-9' </dev/urandom | head -c 16)"

golden::step "Before"
curl --silent --insecure "https://greeting-service.${CF_APPS_DOMAIN}/secret-fingerprint"; echo

golden::step "Rotating partner.api.key in Vault"
curl --silent --show-error --fail --insecure \
  --header "X-Vault-Token: ${VAULT_ROOT_TOKEN}" \
  --request POST \
  --data "{\"data\":{\"partner.api.key\":\"${NEW_KEY}\",\"partner.api.tenant\":\"northwind\"}}" \
  "https://${VAULT_PUBLIC}/v1/secret/data/greeting-service" >/dev/null
echo "  written"

golden::step "Asking the application to refresh"
curl --silent --show-error --insecure --request POST \
  "https://greeting-service.${CF_APPS_DOMAIN}/actuator/refresh"; echo

golden::step "After"
curl --silent --insecure "https://greeting-service.${CF_APPS_DOMAIN}/secret-fingerprint"; echo

echo
echo "The application was never restarted and no developer saw the value."
