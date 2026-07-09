# SimpliPay mobile — user-flow enhancements

Remediation plan from a hands-on UX audit of the live iOS build (every major flow walked + screenshotted),
peer-reviewed by a second designer pass. This is the source of truth for the UX work; it turns findings into
sequenced, buildable tasks.

Legend — **Layer**: App (Kotlin/Compose), BE (payout connector / ledger), Prod (product decision).
**Effort**: S ≤0.5d · M 1–2d · L 3d+. Verification for every item = live-simulator walkthrough + screenshot
of the changed flow.

## Guiding principles
1. Never show the user internal/technical language (product codes, ledger phases, internal row ids).
2. Every actionable-looking control does something or is visibly disabled — no silent dead ends.
3. One concept, one place — collapse duplicated/contradictory flows.
4. One design system for money: a single currency formatter and a single amount-input component.
5. Money-out is deliberate and honest: a review step before funds leave, with fees and recipient shown.

---

## Findings summary (what the audit found)
- **Internal language leaks (highest impact, cheapest):** wallet shown as raw `SIMPLIWALLET-SAWL`;
  transaction rows have no description (just "DEBIT"/"CREDIT"); detail screen shows `Note: "payout COMMIT"`,
  internal `Transaction ID: 59`, an empty "Transferred To" section, and title "Specific Transactions History";
  Buy "Recent" shows `payout COMMIT`; Request tab uses Indian-UPI "Virtual Payment Address (VPA)" wording.
- **Money-out safety gaps (elevated by review):** there is **no confirm/review step before sending** (the Pay
  form commits straight to "Sent"); the locked "Account not activated" state shows a **fabricated R12,480.50
  balance + fake transaction**; no fee shown before/after sending; balance labelled "Available balance" when
  locked vs "Wallet balance" when active.
- **Dead ends:** Top-Up (EFT/Cash/Card) rows look tappable but no-op; Receive-sheet "coming soon" items are
  still tappable.
- **Fragmentation/contradiction:** two Receive experiences (Home sheet vs Payments→Request) that disagree on
  pay-link and mobile-number availability.
- **Consistency:** four currency formats; amount quick-chips on Buy but not Send; low-contrast/ambiguous
  primary buttons (pale sage reads like a CTA but signals disabled); horizontally-clipped tab bars.

Full screenshots + per-finding evidence: see the audit artifacts (UX_AUDIT.md) captured during review.

---

## Workstream 1 — Humanize money & transactions
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 1.1 | Wallet shows raw "SIMPLIWALLET-SAWL" | One mapper: product code → friendly name ("SimpliPay Wallet"), used by Home card + Finance row | App | S |
| 1.2 | Rows have no description | Enrich history from the connector payment record: counterparty + method (e.g. "Payout to Capitec ••0777 · Instant", "Airtime 082…", "Received via EFT") | App + BE | L |
| 1.3 | Detail leaks "payout COMMIT" | Connector writes human narratives on ledger legs; app maps residual internal notes to plain language | BE + App | M |
| 1.4 | Detail "Transferred To" empty | Populate recipient (name/bank/masked account or proxy) from the payment record | App + BE | M |
| 1.5 | Internal "Transaction ID: 59" | Relabel "Reference"; show client reference, not the Fineract row id | App + BE | S |
| 1.6 | Detail missing status/rail/fee | Add Status, Rail (Instant/Standard), Fee line to the receipt | App + BE | M |
| 1.7 | Title "Specific Transactions History" | Rename "Transaction details" / "Receipt" | App | S |
| 1.8 | Buy "Recent" shows payouts + jargon | Scope Buy-recent to VAS purchases (or relabel); reuse humanised row | App | S |

**Enabler (start early):** connector writes human narratives + exposes a payment-list endpoint
(counterparty/rail/status/fee) so the app has data to render. Without it, 1.2/1.4/1.6 are cosmetic only.

## Workstream 2 — Money-out safety & trust  (do first)
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 2.1 | **No confirm step before money leaves** (CRITICAL) | Confirm screen between Pay form and submit: recipient name + masked account/proxy, amount, rail, **fee**, "arrives in…", explicit Confirm | App | M |
| 2.2 | **Locked state shows fabricated balance/txns** | Remove fake preview (show real empty/R0) or clearly watermark "Preview"; never imply held funds | App | M |
| 2.3 | Balance label inconsistent (Available vs Wallet) | One balance term app-wide | App | S |
| 2.4 | No fee transparency | Show fee on confirm (2.1) + receipt (1.6); "free" as a stated value | App + BE | S |
| 2.5 | Success/failure trust detail | Receipt shows reference + "view in history"; keep reason-code + retry on failure | App | S |
| 2.6 | Weak security cue on money-out | Optional biometric/passcode re-auth above a config threshold | App | M |

## Workstream 3 — Eliminate dead ends & "coming soon" hygiene
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 3.1 | Top-Up rows no-op | Route each to a real screen; where BE not ready, a proper "coming soon" screen | App (+BE) | M |
| 3.2 | Coming-soon items look tappable | Disabled visual state (dimmed, non-tappable, badge) on Receive + Send sheets | App | S |
| 3.3 | AutoPay inside "Send money" sheet | Keep AutoPay as the Payments tab; de-emphasise in the send sheet | App | S |

## Workstream 4 — Unify & de-duplicate Receive
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 4.1 | Two receive experiences | One "Receive" screen (QR + mobile number + pay link) from both entry points | App | M |
| 4.2 | Pay-link & mobile-number contradictions | Single feature-availability source; render consistently | App | S |
| 4.3 | "VPA" jargon | Rename "Payment QR" / "SimpliPay ID"; purge UPI/VPA wording | App | S |

## Workstream 5 — Consistency system
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 5.1 | Four currency formats | One `ZarFormatter` (e.g. "R30.00") everywhere | App | M |
| 5.2 | Amount entry differs | Shared `AmountField` with quick-amount chips, used in Send + Buy | App | M |
| 5.3 | Row arrows ambiguous | Solved by the 1.2 row redesign — no standalone work | App | — |
| 5.4 | Casing/grammar nits | "Top-Up Wallet" consistency; title fixes | App | S |
| 5.5 | Button affordance/contrast | Explicit enabled/disabled/secondary button tokens; fix low-contrast rail card | App | M |

## Workstream 6 — Navigation / IA  (observations only — nav is a settled decision)
Bottom-nav layout and the Payments→History sub-tab are locked. Do NOT remove/replace nav slots. Keep the
"History appears in 3 places" observation only.
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 6.1 | Tab bars clip later tabs off-edge | Scroll/edge affordance or trim tab count within existing screens | App | S |
| 6.2 | Finance heavy for single-wallet user | OPEN product question (below) — no build until answered | Prod | — |

## Workstream 7 — Foundational states & accessibility
| # | Concern | Fix | Layer | Effort |
|---|---------|-----|-------|--------|
| 7.1 | Empty states | Friendly empty states (History/Finance/Receive) | App | S |
| 7.2 | Loading/error states | Consistent skeletons + retryable errors on network screens | App | M |
| 7.3 | Accessibility | Tap-targets, contrast, content descriptions, dynamic type | App | M |
| 7.4 | First-run / activation | Clear next step from the locked state | App | M |

---

## Phasing

**Phase 1 — Safety + perceived quality.** SAFETY FIRST: 2.1 confirm-before-send, 2.2/2.3 locked-state +
balance label, 2.4 fee transparency. Then WS1 core (1.1, 1.3, 1.4, 1.5, 1.7), 3.1/3.2 dead ends, 5.1 currency
formatter. → Money-out becomes deliberate and honest; the app stops leaking plumbing; no dead ends.

**Phase 2 — Coherence.** WS1 (1.2 rich history + BE enabler, 1.6 receipt, 1.8), WS4 unify Receive, 5.2 amount
field, 5.5 button/contrast tokens, 3.3, 7.1/7.2 states, 5.4 microcopy.

**Phase 3 — Polish.** 2.6 re-auth, 7.3/7.4 accessibility + first-run, 6.1 tab affordance, plus Finance-scope
work once the product questions are answered.

## Cross-cutting enablers (start in Phase 1)
- **Connector:** human transaction narratives on ledger legs + a payment-list endpoint (feeds WS1). Main BE
  dependency; without it, history/receipt humanisation is cosmetic-only.
- **App shared components:** `ZarFormatter`, `AmountField`, `TransactionRow`, `FeatureAvailability` source of
  truth, button state tokens.

## Product decisions needed (block WS6.2 / Finance scope)
1. Primary user: consumer wallet vs agent/merchant? (drives Finance depth)
2. Single-wallet or genuinely multi-account? (drives the Finance Accounts "+")

## How we verify (every phase)
Re-run the live-simulator walkthrough; screenshot each changed flow; check: no internal strings visible; every
tappable control acts or is disabled; one currency format; a confirm step exists before send; Receive is one
screen; locked state shows no fabricated funds. Ledger correctness stays covered by connector tests + recon.
