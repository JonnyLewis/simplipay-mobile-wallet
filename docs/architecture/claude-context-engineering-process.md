# Claude Code Context Engineering Process

## Purpose

This document records the process followed to prepare the `simplipay-mobile-wallet` repository for high-accuracy Claude Code usage.

The goal was not just to add prompt files, but to build a **repo-aware context system** so Claude can:
- understand the real structure of the codebase
- work with less slop and fewer assumptions
- respect shared-contract blast radius
- follow a disciplined **Research → Plan → Implement** workflow
- generate safer, more grounded code changes in a brownfield financial application

## Why this was needed

At the start, the repo was being treated more like a normal Android/iOS app. Once the real repository shape was reviewed, it became clear that this is a much more complex codebase:

- Kotlin Multiplatform / Compose Multiplatform
- shared code in `cmp-shared/`
- many feature modules in `feature/`
- shared infrastructure in `core/` and `core-base/`
- platform shells in `cmp-android/` and `cmp-ios/`
- custom build conventions in `build-logic/`
- release and deployment automation in `fastlane/`, `.github/`, and scripts

Because of that, a single generic root prompt would have been too weak and too noisy.

We needed Claude Code guidance that was:
- scoped
- evidence-based
- repo-specific
- maintainable over time

## What we decided to build

We designed a layered context system with:

- one root `CLAUDE.md` for repo-wide rules
- scoped `CLAUDE.md` files for major repo areas
- `.ai/` folders for research, plans, and reviews
- `.claude/` folders for skills, agents, and settings
- a disciplined audit-and-rewrite process before trusting the docs

The intent was to move from:
- **generic prompt engineering**
to:
- **repo-specific context engineering**

## Repo areas identified

After reviewing the actual repository shape, the main context areas were identified as:

- `CLAUDE.md` — repo-wide rules
- `cmp-shared/CLAUDE.md` — shared cross-platform assembly rules
- `feature/CLAUDE.md` — feature module guidance
- `core/CLAUDE.md` — shared infrastructure and layered core guidance
- `android/CLAUDE.md` — Android context note and redirect to `cmp-android/`
- `ios/CLAUDE.md` — iOS context note and redirect to `cmp-ios/`
- `build-logic/CLAUDE.md` — Gradle/build convention safety rules
- `scripts/CLAUDE.md` — automation and script safety rules

Later, additional repo areas were also reviewed:
- `.github/CLAUDE.md`
- `fastlane/CLAUDE.md`

## What we did

### 1. Recognised the repo shape correctly

The first step was understanding that the repo is not a simple mobile app repo.

From the actual tree and upstream Mifos Pay research, it was identified as:
- KMP / Compose Multiplatform
- modular
- multi-surface
- brownfield
- financial domain
- shared-contract heavy

This changed the context strategy completely.

### 2. Designed the scoped context model

Instead of using one giant `CLAUDE.md`, a layered structure was chosen:
- root rules for all tasks
- local rules for major folders
- future skills and agents for reusable specialist behavior

This keeps repo-wide rules concise while still giving Claude deep local guidance when working in the relevant directories.

### 3. Created the bootstrap scaffold

A bootstrap shell script was created to:
- create `.ai/` directories
- create `.claude/` directories
- create empty `CLAUDE.md` files in the right scoped locations
- prepare the repo for structured context engineering work

This made the setup repeatable and easy to apply in the project root.

### 4. Drafted the initial context files

Initial versions of the markdown files were proposed for:
- root
- shared
- feature
- core
- Android
- iOS
- build logic
- scripts

These were good starting points, but still partly generic.

### 5. Audited the context files against real code

Instead of trusting the drafts immediately, the next step was a **code-grounded context audit**.

Claude was instructed to compare the `CLAUDE.md` files against the actual repository and classify:
- what was confirmed
- what was missing
- what was too generic
- what was inaccurate
- what should not be added yet because it was not verified

This was a key step. It prevented the repo docs from becoming “smart-looking fiction.”

### 6. Produced a rewrite plan

From the audit, a rewrite plan was created.

For each `CLAUDE.md` file, the plan specified:
- what to add
- what to remove
- what to rewrite
- what to leave unchanged
- what must not be added yet

This created a controlled path from draft files to high-confidence repo docs.

### 7. Drafted improved versions

Using the rewrite plan, improved drafts were created for the target files.

The aim was:
- more verified detail
- more repo truth
- less generic wording
- no invented architecture claims

### 8. Reviewed the drafts before applying them

A second review pass was then performed against:
- the audit
- the rewrite plan
- the draft files

This review checked for:
- statements that overreached beyond verified facts
- misleading wording
- unnecessary verbosity
- claims based on weak inference

This found:
- 2 blocker issues
- 2 minor fixes

That meant the system was almost ready, but still required a final tightening pass before filing the docs as real repo files.

## Key findings from the audit

Some of the most important repo truths discovered were:

### The repo is KMP / Compose Multiplatform
This is foundational. It changes how Claude should reason about shared code, blast radius, and platform boundaries.

### `feature/CLAUDE.md` was empty
This was the biggest gap because `feature/` is one of the most active and important areas in the codebase.

### `android/` and `ios/` are placeholder directories
The real platform shells are:
- `cmp-android/`
- `cmp-ios/`

The `android/` and `ios/` directories contain only their `CLAUDE.md` files, so the docs needed to make this explicit.

### Shared blast radius is real
Changes in:
- `cmp-shared/`
- `core/`
- `core-base/`
- build logic
can affect multiple features and multiple surfaces.

### The codebase still carries upstream Mifos identity
Confirmed identifiers included:
- root project name `mobile-wallet`
- Android namespace `org.mifospay`
- “Mifos Initiative” branding in code/config

This required a careful documentation note so Claude uses **code truth** when discussing package names, app IDs, namespaces, and config.

### Build logic needed more specificity
The audit confirmed actual convention plugins, static analysis tooling, git hook behavior, and versioning setup, which made `build-logic/CLAUDE.md` far more useful once grounded.

## Why this process mattered

Without this process, the repo would likely have ended up with:
- over-generic Claude docs
- inaccurate assumptions
- weak guidance for shared code
- confusion between platform shells and placeholder directories
- brittle or misleading repo context

By auditing everything against code first, the resulting docs become:
- more trustworthy
- more reusable
- more maintainable
- safer for future coding tasks

This is especially important in a financial app, where wrong assumptions can cause harmful code changes.

## Outcome so far

At the current stage, the work has produced:

### Completed
- repo structure understood correctly
- scoped context model designed
- bootstrap script created
- initial drafts created
- code-grounded context audit completed
- rewrite plan completed
- draft review completed

### Remaining before full filing
Only a small number of final corrections remained before the drafts could be written into the real `CLAUDE.md` files:

#### Blockers
1. `cmp-shared-CLAUDE` draft contained one unverified “navigation host” statement that needed softening or removal
2. `scripts-CLAUDE` draft incorrectly implied all scripts in `scripts/` were iOS-focused, when some are platform-agnostic git hooks

#### Minor polish
1. `build-logic-CLAUDE` needed a more precise `gradle/libs.versions.toml` path reference
2. `feature-CLAUDE` needed a slightly more precise explanation of `core:domain` blast radius as transitive via `core:data`

## What this enables

Once the final draft fixes are applied and the real `CLAUDE.md` files are updated, Claude Code is ready to begin real coding work with much better context.

That means Claude will have:
- repo-wide operating rules
- scoped local guidance
- clearer shared-code awareness
- clearer platform-shell awareness
- better understanding of blast radius
- stronger guardrails for a financial brownfield app

## Recommended coding workflow going forward

For real coding work, the recommended workflow is:

1. **Research**
   - inspect the code
   - identify exact files, flows, and dependencies
   - write findings to `.ai/research/...`

2. **Plan**
   - define exact files to change
   - state intended behavior change
   - state what must not change
   - write plan to `.ai/plans/...`

3. **Implement**
   - make the smallest safe patch
   - avoid scope creep
   - summarize changes and validation

4. **Review reusable truth**
   - ask whether the task uncovered any reusable repo truth that should be added back to a relevant `CLAUDE.md` or skill

## Final summary

In short, this process turned Claude setup from a **prompt-writing exercise** into a **repo-specific context engineering system**.

We did not just write markdown files.

We:
- inspected the real codebase
- mapped its structure properly
- designed a scoped context model
- audited draft guidance against code
- planned and reviewed rewrites
- reduced unverified assumptions
- prepared Claude to work more accurately inside a complex KMP financial wallet repo

The result is a much stronger foundation for Claude-assisted coding, with less slop, less drift, and better awareness of real repo boundaries and shared-contract risk.