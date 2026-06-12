---
name: build-ft
description: Drive a feature through the gated SDLC. Claude Code wrapper for the shared build-ft workflow.
---

# build-ft

This Claude Code skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/build-ft.md
```

Rules for this wrapper:

- Use Claude Code subagents for bounded SDLC phases when available and useful.
- If subagents are not available, execute the shared workflow sequentially without skipping gates.
- Keep the mandatory user approval gates for the feature brief and technical plan.
- Record progress in `engineering/features/FT-NNN/`.
- Do not add feature-specific rules to this wrapper; update the shared workflow if the procedure changes for all agents.
