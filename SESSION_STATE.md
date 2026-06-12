# Session State — SimpliPay Mobile Wallet

Handoff between agent sessions. Read at the start of any Codex or Claude Code session, after the root [AGENTS.md](AGENTS.md).

**Last updated:** 2026-06-12 — codebot kit imported into the brownfield repo (import-codebase).

## 1. Where we are

Brownfield codebase (KMP/Compose wallet client, Mifos Pay derivative) imported into the codebot operating system on 2026-06-12 at commit `bc52526c` (branch `vibe-dev`). The repo's pre-existing way of work (Research→Plan→Implement via `.ai/`, 5 custom agents, 8 domain skills, per-folder context files) was preserved and merged with the kit (shared AGENTS.md contract, gated SDLC, context registry, consistency checks). No source code, CI, or doc content was modified. The import has not yet been committed.

## 2. Status by area

| Area | State | Authoritative file(s) |
|---|---|---|
| Shared operating manual | ✓ merged from pre-import CLAUDE.md | [AGENTS.md](AGENTS.md) |
| Scope fence | draft — observed classification, awaits founder confirmation (OQ-4) | docs/05-mvp-scope.md |
| Engineering decisions | 5 observed + 4 proposed, unverified | engineering/AGENTS.md |
| Risk / high-risk areas | ✓ moved verbatim from pre-import CLAUDE.md | risk/AGENTS.md |
| Context tree | 9 module files registered; gaps listed in registry | engineering/agents/context-index.md |
| Feature tracking | empty backlog seeded | engineering/features/backlog.md |
| Test baseline | ⬜ not captured at import (user choice) | — |

## 3. What's in flight

The import itself: changeset complete on disk, uncommitted, awaiting user review and commit.

## 4. Next concrete action

1. Review and commit the import changeset as one commit.
2. Run `/update-context` to fill the registry gaps (`core-base/`, the four `cmp-*` surface dirs, `libs/`, `config/`) and split the two over-budget reference files (`fastlane/REFERENCE.md`, `.github/REFERENCE.md`) into budgeted context.
3. Resolve OQ-4 in `docs/05-mvp-scope.md` (confirm the Active module list) and rule on the four `(proposed)` EDs in `engineering/AGENTS.md`.

## 5. Locked decisions

| # | Decision | Source |
|---|---|---|
| D-1 | Repo shape: `frontend-only`; Fineract backend is external and authoritative | import survey 2026-06-12 (observed) |
| D-2 | `.ai/` working-notes flow and `engineering/features/FT-NNN/` lifecycle coexist | user decision 2026-06-12 |
| D-3 | Kit weight: Full (production financial app) | user decision 2026-06-12 |
| D-4 | Brief/plan approver: Jonathan | default confirmed 2026-06-12 |

## 6. Open questions

| # | Question | Where flagged | Blocking? |
|---|---|---|---|
| Q-1 | OQ-1..OQ-4: inherited-module scope + Active-list confirmation | docs/05-mvp-scope.md | OQ-4 blocks scope-fence authority |
| Q-2 | ED-6..ED-9 (money handling, idempotency, log redaction, typed IDs): confirm or reject | engineering/AGENTS.md | blocks enforcing them in review |
| Q-3 | Regulatory constraints (POPIA, SARB?) — unconfirmed, requires review | risk/AGENTS.md | no |
| Q-4 | Test baseline not captured — run `./gradlew check` once and record the result | engineering/AGENTS.md | no |

## 7. How a new session should resume

Run `start-session` (reads this file + root contract + relevant folder `AGENTS.md`, then confirms §4). Close every session with `stop-session`.
