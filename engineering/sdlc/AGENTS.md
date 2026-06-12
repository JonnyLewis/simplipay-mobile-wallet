# AGENTS.md — `/engineering/sdlc`

The agentic Software Development Lifecycle. Every agent that participates in feature delivery must read this file.

---

## SDLC overview

Six phases, ordered. Each has a gate — work does not advance until the gate passes.

```
Phase 1: Intake      → Feature brief created and user-approved
Phase 2: Plan        → Task graph created and user-approved
Phase 3: Spec Gates  → Spec files updated (data-model, API, docs) — if required
Phase 4: Implement   → Code written per the task graph
Phase 5: Test        → Tests written and passing
Phase 6: Review      → Code reviewed; verify agent confirms behaviour
```

**The orchestrator** (`workflows/agent/build-ft.md`) drives the sequence. Tools with subagents may delegate bounded phases; tools without subagents execute the phases sequentially. In both modes, the gates are mandatory.

---

## Phase 1 — Feature Intake
**Who:** Orchestrator · **Output:** `engineering/features/FT-NNN/brief.md`

1. Assign the next feature ID (`FT-NNN`, zero-padded).
2. Create the feature directory; write `brief.md` per [`01-feature-intake.md`](01-feature-intake.md).
3. Present to the user. Ask for approval before planning.

**Gate:** feature classified (In MVP / Phase 1.1 / …) · affected capabilities identified · out-of-scope listed · user approved.

---

## Phase 2 — Technical Plan
**Who:** Plan agent or active agent · **Input:** `brief.md` + affected spec files · **Output:** `plan.md`

Produce a task graph per [`02-planning.md`](02-planning.md): each task self-contained, dependencies form a DAG, acceptance criteria testable.

**Gate:** every task has ID/module/files/deps/acceptance/out-of-scope · DAG is acyclic · spec changes listed · parallelism identified · test plan included · user approved.

---

## Phase 3 — Spec Gates
**Who:** Spec agent or active agent, one pass per spec file · **Output:** updated spec files

Update spec files in dependency order (entities → status-models → audit-events → schema → API contract → docs). Run the per-file-type gate checklist from [`03-spec-gates.md`](03-spec-gates.md). Catching spec errors here is far cheaper than in code.

**Gate:** each spec file passes its checklist; state/enum values match exactly across data model, schema, and API.

---

## Phase 4 — Implementation
**Who:** Implementation agent or active agent, one pass per task · **Isolation:** worktree for parallel same-repo tasks

Execute the task graph wave by wave. Each agent reads exactly its task's context (see [`04-implementation.md`](04-implementation.md)) and the mandatory patterns. Merge a wave only when all its gates pass.

**Gate:** output files match the plan · no files outside the declared module touched · no ED violated · no out-of-scope features · every acceptance criterion has a code path.

---

## Phase 5 — Testing
**Who:** Test agent or active agent, one pass per task · **Output:** test files

Write tests for each task's acceptance criteria plus the mandatory scenarios from [`05-testing.md`](05-testing.md). Run them; all must pass before Phase 6.

**Gate:** happy path + ≥2 failure/edge cases per public unit · mandatory scenarios covered · all tests pass.

---

## Phase 6 — Review & Verification
**Who:** review agent(s) + verify agent · **Output:** findings + verification result

Review the full diff in the mode set by the feature weight: Light = L1 + scope check, Standard = the combined [`06-review.md`](06-review.md) checklist in one pass, Full = the lens fan-out in [`../../workflows/agent/review-lenses.md`](../../workflows/agent/review-lenses.md). Then, if the app runs locally, verify the acceptance criteria in the running app.

**Gate:** zero blocking findings · all acceptance criteria verified PASS. If verification is blocked or deferred, the feature is not done; record the blocker in feature `status.md` and `SESSION_STATE.md`.

---

## Agent context loading rules

Full detail: [`../agents/AGENTS.md`](../agents/AGENTS.md). Minimum context per agent type:

| Agent | Always reads | Also reads |
|---|---|---|
| Intake / Orchestrator | `AGENTS.md` · `engineering/AGENTS.md` | `DEV_PLAN.md` |
| Plan | `engineering/AGENTS.md` · `architecture/AGENTS.md` · `sdlc/02-planning.md` | `brief.md` + spec files in brief |
| Spec | `engineering/AGENTS.md` + target file's `CLAUDE.md` | the file being changed |
| Implementation | `engineering/AGENTS.md` · **`architecture/AGENTS.md`** · `architecture/{backend|frontend}-patterns.md` · `sdlc/04-implementation.md` | the task from `plan.md` + module `CLAUDE.md` |
| Test | `engineering/AGENTS.md` · `sdlc/05-testing.md` | implemented code + acceptance criteria |
| Review | `engineering/AGENTS.md` · **`architecture/AGENTS.md`** · `sdlc/06-review.md` | the full diff |

**Agents must not read the entire repository.** Load only what's listed plus files the task references.

---

## Feature ID format

`FT-NNN` — zero-padded 3-digit integer, assigned sequentially. Each feature lives in `engineering/features/FT-NNN/`.

---

## SDLC failure modes

| Failure | Response |
|---|---|
| Incomplete task graph | Return to Phase 2; clarify scope |
| Spec gate fails | Spec agent fixes; re-check before Phase 4 |
| Implementation violates an ED | Fail the task; surface; don't merge |
| Failing test | Implementation agent fixes; re-run |
| Hung or inconclusive check | Stop, diagnose with timeout/narrow reproduction, fix repo-owned causes, and only block after the diagnostic threshold |
| Blocking review finding | List blockers; don't merge; await user decision |
| Code repo absent | Defer Phases 4–6; complete 1–3; record in status |
