# update-context workflow

Create, refresh, split, and prune the per-directory context files ("subcorpus") so agents always load small, current, high-signal context instead of re-exploring the codebase.

This workflow is **diff-driven**: its cost scales with what changed since the last run, never with repository size. It is the mechanical replacement for ad-hoc "audit the context files" judgement.

Run it: standalone (`/update-context`), as a step inside `wrap-up`, or after `build-ft` Phase 6 when source modules changed.

## The context file contract

Every per-directory context file is an `AGENTS.md` with a sibling one-line `CLAUDE.md` shim containing `@AGENTS.md`. Codex merges nested `AGENTS.md` automatically; Claude Code loads the shim on demand. One mechanism, both tools.

Each managed context file carries frontmatter:

```yaml
---
scope: src/payments/          # directory this file describes
features: [FT-003, FT-007]    # features owning code in this scope
last-synced: a1b2c3d          # short commit SHA at last update
budget: 150                   # max lines before this file must split or evict
---
```

Body sections (see `engineering/agents/module-context-template.md`):
Purpose · What exists · Public interface · Invariants and gotchas · Do NOT recreate · Feature map.

The registry at `engineering/agents/context-index.md` lists every managed context file with scope, last-synced, and line count. It is the single place to see the whole context tree and its token budget.

## Step 1 - detect staleness (cheap pass)

1. Read `engineering/agents/context-index.md`.
2. For each registry entry, run:

   ```bash
   git diff --stat <last-synced>..HEAD -- <scope>
   ```

   - Empty output: scope unchanged. Skip. **Do not read the context file or the directory.**
   - Non-empty: mark the entry stale and record the changed file list.
3. If the repo has no commits or an entry has no `last-synced`, mark it stale.

## Step 2 - detect coverage gaps

1. List source directories (in the code repo recorded in `engineering/AGENTS.md`; in this spec repo, top-level folders).
2. A directory needs a context file when any of:
   - it contains ≥ 5 source files, or
   - a feature `plan.md` declares tasks touching it, or
   - it holds non-obvious invariants an agent could violate (judgement call - record why).
3. A directory in the registry that no longer exists is an orphan - mark for pruning.

## Step 3 - create missing files

For each gap:

1. Read **only that directory** (file list, public surface, existing docs).
2. Write `AGENTS.md` from `engineering/agents/module-context-template.md`. Stay under the budget; link rather than restate specs.
3. Write the sibling `CLAUDE.md` shim: one line, `@AGENTS.md`.
4. Add the registry row with the current short SHA.

## Step 4 - update stale files

For each stale entry:

1. Read the diff for the scope (`git diff <last-synced>..HEAD -- <scope>`), **not** the whole directory.
2. Revise only the sections the diff invalidates: new/removed files, changed public interfaces, new invariants from review findings, feature map changes.
3. Bump `last-synced` to the current short SHA. Update the registry row's line count.

## Step 5 - enforce budgets (anti-context-rot gate)

For any context file over its `budget`:

1. **Split**: create child `AGENTS.md` files (+ shims) in subdirectories for the deep detail; the parent keeps a one-line pointer per child. This is how the context tree deepens as the code deepens.
2. Or **evict**: move reference material into a normal doc the context file links to. Context files hold orientation and constraints, not reference dumps.
3. Never raise the budget to avoid splitting. Default budget: 150 lines; 200 for the repo root.

## Step 6 - prune

For orphaned registry entries: delete the context file and shim (if the directory is gone) or mark the row `orphaned` for user confirmation (if uncertain). Never silently keep stale context - a wrong context file is worse than none.

## Step 7 - reconcile the feature map

1. For each active feature in `engineering/features/`, read the declared files/modules from `plan.md`.
2. Ensure each touched scope's `features:` frontmatter lists the feature, and the file's Feature map section says what the feature added.
3. Mismatches (plan declares a scope with no context entry, or vice versa) are findings - fix or surface them.

## Step 8 - report

```markdown
# Context update - {date} @ {SHA}
| Action | Count | Files |
|---|---|---|
| Skipped (fresh) | N | - |
| Updated | N | ... |
| Created | N | ... |
| Split | N | ... |
| Pruned / orphaned | N | ... |
Total context lines: {N} ({+/-N} since last run)
```

Flag any file still over budget, any orphan awaiting confirmation, and any feature-map mismatch.

## Rules

- Diff-driven only. Never re-read the whole repository to "refresh" context.
- Every managed context file appears in the registry; every registry row points at a real file.
- Budgets are hard. Split or evict; never bloat.
- Context files describe what IS, never what SHOULD BE built (that belongs in specs and plans).
- No secrets, credentials, or PII in context files.
- `AGENTS.md` holds the content; `CLAUDE.md` is always a one-line shim. Both files travel together.

## Failure handling

| Failure | Response |
|---|---|
| No git history | Treat everything as stale once; record the first SHA |
| Registry missing | Recreate it by scanning for existing `AGENTS.md` files with frontmatter |
| Registry disagrees with disk | Disk wins; rebuild the affected rows; report the drift |
| Scope too large to summarise under budget | Split immediately; never write an over-budget file |
| Code repo path not recorded | Update spec-repo folders only; flag the gap |
