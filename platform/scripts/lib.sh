#!/usr/bin/env bash
# Shared setup for every script in this directory.

set -euo pipefail

GOLDEN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export GOLDEN_ROOT

ENV_FILE="${GOLDEN_ROOT}/platform/.env"

# shellcheck source=../../bin/lib-java.sh
source "${GOLDEN_ROOT}/bin/lib-java.sh"
golden::use_pinned_java "${GOLDEN_ROOT}"

golden::load_env() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "No platform/.env yet. Run 'make platform-init' first." >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
}

golden::require() {
  for tool in "$@"; do
    command -v "${tool}" >/dev/null 2>&1 || { echo "Missing required tool: ${tool}" >&2; exit 1; }
  done
}

golden::target() {
  cf target -o "${CF_ORG}" -s "${CF_SPACE}" >/dev/null
  echo "Targeted ${CF_ORG}/${CF_SPACE}"
}

golden::step() {
  printf '\n\033[1;32m==>\033[0m %s\n' "$*"
}

golden::jar() {
  local module="$1"
  ls "${GOLDEN_ROOT}/apps/${module}/target/${module}-"*.jar 2>/dev/null | head -1
}
