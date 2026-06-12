# Core module guidance

This file contains guidance for work inside `core/`.

## Why this area matters

`core/` is shared infrastructure with broad downstream impact. In this repo, core modules include areas such as data, domain, datastore, designsystem, UI, common, network, model, and analytics.

Changes here can affect many feature modules and multiple surfaces.

## Core rule

Treat `core/` changes as high-blast-radius by default.

Before editing code in `core/`, identify:
- all directly affected modules
- downstream feature consumers
- whether the change affects data contracts, domain behavior, model shape, network behavior, persistence, analytics, or shared UI primitives

## Module responsibilities

Use these distinctions when reasoning about changes:
- `core:domain` should remain focused on shared business/domain behavior
- `core:data` should remain focused on shared data orchestration and repository-like data access
- `core:network` should remain focused on transport/API/network concerns
- `core:model` should remain focused on model shape and shared types
- `core:datastore` should remain focused on stored/shared state
- `core:analytics` should remain focused on instrumentation
- `core:ui` and `core:designsystem` should remain focused on reusable UI foundations

Do not blur these boundaries casually.

## Special caution

When changing `core:data`, `core:domain`, `core:network`, or `core:model`:
- trace all known consumers first
- do not casually alter serialization or mapping behavior
- do not casually alter defaults or null handling
- do not casually change state semantics used by feature/UI layers
- do not casually change retry or error-mapping behavior

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