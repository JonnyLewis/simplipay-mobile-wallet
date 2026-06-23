# Modules Index — Feature Layer

**Last Updated**: 2026-06-23
**Source**: `feature/` directory

---

## Module Summary

| Total | With DI | With Nav | Partial |
|:-----:|:-------:|:--------:|:-------:|
| 29 | 27 | 27 | 2 |

---

## Module Index

| # | Module | Path | DI | Nav | VMs | Screens | Notes |
|:-:|--------|------|----|-----|:---:|:-------:|-------|
| 1 | auth | feature/auth | ✅ | ✅ | - | - | |
| 2 | home | feature/home | ✅ | ✅ | 1 | 1 | |
| 3 | accounts | feature/accounts | ✅ | ✅ | - | - | |
| 4 | history | feature/history | ✅ | ✅ | - | - | |
| 5 | receipt | feature/receipt | ✅ | ✅ | - | - | |
| 6 | faq | feature/faq | ✅ | ✅ | - | - | |
| 7 | make-transfer | feature/make-transfer | ❌ | ❌ | 1 | 0 | ViewModel only |
| 8 | send-money | feature/send-money | ✅ | ✅ | - | - | |
| 9 | transfer-intrabank | feature/transfer-intrabank | ✅ | ✅ | - | - | |
| 10 | transfer-interbank | feature/transfer-interbank | ✅ | ✅ | - | - | |
| 11 | notification | feature/notification | ✅ | ✅ | - | - | |
| 12 | editpassword | feature/editpassword | ✅ | ✅ | - | - | |
| 13 | kyc | feature/kyc | ✅ | ✅ | - | - | |
| 14 | savedcards | feature/savedcards | ✅ | ✅ | - | - | |
| 15 | invoices | feature/invoices | ✅ | ✅ | - | - | |
| 16 | settings | feature/settings | ✅ | ✅ | - | - | |
| 17 | profile | feature/profile | ✅ | ✅ | - | - | |
| 18 | finance | feature/finance | ✅ | ✅ | - | - | |
| 19 | merchants | feature/merchants | ✅ | ✅ | - | - | |
| 20 | beneficiary | feature/beneficiary | ✅ | ✅ | - | - | |
| 21 | standing-instruction | feature/standing-instruction | ✅ | ✅ | - | - | |
| 22 | payments | feature/payments | ✅ | ✅ | - | - | |
| 23 | upi-setup | feature/upi-setup | ✅ | ✅ | - | - | |
| 24 | qr | feature/qr | ❌ | ❌ | 1 | 1 | Screen+VM only |
| 25 | autopay | feature/autopay | ✅ | ✅ | - | - | |
| 26 | mpay-qr | feature/mpay-qr | ✅ | ✅ | - | - | |
| 27 | mpay-qr-scan | feature/mpay-qr-scan | ✅ | ✅ | - | - | |
| 28 | fast-mpay | feature/fast-mpay | ✅ | ✅ | - | - | |
| 29 | passcode | feature/passcode | ✅ | ✅ | - | - | |

---

## Partial Modules

| Module | Issue | Files Present | Missing |
|--------|-------|---------------|---------|
| make-transfer | Orchestration ViewModel only | MakeTransferViewModel.kt | Screen, Navigation, DI |
| qr | Screen+VM without wiring | ScanQrCodeScreen.kt, ScanQrViewModel.kt | Navigation, DI |

---

## Path Pattern

```
feature/[name]/src/commonMain/kotlin/org/mifospay/feature/[name]/
├── [Feature]Screen.kt
├── [Feature]ViewModel.kt
├── navigation/
│   └── [Feature]Navigation.kt
└── di/
    └── [Feature]Module.kt
```

*Note: This is the canonical pattern (used by home, editpassword, autopay, etc.). Some modules deviate:*
- *`upi-setup` — ViewModels in `viewmodel/`, Screens in `screens/`; package `org.mifospay.feature.upi.setup`*
- *`merchants` — Screens in `ui/`*
- *`transfer-interbank` — Screens in `screens/`; package `org.mifospay.feature.transfer.interbank`*
- *`beneficiary` — Sub-packages (`list/`, `addupdatebeneficiary/`, `deletebeneficiary/`)*
- *`send-money` — Package `org.mifospay.feature.send.money` (dot-separated, not hyphenated)*

*Always check the actual module structure before referencing file paths.*
