# Gap Analysis Command

Comprehensive analysis showing ALL implemented items and ALL gaps across the 5-layer lifecycle. Runs on O(1) by reading index files.

## Usage

```
/gap-analysis                        # FULL comprehensive view (recommended)
/gap-analysis design                 # Design layer only
/gap-analysis design mockup          # Design → Mockup sub-section
/gap-analysis server                 # Server layer only
/gap-analysis client                 # Client layer only
/gap-analysis feature                # Feature layer only
/gap-analysis feature [name]         # Specific feature only
/gap-analysis platform               # Platform layer only
/gap-analysis testing                # Testing status (all layers)
/gap-analysis testing [layer]        # Testing for specific layer
/gap-analysis [feature-name]         # Single feature (all 5 layers)
```

## Comprehensive Output (No Parameters)

When `/gap-analysis` is called without parameters, show the **FULL comprehensive view**:

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  MIFOSPAY MOBILE WALLET - GAP ANALYSIS (O(1) Lookup)                         ║
╠══════════════════════════════════════════════════════════════════════════════╣

## 5-Layer Health Overview

┌─────────────────────────────────────────────────────────────────────────────┐
│  Layer          Progress              Implemented    Gaps    Status         │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. Design      [░░░░░░░░░░] 0%       0/29           29      ❌ Not Started │
│  2. Server      [██████████] 100%     23/23          0       ✅ Complete    │
│  3. Client      [██████████] 100%     53/53          0       ✅ Complete    │
│  4. Feature     [██████████] 100%     29/29          0       ✅ Complete    │
│  5. Platform    [█████████░] 95%      4/4            1       ⚠️ Web exp.    │
├─────────────────────────────────────────────────────────────────────────────┤
│  OVERALL        [██████░░░░] 59%                                            │
└─────────────────────────────────────────────────────────────────────────────┘

---

## ✅ IMPLEMENTED (What's Complete)

### Design Layer (29 features)
| Feature | SPEC | API | STATUS | Mockups |
|---------|:----:|:---:|:------:|:-------:|
| auth | ❌ | ❌ | ❌ | ❌ |
| home | ❌ | ❌ | ❌ | ❌ |
| accounts | ❌ | ❌ | ❌ | ❌ |
[... populate from FEATURES_INDEX.md ...]

### Server Layer (23 services)
| Service | Endpoints | Status |
|---------|:---------:|:------:|
| AuthenticationService | 3 | ✅ |
| BeneficiaryService | 5 | ✅ |
| AutoPayService | - | ✅ |
| BillService | - | ✅ |
| BillerService | - | ✅ |
| ClientService | - | ✅ |
| DocumentService | - | ✅ |
| InterBankService | - | ✅ |
| InvoiceService | - | ✅ |
| KYCLevel1Service | - | ✅ |
| NotificationService | - | ✅ |
| OfficeService | - | ✅ |
| RegistrationService | - | ✅ |
| RunReportService | - | ✅ |
| SavedCardService | - | ✅ |
| SavingsAccountsService | - | ✅ |
| SearchService | - | ✅ |
| StandingInstructionService | - | ✅ |
| ThirdPartyTransferService | - | ✅ |
| TwoFactorAuthService | - | ✅ |
| UserService | - | ✅ |
| AppConfigService | - | ✅ |
| AccountTransfersService | - | ✅ |
[... all 23 services ...]

### Client Layer (23 services, 29 repositories)
| Component | Count | Status |
|-----------|:-----:|:------:|
| Services | 23/23 | ✅ |
| Repositories | 29/29 | ✅ |
| DI Modules | 2/2 | ✅ |

### Feature Layer (29 modules)
| Component | Count | Status |
|-----------|:-----:|:------:|
| Modules | 29/29 | ✅ |
| ViewModels | - | ✅ |
| Screens | - | ✅ |
| DI Modules | 29/29 | ✅ |

### Platform Layer (4 platforms)
| Platform | Build | Status |
|----------|:-----:|:------:|
| Android | ✅ | Primary |
| iOS | ✅ | CocoaPods |
| Desktop | ✅ | JVM |
| Web | ⚠️ | Experimental |

> **Parity is a first-class gap category here.** Per `CLAUDE.md` → *Cross-Platform Parity*, a feature
> that works on iOS but not Android (or vice versa) is a **P1 gap**, not "done on one platform." When
> analysing the platform layer, scan for **shell asymmetry**: an `expect` with a real `actual` on one
> OS and a stub/`// TODO` on the other, a permission in `Info.plist` but not `AndroidManifest.xml`
> (or vice versa), a native dep wired in `Podfile` but not Gradle, or app-shell wiring present in the
> `AppDelegate` but not `MainActivity`/`MifosPayApp`. A missing feature is a parity gap; an
> OS-specific *bug* is tracked separately.

---

## ❌ GAPS (What Needs Work)

### P0 - Critical (Blocks Other Work)
| Gap | Layer | Impact | Plan Command |
|-----|-------|--------|--------------|
| (none currently) | - | - | - |

### P1 - High Priority (User-Facing)
| Gap | Layer | Impact | Plan Command |
|-----|-------|--------|--------------|
| Missing specs (29) | Design | Feature planning blocked | `/gap-planning design spec` |
| Missing mockups (29) | Design | UI consistency blocked | `/gap-planning design mockup` |

### P2 - Nice to Have (Polish)
| Gap | Layer | Impact | Plan Command |
|-----|-------|--------|--------------|
| Web experimental | Platform | Limited browser support | `/gap-planning platform web` |

---

## 🧪 TESTING STATUS

| Layer | Unit | UI | Integration | Screenshot | Status |
|-------|:----:|:--:|:-----------:|:----------:|:------:|
| Client (Repo) | - | - | - | - | ⬜ Not Started |
| Feature (VM) | 0 | 0 | 0 | 0 | ⬜ Not Started |
| Platform (E2E) | - | - | 0 | 0 | ⬜ Not Started |

**Testing Gaps** (P1):
| Gap | Impact | Plan Command |
|-----|--------|--------------|
| ViewModel tests | No regression safety | `/gap-planning testing feature` |
| UI tests | No UI verification | `/gap-planning testing feature` |
| E2E tests | No flow coverage | `/gap-planning testing platform` |
| Screenshot tests | No visual regression | `/gap-planning testing platform` |

→ For detailed testing status: `/gap-analysis testing`

---

## 🛠️ AVAILABLE ACTIONS

### Create Plans
| Gap | Command | What It Does |
|-----|---------|--------------|
| All specs | `/gap-planning design spec` | Write specs for 29 features |
| All mockups | `/gap-planning design mockup` | Generate mockups for 29 features |
| Specific feature | `/gap-planning [feature]` | Plan single feature improvements |
| Web platform | `/gap-planning platform web` | Stabilize web build |

### Implement
| Target | Command | What It Does |
|--------|---------|--------------|
| E2E feature | `/implement [feature]` | Full implementation |
| Client only | `/client [feature]` | Network + Data layers |
| UI only | `/feature [feature]` | ViewModel + Screen |

### Verify
| Target | Command | What It Does |
|--------|---------|--------------|
| Any feature | `/verify [feature]` | Check implementation vs spec |

### Testing
| Target | Command | What It Does |
|--------|---------|--------------|
| Run tests | `/verify-tests [feature]` | Run tests for feature |
| Test status | `/gap-analysis testing` | See testing coverage |
| Plan tests | `/gap-planning testing [layer]` | Plan test implementation |

---

## 📊 O(1) PERFORMANCE

| Metric | Before | After | Improvement |
|--------|:------:|:-----:|:-----------:|
| Files to scan | 10-50 | 1-2 | **90% fewer** |
| Lines to read | 500-3000 | 60-200 | **80-95% less** |
| Tool calls | 3-5 | 1-2 | **60% fewer** |

---

## 📁 O(1) INDEX FILES

| Layer | Index File | Lines | Use For |
|-------|------------|:-----:|---------|
| Feature | `MODULES_INDEX.md` | ~180 | Find any module |
| Feature | `SCREENS_INDEX.md` | ~250 | Find any screen |
| Design | `FEATURES_INDEX.md` | ~120 | Check feature status |
| Design | `MOCKUPS_INDEX.md` | ~120 | Check mockup status |
| Client | `FEATURE_MAP.md` | ~200 | Map feature → services |
| Server | `API_INDEX.md` | ~100 | Find any endpoint |
| Platform | `LAYER_STATUS.md` | ~80 | Platform commands |

---

## 🎯 RECOMMENDED NEXT STEPS

Based on current gaps:

1. **Design Specs** (P1) - 29 features need specs written
   → `/gap-planning design spec`

2. **Design Mockups** (P1) - 29 features need mockups
   → `/gap-planning design mockup`

3. **Web Platform** (P2) - Experimental status
   → `/gap-planning platform web`

4. **Verify Features** - Ensure all features match spec
   → `/verify [feature-name]`

╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## 5-Layer Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Design   → spec | mockup | api | status                     │
│  2. Server   → endpoints | availability                         │
│  3. Client   → network | data | model                           │
│  4. Feature  → viewmodel | screen | navigation | di             │
│  5. Platform → android | ios | desktop | web                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Instructions

### Step 1: Read O(1) Index Files

Read these files for instant status (DO NOT scan directories):

| Layer | Index File | Path |
|-------|------------|------|
| Feature | MODULES_INDEX.md | `claude-product-cycle/feature-layer/MODULES_INDEX.md` |
| Feature | SCREENS_INDEX.md | `claude-product-cycle/feature-layer/SCREENS_INDEX.md` |
| Design | FEATURES_INDEX.md | `claude-product-cycle/design-spec-layer/FEATURES_INDEX.md` |
| Design | MOCKUPS_INDEX.md | `claude-product-cycle/design-spec-layer/MOCKUPS_INDEX.md` |
| Client | FEATURE_MAP.md | `claude-product-cycle/client-layer/FEATURE_MAP.md` |
| Server | API_INDEX.md | `claude-product-cycle/server-layer/API_INDEX.md` |
| Platform | LAYER_STATUS.md | `claude-product-cycle/platform-layer/LAYER_STATUS.md` |
| Testing | TESTING_STATUS.md | `claude-product-cycle/*/TESTING_STATUS.md` (per layer) |

### Step 2: Calculate Progress

From index files, calculate:
- **Design**: Count ✅ in FEATURES_INDEX.md + MOCKUPS_INDEX.md
- **Server**: All 23 services in core/network/services/ = 100%
- **Client**: Count services + repositories in FEATURE_MAP.md
- **Feature**: Count modules + screens in MODULES_INDEX.md + SCREENS_INDEX.md
- **Platform**: Count working platforms in LAYER_STATUS.md

### Step 3: Identify Gaps

From index files, find items marked ⚠️ or ❌:
- Missing specs/mockups → `MOCKUPS_INDEX.md`, `FEATURES_INDEX.md`
- Missing services → `FEATURE_MAP.md`
- Missing screens → `SCREENS_INDEX.md`
- Platform issues → `LAYER_STATUS.md`

### Step 4: Generate Output

Fill the comprehensive template with:
1. Real percentages from index files
2. All implemented items (✅)
3. All gaps (⚠️/❌) with `/gap-planning` commands
4. Recommended next steps based on priorities

---

## Layer-Specific Parameters

When a layer parameter is provided, show detailed view for that layer:

| Parameter | Shows |
|-----------|-------|
| `design` | All 29 features: SPEC, API, STATUS, Mockups status |
| `design mockup` | Mockup-specific: Figma links, Stitch prompts, design-tokens |
| `design spec` | Specification status for all 29 features |
| `server` | All 23 services with endpoint counts |
| `client` | All 23 services and 29 repositories |
| `client network` | Network services only |
| `client data` | Repositories only |
| `feature` | All 29 modules with screens, ViewModels, DI |
| `feature [name]` | Single feature: all layers |
| `platform` | All 4 platforms with build commands |
| `platform [name]` | Single platform details |
| `testing` | Testing coverage across all layers |
| `testing [layer]` | Testing for specific layer (design/client/feature/platform) |

---

## Feature Reference

| # | Feature | Design Dir | Feature Module |
|:-:|---------|------------|----------------|
| 1 | auth | features/auth/ | feature/auth/ |
| 2 | home | features/home/ | feature/home/ |
| 3 | accounts | features/accounts/ | feature/accounts/ |
| 4 | history | features/history/ | feature/history/ |
| 5 | receipt | features/receipt/ | feature/receipt/ |
| 6 | faq | features/faq/ | feature/faq/ |
| 7 | make-transfer | features/make-transfer/ | feature/make-transfer/ |
| 8 | send-money | features/send-money/ | feature/send-money/ |
| 9 | transfer-intrabank | features/transfer-intrabank/ | feature/transfer-intrabank/ |
| 10 | transfer-interbank | features/transfer-interbank/ | feature/transfer-interbank/ |
| 11 | notification | features/notification/ | feature/notification/ |
| 12 | editpassword | features/editpassword/ | feature/editpassword/ |
| 13 | kyc | features/kyc/ | feature/kyc/ |
| 14 | savedcards | features/savedcards/ | feature/savedcards/ |
| 15 | invoices | features/invoices/ | feature/invoices/ |
| 16 | settings | features/settings/ | feature/settings/ |
| 17 | profile | features/profile/ | feature/profile/ |
| 18 | finance | features/finance/ | feature/finance/ |
| 19 | merchants | features/merchants/ | feature/merchants/ |
| 20 | beneficiary | features/beneficiary/ | feature/beneficiary/ |
| 21 | standing-instruction | features/standing-instruction/ | feature/standing-instruction/ |
| 22 | payments | features/payments/ | feature/payments/ |
| 23 | upi-setup | features/upi-setup/ | feature/upi-setup/ |
| 24 | qr | features/qr/ | feature/qr/ |
| 25 | autopay | features/autopay/ | feature/autopay/ |
| 26 | mpay-qr | features/mpay-qr/ | feature/mpay-qr/ |
| 27 | mpay-qr-scan | features/mpay-qr-scan/ | feature/mpay-qr-scan/ |
| 28 | fast-mpay | features/fast-mpay/ | feature/fast-mpay/ |
| 29 | passcode | features/passcode/ | feature/passcode/ |

---

## Output Rules

1. **Read index files only** - Never scan directories when index files exist
2. **Show everything** - All implemented + all gaps in one view
3. **Include all `/gap-planning` commands** - For every gap found
4. **Use progress bars** - Visual at-a-glance status
5. **Prioritize gaps** - P0 → P1 → P2
6. **Show recommended next steps** - Based on current gaps
7. **NO interactive questions** - Comprehensive view, user decides

---

## Progress Bar Reference

```
100% = [██████████]  |  50% = [█████░░░░░]
 95% = [█████████▌]  |  40% = [████░░░░░░]
 90% = [█████████░]  |  30% = [███░░░░░░░]
 80% = [████████░░]  |  20% = [██░░░░░░░░]
 70% = [███████░░░]  |  10% = [█░░░░░░░░░]
 60% = [██████░░░░]  |   0% = [░░░░░░░░░░]
```

**Status Icons**: ✅ Complete | ⚠️ Partial | ❌ Missing | `-` N/A
