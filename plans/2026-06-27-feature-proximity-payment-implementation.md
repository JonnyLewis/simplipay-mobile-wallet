# Technical Implementation Plan — Proximity Payment

> **Type**: feature
> **Status**: planning
> **Created**: 2026-06-27
> **Spec**: `claude-product-cycle/design-spec-layer/features/proximity/proximity-payment.md` (v6) + `API.md`
> **Module**: new `feature/proximity` (KMP) + `core/network` + `core/data` additions
> **Targets**: Android + iOS (full); Desktop JVM (best-effort Sender); JS/WASM (Noop → QR)

This plan turns the v6 spec into a source-code build order. It does **not** restate the spec — it maps spec sections (§) to concrete files, tasks, dependencies, and acceptance criteria, following this repo's existing conventions (`org.convention.cmp.feature`, `BaseViewModel`, Koin in `KoinModules.kt`, expect/actual seams like `CameraView`/`IdOcr`, navigation in `MifosNavHost.kt`).

Backend endpoints (`/proximity/*`) are a **separate workstream**; this plan covers the client + the contract it consumes (per `API.md`) and assumes the backend is delivered in parallel. Where the client can be built/tested against a stub, that is called out.

---

## 0. Pre-work decisions (resolve before Task 1)

| # | Decision | Recommendation | Blocks |
|---|----------|----------------|--------|
| D1 | KMP crypto provider for Ed25519 verify, X25519 ECDH, HMAC-SHA256, HKDF (none in tree today, spec §15) | Add **`cryptography-kotlin`** (dev.whyoleg) — KMP, has the needed primitives on Android (JDK/BC) + Darwin (CryptoKit/Security). Fallback: `expect/actual` over `java.security`/CryptoKit. | T2 |
| D2 | GATT UUIDs (discovery service + `PAYLOAD`/`COMMIT`/`TX_PUBKEY`/`RX_PUBKEY`) | Generate 4 random 128-bit UUIDs; freeze them in a shared `ProximityGatt` constants object | T3, T4 |
| D3 | Distance txPower reference (RSSI→metres calibration, §6.5) | Per-model constant table + a default; refine during the Phase-0 spike | T6 |
| D4 | UWB precise ranging in scope for v1? | **No** — RSSI estimate ships in Phase 1; UWB (iOS `NearbyInteraction` / Android `androidx.core.uwb`) is Phase 4 (§11). Keep `supportsPreciseRanging=false` initially. | — |
| D5 | Desktop transport effort | Ship **Noop on desktop for v1** (gray-out + QR), add best-effort macOS/Linux central later. Avoids a JVM-BLE rabbit hole blocking mobile. | T4 |

> **Phase-0 spike first (strongly recommended, separate throwaway branch):** two devices (1 iOS, 1 Android), advertise a service UUID → scan → GATT connect → read a 20-byte characteristic, no UI, no crypto. Proves the cross-platform path and surfaces MTU/`value=nil`/poweredOn quirks before real investment. ~2–4 days.

---

## 1. Module & build wiring (Task T1)

**Goal:** an empty-but-compiling `feature/proximity` module registered everywhere.

- `feature/proximity/build.gradle.kts` → `alias(libs.plugins.cmp.feature.convention)`, namespace `org.mifospay.feature.proximity`; declare source sets `androidMain, nativeMain, desktopMain, jsMain, wasmJsMain`.
- Add to `settings.gradle.kts` (`include(":feature:proximity")`).
- Add `projects.feature.proximity` to `cmp-shared/build.gradle.kts` `commonMain.dependencies`.
- DI wiring follows the repo's **`expect/actual` platform-module** pattern (NOT a per-platform edit of `allModules`, which is commonMain-only): declare `expect val proximityPlatformModule: Module` in commonMain, `actual val` it in each leaf source set (Android/native/desktop/js/wasm), and have the commonMain `ProximityModule` pull it in via `includes(proximityPlatformModule)`. Then add `ProximityModule` **once** to `featureModules` in `cmp-shared/.../di/KoinModules.kt`. Mirror `core/data/.../di/PlatformDependentDataModule.kt` + `core/common/.../DispatchersModule.kt` (`expect val ioDispatcherModule`).
- Dependencies: `core:designsystem`, `core:ui`, `core:data`, `core:common`, `core:model`, plus D1 crypto lib; androidMain `accompanist.permissions`.

**Acceptance:** `./gradlew :feature:proximity:assemble` and `:cmp-shared:compileKotlin*` green on all targets; Koin graph still starts (`cmp-android` boots).

---

## 2. Protocol codec + crypto core (Task T2) — commonMain, pure, no BLE/UI

Build the platform-free heart first so it's unit-testable in isolation.

- `transport/ProximityPayload.kt` — encode/decode the exact **20-byte** layout (§5.1): version, device_class, flags, 16-B token, CRC-8/SMBUS (poly `0x07`/init `0x00`/no-reflect). `ByteArray`↔model.
- `security/Tokens.kt` — windowed-HMAC token `trunc16(HMAC_SHA256(token_key, floor(epoch/W)))` (§5.2); `trunc16` = first **16 bytes**.
- `security/Handshake.kt` — X25519 keygen; commit `C_r = HKDF(epr_pub, info="…/commit/v1")`; `shared = X25519(...)`; verify commitment (§5.7). **All bilateral values salted with `receiver_session_pubkey`, never the token** (§5.7/Appendix C).
- `security/Derivations.kt` — `badge = symbolMap(HKDF(epr_pub, salt=session_id, info=badge/v1))`; `matchHandle = symbolMap(HKDF(shared, salt=epr_pub, info=match/v1))`; `linkProof = HKDF(shared, salt=epr_pub, info=link-proof/v1)`.
- `security/SymbolMap.kt` — 20-bit → colour+emoji pair from the curated ≥1024 set (§14.2). Pure Kotlin.
- `security/SignatureVerifier.kt` — Ed25519 verify over raw `signed_payload` bytes; pinned **key-ring + root-chain `kid`** selection, fail-closed on unknown `kid` (§5.8).

**Acceptance:** unit tests (§13) pass — codec round-trip + CRC; **commitment binds before reveal (grinding attempt fails)**; badge≠handle with correct domain separation; forged/expired/unknown-`kid` signatures rejected; token rotation timing. No Android/iOS deps in this package.

---

## 3. Transport interface + GATT contract (Task T3) — commonMain

- `transport/BleProximityTransport.kt` — the plain **interface** (§4.2): `capabilities`, `startAdvertising(peripheral)→Flow<PeripheralHandshakeResult>`, `stopAdvertising`, `scanForReceivers():Flow<BleDiscovery>`, `stopScan`, `connectAndHandshake(deviceId):GattHandshakeResult`, `disconnect`. Data classes `BleDiscovery`, `GattHandshakeResult`, `BleCapabilities(canAdvertise, canScan, isBluetoothOn, supportsPreciseRanging)`.
- `transport/ProximityGatt.kt` — frozen UUIDs (D2) + characteristic properties + handshake order constants (API Appendix A): `COMMIT`/`TX_PUBKEY`/`PAYLOAD` read, `RX_PUBKEY` write; MTU≥64; per-central `TX_PUBKEY`-withhold-until-`RX_PUBKEY`-written rule.

**Acceptance:** compiles commonMain; no platform import; interface matches §4.2 byte-for-byte.

---

## 4. Platform transports (Task T4) — per-platform actuals + Koin binding

- **androidMain** `AndroidBleProximityTransport` — `BluetoothLeAdvertiser` (advertise service UUID), `BluetoothGattServer` (serve `COMMIT`/`TX_PUBKEY`(withhold)/`PAYLOAD`(dynamic per read) + accept `RX_PUBKEY` write), `BluetoothLeScanner` + `ScanFilter`/`SCAN_MODE_LOW_LATENCY`, `connectGatt`→`requestMtu(64)`→discover→read. Callbacks via explicit `Handler`→`trySend` (§9.2). `di/ProximityModule.android.kt` binds via `androidContext()` + `BluetoothManager`.
- **nativeMain (iOS)** `IosBleProximityTransport` — `CBPeripheralManager` (advertise + GATT server, **`COMMIT`/`TX_PUBKEY`/`PAYLOAD` all `value=nil`** so reads hit `didReceiveRead`, §6.1), `CBCentralManager` (scan + connect + `readValue`/`writeValue`). Dedicated **serial dispatch queue**; **retain managers + delegates as strong props** (§9.2/§15). Await `poweredOn`; `addService` before `startAdvertising`.
- **desktopMain** `DesktopBleProximityTransport` → Noop (D5): `capabilities=all false`.
- **jsMain / wasmJsMain** → Noop (`capabilities=all false`).
- Each non-common source set provides `actual val proximityPlatformModule` binding its transport (per the `expect/actual` pattern in T1) — not a direct `allModules` edit.

**Acceptance:** all five source sets compile; Phase-0-spike behaviours hold on real devices — iOS↔Android advertise→scan→handshake→read; iOS token actually rotates per read (dynamic value); `TX_PUBKEY` refused before `RX_PUBKEY` write; desktop shows `canScan=false`.

---

## 5. Permissions (Task T5)

- `permission/BlePermissionState.kt` — `expect fun rememberBlePermissionState()`, mirroring `CameraPermissionState` (§6.6). androidMain via `accompanist.permissions` (`BLUETOOTH_SCAN`+neverForLocation/`ADVERTISE`/`CONNECT`, pre-12 `ACCESS_FINE_LOCATION`); nativeMain via `CBManager.authorization` (Info.plist only).
- Manifest: add BLE perms to `cmp-android` (or feature) manifest. Info.plist: add `NSBluetoothAlwaysUsageDescription` to `cmp-ios/iosApp/Info.plist` (**release gate — crash if missing**).

**Acceptance:** first-use prompt fires; denied/settings path works; pre-12 device requests location with priming copy (§8.6).

---

## 6. Network + data (Task T6) — `core/network` + `core/data`

- `core/network/.../services/ProximityService.kt` — Ktorfit service for the 9 endpoints (`API.md`): `session`, `provision-donation`, `session/bind`, `resolve`, `quote`, `confirm`, `accept`, `session/{id}/events` (SSE/long-poll), `session/stop`. DTOs per `API.md` (base64url blobs, minor-unit money, epoch-seconds, opaque `signed_payload`).
- Wire into `FineractApiManager`/`SelfServiceApiManager` (whichever host; bearer auth).
- `core/data/.../repository/ProximityRepository(Impl)` — wraps the service in `DataState<T>`/`SafeApiCall`; owns signature verification (T2) on resolve; exposes the events `Flow`. Token derivation helper (windowed HMAC) lives here or in `ProximitySession`.
- RSSI→distance estimator (§6.5) with txPower table (D3).

**Acceptance:** repository unit-tested against a mock service; resolve rejects bad signatures before returning; can run end-to-end against a backend stub.

---

## 7. ViewModel, session state machine, navigation (Task T7) — commonMain

- `ProximityViewModel : BaseViewModel<ProximityState, ProximityEvent, ProximityAction>` (§9) — state/actions/events as specced; scan via `scanForReceivers().stateIn(WhileSubscribed(5_000))` (one upstream scan, ≥6 s restart debounce, §6.4/§9); **eagerly resolve the nearest N=3 at the initials tier**, full-resolve on tap (§6.3/§5.11/§14.4); lifecycle→`LifecycleChanged` stops scan/advertise + `/session/stop` on background.
- **Settle notification** reuses the existing `NotificationRepository` (+ FCM/poll) path (§7.3); the `/session/{id}/events` SSE is the primary in-session signal, FCM data message a redundant wake (§5.10).
- `session/ProximitySession.kt` — the §9.1 state machine (`Idle→Minting→Advertising→SenderConnecting→PendingAccept→Settling→Settled|Rejected|Expired|Stopped`); token-rotation timer; holds the `/session/{id}/events` channel for receiver-accept; DONATION returns to `Advertising` (tally++).
- `navigation/ProximityNavigation.kt` — `NavGraphBuilder.proximityScreen(...)` + `navigateToProximity(...)` (mirror `fastMpayScreen`/`FastMpayNavigation`). Two entry doors (§8.1): Send-money "Nearby" → `Mode.RadarScan`; Request/Get-paid "Nearby" → `Mode.Receive`. Wire both call-sites + the route into `MifosNavHost.kt`.

**Acceptance:** VM unit tests on the reducer + session transitions; nav compiles and both doors reach the graph; capability gating drives grayed-out entry (§8.1).

---

## 8. UI (Task T8) — commonMain Compose, SimpliPay tokens

- `ProximityReceiveScreen` (§8.2/8.3) — mode + amount (Open/Fixed, Geist Mono tabular), live beacon w/ **session badge**, swappable bottom region (P2P liveness / DONATION tally), keep-screen-on, idle timeout, receiver-accept prompt.
- `ProximityNearbyScreen` (§8.4) — list-first (decorative `SonarBackdrop` reusing `ParticleField`); rows = initials + amount band + **estimated distance**, nearest first; hold-to-ping; no badge pre-connect.
- `ProximityConfirmSheet` (§8.5) — identity-first (photo+name+badge+distance); escalated tier reveals **match handle** + receiver-accept; Open amount entry + signed quote; tip chips.
- `component/SlideToConfirm.kt` (§8.7) — new reusable drag-to-confirm (jade/ivory tokens, haptics, reduce-motion, non-long-press a11y).
- `component/HandleBadge.kt` — colour+emoji symbol pair renderer.
- Capability gating + "Device not capable" banner + QR fallback CTA (§8.1/§12); permission priming screen (§8.6); humane error states (§12).

**Acceptance:** headless render (`:cmp-desktop:renderShots` pattern already used in this repo) of each screen matches the spec mockups; SlideToConfirm interaction tests; capability-disabled state renders banner.

---

## 9. Test + hardening (Task T9)

Implement the §13 test plan: unit (codec, commitment-grind-fail, derivations, single-settle), CI security gates (≤20-byte adv, unlinkable captures, forged sig, replay→original, 409 on settled session, initials tier hides name), instrumented two-device transport, UX, E2E incl. relay simulation (rebroadcast → handle mismatch + receiver-accept block).

**Acceptance:** all gates green; `./ci-prepush.sh` clean (spotless/detekt/dependency-guard updated for new deps).

---

## 10. Sequencing & dependencies

```
Phase-0 spike (throwaway) ──► D1..D5 decisions
T1 module wiring
  └► T2 codec+crypto (parallel w/ T3)
  └► T3 transport iface+GATT ──► T4 platform transports ──┐
  └► T5 permissions                                       │
T6 network+data ───────────────────────────────────────► T7 VM+session+nav ─► T8 UI ─► T9 tests
```
- **Critical path:** T1→T3→T4 (real-device BLE) and T6→T7→T8.
- T2, T5, T6 parallelizable after T1.
- Maps to spec **Phase 0** (T1–T7 backend-integrated, no UI) then **Phase 1** (T7–T9, P2P shippable). DONATION/POS/UWB = spec Phases 2–4, out of this plan.

## 11. Risks & mitigations
- **iOS dynamic-characteristic / poweredOn / strong-ref pitfalls** (§6.1/§9.2) → de-risk in Phase-0 spike; explicit acceptance in T4.
- **Android scan throttle (5/30 s)** → single shared upstream scan + debounce (T7).
- **No crypto in tree** (D1) → resolve before T2; spike the chosen lib on both platforms.
- **Backend not ready** → build client against an `API.md`-faithful mock; gate E2E on backend.
- **Cross-platform field quirks (MTU, RSSI variance)** → real-device matrix in T9; distance shown as estimate only.

## 12. Definition of done (Phase 1)
P2P pay works iOS↔Android foreground (open + fixed), identity-first confirm + slide, receiver-accept above threshold, settle notification, estimated distance on radar, capability gray-out + QR fallback, all §13 gates green, `ci-prepush.sh` clean, headless render shots match spec.
