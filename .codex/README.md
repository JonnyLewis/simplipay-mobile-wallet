# Codex notes

This folder is for Codex-specific execution notes or config examples only.

Shared project rules belong in `AGENTS.md`.

Shared workflows belong in `workflows/agent/`. To run one in Codex, ask it to follow the file, e.g. "Follow workflows/agent/build-ft.md".

`config.toml.example` shows the profile setup matching the model-routing table in `engineering/agents/AGENTS.md` (top / mid / fast tiers).

Codex merges nested `AGENTS.md` files automatically from the repo root down to the working directory - per-folder context loads with no extra steps.

Do not copy the shared operating manual into `.codex/`; that creates drift between Codex and Claude Code.
