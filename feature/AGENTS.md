# AGENTS.md — `/feature`

Per-folder context for the ~28 product feature modules. Read after the root [AGENTS.md](../AGENTS.md). *(The pre-import `CLAUDE.md` here was empty; this file was generated from the build files at import, 2026-06-12 — refine it from real code via `update-context`.)*

## What exists (observed from `settings.gradle.kts`)

| Group | Modules |
|---|---|
| Money movement | `payments` · `transfer-intrabank` · `transfer-interbank` · `beneficiary` · `standing-instruction`* |
| Accounts & money view | `home` · `accounts` · `finance` · `history` · `receipt` · `invoices` · `merchants` · `savedcards` |
| Identity & security | `auth` · `passcode` · `editpassword` · `kyc` |
| QR / device payments | `mpay-qr`* · `mpay-qr-scan`* · `fast-mpay`* · `upi-setup`* |
| Supporting | `notification` · `profile` · `settings` · `faq` |

\* **Flagged (inherited)** — frozen pending the open questions in [`docs/05-mvp-scope.md`](../docs/05-mvp-scope.md). Do not extend without a founder decision.

## Invariants and gotchas

- Feature modules consume `core/*` and `core-base/*` foundations; cross-feature imports are a blast-radius smell — flag them (ED-2).
- Most modules ship to **four surfaces** (Android, iOS, Desktop, Web) via `cmp-shared` assembly: any shared-state or contract change here needs the multi-surface impact statement from `risk/AGENTS.md`.
- Money-movement modules are high-risk by definition — research-first (ED-4), impact call-outs mandatory.

## Do NOT recreate

`core:designsystem` / `core:ui` components, `core:network` clients, and `core:datastore` persistence — features compose these, they do not own their own variants.
