# import-codebase workflow

Retrofit this kit onto an existing brownfield codebase. After this workflow, the target repository has the same operating system a greenfield product gets from `init-product`: the shared agent contract, the workflows and skills, the gated SDLC, the per-directory context tree, session continuity, and the consistency checks - built from the code that already exists, not from an empty template.

The inverse of `init-product`: there, the interview defines the product and the specs come first. Here, **the code is the senior witness** - the workflow reads what exists, asks only what code cannot reveal, and labels everything it inferred as `observed` until the user confirms it.

## Inputs

| Input | How obtained |
|---|---|
| `KIT_ROOT` | The codebot kit repo this workflow runs from |
| `TARGET` | Absolute path to the brownfield repo - ask if not given |
| Deployment shape | In-place (kit files inside the code repo - default for a single repo) or sidecar (separate spec repo pointing at the code repo - offer for multi-repo products) |
| Kit weight | Light / Standard / Full - same scale-by-deletion rule as `init-product` |

## Step 0 - pre-flight (hard gates)

1. `TARGET` exists, is a directory, and is a git repository. If not: stop and ask.
2. `git -C TARGET status --porcelain` is clean. If dirty: stop and ask the user to commit or stash first - the import must land as one reviewable changeset.
3. Detect collisions: existing `AGENTS.md`, `CLAUDE.md`, `.claude/`, `.codex/`, `scripts/check-state.sh` in `TARGET`. Record them; the merge policy in Step 3 applies. **Never overwrite an existing file without showing the user what would change.**
4. If `TARGET` already contains this kit (root `AGENTS.md` referencing `workflows/agent/`), offer `start-session` or a kit **upgrade** (refresh workflow bodies and scripts only; never touch filled product content) instead of a re-import.

## Step 1 - survey the codebase (read-only, bounded)

Build the inventory without reading the whole repository. Read manifests, configs, and directory listings first; open source files only to resolve ambiguity.

| Question | Evidence |
|---|---|
| Stack and runtime | `package.json`, `pom.xml`/`build.gradle`, `go.mod`, `requirements.txt`/`pyproject.toml`, `*.csproj`, lockfiles |
| Build / test / lint commands | manifest scripts, `Makefile`, `Taskfile`, CI config (`.github/workflows/`, etc.) |
| Module map | top-level and second-level directory structure; monorepo workspaces |
| Frontend/backend separation | candidate root pairs (`frontend/`+`backend/`, `client/`+`server/`, `web/`+`api/`, `app/`+`services/`), workspace definitions, per-directory manifests, framework markers (React/Vue/Angular/Svelte vs. Spring/Express/Django/Rails/.NET) |
| Persistence and entities | migrations, schema files, ORM models (inventory only) |
| API surface | route definitions, controllers, OpenAPI/proto files (inventory only) |
| Existing conventions | linter configs, error-handling and naming patterns in 2-3 representative files per major module |
| Existing agent/docs assets | `README`, `docs/`, `CONTRIBUTING`, any `AGENTS.md`/`CLAUDE.md`/cursor rules already present |
| Tests | framework, where they live, whether they pass (run the test command once if cheap; record result, do not fix anything) |

Output: a survey table the user sees in Step 2. Every entry is labelled `observed`. Cap the survey at roughly 30 file reads; gaps become open questions, not deeper excavation.

### Repo shape classification (mandatory)

Classify the target into exactly one shape. Decide from evidence; **never guess on conflicting signals**.

| Shape | Evidence rule | Consequence |
|---|---|---|
| `split` | Two distinct roots, each with its own manifest and unambiguous framework markers (one frontend, one backend) - e.g. `client/` with React + `server/` with Express, or workspace packages split the same way | The separation **must be used**: per-side stack rows and root paths in `engineering/AGENTS.md`, per-side patterns files, per-side context-tree roots in Step 4 |
| `frontend-only` | Only frontend markers anywhere in the tree | Single-unit treatment; only `frontend-patterns.md` is kept |
| `backend-only` | Only backend markers anywhere in the tree | Single-unit treatment; only `backend-patterns.md` is kept |
| `monolith` | Both technologies present but interleaved in one tree with no clean boundary (server-rendered templates, inline assets, a single `src/` mixing both) | Treat as one unit: **no front/back distinction** in the repos table or context tree; one combined `code-patterns.md` replaces the two pattern files |
| `ambiguous` | Signals conflict - e.g. two manifests but a shared `src/`, three candidate roots, framework markers on both sides of an unclear boundary | Do not classify. Add the shape question to the Step 2 interview and let the user choose `split` (naming the two roots), `monolith`, or one-side |

Record the shape and its evidence in the survey table as `observed` (or `user-declared` when the user resolved an ambiguous case).

## Step 2 - delta interview

Ask only what the code cannot say. One round, 5-8 questions:

1. What is this product, in one or two sentences - and what is it deliberately **not**?
2. What is the scope fence going forward? (MVP-done / active feature set vs. explicitly out.)
3. Which of the observed conventions are load-bearing rules vs. accidents to migrate away from?
4. Who approves feature briefs and plans?
5. Hard constraints: regulatory, data residency, deadlines.
6. Deployment shape and kit weight (from Inputs, if not already given).
7. **Only if repo shape is `ambiguous`:** "I see {evidence for each candidate}. Is this a frontend+backend split (which directories are the two roots?), a monolith with no separation, or frontend-/backend-only?" Skip this question entirely when detection was conclusive.

Then present the survey + answers as a confirmation table. **Do not write anything to `TARGET` until the user confirms.**

## Step 3 - install the kit scaffolding

Copy from `KIT_ROOT`, instantiate, never blind-overwrite:

| What | To | Collision policy |
|---|---|---|
| `workflows/agent/*.md` | `TARGET/workflows/agent/` | Overwrite only kit-owned bodies on upgrade; otherwise create |
| `.claude/skills/*` | `TARGET/.claude/skills/` | Skip any skill name that already exists; report it |
| `scripts/check-state.sh` | `TARGET/scripts/` | Create; if present, diff and ask |
| `.codex/` notes + config example | `TARGET/.codex/` | Create |
| `.claude/settings.json` safe-tool allowlist | `TARGET/.claude/settings.json` | Merge per `workflows/agent/setup-permissions.md`; never clobber |
| `engineering/` skeleton (sdlc/, agents/ incl. `context-index.md` + `module-context-template.md`, features/ with empty `backlog.md`, architecture/) | `TARGET/engineering/` | Create |
| `docs/00-document-index.md`, `docs/05-mvp-scope.md`, `docs/_TEMPLATE-spec.md` | `TARGET/docs/` | Create; if `docs/` exists, add without disturbing existing files and index them |

Then write the foundation files **filled, not as templates**:

1. `TARGET/AGENTS.md` - root contract from the survey + interview, **≤ 150 lines**: identity, negative scope, scope fence, observed stack table (each row `locked (observed)` or `undecided`), multi-agent rule, workflow table, discipline pointer table, non-negotiables. No unresolved `{{placeholders}}`.
2. `TARGET/CLAUDE.md` - the thin shim importing `@AGENTS.md` (merge: if a `CLAUDE.md` with real content exists, move its content into `AGENTS.md` or a folder context file, then reduce it to the shim + Claude-only notes).
3. `TARGET/engineering/AGENTS.md` - repos table shaped by the classification (`split`: one row per side with its root path and stack; otherwise: a single row, no front/back distinction), build/test/lint commands as observed, and the ED register: observed conventions worth enforcing become `ED-N (observed)`; kit-standard EDs the codebase does not yet meet become `ED-N (proposed)` with the gap noted. Never present a proposed ED as already true.
   - Architecture patterns by shape: `split` → instantiate both `engineering/architecture/backend-patterns.md` and `frontend-patterns.md` for the respective stacks; `frontend-only`/`backend-only` → keep only the matching file, delete the other; `monolith` → one combined `engineering/architecture/code-patterns.md`, delete both split files and point `engineering/sdlc/04-implementation.md` context references at it.
4. `TARGET/docs/05-mvp-scope.md` - the scope fence going forward, from the interview.
5. `TARGET/SESSION_STATE.md` - section 1 "Imported brownfield codebase on {date}"; section 2 status by area (most rows `observed - unverified`); section 4 next concrete action; section 6 open questions from survey gaps and every `(proposed)` ED.

## Step 4 - build the context tree

Run the `update-context` workflow (`workflows/agent/update-context.md`) against `TARGET` for its first pass:

- Per-directory `AGENTS.md` + one-line `CLAUDE.md` shim for each module meeting the trigger rules, from `module-context-template.md`, each within budget, `last-synced` set to the current HEAD.
- Roots follow the repo shape: `split` → one context root per side (the frontend root and the backend root each get their own file, then their modules); all other shapes → a single root, no front/back distinction.
- **Bound the first pass:** top-level modules first, maximum ~15 context files. Deeper directories split out later when `update-context` budget pressure demands it - the tree deepens on demand, not speculatively.
- Register everything in `engineering/agents/context-index.md`.

This is the highest-value artifact of the import: the next agent session loads module context instead of re-exploring the codebase.

## Step 5 - baseline reverse-engineered specs (inventory, not invention)

Record what is observable; fabricate nothing:

- `data-model/entities.md` - entity **inventory** from migrations/models: name, table, module. Status: `observed - unverified`. No invented lifecycles; if state columns/enums exist, list raw values as an open question for a proper status model.
- `api/api-contract.md` - endpoint **inventory**: method, path, module. Status: `observed - unverified`.
- Skip both (leave kit templates) if the survey could not see schema or routes cheaply. An honest gap beats a fabricated spec - the kit's no-fabrication rule applies to brownfield too.

## Step 6 - verify

1. `TARGET/scripts/check-state.sh` passes: 0 errors.
2. Placeholder sweep: `grep -rln "{{" TARGET --include="*.md"` returns only intentionally-left template files.
3. The observed build/test command still passes (or fails identically to the Step 1 baseline) - the import adds files; it must not change behaviour.
4. `git -C TARGET status` shows only added files plus the explicitly-merged collisions from Step 3.

## Step 7 - report and handoff

- Survey summary: stack, modules, test baseline.
- Files installed; collisions and how each was merged.
- Context tree: N module files created, total lines, registry path.
- EDs: observed vs. proposed counts; specs recorded as inventory.
- Open questions (survey gaps + proposed EDs).
- Next concrete action from `SESSION_STATE.md` §4 - typically: *"Review the (proposed) EDs and the scope fence, then run `build-ft` for the first feature."*
- Remind: commit the import as one changeset (the user commits; do not commit unless asked).

## Failure handling

| Failure | Response |
|---|---|
| `TARGET` missing / not git | Stop; ask for the correct path |
| Dirty working tree | Stop; ask the user to commit/stash first |
| Kit already installed | Offer `start-session` or upgrade mode; never blind re-import |
| Existing `AGENTS.md`/`CLAUDE.md` with content | Merge per Step 3; show the user the diff before writing |
| Skill name collision | Skip, report, let the user resolve |
| Monorepo with many packages | Confirm scope: import at the root with per-package context files, or one package only |
| Front/backend signals conflict | Classify `ambiguous`; ask the user in Step 2 - never guess a split that is not there |
| Huge repo blows the survey cap | Stop expanding; record gaps as open questions |
| Tests already failing pre-import | Record as baseline; not the import's problem; do not fix silently |

## Rules

- The import is **additive**: never modify existing source code, CI, or docs content.
- Everything inferred is labelled `observed` or `proposed` - never presented as user-confirmed.
- No fabricated lifecycles, audit events, or API semantics. Inventory only.
- Respect the survey read cap and the context-tree file cap; deepen on demand later.
- Confirm before writing; one reviewable changeset; the user commits.
