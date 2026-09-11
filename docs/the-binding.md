## A binding is a contract

<div class="ledger">
<div><strong>Name</strong><p>What to call it.</p><p><code>golden-config</code></p></div>
<div><strong>Type</strong><p>What kind of thing it is.</p><p><code>config</code></p></div>
<div><strong>Credentials</strong><p>How to reach it and prove who you are.</p><p><code>uri</code>, <code>username</code>, <code>password</code></p></div>
</div>

<p class="big">The application asks for a type. The platform picks the instance.</p>

Notes:
This is not a Spring idea or a Cloud Foundry idea. It is the same contract as
the Kubernetes service binding specification. Which is why the same
application can read it in three different places without noticing.

---

## The same binding, three environments

<div class="split">
<div>

### On a laptop

```text
bindings/golden-config/
  type      -> config
  uri       -> https://config-server...
  username  -> golden
  password  -> ...
```

</div>
<div>

### On Cloud Foundry

```json
{ "user-provided": [{
    "name": "golden-config",
    "tags": ["config"],
    "credentials": {
      "uri": "http://config-server...",
      "username": "golden",
      "password": "..." }}]}
```

</div>
</div>

<p class="small">Kubernetes uses the directory form. Cloud Foundry uses the JSON form. Same three keys.</p>

Notes:
Left is a directory of files, which is what the service binding spec says and
what a laptop can hold. Right is what Cloud Foundry puts in VCAP_SERVICES when
you bind a user-provided service with the tag "config". Point at the tag. The
tag is the type.

---

## The only code that knows the difference

```java
Optional<ServiceBinding> binding = sources.stream()
        .flatMap(source -> source.bindings(environment).stream())
        .filter(candidate -> BINDING_TYPE.equalsIgnoreCase(candidate.type()))
        .findFirst();

put(properties, "spring.cloud.config.uri", found.get("uri"));
put(properties, "spring.cloud.config.username", found.get("username"));
put(properties, "spring.cloud.config.password", found.get("password"));

environment.getPropertySources().addFirst(
        new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
```

<p class="small">An <code>EnvironmentPostProcessor</code>, ordered just before config data import.</p>

Notes:
Two sources: a directory and a JSON blob. One filter on type. Three
properties. That is the entire bridge between a laptop and a platform. Spring
Cloud Bindings does the directory half for you if you would rather not own
this; I am showing it because it is small enough to read out loud.

---

## Binding a laptop

```bash
bin/dev-bind.sh
```

- Uses the identity you already have from `cf login`
- Reads the credentials the platform is holding, not ones you were sent
- Writes them to a git-ignored directory, mode 600
- `bin/dev-unbind.sh` removes every trace

<p class="big">Nothing was typed. Nothing was stored in the repository. Nothing went into your shell.</p>

Notes:
Demo one. Run dev-bind, show the files, show that bindings/ is in .gitignore,
then start the application from the IDE with one environment variable:
SERVICE_BINDING_ROOT. Show the greeting endpoint returning a value that came
from Vault by way of the config server.

---

<!-- .slide: data-background-color="#6db33f" -->

## Then push the same thing

<p class="big">cf push</p>

The jar does not change. The manifest names the service, not the server.

Notes:
Demo two. Push the same artifact. The manifest has `services: [golden-config]`
and nothing else about configuration. Same endpoint, same value, different
place. Nothing rebuilt.
