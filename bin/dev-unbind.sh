#!/usr/bin/env bash
# Remove every credential this machine was holding.

set -euo pipefail
GOLDEN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rm -rf "${GOLDEN_ROOT}/bindings"
echo "Removed bindings/. Nothing on this machine can reach the config server now."
