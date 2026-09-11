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

# An application container is not a member of your tailnet, so it cannot
# resolve a MagicDNS name. It can route to the address, so resolve the name
# here and hand the container the result.
resolve_ollama() {
  local url="${OLLAMA_BASE_URL}"
  local rest="${url#*://}"
  local scheme="${url%%://*}"
  local hostport="${rest%%/*}"
  local host="${hostport%%:*}"
  local port="${hostport#*:}"
  [[ "${port}" == "${hostport}" ]] && port=""

  if [[ "${host}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    printf '%s' "${url}"
    return
  fi

  local address
  address="$(ping -c1 -t1 "${host}" 2>/dev/null | head -1 | sed -n 's/.*(\([0-9.]*\)).*/\1/p')"
  if [[ -z "${address}" ]]; then
    echo "  could not resolve ${host}; the agent will get the name as written" >&2
    printf '%s' "${url}"
    return
  fi
  echo "  ${host} resolves to ${address}" >&2
  printf '%s://%s%s' "${scheme}" "${address}" "${port:+:${port}}"
}

golden::step "Setting the values that are locations, not secrets"
OLLAMA_URL_FOR_PLATFORM="$(resolve_ollama)"
cf set-env code-mcp-server GOLDEN_MCP_WORKSPACEROOT /home/vcap/app/workspace >/dev/null
cf set-env steward-agent OLLAMA_BASE_URL "${OLLAMA_URL_FOR_PLATFORM}" >/dev/null
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
