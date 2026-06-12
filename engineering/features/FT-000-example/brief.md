# Feature Brief — FT-000: User Sign-Up with Email Verification  *(WORKED EXAMPLE)*

> **This is a reference example**, not a real feature. It shows what a filled-in brief looks like for a generic SaaS. Read it alongside `plan.md` and `status.md` in this folder, then delete `FT-000-example/` once you're comfortable with the format.

**Status:** approved
**Created:** 2026-01-15
**Milestone:** M2 — Accounts & Identity
**MVP classification:** In MVP

---

## Description

A new user can create an account with an email address and a password, and must verify the email address before the account becomes active. This is the front door of the product — every other user-facing capability depends on an authenticated, verified account. From the user's perspective: "I enter my email and a password, I get a verification link, I click it, and I'm in."

## Affected capabilities

| Capability | Code | Notes |
|---|---|---|
| Account creation | C-1 | The sign-up endpoint and verification flow |
| Authentication | C-2 | Verified accounts can authenticate (login is a separate feature) |

## Out of scope for this feature

- Social / SSO sign-in (Phase 2)
- Password reset (separate feature, FT-00X)
- Login / session issuance (separate feature)
- Multi-factor authentication (Phase 2)
- Organisation / team invites (later milestone)

## Affected components

**Spec files:**
- [x] `data-model/entities.md` — new `User` entity
- [x] `data-model/status-models.md` — `User` lifecycle (pending_verification → active → suspended)
- [x] `data-model/audit-events.md` — `user.registered`, `user.email_verified`
- [x] schema — new `user` and `email_verification_token` tables
- [x] `api/` — `POST /users`, `POST /users/verify-email`
- [ ] `docs/*.md` — no narrative change

**Code modules/components:**
- [x] `identity` — User aggregate, sign-up + verification use cases, controllers
- [x] `core` — uses the shared email-sending adapter and hashing service

**Data / tables directly touched:**

| Table | Access | Notes |
|---|---|---|
| `user` | WRITE | created on sign-up; status updated on verify |
| `email_verification_token` | WRITE | one-time token; consumed on verify |

## Risk flags

- [x] **Sensitive-data handling** — passwords hashed (Argon2/bcrypt), never stored or logged in plaintext; verification tokens hashed at rest.
- [x] **Abuse / enumeration** — sign-up must not reveal whether an email already exists in its response (return the same shape either way).

## Assumptions

1. Email delivery uses the shared email adapter (already built in M1). If not, this feature depends on it.
2. Password policy: min 12 chars; complexity enforced at the API boundary.
3. Verification tokens expire after 24h.

## Open questions

| # | Question | Blocking? |
|---|---|---|
| Q-1 | Do we rate-limit sign-up per IP at this layer or at the gateway? | No — gateway default is fine for MVP |

---
*This brief was approved by Jordan (founder) on 2026-01-15.*
