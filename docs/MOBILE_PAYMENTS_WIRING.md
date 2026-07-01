# SimpliPay Payments API — mobile integration

Wires the app to the **SimpliPay Payments API** (funding, on-us payments, EFT / PayShap payouts,
status tracking). Contract: `ph-ee-connector/docs/MOBILE_APP_INTEGRATION_PROMPT.md` +
`openapi.yaml`. This doc covers what landed and the remaining per-screen wiring.

## What landed (core, done + tested)

| Layer | File |
|---|---|
| Config (base URL + token, build-config) | `core/network/.../config/PaymentsApiConfig.kt` |
| DTOs (transfer / status / payin / bankAccount) | `core/network/.../model/payments/PaymentsModels.kt` |
| ZAR money type (minor units ↔ `"50.00"`) | `core/network/.../model/payments/ZarAmount.kt` |
| Request factory (payee mapping, bankAccount JSON-string, PayShap cap) | `core/network/.../model/payments/PaymentRequestFactory.kt` |
| Ktorfit service | `core/network/.../services/PaymentsService.kt` |
| API manager + `PaymentsClient` DI + qualifier | `core/network/.../PaymentsApiManager.kt`, `di/NetworkModule.kt`, `di/Qualifier.kt` |
| Repository (initiate + poll) | `core/data/.../repository/PaymentsRepository.kt` (+ `repositoryImpl/…Impl.kt`) |
| Unit tests | `core/network/src/commonTest/.../payments/*Test.kt` |
| Representative screen wired | `feature/send-money/.../PaymentProcessingViewModel.kt` |

### The Payments API is a separate service
It is **not** the per-instance Fineract config. It has its own Ktorfit client (`PaymentsClient`
qualifier) with a fixed base URL from `PaymentsApiConfig`, the `Platform-TenantId` header, and no
Fineract Basic auth. `GET /payments` and `POST /payin` take a shared `?token=` query.

## Repository API

```kotlin
paymentsRepository.payOnUs(payerAccountId, target, amount, clientRefId)      // phone / account / QR / proximity
paymentsRepository.payoutToBank(payerAccountId, bankAccount, amount, clientRefId) // EFT
paymentsRepository.payoutInstant(payerAccountId, phoneMsisdn, amount, clientRefId) // PayShap (< R50k)
paymentsRepository.getPaymentStatus(paymentId)                               // poll to terminal
```

- `payerAccountId` = `UserPreferencesRepository.defaultAccountId` (Fineract savings account id), as a string.
- `target` = `PaymentTarget.Phone(msisdn)` or `PaymentTarget.Account(accountId)`.
- `amount` = `ZarAmount.fromMinorUnits(paise)` or `ZarAmount.fromRands("50.00")`.
- `clientRefId` = **idempotency key**: generate once with `Uuid.random()` when the user *confirms*,
  and **reuse it on every retry** of that payment. Never regenerate on resend.

`payOnUs` / `payoutInstant` / `payoutToBank` return `Flow<DataState<TransferResponse>>` — the
`transactionId` is *accepted, not settled*. Poll `getPaymentStatus` until
`PaymentState.isTerminal(state)` (`SUCCESS` / `FAILED` / `EXPIRED`). See
`PaymentProcessingViewModel.pollUntilTerminal` for the reference loop.

## Server rules the UI must respect
- **On-us first:** paying a phone that is a SimpliPay customer is a free internal transfer; the same
  number to a non-customer goes out as PayShap. The **server decides** — the app just sends the phone.
- **PayShap cap:** proxy/phone payouts must be **< R50,000**. `payoutInstant` throws
  `PayShapCapExceededException` at/above the cap (surfaced as `DataState.Error`); route those to
  `payoutToBank` (EFT). Mirror this in the UI; the server is the backstop.
- **EFT `bankAccount`** is serialized to a **JSON string** in `customData.value` (not nested) — the
  factory + `PaymentRequestFactoryTest.payoutToBank_encodesBankAccountAsJsonString` cover this.

## Remaining wiring (next stage — needs product/routing decisions)

The core + one representative flow (send-money → PaymentProcessing) are wired. Each screen below
now just needs to call the repository with the right `PaymentTarget` / method:

1. **Send/Pay by phone** (`feature/send-money`, `feature/payments`): pass the phone as
   `PAYMENT_PROCESSING_PAYEE_IDENTIFIER_ARG` with `payeeType = MSISDN` from PayeeDetails/UpiPin.
   `PaymentProcessingViewModel` already consumes these nav args and does initiate + poll.
2. **Pay by account / QR / proximity** (`feature/mpay-qr-scan`, `feature/proximity`): decode the
   account id in-app, pass it as the identifier with `payeeType = ACCOUNT_ID`.
3. **Withdraw → bank (EFT)** (`feature/transfer-interbank`): collect `BankAccount`
   (holder / bank / universalBranchCode / accountNumber / accountType ∈ CHEQUE|SAVINGS|TRANSMISSION)
   → `payoutToBank`.
4. **Withdraw → instant (PayShap)**: phone + amount < R50k → `payoutInstant`; enforce the cap in UI.
5. **Receive money / top-up:** `POST /payin` is **not app-initiated** in v1 (client method exists on
   `PaymentsService` but no screen). The receive-money flow shares pay-in info; the backend settles.

## Before shipping (secrets)
- `PaymentsApiConfig.SHARED_TOKEN` must be **blank in VCS** and injected at build time (gitignored
  properties / CI secret), same as `ServiceAccountConfig`. Never commit a real token.
- `PaymentsApiConfig.NON_PROD_BASE_URL` currently equals prod; point it at staging when it exists.

## Open product questions
- Fee / limit **copy** to display (backend charges are config-driven; the app must not invent amounts).
- Confirmation of the non-prod Payments API host.
- Whether `payerAccountId` sent to the Payments API should be the Fineract savings id (current
  assumption) or an external/wallet id, per environment.
