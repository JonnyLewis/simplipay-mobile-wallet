---
scope: libs/
features: []
last-synced: bc52526c
budget: 150
---

# libs — vendored libraries

Read after the root [AGENTS.md](../AGENTS.md). Vendored (in-repo) library modules, included via `settings.gradle.kts`.

## What exists

- `mifos-passcode/` — vendored legacy passcode/lock-screen KMP module (`org.mifos.library.passcode.*`): `PassCodeScreen`, `PasscodeNavigation`, `PasscodeViewModel`, Koin modules (`PasscodeModule`, `PreferenceModule`), proto-backed `PasscodePreferencesDataSource`/`PasscodeManager`, plus its own components and theme.

## Invariants and gotchas — passcode migration in flight

Three passcode artifacts currently coexist (state as of MW-346, commit `0403be73`):

1. `libs/mifos-passcode` — this vendored legacy module, **still wired in**: included in `settings.gradle.kts` and consumed by `cmp-shared/build.gradle.kts` (`projects.libs.mifosPasscode`).
2. `feature/passcode` — the passcode feature module (`projects.feature.passcode`).
3. `io.github.openmf:mifos-authenticator-passcode:2.0.6` — the **new external** module MW-346 added via `CMPFeatureConventionPlugin` (`gradle/libs.versions.toml`).

Consequences:

- Do **not** extend this legacy module — new passcode work targets the external `mifos-authenticator-passcode` path. If a task seems to require changes here, surface it as a question first.
- Passcode/app-lock is on the high-risk list (`risk/AGENTS.md`): auth/session behavior — call out impact before editing anything here.
- Whether `libs/mifos-passcode` can be removed is an open question; do not delete or unwire it without a founder decision.

## Feature map

| Feature | What it added here |
|---|---|
| — | none tracked yet (MW-346 predates feature tracking) |
