---
name: stop-session
description: Update SESSION_STATE.md at the end of a productive session. Codex wrapper for the shared stop-session workflow.
---

# stop-session

This Codex skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/stop-session.md
```

Rules for this wrapper:

- Update `SESSION_STATE.md` only unless the user explicitly asks for other changes.
- Use targeted edits.
- Reflect actual session work; do not invent progress.
- Print the bootstrap message only after the workflow's confirmation gate.
