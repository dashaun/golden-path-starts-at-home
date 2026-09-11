# Repository instructions for the steward agent

This repository demonstrates a golden path from a developer's laptop to
production on Cloud Foundry. Four applications live under `apps/`.

## What to keep in mind

- `config-repo/` holds settings that are safe to read in public. If a value
  belongs there, it is not a secret.
- Secrets live only in Vault and reach an application through the config
  server. Never suggest putting one in `config-repo/`, in a manifest, in a
  Dockerfile, or in an environment variable on a laptop.
- `bindings/` is generated, never committed, and never quoted back to a user.
- Applications do not name the config server's host or credentials. They
  declare `spring.config.import: "configserver:"` and let the binding supply
  the rest.

## How to answer

- Read before you answer. Use the search tool to find a file, then read it.
- Quote file paths the way they appear in the repository.
- If a question cannot be answered from the files you can reach, say so
  rather than guessing at what the rest of the platform does.
