# AGENTS.md — `/engineering/features`

Feature tracking. Each feature that goes through the SDLC gets a directory here.

---

## Directory structure

```
engineering/features/
├── CLAUDE.md                 ← this file
├── backlog.md                ← prioritised list of upcoming features
├── FT-000-example/           ← a worked example (brief + plan + status) — keep for reference
├── FT-001/                   ← Standard/Full weight feature
│   ├── brief.md              ← Phase 1 output: what the feature is
│   ├── plan.md               ← Phase 2 output: task graph
│   ├── status.md             ← current status and phase
│   └── verify.md             ← Phase 6 output (created in Phase 6)
├── FT-002/                   ← Light weight feature
│   └── FEATURE.md            ← brief + mini-plan + status in one file
└── FT-003/ …
```

---

## Feature ID assignment

Sequential: `FT-001`, `FT-002`, … To assign the next: list directories, find the highest `FT-NNN`, increment. Do not reuse IDs. If a feature is cancelled, mark it cancelled in `status.md` — do not delete the directory. (`FT-000-example` is reserved for the reference example.)

---

## `status.md` template

```markdown
# Feature Status — FT-NNN: {Feature Name}

| Field | Value |
|---|---|
| **ID** | FT-NNN |
| **Name** | {Feature Name} |
| **Milestone** | M{N} — {Milestone name} |
| **MVP classification** | In MVP / Phase 1.1 / … |
| **Created** | {YYYY-MM-DD} |
| **Last updated** | {YYYY-MM-DD} |

## Phase status
| Phase | Status | Date | Notes |
|---|---|---|---|
| 1: Intake | done / in-progress / not-started | | |
| 2: Plan | … | | |
| 3: Spec Gates | … / skipped (no spec changes) | | |
| 4: Implement | … / deferred (no code repo) | | |
| 5: Test | … / deferred | | |
| 6: Review | … / deferred | | |

## Overall status
`draft` | `planning` | `spec-complete` | `in-progress` | `testing` | `review-blocked` | `verify-blocked` | `done` | `cancelled`

## Notes
{Important history — decisions, pivots, blockers resolved.}
```

---

## `backlog.md` format

Maintained by the orchestrator; updated at the end of each session. Sections: **In progress** · **Queued (approved, not started)** · **Proposed (not yet approved)** · **Done** · **Cancelled** — each a small table of ID / feature / milestone / (phase | priority | reason).

---

## Rules

- Feature **weight** (Light / Standard / Full) is set at intake per `workflows/agent/build-ft.md` Step 2. Light features use a single `FEATURE.md`; Standard/Full use the full file set. Weight may only escalate, never de-escalate.
- Do not delete feature directories — mark cancelled.
- Do not modify `brief.md`/`plan.md` after approval without re-running the phase and getting re-approval.
- `status.md` is updated by the orchestrator after every phase, task wave, test/review result, or blocker change, and again at the end of every session via `stop-session`/`wrap-up`.
- `verify.md` is written by the verify agent in Phase 6 — do not write it manually.
- Keep `backlog.md` current — it is the orchestrator's source of truth for what's next.
- Do not mark a feature `done` unless Phase 6 verification is PASS. BLOCKED or DEFERRED verification keeps the feature open.
