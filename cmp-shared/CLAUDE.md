# Shared cross-platform guidance

This file contains guidance for work inside `cmp-shared/`.

## Why this area matters

`cmp-shared/` is a high-value shared surface assembly area. It pulls together shared dependencies and feature modules used across multiple targets. Changes here may affect Android, iOS, desktop, and web behavior.

Treat changes in this directory as medium-to-high blast radius by default.

## Core rule

Do not casually change shared contracts, shared dependencies, or app assembly behavior.

Before editing code in this area, identify:
- which surfaces consume the affected code
- which feature modules are involved
- which core modules are involved
- whether the change affects shared model shape, navigation, state assembly, or shared dependency wiring

## Required workflow

For non-trivial work in `cmp-shared/`:
- research first
- plan second
- implement third

Do not jump into coding until blast radius is understood.

## What to identify before editing

Before changing code in this area, identify:
- the owning feature or domain
- all directly referenced models, interfaces, and mappers
- whether the change affects `core:domain`, `core:data`, or `core:network`
- whether the code is used by multiple surfaces
- whether the code feeds wallet state, auth state, transaction state, or shared UI composition
- whether null/default handling is relied on downstream

## Shared-assembly rules

When changing code in `cmp-shared/`:
- assume multiple features may be composed from here
- trace all direct consumers before removing or changing shared references
- do not casually alter shared app composition or shared dependency wiring
- prefer additive changes over structural rewrites unless explicitly requested

## Validation expectations

When `cmp-shared/` changes:
- validate the narrowest affected target first
- call out all surfaces that may also be affected
- if model or contract shape changes, call that out explicitly even if only one surface was built locally

## Avoid

- casual changes to shared dependency wiring
- changing shared model semantics without tracing consumers
- mixing unrelated cleanup into shared behavior changes
- assuming a shared change is Android-only or iOS-only without proof