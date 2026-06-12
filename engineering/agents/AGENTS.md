# AGENTS.md — `/engineering/agents`

Context-loading guide for all agents and subagents in the SDLC. Every orchestrator follows these rules when spawning agents or when executing phases sequentially in a tool without subagents.

**Core principle: agents receive the minimum context required to complete their task.** Over-loading context wastes tokens, introduces confusion, and risks an agent acting on information it should not have.

## Agent type map

| Phase | Agent type | Isolation |
|---|---|---|
| 1 Intake | Orchestrator (active agent) | none |
| 2 Plan | Plan agent or active agent | none |
| 3 Spec update | Spec agent or active agent | none (spec repo, no code) |
| 4 Implementation | Active agent, sequential by default | `worktree` only if parallelising file-disjoint tasks |
| 5 Testing | Test agent or active agent | none (separate test files) |
| 6 Code review | Review lens agents per `workflows/agent/review-lenses.md` | none (read-only) |
| 6 Verify | Verify agent | none |
| Context maintenance | `update-context` per `workflows/agent/update-context.md` | none |

In Claude Code these map to subagents/skills (`Plan`, `code-review`, `verify`, parallel lens subagents). In Codex or any tool without subagents, the active agent executes the same roles sequentially with the same context bounds and gates.

## Context loading by agent type

### Plan agent (Phase 2)
**Always:** `engineering/AGENTS.md` · `engineering/architecture/AGENTS.md` · `engineering/sdlc/02-planning.md` · `engineering/features/FT-NNN/brief.md`
**Also (from brief §Affected components):** the specific data-model/API/docs files flagged — scoped to relevant sections, not whole files.
**Never:** the entire API contract unscoped · docs not in the brief · unrelated folders.

### Spec update agent (Phase 3)
**Always:** `engineering/AGENTS.md` · `engineering/sdlc/03-spec-gates.md` · the target file's own folder `AGENTS.md` · the file being changed.
**Also:** the specific change from `plan.md`; if changing the schema, also `status-models.md` (for constraint values); if changing the API, also the relevant `status-models.md` section (for enums).
**Never:** other spec files · the codebase.

### Implementation agent (Phase 4)
**Always:** `engineering/AGENTS.md` · **`engineering/architecture/AGENTS.md`** · `engineering/sdlc/04-implementation.md` · `engineering/architecture/{backend|frontend}-patterns.md`.
**Also (from the task):** the module's own `AGENTS.md` context file (what already exists — do NOT recreate) · the specific schema sections for tables touched · the specific API sections for endpoints implemented · existing source files the task modifies.

**Module context files** (in the code repo) are `AGENTS.md` + a one-line `CLAUDE.md` shim per module, created from [`module-context-template.md`](module-context-template.md) and registered in [`context-index.md`](context-index.md). They must be read before writing code in that module. They are created at the START of the milestone that creates the module and maintained by the `update-context` workflow after every feature that touches the module (`build-ft` Step 9 and `wrap-up` both run it).

**Prompt template:**
```
You are implementing Task {IMPL-NN} for Feature FT-NNN.
Package structure, aggregate rules, and cross-module communication patterns are mandatory —
see engineering/architecture/AGENTS.md.

## Your task
{description from plan.md}
## Module
{module}
## Files to create / modify
{from plan.md, with descriptions}
## Acceptance criteria
{verbatim from plan.md}
## Out of scope for this task
{verbatim from plan.md}
## Context you have loaded
- engineering/AGENTS.md (ED-1…ED-N)
- engineering/architecture/AGENTS.md
- engineering/sdlc/04-implementation.md
- {patterns file} + {module AGENTS.md} + {spec sections}
## Constraints
- Do not read or modify files outside the declared module
- Do not implement acceptance criteria not listed
- Do not introduce new dependencies without surfacing them as a blocker
- Apply every relevant ED; if one prevents completion, stop and surface the conflict
## Output
Write the implementation, then the output summary from engineering/sdlc/04-implementation.md.
```

### Test agent (Phase 5)
**Always:** `engineering/AGENTS.md` · `engineering/sdlc/05-testing.md` · `engineering/features/FT-NNN/plan.md` (acceptance criteria) · the source files from the corresponding IMPL task.
**Prompt template:**
```
You are writing tests for Task {IMPL-NN} of Feature FT-NNN.
## Code to test
{source files}
## Acceptance criteria
{verbatim from plan.md}
## Mandatory scenarios (engineering/sdlc/05-testing.md)
{the ones that apply to this product}
## Constraints
- Test the code above — do not modify the implementation
- Use a real datastore for integration tests — do not mock it
- Every public unit needs ≥1 failure/edge case
- Run the tests and report PASS or FAIL
## Output
The test summary format from engineering/sdlc/05-testing.md.
```

### Code review agent (Phase 6)
**Always:** `engineering/AGENTS.md` · `engineering/architecture/AGENTS.md` · `engineering/sdlc/06-review.md` · `engineering/features/FT-NNN/plan.md` · the full diff.
**Prompt template:**
```
You are performing a code review for Feature FT-NNN.
## What to review
{full diff}
## Review against
- Every engineering decision in engineering/AGENTS.md (ED-1…ED-N)
- The security, contract, data-model, audit, and scope checklists in engineering/sdlc/06-review.md
- The acceptance criteria in engineering/features/FT-NNN/plan.md
## Output
Use the format from engineering/sdlc/06-review.md.
Categorise every finding BLOCKING or NON-BLOCKING. Do not pass a feature with blocking findings.
```

---

## Parallelism and isolation

**Default: sequential.** A subagent run costs roughly 3-6x the tokens of inline work and pays off only when the work is genuinely independent and read-heavy. Apply this test before any fan-out:

| Work type | Mode |
|---|---|
| Implementation tasks (write-heavy, shared state) | Sequential, inline. Parallelise only genuinely file-disjoint tasks, max 2-3, with worktree isolation |
| Review lenses on a Full-weight feature (read-only) | Parallel - this is where fan-out measurably improves recall |
| Research / exploration across many files | Parallel subagents acceptable |
| Trivial tasks (< a few minutes of work) | Never spawn a subagent |

**Worktree isolation** is mandatory when 2+ implementation agents modify the same repo in one wave. Merging: review each output summary; if all gates pass, merge all; if any fails, surface to the user before merging anything.

---

## Model routing

Match the model tier to the phase. The tiers are tool-neutral; map them in your tool's config (Claude Code: `/model`, `--model`, or `model` frontmatter on skills/agents · Codex: profiles, see `.codex/config.toml.example`).

| Work | Tier | Why |
|---|---|---|
| Phase 2 Plan · architecture decisions · L1/L2/L3 review lenses | **Top reasoning** | Errors here are the most expensive downstream |
| Phase 4 Implementation · Phase 5 Testing | **Mid/high coding** | The default coding tier |
| Spec edits · status/backlog updates · L4/L5 lenses · `update-context` runs | **Fast/cheap** | Mechanical, bounded, easily verified work |

Switching provider = keeping the tier mapping and swapping the model names. Never route Plan or security review to the cheap tier to save cost - that trade always loses.

---

## Agent handoff format

When passing output between phases, the orchestrator prepends a brief handoff note:

```
## Context from previous phases
Phase 1 (Intake): FT-NNN brief approved {date}.
Phase 2 (Plan): Plan approved {date}. {N} tasks. {N} spec changes.
Phase 3 (Spec): all changes applied; no gate failures.
Phase 4 (Implement): IMPL-01…IMPL-{N} complete. {one line per task}.
Phase 5 (Test): all passing. {N} unit, {N} integration.
Your task: Phase 6 — code review. See plan.md for acceptance criteria.
```
