# Draft Review: All CLAUDE.md Drafts

Reviewed against: `.ai/reviews/context-audit.md` and `.ai/reviews/context-md-rewrite-plan.md`

Status legend:
- **PASS** — All claims confirmed, no significant issues
- **FLAG** — One or more issues requiring correction before filing
- **MINOR** — Low-severity note; does not block filing

---

## 1. feature-CLAUDE-draft.md — PASS

All claims confirmed against `settings.gradle.kts`, `feature/` filesystem,
`CMPFeatureConventionPlugin.kt`, and `feature/auth/src/`.

**Confirmed:**
- 25 unique feature modules (26 entries in settings.gradle.kts, `:feature:invoices`
  duplicated — correctly noted in draft)
- Convention plugin ID `org.convention.cmp.feature` confirmed
- Dependency table (core:ui, core:designsystem, core:data, Koin, ViewModel, Navigation,
  Serialization, passcode, biometrics) matches CMPFeatureConventionPlugin.kt
- Source set structure (`commonMain`, `androidMain`, `commonTest`) confirmed from
  `feature/auth/src/`
- High-risk module list is appropriate for a financial app

**Minor notes:**
- "Other modules may also have `iosMain` or `desktopMain` source sets" — inference,
  but correctly hedged with "may". Acceptable.
- Blast radius rule referencing `core:domain` is stated as consumed by every feature
  "through the convention plugin" — `core:domain` is not directly in CMPFeatureConventionPlugin's
  dep list (only core:data, core:ui, core:designsystem are explicit). `core:domain` is
  consumed transitively via `core:data`. This is a minor imprecision that should be
  softened: "transitively through `core:data`" rather than "through the convention plugin".

**No removals needed.**

---

## 2. root-CLAUDE-draft.md — PASS

All claims confirmed. The branding note, tech stack additions, and repo shape updates
are all grounded in confirmed codebase facts.

**Confirmed:**
- Gradle root project `mobile-wallet`, namespace `org.mifospay`, copyright "Mifos Initiative"
  all confirmed from build files and `settings.gradle.kts`
- Tech stack facts (Koin, Ktorfit/Ktor, reckon, Detekt/Spotless/ktlint/dependencyGuard,
  CMP/KMP, Jetbrains Compose Navigation) all confirmed from build files
- `android/` and `ios/` directory-status note confirmed (those directories contain only
  CLAUDE.md)
- `fastlane-config/` listed as a top-level directory (confirmed — it is not a subdirectory
  of `fastlane/`)
- `core-base/` module list matches `settings.gradle.kts`
- Cross-references to `feature/CLAUDE.md` and `build-logic/CLAUDE.md` are valid

**Minor notes:**
- `cmp-web/` and `cmp-desktop/` listed as "platform/surface entry points" — these follow
  the established `cmp-*` naming pattern and are present in the repo shape, but were not
  confirmed from their own build files. Inference, but low risk (pattern is clearly
  established by `cmp-android/` and `cmp-ios/`).

**No removals needed.**

---

## 3. core-CLAUDE-draft.md — PASS

All claims confirmed against `settings.gradle.kts`, `core/network/build.gradle.kts`,
and `build-logic/convention/build.gradle.kts`.

**Confirmed:**
- core/ module table (9 modules) matches `settings.gradle.kts`
- core-base/ module table (8 modules) matches `settings.gradle.kts`
- `core-base:database` is the only database module (correct — core/ has no database module)
- Ktorfit over Ktor confirmed from `core/network/build.gradle.kts`
- `kmp.supabase.config` plugin applied to `core:network` confirmed
- `core:network` depends on `core:common`, `core:model`, `core:datastore` confirmed
- Koin wiring via `org.convention.kmp.koin` confirmed
- "all 25 feature modules consume `core:data` through the convention plugin" — confirmed;
  core:ui and core:designsystem are also wired by convention plugin, so the statement
  is accurate

**Supabase caution** is appropriately hedged ("not yet fully documented"). Correct.

**No removals needed.**

---

## 4. build-logic-CLAUDE-draft.md — PASS with one minor path note

All plugin tables and build constraints confirmed.

**Confirmed:**
- 13 registered plugins in `build-logic/convention/build.gradle.kts` (verified via grep;
  previous session summary incorrectly said "14" — the draft tables are accurate at 13)
- JDK 17 confirmed from `build-logic/convention/build.gradle.kts`
- PR check order (build-logic → spotlessCheck → detekt → dependencyGuard) confirmed
  from `.github/workflows/pr-check.yml`
- Git hooks installed from `scripts/` confirmed from `MifosGitHooksConventionPlugin.kt`
  (copies `*.sh` files from `${project.rootDir}/scripts/` to `.git/hooks/`)

**Minor note:**
- Line 73: "library versions in `libs.versions.toml`" — the actual file is at
  `gradle/libs.versions.toml`, not root-level. The reference is understandable in
  context (Gradle version catalog is a known convention) but adding the path
  `gradle/libs.versions.toml` would be more precise.

**No removals needed.**

---

## 5. cmp-shared-CLAUDE-draft.md — FLAG (one inference not hedged)

Most content confirmed. One unhedged inference.

**Confirmed:**
- `commonMain`, `iosMain`, `nativeMain` source sets confirmed from `cmp-shared/build.gradle.kts`
- CocoaPods framework: `ComposeApp`, static, iOS 16.0, Podfile at `../cmp-ios/Podfile` —
  all confirmed from `cmp-shared/build.gradle.kts`
- Core deps (`projects.core.domain`, `projects.core.data`, `projects.core.network`,
  `projects.coreBase.ui`) confirmed from `cmp-shared/build.gradle.kts`
- All 25 feature modules in dependency list confirmed from `cmp-shared/build.gradle.kts`
- `libs.mifosPasscode` dependency confirmed

**FLAG — unhedged inference:**
- Line 19: "`commonMain` — shared app assembly, navigation host, and composition"
  — "navigation host" is an inference. The `cmp-shared/build.gradle.kts` confirms this
  module assembles all features and declares the CocoaPods framework, but the source
  code in `commonMain` was not read to confirm a navigation host lives there.
  **Correction:** Soften to "shared app assembly and composition" and add "(likely
  includes the top-level navigation host, but not verified from source)" or simply
  remove "navigation host" entirely to stay within confirmed facts.

**No other removals needed.**

---

## 6. android-CLAUDE-draft.md — PASS

All facts in the Known Facts section confirmed.

**Confirmed:**
- Namespace / applicationId `org.mifospay` confirmed from `cmp-android/build.gradle.kts`
- Build types: `debug` (suffix `.debug`), `release` (no suffix) — confirmed from
  `build-logic/convention/src/main/kotlin/org/mifospay/AppBuildType.kt`
- Flavors: `prod` (no suffix), `demo` (suffix `.demo`), dimension `contentType` —
  confirmed from `AppFlavor.kt`
- Direct dependencies: `projects.cmpShared`, `projects.core.data`, `projects.core.ui`
  confirmed from `cmp-android/build.gradle.kts`
- Signing env vars confirmed
- ProGuard disabled, open issue #1815 confirmed
- `google-services.json` location confirmed
- Directory-status note (android/ contains only CLAUDE.md, real shell is cmp-android/)
  is correct

**No removals needed.**

---

## 7. ios-CLAUDE-draft.md — PASS

All facts in the Known Facts section confirmed.

**Confirmed:**
- Xcode workspace `cmp-ios/iosApp.xcworkspace`, project `cmp-ios/iosApp.xcodeproj`,
  Podfile `cmp-ios/Podfile` — all confirmed from `cmp-ios/` filesystem
- CocoaPods framework name `ComposeApp` (static, produced by `cmp-shared`) confirmed
  from `cmp-shared/build.gradle.kts`
- iOS deployment target 16.0 confirmed from `cmp-shared/build.gradle.kts`
- `cmp-ios/Configuration/` exists and has not been fully audited — appropriately noted
  with caution
- Directory-status note (ios/ contains only CLAUDE.md, real shell is cmp-ios/) is correct

**No removals needed.**

---

## 8. scripts-CLAUDE-draft.md — FLAG (inaccurate section claim)

The root-level scripts table is accurate. The `scripts/` table is accurate in content
but the section framing is factually wrong for two of the listed scripts.

**FLAG — inaccurate claim:**
- "All scripts in `scripts/` are iOS-focused. Confirmed from `scripts/` filesystem"
  — **INCORRECT.** `pre-commit.sh` and `pre-push.sh` are platform-agnostic git hooks.
  Reading `pre-commit.sh` confirms: it checks the current branch name and runs
  `./gradlew spotlessApply` — no iOS references, no Xcode, no CocoaPods, no Fastlane.
  `pre-push.sh` similarly has no iOS references.
  These scripts are installed for all developers on any platform via `MifosGitHooksConventionPlugin`.
  **Correction:** Change the section framing to: "Scripts in `scripts/` include both
  platform-agnostic git hooks (used by all developers) and iOS deployment scripts."
  Optionally split the table into two groups: (1) Git hooks (pre-commit.sh, pre-push.sh),
  (2) iOS deployment and verification (the remaining 8 scripts).

**Confirmed (correct items):**
- Root-level scripts table matches known filesystem (keystore-manager.sh, generateModuleGraphs.sh,
  ci-prepush.sh, ci-prepush.bat, sync-dirs.sh, bootstrap-claude-setup-v2.sh)
- `keystore-manager.sh` high-risk flag is appropriate
- Git hooks installed from `scripts/` via `MifosGitHooksConventionPlugin` confirmed
  from source (copies `*.sh` from `${project.rootDir}/scripts/` to `.git/hooks/`)
- iOS deployment scripts (deploy_firebase.sh, deploy_testflight.sh, deploy_appstore.sh,
  setup_ios_complete.sh, setup_apn_key.sh, verify_ios_deployment.sh, verify_apn_setup.sh,
  check_ios_version.sh) — names are consistent with iOS scope; purposes match
  the script table

**No other removals needed.**

---

## 9. github-CLAUDE-draft.md — PASS

Verified in prior session. All key issues resolved.

**Confirmed:**
- All 8 broken `docs/claude/` and `docs/analysis/` link instances removed
- Both navigation links removed
- Additional Workflows section added with all 6 previously undocumented workflows,
  using confirmed triggers and purposes from workflow file headers
- Core workflow documentation unchanged (pr-check.yml, release-publish.yml, nightly-CI.yml
  all retain their confirmed facts)

**No removals needed.**

---

## 10. fastlane-CLAUDE-draft.md — PASS

Verified in prior session. All key issues resolved.

**Confirmed:**
- Line count corrected: 755 → 815 (confirmed from `fastlane/FastFile`)
- Lane count corrected: reflects 18 total lanes (8 Android public + 5 iOS public +
  5 internal)
- Internal Lanes section added
- File structure diagram corrected: `fastlane-config/` shown as a top-level directory,
  not a subdirectory of `fastlane/` (confirmed from filesystem)
- All broken `docs/claude/` and `docs/analysis/` link instances removed
- Navigation links removed

**No removals needed.**

---

## Summary

| Draft | Status | Required fix before filing |
|-------|--------|---------------------------|
| feature-CLAUDE-draft.md | PASS | Soften `core:domain` blast radius wording (minor) |
| root-CLAUDE-draft.md | PASS | None |
| core-CLAUDE-draft.md | PASS | None |
| build-logic-CLAUDE-draft.md | PASS | Add path `gradle/libs.versions.toml` for precision (minor) |
| cmp-shared-CLAUDE-draft.md | FLAG | Remove or hedge "navigation host" in commonMain description |
| android-CLAUDE-draft.md | PASS | None |
| ios-CLAUDE-draft.md | PASS | None |
| scripts-CLAUDE-draft.md | FLAG | Fix "All scripts in `scripts/` are iOS-focused" — git hooks are platform-agnostic |
| github-CLAUDE-draft.md | PASS | None |
| fastlane-CLAUDE-draft.md | PASS | None |

### Required changes before any draft is filed as a real CLAUDE.md

1. **cmp-shared-CLAUDE-draft.md, line 19:** Remove "navigation host" or hedge it.
   — Unverified inference presented as confirmed fact.

2. **scripts-CLAUDE-draft.md, section header:** Change "All scripts in `scripts/`
   are iOS-focused" to an accurate description. pre-commit.sh and pre-push.sh are
   confirmed platform-agnostic git hooks; they are not iOS-specific.

### Recommended (minor, not blocking)

3. **build-logic-CLAUDE-draft.md, line 73:** Specify the full path
   `gradle/libs.versions.toml` rather than just `libs.versions.toml`.

4. **feature-CLAUDE-draft.md, blast radius section:** Clarify that `core:domain`
   is consumed transitively via `core:data`, not directly through the convention plugin.
