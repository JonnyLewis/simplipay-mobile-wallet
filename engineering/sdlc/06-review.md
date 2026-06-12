# Phase 6 — Review & Verification

The final gate before a feature is marked done. Two independent checks: code review (structural + correctness) and live verification (behaviour in the running app).

**Review mode by feature weight** (set at intake): Light = L1 correctness + scope check only · Standard = this checklist in a single pass · Full = independent lens reviewers per [`../../workflows/agent/review-lenses.md`](../../workflows/agent/review-lenses.md), merged. Escalate one level if the diff touches money, auth, audit, or irreversible operations.

---

## Review agent — context to load

```
engineering/AGENTS.md                     ← engineering decisions (ED-1…ED-N) + hard constraints
engineering/architecture/AGENTS.md        ← architecture rules being enforced
engineering/sdlc/06-review.md             ← this file
engineering/features/FT-NNN/plan.md       ← acceptance criteria to verify
{full diff for the feature}
```

---

## Finding severity

| Severity | Definition | Effect |
|---|---|---|
| **Blocking** | Violates an ED, causes data loss, security issue, breaks the contract, or introduces out-of-scope work | **Must be fixed before merge** |
| **Non-blocking** | Suboptimal but correct; style; minor improvement | **Documented; merged with an open item** |

---

## Review checklist

### Architecture (all blocking)
- [ ] Package/layer structure followed; no class in the wrong layer
- [ ] Domain layer free of framework annotations where the project forbids them
- [ ] State transitions are methods on the domain model, not in services/controllers
- [ ] State transitions raise events; the use case publishes them after save
- [ ] Typed IDs across boundaries — no bare strings/ints as IDs
- [ ] Value objects immutable and self-validating
- [ ] Repository/data interface in the domain layer; implementation in infrastructure
- [ ] Persistence entities don't leak into application/domain layers
- [ ] Cross-module communication only via events or published interfaces — no reaching into internals
- [ ] Thin controllers — input validation + delegation only
- [ ] No out-of-scope work; no TODO/flags for Future features

### Engineering decisions (ED-1…ED-N)
- [ ] Walk every ED that applies to this diff and confirm compliance. Cite the ED number for each violation.

### Security
- [ ] No queries built from unsanitised input
- [ ] No secrets/PII in logs
- [ ] No secrets in code/config (reference a secret manager)
- [ ] No permissive CORS / auth bypass
- [ ] Exception messages don't leak internals to callers
- [ ] Input validation present on every endpoint

### Contract alignment
- [ ] Response shapes match the API contract (names, types, required)
- [ ] Status codes correct (201 create, 200 read, 204 delete, 409 conflict, …)
- [ ] Idempotency-key processed on mutating endpoints
- [ ] Correlation ID propagated and returned
- [ ] Errors use the typed error model — no free-text codes

### Data-model alignment
- [ ] Entity field names/constraints match the schema exactly
- [ ] State-transition code covers all constraint values
- [ ] Indexes/unique constraints present where the schema declares them

### Audit completeness *(if audited)*
- [ ] Every transition emits an audit record with correct type/actor/entity/payload
- [ ] Audit integrity field (hash etc.) computed correctly
- [ ] Audit is in the same transaction as the state change

### Scope fence
- [ ] No out-of-scope features introduced
- [ ] Feature correctly classified per `docs/05-mvp-scope.md`

### Code quality (non-blocking unless severe)
- [ ] No TODOs hiding incomplete logic
- [ ] Structured logging only (no stray prints / stack-trace dumps)
- [ ] No caught-and-swallowed exceptions
- [ ] No magic strings for business values
- [ ] Every new public unit has ≥1 test; integration tests don't mock the datastore

---

## Review output format

```markdown
# Code Review — FT-NNN: {Feature Name}
**Date:** {YYYY-MM-DD} · **Diff:** {reference}

## Blocking findings ({N})
### BLK-001: {title}
**File:** {path}:{line}
**Rule violated:** ED-{N} / Security / Contract / Scope fence
**Description:** {specific problem}
**Required fix:** {exactly what must change}

## Non-blocking findings ({N})
### NBL-001: {title}
**File:** {path}:{line} · **Recommendation:** {suggestion}

## Summary
**Result:** PASS (no blocking) | FAIL ({N} blocking)
**Merge decision:** Ready | Blocked — fix BLK-… first
```

---

## Verification agent

After a clean review, spawn a `verify` agent **only if the app runs locally**.

**Context:** `engineering/sdlc/06-review.md` (this section) + `plan.md` (acceptance criteria).

**Protocol:**
1. Start the app per the project's run command.
2. For each acceptance criterion in `plan.md`: execute the scenario; record pass / fail / blocked.
3. Write `engineering/features/FT-NNN/verify.md`.

**If verification hangs:** do not immediately hand off. Stop the hung command, capture elapsed time and last output, retry with an explicit timeout or one-shot mode, narrow to the smallest reproducible scenario, and inspect common causes (port conflict, watcher mode, missing env var, unmocked network call, leaked browser/process handle, unresolved promise). Fix repo-owned causes and retry. Mark BLOCKED only after at least three concrete diagnostic attempts or a genuinely external blocker.

```markdown
# Verification — FT-NNN: {Feature Name}
**Environment:** local | staging · **Date:** {YYYY-MM-DD}

| Criterion | Result | Notes |
|---|---|---|

**Overall:** PASS | FAIL | BLOCKED
**Blockers (if any):** {description, diagnostic attempts, smallest reproduction command}
```

---

## Feature completion

A feature is **done** when:
1. [ ] Code review: zero blocking findings
2. [ ] All tests pass
3. [ ] Verification: all criteria PASS
4. [ ] `engineering/features/FT-NNN/status.md` set to `done`
5. [ ] `SESSION_STATE.md` updated via `stop-session`

If verification is BLOCKED, the feature is not done. Keep it `in-progress`, `review-blocked`, or `verify-blocked`, document diagnostic attempts and the smallest reproduction command, and make resolving the blocker the next concrete action.

If the code repo doesn't exist yet, a feature can only reach `spec-complete` (Phases 1–3). Mark Phases 4–6 `deferred — M0 not yet complete`.
