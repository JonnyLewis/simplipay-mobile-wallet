# AGENTS.md — `/risk`

Risk, control, and safety catalogues for a production financial client. The high-risk list and evidence rules below were moved verbatim from the pre-import root `CLAUDE.md` (2026-06-12) — they are observed, load-bearing conventions.

## High-risk areas (observed)

Treat these as high risk and call out impact before editing:

- auth, passcode, OTP, biometrics, session restore, session refresh
- wallet balance source of truth
- payments, transfers, top-up, cash-in, cash-out
- transaction history, receipts, and status mapping
- shared models, DTOs, serializers, and mappers
- network request/response contracts
- token handling, secure storage, encryption
- retry behavior, idempotency, duplicate prevention
- background work, sync, and pending-operation handling
- amount formatting, fee handling, rounding
- Gradle conventions, signing, release, and automation scripts

## Evidence rules (observed)

Research must reference exact file paths, classes, functions, routes, screens, models, mappers, plugins, or scripts. Every research note separates **Confirmed facts / Inferences / Unknowns**; anything inferred is said to be inferred.

Do not invent: APIs · DTO fields · routes · feature flags · transaction states · storage keys · Gradle behavior · surface-specific behavior · validation assumptions.

## Shared-code rules (observed)

If a task touches shared code: assume multiple surfaces are affected; trace all known consumers before changing contracts; explicitly state blast radius; identify whether the change affects model shape, serialization, state semantics, or UI expectations.

## Human-in-the-loop rules

| High-risk area | Required behaviour |
|---|---|
| Payments, transfers, balance handling | Flag for founder review; never finalise silently |
| Auth / session / passcode / biometrics | Flag for founder review |
| Shared contract changes (multi-surface) | Blast radius statement + approval before merge |
| Release / signing / store automation | Require explicit approval before executing |
| Anything legal / regulatory (POPIA, SARB — unconfirmed) | Mark "requires legal review"; never assert a legal conclusion |
| Anything irreversible (deletes, deploys, outbound sends, payments) | Require explicit approval before executing |

When uncertain, create an Open Questions entry in `SESSION_STATE.md` rather than pretending the answer is final.

## Suggested catalogues (create as needed)

| File | Purpose |
|---|---|
| `risk-register.md` | Identified risks: impact, likelihood, owner, mitigation, status |
| `abuse-and-fraud-scenarios.md` | Threat scenarios: detection signal, prevention, alert, audit evidence, owner |
| `compliance-checklist.md` | Regulatory/privacy obligations and status (POPIA et al. — requires confirmation) |
