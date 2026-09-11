#!/usr/bin/env bash
# Builds and pushes the config server, gives it the one credential it needs,
# and opens the single network path from it to Vault.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf
golden::target

golden::step "Building the config server"
"${GOLDEN_ROOT}/mvnw" -q -B -DskipTests -f "${GOLDEN_ROOT}/apps/config-server/pom.xml" package

golden::step "Pushing the config server"
cf push -f "${GOLDEN_ROOT}/apps/config-server/manifest.yml" \
  --var apps_domain="${CF_APPS_DOMAIN}" \
  --no-start

golden::step "Handing it its Vault token and its own credentials"
cf set-env config-server VAULT_TOKEN "${VAULT_ROOT_TOKEN}" >/dev/null
cf set-env config-server VAULT_HOST golden-vault.apps.internal >/dev/null
cf set-env config-server VAULT_PORT 8200 >/dev/null
cf set-env config-server VAULT_SCHEME http >/dev/null
cf set-env config-server CONFIG_CLIENT_USERNAME "${CONFIG_CLIENT_USERNAME}" >/dev/null
cf set-env config-server CONFIG_CLIENT_PASSWORD "${CONFIG_CLIENT_PASSWORD}" >/dev/null

golden::step "Allowing the config server, and only it, to reach Vault"
cf add-network-policy config-server golden-vault --protocol tcp --port 8200 >/dev/null

cf start config-server

golden::step "Checking that Vault values are reaching the config server"
curl --silent --show-error --insecure \
  --user "${CONFIG_CLIENT_USERNAME}:${CONFIG_CLIENT_PASSWORD}" \
  "https://config-server.${CF_APPS_DOMAIN}/greeting-service/default" | head -c 600
echo
