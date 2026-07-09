# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Context (Start Here)

**Current Focus**: Mobile Wallet Feature Development

### Session Workflow (Never Lose Context)

```bash
# START of session
/session-start                # Load context from previous session

# DURING session - use these commands
/gap-analysis                 # What's done vs what's needed (5 layers)
/gap-analysis [layer]         # Layer-specific (design|server|client|feature|platform)
/gap-planning [feature]       # Plan specific improvements
/implement [feature]          # Execute implementation
/verify [feature]             # Confirm implementation

# END of session
/session-end                  # Save context for next session
```

### 5-Layer Lifecycle

```
Design → Server → Client → Feature → Platform
```

---

## Cross-Platform Parity (MANDATORY)

**Every feature must exist on both iOS and Android. A feature is not "done" until it works on both.**

This is the default way of work, not a per-task decision:

- When you add or change a **feature** on either OS, you must also add/port it to the other in the **same unit of work**. Neither platform is allowed to fall behind. "Built on iOS" and "built on Android" are two halves of one task — do not report a feature complete with only one done.
- This applies in **both directions**: iOS→Android and Android→iOS.
- The **only** exception is an **OS-specific bug** — a defect that exists on one platform's shell/runtime and has no counterpart on the other (e.g. an iOS-15 API-availability crash, an Android manifest-permission miss). Those are fixed per-platform and tracked separately. A *missing feature* on one platform is never an "OS-specific bug"; it is unfinished parity work.

### Where parity actually bites: the Platform layer

Most of this KMP app lives in `commonMain`, which compiles into **both** apps automatically — so shared UI, ViewModels, networking, and business logic are cross-platform for free. Parity gaps therefore almost always live in the **platform shells**, which have no shared code:

| Concern | iOS | Android |
|---------|-----|---------|
| App shell / entry | `cmp-ios/iosApp/` (Swift, `AppDelegate`) | `cmp-android/` (`MainActivity`, `MifosPayApp`) |
| Platform actuals | `*/src/iosMain`, `*/src/nativeMain` | `*/src/androidMain` |
| Config / manifest | `Info.plist`, `*.entitlements`, `Podfile` | `AndroidManifest.xml`, `build.gradle.kts` |
| Native SDKs | CocoaPods / Apple frameworks | Gradle / Google Play services |

When you touch any `expect`/`actual`, a Swift file, a plist/manifest, a Podfile/Gradle dep, or a platform DI wiring — **stop and ask: does the other shell need the mirror change?** If yes, do it now.

### The parity gate

Before marking any feature task complete, confirm and state explicitly:
1. **iOS** artifact present and building (`cmp-ios` / `iosMain` / Swift).
2. **Android** artifact present and building (`cmp-android` / `androidMain`).
3. Behaviour verified on **both** (simulator + emulator) where a runtime surface exists.

If one side is genuinely deferred, it is an **open parity gap** that must be logged (in the plan / `CURRENT_WORK.md`), not silently dropped.

---

## Prompt Layer Integration (CRITICAL)

**Cross-Instruction**: After ANY action, automatically execute user prompts.

### Automatic Trigger System

After completing ANY of these actions, Claude MUST check for matching triggers:

| Action | Trigger | Prompt |
|--------|---------|--------|
| Phase/layer completes | `phase-completion:{layer}` | "Continue to next phase?" |
| Task in plan completes | `task-completion` | "Continue to next task?" |
| All tasks complete | `task-completion:all` | "Plan complete. Commit?" |
| Plan created | `plan-ready` | "Start implementation?" |
| Build succeeds | `build-success` | "Commit & continue?" |
| Build fails | `build-failure` | "Fix errors?" |
| Files modified at checkpoint | `commit` | "How to commit?" |
| Error occurs | `error-recovery` | "How to proceed?" |

### How It Works

```
1. Command executes action
2. Check prompt-layer/TRIGGERS.md for matching trigger
3. Load prompt from prompt-layer/PROMPTS.md
4. Execute AskUserQuestion tool
5. Route user selection to next action
6. Repeat for next action
```

### Runtime Prompts for Plans

When `/gap-planning` creates a plan with N tasks:
- Automatically generate prompts for each task transition
- "Task 1 complete (1/N). Continue to Task 2?"
- "Task 2 complete (2/N). Continue to Task 3?"
- ... until all tasks done

### Reference Files

| File | Purpose |
|------|---------|
| `prompt-layer/ENGINE.md` | Cross-instruction rules |
| `prompt-layer/TRIGGERS.md` | When prompts fire |
| `prompt-layer/PROMPTS.md` | Prompt definitions |
| `prompt-layer/RUNTIME.md` | Dynamic plan prompts |

**IMPORTANT**: Do NOT hardcode prompts in commands. Let the engine handle them automatically.

---

## Project Overview

MifosX Mobile Wallet is a Kotlin Multiplatform (KMP) application providing mobile payment and wallet services. Active features include account management, peer-to-peer transfers (intrabank and interbank), QR payments, send-money, and autopay/bill management. Additional modules — merchants, invoices, saved cards, KYC, UPI setup, and standing instructions — are built and DI-wired but currently disabled (their navigation entry points are commented out in `MifosNavHost.kt`) in the v2 app shell. It targets Android, iOS, Desktop (JVM), and Web (Kotlin/JS + WASM).

## Build Commands

```bash
# Build the project
./gradlew build

# Run all pre-push checks (recommended before creating PR)
./ci-prepush.sh

# Individual checks
./gradlew check -p build-logic                     # Verify build-logic configuration
./gradlew spotlessApply --no-configuration-cache   # Apply code formatting
./gradlew dependencyGuardBaseline                  # Generate dependency-guard baseline
./gradlew detekt                                   # Run static analysis

# Run tests
./gradlew :cmp-android:testDebugUnitTest            # Run Android unit tests
./gradlew :core:data:desktopTest                    # Run JVM/desktop tests for a module

# Lint checks
./gradlew :cmp-android:lintRelease                 # Run lint on Android app

# Android builds
./gradlew :cmp-android:assembleDemoDebug           # Build demo debug APK
./gradlew :cmp-android:assembleProdRelease         # Build production release APK
```

## Architecture

### Module Structure

**Platform Entry Points:**
- `cmp-android/` - Android application module
- `cmp-ios/` - iOS application (uses CocoaPods via `cmp-shared`)
- `cmp-desktop/` - Desktop (JVM) application
- `cmp-web/` - Web application (Kotlin/JS)
- `cmp-shared/` - Shared KMP module compiled for all platforms; also hosts navigation graphs in `src/commonMain/kotlin/org/mifospay/shared/navigation/`

**Core Modules (`core/`):**
- `data/` - Repository implementations, connects network to UI
- `domain/` - Domain use cases and business logic
- `network/` - Ktorfit-based API services and HTTP client
- `model/` - Domain models shared across features
- `datastore/` - Local data persistence (DataStore)
- `ui/` - Shared UI components and `BaseViewModel`
- `designsystem/` - Design tokens, theme, common composables
- `common/` - Shared utilities (`DataState`, `ErrorHandler`, `SafeApiCall`)
- `analytics/` - Analytics abstractions

**Core Base Modules (`core-base/`):** Platform-agnostic infrastructure modules shared across the template system (8 modules):
- `datastore/` - Generic reactive preferences (contracts, factory, cache, serialization, validation)
- `common/` - Shared utilities
- `database/` - Database abstractions
- `network/` - KtorHttpClient, DynamicBaseUrlPlugin, SupabaseConfigClient, NetworkResult
- `designsystem/` - KptMaterialTheme, KptTheme, adaptive layout (ListDetail, SplitPane, ResponsiveLayout, Grid, MasonryGrid)
- `platform/` - IntentManager, AppReviewManager, AppUpdateManager, GarbageCollectionManager
- `ui/` - BaseViewModel (template), EventsEffect, NavGraphBuilder extensions, ShareUtils, LifecycleEventEffect
- `analytics/` - Analytics abstractions

**Feature Modules (`feature/`):** Each feature is a separate KMP module containing screens, ViewModels, and navigation. The 27 feature modules are listed below; the **Status** column marks whether the feature currently has a reachable entry point in the v2 app shell:

| # | Module | Domain | Status |
|:-:|--------|--------|:------:|
| 1 | `auth` | Login, signup, mobile verification | Active |
| 2 | `home` | Wallet home — accounts, quick actions, recent transactions | Active |
| 3 | `accounts` | Savings account list and detail | Active |
| 4 | `history` | Transaction history, detail, specific account transactions | Active |
| 5 | `receipt` | Transaction receipt view | Active |
| 6 | `faq` | Frequently asked questions | Active |
| 7 | `send-money` | UPI/contact-based send money flow | Active |
| 8 | `transfer-intrabank` | Intra-bank transfer hub, payee selection, confirm, success | Active |
| 9 | `transfer-interbank` | Inter-bank transfer flow | Active |
| 10 | `notification` | Push notification list | Active |
| 11 | `editpassword` | Change password | Active |
| 12 | `kyc` | KYC levels 1–3 | Disabled |
| 13 | `savedcards` | Saved debit/credit card management | Disabled |
| 14 | `invoices` | Invoice list and detail | Disabled |
| 15 | `settings` | App settings, language selection | Active |
| 16 | `profile` | User profile, edit profile | Active |
| 17 | `finance` | Finance overview tab | Active |
| 18 | `merchants` | Merchant list and merchant transfer | Disabled |
| 19 | `beneficiary` | Beneficiary list, add/edit/delete | Active |
| 20 | `standing-instruction` | Standing instruction list, create/edit/detail | Disabled |
| 21 | `payments` | Payments tab, request money, transfer type selection | Active |
| 22 | `upi-setup` | UPI PIN setup via debit card + OTP | Disabled |
| 23 | `autopay` | Autopay schedules, bills, billers, history | Active |
| 24 | `mpay-qr` | QR code generation for receiving payments | Active |
| 25 | `mpay-qr-scan` | Full QR scan flow with camera, import, processing | Active |
| 26 | `fast-mpay` | Fast payment via QR processing | Active |
| 27 | `passcode` | Passcode and biometrics setup/auth | Active |

> **Disabled** = the module builds and is registered in Koin, but its navigation entry point is commented out in `MifosNavHost.kt`, so it has no reachable path in the current v2 app shell. (Two earlier-listed modules, `make-transfer` and `qr`, were removed — they were build-less orphan stubs with no `build.gradle.kts`, never compiled, and depended on by nothing.)

### Key Patterns

**Dependency Injection:** Koin for all platforms. Each module defines a Koin module in its `di/` package. All modules are registered in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt`.

**Navigation:** Uses Jetbrains Compose Navigation. Navigation graphs defined in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/`:
- `ROOT_GRAPH` → `LOGIN_GRAPH` (login, signup, mobile verification)
- `ROOT_GRAPH` → passcode screens (rootMifosPasscodeScreen, biometricSetupScreen, reAuthMifosPasscodeScreen — registered directly in RootNavGraph, not a separate graph)
- `ROOT_GRAPH` → `MAIN_GRAPH` (authenticated shell; starting destination: HOME_ROUTE; bottom-nav tabs: HOME, PAYMENTS, FINANCE, HISTORY)

**Network Layer:**Ktorfit (Retrofit-like for Ktor) with services in `core/network/src/commonMain/kotlin/org/mifospay/core/network/services/`. Two API managers:
- `SelfServiceApiManager` — Fineract self-service layer (authentication, clients, savings accounts, beneficiaries, transfers, account transfers, office, user)
- `FineractApiManager` — Direct Fineract APIs (KYC, invoices, notifications, saved cards, standing instructions, autopay, bills, billers, registration, search, documents, run reports, two-factor auth)

**State Management:** ViewModels extend `BaseViewModel<State, Event, Action>` from `org.mifospay.core.ui.utils.BaseViewModel`. Features use `DataState<T>` (from `org.mifospay.core.common.DataState`) for loading/success/error states. State is `@Serializable data class`, Events and Actions are `sealed interface`.

### Convention Plugins

Custom Gradle plugins in `build-logic/convention/` standardize module configuration:
- `mifos.android.application` - Android app configuration
- `org.convention.cmp.feature` - KMP feature module (applies Compose, Koin, core dependencies)
- `org.convention.kmp.library` - KMP library module
- `mifos.kmp.room` - Room database setup for KMP
- `mifos.spotless.plugin`, `mifos.detekt.plugin` - Code quality

### Build Flavors

Android has two product flavors:
- `demo` - Development/testing
- `prod` - Production

## Development Notes

- JDK 21 required (see `build-logic/convention/build.gradle.kts`)
- Pull requests target the `development` branch
- Commit message format: `<type>(<scope>): <subject>` (feat, fix, docs, refactor, test, chore)

## Git Commit & PR Guidelines

- **Always use feature branches**: NEVER push directly to `development` branch
  - Create a feature branch: `git checkout -b feature/[description]`
  - Push to feature branch: `git push origin feature/[description]`
  - Create PR targeting `development` branch
- **No Claude references**: Do NOT add Claude attribution, co-author lines, or "Generated with Claude Code" footers to commits or PRs
- Keep commit messages clean and focused on the changes made
- PR descriptions should only contain relevant technical information
