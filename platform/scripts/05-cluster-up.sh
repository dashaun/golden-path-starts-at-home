#!/usr/bin/env bash
# Brings up Cloud Foundry on a local kind cluster, using the upstream
# cloudfoundry/kind-deployment project unchanged.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require docker git make cf

KIND_DIR="${GOLDEN_ROOT}/platform/kind-deployment"

if curl --silent --fail --insecure --max-time 5 "${CF_API}/v3/info" >/dev/null 2>&1; then
  golden::step "Cloud Foundry is already answering at ${CF_API}"
else
  golden::step "Cloning cloudfoundry/kind-deployment"
  [[ -d "${KIND_DIR}" ]] || git clone https://github.com/cloudfoundry/kind-deployment.git "${KIND_DIR}"

  golden::step "Standing up the cluster (this takes a while the first time)"
  make -C "${KIND_DIR}" up
  make -C "${KIND_DIR}" bootstrap
fi

golden::step "Logging in"
if [[ -f "${KIND_DIR}/temp/secrets.sh" ]]; then
  # shellcheck disable=SC1091
  source "${KIND_DIR}/temp/secrets.sh"
  cf login -a "${CF_API}" -u "${CF_ADMIN_USER}" -p "${CC_ADMIN_PASSWORD}" --skip-ssl-validation
else
  echo "No generated secrets found; assuming you are already logged in."
  cf target >/dev/null
fi
