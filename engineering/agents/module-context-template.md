# Module context template

Template for a per-directory `AGENTS.md` in the code repo, created and maintained by `workflows/agent/update-context.md`. Always create the sibling one-line `CLAUDE.md` shim (`@AGENTS.md`) with it.

Keep it under the budget. Orientation and constraints only - link to specs instead of restating them. Describe what IS, never what should be built.

```markdown
---
scope: {path/to/module/}
features: [FT-NNN, ...]
last-synced: {short SHA}
budget: 150
---

# {Module name}

## Purpose
{1-2 sentences: what this module owns and its bounded context. Link the
architecture context row in engineering/architecture/AGENTS.md.}

## What exists
{One line per significant file/sub-package: name - role. Not a full listing;
what an agent needs to avoid recreating or misplacing things.}

## Public interface
{The endpoints / ports / events / exported functions other modules may use.
Anything not listed here is internal - do not call it from outside.}

## Invariants and gotchas
{The rules an agent could violate: ordering constraints, transaction
boundaries, locked review decisions (D-NN), non-obvious coupling, perf traps.}

## Do NOT recreate
{Existing utilities/helpers/abstractions that agents tend to reimplement:
name - where it lives - what it does.}

## Feature map
| Feature | What it added here |
|---|---|
| FT-NNN | {one line} |
```
