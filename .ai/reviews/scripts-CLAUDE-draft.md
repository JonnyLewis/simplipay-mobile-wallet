# Scripts and automation guidance

This file contains guidance for work inside `scripts/` and repo automation scripts.

## Why this area matters

Scripts in this repo may affect setup, CI, signing, keystore handling, sync workflows, module graph generation, pre-push checks, and release support. These changes can have indirect high blast radius even when the script diff is small.

## Script inventory

Scripts in this repo are split across two locations.

### `scripts/` directory

Scripts in `scripts/` fall into two groups. Confirmed from `scripts/` filesystem:

**Git hooks (platform-agnostic — installed for all developers via `mifos.git.hooks` build-logic plugin):**

| Script | Purpose |
|--------|---------|
| `pre-commit.sh` | Branch protection and Spotless formatting check |
| `pre-push.sh` | Pre-push checks |

**iOS deployment, setup, and verification:**

| Script | Purpose |
|--------|---------|
| `deploy_firebase.sh` | iOS Firebase App Distribution deployment |
| `deploy_testflight.sh` | iOS TestFlight deployment |
| `deploy_appstore.sh` | iOS App Store deployment |
| `setup_ios_complete.sh` | iOS setup wizard (Match certificates, APN, provisioning) |
| `setup_apn_key.sh` | APN key configuration |
| `verify_ios_deployment.sh` | iOS deployment verification |
| `verify_apn_setup.sh` | APN setup verification |
| `check_ios_version.sh` | iOS version sanitization check (Gradle semver → App Store format) |

For iOS deployment details, also see `fastlane/CLAUDE.md`.

### Root-level scripts

These scripts live at the repo root, not in `scripts/`. Confirmed from root filesystem:

| Script | Purpose |
|--------|---------|
| `keystore-manager.sh` | Keystore and secret encoding/management — **high risk** |
| `generateModuleGraphs.sh` | Module dependency graph generation |
| `ci-prepush.sh` | CI pre-push checks (Unix) |
| `ci-prepush.bat` | CI pre-push checks (Windows) |
| `sync-dirs.sh` | Directory sync (local counterpart to `sync-dirs.yaml` workflow) |
| `bootstrap-claude-setup-v2.sh` | Repo bootstrap — purpose not yet fully documented |

`keystore-manager.sh` is the primary tool for encoding and managing secrets for
CI. It is referenced from both `.github/CLAUDE.md` and `fastlane/CLAUDE.md`.
Treat it as high risk: it handles keystore files and Base64-encoded secrets.

## Core rule

Do not casually edit scripts.

Before editing a script, identify:
- who or what calls it
- whether it is local-dev only, CI-related, release-related, or signing-related
- which paths, tools, or environment variables it depends on
- whether it interacts with secrets, keystores, generated files, or module graphs

## Script rules

When working on scripts:
- preserve existing calling conventions unless explicitly changing them
- prefer portable, readable shell behavior
- do not hard-code secrets
- do not silently change path assumptions
- do not silently change environment variable names or meanings
- call out any developer workflow impact

## Special caution

Treat these as high risk:
- pre-push / CI scripts
- signing or keystore scripts
- sync scripts
- module graph generation
- release automation
- scripts that modify repo structure or generated artifacts

## Validation expectations

If a script changes:
- describe how it was validated
- describe who is affected
- describe any environment assumptions
- call out anything not validated due to missing local prerequisites

## Avoid

- changing secrets handling casually
- changing CI assumptions without stating it
- mixing cleanup with behavior changes
- silently changing developer workflow contracts
