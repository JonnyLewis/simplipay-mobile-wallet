# Scripts and automation guidance

This file contains guidance for work inside `scripts/` and repo automation scripts.

## Why this area matters

Scripts in this repo may affect setup, CI, signing, keystore handling, sync workflows, module graph generation, pre-push checks, and release support. These changes can have indirect high blast radius even when the script diff is small.

## Core rule

Do not casually edit scripts.

Before editing a script, identify:
- who or what calls it
- whether it is local-dev only, CI-related, release-related, or signing-related
- which paths, tools, or environment variables it depends on
- whether it interacts with secrets, keystores, generated files, or module graphs

## Script rules

When working on scripts:
- preserve existing calling conventions unless explicitly changing them
- prefer portable, readable shell behavior
- do not hard-code secrets
- do not silently change path assumptions
- do not silently change environment variable names or meanings
- call out any developer workflow impact

## Special caution

Treat these as high risk:
- pre-push / CI scripts
- signing or keystore scripts
- sync scripts
- module graph generation
- release automation
- scripts that modify repo structure or generated artifacts

## Validation expectations

If a script changes:
- describe how it was validated
- describe who is affected
- describe any environment assumptions
- call out anything not validated due to missing local prerequisites

## Avoid

- changing secrets handling casually
- changing CI assumptions without stating it
- mixing cleanup with behavior changes
- silently changing developer workflow contracts