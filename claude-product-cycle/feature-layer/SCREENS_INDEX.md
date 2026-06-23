# Screens Index — Feature Layer

**Last Updated**: 2026-06-23
**Source**: `feature/` directory

---

## Summary

| Feature | Screens | ViewModels |
|---------|:-------:|:----------:|
| auth | - | - |
| home | 1 | 1 |
| accounts | - | - |
| history | - | - |
| receipt | - | - |
| faq | - | - |
| make-transfer | 0 | 1 |
| send-money | - | - |
| transfer-intrabank | - | - |
| transfer-interbank | - | - |
| notification | - | - |
| editpassword | - | - |
| kyc | - | - |
| savedcards | - | - |
| invoices | - | - |
| settings | - | - |
| profile | - | - |
| finance | - | - |
| merchants | - | - |
| beneficiary | - | - |
| standing-instruction | - | - |
| payments | - | - |
| upi-setup | - | - |
| qr | 1 | 1 |
| autopay | - | - |
| mpay-qr | - | - |
| mpay-qr-scan | - | - |
| fast-mpay | - | - |
| passcode | - | - |

*Note: `-` indicates screens exist but counts not yet enumerated. Run `/gap-analysis feature` to populate.*

---

## Known Screens

### home

| Screen | ViewModel | File |
|--------|-----------|------|
| HomeScreen | HomeViewModel | HomeScreen.kt |

### make-transfer (partial)

| Screen | ViewModel | File |
|--------|-----------|------|
| - | MakeTransferViewModel | MakeTransferViewModel.kt |

### qr (partial)

| Screen | ViewModel | File |
|--------|-----------|------|
| ScanQrCodeScreen | ScanQrViewModel | ScanQrCodeScreen.kt |

---

## Path Pattern

```
feature/[name]/src/commonMain/kotlin/org/mifospay/feature/[name]/
├── [Feature]Screen.kt          # Main composable screen
└── [Feature]ViewModel.kt       # MVI ViewModel
```

*Navigation is always at `navigation/[Feature]Navigation.kt`*
*DI is always at `di/[Feature]Module.kt`*

*Note: Some modules place files in subdirectories rather than flat in the root package:*
- *`upi-setup` — ViewModels in `viewmodel/`, Screens in `screens/`*
- *`merchants` — Screens in `ui/`*
- *`transfer-interbank` — Screens in `screens/`*
- *`beneficiary` — Files in sub-packages (`list/`, `addupdatebeneficiary/`, `deletebeneficiary/`)*

*Always verify the actual directory layout before reading or writing files in a module.*
