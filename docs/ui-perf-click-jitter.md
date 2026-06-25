# Click / Navigation Animation Jitter — Converged Optimization Strategy

iOS-first KMP wallet (CMP 1.9.3, Kotlin 2.2.21, Compose Navigation 2.9.1). Symptom: tapping
buttons (esp. navigating ones) produces jittery animations, worse on first visit.

This is the converged output of a 3-generation review (Gen-0 main agent → Gen-1 + Gen-2
subagents). Each generation disproved part of the prior one; the verdict below is the
survivor, verified against code.

## Root-cause verdict (verified)

**Primary suspect: the loading spinner shown *during* the navigation transition is itself an
expensive per-frame animation.**
- `MifosProgressIndicator` → `RosetteLoadingIndicator` runs an **infinite** `animateFloat`
  (`RosetteLoader.kt:106-110`, `rememberInfiniteTransition` + `infiniteRepeatable(tween(LinearEasing))`).
- Each frame it redraws the rosette: `for (i in 0 until count) { rotate(...) { drawOval(style = Stroke(strokePx)) } }`
  (`Rosette.kt:66-73`) — ~35 transform push/pops + 35 **sub-pixel hairline stroked ovals** per frame.
- On a navigating tap, the destination gates its content behind `ViewState.Loading` and shows
  this spinner on the first frames (e.g. `HomeScreen.kt:224-225`, `SendMoneyScreen.kt:413`), so
  **two per-frame animations run at once** (nav crossfade + rosette) and the stroked-oval draw
  triggers a **first-time iOS Metal shader compile** → matches the "first visit stutters" report.

**Disproven along the way (don't chase these):**
- ❌ "VM `init{}` does a synchronous main-thread load during the transition." Loads are already
  off-main: `BaseViewModel.launchIO` uses `Dispatchers.Default` (`BaseViewModel.kt:114-116`),
  repos `flowOn(IO)`. Moving `init{}`→`LaunchedEffect` only changes *when* the load starts, not jitter.
- ❌ "Koin VM construction is frame-droppingly expensive." All repositories are Koin `single`s
  (`core/data/.../di/RepositoryModule.kt:88-122`); VM construction just grabs pre-built singletons.
- ❌ "Large screens (Home/Send) compose during the transition." They gate behind `Loading` and
  render only the spinner on the first frame — so the *spinner*, not the screen tree, is what animates.
- ❌ "Default transition is a slide." Compose Nav 2.9.1 default is already a fade.
- ❌ "Use a CMP shader-prewarm API." No such public/in-repo API for 1.9.3.
- ✅ Confirmed: no custom `NavHost` transitions (`MifosNavHost.kt:300`); `MifosGradientBackground`
  is cached (`drawWithCache`/`onDrawBehind`, `Background.kt:85-94`) — not a suspect.

## Prioritised plan

### P0 — Measure (do first; decides the rest)
Profile a **Release** build in Xcode Instruments (Time Profiler + Core Animation / Hitches).
Confirm the signature: hitches localize to the **spinner-visible window** and are **worse on
first visit** to each screen. Also check whether purely in-place buttons (no navigation) also
stutter — if so, widen scope. Zero risk; tells us if the rosette hypothesis is right.

### P1 — High impact
1. **Make the loading spinner cheap (primary fix).** In `RosetteLoader.kt` / `Rosette.kt` /
   `MifosProgressIndicator.kt`:
   - The rosette is re-tessellated every frame because the animated `rotation` is read **inside**
     the Canvas draw lambda (`Rosette.kt:67`). Instead draw the rosette **once at rotation 0** and
     animate it with a single `Modifier.graphicsLayer { rotationZ = angle }` (centre matches the
     default transform origin; this exact pattern is already used at `HomeScreen.kt:621`).
   - Raise stroke to **≥1dp** (sub-pixel `0.5px` hairline strokes are slow + alias) and/or reduce
     `count` for the small in-screen loader.
   - **Delay the spinner ~180ms** so fast loads never show it (and it never competes with the transition).
   - Implement inside the shared composable so the **splash hero** (`SplashScreen.kt:78`) keeps spinning.
   - Confirmed this is the global spinner: `MifosProgressIndicator/Mini/Overlay` all render only
     `RosetteLoadingIndicator()` (`MifosProgressIndicator.kt:30-71`), shown on the Loading branch of
     Home/History/AcctDetail/Accounts/transfers/QR — AND as in-place overlays on non-navigating
     actions (Login `:122`, Profile `:119`, TransferConfirm `:231/363`, Beneficiary `:132`, KYC `:88`,
     Home Mini `:840`). So this one fix plausibly addresses **both** navigation jitter and in-place jitter.
   - Risk: low (one component). Expected impact: removes the dominant per-frame cost.
2. **Cache the static (non-spinning) card rosettes (NEW in Gen-3).** Home draws two stroked-oval
   rosettes per account card (`HomeScreen.kt:452` count=40, `:461` count=34 ≈ 74 ovals) inside a
   `HorizontalPager`, and Buy draws one (`BuyScreen.kt:231`). These are a compounding first-composition
   cost on the same screens, using the same thin-stroke-oval primitive. Cache them (e.g. draw into a
   cached layer / `drawWithCache`) so they aren't re-tessellated on recomposition/scroll. Risk: low.
3. **Add a short global nav transition** on the single `NavHost` (`MifosNavHost.kt:300-305`):
   `enterTransition/exitTransition = fadeIn/fadeOut(tween(~140ms))` (valid API in nav-compose 2.9.1).
   Shortens the window in which anything animates concurrently. Risk: low, one place.

### P2 — Cleanups (gated on profiling)
- Recomposition-stability pass on the worst screens (use Compose metrics report).
- Experiment with the iOS `ComposeUIViewController` `opaque` option only if Instruments shows
  compositing cost.

## IMPLEMENTED — P1 (2026-06-25)

Verified: `:cmp-shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.

- **P1.1a — spinner draws once, spins as a layer.** `RosetteLoader.kt`: both `RosetteLoader`
  (splash) and `RosetteLoadingIndicator` now pass the static fan (`rotation` left at 0) and spin
  via `Modifier.graphicsLayer { rotationZ = angle }` instead of feeding the animated angle into
  the draw. Splash still spins.
- **P1.2 — fan tessellation cached.** `Rosette.kt`: the `Canvas` is now `…graphicsLayer()` so the
  35 hairline ovals are recorded once into a cached layer; recomposition, `HorizontalPager`
  scrolling (Home's two per-card rosettes) and the outer rotation just re-composite the cached
  layer. Covers both the spinner and the static card rosettes in one change.
- **P1.1b — delayed spinner.** `MifosProgressIndicator.kt`: `MifosProgressIndicator`, `…Mini` and
  `…Overlay` gate the rosette behind `rememberDelayedVisible(180ms)`, so loads that finish quickly
  never flash a spinner during a transition.
- **P1.3 — short nav cross-fade.** `MifosNavHost.kt`: NavHost now uses
  `enter/exit/popEnter/popExit = fadeIn/fadeOut(tween(140))`.

NOTE: still want the **P0 Instruments pass on a Release build** to confirm the jitter is gone and
attribute any residual hitches (one-time Metal shader compile vs steady draw).

## Deliberately NOT doing (and why)
- `init{}` → `LaunchedEffect` migration **as a jitter fix** — loads already off-main; cosmetic here.
- Splitting/lazy-wrapping Home/Send — they already gate behind `Loading`; the tree isn't the cost.
- Koin/preload tuning — singletons already; construction is microseconds.
- ~~Gate the Buy hub behind Loading~~ — DROPPED (Gen-3): `BuyScreen` has no Loading branch and
  doesn't use the rosette spinner at all, so there's nothing to gate. Its only rosette cost is the
  static card rosette, covered by P1.2.
- Ruled out as causes (Gen-3): `notificationDot` (`drawWithCache`, cheap), the 3 real `.shadow(`
  usages, other infinite transitions (QrViewfinder/LoadingWheel — not on hot paths), and Home's
  `AnimatedContent:491` (trivial chip swap).

## Still needs on-device profiling to confirm
- That the rosette spinner is actually the dominant cost (vs. some other first-visit cost).
- First-visit vs every-visit per screen; whether in-place (non-nav) buttons also stutter.
- Buy-specific composition cost (since it's ungated).

> Process note: the rosette-spinner root cause was found only on Gen-2 (adversarial pass) after
> Gen-0/Gen-1's plausible-but-wrong hypotheses were falsified against the code — the value of the
> multi-generation review.
