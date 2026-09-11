#!/usr/bin/env bash
# Materialize the config server binding for local development.
#
# Nothing is typed, nothing is stored in the repository, and nothing is put in
# your shell. The credentials come from the platform, using the identity you
# already have from `cf login`, and land in a git-ignored directory that only
# the running process reads. Delete the directory and they are gone.

set -euo pipefail

GOLDEN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE="${1:-golden-config-external}"
BINDING_DIR="${GOLDEN_ROOT}/bindings/golden-config"

command -v cf >/dev/null || { echo "cf CLI not found" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 not found" >&2; exit 1; }

cf target >/dev/null 2>&1 || { echo "Run 'cf login' first." >&2; exit 1; }

guid="$(cf service "${SERVICE}" --guid 2>/dev/null || true)"
if [[ -z "${guid}" ]]; then
  echo "No service instance named '${SERVICE}' in the targeted space." >&2
  echo "Check 'cf target' and 'cf services'." >&2
  exit 1
fi

credentials="$(cf curl "/v3/service_instances/${guid}/credentials")"

rm -rf "${BINDING_DIR}"
mkdir -p "${BINDING_DIR}"
chmod 700 "${GOLDEN_ROOT}/bindings" "${BINDING_DIR}"

printf 'config' > "${BINDING_DIR}/type"
printf 'cloud-foundry' > "${BINDING_DIR}/provider"

GOLDEN_CREDENTIALS="${credentials}" python3 - "${BINDING_DIR}" <<'PYEOF'
import json, os, sys
target = sys.argv[1]
data = json.loads(os.environ["GOLDEN_CREDENTIALS"])
if "errors" in data:
    sys.exit("Could not read credentials: %s" % data["errors"])
written = []
for key, value in data.items():
    path = os.path.join(target, key)
    with open(path, "w") as handle:
        handle.write(str(value))
    os.chmod(path, 0o600)
    written.append(key)
# One of these keys is the platform's certificate authority. It arrived the
# same way the password did, because it is the platform's job to supply it.
print("  keys: %s" % ", ".join(sorted(written)))
PYEOF

echo
echo "Binding written to bindings/golden-config"
echo "Run an application with:"
echo "  bin/run-local.sh greeting-service"
echo
echo "Or in your IDE, set one environment variable on the run configuration:"
echo "  SERVICE_BINDING_ROOT=${GOLDEN_ROOT}/bindings"
