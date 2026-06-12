# start-session workflow

Bootstrap a new agent session in this specification-first product repository.

Do not start fresh exploration. The context hierarchy exists so a new session can resume from durable repo state.

## Pre-flight

If `AGENTS.md` still contains `{{PRODUCT_NAME}}`, the product has not been bootstrapped yet. Redirect to `init-product`.

## Read the context layers

Read these in order:

1. `AGENTS.md` - shared operating manual, scope discipline, workflow map.
2. `SESSION_STATE.md` - where the last session left off, in-flight work, decisions, open questions, next concrete action.
3. The relevant per-folder `AGENTS.md` for the intended work area.

If the user's intent is unclear, default to `SESSION_STATE.md` section 4 and read the folder it points to.

## Reconcile continuation state

Before confirming the next action:

1. Run `scripts/check-state.sh` for a fast consistency report.
2. Compare `SESSION_STATE.md`, the active feature `status.md` if one is referenced, `engineering/features/backlog.md`, and the active feature `plan.md` if it exists.
3. Check whether on-disk files or current `git status` suggest newer work than the handoff describes.
4. If durable files disagree, surface the conflict clearly and ask whether to update state or continue from the most specific feature status plus on-disk evidence.
5. Do not proceed from a stale "next action" when feature status or on-disk evidence shows later progress.

## Produce a state summary

Keep the summary under about 250 words.

| Section | Content |
|---|---|
| Where we are | One paragraph from `SESSION_STATE.md` section 1 |
| What's in flight | Active workstream from section 3 |
| Locked decisions | 3-5 most relevant decisions from section 5 |
| Open questions | 3-5 most relevant questions from section 6 |
| Next concrete action | Quote section 4 verbatim |

## Confirm before changing

Ask one clear question: confirm the next concrete action from section 4, or ask what the user wants to work on instead.

Make no edits until the user confirms or redirects.

## Failure modes

| Failure | Response |
|---|---|
| `AGENTS.md` still has `{{PRODUCT_NAME}}` | Product not bootstrapped; redirect to `init-product` |
| `SESSION_STATE.md` missing | Offer to recreate it from the seed structure, flagging the discontinuity |
| State and folder context conflict | Flag it; ask whether to update state or follow the newer authoritative spec |
| On-disk work is newer than state | Flag it; inspect relevant diff/status and update handoff before continuing |
| User intent maps to no folder | Ask which area to focus on |
| Section 4 references completed work | Say so and ask whether to update state first or pick the next task |

## Rules

- Read known context files directly; do not survey the whole repo.
- No silent edits during bootstrap.
- No assumptions about intent.
- Honour the scope, specification, lifecycle, approval, and risk-control disciplines in `AGENTS.md`.
