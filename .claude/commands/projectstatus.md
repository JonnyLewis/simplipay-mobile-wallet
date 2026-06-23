# /projectstatus - Project Overview

## Purpose
Display the current state of the MifosX Mobile Wallet project, including feature implementation status, available commands, and suggested next steps.

---

## Workflow

```
┌───────────────────────────────────────────────────────────────────┐
│                    /projectstatus WORKFLOW                         │
├───────────────────────────────────────────────────────────────────┤
│                                                                    │
│  STEP 1: READ STATUS FILES                                        │
│  ├─→ claude-product-cycle/design-spec-layer/STATUS.md             │
│  └─→ Individual feature STATUS.md files                           │
│                                                                    │
│  STEP 2: ANALYZE CODEBASE                                         │
│  ├─→ Check feature/ directory for implemented features            │
│  ├─→ Check core/network/services/ for API services                │
│  ├─→ Check core/data/repository/ for repositories                 │
│  └─→ Compare spec vs implementation                               │
│                                                                    │
│  STEP 3: GENERATE DASHBOARD                                       │
│  ├─→ Feature status table                                         │
│  ├─→ Layer completion summary                                     │
│  ├─→ Available commands                                           │
│  └─→ Suggested next steps                                         │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

---

## Output Template

```
╔══════════════════════════════════════════════════════════════════════╗
║  MIFOSPAY MOBILE WALLET - PROJECT STATUS                             ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                       ║
║  PROJECT: MifosX Mobile Wallet (Payments & Wallet App)               ║
║  TECH STACK: Kotlin Multiplatform + Compose + Fineract API           ║
║  LAST UPDATED: [Date]                                                ║
║                                                                       ║
╠══════════════════════════════════════════════════════════════════════╣
║  FEATURE STATUS                                                       ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                       ║
║  | Feature              | Status     | Client | Feature | Gaps |     ║
║  |----------------------|------------|--------|---------|------|     ║
║  | Auth                 | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Home                 | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Accounts             | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | History              | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Receipt              | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | FAQ                  | ✅ Done    | -      | ✅      | 0    |     ║
║  | Make Transfer        | ✅ Done    | ✅     | ⚠️      | 1    |     ║
║  | Send Money           | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Transfer (Intrabank) | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Transfer (Interbank) | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Notification         | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Edit Password        | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | KYC                  | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Saved Cards          | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Invoices             | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Settings             | ✅ Done    | -      | ✅      | 0    |     ║
║  | Profile              | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Finance              | ✅ Done    | -      | ✅      | 0    |     ║
║  | Merchants            | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Beneficiary          | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Standing Instruction | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Payments             | ✅ Done    | -      | ✅      | 0    |     ║
║  | UPI Setup            | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | QR                   | ✅ Done    | -      | ⚠️      | 1    |     ║
║  | Autopay              | ✅ Done    | ✅     | ✅      | 0    |     ║
║  | Mpay QR              | ✅ Done    | -      | ✅      | 0    |     ║
║  | Mpay QR Scan         | ✅ Done    | -      | ✅      | 0    |     ║
║  | Fast Mpay            | ✅ Done    | -      | ✅      | 0    |     ║
║  | Passcode             | ✅ Done    | -      | ✅      | 0    |     ║
║                                                                       ║
╠══════════════════════════════════════════════════════════════════════╣
║  AVAILABLE COMMANDS                                                   ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                       ║
║  Design:                                                              ║
║    /design [Feature]      → Create/update feature specification      ║
║                                                                       ║
║  Implement:                                                           ║
║    /implement [Feature]   → Full E2E implementation                  ║
║    /client [Feature]      → Network + Data layers                    ║
║    /feature [Feature]     → UI layer (ViewModel + Screen)            ║
║                                                                       ║
║  Verify:                                                              ║
║    /verify [Feature]      → Validate implementation vs spec          ║
║                                                                       ║
╠══════════════════════════════════════════════════════════════════════╣
║  SUGGESTED NEXT STEPS                                                 ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                       ║
║  1. Review existing features: /verify [Feature]                      ║
║  2. Improve feature: /design [Feature] for enhancements              ║
║  3. Add new feature: /design [NewFeature]                            ║
║                                                                       ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

## Key Files to Read

1. `claude-product-cycle/design-spec-layer/STATUS.md` - Master status tracker
2. `feature/*/` - Feature module directories
3. `core/network/src/commonMain/kotlin/org/mifospay/core/network/services/` - API services (23 services)
4. `core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/` - Repositories (29 repositories)

---

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Done | Feature complete, all working |
| ⚠️ Needs Update | Has gaps, spec changed, or incomplete |
| 🔄 In Progress | Currently being implemented |
| 📋 Planned | Spec exists, not started |
| 🆕 Not Started | No work done |

---

## Notes on Partial Features

Two feature modules are partially implemented and warrant attention:

- **`make-transfer`** (`feature/make-transfer/`): Only contains `MakeTransferViewModel.kt` — no Screen or Navigation. Acts as an orchestration ViewModel; the UI layer may live in other transfer-related features.
- **`qr`** (`feature/qr/`): Only contains `ScanQrCodeScreen.kt` and `ScanQrViewModel.kt` — no Navigation or DI module. The full QR flow is implemented in `mpay-qr` and `mpay-qr-scan`.
