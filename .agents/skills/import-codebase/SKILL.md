---
name: import-codebase
description: Retrofit the codebot kit onto an existing brownfield codebase at a given local path - survey the code, install the shared contract, workflows, SDLC, and per-directory context tree there. Codex wrapper for the shared import-codebase workflow.
---

# import-codebase

This Codex skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/import-codebase.md
```

Rules for this wrapper:

- The argument (or first question) is the absolute path to the target repository.
- Hard gates first: target must be a clean git repo; never blind-overwrite existing `AGENTS.md`, `AGENTS.md`, `.Codex/`, or scripts - merge per the workflow's collision policy and show diffs before writing.
- The import is additive: never modify the target's existing source code, CI, or doc content.
- Label everything inferred from code as `observed` and every unverified kit convention as `proposed`.
- Confirm the survey + interview summary with the user before writing anything to the target.
- Do not add procedure here; update the shared workflow body if the process changes for all agents.
