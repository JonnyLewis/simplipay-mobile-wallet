# Prompt Triggers

Defines when automated prompts fire during the product lifecycle. Trigger IDs here are the ones the command files and `CLAUDE.md` dereference (e.g. `gap-planning.md` → `plan-ready`). Each ID maps to a prompt definition of the same name in [`PROMPTS.md`](PROMPTS.md).

---

## Lifecycle Triggers

These fire automatically after a command completes an action. The engine checks for a matching trigger, loads the prompt from `PROMPTS.md`, and presents it via the `AskUserQuestion` tool.

| Trigger ID | Fires When | Prompt intent | Raised by |
|------------|------------|---------------|-----------|
| `phase-completion` | A lifecycle layer finishes (`design`/`server`/`client`/`feature`/`platform`) | "Continue to next phase?" | `/implement`, `/feature`, `/client`, `/design` |
| `task-completion` | One task in a multi-task plan finishes | "Continue to next task?" | `/gap-planning`, `/gap-status` |
| `task-completion:all` | All tasks in a plan finish | "Plan complete. Commit?" | `/gap-planning`, `/gap-status` |
| `plan-ready` | A new plan has been created | "Start implementation?" | `/gap-planning` |
| `plan-layer` | A layer-scoped plan has been created | "Execute this layer's plan?" | `/gap-planning` |
| `plan-continue` | Resuming an in-progress plan | "Continue from current step?" | `/gap-status` |
| `build-success` | A Gradle build/test succeeds | "Commit & continue?" | `/implement`, `/feature`, `/client` |
| `build-failure` | A Gradle build/test fails | "Fix errors?" | `/implement`, `/feature`, `/client` |
| `commit` | Files modified at a checkpoint | "How to commit?" | `/gap-status`, `/session-end` |
| `commit-post` | Immediately after a commit is made | "Push / open PR / continue?" | `/gap-status`, `/session-end` |
| `error-recovery` | An unrecoverable error occurs mid-task | "How to proceed?" | any command |
| `retrospective-ready` | Verified learnings ready for review at session end | "Apply learnings?" | `/session-end` |

`phase-completion` accepts a layer suffix — `phase-completion:design`, `phase-completion:server`, `phase-completion:client`, `phase-completion:feature`, `phase-completion:platform` — so the prompt can name the next layer.

---

## Trigger Format

Each lifecycle trigger resolves through this structure:

```
TRIGGER: [trigger-id]
  WHEN:   [the action that just completed]
  LOAD:   PROMPTS.md → [trigger-id]
  ASK:    AskUserQuestion with the prompt's options
  ROUTE:  map the user's selection to the next command/action
```

Do NOT hardcode the prompt text in commands — the command only names the trigger ID; the engine resolves text and options from `PROMPTS.md`.

---

## Pattern Reminders (code-generation guardrails)

Lower-priority reminders the engine applies while generating code. These are guardrails, not user prompts — they don't fire `AskUserQuestion`.

| Reminder | When | Check |
|----------|------|-------|
| Feature start | `/implement` or `/feature` begins | Check MODULES_INDEX.md and FEATURE_MAP.md for existing files before creating |
| New service | Writing a Ktorfit service | Endpoint exists in API_INDEX.md? KtorfitClient registration needed? |
| New ViewModel | Writing a ViewModel | Extends `BaseViewModel` from `org.mifospay.core.ui.utils.BaseViewModel`; `viewModelOf(::X)` in `di/XModule.kt`; module added to `KoinModules.allModules` |
| New navigation | Writing `XNavigation.kt` | Register destination in `cmp-shared/.../navigation/MifosNavHost.kt` |
| Repository impl | Writing `XRepositoryImpl.kt` | `asDataStateFlow()` for queries; `withContext(ioDispatcher)` for mutations; inject `ioDispatcher` via Koin |

---

## Related Files

- `ENGINE.md` — cross-instruction priority and rules
- `PROMPTS.md` — prompt definitions keyed by the trigger IDs above
- `RUNTIME.md` — dynamic runtime prompt assembly
