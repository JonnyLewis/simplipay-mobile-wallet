# Phase 3 — Spec Gates

Spec files are updated in dependency order before any code is written. A spec gate failure blocks Phase 4. Catching spec errors here is cheaper than catching them in implementation.

---

## Ordering (mandatory)

```
1. data-model/entities.md       (entity definition)
2. data-model/status-models.md  (lifecycle states)
3. data-model/audit-events.md   (audit event catalogue)
4. {schema file}                (DDL/migration — constraints must match status-models.md)
5. api/ contract                (enums must match the schema)
6. docs/*.md                    (narrative — may run parallel with step 5)
```

Do not start step N+1 until step N's gate passes.

---

## Spec agent — context to load

```
engineering/AGENTS.md            ← engineering decisions
{target file's own folder AGENTS.md}   ← discipline rules for that file
{the specific change from plan.md §Spec changes}
{the target file itself}
```

The agent makes exactly the change described. Nothing more.

---

## Gate checklists

### Gate: `data-model/entities.md`
- [ ] New entity has: name, description, sensitivity classification (e.g. PII / internal / public), ownership scope, key fields, relationships
- [ ] No out-of-scope fields introduced
- [ ] Numeric/money fields use the exact-type convention (e.g. `amount_minor` + `currency_code`), not domain-hardcoded names
- [ ] Sensitive fields labelled "encrypted at rest" / "hashed" where applicable

### Gate: `data-model/status-models.md`
- [ ] New states include: name, meaning, allowed transitions, triggering actor/system, audit event, failure cases, downstream impact
- [ ] State string literals match exactly what will go into the schema constraint (no case/spelling drift)
- [ ] Any lifecycle diagram updated

### Gate: `data-model/audit-events.md`
- [ ] Every new transition has a catalogued event: `event_type`, category, typical actor, payload schema, retention
- [ ] `event_type` follows `{entity}.{transition_or_action}` (e.g. `order.shipped`)
- [ ] No audit events for read-only operations — only consequential actions

### Gate: schema / migration file
- [ ] New tables follow the project's DDL conventions (ID type, timestamp type, exact numerics, engine/charset)
- [ ] Mutable tables have an optimistic-lock/version column where the project requires it
- [ ] Append-only tables have no update/version columns
- [ ] CHECK/enum constraint values match `status-models.md` **exactly** (diff them)
- [ ] New foreign keys declared
- [ ] Scoped tables have the ownership/tenant index
- [ ] Sensitive columns use the project's encrypted/hashed column types

### Gate: `api/` contract
- [ ] State enum values match the schema constraints **exactly**
- [ ] Numeric/money fields use the shared exact-type schema + unit field
- [ ] Identifier fields use the shared typed-ID schemas
- [ ] New mutating endpoints declare an idempotency-key parameter
- [ ] New async/event variants follow the project's discriminator pattern
- [ ] New event types exist in `data-model/audit-events.md` before being added here
- [ ] No vendor-specific fields in the public contract

### Gate: `docs/*.md`
- [ ] MVP vs Future classification preserved — no Future feature labelled MVP
- [ ] Legal/review markings not softened or removed
- [ ] Cross-references updated for new entities/states/events
- [ ] No out-of-scope scope introduced

---

## Handling gate failures

1. Give the spec agent the specific failure; ask for a correction.
2. Re-check after correction.
3. Don't advance to the next file until the current one passes.
4. If the failure reveals a problem with the **plan** (e.g. a requested state conflicts with the existing lifecycle): return to Phase 2, update the plan, get user re-approval, then re-run Phase 3.

---

## What spec agents must NOT do

- Change content beyond the described change.
- Refactor or reorganise spec files.
- Invent answers to open questions (flag genuine ambiguity; don't resolve it silently).
- Soften legal/review markings.
- Add out-of-scope content.
