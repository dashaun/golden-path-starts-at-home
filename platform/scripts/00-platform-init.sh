#!/usr/bin/env bash
# Generates platform/.env with fresh random credentials. Safe to re-run:
# an existing file is left alone so a running demo keeps working.

set -euo pipefail
GOLDEN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${GOLDEN_ROOT}/platform/.env"

if [[ -f "${ENV_FILE}" ]]; then
  echo "platform/.env already exists, leaving it alone."
  exit 0
fi

rand() { LC_ALL=C tr -dc 'a-zA-Z0-9' </dev/urandom | head -c 32; }

sed \
  -e "s|^VAULT_ROOT_TOKEN=.*|VAULT_ROOT_TOKEN=$(rand)|" \
  -e "s|^CONFIG_CLIENT_PASSWORD=.*|CONFIG_CLIENT_PASSWORD=$(rand)|" \
  "${GOLDEN_ROOT}/platform/.env.example" > "${ENV_FILE}"

chmod 600 "${ENV_FILE}"
echo "Wrote ${ENV_FILE} with generated credentials."
echo "This file is git-ignored and is the only place on this machine they exist."
