# /verify - Implementation Verification

## Purpose

Validate implementation matches specification using O(1) lookup. Compares SPEC.md requirements against actual code and identifies gaps with actionable fixes.

---

## Command Variants

```
/verify                          # Show all features verification status
/verify [Feature]                # Full verification for feature
/verify [Feature] --quick        # Skip detailed code analysis
/verify [Feature] --spec         # Verify spec completeness only
/verify [Feature] --code         # Verify code completeness only
/verify all                      # Verify all features (summary)
```

---

## Verification Pipeline with O(1) Optimization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  /verify [Feature] - O(1) OPTIMIZED PIPELINE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PHASE 0: O(1) CONTEXT LOADING                                              │
│  ├─→ Read FEATURES_INDEX.md           → Feature exists? Spec status?        │
│  ├─→ Read FEATURE_MAP.md              → Expected services/repos             │
│  ├─→ Read MODULES_INDEX.md            → Expected VMs/Screens                │
│  ├─→ Read SCREENS_INDEX.md            → Screen-ViewModel mapping            │
│  └─→ Read API_INDEX.md                → Expected endpoints                  │
│                                                                              │
│  PHASE 1: SPEC ANALYSIS                                                     │
│  ├─→ Read design-spec-layer/features/[name]/SPEC.md  → requirements         │
│  ├─→ Read design-spec-layer/features/[name]/API.md   → API requirements     │
│  ├─→ Read design-spec-layer/features/[name]/STATUS.md → status claims       │
│  └─→ Build requirement checklist      → What SHOULD exist                   │
│                                                                              │
│  PHASE 2: CODE ANALYSIS (O(1) paths from indexes)                           │
│  ├─→ Check ViewModel exists           → From SCREENS_INDEX.md path          │
│  ├─→ Check Screen exists              → From SCREENS_INDEX.md path          │
│  ├─→ Check Service exists             → From FEATURE_MAP.md path            │
│  ├─→ Check Repository exists          → From FEATURE_MAP.md path            │
│  └─→ Build implementation checklist   → What DOES exist                     │
│                                                                              │
│  PHASE 3: DEEP VERIFICATION (if not --quick)                                │
│  ├─→ Read ViewModel code              → Check State/Event/Action            │
│  ├─→ Read Screen code                 → Check UI states, TestTags           │
│  ├─→ Compare SPEC actions vs code     → All actions handled?                │
│  ├─→ Compare SPEC states vs code      → All states rendered?                │
│  └─→ Check DI registration            → Koin modules complete?              │
│                                                                              │
│  PHASE 4: GAP DETECTION                                                     │
│  ├─→ Compare requirement vs impl      → Identify missing items              │
│  ├─→ Categorize gaps by severity      → P0 (critical) → P2 (polish)         │
│  ├─→ Generate fix suggestions         → Actionable steps                    │
│  └─→ Calculate verification score     → Percentage complete                 │
│                                                                              │
│  PHASE 5: REPORT & UPDATE                                                   │
│  ├─→ Generate verification report     → Structured output                   │
│  ├─→ Update STATUS.md (optional)      → If user approves                    │
│  └─→ Suggest next command             → /implement or /gap-planning         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PHASE 0: O(1) Context Loading

### Files to Read (~500 lines total instead of scanning)

| File | Purpose | Data Extracted |
|------|---------|----------------|
| `claude-product-cycle/design-spec-layer/FEATURES_INDEX.md` | Feature inventory | featureExists, specStatus |
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Service/Repo mapping | expectedServices[], expectedRepos[] |
| `claude-product-cycle/feature-layer/MODULES_INDEX.md` | Module structure | expectedVMs, expectedScreens |
| `claude-product-cycle/feature-layer/SCREENS_INDEX.md` | Screen details | screenPaths[], vmPaths[] |
| `claude-product-cycle/server-layer/API_INDEX.md` | Endpoint inventory | expectedEndpoints[] |

---

## PHASE 2: Code Analysis (O(1) Paths)

### File Paths from Index Files

| Component | Path Source | Example Path |
|-----------|-------------|--------------|
| ViewModel | SCREENS_INDEX.md | `feature/beneficiary/src/commonMain/kotlin/org/mifospay/feature/beneficiary/list/BeneficiaryListViewModel.kt` |
| Screen | SCREENS_INDEX.md | `feature/beneficiary/src/commonMain/kotlin/org/mifospay/feature/beneficiary/list/BeneficiaryListScreen.kt` |
| Service | FEATURE_MAP.md | `core/network/src/commonMain/kotlin/org/mifospay/core/network/services/BeneficiaryService.kt` |
| Repository | FEATURE_MAP.md | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/BeneficiaryRepository.kt` |
| Repository Impl | FEATURE_MAP.md | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/BeneficiaryRepositoryImpl.kt` |
| DI Module | MODULES_INDEX.md | `feature/beneficiary/src/commonMain/kotlin/org/mifospay/feature/beneficiary/di/BeneficiaryModule.kt` |

*Note: Some modules (beneficiary, upi-setup, merchants, transfer-interbank) place ViewModels and Screens in subdirectories rather than flat in the package root. Always resolve the actual path from SCREENS_INDEX.md or by checking the module directory before referencing.*

---

## PHASE 5: Report Generation

### Full Verification Report

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║  /verify beneficiary - VERIFICATION REPORT                                    ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  📊 VERIFICATION SCORE: 85%  [████████░░]                                     ║
║                                                                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║  ✅ PASSING CHECKS                                                            ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  CLIENT LAYER:                                                                 ║
║  ├─ BeneficiaryService.kt                     ✅ Exists                       ║
║  ├─ BeneficiaryRepository.kt                  ✅ Exists                       ║
║  ├─ BeneficiaryRepositoryImpl.kt              ✅ Exists                       ║
║  ├─ KtorfitClient registration                ✅ Registered                   ║
║  └─ RepositoryModule registration             ✅ Registered                   ║
║                                                                                ║
║  FEATURE LAYER:                                                                ║
║  ├─ list/BeneficiaryListViewModel.kt            ✅ Exists                       ║
║  ├─ list/BeneficiaryListScreen.kt             ✅ Exists                       ║
║  ├─ addupdatebeneficiary/BeneficiaryNavigation.kt ✅ Exists                   ║
║  ├─ di/BeneficiaryModule.kt                   ✅ DI registered                ║
║  ├─ Registered in KoinModules.kt              ✅                              ║
║  └─ Registered in MifosNavHost.kt             ✅                              ║
║                                                                                ║
║  STATE MODEL:                                                                  ║
║  ├─ State class                               ✅ Defined                      ║
║  ├─ UiState sealed interface                  ✅ Loading/Success/Error        ║
║  ├─ Event sealed interface                    ✅ Navigation events            ║
║  └─ Action sealed interface                   ✅ User actions                 ║
║                                                                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║  ⚠️ GAPS FOUND (3)                                                            ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  P1 - MAJOR (2):                                                              ║
║  ┌────────────────────────────────────────────────────────────────────────┐   ║
║  │ Gap: Empty state not implemented                                       │   ║
║  │ Spec: SPEC.md Section 2.4 - "Show empty illustration when no data"     │   ║
║  │ File: feature/beneficiary/.../BeneficiaryListScreen.kt                 │   ║
║  │                                                                         │   ║
║  │ 📍 Fix:                                                                 │   ║
║  │ Add to BeneficiaryListScreen:                                          │   ║
║  │ ```kotlin                                                              │   ║
║  │ is BeneficiaryUiState.Empty -> {                                       │   ║
║  │     BeneficiaryEmpty(                                                  │   ║
║  │         onAddClick = { onAction(BeneficiaryAction.OnAddClick) }        │   ║
║  │     )                                                                  │   ║
║  │ }                                                                      │   ║
║  │ ```                                                                    │   ║
║  └────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                ║
║  P2 - MINOR (1):                                                              ║
║  ┌────────────────────────────────────────────────────────────────────────┐   ║
║  │ Gap: TestTags object missing                                           │   ║
║  │ File: feature/beneficiary/.../BeneficiaryTestTags.kt (create)         │   ║
║  │ 📍 Fix: Run /feature beneficiary --tags to generate                    │   ║
║  └────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║  🎯 NEXT STEPS                                                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  Options:                                                                      ║
║  • f / fix       → Run /implement beneficiary to auto-fix gaps                ║
║  • m / manual    → Fix gaps manually using suggestions above                  ║
║  • u / update    → Update STATUS.md to reflect current state                  ║
║  • i / ignore    → Mark gaps as intentional (document reason)                 ║
║                                                                                ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

---

## All Features Verification (No Argument)

When `/verify` called without arguments, show summary from index files:

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║  /verify - ALL FEATURES VERIFICATION STATUS                                   ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  | # | Feature              | Spec | Client | Feature | Score | Gaps |        ║
║  |:-:|----------------------|:----:|:------:|:-------:|:-----:|:----:|        ║
║  | 1 | auth                 | ✅   | ✅     | ✅      | 95%   | 1    |        ║
║  | 2 | home                 | ✅   | ✅     | ✅      | 100%  | 0    |        ║
║  | 3 | accounts             | ✅   | ✅     | ✅      | 98%   | 1    |        ║
║  | 4 | beneficiary          | ✅   | ✅     | ✅      | 85%   | 3    |        ║
║  | 5 | make-transfer        | ⚠️   | ✅     | ⚠️      | 60%   | 4    |        ║
║  | 6 | send-money           | ✅   | ✅     | ✅      | 90%   | 2    |        ║
║  | 7 | transfer-intrabank   | ✅   | ✅     | ✅      | 95%   | 1    |        ║
║  | 8 | transfer-interbank   | ✅   | ✅     | ✅      | 90%   | 2    |        ║
║  | 9 | notification         | ✅   | ✅     | ✅      | 100%  | 0    |        ║
║  | 10| editpassword         | ✅   | ✅     | ✅      | 100%  | 0    |        ║
║  | 11| kyc                  | ✅   | ✅     | ✅      | 85%   | 3    |        ║
║  | 12| savedcards           | ✅   | ✅     | ✅      | 92%   | 1    |        ║
║  | 13| invoices             | ✅   | ✅     | ✅      | 88%   | 2    |        ║
║  | 14| settings             | ✅   | -      | ✅      | 85%   | 1    |        ║
║  | 15| profile              | ✅   | ✅     | ✅      | 92%   | 1    |        ║
║  | 16| finance              | ✅   | -      | ✅      | 95%   | 1    |        ║
║  | 17| merchants            | ✅   | ✅     | ✅      | 90%   | 1    |        ║
║  | 18| autopay              | ✅   | ✅     | ✅      | 88%   | 2    |        ║
║  | 19| qr                   | ⚠️   | -      | ⚠️      | 55%   | 4    |        ║
║  | 20| payments             | ✅   | -      | ✅      | 95%   | 1    |        ║
║  | 21| upi-setup            | ✅   | ✅     | ✅      | 90%   | 1    |        ║
║  | 22| standing-instruction | ✅   | ✅     | ✅      | 88%   | 2    |        ║
║  | 23| history              | ✅   | ✅     | ✅      | 100%  | 0    |        ║
║  | 24| receipt              | ✅   | ✅     | ✅      | 100%  | 0    |        ║
║  | 25| faq                  | ✅   | -      | ✅      | 100%  | 0    |        ║
║  | 26| mpay-qr              | ✅   | -      | ✅      | 90%   | 1    |        ║
║  | 27| mpay-qr-scan         | ✅   | -      | ✅      | 90%   | 1    |        ║
║  | 28| fast-mpay            | ✅   | -      | ✅      | 88%   | 1    |        ║
║  | 29| passcode             | ✅   | -      | ✅      | 100%  | 0    |        ║
║                                                                                ║
║  Commands:                                                                     ║
║  • /verify [feature]     → Detailed verification                              ║
║  • /verify all --fix     → Show all gaps with fixes                           ║
║  • /gap-planning feature → Plan to fix gaps                                   ║
║                                                                                ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

---

## Verification Checklist (Quick Reference)

### Client Layer Checks

| Check | Source | Verification |
|-------|--------|--------------|
| Service exists | FEATURE_MAP.md | File exists at path |
| Repository exists | FEATURE_MAP.md | File exists at path |
| RepositoryImpl exists | FEATURE_MAP.md | File exists at path |
| KtorfitClient registration | KtorfitClient.kt | Contains service lazy property |
| RepositoryModule registration | RepositoryModule.kt | Contains repo binding |

### Feature Layer Checks

| Check | Source | Verification |
|-------|--------|--------------|
| ViewModel exists | SCREENS_INDEX.md | File exists at path |
| Screen exists | SCREENS_INDEX.md | File exists at path |
| Navigation exists | MODULES_INDEX.md | `navigation/${Feature}Navigation.kt` |
| DI Module exists | MODULES_INDEX.md | `di/${Feature}Module.kt` |
| KoinModules registration | KoinModules.kt | Module in allModules list |
| MifosNavHost registration | MifosNavHost.kt | Contains nav destination |

### State Model Checks

| Check | Source | Verification |
|-------|--------|--------------|
| State class defined | ViewModel file | `data class ${Feature}State` |
| UiState sealed | ViewModel file | `sealed interface ViewState` (nested inside State class) or `sealed interface ${Feature}UiState` (top-level, generated by `/feature`) |
| Event sealed | ViewModel file | `sealed interface ${Feature}Event` |
| Action sealed | ViewModel file | `sealed interface ${Feature}Action` |
| handleAction implemented | ViewModel file | `override fun handleAction` |

### UI State Checks

| Check | Source | Verification |
|-------|--------|--------------|
| Loading state | Screen file | `${Feature}UiState.Loading` branch |
| Success state | Screen file | `${Feature}UiState.Success` branch |
| Error state | Screen file | `${Feature}UiState.Error` branch |
| Empty state | Screen file | `${Feature}UiState.Empty` branch (if in spec) |

### Testing Checks

| Check | Source | Verification |
|-------|--------|--------------|
| TestTags object | Screen directory | `${Feature}TestTags.kt` exists |
| testTag modifiers | Screen file | `Modifier.testTag()` used |
| TestTag naming | TestTags object | Follows `feature:component:id` pattern |
| All states tagged | Screen file | Loading, Success, Error have tags |

---

## TestTag Validation

### Naming Convention

Pattern: `feature:component:element`

| Component | Pattern | Example |
|-----------|---------|---------|
| Screen | `{feature}:screen` | `beneficiary:screen` |
| Loading | `{feature}:loading` | `beneficiary:loading` |
| Error | `{feature}:error` | `beneficiary:error` |
| List | `{feature}:list` | `beneficiary:list` |
| Item | `{feature}:item:{id}` | `beneficiary:item:123` |
| Button | `{feature}:{action}` | `beneficiary:retry`, `beneficiary:add` |
| Input | `{feature}:input:{name}` | `auth:input:username` |

---

## Error Handling

### Feature Not Found

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ❌ FEATURE NOT FOUND                                                        │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Feature: "xyz"                                                              │
│  Checked: FEATURES_INDEX.md                                                  │
│                                                                               │
│  Did you mean one of these?                                                  │
│  • beneficiary, payments, autopay, qr                                        │
│                                                                               │
│  Or run /verify to see all 29 features.                                      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## O(1) File Reference

| Index File | Data Used For |
|------------|---------------|
| `claude-product-cycle/design-spec-layer/FEATURES_INDEX.md` | Feature list, spec status |
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Service/Repository paths |
| `claude-product-cycle/feature-layer/MODULES_INDEX.md` | Module structure, VM/Screen counts |
| `claude-product-cycle/feature-layer/SCREENS_INDEX.md` | Screen-ViewModel mappings, file paths |
| `claude-product-cycle/server-layer/API_INDEX.md` | Expected API endpoints |

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/implement [Feature]` | Fix gaps automatically |
| `/gap-analysis [Feature]` | Broader gap analysis |
| `/gap-planning [Feature]` | Plan fixes for gaps |
| `/design [Feature]` | Update specification |
| `/verify-tests [Feature]` | Verify test coverage |
