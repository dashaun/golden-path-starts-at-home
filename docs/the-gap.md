## The gap that is left

<div class="ledger danger">
<div><strong>In the repo</strong><p>Settings, defaults, feature flags.</p><p>Reviewable. Diffable. Fine.</p></div>
<div><strong>On the laptop</strong><p>A <code>.env</code> file someone sent you.</p><p>Exported in a shell. Pasted in Slack.</p></div>
<div><strong>In production</strong><p>Something else entirely.</p><p>Rotated on a schedule nobody local knows about.</p></div>
</div>

Notes:
The third column is the tell. When production gets its credentials one way and
the laptop gets them another way, the laptop is not on the golden path. It is
on a parallel path that happens to work.

---

## How the laptop usually gets a secret

```bash
# Someone's onboarding doc, step 14
export PARTNER_API_KEY=pk_live_9f2c41d8ab77e05b
export PARTNER_API_TENANT=northwind
```

- Now it is in your shell history
- Now it is in every process you start, including your editor
- Now it is in every tool your editor starts
- Nobody can rotate it, because nobody knows who has it

Notes:
Ask the room how many have a .env file with a real production-adjacent
credential in it right now. Then point out the last bullet. Rotation is the
thing that actually fails. You cannot rotate what you cannot account for.

---

<!-- .slide: data-background-color="#191e1e" -->

## The rule I want to hold

<p class="big">An application should ask for <strong>a</strong> config server, never <strong>the</strong> config server.</p>

Notes:
That one sentence is the whole talk. If the application names a host, it has
an opinion about which environment it is in, and now you need one build per
environment or one file per environment. If it only names a kind of thing, the
platform can answer differently in every environment and the artifact never
changes.

---

## What the application says

```yaml
spring:
  application:
    name: greeting-service
  config:
    import: "configserver:"
```

<p class="big">That is the entire configuration story in the application.</p>

No host. No user. No password. No profile per environment.

Notes:
Show this in the editor. `spring.config.import: configserver:` with nothing
after the colon is not a placeholder I am filling in later. It is the finished
form. Something else supplies the uri before this line is resolved.
