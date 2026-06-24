---
name: setup-permissions
description: Install or merge a safe read-only tool allowlist into the project's .Codex/settings.json (ls, find, grep, rg, git status/log/diff, check-state.sh) so agents stop prompting for harmless commands. Codex wrapper for the shared setup-permissions workflow.
---

# setup-permissions

This Codex skill is a thin wrapper.

Read and follow the shared workflow body:

```text
workflows/agent/setup-permissions.md
```

Rules for this wrapper:

- Merge into any existing `.Codex/settings.json` - union the allow arrays, keep all other keys, show the diff before writing.
- Default list is read-only inspection commands only; anything that executes project code (test/build/lint) is offered but opt-in.
- Never add destructive or network commands to the allowlist, and never write deny entries by default.
- Validate the result parses as JSON before finishing.
- Do not add procedure here; update the shared workflow body if the process changes for all agents.
