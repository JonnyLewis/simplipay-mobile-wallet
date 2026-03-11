# CLAUDE.md Rewrite Plan

**Date:** 2026-03-11
**Source of truth:** `.ai/reviews/context-audit.md`
**Status:** Plan only — no files modified.

All additions, removals, and rewrites below are grounded in confirmed codebase facts from the audit. Nothing is invented. Proposed wording is clearly marked.

---

## File 1: `feature/CLAUDE.md`

### Overall recommendation

Write from scratch. The file is currently 0 bytes. The `feature/` directory is the most actively developed area of the repo with 22 modules. The absence of guidance is the highest-priority gap in the context set.

### Exact additions to make

The entire file content below is new. Every claim is confirmed from the audit.

**Source facts used:**
- Module list: confirmed from `settings.gradle.kts` and filesystem
- Convention plugin: confirmed from `feature/auth/build.gradle.kts` and `CMPFeatureConventionPlugin.kt`
- Standard deps: confirmed from `CMPFeatureConventionPlugin.kt`
- Source set structure: confirmed from `feature/auth/src/`

**Proposed full file content:**

```markdown
# Feature module guidance

This file contains guidance for work inside `feature/`.

## Why this area matters

`feature/` contains 22 product feature modules. This is the most actively
developed area of the codebase. Each module is a Kotlin Multiplatform (KMP)
shared module using Compose Multiplatform (CMP) for UI.

## Feature modules

Confirmed from `settings.gradle.kts` and filesystem:

| Module | Path |
|--------|------|
| accounts | `feature/accounts` |
| auth | `feature/auth` |
| beneficiary | `feature/beneficiary` |
| editpassword | `feature/editpassword` |
| faq | `feature/faq` |
| fast-mpay | `feature/fast-mpay` |
| finance | `feature/finance` |
| history | `feature/history` |
| home | `feature/home` |
| invoices | `feature/invoices` |
| kyc | `feature/kyc` |
| merchants | `feature/merchants` |
| mpay-qr | `feature/mpay-qr` |
| mpay-qr-scan | `feature/mpay-qr-scan` |
| notification | `feature/notification` |
| passcode | `feature/passcode` |
| payments | `feature/payments` |
| profile | `feature/profile` |
| receipt | `feature/receipt` |
| savedcards | `feature/savedcards` |
| settings | `feature/settings` |
| standing-instruction | `feature/standing-instruction` |
| transfer-interbank | `feature/transfer-interbank` |
| transfer-intrabank | `feature/transfer-intrabank` |
| upi-setup | `feature/upi-setup` |

## Standard module structure

Each feature module follows this source set layout (confirmed from `feature/auth/src/`):

```
feature/<name>/
├── build.gradle.kts
└── src/
    ├── commonMain/   — shared KMP code (UI, state, domain interaction)
    ├── androidMain/  — Android-specific code (if any)
    └── commonTest/   — shared tests
```

## Convention plugin

Every feature module applies:
```kotlin
alias(libs.plugins.cmp.feature.convention)  // org.convention.cmp.feature
```

This plugin automatically provides (confirmed from `CMPFeatureConventionPlugin.kt`):
- `core:ui` and `core:designsystem` — shared UI primitives
- `core:data` — data access layer
- Koin Compose DI
- Jetbrains ViewModel and Lifecycle
- Jetbrains Compose Navigation
- Kotlin Serialization
- Passcode and biometrics authenticator libs

Do not manually re-add these dependencies in a feature's build.gradle.kts unless
overriding or adding platform-specific extras.

## High-risk feature modules

Treat changes to these modules as high risk and follow the full Research → Plan
→ Implement sequence:

- `feature/auth` — login, session creation, token handling
- `feature/passcode` — passcode entry, verification, biometrics integration
- `feature/payments` — payment initiation and confirmation
- `feature/kyc` — identity verification flow
- `feature/history` — transaction record source of truth display
- `feature/transfer-intrabank` and `feature/transfer-interbank` — fund movement

## Blast radius rules

- Any change to `core:data`, `core:model`, or `core:domain` is automatically
  consumed by every feature module via the convention plugin. State all
  downstream impact before editing those modules.
- Any change to `core:ui` or `core:designsystem` affects every feature's visual
  surface.
- A change to `cmp-shared/` (where all features are assembled) affects all
  platform targets simultaneously.

## Required workflow

For any non-trivial change to a feature module:
1. Research — identify the exact UI → state → domain → data chain
2. Plan — list every file expected to change
3. Implement — smallest safe patch only

Do not mix feature behavior changes with unrelated cleanup.

## Avoid

- Adding dependencies that the convention plugin already provides
- Bypassing the shared data layer for platform-specific workarounds
- Changing feature contracts (navigation inputs/outputs, state models) without
  tracing all consumers
- Mixing UI changes with payment or auth behavior changes
```

### Exact removals to make

None — file is empty.

### Exact rewrites to make

None — write from scratch.

### What should remain unchanged

Nothing — file is empty.

### What should NOT be added yet

- Per-feature internal state machine or ViewModel details (not verified across all 22 modules).
- Per-feature navigation routes or argument types (not verified uniformly).
- Internal state management patterns — each module may differ.

---

## File 2: `.github/CLAUDE.md`

### Overall recommendation

Keep all accurate content for the 3 documented workflows, custom actions, and secrets table. Make targeted fixes:
1. Remove all broken `docs/claude/` and `docs/analysis/` links.
2. Remove non-functional `[← Back to Main]` navigation links.
3. Add stubs for the 6 undocumented workflows.
4. Fix the Fastfile line count reference.

### Exact additions to make

**Add a new "Additional Workflows" section** after the existing `### 3. promote-to-production.yml` section (before the `## Custom Actions` section).

**Proposed wording** (all workflow names confirmed from `.github/workflows/` filesystem):

```markdown
### Additional Workflows

The following workflows exist in `.github/workflows/` but are not yet fully
documented. Do not modify these without first reading the workflow file.

| Workflow | File | Purpose |
|----------|------|---------|
| Build and Deploy Site | `build-and-deploy-site.yml` | Documentation/site deployment |
| Cache Cleanup | `cache-cleanup.yaml` | Gradle/CI cache maintenance |
| Monthly Version Tag | `monthly-version-tag.yml` | Automated monthly version tagging via reckon |
| Sync Dirs | `sync-dirs.yaml` | Directory sync automation |
| Tag Weekly Release | `tag-weekly-release.yml` | Automated weekly release tagging |
| Upload Demo App on Firebase | `upload-demo-app-on-firebase.yaml` | Demo APK Firebase distribution |

> These workflows have not been audited in detail. Inspect the workflow file
> before making changes.
```

### Exact removals to make

Remove every link that points to a nonexistent file. The specific instances are:

**Line 157:**
```
See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#5-production-promotion-has-no-validation).
```
Replace with:
```
See known issue: no validation that beta release exists before promotion.
```

**Line 189:**
```
**⚠️ Known Issue:** `set +e` swallows versionFile errors. See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#4-version-generation-task-may-fail-silently).
```
Replace with:
```
**⚠️ Known Issue:** `set +e` swallows versionFile errors — build may proceed with a missing or stale version.
```

**Line 219:**
```
See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#1-firebase-tester-groups-parameter-ignored)
```
Replace with: *(remove the sentence — the workaround is already documented inline)*

**Line 221:**
```
- See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#1-firebase-tester-groups-parameter-ignored)
```
Replace with: *(remove line)*

**Line 272:**
```
See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#5-production-promotion-has-no-validation).
```
Replace with: *(remove the sentence — the bug is already described in the same bullet)*

**Line 344:**
```
- See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#1-firebase-tester-groups-parameter-ignored)
```
Replace with: *(remove line)*

**Lines 618:**
```
See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#1-firebase-tester-groups-parameter-ignored)
```
Replace with: *(remove the sentence)*

**Lines 710–712 (Troubleshooting footer):**
```
- [Deployment Playbook](../docs/claude/deployment-playbook.md)
- [Known Issues](../docs/analysis/BUGS_AND_ISSUES.md)
- [GitHub Actions Deep Dive](../docs/claude/github-actions-deep-dive.md)
```
Replace with:
```
- See `fastlane/CLAUDE.md` for Fastlane lane details
- See `fastlane-config/project_config.rb` for deployment configuration
```

**Line 8 and line 714 — navigation links:**
```
[← Back to Main](../CLAUDE.md)
```
Remove both. These links are non-functional in the Claude Code context.

### Exact rewrites to make

**Line 302 — Fastfile line count reference** (appears in Custom Actions section):

Current:
```
Fastfile (755 lines)
```
Rewrite to:
```
Fastfile (815 lines)
```

### What should remain unchanged

- All content for the 3 documented workflows (`multi-platform-build-and-publish.yml`, `pr-check.yml`, `promote-to-production.yml`).
- All 13 Custom Actions descriptions.
- Secrets table (categories, names, file-to-secret mapping).
- All troubleshooting items that do not reference broken links.
- `keystore-manager.sh` command examples.
- Last Updated date, reusable workflow version, architecture diagram.

### What should NOT be added yet

- Full job-level documentation for the 6 undocumented workflows (requires workflow file inspection).
- Secret values or encoded content.

---

## File 3: `CLAUDE.md` (root)

### Overall recommendation

Keep all existing workflow, safety, and operating-mode content. It is accurate and well-structured. Make targeted additions only: a tech stack section, a branding note, and corrections to the repo shape section.

### Exact additions to make

**1. Add a new "Tech stack" section** after the `## Repo shape` section and before `## Product and safety context`.

**Proposed wording** (all facts confirmed from audit):

```markdown
## Tech stack

This is a Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) codebase.

Confirmed from build files and convention plugins:
- **Language:** Kotlin (multiplatform)
- **UI:** Compose Multiplatform (Jetbrains Compose)
- **DI:** Koin (`org.convention.kmp.koin` convention plugin)
- **Navigation:** Jetbrains Compose Navigation
- **Network:** Ktorfit over Ktor (not Retrofit), Kotlinx Serialization
- **Versioning:** `org.ajoberstar.reckon` (in `settings.gradle.kts`)
- **Static analysis:** Detekt, Spotless, ktlint, dependencyGuard (enforced in CI)
- **Platforms:** Android, iOS, Desktop, Web

The `cmp.feature.convention` plugin wires Koin, Navigation, and core
dependencies into every feature module automatically.
```

**2. Add `libs/mifos-passcode` to the repo shape bullet list.** Confirmed from `settings.gradle.kts` (`include(":libs:mifos-passcode")`).

Current list ends with:
```
- `docs/`, `config/`, `fastlane/`, `libs/`, and supporting repo infrastructure
```
Rewrite to:
```
- `libs/mifos-passcode/` for the embedded passcode library
- `fastlane-config/` for deployment configuration (`project_config.rb`, `android_config.rb`, `ios_config.rb`)
- `docs/`, `config/`, `fastlane/`, and supporting repo infrastructure
```

**3. Expand the `core-base/` bullet** in the repo shape. Current wording:
```
- `core-base/` for lower-level shared platform/design/network/database abstractions
```
Rewrite to:
```
- `core-base/` for lower-level platform abstractions used by `core/`: analytics,
  common, database, datastore, designsystem, network, platform, ui
  (distinct from `core/` — see `core/CLAUDE.md`)
```

**4. Add a branding note** near the top of the file after the intro paragraph.

**Proposed wording:**

```markdown
> **Codebase identity note:** The Gradle root project is named `mobile-wallet`.
> The Android namespace and applicationId are `org.mifospay`. Copyright headers
> read "Mifos Initiative". These identifiers are from the upstream Mifos Pay
> origin. When referencing package names, namespaces, or app identifiers, use
> the values in the codebase, not the product display name.
```

**5. Add a note to the `android/` and `ios/` bullets** clarifying they contain no source code.

Current:
```
- `android/` and `ios/` for platform-specific application support and wiring
```
Rewrite to:
```
- `android/` and `ios/` — these directories currently contain only their own
  CLAUDE.md files. The real platform shells are `cmp-android/` and `cmp-ios/`.
```

### Exact removals to make

None. All existing content is accurate.

### Exact rewrites to make

Only the bullet-level rewrites described in additions above (repo shape bullets for `core-base/`, `android/`+`ios/`, and the deployment config line).

### What should remain unchanged

- All of `## Primary operating mode` (Research → Plan → Implement).
- All of `## Evidence rules`.
- All of `## Shared-code rules`.
- All of `## High-risk areas`.
- All of `## Definition of done`.
- All of `## Expected output style`.
- The `## Product and safety context` section.
- The domain list (auth, passcode, wallet, transfers, etc.).

### What should NOT be added yet

- Exact DI module wiring or injection graphs.
- Route definitions or navigation graph structure.
- Backend API contracts or endpoint URLs.
- Per-module responsibility descriptions.

---

## File 4: `core/CLAUDE.md`

### Overall recommendation

Keep all existing layer-separation guidance and caution rules. Add a new section on `core-base/`, add implementation-level facts for the network module, and add the DI framework and Supabase signal.

### Exact additions to make

**1. Add a new "core-base vs core" section** before `## Module responsibilities`.

**Proposed wording** (confirmed from `settings.gradle.kts` and filesystem):

```markdown
## core vs core-base

There are two parallel module groups in this repo:

- **`core/`** — app-level KMP shared modules: data, domain, datastore,
  designsystem, ui, common, network, model, analytics. These are the modules
  consumed by `feature/` modules through the `cmp.feature.convention` plugin.

- **`core-base/`** — lower-level platform abstractions: analytics, common,
  database, datastore, designsystem, network, platform, ui. These are
  foundational modules that `core/` modules may build upon.

Do not confuse the two groups. `core-base:database` provides the database layer;
`core/` has no database module. If a task involves database access, trace it
through `core-base:database`.
```

**2. Add implementation facts to the `core:network` entry in `## Module responsibilities`.**

Current:
```
- `core:network` should remain focused on transport/API/network concerns
```
Rewrite to:
```
- `core:network` should remain focused on transport/API/network concerns.
  Implementation: Ktorfit over Ktor (not Retrofit), Kotlinx Serialization.
  Applies the `kmp.supabase.config` convention plugin — Supabase is part of the
  network layer. Depends on `core:common`, `core:model`, `core:datastore`.
```

**3. Add a DI note** to the `## Core rule` section.

After the existing "Before editing code in `core/`, identify:" bullet list, add:

```markdown
## Dependency injection

DI throughout `core/` and `feature/` is via **Koin**. The `kmp.koin` convention
plugin wires Koin into every KMP module. When tracing how a dependency is
provided, look for Koin module declarations.
```

**4. Add a Supabase signal** to `## Special caution`.

After the existing bullets in that section, add:

```markdown
When changing `core:network`:
- Note that `kmp.supabase.config` is applied — Supabase-related configuration
  may be affected by network module changes. Supabase integration details are
  not yet fully documented; treat as unverified until confirmed.
```

### Exact removals to make

None.

### Exact rewrites to make

Only the `core:network` bullet rewrite described above.

### What should remain unchanged

- All existing module responsibilities bullets (except the network one being expanded).
- `## Special caution` block (additions are appended, not rewrites).
- `## Validation expectations` section.
- `## Avoid` section.
- The overall blast-radius framing.

### What should NOT be added yet

- Exact Supabase configuration, base URL, or auth mechanism (not verified from code).
- Specific repository interface or use-case names (require per-module research).
- `core-base/` module internal responsibilities (not yet audited in detail).

---

## File 5: `build-logic/CLAUDE.md`

### Overall recommendation

Keep all existing principles. Add a concrete reference section listing the actual convention plugins, static analysis tools, and key constraints. This turns the generic guidance into grounded orientation.

### Exact additions to make

**1. Add a new "Convention plugins" section** after `## Core rule`.

**Proposed wording** (all plugin IDs confirmed from `build-logic/convention/build.gradle.kts`):

```markdown
## Convention plugins

All plugins are defined in `build-logic/convention/`. Changes here affect every
module that applies the plugin.

### Android plugins
| Plugin ID | Class | Purpose |
|-----------|-------|---------|
| `mifospay.android.application` | `AndroidApplicationConventionPlugin` | Base Android app config |
| `mifospay.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | Compose config for Android app |
| `mifospay.android.application.flavors` | `AndroidApplicationFlavorsConventionPlugin` | prod/demo build flavors |
| `org.convention.android.application.firebase` | `AndroidApplicationFirebaseConventionPlugin` | Firebase plugin wiring |
| `org.convention.android.application.lint` | `AndroidLintConventionPlugin` | Android Lint config |

### KMP / CMP plugins
| Plugin ID | Class | Purpose |
|-----------|-------|---------|
| `org.convention.cmp.feature` | `CMPFeatureConventionPlugin` | Standard feature module config — Koin, Compose, Navigation, core deps |
| `org.convention.kmp.library` | `KMPLibraryConventionPlugin` | KMP library module config |
| `org.convention.kmp.koin` | `KMPKoinConventionPlugin` | Koin DI wiring |
| `org.convention.kmp.supabase.config` | `SupabaseConfigConventionPlugin` | Supabase configuration injection |

### Static analysis and formatting
| Plugin ID | Class | Enforced in CI |
|-----------|-------|----------------|
| `mifos.detekt.plugin` | `MifosDetektConventionPlugin` | Yes |
| `mifos.spotless.plugin` | `MifosSpotlessConventionPlugin` | Yes |
| `mifos.ktlint.plugin` | `MifosKtlintConventionPlugin` | Yes |

### Git hooks
| Plugin ID | Class | Purpose |
|-----------|-------|---------|
| `mifos.git.hooks` | `MifosGitHooksConventionPlugin` | Installs pre-commit and pre-push hooks |
```

**2. Add a "CI and PR checks" section** after the new convention plugins section.

**Proposed wording** (confirmed from `.github/CLAUDE.md` and audit):

```markdown
## CI and PR checks

PR checks run in this order (confirmed from `.github/workflows/pr-check.yml`
via `static-analysis-check@v1.0.1`):

1. `./gradlew check -p build-logic` — build-logic self-check
2. `./gradlew spotlessCheck` — formatting
3. `./gradlew detekt` — Kotlin static analysis
4. `./gradlew dependencyGuard` — dependency baseline validation

**Changing any library version or adding a dependency may fail `dependencyGuard`**
until the baseline is updated. State this explicitly in any plan that touches
library versions.
```

**3. Add a "Build constraints" section** before `## Validation expectations`.

**Proposed wording** (confirmed from `build-logic/convention/build.gradle.kts`):

```markdown
## Build constraints

- **Build-logic target JDK:** 17 (`sourceCompatibility`, `targetCompatibility`,
  `jvmTarget` all set to JDK 17 in `build-logic/convention/build.gradle.kts`).
- **Versioning:** The root version system is `org.ajoberstar.reckon` (configured
  in `settings.gradle.kts`). Build-logic changes that affect version generation
  interact with reckon behavior.
```

### Exact removals to make

None.

### Exact rewrites to make

None — only additions.

### What should remain unchanged

- `## Why this area matters` section.
- `## Core rule` section.
- `## Build logic rules` section.
- `## Validation expectations` section.
- `## Avoid` section.

### What should NOT be added yet

- Per-plugin internal implementation details.
- Gradle daemon or parallel build configuration specifics.
- Dependency version catalog structure (requires separate audit of `libs.versions.toml`).

---

## File 6: `fastlane/CLAUDE.md`

### Overall recommendation

Keep all accurate lane documentation, secrets guidance, and troubleshooting content. Make three targeted fixes: correct the file structure diagram, fix the Fastfile line count, and replace all broken `docs/claude/` and `docs/analysis/` links.

### Exact additions to make

**1. Add an "Internal lanes" section** after the iOS lanes section, before `## Configuration`.

**Proposed wording** (confirmed from Fastfile lane grep):

```markdown
## Internal lanes

These lanes are called by other lanes and are not intended to be run directly.
They are documented here to aid debugging.

| Lane | Platform | Purpose |
|------|----------|---------|
| `generateVersion` | Android | Generates version string for build; reads from Gradle or Git |
| `generateReleaseNote` | Android | Generates release notes from last commit |
| `generateFullReleaseNote` | Android | Generates full release notes |
| `increment_version` | iOS | Gets version from Gradle, sets in Xcode project, increments build number |
| `generateReleaseNote` | iOS | Generates release notes from last commit |
```

### Exact removals to make

Remove every link pointing to nonexistent `docs/claude/` or `docs/analysis/` paths.

**Troubleshooting footer (near line 797–802):**

Current:
```markdown
**Need more help?**
- [Deployment Playbook](../docs/claude/deployment-playbook.md)
- [Version Handling Guide](../docs/claude/version-handling.md)
- [Secrets Management Guide](../docs/claude/secrets-management.md)
- [Known Issues](../docs/analysis/BUGS_AND_ISSUES.md)
```
Replace with:
```markdown
**Need more help?**
- See `.github/CLAUDE.md` for CI/CD workflow documentation
- See `fastlane-config/project_config.rb` for all deployment identifiers
- See `keystore-manager.sh` in the repo root for secret encoding/management
```

**Line 430 — Version Handling Guide link:**
```
See [Version Handling Guide](../docs/claude/version-handling.md)
```
Replace with: *(remove sentence — the sanitization behavior is already fully described in the lane documentation above it)*

**Line 726 — Version Handling Guide link in iOS troubleshooting:**
```
- See [Version Handling Guide](../docs/claude/version-handling.md)
```
Replace with: *(remove line)*

**BUGS_AND_ISSUES links:**

Line 151–152:
```
- See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#1-firebase-tester-groups-parameter-ignored)
```
Replace with: *(remove line — bug is described inline)*

Line 179:
```
**⚠️ Same bug:** `groups` parameter ignored
```
Keep this line. Remove any trailing link if present.

Line 241:
```
**⚠️ No Validation:** Doesn't check if internal release exists. See [BUGS_AND_ISSUES.md](../docs/analysis/BUGS_AND_ISSUES.md#5-production-promotion-has-no-validation).
```
Replace with:
```
**⚠️ No Validation:** Doesn't check if internal release exists before promoting.
```

Line 268:
```
**⚠️ No Validation:** Doesn't check if beta release exists
```
Keep. Remove any trailing link if present.

**Navigation links (line 7 and line 804):**
```
[← Back to Main](../CLAUDE.md)
```
Remove both instances.

### Exact rewrites to make

**1. Header line count** (line 4):

Current:
```
**Total Lanes:** 12 (7 Android + 5 iOS)
```
Rewrite to:
```
**Total Lanes:** 18 (8 Android + 5 iOS + 5 internal)
```

**2. File structure diagram** in `## Configuration → File Structure` section:

Current:
```
fastlane-config/
├── project_config.rb     # Master configuration (ANDROID, IOS, IOS_SHARED)
├── android_config.rb     # Android-specific helpers
└── ios_config.rb         # iOS-specific helpers

fastlane/
├── Fastfile              # All lanes (755 lines)
├── config/
│   └── config_helpers.rb # Helper methods (get_firebase_config, etc.)
└── metadata/             # App Store metadata
    └── en-US/
        └── release_notes.txt
```
Rewrite to:
```
# Top-level deployment config (separate directory from fastlane/)
fastlane-config/
├── project_config.rb     # Master configuration (ANDROID, IOS, IOS_SHARED)
├── android_config.rb     # Android-specific helpers
├── ios_config.rb         # iOS-specific helpers
└── extract_config.rb     # Config extraction helpers

# Fastlane automation
fastlane/
├── FastFile              # All lanes (815 lines)
├── AppFile               # App identifier config
├── PluginFile            # Plugin declarations
├── config/
│   └── config_helpers.rb # Helper methods (get_firebase_config, etc.)
└── metadata/             # App Store metadata
    └── en-US/
        └── release_notes.txt
```

### What should remain unchanged

- All lane documentation (purpose, inputs, behavior, output artifacts) for all 12 public-facing lanes.
- Android signing config env var names (`KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, etc.).
- iOS Match repository URL, branch, SSH key path.
- Version sanitization documentation.
- Firebase tester groups bug description (inline).
- All troubleshooting items that don't reference broken links.
- `project_config.rb` section descriptions.
- Last Updated date and configuration table.

### What should NOT be added yet

- Fastfile internal implementation details.
- Complete `project_config.rb` content inline (creates duplication and drift).
- Actual Firebase App IDs, Play Store package names, or App Store identifiers.

---

## File 7: `cmp-shared/CLAUDE.md`

### Overall recommendation

Keep all existing blast-radius guidance and rules. Add a concrete "What this module contains" section at the top to ground the generic guidance in actual facts.

### Exact additions to make

**1. Add a "What this module contains" section** after the `## Why this area matters` section.

**Proposed wording** (all facts confirmed from `cmp-shared/build.gradle.kts` and `cmp-shared/src/`):

```markdown
## What this module contains

`cmp-shared` is the Compose Multiplatform assembly point. It composes all feature
modules and core modules into a single shared module consumed by each platform target.

**Platform targets:** Android, iOS (arm64, x64, simulatorArm64), Desktop, Web

**Source sets (confirmed from `cmp-shared/src/`):**
- `commonMain` — shared app assembly, navigation, and composition
- `iosMain` — iOS-specific bridging code
- `nativeMain` — native platform bridging

**CocoaPods framework (confirmed from `cmp-shared/build.gradle.kts`):**
- Framework name: `ComposeApp` (this is the name imported by iOS code)
- Type: static (`isStatic = true`)
- iOS deployment target: 16.0
- Podfile location: `../cmp-ios/Podfile`

**Core dependencies (confirmed):**
- `projects.core.domain`
- `projects.core.data`
- `projects.core.network`
- `projects.coreBase.ui`

**Feature modules composed here (all 22, confirmed from `build.gradle.kts`):**
accounts, auth, beneficiary, editpassword, faq, fast-mpay, finance, history,
home, invoices, kyc, merchants, mpay-qr, mpay-qr-scan, notification, passcode,
payments, profile, receipt, savedcards, settings, standing-instruction,
transfer-interbank, transfer-intrabank, upi-setup

Also includes: `libs.mifosPasscode`
```

### Exact removals to make

None.

### Exact rewrites to make

In `## Why this area matters`, the phrase "multiple targets" is vague. Replace:

Current:
```
It pulls together shared dependencies and feature modules used across multiple targets.
Changes here may affect Android, iOS, desktop, and web behavior.
```
Rewrite to:
```
It pulls together all feature modules and core modules for Android, iOS, Desktop,
and Web targets. Changes here affect all four platform surfaces simultaneously.
```

### What should remain unchanged

- `## Core rule` section.
- `## Required workflow` section.
- `## What to identify before editing` section.
- `## Shared-assembly rules` section.
- `## Validation expectations` section.
- `## Avoid` section.

### What should NOT be added yet

- Internal navigation graph or app-level navigation route details.
- Feature module lifecycle or ordering specifics.

---

## File 8: `android/CLAUDE.md`

### Overall recommendation

Keep all existing guidance (it is accurate). Add a directory-status note at the top and add concrete known facts about the Android shell.

### Exact additions to make

**1. Add a directory-status note** at the very top of the file, before all other content.

**Proposed wording** (confirmed from filesystem):

```markdown
> **Directory status:** The `android/` directory currently contains only this
> CLAUDE.md file. There is no Android application source code here.
> The Android application shell is in `cmp-android/`.
```

**2. Add a "Known facts: cmp-android" section** after `## Known upstream facts`.

**Proposed wording** (confirmed from `cmp-android/build.gradle.kts` and `AppFlavor.kt`):

```markdown
## Known facts: cmp-android

Confirmed from `cmp-android/build.gradle.kts`:

- **Namespace / applicationId:** `org.mifospay`
- **Build types:** `debug` (id suffix `.debug`), `release`
- **Build flavors:** `prod` (no suffix), `demo` (id suffix `.demo`)
  - Flavor dimension: `contentType`
- **Direct dependencies:** `projects.cmpShared`, `projects.core.data`,
  `projects.core.ui`
- **Signing env vars:** `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`,
  `KEYSTORE_ALIAS_PASSWORD`
- **ProGuard:** Currently disabled (`isMinifyEnabled = false`). Open issue #1815.
  Do not re-enable without resolving that issue.
- **Google services:** `google-services.json` is in `cmp-android/`
```

### Exact removals to make

None.

### Exact rewrites to make

In `## Known upstream facts`, the existing wording is:

```
The upstream Android app shell is `cmp-android`, uses Android application
conventions plus Compose, flavors, Google services, KSP, and depends on
`projects.cmpShared`, `projects.core.data`, and `projects.core.ui`.
```

This is accurate but partially redundant with the new section being added. Keep it as-is since it provides a useful one-line orientation. No rewrite needed.

### What should remain unchanged

- `## Scope` section.
- `## Why this area matters` section.
- `## Android rules` section.
- `## Validation expectations` section.
- `## Avoid` section.

### What should NOT be added yet

- Firebase App IDs (in `fastlane-config/project_config.rb` — do not duplicate here).
- Specific Gradle variant task names.
- Play Store package configuration.

---

## File 9: `ios/CLAUDE.md`

### Overall recommendation

Keep all existing guidance. Add a directory-status note and add known concrete facts about `cmp-ios/`.

### Exact additions to make

**1. Add a directory-status note** at the very top.

**Proposed wording** (confirmed from filesystem):

```markdown
> **Directory status:** The `ios/` directory currently contains only this
> CLAUDE.md file. There is no iOS application source code here.
> The iOS application shell is in `cmp-ios/`.
```

**2. Add a "Known facts: cmp-ios" section** after `## Known upstream facts`.

**Proposed wording** (confirmed from `cmp-ios/` filesystem and `cmp-shared/build.gradle.kts`):

```markdown
## Known facts: cmp-ios

Confirmed from `cmp-ios/` filesystem and `cmp-shared/build.gradle.kts`:

- **Xcode workspace:** `cmp-ios/iosApp.xcworkspace`
- **Xcode project:** `cmp-ios/iosApp.xcodeproj`
- **Podfile:** `cmp-ios/Podfile`
- **CocoaPods framework imported:** `ComposeApp` (static; produced by `cmp-shared`)
- **iOS deployment target:** 16.0
- **Configuration directory:** `cmp-ios/Configuration/` (build configurations;
  not yet audited in detail)

When working on iOS-specific issues, open `cmp-ios/iosApp.xcworkspace` (not
the `.xcodeproj` directly, as CocoaPods requires the workspace).
```

### Exact removals to make

None.

### Exact rewrites to make

None.

### What should remain unchanged

- `## Scope` section.
- `## Why this area matters` section.
- `## Known upstream facts` section (the existing one-line fact is accurate).
- `## iOS rules` section.
- `## Validation expectations` section.
- `## Avoid` section.

### What should NOT be added yet

- Scheme names and build configurations inside `cmp-ios/iosApp.xcodeproj` (not yet verified).
- APN/push notification configuration details (scripts exist but internals not audited).

---

## File 10: `scripts/CLAUDE.md`

### Overall recommendation

Keep all existing guidance (it is accurate and generic enough to remain useful). Add a concrete "Script inventory" section that separates `scripts/` from root-level scripts.

### Exact additions to make

**1. Add a "Script inventory" section** after `## Why this area matters`.

**Proposed wording** (all script names confirmed from filesystem):

```markdown
## Script inventory

Scripts in this repo are split across two locations.

### `scripts/` directory — iOS deployment and verification

Confirmed from `scripts/` filesystem:

| Script | Purpose |
|--------|---------|
| `pre-commit.sh` | Pre-commit hook (installed via `mifos.git.hooks` plugin) |
| `pre-push.sh` | Pre-push hook |
| `deploy_firebase.sh` | iOS Firebase App Distribution deployment |
| `deploy_testflight.sh` | iOS TestFlight deployment |
| `deploy_appstore.sh` | iOS App Store deployment |
| `setup_ios_complete.sh` | iOS setup wizard (Match, APN, provisioning) |
| `setup_apn_key.sh` | APN key configuration |
| `verify_ios_deployment.sh` | iOS deployment verification |
| `verify_apn_setup.sh` | APN setup verification |
| `check_ios_version.sh` | iOS version sanitization check |

### Root-level scripts

Confirmed from root filesystem:

| Script | Purpose |
|--------|---------|
| `keystore-manager.sh` | Keystore and secret encoding/management — **high risk** |
| `generateModuleGraphs.sh` | Module dependency graph generation |
| `ci-prepush.sh` / `ci-prepush.bat` | CI pre-push checks (cross-platform) |
| `sync-dirs.sh` | Directory sync automation |
| `bootstrap-claude-setup-v2.sh` | Repo bootstrap (purpose not yet fully documented) |

The same caution rules in this file apply to both locations.
```

### Exact removals to make

None.

### Exact rewrites to make

None.

### What should remain unchanged

- `## Why this area matters` section.
- `## Core rule` section.
- `## Script rules` section.
- `## Special caution` section.
- `## Validation expectations` section.
- `## Avoid` section.

### What should NOT be added yet

- Internal environment variable requirements per script (requires per-script validation).
- CI environment assumptions for each script.
- `bootstrap-claude-setup-v2.sh` purpose (not yet verified).

---

## Summary Table

| File | New Sections | Rewrites | Removals | Unchanged |
|------|-------------|---------|----------|-----------|
| `feature/CLAUDE.md` | Full file (write from scratch) | — | — | Nothing (was empty) |
| `.github/CLAUDE.md` | Additional Workflows section | Fastfile line count, broken link replacements | 8 broken link instances, 2 nav links | All 3 workflow docs, all custom actions, secrets table |
| `CLAUDE.md` | Tech stack section, branding note | 3 repo-shape bullets | None | All workflow/safety/operating-mode content |
| `core/CLAUDE.md` | core vs core-base, DI, Supabase signal | core:network bullet | None | All existing layer-separation guidance |
| `build-logic/CLAUDE.md` | Convention plugins table, CI checks, Build constraints | None | None | All existing principles |
| `fastlane/CLAUDE.md` | Internal lanes section | Header lane count, file structure diagram, broken link replacements | 6 broken link instances, 2 nav links | All 12 public lane docs, signing vars, Match config |
| `cmp-shared/CLAUDE.md` | What this module contains | "multiple targets" sentence | None | All guidance sections |
| `android/CLAUDE.md` | Directory-status note, Known facts section | None | None | All guidance sections |
| `ios/CLAUDE.md` | Directory-status note, Known facts section | None | None | All guidance sections |
| `scripts/CLAUDE.md` | Script inventory section | None | None | All guidance sections |
