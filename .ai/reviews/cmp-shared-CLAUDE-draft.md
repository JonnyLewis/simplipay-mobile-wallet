# Shared cross-platform guidance

This file contains guidance for work inside `cmp-shared/`.

## Why this area matters

`cmp-shared/` is the Compose Multiplatform assembly point for all four platform
targets: **Android, iOS, Desktop, and Web**. It pulls together all feature
modules and core modules into a single shared module consumed by each platform.
Changes here affect all four surfaces simultaneously.

Treat changes in this directory as medium-to-high blast radius by default.

## What this module contains

Confirmed from `cmp-shared/build.gradle.kts` and `cmp-shared/src/`:

**Source sets:**
- `commonMain` — shared app assembly and composition
- `iosMain` — iOS-specific bridging code
- `nativeMain` — native platform bridging

**CocoaPods framework (iOS):**
- Framework name: `ComposeApp` (the name iOS code imports)
- Type: static (`isStatic = true`)
- iOS deployment target: 16.0
- Podfile location: `../cmp-ios/Podfile`

**Core dependencies:**
- `projects.core.domain`
- `projects.core.data`
- `projects.core.network`
- `projects.coreBase.ui`

**Feature modules composed here (all 25, confirmed from `build.gradle.kts`):**

accounts, auth, beneficiary, editpassword, faq, fast-mpay, finance, history,
home, invoices, kyc, merchants, mpay-qr, mpay-qr-scan, notification, passcode,
payments, profile, receipt, savedcards, settings, standing-instruction,
transfer-interbank, transfer-intrabank, upi-setup

Also includes: `libs.mifosPasscode`

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
