#!/usr/bin/env bash
# Removes only what this demo created. The kind cluster and anything else
# running on it are left untouched.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require cf

if ! cf target -o "${CF_ORG}" -s "${CF_SPACE}" >/dev/null 2>&1; then
  echo "Org ${CF_ORG} is already gone."
  exit 0
fi

golden::step "Deleting the ${CF_ORG} org and everything in it"
cf delete-org "${CF_ORG}" -f

golden::step "Removing local credentials"
rm -rf "${GOLDEN_ROOT}/bindings"
echo "Left platform/.env in place; delete it yourself if you want a clean slate."
