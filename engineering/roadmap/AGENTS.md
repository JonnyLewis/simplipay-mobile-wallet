# AGENTS.md — `/engineering/roadmap`

The build roadmap. Single source of truth for what has been built, what's in progress, and what's next at the *task* level (finer-grained than the milestone view in `DEV_PLAN.md`).

---

## What's in this folder

| File | Purpose |
|---|---|
| `feature-roadmap.md` | Every coding task, organised by milestone and layer, with live status |

---

## Build order (mandatory — do not skip layers)

Within each milestone, build in this order. Never start a layer until the previous is green. (Adapt the layer names to your architecture.)

```
1. Persistence   data models → repositories → migration verified
2. Domain        aggregates → value objects → events → domain services
3. Application   use cases → ports → command/query objects
4. Interface     controllers/handlers → request/response DTOs → error mappings
5. Integration   external adapters → scheduled jobs → event listeners
```

UI follows the backend stack for a feature, not before it.

---

## Status symbols

| Symbol | Meaning |
|---|---|
| ⬜ | Not started |
| 🔄 | In progress |
| ✅ | Done — passes tests and review |
| 🚫 | Blocked — reason in the comment |
| 🔁 | Stub only — real implementation deferred |

---

## Task ID format

`M{milestone}-{layer}{seq}` — layer prefixes: `S` scaffold · `P` persistence · `D` domain · `A` application · `C` interface · `I` integration. Examples: `M0-S01`, `M1-P01`, `M2-D03`.

---

## Updating

- Starting: `⬜` → `🔄`. Done: `🔄` → `✅`. Blocked: `🚫` + comment.
- New task identified: add a row with the next ID.
- Run `stop-session` after every coding session.

---

## Cross-references

| | |
|---|---|
| Milestones | [`DEV_PLAN.md`](../../DEV_PLAN.md) |
| Engineering decisions | [`../AGENTS.md`](../AGENTS.md) |
| Architecture | [`../architecture/AGENTS.md`](../architecture/AGENTS.md) |
| Data model | [`../../data-model/`](../../data-model/) |
