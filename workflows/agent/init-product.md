# init-product workflow

Bootstrap a brand-new product into the `codebot` kit. Run this once at the start of a copied product repo.

After this workflow, the kit stops being generic. Foundation files should no longer contain unresolved product placeholders.

## Pre-flight

1. Confirm the repo is still a fresh kit by checking whether `AGENTS.md` contains `{{PRODUCT_NAME}}`.
2. If it does not, the product is already bootstrapped. Offer `start-session` instead and do not overwrite populated files without explicit confirmation.
3. Read `README.md`, `GETTING-STARTED.md`, and this file.

## Interview

Ask in small batches. Aim for 6-10 answers across 2-3 rounds.

### Round 1 - idea

- What is the product, in one or two sentences?
- What problem does it solve, and for whom?
- What is it deliberately not?

The negative-scope boundary is mandatory. If it is weak, ask one focused follow-up.

### Round 2 - scope and shape

- What does MVP 1 deliver? List 4-8 core capabilities.
- What is explicitly out of MVP, future, or never?
- Who are the 3-6 user types and their core need?

### Round 3 - technical and constraints

- Technical stack: backend, frontend, database, infra. "Undecided" is valid and becomes an open question.
- Hard constraints: regulatory, data residency, deadline, budget, launch customer.
- Who approves feature briefs and plans?
- Absolute repo path. Use the working directory if available.
- Kit weight: Light, Standard, or Full. Scale by deletion, not half-filled files.

## Confirm before writing

Summarise back in a compact table:

| Topic | Captured value |
|---|---|
| Product name | |
| Tagline | |
| MVP capabilities | |
| Out of scope | |
| Users | |
| Stack | |
| Constraints | |
| Kit weight | |

Ask: "Have I captured this correctly? Confirm and I will write the foundation files."

Do not write until the user confirms.

## Foundation files to populate

Use targeted edits where possible. Rewrite a file wholesale only when most of it changes.

1. `AGENTS.md` - shared product operating manual: product name, thesis, target users, MVP scope, design rules, constraints, workflow map.
2. `CLAUDE.md` - keep as a thin Claude adapter importing `@AGENTS.md`; do not expand it into a duplicate manual.
3. `README.md` - product-facing readme replacing the kit readme.
4. `docs/01-product-vision.md` - vision spec from the interview.
5. `docs/05-mvp-scope.md` - authoritative MVP scope fence: numbered capabilities, classification table, exclusion list.
6. `DEV_PLAN.md` - first-pass milestone breakdown: M0 skeleton plus 2-4 plausible milestones.
7. `engineering/AGENTS.md` - bootstrap-time engineering context: stack, repo paths, starter ED decisions. Leave design-time placeholders that must be decided later.
8. `.claude/settings.json` - ensure the safe-tool allowlist is present per `workflows/agent/setup-permissions.md` (merge, never clobber).
9. `SESSION_STATE.md` - seed current state, status table, next concrete action, locked decisions, open questions, and bootstrap message.

Leave later-stage template files (`data-model/*`, `api/*`, `risk/*`, and similar) as templates unless the user explicitly designed that area.

## Sweep

Run:

```bash
grep -rln "{{" . --include="*.md" | grep -v "_TEMPLATE"
```

Any foundation file listed above that still contains unresolved product placeholders was missed. Template files intentionally left for later are fine.

## Report

Show:

- Files populated and one-line summary of each.
- MVP scope fence: in and out.
- The next concrete action from `SESSION_STATE.md` section 4.
- Reminder: use `build-ft` for the first feature and `stop-session` before closing.

## Rules

- Interview; do not assume.
- "Undecided" becomes an open question, never a silent guess.
- Negative scope is mandatory.
- No fabricated data model or API before the user designs it.
- Confirm before writing.
- Do not duplicate shared rules into `CLAUDE.md`.
