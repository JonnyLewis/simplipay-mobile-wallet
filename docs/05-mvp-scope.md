# MVP Scope — SimpliPay Mobile Wallet

**Status:** draft — module classification below is `observed` from the build files (2026-06-12, import-codebase) and awaits founder confirmation. Until confirmed, treat *Active* rows as working scope and *Flagged* rows as frozen.

This document is the authoritative scope fence. When any other document conflicts with it on scope, this one wins.

## Scope rule

Work proceeds only on **Active** modules. **Flagged (inherited)** modules are Mifos/India-inherited capabilities with no confirmed SimpliPay role — extending them requires a founder decision recorded here first.

## Feature module classification (observed)

| Module | Classification | Note |
|---|---|---|
| `feature:auth` | Active | |
| `feature:passcode` | Active | |
| `feature:home` | Active | |
| `feature:accounts` | Active | |
| `feature:finance` | Active | |
| `feature:payments` | Active | |
| `feature:transfer-intrabank` | Active | |
| `feature:transfer-interbank` | Active | |
| `feature:beneficiary` | Active | |
| `feature:history` | Active | |
| `feature:receipt` | Active | |
| `feature:notification` | Active | |
| `feature:profile` | Active | |
| `feature:settings` | Active | |
| `feature:editpassword` | Active | |
| `feature:kyc` | Active | |
| `feature:savedcards` | Active | |
| `feature:invoices` | Active | |
| `feature:merchants` | Active | |
| `feature:faq` | Active | |
| `feature:upi-setup` | **Flagged (inherited)** | UPI is India-specific — OQ-1 |
| `feature:mpay-qr` | **Flagged (inherited)** | OQ-2 |
| `feature:mpay-qr-scan` | **Flagged (inherited)** | OQ-2 |
| `feature:fast-mpay` | **Flagged (inherited)** | OQ-2 |
| `feature:standing-instruction` | **Flagged (inherited)** | OQ-3 |

## Open questions

| # | Question | Blocking? |
|---|---|---|
| OQ-1 | Is `upi-setup` in SimpliPay scope at all, or removable? | no — frozen until decided |
| OQ-2 | Do the `mpay-qr*` / `fast-mpay` modules become SimpliPay's QR payment path (see `docs/QR_PAYMENT_ROUTING_APPROACH.md`) or are they replaced? | no — frozen until decided |
| OQ-3 | Are standing instructions a SimpliPay capability? | no — frozen until decided |
| OQ-4 | Confirm the Active list above actually matches SimpliPay's MVP — classification is observed from build files, not from a product decision. | yes, for scope-fence authority |

## Permanently out of scope

- Backend/server behavior of any kind — Fineract is authoritative (ED-1).
- *(Add product-level exclusions when OQ-4 is resolved.)*
