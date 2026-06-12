---
scope: core-base/
features: []
last-synced: bc52526c
budget: 150
---

# core-base — platform foundation modules

Read after the root [AGENTS.md](../AGENTS.md). One level below [`core/`](../core/AGENTS.md): reusable KMP foundations with no wallet domain knowledge. Each module carries its own detailed README; this file is orientation only.

## What exists

| Module | Role |
|---|---|
| `analytics` | Analytics abstraction layer |
| `common` | Base utilities and shared primitives |
| `database` | Room-based cross-platform database abstractions (Android / Desktop / Native) |
| `datastore` | **Deprecated** key-value storage built on Multiplatform Settings — its README says use the settings library directly; it "will be removed soon" |
| `designsystem` | "KPT Design System" — Material3-based components, theming, layout primitives |
| `network` | Ktorfit + HTTP client setup with Result wrappers for suspend and Flow return types |
| `platform` | Platform-specific glue (expect/actual) |
| `ui` | Cross-platform UI foundations |

## Consumers

Consumed broadly across the repo: `core/*` (e.g. `core/network`, `core/designsystem`), `cmp-shared/`, and many `feature/*` modules (auth, passcode, fast-mpay, mpay-qr-scan, ...). Public surfaces here are shared contracts (ED-2): trace consumers and state the blast radius before changing anything exported.

## Invariants and gotchas

- Mixed package namespaces from template heritage: most modules use `template.core.base.*`, but `network` uses `org.mifos.corebase.*`. Match the namespace of the module you touch; do not rename wholesale (ED-3).
- `datastore` is explicitly deprecated in its own README — do not add new dependencies on it; prefer Multiplatform Settings directly.
- Some READMEs are stale (`common/README.md` titles itself `:core:common`). Trust code over README.

## Do NOT recreate

- Result-wrapper response handling for network calls — `core-base/network` (see its README).
- UI components / theming — `core-base/designsystem`; check there before adding components in feature modules.

## Feature map

| Feature | What it added here |
|---|---|
| — | none tracked yet |
