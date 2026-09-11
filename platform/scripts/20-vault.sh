#!/usr/bin/env bash
# Pushes Vault as an application on the platform and seeds the secrets the
# demo needs. Vault answers on an internal route that only the config server
# is allowed to reach; the public route exists so an operator can seed it and
# open the UI during the talk.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf curl
golden::target

VAULT_IMAGE="${VAULT_IMAGE:-hashicorp/vault:1.20}"
VAULT_PUBLIC="golden-vault.${CF_APPS_DOMAIN}"

golden::step "Pushing Vault in dev mode"
# The image exposes 8200 and Cloud Foundry uses the port the image declares,
# so the listener has to agree with it. The dev root token is supplied through
# the image's own environment contract rather than a start command override.
cf push golden-vault \
  --docker-image "${VAULT_IMAGE}" \
  -m 512M -k 1G -i 1 \
  --no-route \
  -u http --endpoint /v1/sys/health \
  --no-start

cf set-env golden-vault VAULT_DEV_ROOT_TOKEN_ID "${VAULT_ROOT_TOKEN}" >/dev/null
cf set-env golden-vault VAULT_DEV_LISTEN_ADDRESS "0.0.0.0:8200" >/dev/null
cf set-env golden-vault VAULT_ADDR "http://127.0.0.1:8200" >/dev/null

cf map-route golden-vault "${CF_APPS_DOMAIN}" --hostname golden-vault >/dev/null
cf map-route golden-vault apps.internal --hostname golden-vault >/dev/null
cf start golden-vault

golden::step "Waiting for Vault to unseal"
for _ in $(seq 1 40); do
  if curl --silent --fail --insecure --max-time 5 "https://${VAULT_PUBLIC}/v1/sys/health" >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

golden::step "Seeding secrets"
vault_write() {
  local path="$1"
  local payload="$2"
  curl --silent --show-error --fail --insecure \
    --header "X-Vault-Token: ${VAULT_ROOT_TOKEN}" \
    --request POST \
    --data "${payload}" \
    "https://${VAULT_PUBLIC}/v1/secret/data/${path}" >/dev/null
  echo "  secret/${path}"
}

vault_write greeting-service '{"data":{"partner.api.key":"pk_live_9f2c41d8ab77e05b","partner.api.tenant":"northwind"}}'
vault_write application '{"data":{"golden.owner":"platform-team"}}'

golden::step "Vault is up"
echo "  internal: http://golden-vault.apps.internal:8200  (config server only)"
echo "  public:   https://${VAULT_PUBLIC}  (operator access, would not exist in production)"
