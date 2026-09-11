#!/usr/bin/env bash
# Builds and pushes the three applications that consume the binding.
#
# Everything is pushed stopped, then the network policies are written, then
# things start in dependency order. An application that cannot reach its
# config server fails fast, which is the behaviour we want everywhere except
# during this script.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf tar
golden::target

golden::step "Building"
"${GOLDEN_ROOT}/mvnw" -q -B -DskipTests install

push() {
  local app="$1"
  golden::step "Pushing ${app}"
  ( cd "${GOLDEN_ROOT}/apps/${app}" && \
      cf push -f manifest.yml --var apps_domain="${CF_APPS_DOMAIN}" --no-start )
}

# The MCP server serves a copy of this repository that the platform staged.
# An agent never reaches a developer's disk.
golden::step "Staging the repository for the MCP server"
WORKSPACE="${GOLDEN_ROOT}/apps/code-mcp-server/target/staged"
rm -rf "${WORKSPACE}"
mkdir -p "${WORKSPACE}/workspace"
cp "$(golden::jar code-mcp-server)" "${WORKSPACE}/code-mcp-server.jar"
tar -C "${GOLDEN_ROOT}" \
  --exclude='./.git' --exclude='./target' --exclude='*/target' \
  --exclude='./platform/kind-deployment' --exclude='./platform/.env' \
  --exclude='./bindings' --exclude='./docs/reveal.js' \
  -cf - . | tar -x -C "${WORKSPACE}/workspace"

push greeting-service
push code-mcp-server
push steward-agent

golden::step "Setting the values that are locations, not secrets"
cf set-env code-mcp-server GOLDEN_MCP_WORKSPACEROOT /home/vcap/app/workspace >/dev/null
cf set-env steward-agent OLLAMA_BASE_URL "${OLLAMA_BASE_URL}" >/dev/null
cf set-env steward-agent OLLAMA_MODEL "${OLLAMA_MODEL}" >/dev/null
cf set-env steward-agent MCP_SERVER_URL "http://code-mcp-server.apps.internal:8080" >/dev/null

golden::step "Writing the network policies"
for app in greeting-service code-mcp-server steward-agent; do
  cf add-network-policy "${app}" config-server --protocol tcp --port 8080 >/dev/null
done
# The agent may reach the MCP server. That is the only thing it may reach.
cf add-network-policy steward-agent code-mcp-server --protocol tcp --port 8080 >/dev/null

golden::step "Starting in dependency order"
cf start greeting-service
cf start code-mcp-server
cf start steward-agent

golden::step "Routes"
cf apps
