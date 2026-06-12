# AGENTS.md — `/fastlane`

Release automation (fastlane) for the four surfaces. Read after the root [AGENTS.md](../AGENTS.md).

**Full pre-import documentation lives verbatim in [`REFERENCE.md`](REFERENCE.md) (804 lines)** — per-lane parameters, configuration internals (`project_config.rb`), helper methods, and step-by-step common tasks. This file is the orientation layer; read the matching REFERENCE.md section before running or editing any lane.

## Lane map (orientation — details in REFERENCE.md)

| Surface | Lanes |
|---|---|
| Android | `assembleDebugApks` · `assembleReleaseApks` · `bundleReleaseApks` · `deployReleaseApkOnFirebase` · `deployDemoApkOnFirebase` · `deployInternal` · `promoteToBeta` · `promote_to_production` |
| iOS | `build_ios` · `build_signed_ios` · `deploy_on_firebase` · `beta` · `release` |

Load-bearing rules from REFERENCE.md:

- `promote_to_production` (Android) and the App Store `release` lane (iOS) require **double confirmation** — never script around it.
- All lanes consume a generated version file (see REFERENCE.md "Version Management"); iOS additionally sanitizes versions.
- Secrets are encoded for GitHub Actions via the secrets-setup flow in REFERENCE.md "Secrets Setup"; `secrets.env.template` is the only in-repo secrets surface.

## High-risk notice

Everything in this folder is on the high-risk list in [`risk/AGENTS.md`](../risk/AGENTS.md) (signing, release, store automation). Call out impact before editing; release/store actions are irreversible and require explicit approval.

## Related

`fastlane-config/` (root) · `keystores/` + `keystore-manager.sh` · deploy scripts in [`scripts/`](../scripts/AGENTS.md) (`deploy_appstore.sh`, `deploy_firebase.sh`, `deploy_testflight.sh`).
