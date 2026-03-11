# SimpliPay Mobile Wallet

This repository contains the SimpliPay mobile wallet codebase.

It is a brownfield, multi-surface codebase derived from the Mifos Pay / mobile-wallet structure and extended for SimpliPay-specific product needs. Treat this repo as a production financial application with shared contracts, cross-surface blast radius, legacy constraints, and platform-specific behavior.

> **Codebase identity note:** The Gradle root project is named `mobile-wallet`.
> The Android namespace and applicationId are `org.mifospay`. Copyright headers
> read "Mifos Initiative". These identifiers are from the upstream Mifos Pay
> origin. When referencing package names, namespaces, or app identifiers in
> tasks, use the values found in the codebase, not the product display name.

## Repo shape

This is not a simple single-app mobile repo.

Key areas in this repository include:
- `cmp-shared/` for shared cross-platform UI composition and app assembly
- `cmp-android/`, `cmp-ios/`, `cmp-web/`, `cmp-desktop/` for platform/surface entry points
- `feature/` for product feature modules (25 modules)
- `core/` for domain, data, model, network, analytics, datastore, UI, and common foundations
- `core-base/` for lower-level platform abstractions used by `core/`: analytics,
  common, database, datastore, designsystem, network, platform, ui
  (distinct from `core/` — see `core/CLAUDE.md`)
- `android/` and `ios/` — these directories currently contain only their own
  CLAUDE.md files. The real platform shells are `cmp-android/` and `cmp-ios/`.
- `build-logic/` for Gradle conventions and shared build behavior
- `scripts/` for iOS deployment and verification scripts
- `libs/mifos-passcode/` for the embedded passcode library
- `fastlane-config/` for deployment configuration (`project_config.rb`,
  `android_config.rb`, `ios_config.rb`)
- `docs/`, `config/`, `fastlane/`, and supporting repo infrastructure

## Tech stack

This is a Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) codebase.

Confirmed from build files and convention plugins:
- **Language:** Kotlin (multiplatform)
- **UI:** Compose Multiplatform (Jetbrains Compose)
- **DI:** Koin (`org.convention.kmp.koin` convention plugin)
- **Navigation:** Jetbrains Compose Navigation
- **Network:** Ktorfit over Ktor (not Retrofit), Kotlinx Serialization
- **Versioning:** `org.ajoberstar.reckon` (configured in `settings.gradle.kts`)
- **Static analysis:** Detekt, Spotless, ktlint, dependencyGuard (enforced in CI)
- **Platforms:** Android, iOS, Desktop, Web

The `cmp.feature.convention` plugin wires Koin, Navigation, and core
dependencies into every feature module automatically. See `feature/CLAUDE.md`
and `build-logic/CLAUDE.md` for details.

## Product and safety context

This is a financial app. Correctness, traceability, reversibility, and preserving user trust matter more than speed or elegance.

Typical domains in this repo may include:
- authentication and session management
- passcode / OTP / biometrics
- wallet and balances
- transfers and payments
- transaction history and receipts
- beneficiaries, accounts, cards, invoices, notifications, profile, and settings
- shared API contracts and shared business logic reused across surfaces

Because this is a financial app:
- do not invent backend behavior
- do not invent transaction states
- do not silently change auth/session behavior
- do not assume displayed state is the same as authoritative backend state
- do not casually change shared contracts used across multiple surfaces

## Primary operating mode

For any non-trivial task, always follow this sequence:

1. Research
2. Plan
3. Implement

Do not jump straight into implementation for medium or large tasks.

### Research
During research:
- do not change code
- identify the exact user flow or system behavior involved
- identify the exact entry point(s)
- trace the path through UI, state holder, domain, data, network, storage, mapping, and side effects
- identify shared-code impact and surface-specific impact
- identify confirmed facts, inferences, and unknowns
- identify likely regression risks

Write research to:
- `.ai/research/<task-name>.md`

### Plan
During planning:
- list the exact files expected to change
- describe intended behavior change
- describe behavior that must not change
- note cross-surface blast radius
- note validation to run
- note rollback or follow-up risk

Write the plan to:
- `.ai/plans/<task-name>.md`

### Implement
During implementation:
- implement only the approved plan
- make the smallest safe patch possible
- preserve behavior outside the requested scope
- if new findings invalidate the plan, stop and update the plan first
- summarize what changed, what did not change, and what was validated

## Evidence rules

Research must reference exact file paths, classes, functions, routes, screens, models, mappers, plugins, or scripts where possible.

Every research note must clearly separate:
- Confirmed facts
- Inferences
- Unknowns

Plans must list exact files before implementation begins.

If something is inferred rather than confirmed from code, say so explicitly.

Do not invent:
- APIs
- DTO fields
- routes
- feature flags
- transaction states
- storage keys
- Gradle behavior
- surface-specific behavior
- validation assumptions

## Shared-code rules

If the task touches shared code:
- assume multiple surfaces may be affected
- trace all known consumers before changing contracts
- explicitly state blast radius
- identify whether the change affects model shape, serialization, state semantics, or UI expectations

## High-risk areas

Treat these as high risk and call out impact before editing:
- auth, passcode, OTP, biometrics, session restore, session refresh
- wallet balance source of truth
- payments, transfers, top-up, cash-in, cash-out
- transaction history, receipts, and status mapping
- shared models, DTOs, serializers, and mappers
- network request/response contracts
- token handling, secure storage, encryption
- retry behavior, idempotency, duplicate prevention
- background work, sync, and pending-operation handling
- amount formatting, fee handling, rounding
- Gradle conventions, signing, release, and automation scripts

## Definition of done

A task is complete only when:
- the exact files involved were identified
- the requested change was implemented with the smallest safe patch
- unrelated refactoring was not mixed in unless explicitly requested
- relevant validation was run and summarized
- shared-contract and cross-surface impact was called out where relevant
- risks and unvalidated areas were clearly stated
- reusable repo truth discovered during the task was written back into docs or reusable skills when appropriate

## Expected output style

When working on a task:
- be explicit about what files are involved
- call out shared-code impact clearly
- keep summaries concise and structured
- state assumptions clearly
- state residual risks clearly
- avoid dumping large logs unless something failed
