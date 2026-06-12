# CLAUDE.md - Claude Code entrypoint

@AGENTS.md

## Claude Code specific

The shared contract (including this repo's financial-safety rules and Research->Plan->Implement operating mode, formerly in this file) now lives in `AGENTS.md` - root and per folder - so Codex and Claude Code read one rulebook. Every `CLAUDE.md` is only a shim.

- Per-folder `CLAUDE.md` files are one-line `@AGENTS.md` imports; folder content lives in the sibling `AGENTS.md`.
- This repo's custom subagents live in `.claude/agents/` (planner, researcher, reviewer, android-implementer, ios-implementer) and its domain skills in `.claude/skills/` (payment-safety-rules, wallet-domain-rules, api-contract-check, kmp-surface-impact, shared-contract-safety, mifosx-legacy-patterns, implement-plan, research-ticket). They are unchanged and complement the kit workflows below.
- Use Claude subagents where `engineering/agents/AGENTS.md` says fan-out pays (review lenses, research); keep implementation sequential by default.

## Kit workflows

| Claude command | Shared workflow |
|---|---|
| `/start-session` | `workflows/agent/start-session.md` |
| `/build-ft` | `workflows/agent/build-ft.md` |
| `/update-context` | `workflows/agent/update-context.md` |
| `/setup-permissions` | `workflows/agent/setup-permissions.md` |
| `/wrap-up` | `workflows/agent/wrap-up.md` |
| `/stop-session` | `workflows/agent/stop-session.md` |
