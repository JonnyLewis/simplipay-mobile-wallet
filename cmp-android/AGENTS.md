---
scope: cmp-android/
features: []
last-synced: bc52526c
budget: 150
---

# cmp-android — Android app entry point

Read after the root [AGENTS.md](../AGENTS.md) and the Android platform guidance in [`android/AGENTS.md`](../android/AGENTS.md), whose rules (signing, build types, Google services, shared-vs-shell separation) all apply here.

## What exists

- `src/main/kotlin/org/mifospay/MainActivity.kt` — single activity hosting the shared Compose UI.
- `src/main/kotlin/org/mifospay/MifosPayApp.kt` — Android `Application` class.
- `src/main/AndroidManifest.xml`, launcher icons, splash resources, `provider_paths.xml`.
- `build.gradle.kts` — Android application conventions, flavors, Google services, KSP; depends on `projects.cmpShared`, `projects.core.data`, `projects.core.ui`.
- `google-services.json`, `proguard-rules.pro`, `lint-baseline.xml`, `dependencies/` (dependency-tree snapshots checked in and updated by tooling).

## Invariants and gotchas

- This is a thin shell: app behavior lives in [`cmp-shared/`](../cmp-shared/AGENTS.md). Do not add business logic here.
- MW-346 (`0403be73`) routed passcode/app-lock through `MainActivity` wiring — passcode is high-risk (see `risk/AGENTS.md`); call out impact before touching startup or lock flows.
- `dependencies/*.txt` files are generated dependency-guard baselines — expect them to churn when dependencies change; do not hand-edit.

## Feature map

| Feature | What it added here |
|---|---|
| — | none tracked yet |
