# The Golden Path Starts at Home

A conference talk and its working demo. Cloud Foundry on a local kind cluster,
with Vault and Spring Cloud Config Server on it, showing one application get
the same configuration and the same secret on a laptop, on `cf push`, and in
production.

## Architecture

- `docs/index.html` — Reveal.js entry point and slide ordering
- `docs/*.md` — presentation sections
- `docs/reveal.js/` — vendored Reveal.js distribution; do not edit
- `config-repo/` — what the config server serves from its native backend
- `libs/golden-binding/` — the only code that knows where a binding lives
- `apps/` — four Spring Boot applications, each with its own `manifest.yml`
- `platform/scripts/` — numbered, idempotent, run through the `Makefile`
- `bin/` — the two commands a developer runs

## Rules that are not negotiable

- No credential is ever committed. `bindings/` and `platform/.env` are
  git-ignored and must stay that way.
- `config-repo/` holds only values that are safe to read in public. Secrets go
  in Vault and reach applications through the config server.
- Applications never name the config server's host or credentials. They
  declare `spring.config.import: "configserver:"` and nothing else.
- Demo endpoints never print a live secret. Print something derived from it,
  the way `/secret-fingerprint` does.

## Spring Boot lines

Two, on purpose, because the config server and the agent do not move on the
same schedule:

| Module | Spring Boot | Also |
| :--- | :--- | :--- |
| `golden-binding`, `config-server`, `greeting-service` | 4.0.8 | Spring Cloud 2025.1.3 |
| `code-mcp-server`, `steward-agent` | 4.1.1 | Spring AI 2.0.1 |

The root `pom.xml` aggregates but does not parent, so each module keeps its
own line. `golden-binding` is built against 4.0.8 and used by both.

## Things that will bite you

- Spring Boot 4 discovers `EnvironmentPostProcessor` through
  `META-INF/spring.factories`, not a `.imports` file.
- `spring-vault-core` is an optional dependency of the config server. Without
  it the composite backend fails with a null pointer at startup.
- Cloud Foundry uses the port a Docker image exposes. Vault exposes 8200, so
  the listener and every policy have to agree on 8200.
- Network policies must exist before a consuming application starts, because
  the config client is configured to fail fast.

## Reveal.js markdown

- `---` creates a vertical slide within a section.
- `Notes:` starts presenter notes.
- Slide attributes go in a leading `<!-- .slide: ... -->` comment.
- Full-bleed statement slides use `data-background-color` with `#6db33f` or
  `#191e1e`.

## Visual system

Inherited from the other decks in this series. All styling lives in the
`<style>` block of `docs/index.html`; the Markdown stays close to plain prose.

- Primary green `#6db33f`, deep green `#4e8c2a`, dark `#191e1e`, gold `#f2b134`
- Work Sans headings, Open Sans body, JetBrains Mono code
- Reusable classes: `.cover`, `.kicker`, `.big`, `.refrain`, `.small`,
  `.path`, `.split`, `.ledger` (add `.danger` for the red variant)
- Keep audience-facing copy short and put delivery guidance in notes.

## Narrative

1. The golden path breaks on credentials, and it breaks first on the laptop.
2. A binding is a contract: name, type, credentials. Ask for a type.
3. The platform answers differently in every environment; the artifact does not change.
4. Rotation is the test of whether secrets are actually managed.
5. Agents deserve the same constraint, and the platform already knows how.
