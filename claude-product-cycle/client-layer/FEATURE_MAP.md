# Feature Map — Client Layer

**Last Updated**: 2026-06-23

---

## Summary

| Services | Repositories | Managers |
|:--------:|:------------:|:--------:|
| 23 | 29 | 4 (+ KtorfitClient) |

Managers: `SelfServiceApiManager`, `FineractApiManager`, `InterBankApiManager`, `SupabaseApiManager`. All wrap the shared `KtorfitClient` (except `SupabaseApiManager`, which wraps `SupabaseConfigClient`).

---

## Feature → Service → Repository Map

Repository column reflects the repository interfaces each feature module actually imports (verified against `feature/<name>/src`). `-` means the feature imports no `core/data` repository directly.

| Feature | Service | API Manager | Repository |
|---------|---------|-------------|------------|
| auth | AuthenticationService | Self | UserRepository, ClientRepository |
| home | SavingsAccountsService | Self | SelfServiceRepository |
| accounts | SavingsAccountsService | Self | SavingsAccountRepository |
| history | RunReportService | Fineract | AccountRepository |
| receipt | - | - | - |
| faq | - | - | - |
| make-transfer | ThirdPartyTransferService | Self | AccountRepository |
| send-money | ThirdPartyTransferService | Self | AccountRepository |
| transfer-intrabank | AccountTransfersService | Self | ThirdPartyTransferRepository |
| transfer-interbank | InterBankService | InterBank | InterBankRepository |
| notification | NotificationService | Fineract | NotificationRepository |
| editpassword | UserService | Self | UserRepository |
| kyc | KYCLevel1Service | Fineract | KycLevelRepository, DocumentRepository |
| savedcards | SavedCardService | Fineract | SavedCardRepository |
| invoices | InvoiceService | Fineract | InvoiceRepository |
| settings | AppConfigService | Supabase | SavingsAccountRepository, UserVerificationRepository |
| profile | ClientService, UserService | Self | ClientRepository |
| finance | - | - | - |
| merchants | BillerService | Fineract | - |
| beneficiary | BeneficiaryService | Self | SelfServiceRepository |
| standing-instruction | StandingInstructionService | Fineract | StandingInstructionRepository |
| payments | BillService, BillerService | Fineract | - |
| upi-setup | UserService | Self | - |
| qr | - | - | - |
| autopay | AutoPayService | Fineract | AutoPayRepository, AutoPayHistoryRepository |
| mpay-qr | - | - | AccountRepository |
| mpay-qr-scan | SearchService | Fineract | - |
| fast-mpay | - | - | BeneficiaryRepository |
| passcode | - | - | AppLockRepository |

> Note: `BeneficiaryRepository`/`BeneficiaryRepositoryImpl` exist and are the canonical pattern reference, but the `beneficiary` feature consumes beneficiary operations via `SelfServiceRepository`; `BeneficiaryRepository` is imported by `fast-mpay`.

---

## SelfServiceApiManager Properties (9)

| Property | Service |
|----------|---------|
| `authenticationApi` | AuthenticationService |
| `clientsApi` | ClientService |
| `savingAccountsListApi` | SavingsAccountsService |
| `registrationAPi` | RegistrationService |
| `beneficiaryApi` | BeneficiaryService |
| `thirdPartyTransferApi` | ThirdPartyTransferService |
| `accountTransfersApi` | AccountTransfersService |
| `officeApi` | OfficeService |
| `userApi` | UserService |

## FineractApiManager Properties (19)

| Property | Service |
|----------|---------|
| `authenticationApi` | AuthenticationService |
| `clientsApi` | ClientService |
| `registrationAPi` | RegistrationService |
| `searchApi` | SearchService |
| `documentApi` | DocumentService |
| `runReportApi` | RunReportService |
| `twoFactorAuthApi` | TwoFactorAuthService |
| `accountTransfersApi` | AccountTransfersService |
| `savedCardApi` | SavedCardService |
| `kycLevel1Api` | KYCLevel1Service |
| `invoiceApi` | InvoiceService |
| `userApi` | UserService |
| `thirdPartyTransferApi` | ThirdPartyTransferService |
| `notificationApi` | NotificationService |
| `savingsAccountsApi` | SavingsAccountsService |
| `standingInstructionApi` | StandingInstructionService |
| `autoPayApi` | AutoPayService |
| `billerApi` | BillerService |
| `billApi` | BillService |

## InterBankApiManager Properties (1)

| Property | Service |
|----------|---------|
| `interBankApi` | InterBankService |

## SupabaseApiManager Properties (1)

| Property | Service |
|----------|---------|
| `appConfigService` | AppConfigService (constructed from `SupabaseConfigClient.postgrest`) |

---

## DI Module Locations

| Module | File |
|--------|------|
| Repository DI | `core/data/src/commonMain/kotlin/org/mifospay/core/data/di/RepositoryModule.kt` |
| Network DI | Registered via `KtorfitClient` lazy properties |
| Central DI | `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt` |
