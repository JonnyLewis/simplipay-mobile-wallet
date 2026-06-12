# Phase 5 — Testing

Each test agent receives one completed implementation task. It writes tests that verify the acceptance criteria, then runs them. The feature does not advance to Phase 6 until all tests pass.

---

## Test agent — context to load

```
engineering/AGENTS.md                     ← engineering decisions (boundary rules, exact numerics, secrets)
engineering/sdlc/05-testing.md            ← this file
engineering/features/FT-NNN/plan.md       ← acceptance criteria for the task
{implemented source files from the task}  ← the code to test
```

Do not load spec files or other tasks' source. Test exactly what was implemented.

---

## Test types and when to write each

| Type | When | Notes |
|---|---|---|
| Unit | Every public method, domain logic, value type | Mock collaborators, not the domain |
| Repository / data integration | Every data-access method | Use a real datastore (containerised), not a mock |
| Endpoint integration | Every interface endpoint | Exercise the real request path |
| Slice / end-to-end | State-machine transitions across layers | Real datastore |

---

## Mandatory scenarios (every feature — adapt to your product)

Drop the ones that don't apply to your product; keep the rest honest.

1. **Ownership / tenant isolation** — actor A cannot read actor B's data. *(If multi-tenant.)*
2. **State machine completeness** — every valid transition succeeds; every invalid transition is rejected.
3. **Numeric exactness** — no rounding error across a large batch. *(If money/quantities.)*
4. **Audit emission** — every state transition writes its audit record in the same transaction. *(If audited.)*
5. **Idempotency** — a duplicate idempotency key returns the first result, with no duplicate insert.
6. **Append-only enforcement** — update/delete on append-only tables throws. *(If applicable.)*
7. **Optimistic locking** — a stale-version update throws the right conflict. *(If applicable.)*
8. **Sensitive-data round-trip** — encrypt→store→decrypt returns the original; stored bytes ≠ plaintext. *(If applicable.)*

---

## Test data rules

- Seed via setup hooks; never depend on data left by another test.
- Roll back unit/slice tests; clean datastore per class for integration tests.
- Use fixture factories — don't inline large object construction everywhere.
- Never use real production identifiers or real personal data in test fixtures.

---

## Test agent output format

```markdown
## Tests for Task {IMPL-NN}

**Files created:** {path} — {N} unit tests · {path} — {N} integration tests
**Acceptance criteria coverage:** [x] {criterion} — tested in {testName}
**Mandatory scenarios covered:** [x] ownership · [x] state machine · [x] numerics · [x] audit · [x] idempotency · …
**Test results:** PASS | FAIL
**Failing tests (if any):** {name} — {reason}
```

---

## Hung or inconclusive checks

A hanging test/check is a test failure mode, not an immediate handoff condition.

Before reporting BLOCKED, the agent must:

1. Stop the hung process and record command, elapsed time, and last visible output.
2. Re-run once with an explicit timeout or one-shot/CI mode.
3. Run the smallest narrower command that could reproduce it: one test file, package, route, or lint/build step.
4. Check for watcher mode, waiting for stdin, port conflicts, missing env vars, unmocked network calls, leaked browser/process handles, unresolved promises, and container startup issues.
5. Fix repo-owned causes and re-run.
6. If still blocked, report the exact attempts and the smallest reproduction command.

Do not mark testing complete, deferred, or blocked merely because the full frontend/backend command hung once. BLOCKED requires at least three diagnostic attempts or a clearly external blocker.

---

## What test agents must NOT do

- Mock the datastore to avoid real queries (use a containerised datastore for integration tests).
- Write only happy-path tests — every public unit needs ≥1 failure/edge case.
- Assert on internal implementation details — assert on observable behaviour.
- Skip ownership/tenant isolation where the product is multi-tenant.
- Leave failing tests — fix or surface as a blocker.
- Treat a hung check as exhausted without the diagnostic attempts above.
