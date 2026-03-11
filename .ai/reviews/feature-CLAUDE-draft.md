# Feature module guidance

This file contains guidance for work inside `feature/`.

## Why this area matters

`feature/` contains 25 product feature modules. This is the most actively
developed area of the codebase. Each module is a Kotlin Multiplatform (KMP)
shared module using Compose Multiplatform (CMP) for UI.

> **Count note:** `settings.gradle.kts` includes 26 feature entries but
> `:feature:invoices` is listed twice (duplicate). Filesystem confirms 25
> unique modules.

## Feature modules

Confirmed from `settings.gradle.kts` and `feature/` filesystem:

| Module | Gradle path |
|--------|-------------|
| accounts | `:feature:accounts` |
| auth | `:feature:auth` |
| beneficiary | `:feature:beneficiary` |
| editpassword | `:feature:editpassword` |
| faq | `:feature:faq` |
| fast-mpay | `:feature:fast-mpay` |
| finance | `:feature:finance` |
| history | `:feature:history` |
| home | `:feature:home` |
| invoices | `:feature:invoices` |
| kyc | `:feature:kyc` |
| merchants | `:feature:merchants` |
| mpay-qr | `:feature:mpay-qr` |
| mpay-qr-scan | `:feature:mpay-qr-scan` |
| notification | `:feature:notification` |
| passcode | `:feature:passcode` |
| payments | `:feature:payments` |
| profile | `:feature:profile` |
| receipt | `:feature:receipt` |
| savedcards | `:feature:savedcards` |
| settings | `:feature:settings` |
| standing-instruction | `:feature:standing-instruction` |
| transfer-interbank | `:feature:transfer-interbank` |
| transfer-intrabank | `:feature:transfer-intrabank` |
| upi-setup | `:feature:upi-setup` |

## Standard module structure

Confirmed from `feature/auth/src/`:

```
feature/<name>/
├── build.gradle.kts
└── src/
    ├── commonMain/   — shared KMP code (UI, ViewModels, domain interaction)
    ├── androidMain/  — Android-specific overrides (if any)
    └── commonTest/   — shared unit tests
```

Other modules may also have `iosMain` or `desktopMain` source sets. `auth`
has `androidMain`, `commonMain`, and `commonTest` confirmed.

## Convention plugin

Every feature module applies the CMP feature convention:

```kotlin
alias(libs.plugins.cmp.feature.convention)  // id: org.convention.cmp.feature
```

This plugin automatically wires the following dependencies into every feature
module (confirmed from `build-logic/convention/src/main/kotlin/CMPFeatureConventionPlugin.kt`):

| Dependency | Purpose |
|------------|---------|
| `core:ui` | Shared UI components |
| `core:designsystem` | Design tokens, theme |
| `core:data` | Data access layer |
| Koin Compose | Dependency injection |
| Jetbrains ViewModel + Lifecycle | State management |
| Jetbrains Compose Navigation | In-app navigation |
| Kotlin Serialization | JSON/data serialization |
| `mifos.authenticator.passcode` | Passcode auth library |
| `mifos.authenticator.biometrics` | Biometrics auth library |

Do not manually re-add these in a feature's `build.gradle.kts` unless adding
platform-specific extras beyond what the plugin provides.

## High-risk feature modules

Treat changes to these modules as high risk. Follow the full Research → Plan
→ Implement sequence before editing:

- `feature/auth` — login, session creation, token issuance and storage
- `feature/passcode` — passcode entry, verification, biometrics integration
- `feature/payments` — payment initiation and confirmation
- `feature/kyc` — identity verification flow
- `feature/history` — transaction record display (source of truth for user)
- `feature/transfer-intrabank` and `feature/transfer-interbank` — fund movement

## Blast radius rules

- Any change to `core:data` or `core:model` is consumed by every feature module
  directly through the convention plugin. Before editing those modules, trace
  all 25 feature consumers.
- Any change to `core:domain` reaches every feature module transitively via
  `core:data`. Before editing it, trace all 25 feature consumers.
- Any change to `core:ui` or `core:designsystem` affects every feature's
  visual surface across all four platform targets.
- Any change to `cmp-shared/` (where all features are assembled) affects
  Android, iOS, Desktop, and Web simultaneously.
- A change to the `cmp.feature.convention` plugin in `build-logic/` affects
  every feature module in the repo.

## Required workflow

For any non-trivial change to a feature module:

1. Research — identify the exact UI → ViewModel → domain → data chain
2. Plan — list every file expected to change
3. Implement — smallest safe patch only

Do not mix feature behavior changes with unrelated cleanup.

## Avoid

- Adding dependencies the convention plugin already provides
- Bypassing `core:data` for platform-specific data workarounds
- Changing feature navigation contracts (route arguments, result types) without
  tracing all callers in `cmp-shared`
- Mixing UI layer changes with auth, payment, or KYC behavior changes
- Assuming a change is single-surface when features are composed into
  `cmp-shared` for all targets
