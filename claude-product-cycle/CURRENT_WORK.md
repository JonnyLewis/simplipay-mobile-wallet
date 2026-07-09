# Current Work

**Last Updated**: 2026-06-23
**Session**: SimpliPay "Platinum Ivory" redesign
**Branch**: `feature/simplipay-platinum-ivory-redesign`

---

## Active Task

Implementing the SimpliPay Platinum Ivory + Jade redesign from the Claude Design handoff.
Master spec: `claude-product-cycle/design-spec-layer/SIMPLIPAY_REDESIGN.md`.

Scope locked: re-skin active screens + build **Buy/VAS** (new); adopt design bottom nav
(Home·Buy·Scan FAB·History·Profile); KYC deferred; light-only first.

---

## In-Progress Features

| Feature | Layer | Status | Next Step |
|---------|-------|--------|-----------|
| Redesign — theme foundation | designsystem | ✅ Done (compiles) | — |
| Redesign — Rosette + ParticleField | designsystem | ✅ Done (compiles) | In use on splash/landing |
| Redesign — WalletWordmark | designsystem | ✅ Done (compiles) | shared `simplipay.` mark |
| Redesign — onboarding (splash/landing) | cmp-shared | ✅ Done (compiles) | Splash→resolved dest; Landing = LOGIN_GRAPH start |
| Redesign — login | feature/auth | ✅ Done (compiles) | Wordmark + ivory floating-label fields + jade glow button; Face ID omitted (no biometric action at login) |
| Redesign — passcode | feature/passcode | ✅ Done (compiles) | Re-themed via configs: jade dots (14dp) + border inactive, ivory rounded keys (mono digits, `shouldShuffleKeys=false`), jade switch/forgot/dialog. Avatar+greeting header NOT done — library exposes only logo painter + header text, no custom header slot |
| Redesign — home | feature/home | ✅ Done (compiles) | Ivory balance card (mono balance + 2 corner rosettes + savings chip), action tiles (Send/Request/Autopay — Buy/Scan deferred to nav task), ivory promo card, bordered activity card. Per-row mono amounts deferred to History (shared `TransactionItem`) |
| Redesign — history | feature/history | ✅ Done (compiles) | In/Out summary cards (mono), All/In/Out filter chips (wired to SetTransactionType+ApplyFilter), themed header; mono amounts + credit/debit colors in all 3 txn renderers (core.ui ×2 + history list) — also upgrades Home rows |
| Redesign — profile | feature/profile | ✅ Done (compiles) | Avatar + name + gold VERIFIED badge (maps to `client.active`); grouped detail rows (mono labels + mono numeric values). Logout/settings rows NOT here — they live in `feature/settings`, not wired into profile VM |
| Redesign — Buy/VAS + bottom nav | feature | ⏭ Next | New feature + nav (Home·Buy·Scan FAB·History·Profile) |

---

## Recent Completions

| Item | Completed | Notes |
|------|-----------|-------|
| Theme foundation | 2026-06-23 | Plus Jakarta Sans + Geist Mono fonts; ivory+jade `Color.kt`; `Type.kt`; `SimpliPayTokens` |
| Login re-skin | 2026-06-24 | `LoginScreen.kt` rebuilt: 26sp wordmark, "Welcome back", ivory 15px floating-label fields (person/lock icons + eye toggle), jade glow button, jade signup link; mono endpoint in debug. All `LoginAction`s + multi-tap instance selector preserved |
| Passcode re-theme | 2026-06-24 | `MifosPasscode.kt` configs → jade dots, ivory rounded mono-digit keys, no shuffle, jade switch/forgot/dialog. Header avatar+greeting blocked by library config API |
| Home re-skin | 2026-06-24 | `HomeScreen.kt`: ivory balance card w/ 2 rosettes + mono balance + savings chip; jade action tiles; ivory promo; bordered activity card. New `ActionTile`/`SavingsChip` helpers; `CardDropdownBox` got `tint` param |
| History re-skin | 2026-06-24 | `HistoryScreen.kt`: In/Out summary cards, filter chips, themed header. Mono amounts + credit(`#0E9466`)/debit(`#4A5560`) colors across `core.ui` `TransactionItem`/`TransactionItemCard` + history `TransactionList` (Home rows benefit too) |
| Profile re-skin | 2026-06-24 | `ProfileScreen.kt` + `components/ProfileCard.kt`: avatar header, name, gold VERIFIED badge, grouped mono detail rows. `ProfileItem` API changed (dropped labelColor/textColor; added valueFontFamily/showDivider) |
| Signature components | 2026-06-23 | `Rosette` + `ParticleField` exact ports, compile clean |
| Claude Code setup port | 2026-06-23 | Commands + indexes bootstrapped |

---

## Verification

2026-06-24 — Rendered all re-skinned screens headless via `:cmp-desktop:renderShots`
(added public `*RenderPreview()` entrypoints per feature + `cmp-shared/.../RedesignRenderPreview.kt`
wrappers + 4 new shots in `RenderShots.kt`). Visually confirmed: **login, home, history, profile**
all match the Platinum Ivory spec (wordmark, ivory cards + rosette, mono money, jade accents,
gold VERIFIED badge). Fixed history filter chips → short **All/In/Out** labels (were wrapping).

## Blockers

None.

---

## Notes

This file is updated by `/session-end` and read by `/session-start`.
