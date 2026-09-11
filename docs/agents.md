<!-- .slide: data-background-color="#191e1e" -->

## Now put an agent in the room

<p class="big">Every argument I just made about laptops applies to agents, and the stakes are higher.</p>

Notes:
An agent is a process that reads your code, runs your tools, and talks to a
model. If it runs on your laptop, it inherits your laptop: your environment,
your shell, your credentials, your whole home directory.

---

## What an agent on your laptop can reach

<div class="ledger danger">
<div><strong>Your environment</strong><p>Every exported variable, including the ones from step 14 of the onboarding doc.</p></div>
<div><strong>Your filesystem</strong><p>Every repository, every notes file, every downloaded credential.</p></div>
<div><strong>Your network</strong><p>Everything your VPN reaches, with your identity.</p></div>
</div>

Notes:
This is not an argument against agents. It is an argument against the default
place we run them. Nobody would accept this blast radius for a service. We
accept it for agents because they start as a terminal command.

---

## Move the agent to the platform

<div class="path">
<b>agent on Cloud Foundry</b><i>&rarr;</i><b>MCP server</b><i>&rarr;</i><b>the code</b>
</div>

- The agent runs in a container, with the container's identity
- It reaches the repository through one MCP server and nothing else
- It gets its configuration the same way every other application does
- The secrets it can read are the secrets it was bound to, which are none

Notes:
Same platform, same binding, same push. The agent is not a special kind of
thing. It is an application with an unusual appetite, and the platform already
knows how to constrain applications.

---

## The MCP server is the whole door

```java
@Tool(description = "Read one text file from the repository.")
public String readFile(String path) {
    Path target = resolve(path);          // rejects anything outside the root
    if (!hasAllowedExtension(target)) {
        return "Refused: this server only reads " + allowed;
    }
    if (Files.size(target) > properties.maxFileBytes()) {
        return "Refused: over the size limit";
    }
    return Files.readString(target, UTF_8);
}
```

<p class="small">List, read, search. No write tool. No shell tool. One root.</p>

Notes:
Three tools, about a hundred lines. The important part is what is missing. No
write, no exec, no network fetch. If the model asks for something outside the
root, `resolve` throws before a file is opened.

---

## And its limits are configuration

```yaml
golden:
  mcp:
    max-file-bytes: 262144
    allowed-extensions: "java,xml,yml,yaml,md,properties,sh,txt,json"
    max-results: 200
```

<p class="big">Reviewable in a pull request, changeable without a deploy.</p>

Notes:
This comes from the same config server as everything else. Which means the
security posture of the agent's access is a diff someone approved, not a flag
someone remembered to pass.

---

## The agent itself

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL}       # a model on the tailnet
      chat.options.model: muse-glimmer:30b-mlx
    mcp:
      client:
        streamable-http:
          connections:
            golden-code:
              url: http://code-mcp-server.apps.internal:8080
```

- Spring AI 2.0, pushed like anything else
- `AGENTS.md` instructions attached to every request
- An internal route, so the connection never leaves the platform

Notes:
Demo four. Ask the agent something that requires reading the repository, like
where secrets are allowed to live. It has to use the tools to answer. Then ask
it for a credential and watch it come back empty, because there is nothing to
find.

---

## What it is told about this repository

```markdown
- `config-repo/` holds settings that are safe to read in public.
  If a value belongs there, it is not a secret.
- Secrets live only in Vault and reach an application through the
  config server. Never suggest putting one in `config-repo/`, in a
  manifest, in a Dockerfile, or in an environment variable.
- Applications do not name the config server's host or credentials.
```

<p class="small">AGENTS.md, read by the platform, attached by <code>spring-ai-agents-md</code>.</p>

Notes:
The instructions live next to the code they describe and travel with it. When
the convention changes, the file changes, and every agent request after that
carries the new rule. No prompt buried in a wiki.
