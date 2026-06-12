# build-ft workflow

Drive one feature through the complete gated SDLC: intake, plan, spec gates, implementation, testing, review.

The active agent is the orchestrator. If the tool supports subagents, delegate bounded phases. If not, execute phases sequentially with the same gates. Do not skip gates.

## Step 1 - collect feature requirements

Read these before asking questions:

```text
engineering/AGENTS.md
engineering/architecture/AGENTS.md
engineering/sdlc/AGENTS.md
engineering/features/AGENTS.md
engineering/features/backlog.md
DEV_PLAN.md section 5
```

Ask 1-3 targeted questions:

1. What feature do you want to build? Describe what it does and the problem it solves.
2. Which milestone does this belong to? Ask only if ambiguous.
3. Any specific constraints or risks I should know upfront? Ask only for sensitive areas.

Do not ask implementation-detail questions during intake.

## Step 2 - scope fence, weight, and repo check

1. Check whether the feature is In MVP per `docs/05-mvp-scope.md`.
   - In MVP: proceed.
   - Explicitly excluded: stop, quote the exclusion, and ask whether to reclassify or redirect.
   - Ambiguous: classify as Phase 1.1 by default, tell the user, and offer to reclassify.
2. Classify the feature weight. Process overhead must scale with feature size - full ceremony on a tiny change burns tokens without reducing risk.

   | Weight | Criteria | Process |
   |---|---|---|
   | **Light** | All of: no spec changes; no new entity, state, or endpoint; expected diff ≤ ~3 files in one module; no money/auth/audit surface | Single `FT-NNN/FEATURE.md` (brief + mini-plan + status in one file); **one** approval; implement → test → single-pass review (L1 + scope check per `workflows/agent/review-lenses.md`) |
   | **Standard** | Default when any Light criterion fails | The six phases below as written; Phase 6 is the combined `06-review.md` checklist |
   | **Full** | Any of: new entity or lifecycle; money movement; auth/security surface; irreversible operations; multi-module diff | Six phases + full review-lens fan-out per `workflows/agent/review-lenses.md`; verification mandatory |

   State the chosen weight in the brief; the user can override it at approval. Escalate (never de-escalate) mid-feature if implementation reveals heavier surface than declared.
3. Check whether the code repo exists per `engineering/AGENTS.md`.
   - If yes, all phases are available.
   - If no, complete Phases 1-3 only and defer Phases 4-6 until M0 exists.

## Continuing an existing feature

If the user asks to continue or build an existing `FT-NNN`, do not create a new feature ID. Read the existing `brief.md`, `plan.md`, `status.md`, `engineering/features/backlog.md`, and `SESSION_STATE.md` before choosing the next action.

### Continuation checkpoint

1. Compare `SESSION_STATE.md`, `engineering/features/FT-NNN/status.md`, `engineering/features/backlog.md`, and `engineering/features/FT-NNN/plan.md`.
2. Inspect current repo diff/status for files touched by the active task.
3. Identify the next incomplete gate, not just the next unchecked task.
4. If files disagree, treat the most specific feature status plus on-disk evidence as the working truth, then update stale state before or during handoff.
5. Do not restart completed tasks unless gate evidence is missing or invalid.
6. Do not mark any task, phase, or feature complete from code presence alone; require gate evidence.

## Step 3 - Phase 1: feature intake

1. Assign the next feature ID by scanning `engineering/features/` for the highest `FT-NNN`.
2. Create `engineering/features/FT-NNN/`.
3. Write `brief.md` using `engineering/sdlc/01-feature-intake.md`.
4. Write `status.md` with status `draft`.
5. Present the brief and ask for approval.
6. Incorporate corrections.

Gate: feature classified, affected capabilities identified, out-of-scope listed, user approved.

Do not proceed without approval.

## Step 4 - Phase 2: technical plan

Create `engineering/features/FT-NNN/plan.md` using `engineering/sdlc/02-planning.md`.

The plan must include:

- Every task with ID, module/component, files, data touched, interface endpoints, dependencies, acceptance criteria, and out-of-scope list.
- An acyclic dependency graph.
- Spec changes in dependency order.
- Parallel task waves.
- Mandatory test scenarios.

Review the plan for scope violations, engineering-decision violations, and circular dependencies. Present the spec changes, task waves, parallelism, and test plan to the user.

Gate: user approves the plan.

Do not proceed without approval.

## Step 5 - Phase 3: spec gates

Skip only if the plan says no spec changes are required.

Update spec files in dependency order:

```text
entities -> status-models -> audit-events -> schema -> API contract -> docs
```

For each file:

1. Read `engineering/sdlc/03-spec-gates.md`.
2. Read the target file's folder `AGENTS.md`.
3. Make exactly the planned change.
4. Run the checklist for that file type.
5. Fix failures before moving to the next file.

Gate: every changed spec passes its checklist, and state/enum values match exactly across data model, schema, and API.

## Step 6 - Phase 4: implementation

If the code repo does not exist, stop here:

- Set feature status to `spec-complete`.
- Report that Phases 4-6 are deferred until M0 exists.
- Tell the user to re-run `build-ft FT-NNN` after the code repo path is recorded.

If the code repo exists:

1. Execute the task graph wave by wave, **sequentially by default**. Parallel implementation subagents cost 3-6x the tokens and only pay off for genuinely file-disjoint tasks; reserve fan-out for read-heavy phases (review, research). If parallelising anyway, worktree isolation is mandatory per `engineering/agents/AGENTS.md`.
2. For each task, load only the task's required context from the plan plus mandatory architecture and implementation docs, including the module's own `AGENTS.md` context file when one exists.
3. Gate each task against the plan: files touched, acceptance criteria, no ED violations, no out-of-scope behavior.
4. Do not start dependent tasks until dependencies pass.

After every task or wave:

1. Run the smallest relevant compile/static check, or record why none exists.
2. Apply the hung-check protocol if the check does not complete.
3. Update `engineering/features/FT-NNN/status.md` with task/wave result, checks run, and blockers before moving to a dependent wave.
4. Do not leave an agent-started dev server, watcher, test runner, build, browser, or verification process running unless it is intentionally backgrounded and the PID, purpose, port, and stop instructions are recorded.

Gate: output files match the plan; no undeclared module touched; no ED violated; every acceptance criterion has a code path and gate evidence.

## Step 7 - Phase 5: testing

Create or update tests for each completed implementation task.

Gate:

- Happy path plus at least two failure/edge cases per public unit where relevant.
- Mandatory scenarios from `engineering/sdlc/05-testing.md` covered.
- All relevant tests pass.

If tests fail, fix implementation and re-run. Do not advance with failing tests.

### Hung or inconclusive checks

A hanging check is not automatically a blocker and not a reason to write a handoff early. Treat it as a failing verification path that must be diagnosed.

Before marking testing or verification blocked:

1. Stop the hung process cleanly and record the exact command, elapsed time, and last visible output.
2. Re-run with an explicit timeout or CI-safe mode where the stack supports it.
3. Run a narrower command to isolate the layer, package, route, or test file that hangs.
4. Check common hang causes: dev server still running, port conflict, waiting for stdin, network call without mock, browser/process leak, watcher mode instead of one-shot mode, missing env var, unresolved promise, or test container startup.
5. Fix the cause when it is in repo code/config and re-run the relevant check.
6. If still blocked, produce a blocked report with the commands tried, hypotheses eliminated, remaining suspected cause, and the smallest reproduction command.

Only mark a check BLOCKED after at least three concrete diagnostic attempts or when the remaining blocker is genuinely external and cannot be changed from the repo.

## Step 8 - Phase 6: review and verification

Review the full diff using `engineering/sdlc/06-review.md`, in the mode set by the feature weight: Light = L1 + scope check, Standard = combined checklist single pass, Full = lens fan-out per `workflows/agent/review-lenses.md`.

Gate:

- Zero blocking findings.
- Non-blocking findings logged.
- Acceptance criteria verified in a running environment when available.
- Verification deferred only when the app genuinely cannot run because of a missing external dependency or environment outside repo control. Failed or hung verification is not deferred; it is blocked and must follow the diagnostic protocol.

Fix blockers and re-review before marking done.

## Step 9 - mark complete and update state

1. Update `engineering/features/FT-NNN/status.md` (or `FEATURE.md` for Light features) to the correct state: `done`, `spec-complete`, `review-blocked`, `verify-blocked`, or still `in-progress`.
2. Update `engineering/features/backlog.md`.
3. Run `scripts/check-state.sh`; fix any errors it reports.
4. If source modules changed, run `workflows/agent/update-context.md` so the module context files reflect the new code.
5. Confirm no agent-owned process is still running unless its PID, command, purpose, port, and stop instructions are recorded.
6. Follow `workflows/agent/stop-session.md` to update `SESSION_STATE.md`.
7. Report phases completed, spec files updated, implementation tasks, tests, review, verification, remaining blockers, and next feature.

## Failure handling

| Failure | Response |
|---|---|
| User rejects brief | Incorporate corrections; re-present |
| User rejects plan | Revise or re-plan; re-present |
| Spec gate fails | Fix in place and re-check |
| ED violation | Fail the task; surface it |
| Failing tests | Fix and re-run |
| Hung checks | Stop, diagnose with timeout/narrow reproduction, fix if repo-owned, and only block after the diagnostic threshold |
| State/status conflict | Reconcile `SESSION_STATE.md`, feature `status.md`, backlog, and on-disk evidence before proceeding |
| Agent-owned process still running | Stop it or record PID/purpose/port/stop instructions before handoff |
| Review blockers | Fix and re-review |
| Code repo missing | Defer Phases 4-6 and mark `spec-complete` |
| Out of MVP scope | Stop and ask whether to reclassify |
| Circular plan dependency | Re-plan before proceeding |

## Rules

- Gate every phase.
- Never use `stop-session` or a handoff as a substitute for a phase gate.
- Keep pushing verification until checks pass or the blocker is genuinely exhausted under the hung-check protocol.
- User approval is mandatory for brief and plan.
- No deferred spec work before implementation.
- Keep context bounded.
- Preserve the MVP fence.
- Log feature progress in the feature directory.
