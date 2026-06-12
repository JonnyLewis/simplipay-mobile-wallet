# Feature Status — FT-000: User Sign-Up with Email Verification  *(WORKED EXAMPLE)*

> Reference example. Delete `FT-000-example/` once you're comfortable with the format.

| Field | Value |
|---|---|
| **ID** | FT-000 (reserved for the example) |
| **Name** | User Sign-Up with Email Verification |
| **Milestone** | M2 — Accounts & Identity |
| **MVP classification** | In MVP |
| **Created** | 2026-01-15 |
| **Last updated** | 2026-01-16 |

## Phase status

| Phase | Status | Date | Notes |
|---|---|---|---|
| 1: Intake | done | 2026-01-15 | Brief approved |
| 2: Plan | done | 2026-01-15 | 5 tasks, 3 waves |
| 3: Spec Gates | done | 2026-01-15 | entities, status-models, audit-events, schema, API all updated and consistent |
| 4: Implement | done | 2026-01-16 | 5 tasks; non-enumeration enforced |
| 5: Test | done | 2026-01-16 | 18 tests passing |
| 6: Review | done | 2026-01-16 | 1 blocking (token not hashed at rest) fixed; 2 non-blocking logged. PASS |

## Overall status

`done`

## Notes

- Illustrates the full 6-phase flow on a small, universally-recognisable feature.
- Note how the brief's risk flags (sensitive data, enumeration) become explicit acceptance criteria and tests — that traceability is the point.
- The one blocking review finding (token stored unhashed) is the kind of issue Phase 6 exists to catch.
