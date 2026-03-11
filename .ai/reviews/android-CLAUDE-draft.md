# Android platform guidance

This file contains guidance for work inside Android-specific platform areas.

> **Directory status:** The `android/` directory currently contains only this
> CLAUDE.md file. There is no Android application source code here.
> The Android application shell is in `cmp-android/`.

## Scope

Use this guidance when working in:
- `cmp-android/` — the real Android application shell
- Android-specific code inside shared modules
- Android-specific build, signing, or runtime behavior

(`android/` itself has no source code and is not an active work area.)

## Why this area matters

Android in this repo is not just UI. It includes app-shell behavior, runtime configuration, signing, platform integrations, Android-specific dependencies, and Android entry-point concerns.

Changes here can affect:
- app startup
- build variants
- signing
- navigation
- credentials integration
- splash / lifecycle behavior
- Android-specific auth flows
- release packaging

## Known upstream facts

The upstream Android app shell is `cmp-android`, uses Android application conventions plus Compose, flavors, Google services, KSP, and depends on `projects.cmpShared`, `projects.core.data`, and `projects.core.ui`.

## Known facts: cmp-android

Confirmed from `cmp-android/build.gradle.kts` and `build-logic/convention/src/main/kotlin/AppFlavor.kt`:

- **Namespace / applicationId:** `org.mifospay`
- **Build types:** `debug` (applicationId suffix `.debug`), `release`
- **Build flavors:** `prod` (no suffix), `demo` (applicationId suffix `.demo`)
  - Flavor dimension: `contentType`
- **Direct dependencies:** `projects.cmpShared`, `projects.core.data`, `projects.core.ui`
- **Signing env vars read at build time:**
  - `KEYSTORE_PATH`
  - `KEYSTORE_PASSWORD`
  - `KEYSTORE_ALIAS`
  - `KEYSTORE_ALIAS_PASSWORD`
- **ProGuard:** Currently disabled (`isMinifyEnabled = false`, `isShrinkResources = false`).
  Open tracking issue #1815. Do not re-enable without first resolving that issue.
- **Google services:** `google-services.json` is in `cmp-android/`

## Android rules

Before editing Android code, identify:
- whether the change belongs in `cmp-android` or shared code
- app entry point / startup path
- Android-specific dependencies involved
- whether signing/build type behavior is affected
- whether the change affects credentials, auth, or secure storage
- whether the change is actually shared logic that should not live only in Android

When working in Android-specific areas:
- keep Android shell logic separate from shared business logic
- do not move shared logic into Android-only code casually
- preserve build type and signing behavior unless the task explicitly targets it
- preserve runtime setup unless the task explicitly targets it
- be extra cautious with Google services, KSP-generated behavior, and release config

## Validation expectations

Prefer the narrowest useful checks first, for example:
- the affected Android module
- targeted Android tests
- targeted build variant
- lint / test / assemble only as needed

If the task affects signing, build types, or Google services setup, say so explicitly.

## Avoid

- casual changes to signing configuration
- casual changes to build types and release behavior
- duplicating shared logic in Android-only code
- mixing Android UI cleanup with auth/payment behavior changes
