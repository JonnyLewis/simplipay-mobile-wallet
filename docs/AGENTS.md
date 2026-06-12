# AGENTS.md — `/docs`

Per-folder context for `/docs`. Read after the root [AGENTS.md](../AGENTS.md) when working here.

## What's in this folder (observed)

This folder predates the kit and keeps its existing structure — do not renumber or reorganise existing content.

| Path | Role |
|---|---|
| `api/` | Consumed Fineract/self-service API reference (pairs with `selfservice-collection.postman.json` at repo root) |
| `architecture/` | Architecture documentation |
| `decisions/` | Decision records |
| `flows/` | User/system flow documentation |
| `readmes/`, `images/` | Supporting material |
| `QR_PAYMENT_ROUTING_APPROACH.md` | QR payment routing design |
| `00-document-index.md` | Catalogue of all docs (kit-added) |
| `05-mvp-scope.md` | **Authoritative scope fence** (kit-added) |
| `_TEMPLATE-spec.md` | Skeleton for new specs (kit-added) |

## Discipline

1. **Each doc is authoritative for its topic** — cross-reference, don't restate.
2. **Scope conflicts → `05-mvp-scope.md` wins.** Update it first; propagate second.
3. New specs use `_TEMPLATE-spec.md` and get a row in `00-document-index.md`; new decision records continue the existing `decisions/` convention.
4. Specification discipline — every major spec includes, where relevant: Purpose, Context, Scope, Out of scope, Actors, Functional requirements, Non-functional requirements, Data requirements, Workflow / lifecycle, Controls, Risks, Open questions, MVP vs. future.
5. Open questions are first-class; never present an unknown as settled. Legal/regulatory positions are marked "requires legal review".
6. Backend semantics belong to Fineract — docs here describe the client's contract *consumption*, never invented server behavior (ED-1).
