---
name: update-context
description: Create, refresh, split, and prune per-directory context files (AGENTS.md + CLAUDE.md shims) based on what changed since the last sync. Claude Code wrapper for the shared update-context workflow.
---

# update-context

This Claude Code skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/update-context.md
```

Rules for this wrapper:

- Diff-driven only: skip scopes whose `git diff <last-synced>..HEAD` is empty; never rescan the whole repository.
- Keep the registry at `engineering/agents/context-index.md` consistent with the files on disk.
- Enforce line budgets by splitting or evicting - never by exceeding them.
- Always write the `AGENTS.md` content file and its one-line `CLAUDE.md` shim together.
- Do not add procedure here; update the shared workflow body if the process changes for all agents.
