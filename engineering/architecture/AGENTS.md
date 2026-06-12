# AGENTS.md — `/engineering/architecture`

Architecture rules for {{PRODUCT_NAME}}. Every implementation agent must read this file. These rules are enforced in the Phase 6 review checklist — violations are **blocking**.

> **Template note:** This file encodes a *mechanism* — bounded contexts, a strict layering, three (and only three) cross-module communication patterns, and a place to record decisions locked by reviews. The example below uses a layered/DDD style (the SimpliSalary reference). Replace the specifics with your stack's idioms; keep the mechanism. If your product is small, a lighter structure is fine — but write down whatever structure you choose so agents follow it.

---

## Architecture philosophy

State the one-paragraph philosophy for this product. Example (modular monolith):

> {{PRODUCT_NAME}} is a **{{ARCHITECTURE_STYLE}}**. It deploys as {{DEPLOYMENT_UNIT}}, but each module is a self-contained domain with its own model, use cases, and persistence. Modules are decoupled through events and published interfaces — not by sharing internals. This gives transactional simplicity, domain isolation, and clear ownership boundaries.

The chosen structure is **not optional** — it is the architecture.

---

## Bounded context map

Each module is a bounded context. Its language, model, and state machine belong to it alone.

| Context | Type (Core / Supporting / Generic) | Module(s) | Aggregate roots / key entities |
|---|---|---|---|
| {{Context}} | {{type}} | {{module}} | {{entities}} |

**Core-domain contexts get the most rigorous design attention.** Supporting/generic contexts may use simpler patterns.

---

## Cross-context communication — three patterns only

Modules communicate through **exactly three mechanisms**. Nothing else.

### Pattern 1 — Events (preferred for state changes)
When a change in Context A should trigger work in Context B, A emits an event; B subscribes (via an outbox/queue for reliability). This is intentional asynchronous eventual consistency.

### Pattern 2 — Published interface / port (for synchronous reads)
When A needs to read from B synchronously, A defines a **port interface in its own layer**; B implements it as an adapter. A depends on its own interface, not on B's model — this is the anti-corruption layer.

### Pattern 3 — Shared kernel (cross-cutting value types only)
A small shared module holds value objects and cross-cutting infrastructure (typed IDs, money, audit writer, encryption, request context). **Nothing in the shared kernel knows about any other module.** No business logic here.

### Forbidden
| Pattern | Why |
|---|---|
| Module A's service injected into Module B directly | Couples implementation, not interface |
| Module A's repository/data-access used by Module B | Cross-module internal access |
| One persistence entity shared between modules | One entity can't be two contexts' aggregate |
| Calling another module's domain service directly | Domain services are internal to their context |

---

## Mandatory layering — every module

State the layering and the **dependency direction**. Example:

```
{module}/
├── api/             ← inbound adapter (controllers, request/response DTOs) — thin
├── application/     ← use cases, commands/queries, ports (cross-module interfaces)
├── domain/          ← aggregates, value objects, events, repository interfaces — pure, no framework
└── infrastructure/  ← persistence entities, repo impls, mappers, external adapters
```

**Dependency rule (strictly enforced):**
```
api → application → domain ← infrastructure
```
- `domain` depends on nothing (no framework, no other modules).
- `application` depends on `domain`.
- `infrastructure` depends on `domain` (implements its interfaces).
- `api` depends on `application` (+ `domain` for response mapping).
- **No reverse dependencies. No cycles.**

---

## Aggregate / domain-model rules

1. **One aggregate root per aggregate.** External code touches only the root.
2. **State transitions are methods on the root** — not in services or controllers. They guard the transition and raise an event.
3. **Aggregates raise events**; the application service pulls and publishes them after save.
4. **Aggregates are small** (>3–4 entities → probably two aggregates).
5. **Aggregates reference other aggregates by ID only** (a typed-ID value object), never by object reference.

## Value object rules

1. **Immutable** (record / final + no setters).
2. **Self-validating** (constructor throws on invalid input).
3. **Equality by value.**
4. **Typed IDs are value objects** — never pass bare strings/ints across boundaries.

## Domain purity rule

Domain classes must not import framework/persistence types where the project forbids it (so the domain is unit-testable with no container). Framework annotations belong in `application`/`infrastructure` only.

---

## Locked implementation decisions (from reviews)

Every Phase-6 blocking finding that becomes a rule is recorded here, with a correct/wrong example. The test: *could a future agent repeat the mistake?* If yes, it belongs here.

> {{Empty until your first review locks a pattern. wrap-up adds them. Example shape:}}
>
> ### D-NN — {short title} (from M{N} review)
> {rule}. Correct vs wrong:
> ```
> // CORRECT … 
> // WRONG …
> ```

---

## Scope separation in code

**If it's not In MVP per `docs/05-mvp-scope.md`, it does not exist in the codebase.** No TODOs for Future features, no feature flags for roadmap items, no half-built stubs. Defer decisions in `docs/05` or `SESSION_STATE.md §6` — not in the code.

---

## Cross-references

| | |
|---|---|
| Engineering decisions | [`../AGENTS.md`](../AGENTS.md) |
| Backend patterns | [`backend-patterns.md`](backend-patterns.md) |
| Frontend patterns | [`frontend-patterns.md`](frontend-patterns.md) |
| Implementation rules | [`../sdlc/04-implementation.md`](../sdlc/04-implementation.md) |
| Review checklist | [`../sdlc/06-review.md`](../sdlc/06-review.md) |
