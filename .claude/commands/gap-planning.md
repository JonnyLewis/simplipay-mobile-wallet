# Gap Planning Command

Creates step-by-step implementation plans for identified gaps. Runs on O(1) by reading index files.

## Usage

```
/gap-planning                        # Show ALL gaps with ALL plan commands
/gap-planning design                 # Plan all design layer work
/gap-planning design mockup          # Plan mockup generation (29 features)
/gap-planning design spec            # Plan specification updates
/gap-planning server                 # Plan server documentation
/gap-planning client                 # Plan all client layer work
/gap-planning client network         # Plan network services
/gap-planning client data            # Plan repositories
/gap-planning feature                # Plan all feature layer work
/gap-planning feature [name]         # Plan specific feature
/gap-planning platform               # Plan all platform work
/gap-planning platform web           # Plan web stabilization
/gap-planning testing                # Plan all testing work
/gap-planning testing client         # Plan client layer tests
/gap-planning testing feature        # Plan feature layer tests (VM + UI)
/gap-planning testing platform       # Plan E2E + screenshot tests
/gap-planning testing [feature]      # Plan tests for specific feature
/gap-planning [feature-name]         # Plan specific feature (all 5 layers)
```

---

## Comprehensive Output (No Parameters)

When `/gap-planning` is called without parameters, show **ALL gaps with ALL implementation plans**:

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  MIFOSPAY MOBILE WALLET - GAP PLANNING (O(1) Lookup)                         ║
║  All Gaps → All Plans → You Choose                                           ║
╠══════════════════════════════════════════════════════════════════════════════╣

## Current Gaps Overview

| Layer | Gaps | Priority | Status |
|-------|:----:|:--------:|--------|
| Design | 29 specs, 29 mockups | P1 | Ready to plan |
| Server | 0 | - | ✅ Complete |
| Client | 0 | - | ✅ Complete |
| Feature | 0 | - | ✅ Complete |
| Platform | 1 (web) | P2 | Ready to plan |

---

## 📋 ALL AVAILABLE PLANS

### P0 - Critical (Blocks Other Work)

| Gap | Plan Command | Tasks | Effort |
|-----|--------------|:-----:|:------:|
| (none currently) | - | - | - |

### P1 - High Priority (User-Facing)

| # | Gap | Plan Command | Tasks | Effort |
|:-:|-----|--------------|:-----:|:------:|
| 1 | Missing specs (29 features) | `/gap-planning design spec` | 29 | L |
| 2 | Missing mockups (29 features) | `/gap-planning design mockup` | 87 | L |

**Design Mockup Tasks Preview**:
```
Features needing mockups:
1. auth          → login, signup, mobile verification screens
2. home          → wallet home, account cards, quick actions
3. accounts      → savings account list, detail
4. history       → transaction history, detail
5. payments      → payments tab, request, transfer type selection
6. send-money    → send, pay anyone, bank transfer, UPI flow
7. transfer-intrabank → hub, payee select, confirm, success
8. transfer-interbank → search recipient, preview, result
9. beneficiary   → list, add, edit, delete
10. autopay      → schedule management, bills, billers, history
11. mpay-qr      → QR generation, account picker
12. mpay-qr-scan → camera scan, import, processing
13. kyc          → levels 1-3 screens
14. savedcards   → card list, add/edit, detail
15. invoices     → invoice list, detail
16. merchants    → merchant list, merchant transfer
17. profile      → profile, edit profile
18. standing-instruction → list, create/edit, detail
19. upi-setup    → debit card, OTP, UPI PIN setup
20. settings     → app settings, language
21. notification → notification list
22. editpassword → change password
23. receipt      → transaction receipt
24. faq          → FAQ list
25. fast-mpay    → fast QR payment processing
26. make-transfer → transfer orchestration
27. qr           → basic QR scanner
28. finance      → finance overview tab
29. passcode     → passcode setup, biometrics

Run `/gap-planning design mockup` for step-by-step tasks.
```

### P2 - Nice to Have (Polish)

| # | Gap | Plan Command | Tasks | Effort |
|:-:|-----|--------------|:-----:|:------:|
| 1 | Web experimental | `/gap-planning platform web` | 5 | M |

**Web Platform Tasks Preview**:
```
1. Fix Kotlin/JS compilation warnings
2. Add CORS handling for production
3. Implement WebSocket fallback
4. Optimize bundle size
5. Add Safari compatibility fixes

Run `/gap-planning platform web` for step-by-step tasks.
```

### 🧪 Testing (Embedded in Layers)

| # | Gap | Plan Command | Tests | Effort |
|:-:|-----|--------------|:-----:|:------:|
| 1 | ViewModel tests | `/gap-planning testing feature` | 200+ | L |
| 2 | UI tests | `/gap-planning testing feature` | 150+ | L |
| 3 | E2E tests | `/gap-planning testing platform` | 30+ | M |
| 4 | Screenshot tests | `/gap-planning testing platform` | 60+ | M |
| 5 | Repository tests | `/gap-planning testing client` | 50+ | M |

**Testing Priority by Feature**:
```
P0 - Core: auth, home, payments, transfer-intrabank
P1 - Payments: send-money, transfer-interbank, mpay-qr, mpay-qr-scan
P2 - Management: autopay, beneficiary, savedcards, standing-instruction
P3 - Supporting: settings, notification, kyc, passcode, invoices
P4 - Other: faq, receipt, editpassword, profile, merchants, finance
```

→ Run `/gap-planning testing [feature]` for per-feature test plan.

---

## 🎯 QUICK START

Pick a plan based on priority:

| Priority | Recommendation | Command |
|:--------:|----------------|---------|
| **P1** | Start with design specs | `/gap-planning design spec` |
| **P1** | Then mockups | `/gap-planning design mockup` |
| **P2** | Then web platform | `/gap-planning platform web` |

Or jump directly to implementation:

| Target | Command |
|--------|---------|
| Single feature spec | `/design [feature-name]` |
| Feature implementation | `/implement [feature-name]` |
| Verify existing | `/verify [feature-name]` |

---

## 🔄 WORKFLOW

```
/gap-analysis           →  See all status (O(1) comprehensive view)
       │
       ▼
/gap-planning           →  See all plans (this view)
       │
       ▼
/gap-planning [target]  →  Get detailed step-by-step tasks
       │
       ▼
/implement [target]     →  Execute the plan
       │
       ▼
/verify [target]        →  Confirm completion
       │
       ▼
/gap-analysis           →  Updated status (loop back)
```

╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## Detailed Plans (With Parameter)

When a specific target is provided, show the **detailed step-by-step plan**.

### Design Mockup Plan (`/gap-planning design mockup`)

```
## Design Mockup Generation Plan

**Target**: 29 features needing mockups
**Effort**: Large (87+ tasks across 29 features)
**Tool**: Google Stitch / Figma

### Features & Tasks (Priority Order)

| # | Feature | Screens | Tasks | Priority |
|:-:|---------|:-------:|:-----:|:--------:|
| 1 | auth | 3 | 6 | P0 |
| 2 | home | 2 | 4 | P0 |
| 3 | payments | 3 | 6 | P0 |
| 4 | transfer-intrabank | 4 | 8 | P1 |
| 5 | send-money | 5 | 10 | P1 |
| 6 | transfer-interbank | 5 | 10 | P1 |
| 7 | beneficiary | 3 | 6 | P1 |
| 8 | autopay | 6 | 12 | P1 |
| 9 | mpay-qr | 2 | 4 | P1 |
| 10 | mpay-qr-scan | 2 | 4 | P1 |
| 11 | accounts | 2 | 4 | P2 |
| 12 | kyc | 3 | 6 | P2 |
| 13 | savedcards | 3 | 6 | P2 |
| 14 | invoices | 2 | 4 | P2 |
| 15 | merchants | 2 | 4 | P2 |
| 16 | profile | 2 | 4 | P2 |
| 17 | standing-instruction | 3 | 6 | P2 |
| 18 | upi-setup | 3 | 6 | P2 |
| 19 | history | 3 | 6 | P3 |
| 20 | settings | 2 | 4 | P3 |
| 21 | notification | 1 | 2 | P3 |
| 22 | editpassword | 1 | 2 | P3 |
| 23 | receipt | 1 | 2 | P3 |
| 24 | faq | 1 | 2 | P3 |
| 25 | fast-mpay | 1 | 2 | P3 |
| 26 | make-transfer | 1 | 2 | P3 |
| 27 | qr | 1 | 2 | P3 |
| 28 | finance | 1 | 2 | P3 |
| 29 | passcode | 2 | 4 | P3 |

### Per-Feature Tasks

For each feature:
1. Read SPEC.md to understand screens
2. Read API.md to understand data
3. Generate PROMPTS_STITCH.md for Google Stitch
4. Generate mockup images
5. Create design-tokens.json
6. Update FIGMA_LINKS.md with URLs

### Execution Commands

| Feature | Command |
|---------|---------|
| auth (P0) | `/design auth mockup` |
| home (P0) | `/design home mockup` |
| payments (P0) | `/design payments mockup` |
| transfer-intrabank | `/design transfer-intrabank mockup` |
| send-money | `/design send-money mockup` |
| transfer-interbank | `/design transfer-interbank mockup` |
| beneficiary | `/design beneficiary mockup` |
| autopay | `/design autopay mockup` |
| mpay-qr | `/design mpay-qr mockup` |
| mpay-qr-scan | `/design mpay-qr-scan mockup` |
[... continue for all 29 ...]

### Verification

After each feature:
- [ ] PROMPTS_STITCH.md exists
- [ ] Mockup images generated
- [ ] design-tokens.json created
- [ ] FIGMA_LINKS.md updated
- [ ] MOCKUPS_INDEX.md updated
```

### Platform Web Plan (`/gap-planning platform web`)

```
## Web Platform Stabilization Plan

**Target**: Move web from experimental to stable
**Effort**: Medium (5 tasks)
**Module**: cmp-web

### Current Status

| Issue | Impact | Fix |
|-------|--------|-----|
| Kotlin/JS warnings | Build noise | Suppress/fix |
| CORS in production | API blocked | Server headers |
| WebSocket issues | Real-time fails | Polling fallback |
| Large bundle | Slow load | Tree shaking |
| Safari compat | 15% users | Polyfills |

### Tasks

1. **Fix compilation warnings**
   - File: `cmp-web/build.gradle.kts`
   - Action: Add suppressions or fix warnings

2. **CORS configuration**
   - File: Server config (Fineract)
   - Action: Add Access-Control-Allow-Origin headers

3. **WebSocket fallback**
   - File: `cmp-shared/.../network/`
   - Action: Implement polling when WebSocket fails

4. **Bundle optimization**
   - File: `cmp-web/build.gradle.kts`
   - Action: Enable tree shaking, code splitting

5. **Safari compatibility**
   - File: `cmp-web/src/jsMain/resources/`
   - Action: Add polyfills for missing APIs

### Verification

- [ ] `./gradlew :cmp-web:jsBrowserProductionWebpack` builds clean
- [ ] App loads in Safari
- [ ] API calls work in production
- [ ] Bundle size < 2MB
```

---

## Instructions for Claude

### Step 1: Read O(1) Index Files

Read these files for gap information:

| Need | Index File | Path |
|------|------------|------|
| Design gaps | MOCKUPS_INDEX.md | `claude-product-cycle/design-spec-layer/MOCKUPS_INDEX.md` |
| Feature gaps | MODULES_INDEX.md | `claude-product-cycle/feature-layer/MODULES_INDEX.md` |
| Client gaps | FEATURE_MAP.md | `claude-product-cycle/client-layer/FEATURE_MAP.md` |
| Platform gaps | LAYER_STATUS.md | `claude-product-cycle/platform-layer/LAYER_STATUS.md` |

### Step 2: Identify Gaps

From index files, find items marked ⚠️ or ❌:
- Design: Features missing specs, mockups, design-tokens
- Client: Missing services or repositories
- Feature: Missing screens or ViewModels
- Platform: Experimental or broken builds

### Step 3: Generate Plans

For each gap found:
1. Determine priority (P0/P1/P2)
2. List specific tasks
3. Estimate effort (S/M/L)
4. Provide execution commands
5. Add verification checklist

### Step 4: Output Format

- **No parameters**: Show all gaps + all plan summaries
- **With layer**: Show detailed plan for that layer
- **With feature**: Show detailed plan for that feature

---

## Priority Guidelines

| Priority | Criteria | Examples |
|----------|----------|----------|
| P0 | Critical - blocks other work | Missing feature module |
| P1 | High value - user-facing | Design specs, mockups, payment flows |
| P2 | Polish - nice to have | Animations, web fixes |

## Effort Guidelines

| Effort | Scope | Tasks |
|--------|-------|:-----:|
| S | Single file change | 1-3 |
| M | Multiple files, one area | 4-10 |
| L | Feature-wide or cross-cutting | 10+ |

---

## Output Rules

1. **Read index files only** - Use O(1) lookup
2. **Show all gaps** - No hidden information
3. **Show all commands** - For every gap
4. **Include effort estimates** - S/M/L
5. **Prioritize** - P0 → P1 → P2
6. **Provide verification** - Checklist for each plan
7. **NO interactive questions** - Show everything, user decides
8. **Save plan to file** - Persist for tracking (see below)

---

## Plan Persistence

When creating a detailed plan (with parameters), **save it to a file** for tracking:

### Save Location

```
plans/YYYY-MM-DD-[type]-[target].md
```

Examples:
- `/gap-planning design mockup` → `plans/2026-06-23-design-mockup.md`
- `/gap-planning testing auth` → `plans/2026-06-23-testing-auth.md`
- `/gap-planning feature payments` → `plans/2026-06-23-feature-payments.md`
- `/gap-planning platform web` → `plans/2026-06-23-platform-web.md`

### Plan File Format

```markdown
# Plan: [Target Description]

**Created**: YYYY-MM-DD
**Status**: 🔄 Active
**Command**: /gap-planning [args]
**Progress**: 0/N steps (0%)

---

## Overview

[Brief description of what this plan accomplishes]

---

## Steps

- [ ] **Step 1**: [Description]
  - Sub-task 1
  - Sub-task 2
  - Command: `[execution command]`
  - Files: `path/to/expected/files`

- [ ] **Step 2**: [Description]
  - Sub-task 1
  - Command: `[execution command]`

[... more steps ...]

---

## Verification

- [ ] All expected files exist
- [ ] Tests pass (if applicable)
- [ ] Index files updated

---

## Progress Log

| Date | Step | Action | Notes |
|------|:----:|--------|-------|
| YYYY-MM-DD | 0 | Created | Plan initialized |
```

### Update PLANS_INDEX.md

After creating a plan file, also update `plans/PLANS_INDEX.md`:

```markdown
## Active Plans

| # | Plan | Target | Progress | Current Step | Created |
|:-:|------|--------|:--------:|--------------|---------|
| 1 | design-mockup | Design mockups | [░░░░░░░░░░] 0% (0/29) | Step 1 | 2026-01-05 |
```

### Check Progress

After plan is saved, show:

```
✅ Plan saved to: plans/YYYY-MM-DD-[name].md

Track progress with: /gap-status [name]
```

---

## Plan Completion Triggers

After creating a plan, **TRIGGER user prompts** using the Prompt Layer.

### After Plan Created

**TRIGGER**: `plan-ready`
**Reference**: `prompt-layer/PROMPTS.md` → `plan-ready`
**Context Variables**: `{FEATURE}`, `{TOTAL_TASKS}`

**Route User Selection**:
| Selection | Action |
|-----------|--------|
| Start implementation | Execute first step of plan |
| Review plan | Show detailed task breakdown |
| Modify plan | Allow user to adjust tasks |
| Save for later | End without executing |

### After Layer Plan Created

**TRIGGER**: `plan-layer`
**Reference**: `prompt-layer/PROMPTS.md` → `plan-layer`
**Context Variables**: `{LAYER}`, `{TOTAL_TASKS}`

**Route User Selection**:
| Selection | Action |
|-----------|--------|
| Execute plan | Start implementing the layer |
| View tasks | Show detailed task list |
| Plan next layer | Continue planning |
| Stop here | Save plan, implement later |

### During Plan Execution

When executing a plan step-by-step:

**After Each Step:**
**TRIGGER**: `task-completion`
**Reference**: `prompt-layer/PROMPTS.md` → `task-completion`

**After All Steps Complete:**
**TRIGGER**: `task-completion:all`
**Reference**: `prompt-layer/PROMPTS.md` → `task-completion:all`

**On Build Success:**
**TRIGGER**: `build-success`
**Reference**: `prompt-layer/PROMPTS.md` → `build-success`

**On Build Failure:**
**TRIGGER**: `build-failure`
**Reference**: `prompt-layer/PROMPTS.md` → `build-failure`

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/gap-analysis` | Identify gaps (run first) |
| `/gap-planning` | Create implementation plans (this command) |
| `/gap-status` | Track plan progress |
| `/implement` | Execute implementation |
| `/verify` | Confirm completion |
