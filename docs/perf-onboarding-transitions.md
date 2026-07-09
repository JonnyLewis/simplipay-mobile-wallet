# Onboarding Transition Jitter — Splash→Login & Landing→Verify (converged plan)

iOS-first KMP wallet (CMP / compose-nav 2.9.0-beta03, Kotlin 2.2.21). Two janky transitions on
initial load. This is the converged output of a 5-generation review (Gen-0 main agent → Gen-1..5
subagents); each generation corrected the previous one. All claims verified against code.

## Root causes (verified)

**B. Landing "Create account" → signup VERIFY** — a **434 KB countries JSON parsed on the main
thread**. `AssetRepositoryImpl.getCountriesWithStates()` is the **only** repository that doesn't
hop to `ioDispatcher` (`AssetRepositoryImpl.kt:19-36`), and `SignupViewModel.loadCountriesFromJson()`
launches it from `init` on a bare `viewModelScope.launch` (Main). So `readBytes` + `decodeToString`
+ `Json.decodeFromString` + `associate` all run on Main during signup's 350 ms `pushLeft` enter
animation → stalls it.

**A. Splash → Login** — three things compound:
1. `ParticleField` (landing) computes its particle count from **pixels**, not dp (~300 at @3x), and
   does an O(n²) ≈ 45 k pair-checks per frame. It animates during the cross-fade *and* permanently
   on Landing — the dominant per-frame cost.
2. The root NavHost has **no transition** (`RootNavGraph.kt:95`), so splash→landing is cross-graph
   and uses the **700 ms** default cross-fade, during which the splash rosette + `LdDots` + the
   particle field all animate while Landing does its first layout.
3. `SplashScreen.holdMillis = 5000` — a fixed **5-second** delay. Not jank, but the dominant
   *perceived* latency of the whole splash→login experience.

## Locked fix list (ship order)

### 1. P1 — cures B (parse off Main). Risk: ~none.
- `AssetRepositoryImpl` (`core/data/.../repositoryImpl/AssetRepositoryImpl.kt:19-36`): add a
  `private val ioDispatcher: CoroutineDispatcher` ctor param and wrap the body in
  `withContext(ioDispatcher) { … }`.
- `RepositoryModule.kt:88`: `AssetRepositoryImpl()` → `AssetRepositoryImpl(get(ioDispatcher))`.
  The `ioDispatcher = named(MifosDispatchers.IO.name)` qualifier and its imports already exist in
  that file (matches the other 20 repos). On iOS `MifosDispatchers.IO` maps to `Dispatchers.Default`.
- **Do NOT** change the VM to `launchIO` — `SignUpViewModelTest` drives coroutines with
  `StandardTestDispatcher`, which doesn't own `Dispatchers.Default`; keeping the VM's bare
  `viewModelScope.launch` keeps `advanceUntilIdle()` deterministic. The test injects a **mock**
  `AssetRepository`, so the impl change is invisible to it.

### 2. P1 — main A per-frame lever + permanent Landing win. Risk: low (tune on-device).
- `ParticleField.kt`: add `maxParticles: Int = 80` (~`:62`); change the count clamp at `~:79` from
  `.coerceAtLeast(1)` to `.coerceIn(1, maxParticles)`. Caps ~300 → 80 (~14× fewer O(n²) checks).
  Additive param; the sole call site (`LandingScreen.kt:58`) uses defaults. Tune 80–120 on device.

### 3. P1.5 — shrink the A overlap window 700 → 140 ms, SCOPED. Risk: low.
- Scope the fast fade to the **splash edge only** — do NOT add a fade to the whole root NavHost
  (that would wrongly shorten passcode / biometric / >15 s-background reAuth / MAIN transitions).
- Splash composable (`RootNavGraph.kt:101`): `exitTransition`/`popExitTransition = { fadeOut(tween(140)) }`.
- Landing destination (`LoginNavGraph.kt:34`): `enterTransition = { fadeIn(tween(140)) }`.
- `composable(route, enterTransition=…, exitTransition=…)` lambdas (`AnimatedContentTransitionScope<NavBackStackEntry>.() -> …`) are supported in compose-nav 2.9.0-beta03; imports mirror `MifosNavHost.kt`.

### 4. P0-UX — needs product sign-off. Biggest perceived-latency payoff.
- `SplashScreen.kt:62`: `holdMillis` 5000 → ~1200 (better: gate forwarding on session readiness
  instead of a fixed delay).

## Measurement checklist (Instruments, Release, @3x)
- **B**: Time Profiler — `readBytes`/`decodeFromString`/`associate` on Main *before*, on a background
  thread *after*.
- **A**: Core Animation — Landing at-rest FPS before/after Fix 2; dropped-frame count across the
  splash→landing window before/after Fix 3.
- **Fix 4**: wall-clock launch → interactive Landing.
- 1st vs 2nd cold launch to isolate first-run Metal shader compile (gates the parked rosette pre-warm).

## Dropped / parked across the 5 generations (don't revisit)
- VM `launchIO` for the countries load — breaks test determinism.
- Defer countries load to the CONTACT step — breaks `SignUpViewModelTest` + shifts the SA default selection.
- Shrink/cache the countries asset — off the critical path once Fix 1 lands.
- Global root-NavHost fade — regresses passcode/biometric/reAuth/MAIN transitions.
- Freeze splash rosette/LdDots on leave; rosette Metal pre-warm; particle loop-start gating; swapping
  the particle `mutableStateListOf` to a plain list — marginal / profiling-gated.

> Process note: Gen-0 guessed "default transition + VM construction"; Gen-1 found the 434 KB
> main-thread parse and the ParticleField; Gen-2 quantified the particle O(n²); Gen-3 caught that the
> off-main fix must be repo-only (test determinism); Gen-4 caught that a root-wide fade regresses other
> transitions and found the 5 s splash hold; Gen-5 locked the dispatcher/scoping details.
