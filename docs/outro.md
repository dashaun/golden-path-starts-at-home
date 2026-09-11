## What this actually bought

<table>
<thead><tr><th></th><th>Before</th><th>After</th></tr></thead>
<tbody>
<tr><td>Who holds a credential</td><td>Every developer, every laptop</td><td>One application, one operator</td></tr>
<tr><td>Onboarding</td><td>git clone, then a doc</td><td>git clone, then one command</td></tr>
<tr><td>Rotation</td><td>An announcement and a hope</td><td>A write and a refresh</td></tr>
<tr><td>Agents</td><td>Your whole machine</td><td>One read-only door</td></tr>
</tbody>
</table>

Notes:
Do not claim the credential problem is solved. Claim the population holding
credentials got small enough to manage, and that the laptop stopped being a
special case.

---

<!-- .slide: data-background-color="#6db33f" -->

## The thing to take home

<p class="refrain">Ask for <span>a</span> config server.<br>Never <span>the</span> config server.</p>

Notes:
If an application names a host, you will end up with a file per environment.
If it names a type, the platform can answer differently everywhere and the
artifact never changes. That is the whole idea, and everything else today was
mechanics.

---

## Try it yourself

```bash
git clone https://github.com/dashaun/golden.git
cd golden
make up          # kind, Cloud Foundry, Vault, config server, apps
bin/dev-bind.sh  # your laptop joins the golden path
bin/run-local.sh greeting-service
```

<p class="small">Everything in this talk runs on one laptop. No cloud account required.</p>

Notes:
Leave this slide up during questions. Mention that `make down` removes the org
and everything in it and leaves the rest of the cluster alone.

---

<!-- .slide: data-background-color="#191e1e" -->

## Thank you

<p class="big">DaShaun Carter</p>

<p class="small">github.com/dashaun</p>

Notes:
Questions.
