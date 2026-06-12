# stop-session workflow

Update `SESSION_STATE.md` at the end of a productive session so the next agent can resume cleanly.

## Review the session

Inventory what happened:

| Aspect | Examples |
|---|---|
| New files created | New specs, feature files, module context files |
| Files modified | Authoritative specs or context files changed |
| Cross-cutting migrations | Started, advanced, completed |
| Decisions locked | New D-N or ED-N decisions confirmed |
| Decisions deferred | New open questions surfaced |
| Status changes | Docs moved from stub to authoritative; features moved phase |

If unclear what happened, ask the user before updating state.

## Read current state

Read `SESSION_STATE.md` before editing it.

If the session changed feature phase, task wave, test result, review result, blocker, or verification status, read the active feature `status.md` before editing it. Update the feature status first, then `SESSION_STATE.md`; do not leave the two files disagreeing.

## Update targeted sections only

Do not rewrite the whole file. Update only what changed.

| Section | Update when |
|---|---|
| Section 1 - Where we are | Major milestone or phase changed |
| Section 2 - Status by area | Authoritative status changed |
| Section 3 - What's in flight | Almost always |
| Section 4 - Next concrete action | Almost always |
| Section 5 - Locked decisions | New decisions confirmed |
| Section 6 - Open questions | Questions surfaced or answered |
| Section 7 - Launch-blocking checklist | Items completed or added |

Update the Last updated line with the exact date and a one-line summary.

## Process cleanup

Before handoff:

1. Confirm no agent-started dev server, watcher, test runner, build, browser, or verification process is still running.
2. Stop anything started only for this session.
3. If a process must remain running, record PID, command, purpose, port, and stop instructions in the handoff.
4. If a check hung, include the diagnostic attempts from the hung-check protocol; do not reduce it to "checks hung".

## Consistency check

Run `scripts/check-state.sh` and fix any errors it reports before writing the handoff.

## Confirm with the user

Show a concise summary of the `SESSION_STATE.md` updates:

- Sections updated.
- New decisions.
- New or resolved open questions.
- Next concrete action.

Ask whether it accurately reflects where the session ended. Do not print the bootstrap message until the user confirms.

## Print bootstrap message

After confirmation, print `SESSION_STATE.md` section 9 verbatim, or remind the user to use `start-session`.

## Rules

- Make only targeted updates to `SESSION_STATE.md` and active feature tracking files needed to preserve consistency.
- Keep active feature `status.md`, `engineering/features/backlog.md`, and `SESSION_STATE.md` consistent.
- Reflect the session; do not invent progress.
- Do not mark a phase complete unless gate evidence is recorded.
- Section 4 must be precise enough for a fresh session to act without asking.
- Convert relative dates to exact dates.
- If work was discussion-only, say so and leave section 4 unchanged unless the user changed direction.
