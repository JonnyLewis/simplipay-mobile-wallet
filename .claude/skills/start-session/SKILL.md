---
name: start-session
description: Bootstrap a new Claude Code session by reading shared repo context and confirming the next action. Claude Code wrapper for the shared start-session workflow.
---

# start-session

This Claude Code skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/start-session.md
```

Rules for this wrapper:

- Do not start fresh exploration.
- Load the root `AGENTS.md` through the root `CLAUDE.md` import.
- Read `SESSION_STATE.md` and the relevant per-folder `AGENTS.md`.
- Make no edits until the workflow's confirmation gate is satisfied.
