# AGENTS.md - shared agent contract for SimpliPay Mobile Wallet

Shared operating manual for Codex, Claude Code, and any other coding agent in this repository. Loaded every session - kept small on purpose; folder-level detail lives in per-folder `AGENTS.md` files.

SimpliPay Mobile Wallet is a Kotlin Multiplatform / Compose Multiplatform client for Fineract-based wallet services, derived from Mifos Pay (openMF mobile-wallet) and extended for SimpliPay product needs. It targets Android, iOS, Desktop, and Web.

## What this repo is

A production financial client application with shared contracts, cross-surface blast radius, and legacy constraints.

**It is not** the backend: Apache Fineract is authoritative for balances, transactions, and state. This repo never invents backend behavior, and displayed state is never assumed equal to authoritative backend state. *(Identity confirmed provisionally 2026-06-12 - refine the negative scope in `docs/05-mvp-scope.md`.)*

## Repo shape (observed)

`frontend-only` - a multiplatform client; the backend lives outside this repo.

| Area | Role |
|---|---|
| `cmp-shared/` + `cmp-android|ios|web|desktop/` | Shared UI composition + per-surface entry points |
| `feature/*` (~28 modules) | Product feature modules |
| `core/*`, `core-base/*` | Domain, data, network, model, datastore, design system, platform foundations |
| `android/`, `ios/` | Platform application support and wiring |
| `build-logic/`, `scripts/`, `fastlane/`, `.github/` | Gradle conventions, automation, release, CI |

## Scope fence

1. `docs/05-mvp-scope.md` is the authoritative scope source.
2. Mifos-inherited modules flagged there (`upi-setup`, `mpay-qr`, `mpay-qr-scan`, `fast-mpay`, `standing-instruction`) are **open questions, not active scope** - do not extend them without a founder decision.
3. Unclear scope defaults to an open question. Never silently expand.

## Financial safety non-negotiables (observed)

- Do not invent backend behavior, transaction states, APIs, DTO fields, routes, feature flags, or storage keys.
- Do not silently change auth/session behavior.
- Do not casually change shared contracts used across surfaces - trace all consumers and state the blast radius first.
- Correctness, traceability, and reversibility beat speed and elegance.
- Full high-risk area list and evidence rules: `risk/AGENTS.md`. Touching anything on that list requires calling out impact before editing.

## Operating mode

**Research -> Plan -> Implement** for any non-trivial task (observed convention - kept):

1. **Research** (no code changes): trace the exact flow through UI, state holder, domain, data, network, storage; separate confirmed facts / inferences / unknowns. Write to `.ai/research/<task>.md`.
2. **Plan**: exact files, intended change, what must not change, blast radius, validation, rollback risk. Write to `.ai/plans/<task>.md`.
3. **Implement**: smallest safe patch, only the approved plan; if findings invalidate the plan, stop and update it first.

The kit's gated SDLC layers on top: features are tracked in `engineering/features/FT-NNN/` (brief, plan, status, gate evidence) while `.ai/` remains the working-notes area. The two coexist: `.ai/` = scratch truth-finding, `FT-NNN/` = durable lifecycle.

## Multi-agent rule

The source of truth is the repository, not chat memory.

| File / folder | Role |
|---|---|
| `AGENTS.md` (root + per folder) | Shared rules and folder context - both tools load these |
| `CLAUDE.md` (root + per folder) | Thin Claude shims importing the sibling `AGENTS.md` |
| `SESSION_STATE.md` | Session handoff and next concrete action |
| `workflows/agent/` | Shared workflow bodies for all agents |
| `.claude/skills/` | Kit workflow wrappers + this repo's domain skills (payment-safety-rules, wallet-domain-rules, ...) |
| `.claude/agents/` | This repo's custom subagents (planner, researcher, reviewer, android/ios-implementer) |
| `.ai/` | Research / plans / reviews working notes |

Never duplicate shared rules into tool-specific files.

## Startup protocol

1. Read this file, then `SESSION_STATE.md`.
2. Read the per-folder `AGENTS.md` for the area being changed.
3. Resume from `SESSION_STATE.md` section 4 unless the user redirects.

## Shared workflows

| User asks for | Read and follow |
|---|---|
| `/start-session`, `resume` | `workflows/agent/start-session.md` |
| `/build-ft`, `build feature`, `implement X` | `workflows/agent/build-ft.md` |
| `/update-context`, `refresh context files` | `workflows/agent/update-context.md` |
| `/setup-permissions` | `workflows/agent/setup-permissions.md` |
| `/wrap-up`, `close milestone` | `workflows/agent/wrap-up.md` |
| `/stop-session`, `end session`, `save state` | `workflows/agent/stop-session.md` |

Claude Code invokes the `.claude/skills/` wrapper; Codex reads the workflow body directly.

## Technical context (observed)

| Concern | Choice | Status |
|---|---|---|
| Client | Kotlin Multiplatform + Compose Multiplatform | locked (observed) |
| Networking / DI | Ktor + Ktorfit, Koin | locked (observed) |
| Build | Gradle, convention plugins in `build-logic/` | locked (observed) |
| Backend | External Apache Fineract API | locked (observed) |
| Checks | `./ci-prepush.sh` (gradlew check + spotlessApply) | locked (observed) |

Engineering decisions (ED-1...ED-N, observed vs proposed): `engineering/AGENTS.md`.

## Where the disciplines live

| Topic | Authoritative file |
|---|---|
| Scope fence and module classification | `docs/05-mvp-scope.md` |
| High-risk areas, evidence rules, human-in-the-loop | `risk/AGENTS.md` |
| Engineering decisions, commands, repos | `engineering/AGENTS.md` |
| SDLC phases and gates | `engineering/sdlc/AGENTS.md` |
| Agent context loading, model routing, parallelism | `engineering/agents/AGENTS.md` |
| Feature tracking, weight tiers | `engineering/features/AGENTS.md` |
| Module context registry | `engineering/agents/context-index.md` |

## Definition of done (observed, kept)

- Exact files identified; smallest safe patch; no unrelated refactoring mixed in.
- Relevant validation run and summarised (`./ci-prepush.sh` or the narrowest relevant gradle task).
- Shared-contract and cross-surface impact called out where relevant.
- Risks and unvalidated areas stated clearly; assumptions stated; no large log dumps unless something failed.
- Reusable repo truth discovered during the task written back into context files or skills.
- `scripts/check-state.sh` passes; `SESSION_STATE.md`, feature status, and backlog consistent before handoff.
