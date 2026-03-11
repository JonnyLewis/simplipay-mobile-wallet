# Core module guidance

This file contains guidance for work inside `core/`.

## Why this area matters

`core/` is shared infrastructure with broad downstream impact. In this repo, core modules include areas such as data, domain, datastore, designsystem, UI, common, network, model, and analytics.

Changes here can affect many feature modules and multiple surfaces.

## core/ vs core-base/

There are two parallel module groups in this repo. Do not confuse them.

**`core/`** — app-level KMP shared modules consumed directly by `feature/` modules
through the `cmp.feature.convention` plugin:

| Module | Purpose |
|--------|---------|
| `core:data` | Data orchestration and repository-layer access |
| `core:domain` | Shared business/domain logic |
| `core:model` | Shared model types and DTOs |
| `core:network` | Transport layer: Ktorfit, Ktor, Kotlinx Serialization |
| `core:datastore` | Stored/shared state (preferences, session) |
| `core:designsystem` | Design tokens, theme, shared UI primitives |
| `core:ui` | Reusable composable components |
| `core:common` | Shared utilities used across core modules |
| `core:analytics` | Instrumentation and event tracking |

**`core-base/`** — lower-level platform abstractions that `core/` modules may
build upon (confirmed from `settings.gradle.kts` and filesystem):

| Module | Notes |
|--------|-------|
| `core-base:common` | Low-level shared utilities |
| `core-base:database` | Database layer — the only database module in the repo |
| `core-base:datastore` | Low-level datastore abstractions |
| `core-base:network` | Low-level network abstractions |
| `core-base:designsystem` | Low-level design primitives |
| `core-base:platform` | Platform-specific abstractions |
| `core-base:ui` | Low-level UI foundations |
| `core-base:analytics` | Low-level analytics instrumentation |

If a task involves database access, trace it through `core-base:database`.
`core/` has no database module of its own.

## Core rule

Treat `core/` changes as high-blast-radius by default.

Before editing code in `core/`, identify:
- all directly affected modules
- downstream feature consumers (all 25 feature modules consume `core:data`,
  `core:ui`, and `core:designsystem` via the convention plugin)
- whether the change affects data contracts, domain behavior, model shape,
  network behavior, persistence, analytics, or shared UI primitives

## Module responsibilities

Use these distinctions when reasoning about changes:
- `core:domain` should remain focused on shared business/domain behavior
- `core:data` should remain focused on shared data orchestration and repository-like data access
- `core:network` should remain focused on transport/API/network concerns.
  Implementation uses Ktorfit over Ktor (not Retrofit) and Kotlinx Serialization.
  Applies the `kmp.supabase.config` convention plugin — Supabase is part of the
  network layer. Depends on `core:common`, `core:model`, `core:datastore`.
- `core:model` should remain focused on model shape and shared types
- `core:datastore` should remain focused on stored/shared state
- `core:analytics` should remain focused on instrumentation
- `core:ui` and `core:designsystem` should remain focused on reusable UI foundations

Do not blur these boundaries casually.

## Dependency injection

DI throughout `core/` and `feature/` is via **Koin**. The `kmp.koin` convention
plugin wires Koin into every KMP module. When tracing how a dependency is
provided or overridden, look for Koin module declarations.

## Special caution

When changing `core:data`, `core:domain`, `core:network`, or `core:model`:
- trace all known consumers first (all 25 feature modules depend on at least
  `core:data` through the convention plugin)
- do not casually alter serialization or mapping behavior
- do not casually alter defaults or null handling
- do not casually change state semantics used by feature/UI layers
- do not casually change retry or error-mapping behavior

When changing `core:network`:
- Ktorfit and Ktor are the transport layer — changes here are not the same as
  Retrofit and must be validated against Ktorfit-specific behavior
- The `kmp.supabase.config` plugin is applied to this module. Supabase
  integration details are not yet fully documented; treat any Supabase-adjacent
  changes as unverified until confirmed through code reading.

## Validation expectations

If `core/` changes:
- validate the narrowest direct consumer first
- state likely downstream consumers
- be explicit about unvalidated blast radius

## Avoid

- broad refactors during a bug fix
- changing shared contracts without consumer tracing
- moving logic across layer boundaries without a strong reason
- hidden behavior changes in mapping, defaults, retries, or persistence
- treating `core/` and `core-base/` as interchangeable
