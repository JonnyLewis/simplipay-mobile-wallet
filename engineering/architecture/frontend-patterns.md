# Frontend patterns — {{PRODUCT_NAME}}

Concrete UI patterns for the frontend stack ({{STACK_FRONTEND}}). Implementation agents read this in Phase 4 for any frontend task. *(Delete this file if the product has no frontend.)*

> **How to use this file:** Fill each section with a real snippet once your frontend stack is locked. Until then, the headings are the checklist every feature follows. The SimpliSalary reference (React / TypeScript) is a worked example.

---

## Pattern catalogue (fill each with a real snippet)

### 1. Feature folder structure
Each feature is self-contained (`components/`, `hooks/`, `api/`, `pages/`, `index.ts` barrel). Features import each other only through the barrel.

### 2. Generated API client
The client is generated from the interface contract (`api/`), not hand-written. No raw `fetch`/HTTP calls scattered in components.

```
{{language}}
// feature/api/ wraps the generated client; components call the wrapper
```

### 3. Data fetching & caching
A single data-layer convention (e.g. query hooks) with consistent loading/error/empty states.

### 4. Forms & validation
A schema validates input; the form binds to it; errors surface field-level. No ad-hoc validation in handlers.

### 5. Money / exact-value display
A single formatter for money/quantities; no inline arithmetic on display values; format from exact stored units.

```
{{language}}
// formatMoney(minorUnits, currency) — the only place money becomes a string
```

### 6. State / status badges
Domain state strings render through typed badge components, not raw strings — so a renamed state is a compile error, not a silent UI bug.

### 7. Error & empty states
Every async view handles loading, error, and empty explicitly.

### 8. Types
Strict typing on; no `any`/unchecked casts in component code; domain types derived from the contract where possible.

---

## Anti-patterns (blocking in review)

- Raw HTTP calls instead of the generated client.
- Inline money/number formatting.
- Raw state strings in the UI instead of typed badges.
- Cross-feature imports that bypass the barrel.
- `any` / unsafe casts in components.
