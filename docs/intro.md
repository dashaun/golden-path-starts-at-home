<!-- .slide: data-background-color="#191e1e" -->
<div class="cover">

# The Golden Path Starts at Home

### Engineering developer experience from laptop to production

<div class="rule"></div>

<p class="byline">DaShaun Carter</p>

</div>

Notes:
This is the second half. The first half was about making local development
feel like production. I am picking it up at the hardest part of that promise:
the secret.

---

<!-- .slide: data-background-color="#6db33f" -->

## Where we left off

<p class="big">Everything in the golden path is easy to share, except the one thing you must not share.</p>

Notes:
Linting, formatting, container images, dev containers: all of it can go in the
repository. Credentials cannot. So that is where the golden path usually
breaks, and it breaks first on the laptop.

---

## What I want to show you

<div class="path">
<b>git clone</b><i>&rarr;</i><b>bind</b><i>&rarr;</i><b>run in your IDE</b><i>&rarr;</i><b>cf push</b><i>&rarr;</i><b>production</b>
</div>

- The same application, unchanged, in all four places
- No secret on the laptop, in the repository, or in a shell
- A secret rotated in one place, live in a running application seconds later
- An agent that can read the code and still cannot reach the secrets

Notes:
Four demos. One repository. Everything I run today runs on a kind cluster on
this laptop, using the real Cloud Foundry deployment, so nothing here depends
on a cloud account.
