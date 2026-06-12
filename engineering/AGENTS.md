# AGENTS.md — `/engineering`

Per-folder context for the engineering execution layer. Read after the root `AGENTS.md` for any engineering task.

## What this folder is

| Path | Purpose |
|---|---|
| `architecture/` | Architecture rules and patterns (template — adapt to the observed KMP module conventions; see `core/AGENTS.md`, `cmp-shared/AGENTS.md`) |
| `sdlc/` | The development lifecycle — phases, gates, agent roles |
| `agents/` | Context-loading guide, model routing, and the context registry |
| `features/` | Per-feature briefs, plans, status (FT-NNN lifecycle; `.ai/` stays the working-notes area) |
| `roadmap/` | Build order and live task status |

## Repositories

| Repo | Purpose | Path |
|---|---|---|
| `simplipay-mobile-wallet` (this repo) | KMP/Compose multiplatform wallet client — shape: `frontend-only` | `.` |
| Backend | External Apache Fineract API — not in any local repo; contract reference in `docs/api/` and `selfservice-collection.postman.json` | — |

## Commands (observed)

| Action | Command |
|---|---|
| Full pre-push check | `./ci-prepush.sh` (gradlew check + `spotlessApply --no-configuration-cache`) |
| Checks only | `./gradlew check` |
| Build-logic checks | `./gradlew check -p build-logic` |
| Kit consistency | `scripts/check-state.sh` |

Test baseline at import (2026-06-12): **not captured** — see SESSION_STATE open questions.

## Engineering decisions — mandatory quick reference

Violating an `(observed)` ED is a blocking defect. `(proposed)` EDs are kit conventions this codebase has not been verified against — confirm or reject them; do not enforce them in review until confirmed.

| # | Status | Decision |
|---|---|---|
| ED-1 | observed | **Backend is authoritative.** Never invent backend behavior, transaction states, APIs, DTO fields, routes, flags, or storage keys. Displayed state ≠ authoritative state. |
| ED-2 | observed | **Shared-contract blast radius.** Changes to shared models/DTOs/serializers/mappers require tracing all consumers across surfaces and stating the blast radius before editing. |
| ED-3 | observed | **Smallest safe patch.** No drive-by refactoring; preserve behavior outside the requested scope. |
| ED-4 | observed | **Research → Plan → Implement** for non-trivial work, written to `.ai/research/` and `.ai/plans/`. |
| ED-5 | observed | **Spotless formatting** via `./ci-prepush.sh` before push. |
| ED-6 | proposed | Exact money handling: amounts as exact types + currency; no float arithmetic in client code. *(Verify current handling before enforcing.)* |
| ED-7 | proposed | Duplicate-prevention on payment/transfer submission paths (idempotency keys / pending-operation guards). *(Verify what exists.)* |
| ED-8 | proposed | No secrets or PII in logs or analytics events; redact at the emitter. *(Verify analytics pipeline.)* |
| ED-9 | proposed | Typed identifiers across module boundaries rather than bare strings. *(Verify current convention.)* |

## Hard constraints for all engineering agents

- No work on flagged Mifos-inherited modules without a founder decision — see `docs/05-mvp-scope.md`.
- High-risk areas (auth, passcode, payments, balances, shared contracts, token handling, amount formatting, release automation) require impact call-outs before editing — full list in `risk/AGENTS.md`.
- No secrets in code or logs; `secrets.env.template` is the only secrets surface in-repo.

## Cross-references

| | |
|---|---|
| Operating manual | [`AGENTS.md`](../AGENTS.md) |
| Scope fence | [`docs/05-mvp-scope.md`](../docs/05-mvp-scope.md) |
| SDLC | [`sdlc/AGENTS.md`](sdlc/AGENTS.md) |
| Agent context guide | [`agents/AGENTS.md`](agents/AGENTS.md) |
| Context registry | [`agents/context-index.md`](agents/context-index.md) |
| Feature tracking | [`features/AGENTS.md`](features/AGENTS.md) |
