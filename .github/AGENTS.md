# AGENTS.md — `/.github`

CI/CD workflows. Read after the root [AGENTS.md](../AGENTS.md).

**Full pre-import documentation lives verbatim in [`REFERENCE.md`](REFERENCE.md) (714 lines)** — per-workflow detail, all 13 custom actions (4 Android, 4 iOS, 2 macOS, 1 desktop, 1 web, 1 static-analysis), the secret categories and file-to-secret mapping, and a troubleshooting/debugging guide. This file is the orientation layer; read the matching REFERENCE.md section before editing any workflow or action.

## Workflows (observed)

`pr-check.yml` (the PR gate) · `multi-platform-build-and-publish.yml` · `build-and-deploy-site.yml` · `upload-demo-app-on-firebase.yaml` · `promote-to-production.yml` · `tag-weekly-release.yml` · `monthly-version-tag.yml` · `sync-dirs.yaml` · `cache-cleanup.yaml`.

## High-risk notice

Release, publish, and promotion workflows are on the high-risk list in [`risk/AGENTS.md`](../risk/AGENTS.md). The import is additive: agents must not modify CI behavior without an approved plan and explicit user approval for anything that deploys or publishes.
