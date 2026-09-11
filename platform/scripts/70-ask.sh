#!/usr/bin/env bash
# Ask the agent a question. It has to use the MCP server to answer, because
# it cannot see the repository any other way.
#
#   platform/scripts/70-ask.sh "where are secrets allowed to live?"

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
golden::load_env
golden::require curl
golden::target

QUESTION="${*:-Where in this repository is it acceptable to put a secret, and why?}"

golden::step "Asking the steward"
echo "  ${QUESTION}"
echo

curl --silent --show-error --insecure --max-time 300 \
  --request POST \
  --header 'Content-Type: application/json' \
  --data "$(QUESTION="${QUESTION}" python3 -c 'import json,os;print(json.dumps({"question":os.environ["QUESTION"]}))')" \
  "https://steward-agent.${CF_APPS_DOMAIN}/ask" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin).get("answer","no answer"))'
