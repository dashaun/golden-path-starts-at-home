#!/usr/bin/env bash
# The point of the whole exercise: change a secret in one place and watch it
# reach a running application, with no redeploy and no developer involved.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf curl python3
golden::target

VAULT_PUBLIC="golden-vault.${CF_APPS_DOMAIN}"
GREETING="https://greeting-service.${CF_APPS_DOMAIN}"
NEW_KEY="pk_live_$(openssl rand -hex 8)"

fingerprint() {
  curl --silent --fail --insecure --max-time 10 "${GREETING}/secret-fingerprint" \
    | python3 -c 'import json,sys;print(json.load(sys.stdin)["fingerprint"])'
}

golden::step "Waiting for the application to answer"
for _ in $(seq 1 30); do
  if fingerprint >/dev/null 2>&1; then break; fi
  sleep 3
done

before="$(fingerprint)" || { echo "greeting-service is not answering." >&2; exit 1; }
echo "  fingerprint now: ${before}"

golden::step "Rotating partner.api.key in Vault"
curl --silent --show-error --fail --insecure \
  --header "X-Vault-Token: ${VAULT_ROOT_TOKEN}" \
  --request POST \
  --data "{\"data\":{\"partner.api.key\":\"${NEW_KEY}\",\"partner.api.tenant\":\"northwind\"}}" \
  "https://${VAULT_PUBLIC}/v1/secret/data/greeting-service" >/dev/null
echo "  written"

golden::step "Asking the application to refresh"
curl --silent --show-error --fail --insecure --request POST \
  "${GREETING}/actuator/refresh"
echo

golden::step "After"
after="$(fingerprint)"
echo "  fingerprint now: ${after}"

echo
if [[ "${before}" == "${after}" ]]; then
  echo "The fingerprint did not change. Something is wrong." >&2
  exit 1
fi
echo "The application was never restarted and no developer saw the value."
