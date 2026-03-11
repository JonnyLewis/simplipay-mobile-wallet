# Build logic guidance

This file contains guidance for work inside `build-logic/`.

## Why this area matters

`build-logic/` is high blast radius. Changes here can affect many or all modules in the repository through shared Gradle conventions, plugins, dependency behavior, versioning, and module graph behavior.

Treat all non-trivial edits here as high risk.

## Core rule

Do not casually edit build logic.

Before editing code in `build-logic/`, identify:
- which modules consume the convention or plugin
- whether the change affects Android, KMP, Compose, KSP, Ktorfit, signing, versioning, or module graph behavior
- whether the change affects CI or release flows
- whether the change affects generated code or plugin application order

## Convention plugins

All plugins are defined in `build-logic/convention/src/main/kotlin/` and
registered in `build-logic/convention/build.gradle.kts`. Build-logic targets
**JDK 17** (`sourceCompatibility`, `targetCompatibility`, and `jvmTarget` are
all set to JDK 17).

### Android plugins

| Plugin ID | Implementation class | Purpose |
|-----------|---------------------|---------|
| `mifospay.android.application` | `AndroidApplicationConventionPlugin` | Base Android app configuration |
| `mifospay.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | Compose config for Android app |
| `mifospay.android.application.flavors` | `AndroidApplicationFlavorsConventionPlugin` | `prod`/`demo` build flavor setup |
| `org.convention.android.application.firebase` | `AndroidApplicationFirebaseConventionPlugin` | Firebase plugin wiring |
| `org.convention.android.application.lint` | `AndroidLintConventionPlugin` | Android Lint configuration |

### KMP / CMP plugins

| Plugin ID | Implementation class | Purpose |
|-----------|---------------------|---------|
| `org.convention.cmp.feature` | `CMPFeatureConventionPlugin` | Standard feature module — wires Koin, Compose, Navigation, core deps |
| `org.convention.kmp.library` | `KMPLibraryConventionPlugin` | KMP library module configuration |
| `org.convention.kmp.koin` | `KMPKoinConventionPlugin` | Koin DI wiring |
| `org.convention.kmp.supabase.config` | `SupabaseConfigConventionPlugin` | Supabase configuration injection |

### Static analysis and formatting

| Plugin ID | Implementation class | Enforced in CI |
|-----------|---------------------|----------------|
| `mifos.detekt.plugin` | `MifosDetektConventionPlugin` | Yes |
| `mifos.spotless.plugin` | `MifosSpotlessConventionPlugin` | Yes |
| `mifos.ktlint.plugin` | `MifosKtlintConventionPlugin` | Yes |

### Git hooks

| Plugin ID | Implementation class | Purpose |
|-----------|---------------------|---------|
| `mifos.git.hooks` | `MifosGitHooksConventionPlugin` | Installs pre-commit and pre-push hooks from `scripts/` |

## CI and PR checks

PR checks run the following (confirmed from `.github/workflows/pr-check.yml`
via `static-analysis-check@v1.0.1`):

1. `./gradlew check -p build-logic` — build-logic self-validation
2. `./gradlew spotlessCheck` — code formatting
3. `./gradlew detekt` — Kotlin static analysis
4. `./gradlew dependencyGuard` — dependency baseline validation

**Changing any library version or adding a dependency may fail `dependencyGuard`**
until the baseline file is updated. State this explicitly in any plan that
touches library versions in `gradle/libs.versions.toml`.

## Build constraints

- **Build-logic JDK target:** 17. Confirmed from `build-logic/convention/build.gradle.kts`.
- **Versioning system:** `org.ajoberstar.reckon` (configured in `settings.gradle.kts`).
  Build-logic changes that affect version generation interact with reckon. See
  `tag-weekly-release.yml` and `monthly-version-tag.yml` for how versions flow
  into CI releases.

## Build logic rules

When working here:
- make the smallest safe change
- isolate the exact convention/plugin being modified
- do not mix style cleanup with behavior changes
- state expected downstream module impact explicitly
- be extra careful with shared plugin aliases, multiplatform conventions, and module graph behavior

## Validation expectations

If `build-logic/` changes:
- validate the smallest affected sample first
- call out all likely downstream module categories affected
- recommend broader validation when impact is repo-wide

## Avoid

- casual repo-wide convention changes
- changing plugin wiring without tracing consumers
- changing generated-code assumptions without clear validation
- mixing versioning/release/signing changes with unrelated edits
