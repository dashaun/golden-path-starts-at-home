#!/usr/bin/env bash
# Creates the org and space this demo lives in. Nothing outside them is touched.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf

golden::step "Creating org ${CF_ORG} and space ${CF_SPACE}"
cf create-org "${CF_ORG}" >/dev/null
cf create-space "${CF_SPACE}" -o "${CF_ORG}" >/dev/null
golden::target

golden::step "Enabling docker images so Vault can be pushed"
cf enable-feature-flag diego_docker >/dev/null || true
