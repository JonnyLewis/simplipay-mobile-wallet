# Context index - the registry of managed context files

Maintained by `workflows/agent/update-context.md`. One row per managed per-directory context file. Do not edit row data by hand - run `update-context`.

Last run: 2026-06-12 @ `bc52526c` (first real run; closed all import-time gaps)

## Code repository

| Scope | Context file | Features | last-synced | Lines | Budget |
|---|---|---|---|---|---|
| `.` (root) | `AGENTS.md` | — | bc52526c | 115 | 200 |
| `cmp-shared/` | `cmp-shared/AGENTS.md` | — | bc52526c | 59 | 150 |
| `cmp-android/` | `cmp-android/AGENTS.md` | — | bc52526c | 30 | 150 |
| `cmp-ios/` | `cmp-ios/AGENTS.md` | — | bc52526c | 29 | 150 |
| `core/` | `core/AGENTS.md` | — | bc52526c | 53 | 150 |
| `core-base/` | `core-base/AGENTS.md` | — | bc52526c | 44 | 150 |
| `feature/` | `feature/AGENTS.md` | — | bc52526c | 25 | 150 |
| `libs/` | `libs/AGENTS.md` | — | bc52526c | 34 | 150 |
| `android/` | `android/AGENTS.md` | — | bc52526c | 62 | 150 |
| `ios/` | `ios/AGENTS.md` | — | bc52526c | 46 | 150 |
| `build-logic/` | `build-logic/AGENTS.md` | — | bc52526c | 41 | 150 |
| `scripts/` | `scripts/AGENTS.md` | — | bc52526c | 51 | 150 |
| `fastlane/` | `fastlane/AGENTS.md` | — | bc52526c | 26 | 150 |
| `.github/` | `.github/AGENTS.md` | — | bc52526c | 13 | 150 |

## Below-trigger scopes (no context file — re-evaluate if they grow)

| Scope | Why no file (2026-06-12) |
|---|---|
| `cmp-desktop/` | 2 source files; trivial shell over `cmp-shared` |
| `cmp-web/` | 3 source files; trivial shell over `cmp-shared` |
| `config/` | 1 file (detekt config) |

## Linked reference docs (unmanaged, deliberately over budget)

| Doc | Linked from |
|---|---|
| `fastlane/REFERENCE.md` (804 lines) | `fastlane/AGENTS.md` (lane map distilled there) |
| `.github/REFERENCE.md` (714 lines) | `.github/AGENTS.md` (workflow list distilled there) |

## Budget summary

| Metric | Value |
|---|---|
| Total managed files | 14 |
| Total managed context lines | 628 |
| Files over budget | 0 |
| Orphaned entries | 0 |
