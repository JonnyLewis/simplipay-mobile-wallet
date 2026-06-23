# Mockups Index — Design Spec Layer

**Last Updated**: 2026-06-23

---

## Mockup Summary

| Total | FIGMA_LINKS | PROMPTS_STITCH | PROMPTS_FIGMA | Tokens |
|:-----:|:-----------:|:--------------:|:-------------:|:------:|
| 29 | 0 | 0 | 0 | 0 |

---

## Feature Mockup Status

| # | Feature | FIGMA | PROMPTS_STITCH | PROMPTS_FIGMA | Tokens | Command |
|:-:|---------|:-----:|:--------------:|:-------------:|:------:|---------|
| 1 | auth | ❌ | ❌ | ❌ | ❌ | `/design auth mockup` |
| 2 | home | ❌ | ❌ | ❌ | ❌ | `/design home mockup` |
| 3 | accounts | ❌ | ❌ | ❌ | ❌ | `/design accounts mockup` |
| 4 | history | ❌ | ❌ | ❌ | ❌ | `/design history mockup` |
| 5 | receipt | ❌ | ❌ | ❌ | ❌ | `/design receipt mockup` |
| 6 | faq | ❌ | ❌ | ❌ | ❌ | `/design faq mockup` |
| 7 | make-transfer | ❌ | ❌ | ❌ | ❌ | `/design make-transfer mockup` |
| 8 | send-money | ❌ | ❌ | ❌ | ❌ | `/design send-money mockup` |
| 9 | transfer-intrabank | ❌ | ❌ | ❌ | ❌ | `/design transfer-intrabank mockup` |
| 10 | transfer-interbank | ❌ | ❌ | ❌ | ❌ | `/design transfer-interbank mockup` |
| 11 | notification | ❌ | ❌ | ❌ | ❌ | `/design notification mockup` |
| 12 | editpassword | ❌ | ❌ | ❌ | ❌ | `/design editpassword mockup` |
| 13 | kyc | ❌ | ❌ | ❌ | ❌ | `/design kyc mockup` |
| 14 | savedcards | ❌ | ❌ | ❌ | ❌ | `/design savedcards mockup` |
| 15 | invoices | ❌ | ❌ | ❌ | ❌ | `/design invoices mockup` |
| 16 | settings | ❌ | ❌ | ❌ | ❌ | `/design settings mockup` |
| 17 | profile | ❌ | ❌ | ❌ | ❌ | `/design profile mockup` |
| 18 | finance | ❌ | ❌ | ❌ | ❌ | `/design finance mockup` |
| 19 | merchants | ❌ | ❌ | ❌ | ❌ | `/design merchants mockup` |
| 20 | beneficiary | ❌ | ❌ | ❌ | ❌ | `/design beneficiary mockup` |
| 21 | standing-instruction | ❌ | ❌ | ❌ | ❌ | `/design standing-instruction mockup` |
| 22 | payments | ❌ | ❌ | ❌ | ❌ | `/design payments mockup` |
| 23 | upi-setup | ❌ | ❌ | ❌ | ❌ | `/design upi-setup mockup` |
| 24 | qr | ❌ | ❌ | ❌ | ❌ | `/design qr mockup` |
| 25 | autopay | ❌ | ❌ | ❌ | ❌ | `/design autopay mockup` |
| 26 | mpay-qr | ❌ | ❌ | ❌ | ❌ | `/design mpay-qr mockup` |
| 27 | mpay-qr-scan | ❌ | ❌ | ❌ | ❌ | `/design mpay-qr-scan mockup` |
| 28 | fast-mpay | ❌ | ❌ | ❌ | ❌ | `/design fast-mpay mockup` |
| 29 | passcode | ❌ | ❌ | ❌ | ❌ | `/design passcode mockup` |

---

## Mockup File Structure

```
claude-product-cycle/design-spec-layer/features/[name]/mockups/
├── PROMPTS_STITCH.md     # Google Stitch prompts (Material Design 3)
├── PROMPTS_FIGMA.md      # Figma-specific prompts
├── design-tokens.json    # Structured design tokens
└── FIGMA_LINKS.md        # Figma URLs (filled by user after export)
```

---

## Recommended Tool

**Google Stitch** — Material Design 3 native, has MCP integration.

```bash
claude mcp add stitch-ai -- npx -y stitch-ai-mcp
```

---

**Next**: Run `/gap-planning design mockup` to create a batch mockup generation plan.
