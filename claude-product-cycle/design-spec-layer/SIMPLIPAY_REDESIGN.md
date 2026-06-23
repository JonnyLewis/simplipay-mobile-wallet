# SimpliPay "Platinum Ivory" Redesign — Master Spec & Implementation Plan

> **Source of truth**: `Splash screen redesign-handoff.zip` → `project/SimpliPay All Screens.dc.html`
> (16 frames: onboarding, KYC, wallet). Extracted directly from the design HTML/CSS/JS — not screenshots.
> **Generated**: 2026-06-23
> **Target codebase**: KMP mobile wallet (`core/designsystem` + feature modules, driven by `KptTheme`).

This is a **full rebrand**, not a tweak. The current app is blue Material-3 (`#0673BA`) on cool-white
(`#F8F9FF`) using the **Outfit** font. The target is **Platinum Ivory + Jade** using **Plus Jakarta Sans**
(display/body) + **Geist Mono** (all numbers, money, IDs, eyebrow labels).

---

## 0. Two signature elements (the user's primary concern)

### 0.1 The Rosette (spirograph) — `rosette(rx, ry, n, op)`

Exact definition from the design (`SimpliPay All Screens.dc.html:563`):

```
svg: width = height = rx * 2.3 ; viewBox "0 0 (rx*2.3) (rx*2.3)"
draw n ellipses, for i in 0..n-1:
    cx = rx * 1.15            (center)
    cy = rx * 1.15
    rx = rx                   (ellipse x-radius)
    ry = ry                   (ellipse y-radius)
    transform = rotate( i * (360 / n) , cx , cy )
    fill   = none
    stroke = #9C9684
    stroke-width = 0.5
    opacity = op
```

It is **n overlapping thin ellipses, each rotated by an equal slice of 360°, all sharing one center** —
producing the fine guilloché/spirograph "fan". Stroke is always hairline (`0.5px`), warm grey `#9C9684`.

**Every usage (parameters matter — replicate exactly):**

| Where | Call | Position / motion |
|-------|------|-------------------|
| Splash/Loading | `rosette(120, 39, 35, 0.85)` | centered; **spins** `90s linear infinite` (`spSpin`), behind wordmark; a `312px` radial jade glow circle sits under it |
| Balance card (Home) | `rosette(86, 30, 40, 0.30)` | top-right, offset `right:-70px; top:-46px` |
| Balance card (Home) | `rosette(70, 22, 34, 0.22)` | bottom-left, offset `left:-78px; bottom:-86px` |
| Buy "Pay from" card | `rosette(60, 20, 34, 0.26)` | top-right, offset `right:-46px; top:-40px` |
| KYC ID card | `rosette(70, 23, 34, 0.26)` | top-right, offset `right:-60px; top:-44px` |

Cards clip the rosette with `overflow:hidden` so only a corner fan shows.

**Compose implementation**: a `Canvas` that loops `n` times, each iteration `rotate(degrees = i*360f/n, pivot = center)`
then `drawOval(color = Color(0xFF9C9684), topLeft, size, style = Stroke(0.5.dp.toPx()), alpha = op)`.
Wrap as `Rosette(rx, ry, n, op, modifier)`. For the splash, drive rotation with
`rememberInfiniteTransition` (`tween(90_000, easing = LinearEasing)`, 0°→360°).

### 0.2 The Particle field (constellation) — `setupField` / `animateField`

From `SimpliPay All Screens.dc.html:480–510`. Used on the **Landing** screen only (canvas `zIndex:1`, content `zIndex:2`).

```
density   = 1.35
count n   = round( W * H / 16000 * density )        // W,H = wrapper px size
per particle:
    x  = rand(0..W) , y = rand(0..H)
    vx = rand()*0.288 - 0.144     // velocity range  [-0.144 , +0.144] px/frame
    vy = rand()*0.288 - 0.144
    size = rand()*1.3 + 0.5       // radius [0.5 , 1.8]
each frame:
    if x<0 or x>W: vx = -vx       (bounce)
    if y<0 or y>H: vy = -vy
    x += vx ; y += vy
    fill circle radius=size, color = rgba(15,138,123, 0.30)         // jade @ 30%
links: for every pair (a,b):
    d2 = dx*dx + dy*dy ; max = 120 ; max2 = max*max
    if d2 < max2:
        o = (1 - d2/max2) * 0.28
        stroke line a→b, color = rgba(15,138,123, o), width = 1
```

So: slow-drifting jade dots that bounce off edges, with **lines drawn between any two within 120px**,
the line fading as they separate. This is the faint "network" visible at the left edge of the login/landing
screenshots.

**Compose implementation**: `Canvas` + a frame loop via `withInfiniteAnimationFrameMillis` (or
`LaunchedEffect` + `withFrameNanos`). Hold particle state in a `remember { mutableStateListOf / Array }`,
seeded once from `size`. Each frame: integrate positions, `drawCircle`, then O(n²) pair loop with
`drawLine`. Particle count for a ~344×748 frame ≈ `round(344*748/16000*1.35) ≈ 22` particles → the n²
loop is ~230 pairs/frame, cheap. Jade = `Color(0xFF0F8A7B)`.

### 0.3 Supporting motion (CSS keyframes → Compose)

| Name | Spec | Use |
|------|------|-----|
| `spSpin` | rotate 0→360, `90s linear infinite` | splash rosette |
| `ldDot` | translateY 0→-4px + opacity 1→0.4, `1.2s ease-in-out`, 3 dots staggered `0/0.2/0.4s` | splash loading dots |
| `kycScan` | translateY 4→150px | KYC scan line |
| `kycFill` | stroke-dashoffset 414→70 / ring fill | KYC face capture ring |
| `kycPulse` | opacity .35↔1, `1.1s` | KYC "matching" |
| `kycPop` | scale .5→1.08→1 + fade, `0.5s cubic-bezier(.2,.8,.2,1)` | KYC success check |

---

## 1. Design tokens (extracted)

### 1.1 Colors

| Token | Hex | Notes |
|-------|-----|-------|
| Board bg | `#EDEAE2` | outermost |
| **Screen bg** | `#F6F4EF` | `t.bg` — every screen |
| Surface | `#FFFFFF` | cards, fields |
| Border | `#DDD8CE` | `t.border` |
| Ink | `#101418` | primary text |
| Sub | `#6F7782` | secondary text |
| Faint | `#A39E93` | tertiary / placeholder |
| **Jade (accent)** | `#0F8A7B` | `JADE` — buttons, active, links, period in wordmark |
| Credit green | `#0E9466` | incoming amounts |
| Debit | `#4A5560` | outgoing amounts |
| Line | `#ECE8DF` | row dividers |
| Jade tint | `rgba(15,138,123,0.10)` | icon chip bg |
| Credit tint | `rgba(14,148,102,0.10)` | credit icon bg |
| Ivory gradient | `linear-gradient(155deg,#FBFAF6 0%,#F0EEE6 60%,#E6E3D8 100%)` | balance/buy/ID cards, PIN keys |
| Ivory border | `#E0DCCE` | on ivory cards |
| Card ink | `#16140E` | text on ivory |
| Card label | `#7A7464` | labels on ivory |
| Verified gold | `#C8B27A` | "VERIFIED" / tier badges |
| Rosette stroke | `#9C9684` | hairline |
| Danger | `#CE6A5E` | logout / destructive |
| Dark inset (camera) | `#1C1A16` | KYC scan wells |

> Map jade → `colorScheme.primary`; screen bg → `background`/`surface` base; ink → `onBackground`/`onSurface`;
> sub → `onSurfaceVariant`; border → `outlineVariant`. Add custom tokens for ivory-gradient, credit, debit,
> card-ink/label, verified-gold, rosette-stroke (these have no Material slot).

### 1.2 Typography

- **Plus Jakarta Sans** weights 400/500/600/700/800 — display, headings, body, buttons.
  Headings weight **800**, letter-spacing **-0.025em to -0.03em**, line-height ~1.04.
- **Geist Mono** weights 400/500/600/700 — **all** money (`R 12,480.50`), card numbers (`•••• 4567`),
  IDs, dates/times where numeric, and eyebrow/section labels (`01 · SPLASH`) with letter-spacing `0.12–0.2em`.
- Wordmark `simplipay.` — weight 800, `-0.03em`, jade period. Sizes: 26px (login), 34px (splash), 48px (landing).

### 1.3 Shape / radius / elevation

| Element | Radius |
|---------|--------|
| Phone frame / inner | 46 / 36px (prototype chrome only — not needed in-app) |
| Balance card | 22px |
| Generic card / tile | 16–18px |
| Text field | 15px |
| Primary button | 16px, height 52px |
| Bottom-nav pill | 22px, height 60px |
| PIN key | 18px, height 60px |
| PIN dot | 14px circle (6 dots) |

Card shadows are soft and warm: e.g. balance `0 18px 40px rgba(40,36,24,0.30)` + `inset 0 1px 0 rgba(255,255,255,0.7)`;
button `0 8px 22px rgba(15,138,123,0.34)` (jade glow).

---

## 2. Screen-by-screen plan (flow order)

Flow requested: **Splash → Landing → Login → PIN → Wallet (Home) → Buy / History / Profile**.

| # | Design frame | App target | Status today | Work |
|:-:|--------------|-----------|--------------|------|
| 01 | Splash / Loading | **NEW** screen | none (nav skips straight in) | Build splash with spinning rosette + glow + wordmark + 3 `ldDot` dots; show during session/auth bootstrap, then route to landing/login/passcode |
| 02 | Landing | **NEW** screen | none | Particle field + big wordmark + tagline + "Create account" (jade) / "I already have an account" |
| 03 | Login | `feature/auth/.../LoginScreen.kt` | plain Material form | Re-skin: wordmark, "Welcome back", ivory floating-label fields, jade button w/ glow, OR divider, Face ID button |
| 04 | Enter PIN | `feature/passcode` (`MifosPasscode.kt` → library `PasscodeScreen`) | configurable but blue/circle | Re-theme via `PasscodeAppearanceConfig`/`DotConfig`/`KeyConfig`: ivory rounded keys, jade dots, avatar+greeting header, faceid/del icon keys, `shouldShuffleKeys=false` |
| 13 | Home | `feature/home/HomeScreen.kt` (1035 lines) | existing wallet home | Rebuild card stack: ivory balance card (mono balance + 2 rosettes + savings chip), Send/Request/Buy/Scan row, promo card, Activity list (txnRow) |
| 15 | History | `feature/history` | existing | Apply tokens: In/Out summary cards, filter chips, day-grouped txn rows w/ mono amounts |
| 16 | Profile | `feature/profile` | existing | Avatar header w/ VERIFIED badge, grouped setting rows, danger logout |
| 14 | Buy / VAS | `feature/payments`? or **new** | no 1:1 feature | **Scope flag** — airtime/data/electricity grid is net-new; not just re-skin |
| 05–12 | KYC (8 frames) | `feature/kyc` | **disabled** in v2 shell | Out of current flow; spec retained for when KYC is re-enabled |

**Bottom nav** (design `navBar`): Home · Buy · **[center jade circular Scan FAB w/ barcode icon]** · History · Profile —
60px white pill, 22px radius, active item jade. Current app tabs are HOME/PAYMENTS/FINANCE/HISTORY → reconcile
labels/order + insert center FAB.

---

## 3. Implementation order (layered)

1. **Foundation — `core/designsystem`** (unblocks everything):
   - Add font files: Plus Jakarta Sans (5 weights) + Geist Mono (4 weights) to
     `core/designsystem/src/commonMain/composeResources/font/`; rewrite `Type.kt` (two families — a
     `displayFontFamily` and a `monoFontFamily` accessor; apply Mono to numeric styles).
   - Rewrite `Color.kt` light palette to Platinum Ivory + Jade; keep dark for later. Map into `ColorScheme.kt`.
   - Add an extension token holder (ivory gradient brush, credit/debit, card-ink/label, verified-gold,
     rosette-stroke) — e.g. a `SimpliPayTokens` CompositionLocal alongside `MifosTheme`.
2. **Shared components — `core/designsystem/component`**:
   `Rosette`, `ParticleField`, `IvoryCard`, `WalletWordmark`, `JadeButton`, `MoneyText` (Geist Mono),
   `TxnRow`, `ActionButton`, `BottomNavBar` w/ center FAB.
3. **Onboarding**: Splash + Landing screens + wire into `RootNavGraph` ahead of `LOGIN_GRAPH`.
4. **Login** re-skin.
5. **Passcode** re-theme via config.
6. **Home**, then **History**, **Profile**.
7. **Buy** (decide: extend `payments` vs new module) and **KYC** (deferred) separately.

---

## 4. Accuracy review — getting "highly accurate" on rosette + particles

Checklist the implementer must satisfy (this is where fidelity is usually lost):

**Rosette**
- [ ] Hairline stroke truly `0.5px` device px (do **not** round up to 1dp — use `0.5.dp.toPx()`); colour exactly `#9C9684`.
- [ ] Ellipse, not circle: `rx ≠ ry` (e.g. 120×39). Using a circle kills the fan look.
- [ ] Exact `n` per usage (35 / 40 / 34 …) and exact `opacity` (0.85 / 0.30 / 0.26 / 0.22). Opacity is per-ellipse,
      so overlap accumulates — don't apply it once to the whole layer.
- [ ] Canvas box = `rx*2.3` square; center pivot at `rx*1.15`. Clip parent card with rounded `overflow:hidden`
      and keep the negative offsets so only a corner shows.
- [ ] Splash rosette spins `90s` **linear** (not eased), continuous; glow circle (`312px` radial jade 0.10→0)
      sits beneath; wordmark above.
- [ ] Splash rosette params `rosette(120,39,35,0.85)` — much higher opacity than the card rosettes; it's the hero.

**Particle field**
- [ ] Jade `#0F8A7B`; dots at **0.30** alpha, lines at **≤0.28** alpha scaled by distance — lines are faint, dots subtle.
- [ ] Connection threshold exactly **120px**; opacity `(1 - d²/max²)*0.28`. (Quadratic falloff, not linear distance.)
- [ ] Velocity range `[-0.144, +0.144]` px/frame @ ~60fps — it is a *slow* drift. Bounce (invert v) at edges, no wrap.
- [ ] Count = `round(W*H/16000*1.35)` from the **actual** measured canvas size; seed once, don't reseed per frame.
- [ ] Sizes `[0.5,1.8]px` radius. Landing only; behind content; `pointerEvents:none` (non-interactive overlay).
- [ ] Respect reduced-motion / lifecycle: stop the frame loop when off-screen (mirror `componentWillUnmount`'s
      `cancelAnimationFrame`).

**Brand**
- [ ] Wordmark always lowercase `simplipay` + jade `.`; weight 800; tight `-0.03em`.
- [ ] Every monetary/numeric value renders in **Geist Mono**, never the sans family. This single rule does the
      most to make screens read as "the design".
- [ ] Ivory cards use the 155° 3-stop gradient + `#E0DCCE` border + warm inset highlight, not a flat fill.

---

## 5. Scope decisions (locked 2026-06-23)

1. **Buy / VAS** — ✅ **In scope**: build as a net-new feature (airtime/data/electricity/betting/bills grid).
2. **KYC** (8 frames) — ⏸ **Deferred**: module stays disabled; spec retained for later.
3. **Bottom nav** — ✅ **Adopt the design's nav**: Home · Buy · [center jade Scan FAB] · History · Profile.
4. **Landing screen** — distinct pre-login screen (Create account / I already have an account).
5. **Dark theme** — ship **light-only** ("Platinum Ivory") first; dark retheme later.

**Build order**: start with **theme foundation** (fonts, `Color.kt`, `Type.kt`, custom tokens), then signature
components, then onboarding → login → passcode → home → history → profile → buy.
