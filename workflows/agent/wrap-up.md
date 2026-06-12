# wrap-up workflow

Close out a milestone by refreshing durable agent context, writing a milestone report, and updating session state.

The primary deliverable is a current context tree. Future agents must be able to resume without rediscovering what already exists.

## Step 1 - identify what changed

Collect facts before editing:

```bash
git log --oneline -10
git diff --stat
cat engineering/features/backlog.md
```

Answer:

- Which milestone completed?
- Which feature IDs completed?
- Which modules/components gained code?
- Did the data model change?
- Did the API contract change?
- Did docs gain new authoritative specs?
- Were any D-N or ED-N decisions locked?

If the repo has no commits yet, use the current file inventory and feature status files instead of `git diff`.

## Step 2 - refresh the context tree

First run the shared `update-context` workflow (`workflows/agent/update-context.md`). It handles the per-directory context files mechanically: creates missing module `AGENTS.md` files (+ `CLAUDE.md` shims), refreshes stale ones diff-driven, enforces line budgets, prunes orphans, and updates `engineering/agents/context-index.md`.

Then audit the files `update-context` does not own. Always check:

| File | What to check |
|---|---|
| `AGENTS.md` | Foundational product rules and shared agent workflow map |
| `CLAUDE.md` | Still a thin Claude adapter importing `@AGENTS.md` |
| `engineering/AGENTS.md` | Current milestone and new ED decisions |
| `engineering/features/backlog.md` | Completed features moved to Done; next feature queued |
| `SESSION_STATE.md` | Where we are, in flight, next action, decisions, open questions |

Update if changed:

| File | Update when |
|---|---|
| `docs/AGENTS.md` | New numbered spec added or status changed |
| `data-model/AGENTS.md` | Entities, states, or audit events changed |
| `api/AGENTS.md` | New endpoints, schemas, or contract decisions |
| `engineering/architecture/AGENTS.md` | A review finding becomes a locked architecture pattern |
| `risk/AGENTS.md` | Risk register or controls changed |
| `workflows/AGENTS.md` | Operational or shared agent workflow files changed |

If code repos exist, every module with source files must have an `AGENTS.md` context file with a `CLAUDE.md` shim — `update-context` enforces this; a gap here means it was not run or its trigger rules need tightening.

## Step 3 - milestone report

Default report path:

```text
dev_reports/M{N}-report.md
```

Include:

1. Header: milestone, date, status, commit SHA if available.
2. Executive summary.
3. What was built.
4. How components connect.
5. Component map.
6. Decisions locked.
7. Testing.
8. What's next.
9. Appendix: file inventory.

## Step 4 - commit only if asked

Do not commit unless the user explicitly asked.

If committing, separate context/spec changes from code changes when useful and follow the user's git workflow.

## Step 5 - self-verify

Run:

```bash
scripts/check-state.sh
grep -A4 "## 4\. Next concrete action" SESSION_STATE.md | head -6
grep -m1 -i "milestone" engineering/AGENTS.md
git status --short
```

Also confirm:

- `check-state.sh` reports no errors (root and per-folder `CLAUDE.md` shims import `@AGENTS.md`; registry matches disk).
- Shared workflow bodies live in `workflows/agent/`.
- `.claude/skills/*/SKILL.md` wrappers point to the shared workflow files.
- `SESSION_STATE.md` section 4 passes the precision test.
- Active feature `status.md`, `engineering/features/backlog.md`, and `SESSION_STATE.md` agree.
- No agent-owned dev server, watcher, test runner, build, browser, or verification process is still running without recorded PID and stop instructions.

## Step 6 - report

Report:

- Context files checked and updated.
- Milestone report path.
- Tests/checks run.
- Next concrete action.
- Any remaining risks, blocked verification, or externally deferred verification, including diagnostic attempts and the smallest reproduction command.

## Rules

- Read before updating.
- Keep `AGENTS.md` as the shared source of truth.
- Keep `CLAUDE.md` thin.
- Update `engineering/AGENTS.md` and `SESSION_STATE.md` every milestone.
- Do not allow stale context files to survive a wrap-up.
