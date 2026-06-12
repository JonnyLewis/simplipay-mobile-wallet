# Review lenses

Tool-neutral definitions of the Phase 6 review perspectives. Each lens is a bounded reviewer with its own focus, context, and output. Multi-lens fan-out measurably improves defect recall, but costs roughly one full review per lens - so lens count scales with feature weight, not habit.

## Lens definitions

Every lens loads `engineering/AGENTS.md` (ED-1...ED-N) + the full diff + its own rows below. Nothing else.

| Lens | Focus | Extra context | Typical findings |
|---|---|---|---|
| **L1 Correctness** | Logic errors, edge cases, error handling, swallowed exceptions, acceptance criteria coverage | `FT-NNN/plan.md` acceptance criteria | Off-by-one, unhandled failure path, criterion without a code path |
| **L2 Security** | Injection, secrets/PII in logs or code, authz gaps, input validation, leaking internals | `risk/AGENTS.md` | Unsanitised query, logged PII, missing endpoint validation |
| **L3 Architecture / ED** | Layering, module isolation, aggregate rules, every applicable ED cited by number | `engineering/architecture/AGENTS.md` | Cross-module internal access, framework types in domain, ED-3 float money |
| **L4 Scope fence** | Out-of-scope features, TODOs/flags for Future work, classification correctness | `docs/05-mvp-scope.md` | Roadmap feature smuggled in, mislabelled classification |
| **L5 Contract & data alignment** | API shapes/status codes/idempotency/correlation IDs; entity fields, enums and constraints matching specs exactly | relevant `api/` + `data-model/` sections | Enum string mismatch, missing idempotency key, wrong status code |

Each lens outputs findings in the `engineering/sdlc/06-review.md` format (BLK-NNN / NBL-NNN with file:line, rule violated, required fix).

## Fan-out by feature weight

| Feature weight (set at intake - see `workflows/agent/build-ft.md`) | Review mode |
|---|---|
| **Light** | Single pass: L1 + the quick checks of L4 |
| **Standard** | Single combined pass of the full `06-review.md` checklist (covers all lenses sequentially in one reviewer) |
| **Full** | All five lenses as independent reviewers, then aggregate |

Escalate one level when the diff touches money movement, auth, audit, or an irreversible operation - regardless of declared weight.

## Execution per tool

- **Claude Code:** run Full-tier lenses as parallel subagents (read-only, bounded context as above). This is the read-heavy case where parallel fan-out pays; implementation stays sequential.
- **Codex / no subagents:** run the lenses as sequential passes, one lens per pass, fresh attention each time. Same definitions, same gates.
- **Model routing:** L1/L2/L3 need the top reasoning tier. L4/L5 are mechanical comparisons - the fast tier is sufficient. See `engineering/agents/AGENTS.md`.

## Aggregation

1. Merge findings; de-duplicate by file:line + rule (keep the higher severity).
2. Any BLOCKING finding from any lens blocks the feature. No lens can overrule another's blocker.
3. Write the merged report in the `06-review.md` output format, attributing each finding to its lens.
4. Verification (the `verify` step) runs after a clean merged review, per `06-review.md`.
