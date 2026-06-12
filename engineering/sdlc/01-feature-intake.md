# Phase 1 — Feature Intake

Produces a Feature Brief that anchors all subsequent phases. The brief is the single source of truth for what the feature is, what it is not, and which parts of the system it touches.

---

## Orchestrator protocol

1. Assign the next feature ID: scan `engineering/features/` for the highest `FT-NNN`, increment.
2. Create `engineering/features/FT-NNN/`.
3. Write `brief.md` from the template below.
4. Present it. Ask: *"Does this brief accurately capture the feature? Approve to proceed to planning, or correct it first."*
5. Incorporate corrections. Do not spawn Phase 2 until the user approves.

---

## Feature Brief template

```markdown
# Feature Brief — FT-NNN: {Feature Name}

**Status:** draft | approved | in-planning | in-progress | done | cancelled
**Created:** {YYYY-MM-DD}
**Milestone:** M{N} — {Milestone name} (per DEV_PLAN.md §5)
**MVP classification:** In MVP | MVP Optional | Phase 1.1 | Phase 2 | Future

---

## Description
{One paragraph: what this feature does and why, from the user's perspective — not the engineer's.}

## Affected capabilities
{Capability codes (C-N) from docs/05-mvp-scope.md this feature contributes to.}

| Capability | Code | Notes |
|---|---|---|

## Out of scope for this feature
{Explicit list of related things NOT included. Writing these down prevents scope creep in the plan.}

## Affected components
**Spec files** (check each that applies):
- [ ] `data-model/entities.md` — new/changed entities
- [ ] `data-model/status-models.md` — new/changed states
- [ ] `data-model/audit-events.md` — new audit events
- [ ] {schema file} — schema changes
- [ ] `api/` — new/changed endpoints or schemas
- [ ] `docs/{N}-*.md` — narrative updates

**Code modules/components:**
- [ ] {module} — {what changes}

**Data / tables directly touched:**
{Be precise about reads vs writes.}

## Risk flags
{Anything touching the high-risk areas from CLAUDE.md "Human-in-the-loop rules". Mandatory where applicable.}
- [ ] {high-risk area} — {why}

## Assumptions
{Each assumption is a potential open question.}

## Open questions
{Must be answered before or during Phase 2. Unresolved questions block the plan.}

---
*This brief was approved by {{FOUNDER_NAME}} on {date}.*
```

---

## Scope fence check (before writing the brief)

1. Is the feature **In MVP** per `docs/05-mvp-scope.md`?
   - **Yes:** classify "In MVP" and proceed.
   - **In the exclusion list:** stop. Tell the user it's explicitly excluded (quote the reason). Do not proceed without a reclassification decision.
   - **Ambiguous:** classify Phase 1.1 by default; add an open question.
2. Does it need schema changes not yet in the data model? Flag and list them.
3. Does it need new interface endpoints/schemas not in `api/`? Flag and list them.

---

## Common mistakes to avoid

- Writing the brief from the engineer's perspective ("we'll add a service…") — write from the user's.
- Including roadmap features in a brief marked "In MVP" — scope creep starts here.
- Leaving "Affected components" unchecked — every box is deliberately checked or left blank ("unchecked = not affected").
- Missing risk flags — if the feature touches a high-risk area, the flag is mandatory.
