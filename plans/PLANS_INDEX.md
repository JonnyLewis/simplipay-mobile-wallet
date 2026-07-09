# Plans Index

**Last Updated**: 2026-06-27

---

## Active Plans

| Plan | Created | Status | Progress | File |
|------|---------|:------:|:--------:|------|
| Platform parity — iOS redesign → Android & Desktop | 2026-06-25 | planning | 0% | [2026-06-25-platform-ios-parity-android-desktop](2026-06-25-platform-ios-parity-android-desktop.md) |
| Proximity Payment — implementation (feature/proximity) | 2026-06-27 | planning | 0% | [2026-06-27-feature-proximity-payment-implementation](2026-06-27-feature-proximity-payment-implementation.md) |

---

## Backlog / Future Specs

| Item | Created | Notes | File |
|------|---------|-------|------|
| Proximity Payment **SDK** (commercialization — white-label, master key, bank devices + kiosks) | 2026-06-27 | Future spec; depends on proximity v6 shipping first | [2026-06-27-backlog-proximity-sdk-commercialization](2026-06-27-backlog-proximity-sdk-commercialization.md) |

---

## Completed Plans

| Plan | Completed | Notes |
|------|-----------|-------|
| [2026-06-23-claude-code-setup-port](2026-06-23-claude-code-setup-port.md) | 2026-06-23 | Ported `.claude/` command set + `claude-product-cycle/` indexes + `prompt-layer/` from Simpli-mobile, adapted to this 29-feature wallet |

---

## Plan File Naming Convention

```
plans/YYYY-MM-DD-{type}-{description}.md

Types: design | client | feature | platform | testing | refactor

Examples:
  plans/2026-06-23-design-auth-spec.md
  plans/2026-06-23-feature-payments-v2.md
  plans/2026-06-23-testing-core-flows.md
```

---

## Creating a Plan

Run `/gap-planning [layer] [feature]` to generate a plan file automatically.

Or create manually and add to this index.
