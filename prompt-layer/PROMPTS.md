# Prompt Definitions

Two kinds of prompt live here:

1. **Lifecycle prompts** — keyed by the trigger IDs in [`TRIGGERS.md`](TRIGGERS.md). When a command names a trigger (e.g. `gap-planning.md` → `plan-ready`), the engine loads the matching entry below and presents it via `AskUserQuestion`.
2. **Pattern prompts** — reusable code-generation templates referenced by `RUNTIME.md` (e.g. `PROMPT:new-service`).

Context variables (`{FEATURE}`, `{LAYER}`, `{TOTAL_TASKS}`, `{CURRENT_TASK}`, `{NEXT_TASK}`, `{VERIFIED_COUNT}`, `{SKILL_LIST}`) are injected at fire time per `ENGINE.md`.

---

## Lifecycle Prompts

### plan-ready

**Trigger**: `plan-ready` — fired after `/gap-planning` creates a plan.
**Context**: `{FEATURE}`, `{TOTAL_TASKS}`

> Plan for **{FEATURE}** created with {TOTAL_TASKS} tasks. How do you want to proceed?

| Option | Routes to |
|--------|-----------|
| Start implementation | Execute task 1 of the plan |
| Review plan | Show the full task breakdown |
| Modify plan | Let the user adjust tasks |
| Save for later | End without executing |

### plan-layer

**Trigger**: `plan-layer` — fired after a layer-scoped plan is created.
**Context**: `{LAYER}`, `{TOTAL_TASKS}`

> {LAYER} layer plan created ({TOTAL_TASKS} tasks). What next?

| Option | Routes to |
|--------|-----------|
| Execute plan | Start implementing the layer |
| View tasks | Show the detailed task list |
| Plan next layer | Continue planning |
| Stop here | Save plan, implement later |

### plan-continue

**Trigger**: `plan-continue` — fired by `/gap-status` when resuming an in-progress plan.
**Context**: `{FEATURE}`, `{CURRENT_TASK}`, `{TOTAL_TASKS}`

> **{FEATURE}** is at task {CURRENT_TASK}/{TOTAL_TASKS}. Continue?

| Option | Routes to |
|--------|-----------|
| Continue | Execute the current step |
| Show remaining | List outstanding tasks |
| Pause | Leave plan as-is |

### task-completion

**Trigger**: `task-completion` — fired after one plan task completes.
**Context**: `{CURRENT_TASK}`, `{TOTAL_TASKS}`, `{NEXT_TASK}`

> Task {CURRENT_TASK}/{TOTAL_TASKS} complete. Continue to {NEXT_TASK}?

| Option | Routes to |
|--------|-----------|
| Continue | Execute the next task |
| Commit first | Fire `commit`, then continue |
| Stop | Save progress, end |

### task-completion:all

**Trigger**: `task-completion:all` — fired when all plan tasks complete.
**Context**: `{FEATURE}`, `{TOTAL_TASKS}`

> All {TOTAL_TASKS} tasks for **{FEATURE}** complete. Commit?

| Option | Routes to |
|--------|-----------|
| Commit | Fire `commit` |
| Verify first | Run `/verify {FEATURE}` |
| Done | Update PLANS_INDEX.md and end |

### phase-completion

**Trigger**: `phase-completion` / `phase-completion:{layer}` — fired when a lifecycle layer finishes.
**Context**: `{LAYER}`, `{FEATURE}`

> {LAYER} layer for **{FEATURE}** complete. Continue to the next phase?

| Option | Routes to |
|--------|-----------|
| Next phase | Run the next layer command (Design→Server→Client→Feature→Platform) |
| Verify | Run `/verify {FEATURE}` |
| Stop here | End |

### build-success

**Trigger**: `build-success` — fired after a successful Gradle build/test.
**Context**: `{FEATURE}`

> Build succeeded for **{FEATURE}**. Commit & continue?

| Option | Routes to |
|--------|-----------|
| Commit & continue | Fire `commit`, then proceed |
| Continue only | Proceed without committing |
| Stop | End |

### build-failure

**Trigger**: `build-failure` — fired after a failed Gradle build/test.
**Context**: `{FEATURE}`, `{ERROR}`

> Build failed for **{FEATURE}**: {ERROR}. How to proceed?

| Option | Routes to |
|--------|-----------|
| Fix errors | Analyze the failure and apply a fix |
| Show output | Print the full build log |
| Abort | Stop implementation |

### commit

**Trigger**: `commit` — fired when files are modified at a checkpoint.
**Context**: `{FEATURE}`, `{CHANGED_FILES}`

> {CHANGED_FILES} changed for **{FEATURE}**. How should I commit?

| Option | Routes to |
|--------|-----------|
| Conventional commit | Create a feature branch + `<type>(<scope>): <subject>` commit |
| Show diff | Print the staged changes first |
| Skip | Continue without committing |

> Follows repo rules: never commit to `development`; use a `feature/` branch; no Claude attribution in the message.

### commit-post

**Trigger**: `commit-post` — fired immediately after a commit is made.
**Context**: `{BRANCH}`

> Committed on `{BRANCH}`. What next?

| Option | Routes to |
|--------|-----------|
| Push | `git push origin {BRANCH}` |
| Open PR | Push and open a PR targeting `development` |
| Continue | Proceed with the next task |

### error-recovery

**Trigger**: `error-recovery` — fired on an unrecoverable mid-task error.
**Context**: `{ERROR}`, `{CONTEXT}`

> Hit an error: {ERROR}. How to proceed?

| Option | Routes to |
|--------|-----------|
| Retry | Re-attempt the failed step |
| Skip | Skip this step and continue |
| Abort | Stop and report state |

### retrospective-ready

**Trigger**: `retrospective-ready` — fired by `/session-end` when verified learnings are ready for review at session end.
**Context**: `{VERIFIED_COUNT}`, `{SKILL_LIST}`

> {VERIFIED_COUNT} verified learning(s) ready for {SKILL_LIST}. How should I apply them?

| Option | Routes to |
|--------|-----------|
| Apply all | Write all previewed edits + append ledger entries (NEVER includes CLAUDE.md or the self-corruption-excluded files — those force Review each) |
| Review each | Step through diffs one at a time for per-edit approval |
| Apply facts (batch-review) | Show ONE consolidated diff of all category-(c) index/fact edits, require a single confirm, then write them; exclusion-set / CLAUDE.md targets are downgraded to Review each and never included in the batch |
| Skip (record pending) | Write nothing to skills; append the previewed entries to the ledger as `pending` |

---

## Pattern Prompts (code-generation templates)

Referenced by `RUNTIME.md` decision trees (e.g. `PROMPT:new-service`).

### PROMPT: new-service

**Use**: Creating a new Ktorfit service interface

```
Create a Ktorfit service interface for [Feature] in the MifosX Mobile Wallet project.

Context:
- Package: org.mifospay.core.network.services
- Use @GET, @POST, @PUT, @DELETE from de.jensklingenberg.ktorfit.http.*
- Endpoint constants from ApiEndPoints object
- Return Flow<List<T>> for list queries, suspend fun for mutations
- Reference: core/network/src/commonMain/kotlin/org/mifospay/core/network/services/BeneficiaryService.kt

After creating the service, register it in KtorfitClient as
`internal val [feature]Api by lazy { ktorfit.create[Feature]Service() }`,
then expose it on the appropriate manager (SelfServiceApiManager / FineractApiManager)
as `val [feature]Api by lazy { ktorfitClient.[feature]Api }`.
```

### PROMPT: new-repository

**Use**: Creating a new repository implementation

```
Create a repository implementation for [Feature] in the MifosX Mobile Wallet project.

Context:
- Interface in: core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/
- Impl in: core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/
- Class name: [Feature]RepositoryImpl (with lowercase 'l' at end)
- Inject: SelfServiceApiManager or FineractApiManager + CoroutineDispatcher
- Use asDataStateFlow().flowOn(ioDispatcher) for queries
- Use withContext(ioDispatcher) + try/catch for mutations
- Register in RepositoryModule.kt: single<[Feature]Repository> { [Feature]RepositoryImpl(get(), get(ioDispatcher)) }
- Reference: BeneficiaryRepositoryImpl.kt
```

### PROMPT: new-viewmodel

**Use**: Creating a new ViewModel

```
Create a ViewModel for [Feature] using the BaseViewModel MVI pattern.

Context:
- Package: org.mifospay.feature.[package]
- Import BaseViewModel from: org.mifospay.core.ui.utils.BaseViewModel
- Create: [Feature]State, [Feature]Event, [Feature]Action
- Mutate state via mutableStateFlow.update { it.copy(...) }; background work via launchIO { }
- Dispatch actions externally via trySendAction(...); handle them in protected handleAction(...)
- Register DI: di/[Feature]Module.kt with viewModelOf(::[Feature]ViewModel)
- Add to KoinModules.allModules in cmp-shared/.../di/KoinModules.kt
- Reference: feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeViewModel.kt
```

### PROMPT: verify-di

**Use**: Checking DI is properly wired

```
Verify DI registration for [Feature]:
1. Check di/[Feature]Module.kt has viewModelOf(::[Feature]ViewModel)
2. Check cmp-shared/.../di/KoinModules.kt has [feature]Module in allModules
3. Check core/data/.../di/RepositoryModule.kt has [Feature]Repository binding
4. Check KtorfitClient has [feature]Api lazy property
```

### PROMPT: verify-navigation

**Use**: Checking navigation is properly wired

```
Verify navigation for [Feature]:
1. Check navigation/[Feature]Navigation.kt exists with @Serializable [Feature]Route
2. Check cmp-shared/.../navigation/MifosNavHost.kt has [feature]Screen() call
3. Check [Feature]Screen accepts navigation callbacks (not direct NavController)
```

### PROMPT: new-spec

**Use**: Creating a SPEC.md for a feature

```
Create a SPEC.md for [Feature] in the MifosX Mobile Wallet.

App context: Mobile payments and wallet platform supporting transfers, QR payments,
autopay, invoices, and account management. Built with Kotlin Multiplatform + Compose.

Sections to include:
1. Overview (summary, user stories)
2. Screen Layout (ASCII mockup, sections table)
3. User Interactions (action/trigger/result table)
4. State Model (State class, UiState, Event, Action)
5. API Requirements (endpoints table)
6. Edge Cases & Error Handling

File: claude-product-cycle/design-spec-layer/features/[feature]/SPEC.md
```
