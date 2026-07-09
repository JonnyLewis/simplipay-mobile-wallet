# UI Performance & Smoothness Analysis (iOS / Compose Multiplatform)

Goal: remove the intermittent micro-stutter ("getting stuck for split-seconds, doesn't flow")
on the SimpliPay KMP wallet. This is an evidence-based audit of the current code with a
prioritised action list. Stack: Kotlin 2.2.21, Compose Multiplatform 1.9.3, Compose
Navigation 2.9.1. Compose **strong skipping is on by default** (Kotlin 2.x), and a stability
config file exists (`compose_compiler_config.conf`) — so the baseline is decent; the stutter
is most likely a handful of specific issues plus build/measurement setup.

---

## 0. The single most important thing to check first

**Are you testing a DEBUG Kotlin/Native build?** Debug K/N is *dramatically* slower than a
release/optimised build — no inlining, no escape analysis, far more allocations and slower GC.
On iOS this alone routinely produces exactly the "stutters every few seconds" symptom.

➡️ Before changing any code, reproduce on a **Release** configuration
(`-Pkotlin.native.cacheKind=static` aside, build the app with the Release scheme / optimised
framework). If smoothness is acceptable in Release, the code is largely fine and we tune the
specific items below. If it still stutters in Release, the findings below are the cause.

---

## 1. Likely root causes (ranked by probability for *this* codebase)

| # | Cause | Evidence | Confidence |
|---|-------|----------|:----------:|
| 1 | Debug K/N build used for "feel" testing | n/a (process) | High |
| 2 | Periodic `WhileSubscribed(5_000)` flow restarts + network monitor emissions recomposing the app shell | `MifosAppState.kt:97-112` | Med |
| 3 | Non-lazy list rendering (`forEachIndexed`) inside a `LazyColumn` item on Home | `HomeScreen.kt:823` | Med |
| 4 | Synchronous `initKoin()` on the iOS main thread before first frame | `MifosViewController.kt:16-19` | Med |
| 5 | `Modifier.composed { }` in the bottom nav (defeats modifier caching) | `MifosApp.kt:373-392` | Low-Med |
| 6 | Per-call `ImageLoader(context)` instead of a configured singleton | `KYCLevel2Screen.kt:190-219` (+ remembered but per-screen in QR screens) | Low |
| 7 | First-frame/first-animation shader & layout cost on Skia/Metal | inherent to CMP iOS | Med |
| 8 | Unstable params (`List<…>`, nullable platform types) forcing recomposition | many screens pass `List<Account>` etc. | Med |

---

## 2. Findings (with file references)

### 2.1 Measurement is off — turn it on
Compose compiler **metrics & reports** are already wired behind Gradle properties
(`AndroidCompose.kt:52-58`) but disabled. We are flying blind on which composables are
restartable-but-not-skippable.

- Generate reports (runs on the Android compilation, which compiles the same `commonMain`
  composables, so the output covers the shared UI):
  ```
  ./gradlew :cmp-android:assembleDemoDebug \
    -PenableComposeCompilerReports=true -PenableComposeCompilerMetrics=true
  ```
  Then read `build/compose-reports/*-composables.txt` for `restartable skippable` vs
  `restartable` (NOT skippable = recomposes every time its parent does). Those are the hot spots.
- For runtime truth on device, profile the **Release** iOS build with **Xcode Instruments →
  Time Profiler** and **Core Animation (FPS)**; look for main-thread stalls and dropped frames,
  and correlate to a screen/interaction.

### 2.2 App-shell recomposition from monitor flows
`MifosAppState` exposes `isOffline` and `currentTimeZone` as `WhileSubscribed(5_000)` flows
(`MifosAppState.kt:97-112`). `isOffline` is collected at the very top of `MifosApp`
(`MifosApp.kt:94`), so any emission recomposes the whole `Scaffold` (top bar + bottom bar +
NavHost host). If the platform `NetworkMonitor` emits frequently (e.g. re-checks connectivity
on a timer/callback) this is a periodic, app-wide recomposition — a strong candidate for the
intermittent stutter.
- **Action:** confirm `NetworkMonitor.isOnline` only emits on *change* (`distinctUntilChanged`),
  not on a poll. Keep the offline read as low in the tree as possible (it already gates only a
  snackbar — consider moving the collection into a small dedicated composable so its
  recomposition scope doesn't include the NavHost).

### 2.3 Non-lazy transaction list on Home
`HomeScreen.kt:823` renders recent transactions with `transactions?.forEachIndexed { … }`
inside a single `LazyColumn` `item {}`. Every transaction (and divider) is composed and laid
out eagerly in one item — no lazy reuse, and the whole block recomposes together when the list
identity changes.
- **Action:** promote these to real lazy `items(transactions, key = { it.id })` entries in the
  `LazyColumn` (or cap to N and keep `forEach` only if N is genuinely small, ≤ ~8). Add stable
  `key`s. This also fixes scroll-position retention and per-item recomposition isolation.

### 2.4 Missing `key`s in lazy lists
Several `items(...)` calls have no `key` (heuristic scan): e.g.
`InstanceSelectorScreen.kt:193`, `transfer-intrabank/.../TransferConfirmScreen.kt:553`,
`transfer-interbank/.../SelectAccountScreen.kt:131`, `accounts/AccountsScreen.kt:255`,
`send-money/...` screens, `beneficiary/BeneficiaryListScreen.kt:212`. Without keys, Compose
can't match items across updates → unnecessary recomposition and lost scroll/animation state.
- **Action:** add `key = { it.id }` (stable, unique) to every `items(...)` over domain lists.

### 2.5 iOS startup work on the main thread
`MifosViewController.kt` runs `initKoin()` inside `ComposeUIViewController(configure = { … })`,
which executes synchronously on the main thread before the first frame. A large eager Koin
graph here delays first paint and can cause an initial hitch.
- **Action:** keep Koin modules **lazy** (`factory`/`single { }` are lazy by default — avoid
  `createdAtStart`/eager singletons). Verify no module does network/disk/JSON work at
  construction. Defer anything heavy to a `LaunchedEffect` after first frame.

### 2.6 `Modifier.composed { }` in navigation bar
`MifosApp.kt:373` (`notificationDot()`) uses `composed { }`, which opts the modifier out of
skipping/caching and re-runs composition logic on every recomposition of every nav item that
uses it.
- **Action:** rewrite as a non-composed modifier using `Modifier.drawWithCache { onDrawWithContent { … } }`
  (read the theme colour outside and pass it in, or use `drawBehind`). Small, safe win on a
  surface that recomposes on every navigation.

### 2.7 Image loading
`KYCLevel2Screen.kt:190,192,219` construct `ImageLoader(context)` per call (KYC is currently
disabled, so low impact today), and `QrImportScreen`/`QrProcessingOverlay` remember a
per-screen `ImageLoader`. There is **no app-wide `setSingletonImageLoader`** with configured
memory/disk caches.
- **Action:** configure one singleton Coil `ImageLoader` (memory + disk cache, crossfade off
  for tiny images) at app start and use `AsyncImage` with the singleton everywhere; never build
  an `ImageLoader` inside a composable.

### 2.8 Off-lifecycle coroutines (correctness > smoothness, but worth noting)
`CameraView.kt:236,268` use `GlobalScope.launch(Dispatchers.Default)` to start/stop the capture
session. Not a home-screen jank source, but `GlobalScope` work isn't cancelled with the
composable and can run past screen exit. Replace with a scope tied to the view/coordinator
lifecycle.

### 2.9 Stability of parameters
With strong skipping on, the main residual cost is composables whose params are *unstable*:
raw `List<T>`/`Map<T>` (used widely, e.g. `accounts: List<Account>`), and nullable
platform-mapped types. Unstable params make a composable restartable-but-not-skippable.
- **Action:** prefer `kotlinx.collections.immutable.ImmutableList`/`PersistentList` for list
  params passed into composables (the project already uses `ImmutableList` in places, e.g.
  `SignUpState.passwordFeedback`). Mark long-lived domain models `@Immutable` where truly
  immutable. Use §2.1 reports to target only the composables that actually show up as
  not-skippable rather than blanket-changing everything.

### 2.10 Navigation transitions
`MifosNavHost.kt:300` uses `NavHost` with **default** transitions (no `enterTransition`/
`exitTransition`). Defaults are usually fine, but if a *destination* screen does heavy work in
its first composition, that work runs *during* the transition animation and reads as a stutter.
- **Action:** ensure each screen's first frame is cheap (data already in state, no synchronous
  parsing/sorting in composition). Optionally standardise a light shared transition
  (fade/slide ~200ms) for consistency. Don't add heavier transitions until the above is clean.

---

## 3. Prioritised action plan

**P0 — verify before coding (hours)**
- [ ] Reproduce on a **Release** iOS build (§0). Decide if there's still a problem.
- [ ] Turn on Compose reports (§2.1); list the not-skippable composables on Home/Payments/History.
- [ ] Profile Release build in Instruments (Time Profiler + Core Animation) to find the actual stall.

**P1 — high impact, low risk (0.5–1 day each)**
- [ ] Confirm/ensure `NetworkMonitor` emits only on change; narrow the `isOffline` collection scope (§2.2).
- [ ] Convert Home's transaction `forEach` to lazy `items(..., key=)` (§2.3).
- [ ] Add `key`s to all lazy lists (§2.4).
- [ ] Audit Koin for eager/heavy initialisation; keep startup lazy (§2.5).

**P2 — cleanups (small)**
- [ ] Replace `composed{}` notification dot with `drawWithCache` (§2.6).
- [ ] Single configured Coil `ImageLoader`; remove per-composable instances (§2.7).
- [ ] Replace `GlobalScope` in `CameraView` with a lifecycle-bound scope (§2.8).
- [ ] Migrate hot list/params to `ImmutableList`, guided by the reports (§2.9).

**P3 — only if still needed**
- [ ] Standardise light nav transitions; ensure cheap first-frames (§2.10).

---

## 3b. IMPLEMENTED (2026-06-25)

Verified compiling: `:cmp-shared:compileKotlinIosSimulatorArm64` (pulls in all changed feature modules) → BUILD SUCCESSFUL.

**Done in code:**
- **§2.6 `composed{}` → `drawWithCache`** — `MifosApp.kt` `notificationDot()` is now a non-composed modifier taking the colour as a param (3 call sites updated). Removes per-recomposition composition work on the bottom nav, which recomposes on every navigation.
- **§2.8 GlobalScope → scoped + disposal** — `CameraView.kt` (iOS QR scanner): `ScannerCameraCoordinator` now owns a `CoroutineScope(Dispatchers.Default + SupervisorJob())`; start/restart use it; added `dispose()` (cancels scope + stops session) wired into the composable's `DisposableEffect.onDispose`.
- **§2.4 keys** — added stable `key`s to the active lazy lists that lacked them: `InstanceSelectorScreen` (endpoint+tenant), interbank `SearchRecipientScreen` (requestId) & `SelectAccountScreen` (Account.id), intrabank `SelectPayeeScreen` (AccountOption.accountId), send-money `PaymentChatHistoryScreen` (date) & `UpiTransactionHistoryScreen` (monthYear). (Most other lists — accounts, beneficiary, etc. — already had keys; the original audit's "missing keys" were false positives from a line-based grep.)

**Assessed — no change needed / deferred (with reason):**
- **§2.2 network monitor** — NON-ISSUE on iOS: the native `NetworkMonitor.isOnline` is `flowOf(true)` (single emission), so it never drives periodic recomposition. No fix needed.
- **§2.5 Koin startup** — already lazy: `createdAtStart` is used nowhere; no eager singletons. No change.
- **§2.3 Home transactions `forEach`** — left as-is: it's a *bounded* "recent" list rendered inside a styled card container; converting to lazy `items` would break the card and yields little for a short list.
- **§2.7 Coil singleton** — deferred (low impact): Coil isn't on the home/hot path (only QR + disabled-KYC screens, which already `remember` their loaders). A configured singleton can be added if image-heavy screens appear; not worth a new `cmp-shared` dependency now.
- **§2.9 ImmutableList migration** — deferred until the Compose metrics report (P0) identifies the actually-not-skippable composables; blanket-changing list params/VM state across the app is broad and risky without that data.

**Still recommended (process, not code):** the P0 items — reproduce on a **Release** build, generate the Compose compiler reports, and profile in Instruments — remain the highest-leverage next steps and will tell us whether §2.9 is worth doing and where.

## 4. How we'll know it worked
- Compose report: target composables move from `restartable` → `restartable skippable`.
- Instruments Core Animation: sustained 60fps (120 on ProMotion) during scroll & tab switches,
  no recurring main-thread spikes.
- Subjective: tab switches and Home scroll feel continuous, no periodic hitch.

> Note: this document is analysis only — no code changed. Items are scoped so each can be done
> and verified independently.
