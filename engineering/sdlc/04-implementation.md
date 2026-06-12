# Phase 4 — Implementation

Each implementation agent receives a single bounded task. It reads exactly the context listed in the task's `Context for agent` field, then writes the code. No more, no less.

---

## Implementation agent — mandatory context load

```
1. engineering/AGENTS.md                       ← ED-1…ED-N, hard constraints
2. engineering/architecture/AGENTS.md          ← bounded contexts, package structure, communication rules
3. engineering/sdlc/04-implementation.md       ← this file
4. engineering/architecture/{backend|frontend}-patterns.md  ← concrete patterns for the stack
5. {spec files listed in the task's Context for agent field}
6. {the module's own AGENTS.md, if it exists}  ← what already exists; do NOT recreate
```

**Do not load files not listed in the task.** If you discover you need an unlisted file, surface it to the orchestrator — do not self-expand context.

---

## Before writing any code — answer these

1. **Which bounded context / module does this belong to?** (Check the context map.)
2. **Which layer am I in?** (Follow the project's layering — e.g. `api → application → domain → infrastructure`.)
3. **Does this cross a module boundary?** If yes, use the published-interface/port pattern — not a direct internal call.
4. **Does this touch money / exact numerics?** If yes, the project's exact type only — never floats.
5. **Does this handle secrets / sensitive data?** If yes, the project's encryption/hashing path — never plaintext, never logged.
6. **Does this produce a state transition?** If yes, the transition belongs on the domain model/aggregate and emits the audit/event records atomically.

If you can't answer one of these from the provided context, stop and surface the gap.

---

## Mandatory patterns (apply the project's concrete versions from `*-patterns.md`)

1. **State transitions live on the domain model**, not in controllers or thin services. They validate the transition and emit an event.
2. **Atomic state + audit (+ outbox)** — the entity update and its audit/event records commit in one transaction, or none do.
3. **Typed identifiers** across boundaries — no bare strings/ints as IDs.
4. **Ownership/tenant scope on every scoped query** (if multi-tenant).
5. **Sensitive data** encrypted/hashed via the shared service; never logged.
6. **Cross-module dependency via a published interface/port** — define the port in your module; the other module provides the adapter. If the adapter doesn't exist yet, declare the port and surface the dependency — do not stub around the pattern.
7. **Append-only tables are insert-only** — no update/delete methods; defensive backstop on the entity.
8. **Structured logging** — IDs not payloads; never sensitive data; appropriate level (INFO transition, WARN recoverable, ERROR unexpected).

## Task gate before completion

Before reporting a task complete:

1. Map every acceptance criterion to a concrete code path.
2. Run the smallest relevant compile/static check, or state why none exists.
3. Confirm no declared out-of-scope behavior was added.
4. Confirm any process started for implementation has stopped, or record PID, purpose, port, and stop instructions.
5. Update the feature `status.md` with task or wave progress before dependent work begins.

If a check fails or hangs, the task remains blocked or in progress and follows the Phase 5 hung-check protocol.

---

## Output format (required after every task)

```markdown
## Task {IMPL-NN} — complete / blocked

**Files created:** {path — one-line description}
**Files modified:** {path — what changed}

**Architecture compliance:**
- Package/layer structure: [x] follows the project layout
- Domain purity: [x] no framework imports where forbidden
- State transition on the model: [x] / N/A
- Atomic state + audit: [x] / N/A
- Typed IDs across boundaries: [x]
- Ownership scope on queries: [x] / N/A
- Cross-module via interface/port: [x] / N/A

**Engineering decisions applied:** ED-{N}: {how}
**Acceptance criteria:** [x] {criterion} — in {unit}
**Checks run:** {command — PASS/FAIL/BLOCKED, or N/A with reason}
**Blockers:** {None | specific}
```

---

## What implementation agents must NOT do

- Put business logic in controllers, or state-transition logic in thin services.
- Use floats for money/exact numerics.
- Call another module's internals directly — use a published interface.
- Log sensitive data.
- Add new dependencies without surfacing them as a blocker.
- Write code for Future/out-of-scope features — stop and surface the scope conflict.
- Leave `TODO`s hiding incomplete logic — surface blockers explicitly.
- Catch-and-swallow exceptions — propagate or convert to typed errors.
- Return `null` where an `Optional`/`Result`/throw is correct.
