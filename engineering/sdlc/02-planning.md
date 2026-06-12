# Phase 2 — Technical Planning

The Plan agent reads the Feature Brief and all affected spec files, then produces a task graph that implementation agents execute directly. The plan is the contract between planning and execution — implementation agents do exactly what the plan says, nothing more.

---

## Plan agent — context to load

```
engineering/AGENTS.md                    ← engineering decisions + module map
engineering/architecture/AGENTS.md       ← bounded contexts, package structure, communication rules
engineering/sdlc/02-planning.md          ← this file
engineering/features/FT-NNN/brief.md     ← the feature brief
{all spec files listed in brief §Affected components}
```

Do not load the entire repo. Load only what the brief flags.

---

## Output format — `engineering/features/FT-NNN/plan.md`

```markdown
# Technical Plan — FT-NNN: {Feature Name}

**Based on brief:** engineering/features/FT-NNN/brief.md
**Plan created:** {YYYY-MM-DD}
**Status:** draft | approved

## Spec changes required (Phase 3 inputs)
{Every spec file that must change BEFORE coding. If none, write "None."}

| File | Change description | Dependency |
|---|---|---|

## Implementation task graph

### Wave 1 — parallel (no inter-task dependencies)

#### IMPL-01: {Task name}
| Field | Value |
|---|---|
| **Module** | {module/component} |
| **Files to create** | {paths} |
| **Files to modify** | {path — what changes} |
| **Data read / written** | {tables/entities} |
| **Interface endpoints** | {endpoint this implements} |
| **Dependencies** | none |
| **Context for agent** | {minimum file list — the agent reads exactly this} |

**Acceptance criteria:** {specific, testable, bounded — names the unit and behaviour}
**Out of scope for this task:** {what the agent must NOT do}

#### IMPL-02 …

### Wave 2 — depends on Wave 1
#### IMPL-03 … (Dependencies: IMPL-01, IMPL-02)

## Test plan (Phase 5 inputs)
| Task | Test type | Key scenarios |
|---|---|---|

**Mandatory scenarios for every feature** (adapt to your product):
- Ownership/tenant isolation (if multi-tenant)
- State machine: valid transitions succeed; invalid ones rejected
- Numeric exactness (if money/quantities)
- Audit emission (if audited)
- Idempotency: duplicate key returns cached result

## Parallelism summary
| Wave | Tasks | Parallel? |
|---|---|---|

## Risk flags from brief
{Copy from brief; add implementation notes.}

## Open questions blocking implementation
{An unanswered question here is a plan blocker.}
```

---

## Planning rules

### Task granularity
- **One module per task.** Don't span components.
- **One concern per task.** Entity, service, controller as separate tasks is correct.
- **Size:** ~1–3 new units or 1–2 significant modifications per task. If it spans modules, split it.

### Dependency rules
- The graph must be a **DAG**. Circular dependencies are a plan defect — fix before presenting.
- A task may depend only on tasks in the same plan.

### Acceptance criteria rules
Each criterion is **specific** (names the unit/behaviour), **testable** (verifiable without extra context), and **bounded** (unit-testable preferred).
- Bad: "The service works correctly."
- Good: "`validate()` returns INVALID with code `TOTAL_MISMATCH` when the line sum ≠ the header total."

### Context loading rules
Each task's `Context for agent` lists the **minimum** files needed. Over-loading wastes tokens and confuses the agent.

### Spec change ordering
1. `entities.md` → 2. `status-models.md` → 3. `audit-events.md` → 4. {schema} (constraints match status-models) → 5. `api/` (enums match schema) → 6. `docs/*.md` (may run parallel with 5).

---

## Common planning mistakes
- Bundling entity + service + controller + tests into one task — split them.
- Omitting "Out of scope for this task" — agents drift without it.
- Acceptance criteria that need the full system running — keep them unit-testable.
- Forgetting the mandatory test scenarios.
- Listing spec files that aren't actually affected — every listed file gets read.
