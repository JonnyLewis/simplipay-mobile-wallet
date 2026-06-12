# AGENTS.md — `/workflows`

Per-folder context for `/workflows`. Read after the root [AGENTS.md](../AGENTS.md).

## What's in this folder

`workflows/agent/` holds the shared, tool-neutral workflow bodies for Codex, Claude Code, and future agents (installed by import-codebase, 2026-06-12). Tool-specific wrappers (`.claude/skills/`) point here rather than duplicating procedure.

| File | Purpose |
|---|---|
| `agent/start-session.md` | Session bootstrap |
| `agent/build-ft.md` | Gated, weight-tiered feature delivery |
| `agent/update-context.md` | Diff-driven context-tree maintenance |
| `agent/review-lenses.md` | Phase-6 review perspectives (L1–L5) |
| `agent/setup-permissions.md` | Safe-tool allowlist |
| `agent/wrap-up.md` | Milestone closeout |
| `agent/stop-session.md` | Session handoff |
| `agent/init-product.md`, `agent/import-codebase.md` | Bootstrap workflows (already applied to this repo; kept for upgrades) |

## Discipline

1. If a procedure applies to both Codex and Claude, update `workflows/agent/` — never `.claude/skills/` or `.codex/`.
2. Operational runbooks added here later follow the kit rule: trigger, actors, happy path, failure paths, escalation.
3. This repo's Research→Plan→Implement working notes live in `.ai/` (see root `AGENTS.md`), not here.
