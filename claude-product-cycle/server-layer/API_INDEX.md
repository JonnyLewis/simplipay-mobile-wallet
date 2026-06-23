# API Index — Server Layer

**Last Updated**: 2026-06-23
**Source**: `core/network/src/commonMain/kotlin/org/mifospay/core/network/`

---

## API Managers

| Manager | Class | Purpose |
|---------|-------|---------|
| `SelfServiceApiManager` | `org.mifospay.core.network.SelfServiceApiManager` | Fineract self-service APIs (9 services) |
| `FineractApiManager` | `org.mifospay.core.network.FineractApiManager` | Direct Fineract APIs (19 services) |
| `InterBankApiManager` | `org.mifospay.core.network.InterBankApiManager` | Inter-bank transfer API |
| `SupabaseApiManager` | `org.mifospay.core.network.SupabaseApiManager` | Supabase-backed config (wraps `SupabaseConfigClient`, not `KtorfitClient`) |

All Ktorfit-based managers wrap the shared `KtorfitClient`. A service may be exposed by more than one manager (e.g. `RegistrationService`, `AuthenticationService` appear on both Self and Fineract).

---

## Services (23 total)

### SelfServiceApiManager Services

| Service | Manager | Path | Endpoints |
|---------|---------|------|-----------|
| AuthenticationService | Self | services/AuthenticationService.kt | login, logout |
| SavingsAccountsService | Self | services/SavingsAccountsService.kt | getSavingsAccounts, getSavingsAccountById |
| BeneficiaryService | Self | services/BeneficiaryService.kt | beneficiaryList, createBeneficiary, updateBeneficiary, deleteBeneficiary |
| ThirdPartyTransferService | Self | services/ThirdPartyTransferService.kt | getTransferTemplates, makeTransfer |
| AccountTransfersService | Self | services/AccountTransfersService.kt | makeAccountTransfer |
| OfficeService | Self | services/OfficeService.kt | offices |
| UserService | Self | services/UserService.kt | getUser, updateUser, getUsers |
| ClientService | Self | services/ClientService.kt | getClientDetails, updateClientImage |

### FineractApiManager Services

| Service | Manager | Path | Endpoints |
|---------|---------|------|-----------|
| KYCLevel1Service | Fineract | services/KYCLevel1Service.kt | addKYCLevel1, fetchKYCLevel1 |
| InvoiceService | Fineract | services/InvoiceService.kt | getInvoices, getInvoiceById, payInvoice |
| AutoPayService | Fineract | services/AutoPayService.kt | getAutoPay, addAutoPay, deleteAutoPay |
| BillService | Fineract | services/BillService.kt | getBills, payBill |
| BillerService | Fineract | services/BillerService.kt | getBillers, getBillerById |
| SavedCardService | Fineract | services/SavedCardService.kt | getSavedCards, addSavedCard, deleteSavedCard |
| StandingInstructionService | Fineract | services/StandingInstructionService.kt | getStandingInstructions, createStandingInstruction |
| NotificationService | Fineract | services/NotificationService.kt | getNotifications, updateNotification |
| RegistrationService | Fineract | services/RegistrationService.kt | registerUser, verifyUser |
| SearchService | Fineract | services/SearchService.kt | searchResources |
| DocumentService | Fineract | services/DocumentService.kt | getDocuments, uploadDocument |
| RunReportService | Fineract | services/RunReportService.kt | runReport |
| TwoFactorAuthService | Fineract | services/TwoFactorAuthService.kt | getOTPToken, validateOTP |

### InterBankApiManager Services

| Service | Manager | Path | Endpoints |
|---------|---------|------|-----------|
| InterBankService | InterBank | services/InterBankService.kt | getInterBankList, makeInterBankTransfer |

### SupabaseApiManager Services

| Service | Manager | Path | Endpoints |
|---------|---------|------|-----------|
| AppConfigService | Supabase | services/AppConfigService.kt | getAppConfig (via `SupabaseConfigClient.postgrest`) |

---

## Endpoint Base

All endpoints use Fineract REST API:
- Self-service base: `/fineract-provider/api/v1/self/`
- Direct Fineract base: `/fineract-provider/api/v1/`

---

## Common Headers

```
Authorization: Basic {base64(username:password)}
Fineract-Platform-TenantId: {tenant}
Content-Type: application/json
```
