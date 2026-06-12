# Technical Plan — FT-000: User Sign-Up with Email Verification  *(WORKED EXAMPLE)*

> Reference example. See `brief.md` in this folder for context.

**Based on brief:** engineering/features/FT-000-example/brief.md
**Plan created:** 2026-01-15
**Status:** approved

---

## Spec changes required (Phase 3 inputs)

| File | Change description | Dependency |
|---|---|---|
| `data-model/entities.md` | Add `User` entity + `EmailVerificationToken` | none |
| `data-model/status-models.md` | Add `User` lifecycle: `pending_verification → active → suspended` | after entities |
| `data-model/audit-events.md` | Add `user.registered`, `user.email_verified` | after status-models |
| schema | Add `user`, `email_verification_token` tables; status CHECK matches status-models | after audit-events |
| `api/` contract | Add `POST /users`, `POST /users/verify-email` | after schema |

---

## Implementation task graph

### Wave 1 — parallel (no dependencies)

#### IMPL-01: User domain model + lifecycle
| Field | Value |
|---|---|
| **Module** | `identity` (domain layer) |
| **Files to create** | `identity/domain/model/User.java`, `UserId.java`, `UserStatus.java`, events `UserRegistered`, `EmailVerified` |
| **Dependencies** | none |
| **Context for agent** | engineering/AGENTS.md · architecture/AGENTS.md · sdlc/04-implementation.md · backend-patterns.md · data-model/status-models.md §User |

**Acceptance criteria:**
- `User.register(email, passwordHash)` creates a user in `pending_verification` and raises `UserRegistered`.
- `User.verifyEmail()` from `pending_verification` → `active` and raises `EmailVerified`; from any other state throws `InvalidStateTransition`.
- `UserId` is a typed value object.

**Out of scope:** persistence, controllers, email sending.

#### IMPL-02: Verification token domain + hashing
| Field | Value |
|---|---|
| **Module** | `identity` (domain + core hashing) |
| **Files to create** | `EmailVerificationToken.java`, token-generation domain service |
| **Dependencies** | none |
| **Context for agent** | …as above + core hashing service |

**Acceptance criteria:** token is single-use; stored hashed; `isExpired(now)` true after 24h.
**Out of scope:** persistence, email delivery.

### Wave 2 — depends on Wave 1

#### IMPL-03: Persistence (entities, repos, migration)
Dependencies: IMPL-01, IMPL-02. Creates persistence models, repository impls, mappers, and the migration for `user` + `email_verification_token`.
**Acceptance criteria:** round-trips User and token; password hash + token hash stored as the project's sensitive-column type; ownership/uniqueness on email enforced at the DB.

#### IMPL-04: Use cases (sign-up, verify)
Dependencies: IMPL-01, IMPL-02. `RegisterUserUseCase` (hash password, persist, emit audit + send verification email via adapter, atomic), `VerifyEmailUseCase` (consume token, transition, emit audit).
**Acceptance criteria:** duplicate email returns the same response shape as success (no enumeration); audit emitted in the same transaction as the state change.

### Wave 3 — depends on Wave 2

#### IMPL-05: API (controllers + DTOs)
Dependencies: IMPL-04. `POST /users`, `POST /users/verify-email`; idempotency key on sign-up; typed error model.
**Acceptance criteria:** matches the `api/` contract; 201 on create; 400 on weak password; 409 mapped without leaking existence.

---

## Test plan (Phase 5 inputs)

| Task | Test type | Key scenarios |
|---|---|---|
| IMPL-01 | Unit | valid transition · invalid transition rejected |
| IMPL-02 | Unit | single-use · expiry |
| IMPL-03 | Data integration | round-trip · email uniqueness · hashed columns ≠ plaintext |
| IMPL-04 | Unit + integration | duplicate-email non-enumeration · atomic audit · idempotency |
| IMPL-05 | Endpoint integration | contract shape · status codes · weak-password rejection |

**Mandatory scenarios:** state machine completeness · audit emission · idempotency · sensitive-data round-trip.

---

## Parallelism summary

| Wave | Tasks | Parallel? |
|---|---|---|
| 1 | IMPL-01, IMPL-02 | Yes — different files in the domain |
| 2 | IMPL-03, IMPL-04 | Yes — persistence vs application |
| 3 | IMPL-05 | No — depends on Wave 2 |

## Risk flags from brief

- Sensitive data: passwords + tokens hashed; covered by IMPL-02/03 acceptance criteria and the sensitive-data round-trip test.
- Enumeration: covered by IMPL-04 non-enumeration test.

## Open questions blocking implementation

None.
