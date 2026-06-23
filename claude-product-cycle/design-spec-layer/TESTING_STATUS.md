# Testing Status — Design Spec Layer

**Last Updated**: 2026-06-23

---

## Feature Testing Matrix

| # | Feature | VM Tests | UI Tests | Fake Repo | Status |
|:-:|---------|:--------:|:--------:|:---------:|--------|
| 1 | auth | 0 | 0 | ❌ | Not Started |
| 2 | home | 0 | 0 | ❌ | Not Started |
| 3 | accounts | 0 | 0 | ❌ | Not Started |
| 4 | history | 0 | 0 | ❌ | Not Started |
| 5 | receipt | 0 | 0 | ❌ | Not Started |
| 6 | faq | 0 | 0 | ❌ | Not Started |
| 7 | make-transfer | 0 | 0 | ❌ | Not Started |
| 8 | send-money | 0 | 0 | ❌ | Not Started |
| 9 | transfer-intrabank | 0 | 0 | ❌ | Not Started |
| 10 | transfer-interbank | 0 | 0 | ❌ | Not Started |
| 11 | notification | 0 | 0 | ❌ | Not Started |
| 12 | editpassword | 0 | 0 | ❌ | Not Started |
| 13 | kyc | 0 | 0 | ❌ | Not Started |
| 14 | savedcards | 0 | 0 | ❌ | Not Started |
| 15 | invoices | 0 | 0 | ❌ | Not Started |
| 16 | settings | 0 | 0 | ❌ | Not Started |
| 17 | profile | 0 | 0 | ❌ | Not Started |
| 18 | finance | 0 | 0 | ❌ | Not Started |
| 19 | merchants | 0 | 0 | ❌ | Not Started |
| 20 | beneficiary | 0 | 0 | ❌ | Not Started |
| 21 | standing-instruction | 0 | 0 | ❌ | Not Started |
| 22 | payments | 0 | 0 | ❌ | Not Started |
| 23 | upi-setup | 0 | 0 | ❌ | Not Started |
| 24 | qr | 0 | 0 | ❌ | Not Started |
| 25 | autopay | 0 | 0 | ❌ | Not Started |
| 26 | mpay-qr | 0 | 0 | ❌ | Not Started |
| 27 | mpay-qr-scan | 0 | 0 | ❌ | Not Started |
| 28 | fast-mpay | 0 | 0 | ❌ | Not Started |
| 29 | passcode | 0 | 0 | ❌ | Not Started |

---

## Testing Priority

| Priority | Features | Reason |
|:--------:|----------|--------|
| P0 | auth, transfer-intrabank | Core user flows |
| P1 | home, payments, send-money | High-traffic screens |
| P2 | All others | Complete coverage |

---

## Test Patterns

| Type | Framework | Location |
|------|-----------|----------|
| Unit (ViewModel) | kotlin.test + Turbine | `feature/[name]/src/commonTest/` |
| Fake Repos | Manual implementation | Feature's own `commonTest/` (e.g. `feature/fast-mpay/src/commonTest/.../TestFakes.kt`) |

> Note: There is no `core/testing` module in this repo — fakes live in each feature's `commonTest` source set. Reference fake: `feature/fast-mpay/src/commonTest/kotlin/org/mifospay/feature/fastmpay/TestFakes.kt`. Compose UI/instrumented tests are not yet wired (no `androidInstrumentedTest` source sets exist).

---

**Next**: Run `/gap-planning testing feature` to create a testing plan.
