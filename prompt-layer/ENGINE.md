# Prompt Engine

Cross-instruction coordination rules for automated prompt triggers in this project.

---

## Purpose

The Prompt Engine defines how automated prompts are triggered, which instructions take priority, and how dynamic runtime context is injected into prompts.

---

## Instruction Priority

```
1. Organization Instructions (highest)
2. User global instructions (~/.claude/CLAUDE.md)
3. Project instructions (CLAUDE.md at repo root)
4. Prompt Layer triggers (this directory)
5. Command instructions (.claude/commands/*.md)
```

---

## Engine Rules

1. **No overriding architecture decisions** — Prompts cannot change the tech stack (KMP, Compose, Koin, Ktorfit).
2. **Always reference O(1) indexes** — Generated code prompts must check MODULES_INDEX.md and FEATURE_MAP.md before creating new files.
3. **Respect naming conventions** — All generated code must follow `org.mifospay.*` package structure.
4. **Repository impl naming** — Always `${Feature}RepositoryImpl`, never `RepositoryImp`.
5. **Navigation registration** — New features must register in `MifosNavHost.kt` and `KoinModules.kt`.

---

## Context Variables

The prompt layer uses two substitution conventions, distinct by role — they are NOT interchangeable:

- **Single-brace `{VAR}` — fire-time prompt variables.** The engine substitutes these into a prompt body when a trigger fires (see `PROMPTS.md`): `{FEATURE}`, `{LAYER}`, `{TOTAL_TASKS}`, `{CURRENT_TASK}`, `{NEXT_TASK}`, `{VERIFIED_COUNT}`, `{SKILL_LIST}`.
- **Double-brace `{{VAR}}` — output-template placeholders.** A command fills these in when it WRITES a document — `CURRENT_WORK.md`, the `LEARNINGS.md` ledger, plan files under `plans/`, and the runtime examples in `RUNTIME.md`.

| Variable | Convention | Source | Example |
|----------|------------|--------|---------|
| `{FEATURE}` | fire-time `{VAR}` | Current feature being worked on | `beneficiary` |
| `{LAYER}` | fire-time `{VAR}` | Current layer | `client` |
| `{{DATE}}` | output-template `{{VAR}}` | Current date | `2026-06-23` |
| `{{SPEC_PATH}}` | output-template `{{VAR}}` | Feature spec path | `claude-product-cycle/design-spec-layer/features/beneficiary/SPEC.md` |

---

## Related Files

- `TRIGGERS.md` — When prompts fire
- `PROMPTS.md` — Prompt definitions
- `RUNTIME.md` — Dynamic runtime prompt assembly
