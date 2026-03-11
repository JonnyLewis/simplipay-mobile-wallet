# CLAUDE.md Context Audit

**Date:** 2026-03-11
**Scope:** All CLAUDE.md files in the repository
**Method:** Each file was compared against confirmed facts from the actual codebase (settings.gradle.kts, build.gradle.kts files, filesystem layout, Fastfile lane list, etc.)
**Status:** Review only — no files were modified.

---

## Summary

| File | Status | Priority |
|------|--------|----------|
| `CLAUDE.md` (root) | Accurate but missing key specifics | Medium |
| `cmp-shared/CLAUDE.md` | Generic guidance, missing structural facts | Medium |
| `feature/CLAUDE.md` | **EMPTY — 0 bytes** | High |
| `core/CLAUDE.md` | Accurate but misses core-base distinction | Medium |
| `android/CLAUDE.md` | Accurate guidance, empty directory misrepresented | Medium |
| `ios/CLAUDE.md` | Accurate guidance, empty directory misrepresented | Low |
| `build-logic/CLAUDE.md` | Good principles, missing concrete plugin facts | Medium |
| `scripts/CLAUDE.md` | Generic, misses root-vs-scripts distinction | Low |
| `.github/CLAUDE.md` | Very detailed but references nonexistent docs paths; missing 6 workflows | High |
| `fastlane/CLAUDE.md` | Very detailed but references nonexistent docs paths; line count off | Medium |

---

## Area-by-Area Review

---

### 1. Root `CLAUDE.md`

#### What the code actually shows

- **Confirmed from `settings.gradle.kts`:** Root project name is `mobile-wallet`. Modules included: `:cmp-shared`, `:cmp-android`, `:cmp-desktop`, `:cmp-web`; `:core:{data,domain,datastore,designsystem,ui,common,network,model,analytics}`; `:core-base:{datastore,common,database,network,designsystem,platform,ui,analytics}`; `:feature:{home,history,receipt,faq,auth,transfer-intrabank,transfer-interbank,notification,editpassword,kyc,savedcards,invoices,settings,profile,finance,merchants,accounts,beneficiary,standing-instruction,payments,upi-setup,mpay-qr,mpay-qr-scan,fast-mpay,passcode}`; `:libs:mifos-passcode`.
- **Confirmed from root filesystem:** `core-base/`, `libs/`, `fastlane-config/`, `cmp-desktop/`, `cmp-web/`, `cmp-ios/` all exist and are not mentioned or underdescribed.
- **Confirmed from `cmp-android/build.gradle.kts`:** App namespace and applicationId are `org.mifospay`, not SimpliPay-branded. Copyright headers say "Mifos Initiative". Product branding is still Mifos/MifosX.
- **Confirmed from `settings.gradle.kts`:** Version system uses `org.ajoberstar.reckon.settings` plugin — not mentioned anywhere in any CLAUDE.md.
- **Confirmed from `build-logic/convention/src/main/kotlin/CMPFeatureConventionPlugin.kt`:** DI is Koin. Navigation is Jetbrains Compose Navigation (`jb.composeNavigation`).
- **Confirmed from filesystem:** `android/` directory contains only `CLAUDE.md`. `ios/` directory contains only `CLAUDE.md`. These are nearly empty directories — the real platform shells are `cmp-android/` and `cmp-ios/`.

#### Comparison to current CLAUDE.md

- **Accurate:** High-level directory map (cmp-shared, cmp-android, cmp-ios, cmp-web, cmp-desktop, feature, core, core-base, android, ios, build-logic, scripts, docs, config, fastlane, libs).
- **Accurate:** Research → Plan → Implement workflow, evidence rules, shared-code rules, high-risk areas, definition of done.
- **Accurate:** Domain list (auth, passcode, OTP, wallet, transfers, payments, history, beneficiaries, accounts, cards, invoices, notifications, profile, settings).

#### Gaps and inaccuracies

1. **Branding mismatch:** The file says "SimpliPay mobile wallet" but the actual codebase uses `org.mifospay` namespace, "Mifos Initiative" copyright, and `mobile-wallet` as the root project name. This could create confusion about what the product actually is at the code level.
2. **Missing tech stack:** No mention of Kotlin Multiplatform (KMP) / Compose Multiplatform (CMP) as the underlying architecture. This is the single most important architectural fact for understanding blast radius.
3. **Missing DI framework:** Koin is the DI framework, used in `KMPKoinConventionPlugin.kt` and declared as a convention dependency. Not mentioned.
4. **Missing navigation approach:** Jetbrains Compose Navigation is the navigation layer. Not mentioned.
5. **Missing versioning system:** `org.ajoberstar.reckon` (from `settings.gradle.kts`) drives all version generation. Not mentioned.
6. **Missing `libs/mifos-passcode`:** This module is included in `settings.gradle.kts` and depended on by feature modules. Not mentioned in the repo shape.
7. **Missing `fastlane-config/`:** This directory (with `project_config.rb`, `android_config.rb`, `ios_config.rb`) is the single source of truth for deployment config and exists as a separate top-level directory. Not mentioned.
8. **`android/` and `ios/` misrepresented:** The CLAUDE.md implies these are real platform areas. In practice, `android/` contains only its own CLAUDE.md file, and `ios/` contains only its own CLAUDE.md file. The real platform shells are `cmp-android/` and `cmp-ios/`.
9. **`core-base/` not described:** Listed in repo shape but not explained. `core-base/` contains: analytics, common, database, datastore, designsystem, network, platform, ui. The distinction between `core/` (app-level KMP shared) and `core-base/` (lower-level platform abstractions) is not stated.

#### What should NOT be added yet

- Exact DI module wiring (not verified from code).
- Route definitions or navigation graph structure (not verified).
- Backend API contracts (not verified from code).

---

### 2. `cmp-shared/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `cmp-shared/build.gradle.kts`:** `cmp-shared` depends on: `projects.core.domain`, `projects.coreBase.ui`, `projects.core.data`, `projects.core.network`, and all 22+ feature modules.
- **Confirmed from `cmp-shared/build.gradle.kts`:** Uses `kotlinCocoapods`, iOS targets `iosArm64`, `iosSimulatorArm64`, `iosX64`. CocoaPods framework: `baseName = "ComposeApp"`, `isStatic = true`, `ios.deploymentTarget = "16.0"`, `podfile = project.file("../cmp-ios/Podfile")`.
- **Confirmed from `cmp-shared/src/`:** Source sets are `commonMain`, `iosMain`, `nativeMain`. (Note: `desktopMain` is referenced in build.gradle.kts dependencies but is a logical source set, not a physical directory — this is normal for KMP.)
- **Confirmed:** The `cmp-shared` module is a KMP/CMP shared module, not just a dependency-sharing project. It is the assembly point for the cross-platform app.

#### Comparison to current CLAUDE.md

- **Accurate:** General blast-radius framing, the call to trace consumers before editing, the need to identify surface impact.
- **Accurate:** References to `core:domain`, `core:data`, `core:network` as dependencies (confirmed by build.gradle.kts).

#### Gaps and inaccuracies

1. **Missing concrete dependency list:** The CLAUDE.md doesn't name which feature modules compose from here, so a reader doesn't know the actual composition footprint.
2. **Missing CocoaPods/framework facts:** Framework name is `ComposeApp`, it is static, iOS deployment target is 16.0. These are specific and important for iOS build troubleshooting.
3. **Missing source set structure:** `commonMain`, `iosMain`, `nativeMain` are real source sets. Mentioning them helps understand where iOS-specific bridging code lives.
4. **Missing `coreBase.ui` dependency:** `cmp-shared` depends on `projects.coreBase.ui`, not just `core.*` modules. This is meaningful for tracing impact.
5. **"multiple targets" wording is vague:** Could be more explicit that Android, iOS, Desktop, and Web are the four targets.

#### What should NOT be added yet

- Internal navigation graph details.
- Feature module lifecycle management specifics.

---

### 3. `feature/CLAUDE.md`

#### What the code actually shows

- **Confirmed from filesystem:** `feature/` directory contains 22 modules: `accounts`, `auth`, `beneficiary`, `editpassword`, `faq`, `fast-mpay`, `finance`, `history`, `home`, `invoices`, `kyc`, `merchants`, `mpay-qr`, `mpay-qr-scan`, `notification`, `passcode`, `payments`, `profile`, `receipt`, `savedcards`, `settings`, `standing-instruction`, `transfer-interbank`, `transfer-intrabank`, `upi-setup`.
- **Confirmed from `settings.gradle.kts`:** All 22 modules are explicitly included.
- **Confirmed from `feature/auth/build.gradle.kts`:** Each feature uses `alias(libs.plugins.cmp.feature.convention)`. This applies the `CMPFeatureConventionPlugin` which adds Koin, Compose, Kotlin Serialization, lifecycle, navigation, and core:ui/designsystem/data as standard dependencies.
- **Confirmed from `feature/auth/src/`:** Each feature has at least `androidMain`, `commonMain`, `commonTest` source sets. (Auth has those three; others may vary.)
- **Confirmed from `CMPFeatureConventionPlugin.kt`:** Features standard deps include: `core:ui`, `core:designsystem`, `core:data`, Koin Compose, Jetbrains ViewModel, Jetbrains Navigation, Kotlin Serialization, `mifos.authenticator.passcode`, `mifos.authenticator.biometrics`.

#### Comparison to current CLAUDE.md

- **The file is EMPTY (0 bytes).** There is no content at all.

#### Gaps and inaccuracies

1. **CRITICAL: No content whatsoever.** This is one of the most actively edited areas of the codebase with 22 modules. The complete absence of guidance is a significant gap.

#### What should be added

- The module list (the 22 feature modules confirmed above).
- The standard feature structure: `commonMain`, `androidMain`, `commonTest`.
- The convention plugin: `cmp.feature.convention` applies Koin, Compose, Serialization, Navigation, core:ui, core:designsystem, core:data automatically.
- High-risk features: auth, passcode, payments, kyc.
- Blast radius rule: any change to `core:data`, `core:model`, or `core:domain` affects all features through their standard convention plugin wiring.

#### What should NOT be added yet

- Per-feature detailed behavior (requires per-feature research).
- Internal state management patterns per module (not verified uniformly).

---

### 4. `core/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `settings.gradle.kts`:** `core/` modules are: `data`, `domain`, `datastore`, `designsystem`, `ui`, `common`, `network`, `model`, `analytics`. All 9 confirmed.
- **Confirmed from `core/network/build.gradle.kts`:** Network module uses Ktorfit (Retrofit-like layer over Ktor), Kotlin Serialization, Ktor client, and depends on `projects.core.common`, `projects.core.model`, `projects.core.datastore`. Also uses `kmp.supabase.config` plugin, suggesting Supabase integration in the network layer.
- **Confirmed from `core-base/` filesystem:** `core-base/` contains its own modules: `analytics`, `common`, `database`, `datastore`, `designsystem`, `network`, `platform`, `ui`. These are distinct from `core/`.
- **Confirmed from `settings.gradle.kts`:** Both `core:` and `core-base:` module groups are included. They coexist.

#### Comparison to current CLAUDE.md

- **Accurate:** Module names match (`data`, `domain`, `datastore`, `designsystem`, `ui`, `common`, `network`, `model`, `analytics`).
- **Accurate:** Layer separation guidance (domain vs data vs network etc.).
- **Accurate:** Caution around serialization, defaults, retry behavior.

#### Gaps and inaccuracies

1. **`core-base/` is not mentioned at all.** This is a separate group of 8 modules that parallels `core/` at a lower level. Failing to distinguish these two groups can cause misdirected changes.
2. **Missing network layer implementation details:** `core:network` uses Ktorfit (not Retrofit) and Ktor. Serialization is Kotlinx Serialization. These are important for anyone working in network code.
3. **Missing Supabase integration signal:** `core:network` applies `kmp.supabase.config` convention plugin. The existence of Supabase as a backend integration is not mentioned.
4. **Missing DI framework:** Koin is the DI framework for all core modules. Not mentioned.
5. **`core-base:database`** is in core-base but not in core — there is a database layer that is not mentioned in the core CLAUDE.md at all.

#### What should NOT be added yet

- Exact Supabase configuration or backend URL (not verified from code).
- Specific repository interfaces or use-case names (require per-module research).

---

### 5. `android/CLAUDE.md`

#### What the code actually shows

- **Confirmed from filesystem:** The `android/` directory contains **only `CLAUDE.md`**. There is no application code, no source tree, no build file. It is essentially empty.
- **Confirmed from filesystem:** `cmp-android/` is the real Android application shell, containing `build.gradle.kts`, `src/main/`, `google-services.json`, proguard rules, lint baseline, etc.
- **Confirmed from `cmp-android/build.gradle.kts`:** App namespace is `org.mifospay`, applicationId is `org.mifospay`. Build flavors: `prod` and `demo`. Build types: `debug` and `release`. Depends on: `projects.cmpShared`, `projects.core.data`, `projects.core.ui`.
- **Confirmed from `cmp-android/build.gradle.kts`:** ProGuard is explicitly disabled (comment references issue #1815).
- **Confirmed from `AppFlavor.kt`:** FlavorDimension is `contentType`. Flavors are `demo` (suffix `.demo`) and `prod` (no suffix).

#### Comparison to current CLAUDE.md

- **Accurate:** Scope description (android/ and cmp-android/).
- **Accurate:** Core dependencies match (`projects.cmpShared`, `projects.core.data`, `projects.core.ui`).
- **Accurate:** Mentions Google services, KSP, flavors, Compose.

#### Gaps and inaccuracies

1. **`android/` directory is empty of code.** The CLAUDE.md presents `android/` as a meaningful platform area. In practice it contains only this CLAUDE.md. A reader expecting to find Android platform code in `android/` will be confused.
2. **Missing app ID:** The namespace and applicationId (`org.mifospay`) are not stated, which is important for build/signing/release work.
3. **Missing flavor names:** Flavors are `prod` and `demo`, with dimension `contentType`. This is concrete and useful.
4. **Missing: ProGuard explicitly disabled.** This is an active known issue (referenced issue #1815) and affects security posture of release builds.
5. **Missing: signing config reads from environment variables** (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD`). These are the actual env var names used.

#### What should NOT be added yet

- Firebase App IDs (already in fastlane-config, duplicating here creates drift risk).
- Specific variant task names (too fragile to document without full build validation).

---

### 6. `ios/CLAUDE.md`

#### What the code actually shows

- **Confirmed from filesystem:** The `ios/` directory contains **only `CLAUDE.md`**. No Xcode project, no source files.
- **Confirmed from filesystem:** `cmp-ios/` is the real iOS shell, containing `Podfile`, `Podfile.lock`, `iosApp.xcodeproj/`, `iosApp.xcworkspace/`, `iosApp/`, `Pods/`, `Configuration/`.
- **Confirmed from `cmp-shared/build.gradle.kts`:** Podfile path is `"../cmp-ios/Podfile"`. Deployment target is `ios.deploymentTarget = "16.0"`. Framework name is `ComposeApp` (static).

#### Comparison to current CLAUDE.md

- **Accurate:** References `cmp-ios` as the iOS module, CocoaPods, iOS 16.0 deployment target, Podfile location.
- **Accurate:** Guidance to separate iOS shell logic from shared code.

#### Gaps and inaccuracies

1. **`ios/` directory is empty of code.** Same pattern as `android/` — the directory this CLAUDE.md lives in has no actual iOS code.
2. **Missing Xcode workspace and project paths:** The workspace is at `cmp-ios/iosApp.xcworkspace` and project at `cmp-ios/iosApp.xcodeproj`. These are critical for any iOS build or configuration work.
3. **Missing CocoaPods framework name:** The framework produced is `ComposeApp` (static). This is the bridge name iOS code imports.
4. **Missing `Configuration/` directory in cmp-ios:** `cmp-ios/Configuration/` exists and likely contains scheme or build configuration files (not yet verified in detail).

#### What should NOT be added yet

- Scheme names and build configurations inside `cmp-ios/iosApp.xcodeproj` (not verified).

---

### 7. `build-logic/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `build-logic/convention/build.gradle.kts`:** Convention plugins registered:
  - Android plugins: `mifospay.android.application.compose`, `mifospay.android.application`, `mifospay.android.application.flavors`, `org.convention.android.application.firebase`, `org.convention.android.application.lint`
  - KMP/CMP plugins: `org.convention.cmp.feature`, `org.convention.kmp.koin`, `org.convention.kmp.library`, `org.convention.kmp.supabase.config`
  - Static analysis: `mifos.detekt.plugin`, `mifos.spotless.plugin`, `mifos.ktlint.plugin`
  - Git: `mifos.git.hooks`
- **Confirmed from `build-logic/convention/build.gradle.kts`:** Target JDK is 17.
- **Confirmed from `build-logic/convention/src/main/kotlin/`:** Implementation classes: `AndroidApplicationComposeConventionPlugin`, `AndroidApplicationConventionPlugin`, `AndroidApplicationFirebaseConventionPlugin`, `AndroidApplicationFlavorsConventionPlugin`, `AndroidLintConventionPlugin`, `CMPFeatureConventionPlugin`, `KMPKoinConventionPlugin`, `KMPLibraryConventionPlugin`, `MifosDetektConventionPlugin`, `MifosGitHooksConventionPlugin`, `MifosKtlintConventionPlugin`, `MifosSpotlessConventionPlugin`, `SupabaseConfigConventionPlugin`.
- **Confirmed from `settings.gradle.kts`:** Versioning uses `org.ajoberstar.reckon.settings` plugin.
- **Confirmed from `.github/CLAUDE.md`:** PR checks include `dependencyGuard`.

#### Comparison to current CLAUDE.md

- **Accurate:** High-level blast-radius framing, caution around module graph, KMP, Compose, KSP, Ktorfit, signing, versioning.
- **Accurate:** Small-safe-change guidance.

#### Gaps and inaccuracies

1. **Missing concrete plugin IDs.** A developer editing build-logic needs to know the actual plugin aliases. None are listed.
2. **Missing static analysis plugin names.** Detekt, Spotless, ktlint — these drive CI and PR checks. Not mentioned.
3. **Missing git hooks plugin.** `MifosGitHooksConventionPlugin` installs pre-commit/pre-push hooks. This is operationally important.
4. **Missing `dependencyGuard`.** PR checks validate dependencies with dependencyGuard. Changing library versions can fail CI. Not mentioned.
5. **Missing reckon versioning system.** The `ajoberstar/reckon` plugin (in settings.gradle.kts) drives version tagging and generation. Changes to build-logic that affect versioning behavior interact with this.
6. **Missing target JDK.** Build-logic targets JDK 17. This is a concrete constraint for build environment setup.
7. **`build-logic/Untitled`** file exists in the directory — likely an editor artifact. Not meaningful but visible in filesystem.

#### What should NOT be added yet

- Per-plugin internal implementation details (too deep for CLAUDE.md).
- Gradle daemon configuration specifics.

---

### 8. `scripts/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `scripts/` filesystem:** The directory contains: `check_ios_version.sh`, `deploy_appstore.sh`, `deploy_firebase.sh`, `deploy_testflight.sh`, `pre-commit.sh`, `pre-push.sh`, `setup_apn_key.sh`, `setup_ios_complete.sh`, `verify_apn_setup.sh`, `verify_ios_deployment.sh`.
- **Confirmed from root filesystem:** Several scripts live at the **root level**, not in `scripts/`: `generateModuleGraphs.sh`, `keystore-manager.sh`, `ci-prepush.sh`, `ci-prepush.bat`, `sync-dirs.sh`, `bootstrap-claude-setup-v2.sh`.
- **Inference:** `scripts/` contains primarily iOS-focused scripts. Root-level scripts handle module graphs, keystore management, CI pre-push checks, and sync.

#### Comparison to current CLAUDE.md

- **Accurate:** General guidance on caution, calling conventions, secrets handling, validation expectations.
- **Accurate:** Calls out pre-push, signing/keystore, sync, module graph generation, release automation as high risk.

#### Gaps and inaccuracies

1. **Missing distinction between `scripts/` and root-level scripts.** `generateModuleGraphs.sh` and `keystore-manager.sh` are in the root, not `scripts/`. The guidance in `scripts/CLAUDE.md` applies to both, but the physical location split is not stated.
2. **Missing specific script names.** The actual scripts in `scripts/` are all iOS-deployment and iOS-verification related. A reader working on an iOS deploy issue would benefit from knowing this.
3. **Missing: `keystore-manager.sh` in root.** This is the primary secret/keystore management tool, referenced in `.github/CLAUDE.md` and `fastlane/CLAUDE.md`. It is not in `scripts/` but it's one of the most consequential root scripts.
4. **`bootstrap-claude-setup-v2.sh`** exists in root. Purpose not documented anywhere.

#### What should NOT be added yet

- Internal implementation of each script (too volatile).
- Exact environment variable requirements for each script (needs validation per script).

---

### 9. `.github/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `.github/workflows/` filesystem:** 9 workflow files exist: `build-and-deploy-site.yml`, `cache-cleanup.yaml`, `monthly-version-tag.yml`, `multi-platform-build-and-publish.yml`, `pr-check.yml`, `promote-to-production.yml`, `sync-dirs.yaml`, `tag-weekly-release.yml`, `upload-demo-app-on-firebase.yaml`.
- **Confirmed:** `.github/CLAUDE.md` documents only 3 workflows (`multi-platform-build-and-publish.yml`, `pr-check.yml`, `promote-to-production.yml`).
- **Confirmed:** 6 workflows are not documented: `build-and-deploy-site.yml`, `cache-cleanup.yaml`, `monthly-version-tag.yml`, `sync-dirs.yaml`, `tag-weekly-release.yml`, `upload-demo-app-on-firebase.yaml`.
- **Confirmed from docs/ filesystem:** `docs/claude/` does NOT exist. `docs/analysis/` does NOT exist. The CLAUDE.md references `../docs/claude/deployment-playbook.md`, `../docs/analysis/BUGS_AND_ISSUES.md`, `../docs/claude/version-handling.md`, `../docs/claude/github-actions-deep-dive.md`, `../docs/claude/secrets-management.md` — all of which are broken links.
- **Confirmed from `fastlane/FastFile`:** Fastfile is 815 lines, not 755 as stated.

#### Comparison to current CLAUDE.md

- **Accurate:** The 3 documented workflows (purpose, trigger, job list, secrets).
- **Accurate:** Custom action versions and job structure for the documented workflows.
- **Accurate:** Secrets table (categories and names).
- **Accurate:** `keystore-manager.sh` reference for encoding secrets.

#### Gaps and inaccuracies

1. **CRITICAL: All `docs/claude/` and `docs/analysis/` references are broken.** These directories do not exist. Any link in this file pointing to them is dead. Examples: `BUGS_AND_ISSUES.md`, `deployment-playbook.md`, `version-handling.md`, `github-actions-deep-dive.md`, `secrets-management.md`, `secrets-management.md`. These references were authored as if the docs existed or would be created, but they have not been.
2. **6 workflows are undocumented:** `build-and-deploy-site.yml`, `cache-cleanup.yaml`, `monthly-version-tag.yml`, `sync-dirs.yaml`, `tag-weekly-release.yml`, `upload-demo-app-on-firebase.yaml`. At minimum their purpose and trigger should be noted.
3. **Fastfile line count is wrong:** States 755 lines; actual count is 815 lines.
4. **`[← Back to Main](../CLAUDE.md)` navigation links** are written as if this is a rendered site. In the Claude Code context, these are non-functional.

#### What should NOT be added yet

- Full job details for undocumented workflows (needs separate workflow file inspection per workflow).
- Specific secret values or encoded formats.

---

### 10. `fastlane/CLAUDE.md`

#### What the code actually shows

- **Confirmed from `fastlane/FastFile` lane grep:** 18 lane definitions total: `assembleDebugApks`, `assembleReleaseApks`, `bundleReleaseApks`, `deployReleaseApkOnFirebase`, `deployDemoApkOnFirebase`, `deployInternal`, `promoteToBeta`, `promote_to_production`, `generateVersion`, `generateReleaseNote` (Android), `generateFullReleaseNote`, `build_ios`, `build_signed_ios`, `increment_version`, `generateReleaseNote` (iOS), `deploy_on_firebase`, `beta`, `release`.
- **Confirmed from `fastlane-config/project_config.rb`:** `ORGANIZATION_NAME = "Mifos Initiative"`. `ANDROID.package_name = "org.mifospay"`. Firebase prod app ID: `1:728434912738:android:0490c291986f0a691a1dbb`. The project config still contains Mifos-branded identifiers.
- **Confirmed from filesystem:** `fastlane/config/config_helpers.rb` exists. The `fastlane-config/` directory at root level contains `project_config.rb`, `android_config.rb`, `ios_config.rb`, `extract_config.rb`. Note: fastlane/CLAUDE.md describes the fastlane-config/ files but incorrectly places them under `fastlane/` in the file structure diagram.
- **Confirmed:** `docs/claude/` does not exist. All links to it are broken.
- **Confirmed from `wc -l`:** Fastfile is 815 lines, CLAUDE.md states 755.

#### Comparison to current CLAUDE.md

- **Accurate:** Lane purposes, inputs, Fastlane Match details, tester groups bug, Play Store promotion flow.
- **Accurate:** Android signing config env vars (`KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD`).
- **Accurate:** iOS Match repository URL, branch, and SSH key path.
- **Accurate:** Version sanitization behavior (Gradle semver → App Store `YYYY.M.CommitCount`).

#### Gaps and inaccuracies

1. **CRITICAL: All `docs/claude/` references are broken** (same issue as .github/CLAUDE.md).
2. **File structure diagram is inaccurate:** Shows `fastlane-config/` files inside `fastlane/`, but `fastlane-config/` is a separate top-level directory. The actual `fastlane/` directory contains: `AppFile`, `FastFile`, `PluginFile`, `config/config_helpers.rb`, `metadata/`.
3. **Fastfile line count wrong:** 755 stated, 815 actual.
4. **Internal lanes not documented:** `generateVersion`, `generateReleaseNote`, `generateFullReleaseNote`, `increment_version` are internal lanes not described in the CLAUDE.md. These are called by other lanes but could be useful to know about for debugging version generation issues.
5. **`fastlane-config/project_config.rb` still contains Mifos branding:** If this is a SimpliPay product, the config still says `ORGANIZATION_NAME = "Mifos Initiative"` and uses `org.mifospay` package names. This is a product configuration gap, not a documentation gap.
6. **Lane count in header is off:** Header says "Total Lanes: 12 (7 Android + 5 iOS)" — this counts public-facing lanes. The file actually contains 18 lane definitions including internal ones.

#### What should NOT be added yet

- Fastfile internal implementation details (too volatile).
- Complete `project_config.rb` content (would create duplication and drift).

---

## Cross-Cutting Issues

### Issue 1: Broken `docs/claude/` and `docs/analysis/` references

Both `.github/CLAUDE.md` and `fastlane/CLAUDE.md` contain numerous links to files in `docs/claude/` and `docs/analysis/` that do not exist in the repository. These directories were either planned and never created, or were removed.

**Affected files referenced but not found:**
- `docs/claude/deployment-playbook.md`
- `docs/claude/version-handling.md`
- `docs/claude/github-actions-deep-dive.md`
- `docs/claude/secrets-management.md`
- `docs/analysis/BUGS_AND_ISSUES.md`

**Decision needed:** Either create these documents with real content, or remove the references from CLAUDE.md files.

### Issue 2: SimpliPay branding vs Mifos codebase

The root CLAUDE.md identifies this as "SimpliPay Mobile Wallet." However:
- Root project name in `settings.gradle.kts` is `mobile-wallet`
- Android namespace and applicationId: `org.mifospay`
- Copyright headers: "Mifos Initiative"
- `fastlane-config/project_config.rb`: `ORGANIZATION_NAME = "Mifos Initiative"`, `package_name: "org.mifospay"`
- `cmp-ios` app identifier (from fastlane config): `org.mifos.kmp.template`

**Implication:** If this is truly a SimpliPay fork, these identifiers have not been updated in the codebase. CLAUDE.md files should either reflect the actual state (Mifos-branded) or note that rebranding is in progress. Documenting it as "SimpliPay" when the code says `org.mifospay` creates ambiguity.

### Issue 3: `feature/CLAUDE.md` is empty

The most actively developed directory in the repo (22 modules, 400+ likely Kotlin files) has zero guidance. This is the highest-priority gap.

### Issue 4: `android/` and `ios/` are empty directories

Both `android/CLAUDE.md` and `ios/CLAUDE.md` exist in directories with no actual code. Readers may incorrectly try to find platform code there. The CLAUDE.md files in both directories should note explicitly that the directory is a placeholder and redirect to `cmp-android/` and `cmp-ios/` respectively.

---

## Recommended Priority Order for Updates

1. **High:** `feature/CLAUDE.md` — write from scratch (confirmed facts above provide the content).
2. **High:** `.github/CLAUDE.md` — remove or clearly mark broken `docs/claude/` and `docs/analysis/` links; add the 6 undocumented workflows.
3. **Medium:** Root `CLAUDE.md` — add tech stack (KMP/CMP, Koin, Jetbrains Navigation), version system (reckon), note `libs/mifos-passcode`, note branding situation.
4. **Medium:** `core/CLAUDE.md` — add `core-base/` distinction, add Ktorfit/Ktor network layer fact, add Koin, flag Supabase.
5. **Medium:** `build-logic/CLAUDE.md` — add concrete plugin IDs, static analysis tools, git hooks, dependencyGuard, JDK 17 target.
6. **Medium:** `fastlane/CLAUDE.md` — fix file structure diagram, fix line count, mark broken doc links.
7. **Low:** `cmp-shared/CLAUDE.md` — add concrete dependency list, CocoaPods framework name, source set structure.
8. **Low:** `android/CLAUDE.md` — add note that `android/` is empty, redirect to `cmp-android/`, add app ID, flavor names.
9. **Low:** `ios/CLAUDE.md` — add note that `ios/` is empty, redirect to `cmp-ios/`, add workspace path.
10. **Low:** `scripts/CLAUDE.md` — clarify distinction between `scripts/` (iOS-focused) and root-level scripts.
