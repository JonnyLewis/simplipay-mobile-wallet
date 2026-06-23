# /design - Feature Specification (O(1) Enhanced)

## Purpose
Create or update feature specifications (SPEC.md + API.md) that define what to build and how to build it.

---

## Command Variants

```
/design                         # Show feature list with status (O(1))
/design [Feature]               # Full spec review/create
/design [Feature] add [section] # Add specific section
/design [Feature] improve       # Suggest improvements
/design [Feature] mockup        # Generate Figma mockups for feature
/design mockup                  # Generate Figma mockups for all features
```

---

## O(1) Workflow

```
+-------------------------------------------------------------------------+
|                    /design WORKFLOW (O(1) ENHANCED)                      |
+-------------------------------------------------------------------------+
|                                                                          |
|  PHASE 0: O(1) CONTEXT LOADING (~300 lines total)                       |
|  +--> Read FEATURES_INDEX.md       --> Feature exists? SPEC/API status? |
|  +--> Read MOCKUPS_INDEX.md        --> Mockup status (4 file types)     |
|  +--> Read API_INDEX.md            --> All endpoints for reference      |
|  +--> O(1) path: features/[name]/  --> Direct file access               |
|                                                                          |
|  PHASE 1: FEATURE STATUS (From Index)                                   |
|  +--> Check if feature exists in FEATURES_INDEX                         |
|  +--> Get SPEC/API/STATUS/Mockups status from index                     |
|  +--> Determine: Create new vs Update existing                          |
|                                                                          |
|  PHASE 2: GATHER CONTEXT (O(1) Paths)                                   |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/SPEC.md (if exists)                       |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/API.md (if exists)                        |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/STATUS.md (if exists)                     |
|  +--> Lookup API endpoints from API_INDEX.md                            |
|                                                                          |
|  PHASE 3: ANALYZE & UPDATE                                              |
|  +--> Compare current spec vs requirements                              |
|  +--> Identify gaps, outdated sections                                  |
|  +--> Update/create spec files                                          |
|                                                                          |
|  PHASE 4: INDEX UPDATE (Mandatory)                                      |
|  +--> Update FEATURES_INDEX.md (if new feature)                         |
|  +--> Update STATUS.md (layer status)                                   |
|  +--> Update feature STATUS.md                                          |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## Phase 0: O(1) Context Loading

### Index Files to Read

| File | Purpose | Lines |
|------|---------|:-----:|
| `claude-product-cycle/design-spec-layer/FEATURES_INDEX.md` | All features + SPEC/API status | ~150 |
| `claude-product-cycle/design-spec-layer/MOCKUPS_INDEX.md` | Mockup completion matrix | ~180 |
| `claude-product-cycle/server-layer/API_INDEX.md` | All API endpoints | ~400 |

### O(1) Path Pattern

```
claude-product-cycle/design-spec-layer/features/[name]/SPEC.md       # Specification
claude-product-cycle/design-spec-layer/features/[name]/API.md        # API requirements
claude-product-cycle/design-spec-layer/features/[name]/STATUS.md     # Feature status
claude-product-cycle/design-spec-layer/features/[name]/MOCKUP.md     # ASCII mockup
claude-product-cycle/design-spec-layer/features/[name]/mockups/      # Generated mockup files
```

---

## If No Feature Name Provided

Read from FEATURES_INDEX.md and show:

```
+========================================================================+
|  DESIGN LAYER - FEATURE STATUS (O(1) Lookup)                           |
+========================================================================+

| # | Feature | SPEC | API | STATUS | Mockups | Command |
|:-:|---------|:----:|:---:|:------:|:-------:|---------|
| 1 | auth | [s] | [a] | [st] | [m] | /design auth |
| 2 | home | [s] | [a] | [st] | [m] | /design home |
| ... (all from FEATURES_INDEX.md)

Legend: [s]=SPEC [a]=API [st]=STATUS [m]=Mockups

**Design Progress**: {complete}/{total} features ({percentage}%)

+------------------------------------------------------------------------+
|  QUICK ACTIONS                                                          |
+------------------------------------------------------------------------+
| Create/Update Spec | /design [feature]                                  |
| Generate Mockups   | /design [feature] mockup                           |
| All Mockups        | /design mockup                                     |
| Improve Feature    | /design [feature] improve                          |
+------------------------------------------------------------------------+
```

---

## Mockup Sub-Command (O(1) Enhanced)

### `/design [Feature] mockup`

```
+-------------------------------------------------------------------------+
|                /design [Feature] mockup WORKFLOW                         |
+-------------------------------------------------------------------------+
|                                                                          |
|  PHASE 0: O(1) STATUS CHECK                                             |
|  +--> Read MOCKUPS_INDEX.md                                             |
|  +--> Check feature row: FIGMA | PROMPTS_FIGMA | PROMPTS_STITCH | tokens|
|  +--> Identify: What exists? What's missing?                            |
|                                                                          |
|  PHASE 1: MCP & TOOL CHECK                                              |
|  +--> Check MCP: claude mcp list                                        |
|  +--> If stitch-ai configured: Use Google Stitch                        |
|  +--> If figma configured: Use Figma MCP                                |
|  +--> Otherwise: Ask user to select tool                                |
|                                                                          |
|  PHASE 2: READ MOCKUP.md                                                |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/MOCKUP.md (ASCII design)                  |
|  +--> Parse screen layouts, components, colors                          |
|  +--> Identify all screens and UI elements                              |
|                                                                          |
|  PHASE 3: GENERATE OUTPUTS                                              |
|  +--> If missing: Generate PROMPTS_FIGMA.md                             |
|  +--> If missing: Generate PROMPTS_STITCH.md                            |
|  +--> If missing: Generate design-tokens.json                           |
|  +--> Skip files that already exist (from MOCKUPS_INDEX)                |
|                                                                          |
|  PHASE 4: INDEX UPDATE                                                  |
|  +--> Update MOCKUPS_INDEX.md with new status                           |
|  +--> Update FEATURES_INDEX.md Mockups column                           |
|                                                                          |
+-------------------------------------------------------------------------+
```

### `/design mockup` (All Features)

Uses O(1) lookup from MOCKUPS_INDEX.md to identify all gaps:

```
+-------------------------------------------------------------------------+
|  MOCKUP GENERATION STATUS (from MOCKUPS_INDEX.md)                        |
+-------------------------------------------------------------------------+

| Feature | FIGMA | PROMPTS_F | PROMPTS_S | Tokens | Status |
|---------|:-----:|:---------:|:---------:|:------:|--------|
| auth | [x] | [x] | [x] | [x] | Complete |
| home | [ ] | [x] | [x] | [x] | Need FIGMA |
| accounts | [ ] | [x] | [x] | [ ] | Need FIGMA, tokens |
| ... (from MOCKUPS_INDEX)

**Summary**:
- Complete: {n} features
- Need FIGMA_LINKS: {n} features
- Need Prompts: {n} features
- Need Tokens: {n} features

**Next Step**: Generate missing files for [first-incomplete-feature]
```

---

## Tool Selection

### Check MCP First

```bash
claude mcp list
```

### AI Design Tools

| Tool | MCP | Best For | Setup |
|------|:---:|----------|-------|
| **Google Stitch** | YES | Material Design 3, Android/KMP | `claude mcp add stitch-ai -- npx -y stitch-ai-mcp` |
| **Figma** | YES | Team collaboration | `claude mcp add figma -- npx -y figma-mcp --token TOKEN` |
| Uizard | NO | Quick prototypes | Manual (web) |
| Visily | NO | Component-focused | Manual (web) |

**Recommended**: Google Stitch (MD3 native, has MCP)

### Tool Selection Prompt (If Not Configured)

```
Select AI Design Tool:

1. Google Stitch (Recommended) - Material Design 3 native
   MCP: claude mcp add stitch-ai -- npx -y stitch-ai-mcp
   Web: https://stitch.withgoogle.com/

2. Figma + AI - Team collaboration
   MCP: claude mcp add figma -- npx -y figma-mcp --token TOKEN

3. Uizard - Quick prototypes (no MCP)
   Web: https://uizard.io/

4. Visily - Component-focused (no MCP)
   Web: https://www.visily.ai/

Which tool? (1-4, default: 1)
```

---

## Output Files Structure

```
claude-product-cycle/design-spec-layer/features/[Feature]/mockups/
+-- PROMPTS_FIGMA.md           # Figma-specific prompts
+-- PROMPTS_STITCH.md          # Google Stitch prompts
+-- design-tokens.json         # Structured design tokens
+-- FIGMA_LINKS.md             # Figma URLs (user fills after export)
```

---

## PROMPTS_STITCH.md Format

```markdown
# [Feature] - Google Stitch Prompts

> **Generated from**: claude-product-cycle/design-spec-layer/features/[feature]/MOCKUP.md
> **Generated on**: [DATE]
> **AI Tool**: Google Stitch

## Screen 1: [Screen Name]

### Google Stitch Prompt

Create a mobile [screen type] screen with Material Design 3:

**App Context:**
MifosX Mobile Wallet - Mobile payments and wallet platform supporting transfers, QR payments, autopay, and invoices.

**Screen Size:** 393 x 852 pixels (iPhone 14 Pro equivalent)

**Header Section:**
- [Component details from MOCKUP.md]

**Main Content:**
- [Section details from MOCKUP.md]

**Style Guidelines:**
- Primary Gradient: #667EEA -> #764BA2
- Surface: #FFFBFE
- Typography: Inter font family
- Spacing: 16px standard padding
```

---

## Main Workflow: `/design [Feature]`

```
+-------------------------------------------------------------------------+
|                    /design [Feature] WORKFLOW                            |
+-------------------------------------------------------------------------+
|                                                                          |
|  PHASE 0: O(1) CONTEXT LOADING                                          |
|  +--> Read FEATURES_INDEX.md --> Feature exists? Status?                |
|  +--> Read MOCKUPS_INDEX.md --> Mockup status                           |
|  +--> Read API_INDEX.md --> Related endpoints                           |
|                                                                          |
|  PHASE 1: DETERMINE ACTION                                              |
|  +--> If feature NOT in index: Create new feature                       |
|  +--> If SPEC missing: Create SPEC.md                                   |
|  +--> If API missing: Create API.md                                     |
|  +--> If exists: Update/improve existing                                |
|                                                                          |
|  PHASE 2: GATHER CONTEXT (O(1) Paths)                                   |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/SPEC.md                                   |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/API.md                                    |
|  +--> Read claude-product-cycle/design-spec-layer/features/[feature]/STATUS.md                                 |
|  +--> Lookup endpoints from API_INDEX.md                                |
|  +--> Read actual code: feature/[feature]/ (if exists)                  |
|                                                                          |
|  PHASE 3: ANALYZE                                                       |
|  +--> Compare current spec vs implementation                            |
|  +--> Identify gaps, outdated sections                                  |
|  +--> Check API availability in API_INDEX                               |
|  +--> Report findings to user                                           |
|                                                                          |
|  PHASE 4: UPDATE FILES                                                  |
|  +--> Update/create SPEC.md with ASCII mockups                          |
|  +--> Update/create API.md with endpoints                               |
|  +--> Update feature STATUS.md                                          |
|                                                                          |
|  PHASE 5: INDEX UPDATE (Mandatory)                                      |
|  +--> Update FEATURES_INDEX.md (status columns)                         |
|  +--> Update design-spec-layer/STATUS.md                                |
|                                                                          |
|  PHASE 6: OUTPUT SUMMARY                                                |
|  +--> Implementation requirements                                       |
|  +--> Next command suggestion                                           |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## SPEC.md Template

```markdown
# [Feature Name] - Feature Specification

> **Purpose**: [One-line description]
> **User Value**: [Why users need this]
> **Last Updated**: [Date]

---

## 1. Overview

### 1.1 Feature Summary
[2-3 sentences describing the feature]

### 1.2 User Stories
- As a user, I want to [action] so that [benefit]

---

## 2. Screen Layout

### 2.1 ASCII Mockup

+-------------------------------------------+
|  <- Back          [Title]            :    |  <- TopBar
+-------------------------------------------+
|                                           |
|  +-----------------------------------+    |
|  |     Section 1                     |    |
|  +-----------------------------------+    |
|                                           |
+-------------------------------------------+

### 2.2 Sections Table

| # | Section | Description | API | Priority |
|---|---------|-------------|-----|----------|
| 1 | [Name] | [What it shows] | [Endpoint] | P0 |

---

## 3. User Interactions

| Action | Trigger | Result | API Call |
|--------|---------|--------|----------|
| Tap item | Click | Navigate | - |
| Pull refresh | Swipe down | Reload data | [Endpoint] |

---

## 4. State Model

@Serializable
data class [Feature]State(
    val viewState: ViewState = ViewState.Loading,
    val data: List<Item> = emptyList(),
    val dialogState: DialogState? = null,
)

sealed interface ViewState {
    data object Loading : ViewState
    data object Content : ViewState
    data class Error(val message: String) : ViewState
}

sealed interface [Feature]Event { ... }
sealed interface [Feature]Action { ... }

---

## 5. API Requirements

| Endpoint | Method | Purpose | Status |
|----------|--------|---------|--------|
| /self/[path] | GET | [Description] | Exists |

---

## 6. Edge Cases & Error Handling

| Scenario | Behavior | UI Feedback |
|----------|----------|-------------|
| No internet | Show cached | Toast |
| Empty results | Show empty state | Illustration |
| API error | Retry logic | Snackbar |

---

## Changelog

| Date | Change |
|------|--------|
| [date] | Initial spec |
```

---

## API.md Template

```markdown
# [Feature Name] - API Reference

## Endpoints Required

### [Endpoint Name]

**Endpoint**: `GET /self/[path]`

**Description**: [What this endpoint does]

**Request**:
Headers:
  Authorization: Basic {token}
  Fineract-Platform-TenantId: {tenant}

**Response**:
{
    "field": "value"
}

**Kotlin DTO**:
@Serializable
data class [Name]Dto(
    @SerialName("field") val field: String,
)

**Status**: Implemented / Missing

---

## API Summary

| Endpoint | Service | Repository | Status |
|----------|---------|------------|--------|
| /self/[path] | [Name]Service | [Name]Repository | Done |
```

---

## Output Template

After completing design, output:

```
+=========================================================================+
|            IMPLEMENTATION REQUIREMENTS                                   |
|            Ready for /implement                                          |
+=========================================================================+
|                                                                          |
|  FEATURE: [Feature Name]                                                |
|  SPEC UPDATED: claude-product-cycle/design-spec-layer/features/[feature]/SPEC.md                               |
|                                                                          |
|  ================================================================       |
|                                                                          |
|  CLIENT WORK NEEDED:                                                    |
|  [ ] Network: [DTO/Service changes]                                     |
|  [ ] Data: [Repository changes]                                         |
|                                                                          |
|  FEATURE WORK NEEDED:                                                   |
|  [ ] ViewModel: [changes]                                               |
|  [ ] Screen: [changes]                                                  |
|  [ ] Components: [new components]                                       |
|                                                                          |
|  ================================================================       |
|                                                                          |
|  INDEXES UPDATED:                                                       |
|  [x] FEATURES_INDEX.md - Status updated                                 |
|  [x] design-spec-layer/STATUS.md - Layer status                         |
|  [x] claude-product-cycle/design-spec-layer/features/[feature]/STATUS.md - Feature status                      |
|                                                                          |
|  ================================================================       |
|                                                                          |
|  NEXT STEP:                                                             |
|  Run:  /implement [Feature]                                             |
|                                                                          |
+=========================================================================+
```

---

## Feature Reference (From FEATURES_INDEX.md)

| # | Feature | Design Dir | Feature Module |
|:-:|---------|------------|----------------|
| 1 | auth | claude-product-cycle/design-spec-layer/features/auth/ | feature/auth/ |
| 2 | home | claude-product-cycle/design-spec-layer/features/home/ | feature/home/ |
| 3 | accounts | claude-product-cycle/design-spec-layer/features/accounts/ | feature/accounts/ |
| 4 | history | claude-product-cycle/design-spec-layer/features/history/ | feature/history/ |
| 5 | receipt | claude-product-cycle/design-spec-layer/features/receipt/ | feature/receipt/ |
| 6 | faq | claude-product-cycle/design-spec-layer/features/faq/ | feature/faq/ |
| 7 | make-transfer | claude-product-cycle/design-spec-layer/features/make-transfer/ | feature/make-transfer/ |
| 8 | send-money | claude-product-cycle/design-spec-layer/features/send-money/ | feature/send-money/ |
| 9 | transfer-intrabank | claude-product-cycle/design-spec-layer/features/transfer-intrabank/ | feature/transfer-intrabank/ |
| 10 | transfer-interbank | claude-product-cycle/design-spec-layer/features/transfer-interbank/ | feature/transfer-interbank/ |
| 11 | notification | claude-product-cycle/design-spec-layer/features/notification/ | feature/notification/ |
| 12 | editpassword | claude-product-cycle/design-spec-layer/features/editpassword/ | feature/editpassword/ |
| 13 | kyc | claude-product-cycle/design-spec-layer/features/kyc/ | feature/kyc/ |
| 14 | savedcards | claude-product-cycle/design-spec-layer/features/savedcards/ | feature/savedcards/ |
| 15 | invoices | claude-product-cycle/design-spec-layer/features/invoices/ | feature/invoices/ |
| 16 | settings | claude-product-cycle/design-spec-layer/features/settings/ | feature/settings/ |
| 17 | profile | claude-product-cycle/design-spec-layer/features/profile/ | feature/profile/ |
| 18 | finance | claude-product-cycle/design-spec-layer/features/finance/ | feature/finance/ |
| 19 | merchants | claude-product-cycle/design-spec-layer/features/merchants/ | feature/merchants/ |
| 20 | beneficiary | claude-product-cycle/design-spec-layer/features/beneficiary/ | feature/beneficiary/ |
| 21 | standing-instruction | claude-product-cycle/design-spec-layer/features/standing-instruction/ | feature/standing-instruction/ |
| 22 | payments | claude-product-cycle/design-spec-layer/features/payments/ | feature/payments/ |
| 23 | upi-setup | claude-product-cycle/design-spec-layer/features/upi-setup/ | feature/upi-setup/ |
| 24 | qr | claude-product-cycle/design-spec-layer/features/qr/ | feature/qr/ |
| 25 | autopay | claude-product-cycle/design-spec-layer/features/autopay/ | feature/autopay/ |
| 26 | mpay-qr | claude-product-cycle/design-spec-layer/features/mpay-qr/ | feature/mpay-qr/ |
| 27 | mpay-qr-scan | claude-product-cycle/design-spec-layer/features/mpay-qr-scan/ | feature/mpay-qr-scan/ |
| 28 | fast-mpay | claude-product-cycle/design-spec-layer/features/fast-mpay/ | feature/fast-mpay/ |
| 29 | passcode | claude-product-cycle/design-spec-layer/features/passcode/ | feature/passcode/ |

---

## Error Handling

### Feature Not Found

```
+-------------------------------------------------------------------------+
|  ERROR: Feature '[name]' not found                                       |
+-------------------------------------------------------------------------+
|                                                                          |
|  The feature '[name]' does not exist in FEATURES_INDEX.md               |
|                                                                          |
|  OPTIONS:                                                               |
|  1. Create new feature: /design [name]                                  |
|  2. Check available features: /design                                   |
|  3. Similar features: [suggestions based on name]                       |
|                                                                          |
+-------------------------------------------------------------------------+
```

### Invalid Sub-command

```
+-------------------------------------------------------------------------+
|  ERROR: Invalid sub-command '[sub]'                                      |
+-------------------------------------------------------------------------+
|                                                                          |
|  Valid sub-commands:                                                    |
|  - mockup    : Generate mockup prompts                                  |
|  - improve   : Suggest improvements                                     |
|  - add [x]   : Add specific section                                     |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## Model Recommendation

**This command is optimized for Opus** for complex architectural decisions and comprehensive specification writing.

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/gap-analysis design` | See design layer gaps |
| `/gap-analysis design mockup` | See mockup gaps specifically |
| `/implement [feature]` | Implement the designed feature |
| `/verify [feature]` | Verify implementation vs spec |

---

## Key Files

```
claude-product-cycle/design-spec-layer/
+-- FEATURES_INDEX.md             # O(1) feature lookup
+-- MOCKUPS_INDEX.md              # O(1) mockup status
+-- STATUS.md                     # Layer status
+-- claude-product-cycle/design-spec-layer/features/[feature]/
    +-- SPEC.md                   # What to build (UI, flows)
    +-- API.md                    # APIs needed
    +-- STATUS.md                 # Feature implementation status
    +-- MOCKUP.md                 # ASCII mockup
    +-- mockups/                  # Generated mockup files
```
