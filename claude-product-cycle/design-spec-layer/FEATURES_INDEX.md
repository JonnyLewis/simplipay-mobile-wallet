# Features Index — Design Spec Layer

**Last Updated**: 2026-06-23

---

## Summary

| Total Features | SPEC Complete | API Complete | Mockups Complete |
|:-:|:-:|:-:|:-:|
| 29 | 0 | 0 | 0 |

---

## Feature Status

| # | Feature | SPEC | API | STATUS | Mockups | Command |
|:-:|---------|:----:|:---:|:------:|:-------:|---------|
| 1 | auth | ❌ | ❌ | ❌ | ❌ | `/design auth` |
| 2 | home | ❌ | ❌ | ❌ | ❌ | `/design home` |
| 3 | accounts | ❌ | ❌ | ❌ | ❌ | `/design accounts` |
| 4 | history | ❌ | ❌ | ❌ | ❌ | `/design history` |
| 5 | receipt | ❌ | ❌ | ❌ | ❌ | `/design receipt` |
| 6 | faq | ❌ | ❌ | ❌ | ❌ | `/design faq` |
| 7 | make-transfer | ❌ | ❌ | ❌ | ❌ | `/design make-transfer` |
| 8 | send-money | ❌ | ❌ | ❌ | ❌ | `/design send-money` |
| 9 | transfer-intrabank | ❌ | ❌ | ❌ | ❌ | `/design transfer-intrabank` |
| 10 | transfer-interbank | ❌ | ❌ | ❌ | ❌ | `/design transfer-interbank` |
| 11 | notification | ❌ | ❌ | ❌ | ❌ | `/design notification` |
| 12 | editpassword | ❌ | ❌ | ❌ | ❌ | `/design editpassword` |
| 13 | kyc | ❌ | ❌ | ❌ | ❌ | `/design kyc` |
| 14 | savedcards | ❌ | ❌ | ❌ | ❌ | `/design savedcards` |
| 15 | invoices | ❌ | ❌ | ❌ | ❌ | `/design invoices` |
| 16 | settings | ❌ | ❌ | ❌ | ❌ | `/design settings` |
| 17 | profile | ❌ | ❌ | ❌ | ❌ | `/design profile` |
| 18 | finance | ❌ | ❌ | ❌ | ❌ | `/design finance` |
| 19 | merchants | ❌ | ❌ | ❌ | ❌ | `/design merchants` |
| 20 | beneficiary | ❌ | ❌ | ❌ | ❌ | `/design beneficiary` |
| 21 | standing-instruction | ❌ | ❌ | ❌ | ❌ | `/design standing-instruction` |
| 22 | payments | ❌ | ❌ | ❌ | ❌ | `/design payments` |
| 23 | upi-setup | ❌ | ❌ | ❌ | ❌ | `/design upi-setup` |
| 24 | qr | ❌ | ❌ | ❌ | ❌ | `/design qr` |
| 25 | autopay | ❌ | ❌ | ❌ | ❌ | `/design autopay` |
| 26 | mpay-qr | ❌ | ❌ | ❌ | ❌ | `/design mpay-qr` |
| 27 | mpay-qr-scan | ❌ | ❌ | ❌ | ❌ | `/design mpay-qr-scan` |
| 28 | fast-mpay | ❌ | ❌ | ❌ | ❌ | `/design fast-mpay` |
| 29 | passcode | ❌ | ❌ | ❌ | ❌ | `/design passcode` |

---

## Path Pattern

```
claude-product-cycle/design-spec-layer/features/[name]/
├── SPEC.md           # Requirements, user stories, state model
├── API.md            # Required endpoints, DTOs
├── STATUS.md         # Implementation status
├── MOCKUP.md         # ASCII design layout
└── mockups/
    ├── PROMPTS_STITCH.md    # Google Stitch prompts
    ├── PROMPTS_FIGMA.md     # Figma prompts
    ├── design-tokens.json   # Design tokens
    └── FIGMA_LINKS.md       # Figma URLs (user fills)
```

---

## Next Steps

Run `/gap-planning design spec` to create a plan for all 29 feature specs.
