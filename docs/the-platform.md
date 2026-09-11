## What is actually running

<div class="path">
<b>kind cluster</b><i>&rarr;</i><b>Cloud Foundry</b><i>&rarr;</i><b>Vault</b><i>&rarr;</i><b>Config Server</b><i>&rarr;</i><b>your apps</b>
</div>

- `cloudfoundry/kind-deployment`, unmodified, on this laptop
- Vault pushed as an application, reachable only on an internal route
- The config server is the one thing allowed to talk to Vault
- Everything else binds to the config server

Notes:
Worth saying out loud: this is a real Cloud Foundry, not a simulator. The
whole thing came from `make up` in the upstream project. Everything I added
lives in its own org and space and can be deleted without touching the rest
of the cluster.

---

## The config server's two backends

```yaml
spring:
  profiles:
    active: composite
  cloud:
    config:
      server:
        composite:
          - type: vault          # secrets, and they win on a collision
            host: golden-vault.apps.internal
            authentication: TOKEN
          - type: native         # settings, straight from git
            search-locations: classpath:/config-repo
```

<p class="big">One response. Two very different sources.</p>

Notes:
The application asks for its configuration once and gets one document back.
It has no idea part of it came from a git repository and part of it came from
Vault, and that is the point. The reviewable half stays reviewable and the
secret half stays secret.

---

## Who is allowed to talk to whom

```bash
cf add-network-policy config-server golden-vault --protocol tcp --port 8200
cf add-network-policy greeting-service config-server --protocol tcp --port 8080
```

- Vault has no public route in production
- One application can reach it, and it is not yours
- Your application's blast radius is the config it was given

Notes:
In the demo Vault does have a public route, because I have to seed it from
this laptop and I want to show you the UI. In production that route does not
exist. The internal route plus the network policy is the real control.

---

<!-- .slide: data-background-color="#191e1e" -->

## The one honest caveat

<p class="big">Somebody still holds the first credential.</p>

The config server needs a Vault token. That token belongs to the platform team, not to you.

Notes:
Do not oversell this. Turtles do not go all the way down. What changed is the
number of people and machines holding a credential: from every developer and
every laptop, to one application and one operator. That is the win, and it is
a big one, but say it plainly.
