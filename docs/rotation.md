## Rotation is the real test

<p class="big">If you cannot rotate it on a Tuesday afternoon, it is not managed.</p>

- Who has a copy?
- What breaks when it changes?
- How long until every running process has the new value?

Notes:
This is the question I would ask any platform team. Not "where are your
secrets stored" but "rotate one right now, with me watching". The first two
questions are unanswerable when credentials spread through env files.

---

## Rotate, without a deploy

```bash
make rotate
```

1. Write a new value into Vault
2. `POST /actuator/refresh` on the running application
3. The fingerprint changes

<p class="big">No rebuild. No redeploy. No developer involved.</p>

Notes:
Demo three. Show the fingerprint endpoint before and after. The endpoint
returns a masked value on purpose, so I can prove it changed without putting
a live credential on a projector. Mention @RefreshScope is what makes the
bean pick it up.

---

## What the application exposes

```java
@GetMapping("/secret-fingerprint")
Map<String, Object> fingerprint() {
    String key = partnerApi.key();
    return Map.of(
            "present", key != null && !key.isBlank(),
            "length", key == null ? 0 : key.length(),
            "fingerprint", mask(key));
}
```

<p class="small">Proving a secret is live without showing it is a habit worth keeping.</p>

Notes:
Small thing, but people copy what they see in talks. Never print a credential
to demonstrate that it arrived. Print something derived from it.
