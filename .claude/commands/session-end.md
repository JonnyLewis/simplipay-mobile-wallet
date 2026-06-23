# Session End Command

Save current work context for the next session, then run a skill retrospective so this command set gets more accurate every session.

## Usage

```
/session-end                    # Save context, run retrospective, summarize session
/session-end "brief note"       # Save with custom note
/session-end --no-retro         # Skip the skill retrospective (save + summarize only)
```

## Prerequisites (one-time bootstrap)

The retrospective (Step 3) depends on three artifacts that may not exist on a first run. This bootstrap is **self-approving, and its paired prompt-layer write is atomic** (the inert ledger created in item 1 is left in place on an item-2 abort — see the note under item 2) — it does NOT depend on the `retrospective-ready` trigger being resolvable (that trigger is one of the things this bootstrap creates). Run it before Step 3 on every invocation; on every run after the first it is a no-op. It is idempotent.

If any artifact below is missing, present a single plain `AskUserQuestion` — "First run: create the retrospective's ledger + prompt-layer entries?" with options `Create` / `Skip retrospective this session`. Only on `Create` proceed. This is a one-time confirmed action, NOT routed through `retrospective-ready`.

1. **Ledger.** If `claude-product-cycle/LEARNINGS.md` does NOT exist, create it with this header and an empty entries section. Treat an absent or empty ledger as "no prior fingerprints" so the Step 3d dedupe is well-defined on the very first run:

   ```markdown
   # Skill Learnings Ledger

   Durable, cross-session learnings mined by `/session-end`. Newest first.
   Each entry follows the format documented in `.claude/commands/session-end.md` Step 3e.
   Transient session state lives in `CURRENT_WORK.md`, NOT here.

   ---
   ```

2. **Approval-gate trigger + prompt (paired, written together).** Run `grep -rn 'retrospective-ready' prompt-layer/`. If it returns NO hits, the approval gate (Step 3e) cannot resolve. Write BOTH paired entries in ONE batch — a trigger named without both entries is a dangling reference (the exact bug from a prior session). After writing, immediately re-grep BOTH files to assert both landed; if EITHER is missing, revert BOTH and abort the bootstrap (do not leave the machinery half-built). Do NOT run Step 3 until both are confirmed present.

   > Scope of the revert-both rollback: it covers the paired `TRIGGERS.md` / `PROMPTS.md` write only. The empty ledger created in item 1 is intentionally LEFT in place on an item-2 abort — it is an inert, header-only file with no entries, the bootstrap is idempotent, and a subsequent run reuses it as the "no prior fingerprints" base. So "atomic" here means the paired prompt-layer write is atomic; the ledger creation is a separate, inert, idempotent step that does not need rollback.

   - Add a row to the `## Lifecycle Triggers` table in `prompt-layer/TRIGGERS.md`. The live table is four columns — `Trigger ID | Fires When | Prompt intent | Raised by`. Keep the `Fires When` cell STATIC and terse like every neighboring row — TRIGGERS rows are not templated (only PROMPTS bodies are), so a `{VAR}` here has no resolution source and would render literally:

     ```
     | `retrospective-ready` | Verified learnings ready for review at session end | "Apply learnings?" | `/session-end` |
     ```

   - Add a matching block to `prompt-layer/PROMPTS.md` under `## Lifecycle Prompts`, AND extend the context-variable enumeration at the top of `PROMPTS.md` (currently `{FEATURE}`, `{LAYER}`, `{TOTAL_TASKS}`, `{CURRENT_TASK}`, `{NEXT_TASK}`) to also include `{VERIFIED_COUNT}` and `{SKILL_LIST}` — otherwise those two new fire-time vars are themselves dangling against the registry that documents them. Use single-brace `{VAR}` in the prompt body to match every existing lifecycle-prompt entry in `PROMPTS.md`:

     ```markdown
     ### retrospective-ready

     **Trigger**: `retrospective-ready` — fired by `/session-end` when verified learnings are ready.
     **Context**: `{VERIFIED_COUNT}`, `{SKILL_LIST}`

     > {VERIFIED_COUNT} verified learning(s) ready for {SKILL_LIST}. How should I apply them?

     | Option | Routes to |
     |--------|-----------|
     | Apply all | Write all previewed edits + append ledger entries (NEVER includes CLAUDE.md or the self-corruption-excluded files — those force Review each) |
     | Review each | Step through diffs one at a time for per-edit approval |
     | Apply facts (batch-review) | Show ONE consolidated diff of all category-(c) index/fact edits, require a single confirm, then write them; exclusion-set / CLAUDE.md targets are downgraded to Review each and never included in the batch |
     | Skip (record pending) | Write nothing to skills; append the previewed entries to the ledger as `pending` |
     ```

   > Brace-convention note: the prompt layer uses two conventions by role, defined authoritatively in `ENGINE.md` → Context Variables. Single-brace `{VAR}` = fire-time prompt variables the engine substitutes into a prompt body (`PROMPTS.md`); double-brace `{{VAR}}` = output-template placeholders a command fills when writing a document (`CURRENT_WORK.md`, the `LEARNINGS.md` ledger). So use single-brace `{VAR}` in the `retrospective-ready` prompt body (matching every other `PROMPTS.md` lifecycle prompt) and double-brace `{{VAR}}` in this command's `CURRENT_WORK.md` / ledger templates. `ENGINE.md` and `PROMPTS.md` agree on the fire-time set.

3. **Trigger ownership.** `/session-end` raises `commit` (Step 4) and `retrospective-ready` (Step 3e). It does not itself raise `commit-post` — the engine fires that after a commit lands — but `commit-post` arises within `/session-end`'s lifecycle, exactly as it does within `/gap-status`'s. The existing `TRIGGERS.md` `Raised by` column does NOT distinguish direct raisers from engine-fired follow-ons: it simply lists the command in whose flow the trigger arises (note `/gap-status` is already listed for `commit-post`, which it likewise only triggers transitively). So when you touch `prompt-layer/TRIGGERS.md` in this bootstrap, add `/session-end` to the `Raised by` cell of both the `commit` row and the `commit-post` row, consistent with that column's existing meaning. (Renaming the column to `Associated with` to make the transitive sense explicit is a reasonable but separate registry-wide cleanup, not done here.)

## Instructions

### Step 1: Gather Session Information

1. Check git status: `git status`
2. Check recent commits this session: `git log --oneline -5`
3. Review any uncommitted changes

### Step 2: Update CURRENT_WORK.md

**Update** `claude-product-cycle/CURRENT_WORK.md`:

```markdown
# Current Work

**Last Updated**: {{DATE}} {{TIME}}
**Branch**: {{BRANCH}}
**Session Note**: {{USER_NOTE_OR_AUTO_SUMMARY}}

---

## Active Tasks

| # | Task | Feature | Status | Files | Notes |
|---|------|---------|:------:|-------|-------|
{{ACTIVE_TASKS_FROM_SESSION}}

---

## In Progress

### {{FEATURE_NAME}}

**What was done**:
- {{COMPLETED_ITEMS}}

**What's next**:
- {{NEXT_ITEMS}}

**Key files touched**:
- {{FILE_LIST}}

---

## Uncommitted Changes

```
{{GIT_STATUS_OUTPUT}}
```

---

## Resume Instructions

1. Run `/session-start` to load this context
2. Continue with: {{NEXT_ACTION}}
3. Key context: {{IMPORTANT_NOTES}}
```

Keep this file purely transient — active task, in-progress feature, uncommitted-changes snapshot, next action. It is fully overwritten each session. Durable learnings belong in `claude-product-cycle/LEARNINGS.md`, never here.

### Step 3: Skill Retrospective & Self-Improvement

Review what happened this session, compare it against the skills (commands, `CLAUDE.md`, `prompt-layer/`, `claude-product-cycle/` indexes) that were used, extract VERIFIED learnings, and PROPOSE skill edits for approval. This stage is **propose-only and bounded** — it never edits a skill without explicit approval routed through the prompt layer. Skip this entire step if `--no-retro` was passed. The Prerequisites bootstrap must have completed first.

Run the pipeline **mine → map → verify → dedupe → propose** in order.

#### 3a. Mine the session for learning signals

Scan the session transcript and git state for these signal types. Only consider skills that were ACTUALLY used this session.

| # | Signal type | Detection heuristic |
|---|-------------|---------------------|
| a | User correction / override | The user corrected the assistant, rejected a suggestion, or overrode a fired trigger's route |
| b | Skill output was wrong / had to be fixed | A skill/command emitted a fact or step that was edited or contradicted mid-session |
| c | Missing / failing reference | A referenced file, path, class, or Gradle task did not exist or failed when run |
| d | Repeated friction / manual step | The same corrective step (e.g. adding `--no-configuration-cache`, choosing `:desktopTest`) was taken 2+ times |
| e | Newly discovered convention | A code pattern, API signature, or binding was confirmed correct by a passing build/test |
| f | Build / test failure + root cause | A Gradle build/test failed and the root cause was identified and resolved |
| g | Correct-but-costly step | A verified-working skill step took a materially slower path than an available cheaper one (e.g. re-ran a long Gradle task where a cached file read would do). Single-occurrence is fine here — it does NOT need the 2+ repetition that signal (d) requires. |
| h | Documented-absence resolved | A previously-absent source set, Gradle task, file, or API now EXISTS, contradicting a skill that documents its absence (nothing failed and nothing was repeated — the world simply changed). Routes through the 3d supersede path to the owning skill. Example: an `androidInstrumentedTest` source set now exists, contradicting `verify-tests.md` which documents that none do. |

> Boundary note: single-occurrence observations that are neither wrong (a/b/c/f), repeated (d), a confirmed convention (e), a clear efficiency win (g), nor a resolved documented-absence (h) are OUT of scope for skill edits. Surface them in the Step 5 "unverified observations" overflow list for the human to judge, rather than laddering them.

Capture each signal as a tuple:

```
{ signal_type, claim_as_written, observed_reality, evidence_command, candidate_target_file }
```

---

#### 3b. Map each signal to the skill file(s) that should change

Route deterministically. A signal may fan out to more than one target. **Before routing, confirm the candidate target file actually exists** (`ls`/file read). If it does not, do NOT invent a path — downgrade the signal to an Unverified observation for the Step 5 overflow list. Target files must resolve to one of the four artifact classes (`.claude/commands/*.md`, `CLAUDE.md`, `prompt-layer/*.md`, `claude-product-cycle/**`) — except `claude-product-cycle/LEARNINGS.md` itself and the net-new `TRIGGERS.md` / `PROMPTS.md` entries created in the Prerequisites bootstrap.

| Signal type | Primary target | Secondary target |
|-------------|----------------|------------------|
| a — user correction | the `.claude/commands/<cmd>.md` whose behavior was overridden | ledger |
| b — wrong skill output | the specific `.claude/commands/<cmd>.md` whose text was wrong | ledger |
| c — missing Gradle task | `CLAUDE.md` Build Commands + `.claude/commands/verify-tests.md` | ledger |
| c — fabricated index data | the concrete index file (see per-layer map below) | ledger |
| d — repeated friction | `CLAUDE.md` Build Commands (if global) or owning `<cmd>.md` (if command-specific) | ledger |
| e — new convention | `prompt-layer/TRIGGERS.md` Pattern Reminders (+ `PROMPTS.md` if a reusable template) | `prompt-layer/ENGINE.md` if a hard guardrail; ledger |
| f — build/test failure | the `<cmd>.md` that ran the build + `CLAUDE.md` if a command was wrong | ledger |
| g — correct-but-costly | the owning `<cmd>.md` (the step that took the slow path) | ledger |
| h — documented-absence resolved | the skill that documents the absence (e.g. `.claude/commands/verify-tests.md`, or the relevant `claude-product-cycle/**` index) — routed through the 3d supersede path | ledger |
| trigger/prompt mismatch (`PROMPTS.md` ↔ `TRIGGERS.md`) | `prompt-layer/PROMPTS.md` + `prompt-layer/TRIGGERS.md` (paired entries) | ledger |
| CLAUDE.md trigger-table / registry drift (`CLAUDE.md` ↔ live `TRIGGERS.md`) | `CLAUDE.md` Prompt Layer Integration trigger table (factual-only, **Review each** per the self-corruption / `CLAUDE.md`-outranks rule) | ledger |
| test-wiring / test-coverage status drift | `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` (the Primary `verify-tests.md` Phase-0 reads) + the matching lazily-created per-layer `claude-product-cycle/<layer>/TESTING_STATUS.md` if it already exists | NOT laddered — transient only |
| other stale-status drift | the layer's actual status file (see per-layer map below) | NOT laddered — transient only |

**Concrete index / status filenames** (no generic `INDEX.md` or `STATUS.md` exists — use these literal, fully-qualified paths under `claude-product-cycle/`, and glob `claude-product-cycle/<layer>/*STATUS*.md` / `claude-product-cycle/<layer>/*INDEX*.md` if unsure):

| Layer | Index file(s) | Status file |
|-------|---------------|-------------|
| design-spec | `claude-product-cycle/design-spec-layer/FEATURES_INDEX.md`, `claude-product-cycle/design-spec-layer/MOCKUPS_INDEX.md` | `claude-product-cycle/design-spec-layer/STATUS.md`; test status → `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` (the Primary verify-tests reads) |
| server | `claude-product-cycle/server-layer/API_INDEX.md` | none → route non-test drift to `claude-product-cycle/PRODUCT_MAP.md` |
| client | `claude-product-cycle/client-layer/FEATURE_MAP.md` | none → route non-test drift to `claude-product-cycle/PRODUCT_MAP.md` |
| feature | `claude-product-cycle/feature-layer/MODULES_INDEX.md`, `claude-product-cycle/feature-layer/SCREENS_INDEX.md` | none for non-test → `claude-product-cycle/PRODUCT_MAP.md`; lazily-created per-layer `claude-product-cycle/feature-layer/TESTING_STATUS.md` (create only if verify-tests already created it) |
| platform | — | `claude-product-cycle/platform-layer/LAYER_STATUS.md`; lazily-created `claude-product-cycle/platform-layer/TESTING_STATUS.md` |

> Test-status routing rule: per `verify-tests.md` Phase-0, the **Primary** test-status authority is `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` — the per-layer `feature-layer` / `client-layer` / `platform-layer` `TESTING_STATUS.md` files do NOT exist until verify-tests creates them on first test write. So a test-wiring / test-coverage status learning ALWAYS writes the Primary, and ALSO updates the matching per-layer file only if it already exists. Never collapse test-status drift into the generic `PRODUCT_MAP.md` fallback — that file is not the test-status authority verify-tests consults, so the drift would land where the next session never reads it.

To resolve "which `<cmd>.md`": match on the command invoked when the contradiction surfaced; if none, match on the index the wrong claim was about and route to the command whose Phase-0 reads that index (e.g. test claims → `verify-tests.md`).

> Two distinct trigger-vocabulary classes — do NOT conflate them. (1) A **`PROMPTS.md` ↔ `TRIGGERS.md`** mismatch (a trigger row with no matching prompt body, or vice versa) routes to the paired-entry row above. (2) A **`CLAUDE.md` ↔ live `TRIGGERS.md`** registry drift — e.g. `CLAUDE.md`'s 8-row Prompt Layer Integration table omitting trigger IDs that the live 11-row `TRIGGERS.md` registry carries (`plan-layer`, `plan-continue`, `commit-post`) — routes to `CLAUDE.md`'s trigger table, is factual-only, and forces **Review each** because `CLAUDE.md` (priority 3) outranks this command. Routing the second class to `PROMPTS.md` is wrong: the prompt bodies are not where the documentation-vs-registry drift lives.

**Self-corruption exclusion (hard rule).** The retrospective MUST NOT auto-apply edits to its own correctness machinery: `.claude/commands/session-end.md`, `prompt-layer/ENGINE.md`, and the WHOLE of `.claude/commands/verify-tests.md` (not just its verification-command definitions — `verify-tests.md` Phase-0 is the authority the unwired-test-tier correctness gate relies on, so a wrong auto-applied fact edit ANYWHERE in that file, e.g. to its Feature Testing Matrix paths, could degrade the gate's surrounding context). A proposed change to any of these may only be applied via the per-edit **Review each** path — never **Apply all** and never the **Apply facts (batch-review)** route — so a wrong learning can never silently weaken the gate that is meant to catch wrong learnings. Any category-(c) edit whose target is in this exclusion set (or is `CLAUDE.md`) is automatically downgraded from **Apply facts** to **Review each**.

**CLAUDE.md outranks this command.** Per `prompt-layer/ENGINE.md` priority, `CLAUDE.md` (project instructions, priority 3) sits ABOVE both the Prompt Layer (4) and command files (5). Retrospective edits to `CLAUDE.md` are therefore limited to factual `Build Commands` / Gradle-task-name / module-count corrections, and they MUST go through the **Review each** per-edit path — never **Apply all** or **Apply facts (batch-review)**. The retrospective never rewrites architecture decisions or overrides org / user-global instructions.

---

#### 3c. Verify each candidate against the ACTUAL codebase (the correctness gate)

NEVER write an unverified claim into a skill. Before proposing ANY edit, run the matching read-only verification command and confirm its output equals `observed_reality`. Verification commands must be idempotent and MUST NOT mutate code.

| Signal type | Verification command class | Passes when |
|-------------|----------------------------|-------------|
| Codebase / index fact | re-run the exact `grep -r` / `find` / file read, scoped to the path the index mirrors (e.g. `feature/<name>/src`) | output reproduces `observed_reality` |
| Gradle task exists/runs | `./gradlew tasks --all \| grep <task>` or `./gradlew <task> --dry-run` | task is absent / fails exactly as claimed |
| **Unwired test tier** (androidInstrumentedTest, Roborazzi/screenshot, E2E) | confirm ABSENCE via a source-set / path check, e.g. `ls feature/<name>/src/androidInstrumentedTest` — do NOT use `--dry-run` | the source set does not exist |
| **Documented-absence resolved (h)** | the SAME source-set / path / task check used to confirm the original absence, e.g. `ls feature/<name>/src/androidInstrumentedTest` or `./gradlew tasks --all \| grep <task>` | the source set / task / file now EXISTS, contradicting the skill's documented-absent state |
| New convention / guardrail | read the canonical reference impl the index points to (e.g. `BeneficiaryRepositoryImpl.kt`) | the pattern is actually present |
| Repeated friction (d) / costly step (g) | re-run the workaround / both paths once | it succeeds deterministically (g: confirm the cheaper path produces the same result) |
| Trigger / prompt gap | structural read of `PROMPTS.md` / `TRIGGERS.md` / git log | the ID is genuinely absent / the markers genuinely contradict |

> Preferred-over-existing-API convention (signal e, sometimes b): when the learning is "prefer X over Y" and BOTH symbols may legitimately exist (e.g. `BaseViewModel` defines `sendAction` and uses `viewModelScope`, yet the documented convention is `trySendAction` / `launchIO` — see `PROMPTS.md` `PROMPT: new-viewmodel`), verify by confirming the PREFERRED symbol exists and is the documented convention, NOT by asserting the deprecated symbol is absent. Only a genuine "this symbol does not exist" claim is verified by absence. Treating `sendAction`/`viewModelScope` as absence facts would mark a valid convention learning Unverified (they ARE present) and drop it.

> Why the unwired-test-tier row is special: `verify-tests.md` Phase-0 documents that there are **no** `androidInstrumentedTest` source sets, no E2E suite, and no Roborazzi screenshot tests yet — their Gradle commands are not runnable. A `--dry-run` on `connectedAndroidTest` / instrumented tasks fails for infrastructure reasons, not because the claim is true, so it would risk a false **Verified**. Confirm absence by path instead. Signal (h) is the inverse of this row: when one of those source sets later lands, the path check now succeeds, which is what supersedes the documented-absence learning.

Tag every candidate **Verified** or **Unverified**.

> CAUTION — a proposed fix can itself be wrong. Example from a real session: an agent claimed "29 repositories should be 28," but a `grep` confirmed there were actually 29. The fix was wrong, not the skill. The gate exists to catch exactly this: re-verify the *correction*, not just the original claim.

- **Verified** candidates proceed to dedupe and proposal.
- **Unverified** candidates are DROPPED from skill edits and surfaced in the Step 5 summary as "unverified observations" so they are not silently lost — the human judges them.

**Bound the scope: carry forward at most the top 5 highest-impact verified learnings.** Rank deterministically so the cut is identical across agents — apply these keys in order:

1. **Severity tier**: wrong / failing (a, b, c, f) rank above conventions / efficiency / absence-resolved (e, g, h), which rank above drift.
2. **Target breadth**: an edit to `CLAUDE.md` or `prompt-layer/ENGINE.md` (read by many commands) outranks a single command-file edit.
3. **In-session repetition count** (primarily distinguishes signal d): more occurrences rank higher.

Note any overflow beyond the top 5 in the Step 5 summary; it is not laddered.

---

#### 3d. Dedupe against the ledger

Compute a fingerprint for each verified learning, deterministically, so the same learning slugifies identically across sessions and agents:

```
fingerprint = category | target-basename | subject-key | facet     (lowercased, slugified)
```

| Component | Rule |
|-----------|------|
| `category` | the single signal-type letter from 3a (`a`–`h`) |
| `target-basename` | the lowercased file basename of the primary target (e.g. `claude.md`, `verify-tests.md`, `api_index.md`) |
| `subject-key` | the canonical symbol or path the claim is about, lowercased basename only (e.g. `beneficiaryrepositoryimpl`, `androidinstrumentedtest`, `repository-modules`) |
| `facet` | the specific attribute, from this closed vocabulary: `count` \| `path` \| `signature` \| `task-name` \| `wiring-state` \| `cost` \| `registry` — use `registry` for trigger-ID / enum / index-membership SET drift (a difference between two membership lists, e.g. documented vs live trigger IDs), NOT `count` (a `count` facet is a scalar tally and would collide with unrelated count learnings on the same file) |

**Decomposition rule (one learning = one atom).** A fingerprint addresses exactly one `(target, subject-key, facet)` atom. A single observation that spans multiple symbols MUST be split into one ledger entry per symbol BEFORE fingerprinting, so the slug is deterministic and each symbol can supersede independently. Example: a "BaseViewModel convention" correction touching three symbols (`updateState` → `mutableStateFlow.update`, `sendAction` → `trySendAction`, `viewModelScope` → `launchIO`) becomes THREE entries keyed `updatestate`, `trysendaction`, and `launchio` (the preferred symbol is the subject-key for signal-(e) convention learnings — see 3c), not one ambiguous `baseviewmodel` entry. Without this rule, agent A might key the whole thing `baseviewmodel|signature` and agent B `updatestate|signature`, the subject keys diverge, the supersede match falls to "none," and the learning duplicates across sessions.

Worked examples: the "how many repository modules are registered" learning fingerprints as `c | claude.md | repository-modules | count` every time; the "CLAUDE.md trigger table is missing `plan-layer`/`plan-continue`/`commit-post`" learning fingerprints as `c | claude.md | prompt-layer-triggers | registry`.

**Supersede-match key (cross-category).** When checking whether a new learning supersedes an existing one, match on the **subject triple** `target-basename | subject-key | facet` only — treat `category` as metadata, NOT part of the supersede key. This is required for signal (h): a documented-absence is usually first ledgered as category `c` (`c|verify-tests.md|androidinstrumentedtest|wiring-state`) but its resolution is mined as category `h` (`h|verify-tests.md|androidinstrumentedtest|wiring-state`). The category letter differs, so a category-inclusive match would falsely fall to "none" and DUPLICATE instead of supersede. Match the subject triple and the `h` event correctly supersedes the original `c` absence entry. (The full four-part fingerprint is still RECORDED on each entry for provenance; only the *match* drops the category.)

**Ledger sequence-number allocation.** Each entry gets a stable `L-NNNN` id. To allocate the next id: scan existing `## L-` headers in `claude-product-cycle/LEARNINGS.md`, take the max integer, and increment (zero-padded to 4 digits). If none exist (first run, empty ledger from bootstrap), start at `L-0001`. A superseded entry KEEPS its original number — superseding never renumbers and never reuses a freed id.

If the ledger is absent or empty (first run), there are no prior fingerprints and every verified learning is NEW. Otherwise scan existing `**Fingerprint**:` lines in `claude-product-cycle/LEARNINGS.md`. Only `applied` entries count as authoritative "reality" for matching — `pending` entries are candidate noise and MUST NOT supersede or suppress a new learning:

| Subject-triple match (vs `applied`) | Reality match | Action |
|-------------------------------------|:-------------:|--------|
| match | same | Skip — already known, propose nothing |
| match | different | Mark old entry `superseded`, propose a NEW entry + re-apply to target (the fact changed) — **subject to the same routing constraints as a new edit: self-corruption-excluded targets (`session-end.md`, `ENGINE.md`, all of `verify-tests.md`) and `CLAUDE.md` still force `Review each`, never `Apply all` / `Apply facts`** |
| none | — | Propose a NEW entry |

The supersede path handles drift over time — e.g. when an `androidInstrumentedTest` source set eventually lands (mined as signal (h)), the "NOT YET WIRED" learning in `verify-tests.md` is superseded rather than duplicated, via the `Review each` path because `verify-tests.md` is self-corruption-excluded.

---

#### 3e. Propose skill edits and fire the retrospective prompt

For each surviving learning, prepare a concrete edit preview — the exact target file, a minimal before/after diff (changed lines only), and the backing evidence command. The Prerequisites bootstrap's `TRIGGERS.md` / `PROMPTS.md` edits (if this was a first run) are surfaced in this same preview. Do NOT apply anything yet.

**Before applying anything, protect reversibility — capture a non-empty snapshot ref first.**

1. Record current `HEAD`.
2. If the working tree is dirty, run `git stash create` to capture a snapshot commit without disturbing the tree. **`git stash create` returns empty output on a clean tree** — if it yields no ref, use `HEAD` as the snapshot base.
3. **Assert a non-empty snapshot ref was captured before applying ANY edit.** If neither `git stash create` nor `HEAD` yields a usable ref (e.g. a fresh branch with no commits at all), ABORT the apply phase and fall back to propose-only output.
4. If HEAD is not already a dedicated `feature/` (or otherwise task-scoped) branch created for this session, create one NOW — before applying any skill edit — so applied edits are never left on a shared base branch (`development`, `master`, or any long-lived working branch such as `v2-app-base`) that the repo rules forbid committing to directly. This is a positive requirement: the guard fires unless you are demonstrably on a fresh `feature/` branch, not just when you happen to be on one of two named branches.

The post-apply equality check below diffs against THIS snapshot ref, never the raw working tree, so pre-existing in-progress edits to a skill file can never spuriously fail the check or be destroyed by a revert.

Fire the `retrospective-ready` trigger (created/verified in the bootstrap; engine resolves the prompt from `prompt-layer/PROMPTS.md`). Do NOT hardcode the prompt text here. **If the prompt cannot be resolved at fire time, write NOTHING to disk — print the previews to the Step 5 summary and exit (true propose-only).** A degraded gate is the worst moment to mutate any durable file, the ledger included.

```
TRIGGER: retrospective-ready
  WHEN:   N verified learnings are ready for review at session end
  LOAD:   PROMPTS.md → retrospective-ready
  ASK:    AskUserQuestion with the prompt's options
  ROUTE:  Apply all → write all previewed edits + append ledger entries
                       (NEVER includes CLAUDE.md or the self-corruption-excluded files — those force Review each)
          Review each → step through diffs one at a time for per-edit approval
          Apply facts (batch-review) → show ONE consolidated diff of all category-(c) index/fact edits,
                       require a single confirm, then write them; exclusion-set / CLAUDE.md targets are
                       downgraded to Review each and never included in the batch
          Skip (record pending) → write nothing to skills; append the previewed entries to the ledger as `pending`
```

**Apply semantics (batch atomicity).** Apply and verify each approved edit ONE at a time, stop-on-first-mismatch. For each edit: apply the hunk, then diff the touched file against the snapshot ref (NOT the dirty working tree) and assert the applied hunk equals the approved preview. **Append that learning's ledger entry ONLY AFTER its own equality check passes.** If an edit's check fails, revert ONLY that edit's hunk (never `git checkout` the whole file — that would destroy pre-existing user edits), stop the batch, and report. Edits already applied-and-verified earlier in the batch keep their ledger entries; the failed and all subsequent edits do not get applied or ledgered. A ledger entry is never appended for an edit whose post-apply equality check did not pass.

If any learning introduces a NEW trigger ID, you MUST add the same-named entry to BOTH `prompt-layer/TRIGGERS.md` (Lifecycle Triggers row) AND `prompt-layer/PROMPTS.md` (matching `###` prompt) in the same edit batch — a trigger named without both entries is a dangling reference (this exact bug occurred in a prior session).

**Pending ledger appends (Skip route only).** The `Skip (record pending)` route is the ONE durable write outside the apply gate, and it is opt-in and visible: the prompt option text itself says "append the previewed entries to the ledger as `pending`," so the user chose it knowingly. It is bounded the same way as applied edits — append AT MOST the top-5 verified learnings as `pending`, never an unbounded dump, and each pending append must have appeared in the diff preview. Pending entries are inert: per 3d they are NOT authoritative "reality" and can never supersede or suppress a future learning. Only `applied` entries earn that status. The **gate-unresolvable fallback does NOT append anything** — it is propose-only print-and-exit (see above).

Append each applied (or, on the Skip route, each pending) learning to `claude-product-cycle/LEARNINGS.md` (newest first) using this format. Double-brace `{{VAR}}` here matches the output-template convention used by this command's `CURRENT_WORK.md` template above — distinct from the single-brace fire-time context vars in `PROMPTS.md` prompt bodies:

```markdown
## L-{{NNNN}} · {{DATE}} · {{SIGNAL_TYPE}} · {{VERIFIED_OR_UNVERIFIED}}

**Claim (as written)**: {{CLAIM_AS_WRITTEN}}
**Reality**: {{OBSERVED_REALITY}}
**Evidence**: {{VERIFICATION_COMMAND}} → {{COMMAND_RESULT}}
**Affected skill**: {{TARGET_FILE_PATH}}{{OPTIONAL_LINE_REF}}
**Proposed change**: {{DIFF_SUMMARY}}
**Targets applied**: {{TARGETS_APPLIED}}
**Fingerprint**: {{CATEGORY_TARGET_SUBJECT_FACET}}
**Status**: applied | pending | superseded
```

Stale-status drift updates go ONLY to the relevant status file (test-status → `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` per the 3b rule; other drift → `claude-product-cycle/design-spec-layer/STATUS.md`, `claude-product-cycle/platform-layer/LAYER_STATUS.md`, or `claude-product-cycle/PRODUCT_MAP.md` for layers with no status file) — they are NOT laddered into the ledger (a feature finished this week is transient, not a durable learning).

### Step 4: Prompt for Commit

If there are uncommitted changes (including any skill edits applied in Step 3), route the commit through the prompt layer instead of asking inline. Fire the `commit` trigger; after a commit is made, the engine may fire `commit-post`. (After the bootstrap, both rows list `/session-end` in their `Raised by` column — `/session-end` raises `commit` directly, while `commit-post` is engine-fired within its lifecycle, the same transitive sense in which `/gap-status` is listed for `commit-post`.)

> Note for reviewers: the inline `### Uncommitted Changes Detected` option box from the prior version of this command is intentionally replaced by the `commit` trigger routed through the prompt layer — matching how `gap-status.md` fires `commit` (lines 407/423/428, plus the `commit-post` follow-on at 433). This is a deliberate consolidation, not a dropped behavior; the engine's `commit` prompt supplies the commit / show-diff / skip options.

```
TRIGGER: commit
  WHEN:   Files were modified this session and remain uncommitted at session end
  LOAD:   PROMPTS.md → commit
  ASK:    AskUserQuestion with the prompt's options
  ROUTE:  per the commit prompt's option table (Conventional commit / Show diff / Skip)
```

Recommended WIP message when the user opts for a quick save: `wip: [feature] - {{AUTO_SUMMARY}}`. Repo rules still apply: never commit to a shared base branch (`development`, `master`, or a long-lived working branch such as `v2-app-base`); use a `feature/` branch (already created in Step 3e if skill edits were applied); no Claude attribution. If skill edits were applied, commit them separately from feature code so the audit trail stays clean.

### Step 5: Output Session Summary

```markdown
## Session Saved

**Duration**: {{APPROXIMATE_WORK}}
**Commits**: {{COMMIT_COUNT}} commits this session
**Files Changed**: {{FILES_CHANGED}}

---

### What We Accomplished

- {{ACCOMPLISHMENT_1}}
- {{ACCOMPLISHMENT_2}}
- {{ACCOMPLISHMENT_3}}

---

### Skill Retrospective

**Verified learnings applied**: {{APPLIED_COUNT}}
**Skills updated**: {{UPDATED_SKILL_LIST}}
**Ledger**: `claude-product-cycle/LEARNINGS.md` (+{{NEW_LEDGER_ENTRIES}} entries, {{SUPERSEDED_COUNT}} superseded, {{PENDING_COUNT}} pending)
**Unverified observations (dropped, for human review)**: {{UNVERIFIED_LIST}}
**Overflow (beyond top-5, not laddered)**: {{OVERFLOW_LIST}}

---

### Next Session

Start with: `/session-start`
Continue: {{NEXT_TASK}}

---

**Context saved to**: `claude-product-cycle/CURRENT_WORK.md`
**Learnings saved to**: `claude-product-cycle/LEARNINGS.md`
```

## Output Rules

1. Always update CURRENT_WORK.md
2. Capture uncommitted changes
3. Note specific files and line numbers if relevant
4. Be specific about next actions
5. Prompt for commit if changes exist
6. Run the skill retrospective unless `--no-retro` is passed; mine only skills used this session and cap at the top 5 verified learnings (pending ledger appends are capped at the same top 5), ranked by severity tier → target breadth → in-session repetition
7. NEVER write an unverified claim into a skill — verify every candidate against the live codebase with a read-only command first; drop and surface failures. For unwired test tiers, confirm ABSENCE by source-set path check, not `--dry-run`; for a documented-absence-resolved (signal h), confirm PRESENCE by the same path/task check
8. Propose-only by default: no skill edit is applied without explicit approval via the `retrospective-ready` prompt; show a diff preview, no silent overwrites. `Apply facts (batch-review)` shows ONE consolidated diff of all category-(c) edits and requires a single confirm before writing; it can only touch index/fact files — any target in the self-corruption exclusion set or `CLAUDE.md` is downgraded to `Review each`. If the gate cannot resolve, write NOTHING to disk (ledger included) — print previews and exit
9. The retrospective may only correct facts, conventions, and guardrails — it MUST NOT rewrite architecture decisions or override `CLAUDE.md` / org / user-global instructions (per `ENGINE.md` priority). Edits to `CLAUDE.md` are factual-only and require the `Review each` path, since `CLAUDE.md` (priority 3) outranks this command (priority 5)
10. Never reference a trigger ID without adding its paired entry to BOTH `TRIGGERS.md` and `PROMPTS.md` in the same edit batch — this includes `retrospective-ready` itself (Prerequisites bootstrap), whose two new context vars `{VERIFIED_COUNT}`/`{SKILL_LIST}` are also added to the `PROMPTS.md` context-variable enumeration. TRIGGERS rows are static (no `{VAR}` substitution); only PROMPTS bodies carry fire-time context vars
11. Keep transient state in CURRENT_WORK.md and durable learnings in LEARNINGS.md — never mix the two; test-status drift goes to `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` (the verify-tests Primary), other stale-status drift to the layer's actual status file or `claude-product-cycle/PRODUCT_MAP.md` for layers with none — not the ledger
12. All retrospective writes are append/edit only and reversible — assert a non-empty snapshot ref (`git stash create`, else `HEAD`) before applying ANY edit and abort to propose-only if none can be obtained; if HEAD is not already a dedicated `feature/` branch for this session, create one before applying (this covers `development`, `master`, and any shared base branch such as `v2-app-base`); apply+verify each edit one at a time, appending its ledger entry only after its equality check passes, and on mismatch revert only that hunk and stop the batch; never delete a ledger entry (supersede instead); never touch a skill not used this session. NEVER auto-apply (`Apply all` or `Apply facts (batch-review)`) to the retrospective's own machinery — `session-end.md`, `prompt-layer/ENGINE.md`, all of `verify-tests.md`, or `CLAUDE.md` — those force the `Review each` path. The Prerequisites bootstrap is self-approving via a plain `AskUserQuestion`, does NOT depend on `retrospective-ready` being resolvable, and writes its paired `TRIGGERS.md`/`PROMPTS.md` entries atomically with an immediate re-grep verify-or-revert

## Integration with Other Commands

| Command | Purpose |
|---------|---------|
| `/session-start` | Resume context; reads CURRENT_WORK.md and PRODUCT_MAP.md (does not yet read LEARNINGS.md) |
| `/gap-analysis` | Identify gaps across the 5 layers |
| `/gap-status` | Track plan progress |
| `/verify-tests` | Verify test wiring (a frequent retrospective target) |