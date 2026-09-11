# The Golden Path Starts at Home

Engineering developer experience from laptop to production.

A conference talk and a working demo. The demo stands up Cloud Foundry on a
local kind cluster, runs Vault and Spring Cloud Config Server on it, and shows
one Spring Boot application getting the same configuration and the same secret
whether it runs in your IDE, on `cf push`, or in production.

Nothing in this repository holds a credential, and neither does your laptop.

## The idea

An application should ask for **a** config server, never **the** config server.

```yaml
spring:
  application:
    name: greeting-service
  config:
    import: "configserver:"
```

That is the entire configuration story in the application. No host, no user,
no password, no profile per environment. A service binding supplies the rest,
and the binding looks the same in every environment:

| Environment | Where the binding lives |
| :--- | :--- |
| Laptop | a directory of files under `SERVICE_BINDING_ROOT` |
| Cloud Foundry | a tagged service instance in `VCAP_SERVICES` |
| Kubernetes | the service binding directory the platform mounts |

`libs/golden-binding` is the only code that knows the difference. It is about
ninety lines.

## What gets deployed

```
kind cluster
└── Cloud Foundry (org: golden, space: dev)
    ├── golden-vault        Vault, internal route only, reachable by one app
    ├── config-server       composite backend: Vault for secrets, git for settings
    ├── greeting-service    an ordinary application that binds to the config server
    ├── code-mcp-server     read-only access to this repository, for agents
    └── steward-agent       Spring AI agent, reaches the code only through MCP
```

The demo lives entirely inside its own org and space. `make down` removes the
org and leaves the rest of your cluster untouched.

## Requirements

- Docker, `make`, and the `cf` CLI
- A JDK, ideally the one in `.sdkmanrc` (`sdk env install`)
- `kubectl`, used once to read the platform's certificate authority

For the agent you also need a machine on your tailnet running Ollama with a
tool-capable model. Set `OLLAMA_BASE_URL` and `OLLAMA_MODEL` in `platform/.env`.

`spring-ai-agents-md` has not been published to Maven Central yet, so install
it once:

```bash
git clone https://github.com/dashaun/spring-ai-agents-md.git
cd spring-ai-agents-md && ./mvnw -DskipTests install
```

## Run the whole thing

```bash
make up
```

That generates credentials, brings up the cluster, creates the org and space,
pushes Vault, seeds it, pushes the config server, creates the service
instance, and pushes the three applications. On a cold cluster it takes a
while; on a warm one it takes a few minutes.

Individual steps are separate targets, and every one is safe to re-run:

```bash
make platform-init    # generate platform/.env
make cluster-up       # kind + Cloud Foundry, then log in
make org              # create the golden org and dev space
make vault            # push Vault and seed the demo secrets
make config-server    # build and push the config server
make service          # create the golden-config service instance
make apps             # build and push the three applications
```

## Develop against it

```bash
bin/dev-bind.sh              # the platform hands your laptop the binding
bin/run-local.sh greeting-service
curl localhost:8080/
```

`bin/dev-bind.sh` uses the identity you already have from `cf login` to read
the credentials the platform is holding. It writes them, and the platform's
certificate authority, to `bindings/`, which is git-ignored and mode 600.
`bin/dev-unbind.sh` removes every trace.

In an IDE, set one environment variable on the run configuration:

```
SERVICE_BINDING_ROOT=/path/to/golden/bindings
```

## Rotate a secret

```bash
make rotate
```

Writes a new value into Vault, asks the running application to refresh, and
shows the fingerprint changing. No rebuild, no redeploy, no developer.

## The slides

```bash
make slides    # http://localhost:8000
```

Reveal.js, with the sections in `docs/*.md` and the order set by
`docs/index.html`. Press `S` for speaker notes, `Esc` for the overview, and
open `?print-pdf` to export a handout.

## Layout

| Path | What it is |
| :--- | :--- |
| `docs/` | the presentation |
| `config-repo/` | settings the config server serves, safe to read in public |
| `libs/golden-binding/` | reads one service binding, wherever it lives |
| `apps/config-server/` | Spring Cloud Config Server, Vault plus git backends |
| `apps/greeting-service/` | an ordinary application on the golden path |
| `apps/code-mcp-server/` | the only door an agent has into this repository |
| `apps/steward-agent/` | Spring AI agent, deployed like anything else |
| `platform/scripts/` | everything the platform operator runs |
| `bin/` | everything a developer runs |

## Tear it down

```bash
make down
```

Deletes the `golden` org and everything in it, and removes local credentials.
`platform/.env` is left alone; delete it yourself for a clean slate.
