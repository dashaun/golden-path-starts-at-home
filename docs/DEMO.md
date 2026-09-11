# Demo script

Four demos, in order. Every command is in the `Makefile` or in `bin/`.

## Before you go on stage

```bash
make up                    # everything, on a warm cluster a few minutes
make ask Q="hello"         # wakes the model up so the first live call is fast
bin/dev-unbind.sh          # start unbound, so the binding step is real
```

Have these open: a terminal, your IDE with `apps/greeting-service` loaded, and
a browser on `https://golden-vault.apps.127-0-0-1.nip.io` for the Vault UI.

## Demo 1 — a laptop joins the golden path

```bash
cat apps/greeting-service/src/main/resources/application.yml
bin/dev-bind.sh
ls -la bindings/golden-config
cat .gitignore | grep bindings
bin/run-local.sh greeting-service
curl localhost:8080/ | jq
```

Say: nothing was typed, the credentials came from the platform using the
`cf login` identity, and `bindings/` is git-ignored. The response has a value
from the git-tracked config repo and a value from Vault, and the application
cannot tell them apart.

## Demo 2 — the same artifact on the platform

```bash
cat apps/greeting-service/manifest.yml
cd apps/greeting-service && cf push -f manifest.yml --var apps_domain=apps.127-0-0-1.nip.io
curl -k https://greeting-service.apps.127-0-0-1.nip.io/ | jq
```

Say: the jar did not change and nothing was rebuilt. Point at `boundTo` in the
two responses. The laptop took the public route, the platform took the
internal one, and that is the only difference.

## Demo 3 — rotation

```bash
make rotate
```

Say: a new value in Vault, a refresh, a different fingerprint. No restart, no
deploy, and nobody had to be told.

## Demo 4 — the agent

```bash
cf app steward-agent
make ask Q="Where in this repository is it acceptable to put a secret, and why?"
make ask Q="What is the partner API key?"
```

Say: the agent runs in a container, reaches the repository only through the
MCP server on an internal route, and holds no binding to anything that has
secrets in it. The second question has no answer for it to find.

Latency on the second question is unpredictable. It has come back in about
two and a half minutes and it has also run past six with nothing. A question
whose answer is "it is not here" gives the model no natural stopping point,
so it keeps searching. Ask it once before the session, and have the network
policy output ready as the fallback if it stalls in front of an audience.

If asked how it is constrained:

```bash
cat config-repo/code-mcp-server.yml
cat apps/steward-agent/src/main/resources/AGENTS.md
cf network-policies
```

## If something goes wrong

| Symptom | Likely cause |
| :--- | :--- |
| An app will not start | its network policy to `config-server` is missing |
| Config server returns 500 | Vault is not answering on `golden-vault.apps.internal:8200` |
| Local run cannot verify TLS | re-run `bin/dev-bind.sh` to refresh `ca.crt` |
| The agent times out | the model address in `cf env steward-agent` is a MagicDNS name, not an address |
| The agent crashes on start | `code-mcp-server` is not running; it connects to MCP eagerly |
| `/ask` returns 502 | restart the agent after restarting the MCP server |
| `/ask` never returns | the model is still searching; `cf logs code-mcp-server` shows the tool calls |

## Afterwards

```bash
make down
```
