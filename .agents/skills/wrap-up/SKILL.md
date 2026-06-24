---
name: wrap-up
description: Close a milestone by refreshing durable agent context, writing a report, and updating session state. Codex wrapper for the shared wrap-up workflow.
---

# wrap-up

This Codex skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/wrap-up.md
```

Rules for this wrapper:

- Treat `AGENTS.md` as the shared source of truth.
- Keep `AGENTS.md` thin and Codex-specific.
- Refresh the context tree described by the shared workflow.
- Commit only if the user explicitly asks.
