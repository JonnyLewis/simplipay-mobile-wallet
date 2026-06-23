# Claude Code Setup Port — Implementation Plan
**Date**: 2026-06-23  
**Scope**: Port the Simpli-mobile `.claude/` setup to simplipay-mobile-wallet, adapting every file for the mobile wallet domain.

---

## Overview

Port 17 Claude Code files (CLAUDE.md + 16 commands) from `/Users/jonathanlewis/Development/home/Simpli-mobile` to `/Users/jonathanlewis/Development/home/simplipay-mobile-wallet`, plus create 11 bootstrap index files. Every file must be read from source before being written — no paraphrasing. Every project-specific reference must be corrected.

---

## What We're NOT Doing

- Replacing `.github/CLAUDE.md` — that is CI/CD infrastructure documentation and must stay untouched
- Porting the `prompt-layer/` directory — the commands reference it, and it will be created in Phase 3 as skeleton files only
- Changing any Kotlin source code
- Creating design specs or implementing features

---

## Current State

Source project has a fully built Claude Code setup at:
```
Simpli-mobile/
├── CLAUDE.md                          ← root-level Claude guidance doc
└── .claude/commands/                  ← 16 custom slash commands
    ├── README.md
    ├── session-start.md
    ├── session-end.md
    ├── gap-analysis.md
    ├── gap-planning.md
    ├── gap-status.md
    ├── design.md
    ├── client.md
    ├── feature.md
    ├── implement.md
    ├── verify.md
    ├── verify-tests.md
    ├── projectstatus.md
    ├── project-add.md
    ├── project-list.md
    └── project-set.md
```

Target project has nothing equivalent — only `.github/CLAUDE.md` (CI/CD docs, preserved).

---

## Substitution Rules (Applied to Every File)

These substitutions apply globally before per-file changes:

| Find | Replace | Reason |
|------|---------|--------|
| `Mifos Mobile` | `MifosX Mobile Wallet` | Product name |
| `self-service banking` | `mobile wallet and payments` | Domain |
| `mifos-mobile` | `mobile-wallet` | Root project name in settings.gradle |
| `cmp-navigation/` | `cmp-shared/navigation/` | No separate nav module here |
| `cmp-navigation` | `cmp-shared` | Same reason |
| `AUTH_GRAPH` | `LOGIN_GRAPH` | Nav graph renamed |
| `PASSCODE_GRAPH` | *(remove — passcode lives inside ROOT_GRAPH, not its own graph)* | Nav structure |
| `tt.mifos.community` | *(remove — no hardcoded instance)* | No demo credentials |
| `gsoc.mifos.community` | *(remove)* | No demo credentials |
| `Username \`maria\`, Password \`password\`` | *(remove)* | No demo credentials |
| `libs/` module section | *(remove)* | No libs/ dir in wallet project |
| `template.core.base.ui.BaseViewModel` | `org.mifospay.core.ui.utils.BaseViewModel` | Correct import in this project |

---

## Phase 1 — CLAUDE.md (root level)

**File**: `simplipay-mobile-wallet/CLAUDE.md` (new file — does not exist)  
**Source**: `Simpli-mobile/CLAUDE.md`

### Changes beyond global substitutions:

**Section: Project Overview**  
Replace the banking description with:
> MifosX Mobile Wallet is a Kotlin Multiplatform (KMP) application providing mobile payment and wallet services. It supports account management, peer-to-peer transfers (intrabank and interbank), QR payments, autopay/bill management, UPI setup, merchants, invoices, saved cards, KYC, and standing instructions. It targets Android, iOS, Desktop (JVM), and Web (Kotlin/JS + WASM).

**Section: Module Structure — Platform Entry Points**  
Remove `cmp-navigation/` line entirely. Navigation graphs live in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/`.

**Section: Module Structure — Core Modules**  
Keep `core/` section. The source CLAUDE.md already has a `core-base/` entry at line 145 — do not add it again, just replace its single-line description with an expanded version for this project:

```
**Core Base Modules (`core-base/`):** Platform-agnostic infrastructure modules shared across the template system (8 modules):
- `datastore/` — Generic reactive preferences (contracts, factory, cache, serialization, validation)
- `common/` — Shared utilities
- `database/` — Database abstractions
- `network/` — KtorHttpClient, DynamicBaseUrlPlugin, SupabaseConfigClient, NetworkResult
- `designsystem/` — KptMaterialTheme, KptTheme, adaptive layout (ListDetail, SplitPane, ResponsiveLayout, Grid, MasonryGrid)
- `platform/` — IntentManager, AppReviewManager, AppUpdateManager, GarbageCollectionManager
- `ui/` — BaseViewModel, EventsEffect, NavGraphBuilder extensions, ShareUtils, LifecycleEventEffect
- `analytics/` — Analytics abstractions
```

Remove `libs/` module section entirely (the wallet project has no `libs/` directory).

**Section: Navigation**  
Replace with:
```
**Navigation:** Uses Jetbrains Compose Navigation. Navigation graphs defined in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/`:
- `ROOT_GRAPH` → `LOGIN_GRAPH` (login, signup, mobile verification)
- `ROOT_GRAPH` → passcode screens (rootMifosPasscodeScreen, biometricSetupScreen, reAuthMifosPasscodeScreen)
- `ROOT_GRAPH` → `MAIN_GRAPH` (authenticated shell with 4 bottom-nav tabs: HOME, PAYMENTS, FINANCE, HISTORY)
```

**Section: Network Layer**  
Replace base URL line. Note that the project uses two API managers:
- `FineractApiManager` — direct Fineract endpoints (auth, client, savings accounts, transfers, etc.)  
- `SelfServiceApiManager` — Fineract self-service layer (beneficiaries, invoices, etc.)  
Remove the hardcoded `tt.mifos.community` URL.

**Section: Build Flavors**  
Keep as-is (same demo/prod flavor structure).

**Section: Development Notes**  
Remove the demo credentials line (`Instance gsoc.mifos.community...`). Keep JDK 21 requirement.

**Section: Feature Modules**  
Replace the 17-feature list with the 29 wallet features:
```
home, history, receipt, faq, auth, make-transfer, send-money, transfer-intrabank,
transfer-interbank, notification, editpassword, kyc, savedcards, invoices, settings,
profile, finance, merchants, accounts, beneficiary, standing-instruction, payments,
upi-setup, qr, autopay, mpay-qr, mpay-qr-scan, fast-mpay, passcode
```

**Section: Prompt Layer**  
Keep entirely as-is — the trigger/prompt system is architecture-agnostic. Only update file path if referenced: `prompt-layer/` stays the same.

---

## Phase 2 — .claude/commands/ (16 files)

Each file is read from `Simpli-mobile/.claude/commands/[name].md`, modified per the rules below, and written to `simplipay-mobile-wallet/.claude/commands/[name].md`.

### 2.1 README.md

Source: `Simpli-mobile/.claude/commands/README.md`

**Changes**:
- Apply global substitutions
- Replace the Feature List table (section "Feature List", 17 rows) with the 29 wallet features:

| # | Feature | Design Path | Command |
|:-:|---------|-------------|---------|
| 1 | auth | features/auth/ | `/design auth` |
| 2 | home | features/home/ | `/design home` |
| 3 | accounts | features/accounts/ | `/design accounts` |
| 4 | history | features/history/ | `/design history` |
| 5 | receipt | features/receipt/ | `/design receipt` |
| 6 | faq | features/faq/ | `/design faq` |
| 7 | make-transfer | features/make-transfer/ | `/design make-transfer` |
| 8 | send-money | features/send-money/ | `/design send-money` |
| 9 | transfer-intrabank | features/transfer-intrabank/ | `/design transfer-intrabank` |
| 10 | transfer-interbank | features/transfer-interbank/ | `/design transfer-interbank` |
| 11 | notification | features/notification/ | `/design notification` |
| 12 | editpassword | features/editpassword/ | `/design editpassword` |
| 13 | kyc | features/kyc/ | `/design kyc` |
| 14 | savedcards | features/savedcards/ | `/design savedcards` |
| 15 | invoices | features/invoices/ | `/design invoices` |
| 16 | settings | features/settings/ | `/design settings` |
| 17 | profile | features/profile/ | `/design profile` |
| 18 | finance | features/finance/ | `/design finance` |
| 19 | merchants | features/merchants/ | `/design merchants` |
| 20 | beneficiary | features/beneficiary/ | `/design beneficiary` |
| 21 | standing-instruction | features/standing-instruction/ | `/design standing-instruction` |
| 22 | payments | features/payments/ | `/design payments` |
| 23 | upi-setup | features/upi-setup/ | `/design upi-setup` |
| 24 | qr | features/qr/ | `/design qr` |
| 25 | autopay | features/autopay/ | `/design autopay` |
| 26 | mpay-qr | features/mpay-qr/ | `/design mpay-qr` |
| 27 | mpay-qr-scan | features/mpay-qr-scan/ | `/design mpay-qr-scan` |
| 28 | fast-mpay | features/fast-mpay/ | `/design fast-mpay` |
| 29 | passcode | features/passcode/ | `/design passcode` |

- Update the Files table at the bottom (same filenames, no change needed there)
- Remove any reference to 17 features, replace with 29

### 2.2 session-start.md

Source: `Simpli-mobile/.claude/commands/session-start.md`

**Changes**: Apply global substitutions only. The file reads `claude-product-cycle/CURRENT_WORK.md` and `claude-product-cycle/PRODUCT_MAP.md` — these paths are intentional (will be bootstrapped in Phase 3). No structural changes.

### 2.3 session-end.md

Source: `Simpli-mobile/.claude/commands/session-end.md`

**Changes**: Apply global substitutions only. No structural changes needed.

### 2.4 gap-analysis.md

Source: `Simpli-mobile/.claude/commands/gap-analysis.md`

**Changes**:
- Apply global substitutions
- Any embedded feature list → replace with 29 wallet features
- Navigation layer description: remove `cmp-navigation/` → `cmp-shared/navigation/`
- Server layer description: keep Fineract references (same backend platform), remove hardcoded URL

### 2.5 gap-planning.md

Source: `Simpli-mobile/.claude/commands/gap-planning.md`

**Changes**:
- Apply global substitutions
- Feature list in any examples/tables → wallet features

### 2.6 gap-status.md

Source: `Simpli-mobile/.claude/commands/gap-status.md`

**Changes**: Apply global substitutions only. No feature-specific content.

### 2.7 design.md

Source: `Simpli-mobile/.claude/commands/design.md`

**Changes**:
- Apply global substitutions
- Feature list table in the "Feature List" section → 29 wallet features (same table format as README.md above)
- Remove any mentions of loan-account, savings-account, share-account, guarantor, location, client-charge, dashboard (old banking-specific features)
- SPEC.md and API.md templates: keep as-is (domain-agnostic)

### 2.8 client.md

Source: `Simpli-mobile/.claude/commands/client.md`

**Changes**:
- Apply global substitutions
- **Critical — pattern reference file names**:
  - `BeneficiaryRepositoryImp.kt` → `BeneficiaryRepositoryImpl.kt` (the 'l' was missing in the source; correct file in wallet project is `core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/BeneficiaryRepositoryImpl.kt`)
  - `NetworkModule.kt` → `FineractApiManager.kt` (the wallet project's equivalent is `core/network/src/commonMain/kotlin/org/mifospay/core/network/FineractApiManager.kt`)
- **Critical — API manager injection**: The wallet project uses two managers; repository implementations receive one or both via Koin. Update any code examples accordingly:
  - `SelfServiceApiManager` (`org.mifospay.core.network.SelfServiceApiManager`) — self-service APIs: authentication, clients, savings accounts, beneficiary, third-party transfers, account transfers, office, user
  - `FineractApiManager` (`org.mifospay.core.network.FineractApiManager`) — direct Fineract APIs: KYC, invoices, notifications, saved cards, standing instructions, autopay, bills, billers, registration, search, documents, run reports, two-factor auth
  - Pattern: `BeneficiaryRepositoryImpl` injects `SelfServiceApiManager` — this is the correct pattern reference for self-service features
- **DI registration for new repositories**: Add to `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt` — the `KoinModules.allModules` list is the centralised registration point
- Remove `cmp-navigation/` import guidance
- Feature examples: use wallet-relevant features (e.g., autopay, beneficiary, savedcards)
- **Note on incomplete features**: `make-transfer` (only has ViewModel, no Screen/Navigation) and `qr` (Screen + ViewModel only) are partial modules. If examples reference these features, note they need completion first.

### 2.9 feature.md

Source: `Simpli-mobile/.claude/commands/feature.md`

**Changes**:
- Apply global substitutions
- **Pattern reference file paths** (update the absolute or relative paths shown in the command):
  - `HomeViewModel.kt`: path is `feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeViewModel.kt` ✓
  - `HomeScreen.kt`: path is `feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeScreen.kt` ✓
  - `HomeNavigation.kt`: path is `feature/home/src/commonMain/kotlin/org/mifospay/feature/home/navigation/HomeNavigation.kt` ← note the `navigation/` subdirectory
  - `HomeModule.kt`: path is `feature/home/src/commonMain/kotlin/org/mifospay/feature/home/di/HomeModule.kt` ← note the `di/` subdirectory
- **BaseViewModel import**: `template.core.base.ui.BaseViewModel` → `org.mifospay.core.ui.utils.BaseViewModel` (this is what wallet's HomeViewModel actually imports)
- **Navigation registration**: Any instruction to add a destination to `cmp-navigation/` → change to add to `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/MifosNavHost.kt`
- **DI module registration**: Point to `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt`
- Feature list in examples → wallet features

### 2.10 implement.md

Source: `Simpli-mobile/.claude/commands/implement.md`

**Changes**:
- Apply all changes from client.md (§2.8) and feature.md (§2.9) — this command orchestrates both layers
- Global substitutions
- Remove cmp-navigation/ references throughout

### 2.11 verify.md

Source: `Simpli-mobile/.claude/commands/verify.md`

**Changes**:
- Apply global substitutions
- Feature list in verification table → wallet features
- Navigation paths: `cmp-navigation/` → `cmp-shared/navigation/`
- Feature count: 17 → 29

### 2.12 verify-tests.md

Source: `Simpli-mobile/.claude/commands/verify-tests.md`

**Changes**:
- Apply global substitutions
- Gradle module path pattern stays the same (`:feature:[name]:test`)
- Feature list in examples → wallet features
- Coverage targets stay the same (ViewModel 80%, Repository 80%, Screen 60%)

### 2.13 projectstatus.md

Source: `Simpli-mobile/.claude/commands/projectstatus.md`

**Changes**:
- Apply global substitutions
- Feature status table: replace 17 banking features with 29 wallet features
- Reads `design-spec-layer/STATUS.md` — this path will be bootstrapped in Phase 3
- All features start as `Not Started` / `❌` in the initial status table

### 2.14 project-add.md

Source: `Simpli-mobile/.claude/commands/project-add.md`

**Changes**: Apply global substitutions. The `claude-product-cycle/workspaces/` path structure is kept as-is — it's a workspace management command that's project-agnostic.

### 2.15 project-list.md

Source: `Simpli-mobile/.claude/commands/project-list.md`

**Changes**: Apply global substitutions only.

### 2.16 project-set.md

Source: `Simpli-mobile/.claude/commands/project-set.md`

**Changes**: Apply global substitutions only.

---

## Phase 3 — Bootstrap Index Files

These files are required for the commands to function on first run. They are created as empty/skeleton state — commands will populate them as the project develops.

All files created at the **project root**: `/Users/jonathanlewis/Development/home/simplipay-mobile-wallet/`

| File | Purpose | Initial Content |
|------|---------|----------------|
| `claude-product-cycle/CURRENT_WORK.md` | Session state tracker | Skeleton with "No active tasks" |
| `claude-product-cycle/PRODUCT_MAP.md` | Master status tracker | Layer table with all 29 features, all ❌ |
| `MODULES_INDEX.md` | O(1) module path lookup | All 29 features with their actual file paths |
| `SCREENS_INDEX.md` | O(1) screen path lookup | All known screen files per feature |
| `FEATURES_INDEX.md` | Feature status overview | 29 features, all layers empty |
| `FEATURE_MAP.md` | Per-feature layer completion map | 29 features × 5 layers |
| `API_INDEX.md` | Network service index | All 23 services from core/network/services/ |
| `LAYER_STATUS.md` | Layer health summary | 5 layers, all at 0% |
| `TESTING_STATUS.md` | Test coverage tracker | 29 features, all untested |
| `MOCKUPS_INDEX.md` | Mockup generation status | 29 features, all not started |
| `plans/PLANS_INDEX.md` | Active/completed plans index | Empty — no plans yet |
| `design-spec-layer/STATUS.md` | Design spec layer overview | 29 features, all not started |
| `prompt-layer/TRIGGERS.md` | Trigger definitions | Created from scratch (see below) |
| `prompt-layer/PROMPTS.md` | Prompt definitions | Created from scratch (see below) |
| `prompt-layer/ENGINE.md` | Cross-instruction rules | Created from scratch (see below) |
| `prompt-layer/RUNTIME.md` | Dynamic plan prompts | Created from scratch (see below) |

**Important**: `prompt-layer/` does NOT exist in the source `Simpli-mobile` project — it is referenced in CLAUDE.md as aspirational infrastructure but was never created. It also does not exist in the wallet project. These four files must be created from scratch as skeleton files whose content is derived from the descriptions in the source CLAUDE.md. See §3.1 below for their content.

**Important**: `claude-product-cycle/` does NOT exist in the source project either. The commands create it dynamically at runtime. We bootstrap `CURRENT_WORK.md` and `PRODUCT_MAP.md` as empty skeletons so the commands don't fail on first run.

### 3.1 prompt-layer/ Skeleton Content

**`prompt-layer/ENGINE.md`** — Cross-instruction rules:
```markdown
# Prompt Layer Engine

After completing ANY action, check TRIGGERS.md for a matching trigger.
If matched, load the prompt from PROMPTS.md and present it using AskUserQuestion.
Route the user's response to the next action.
```

**`prompt-layer/TRIGGERS.md`** — Trigger definitions:
```markdown
# Triggers

| Trigger ID | Fires When |
|------------|-----------|
| phase-completion:{layer} | A layer phase completes |
| task-completion | A task in the active plan completes |
| task-completion:all | All tasks in a plan are done |
| plan-ready | /gap-planning creates a new plan |
| build-success | Gradle build exits 0 |
| build-failure | Gradle build exits non-zero |
| commit | Files have been modified and not committed |
| error-recovery | An error occurs during execution |
| plan-continue | User continues an active plan step |
```

**`prompt-layer/PROMPTS.md`** — Prompt definitions:
```markdown
# Prompts

| Trigger | Prompt Text |
|---------|------------|
| phase-completion:{layer} | "Continue to next phase?" |
| task-completion | "Continue to next task?" |
| task-completion:all | "Plan complete. Commit?" |
| plan-ready | "Start implementation?" |
| build-success | "Commit & continue?" |
| build-failure | "Fix errors?" |
| commit | "How to commit?" |
| error-recovery | "How to proceed?" |
| plan-continue | "Continue to next step?" |
```

**`prompt-layer/RUNTIME.md`** — Dynamic plan prompts:
```markdown
# Runtime Prompts

When /gap-planning creates a plan with N tasks, auto-generate transition prompts:
- "Task 1 complete (1/N). Continue to Task 2?"
- "Task 2 complete (2/N). Continue to Task 3?"
... until all tasks done.
```

---

## Phase 4 — Review Agent

After all files are written, spawn a subagent with `subagent_type: codebase-analyzer` to verify every single file in `simplipay-mobile-wallet/.claude/` and `CLAUDE.md`.

The review agent checks:
1. Every mention of "Mifos Mobile", "self-service banking", "loan", "savings-account", "share-account", "guarantor", "dashboard" → flag if found
2. Every file path that references `cmp-navigation/` → flag if found
3. `BeneficiaryRepositoryImp.kt` (without the 'l') → flag if found
4. `NetworkModule.kt` → flag if found
5. `AUTH_GRAPH` or `PASSCODE_GRAPH` as standalone nav graphs → flag if found
6. `tt.mifos.community` or `gsoc.mifos.community` or `maria`/`password` credentials → flag if found
7. Feature count assertions ("17 features") → flag if found
8. `libs/` module references → flag if found
9. `template.core.base.ui.BaseViewModel` → flag if found (should be `org.mifospay.core.ui.utils.BaseViewModel`)

The agent returns a report of all flags. Any flagged items are fixed before the plan is considered complete.

---

## Success Criteria

### Automated:
- [ ] `ls .claude/commands/` returns exactly 16 files
- [ ] `CLAUDE.md` exists at project root
- [ ] All 11 index files exist
- [ ] All 4 `prompt-layer/` files exist
- [ ] `grep -r "Mifos Mobile\|self-service banking\|cmp-navigation\|AUTH_GRAPH\|tt\.mifos\|gsoc\.mifos\|NetworkModule\|BeneficiaryRepositoryImp\.kt\|template\.core\.base\.ui\.BaseViewModel\|libs/" .claude/ CLAUDE.md` returns no results

### Manual:
- [ ] Running `/session-start` in Claude Code loads correctly without errors
- [ ] Running `/gap-analysis` reads the correct index files
- [ ] Running `/projectstatus` shows the correct 29-feature table

---

## File Count Summary

| Category | Count |
|----------|-------|
| CLAUDE.md | 1 |
| .claude/commands/ | 16 |
| Bootstrap index files | 11 |
| prompt-layer/ files (copy) | 4 |
| **Total** | **32** |
