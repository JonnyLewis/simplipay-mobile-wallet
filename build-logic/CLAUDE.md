# Build logic guidance

This file contains guidance for work inside `build-logic/`.

## Why this area matters

`build-logic/` is high blast radius. Changes here can affect many or all modules in the repository through shared Gradle conventions, plugins, dependency behavior, versioning, and module graph behavior.

Treat all non-trivial edits here as high risk.

## Core rule

Do not casually edit build logic.

Before editing code in `build-logic/`, identify:
- which modules consume the convention or plugin
- whether the change affects Android, KMP, Compose, KSP, Ktorfit, signing, versioning, or module graph behavior
- whether the change affects CI or release flows
- whether the change affects generated code or plugin application order

## Build logic rules

When working here:
- make the smallest safe change
- isolate the exact convention/plugin being modified
- do not mix style cleanup with behavior changes
- state expected downstream module impact explicitly
- be extra careful with shared plugin aliases, multiplatform conventions, and module graph behavior

## Validation expectations

If `build-logic/` changes:
- validate the smallest affected sample first
- call out all likely downstream module categories affected
- recommend broader validation when impact is repo-wide

## Avoid

- casual repo-wide convention changes
- changing plugin wiring without tracing consumers
- changing generated-code assumptions without clear validation
- mixing versioning/release/signing changes with unrelated edits