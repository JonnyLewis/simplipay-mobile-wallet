# Proximity Payment — Engineering Specification

> **Status**: READY FOR IMPLEMENTATION — v6 (review-loop gens 1–5 + product-clarification pass: cross-platform/desktop capability gating, radar distance display, POS role, cloud-POS future)
> **Companion**: `API.md` (full DTOs, encodings, error codes, GATT profile, threading)
> **Feature module**: `feature/proximity`
> **Platforms**: Android + iOS (full) · Desktop/Web (graceful fallback to QR)
> **Last Updated**: 2026-06-27
> **Owner spec file** (the deliverable engineers build from): this file.

---

## 0. TL;DR for Engineers

Proximity Payment lets a **Receiver** stand up a BLE beacon ("I want to be paid") and lets nearby **Senders** discover them, pick them, and pay with a **slide-to-confirm** gesture. The thing being paid is the wallet's existing **pay-link** — but the pay-link **never travels over Bluetooth**. BLE carries only a tiny, rotating, opaque **token**. The Sender resolves that token against our backend over TLS to get a **server-signed** display payload (name, amount, verified flag), then confirms. (The visual badge and match handle are derived **locally** on both devices — never in the payload, §5.7.)

The BLE radio work is a **thin platform seam** — a plain `commonMain` **interface** (`BleProximityTransport`) bound by a **per-platform Koin module** (Android BLE APIs / iOS CoreBluetooth via Kotlin/Native cinterop). Everything above the radio — radar/list UI, token resolve, confirm, slide-to-pay, settlement — is **shared KMP Compose** in `commonMain`.

Three **device classes** are baked into the protocol from day one: `P2P` (this release), `DONATION` (near-future, one receiver / many payers), `POS` (near-future, terminal acts as Sender).

**Security posture in one line:** BLE is untrusted transport; the trust anchor is a **server Ed25519 signature** over the resolved payload; relay/wrong-person are defended by **two-sided human confirmation** (identity-first + receiver-accept above a threshold) bound to a **live ECDH over the GATT link**, not by anything carried in the token.

---

## 1. Goals & Non-Goals

### 1.1 Goals
- Cross-platform (one KMP codebase) proximity discovery + payment, working iOS↔Android.
- Two user modes: **Receive** (advertise) and **Send** (radar/list → pay).
- Receive supports **open amount** ("pay what you want") or **fixed amount**.
- Strong security: **no account data, no pay-link, no PII on the air**; trust anchored server-side.
- Deliberate **slide-to-confirm** pay gesture with appropriate animation.
- Receiver gets a **notification** when a proximity payment settles.
- A **device-class** field (P2P / DONATION / POS) in the on-air protocol, extensible for future classes.

### 1.1a Cross-platform & device-capability requirements (explicit)
- **Fully interoperable iOS ↔ Android** for the whole flow (this is a first-class requirement, not best-effort — our POS runs Android while many customers are on iOS).
- **Desktop (macOS/Windows/Linux): best-effort.** Where the OS exposes a usable BLE central stack, a desktop may act as a **Sender** (scan + pay). Advertising (Receiver) is not supported on desktop. **Capability is probed at runtime**; where BLE is unavailable the feature entry is **grayed out with a "Device not capable" banner** (§8.1) and the QR fallback is offered.
- **Distance on the radar** is a product requirement (§6.5): estimated (RSSI, cross-platform) baseline, precise (UWB) where both ends are same-platform-capable.

### 1.2 Non-Goals (this release)
- Background advertising/scanning (impossible to do reliably cross-platform — §6.4). Sessions are **foreground-only**.
- Desktop **Receiver** (advertise) role and any Web BLE participation (fall back to existing QR; gray out).
- A dedicated **POS terminal module** and the **cloud/remote-POS connector** (push a transaction to a specific counter) — **future**, see §11 Phase 5; the protocol already accommodates them.
- UWB precise ranging as a *baseline* (it's a same-platform-only enhancement — iOS↔iOS and Android↔Android, §6.5/Phase 4; the cross-platform baseline is RSSI estimated distance, which ships in Phase 1). UWB distance-bounding is the only true cryptographic relay defense but, being same-platform-only, can't be the cross-platform security baseline.
- DONATION and POS full UX — protocol-ready now, screens shipped later (§11).
- BLE-level pairing (LE Secure Connections) as the trust root — server signature is the trust root (§5.6).

---

## 2. User Stories

| # | As a… | I want to… | So that… |
|---|-------|-----------|----------|
| 1 | Receiver | tap **Get paid nearby**, choose open or fixed amount, and start | nearby people can pay me without typing my details |
| 2 | Sender | open **Send money → Nearby** and see who's ready to be paid | I can pick the person in front of me |
| 3 | Sender | match the **badge** the receiver is showing on their screen | I'm sure I tapped the right person, not a stranger (wrong-person check; relay is handled by story 7) |
| 4 | Sender | see name + photo + requested amount before paying | I know who and how much |
| 5 | Sender | enter my own amount when the request is "open" | I control what I pay |
| 6 | Sender | **slide to confirm** to pay | I don't pay by accident |
| 7 | Receiver | approve "X is paying you R50" for larger amounts | a far-away relay can't make a stranger pay me / I can't be tricked |
| 8 | Receiver | get a notification the moment payment lands | I know it's done without watching the screen |
| 9 | Either | be told clearly when Bluetooth/permission is off or the platform is unsupported, with a QR fallback | I'm never stuck on a dead screen |

---

## 3. End-to-End Flow

```
RECEIVER (advertise)                         SENDER (scan → pay)
───────────────────                          ──────────────────
1. Tap "Get paid nearby"
2. Choose Open / Fixed (+amount)
3. POST /proximity/session  ───────────────► returns {session_id, token_key, W, exp}
   (binds receiver's pay-link + amount)         (device derives rotating token locally)
4. Start BLE advertise(serviceUUID)
   GATT: PAYLOAD(value=nil) + COMMIT            4. Send money → Nearby
        + TX_PUBKEY + RX_PUBKEY                  5. BLE scan(serviceUUID) → list by RSSI
   screen shows BADGE 🟢🦊 (same for all)        6. Resolve nearest N=3 (initials tier) /
   holds /session/{id}/events (long-poll)            full resolve on tap:
                                               7. GATT: read C_r→write eps_pub→read epr_pub
   ◄────── commit/reveal ECDH over GATT ─────►     +PAYLOAD(token) atomically→verify C_r→shared
   receiver POST /session/bind {link_proof}    8. POST /proximity/resolve {token, link_proof}
   ◄─────────────────────────────────────────    ◄── signed{name, photo, amount, class,
                                                       verified, iat, exp}  (NO handle/proof)
   badge matches list row                       8. List shows name+photo+amount; badge match
                                               9. Sender taps row → Confirm sheet
   (>threshold) events:"X pays you R50?"        10. (open) /quote → signed amount; enter amt
13. Receiver taps "That's me ✓"                11. (>threshold) compare MATCH HANDLE 🟣🦉
    POST /proximity/accept                          SLIDE TO CONFIRM
   ◄──── push/notif "X paid you R50" ──────── 12. POST /proximity/confirm
14. Receiver sees success + notification          {session_id, payment_intent_id, amount,
    (session single-settles, P2P auto-stops)       link_proof, signed_quote?}
                                                   backend settles, idempotent, single-settle
                                              13. Sender sees success
```

---

## 4. Architecture

### 4.1 Layer split (what lives where)

```
┌─ commonMain (shared, ~90% of code) ───────────────────────────────┐
│  feature/proximity/                                                │
│   ├─ ProximityViewModel           BaseViewModel<State,Event,Action>│
│   ├─ ProximityReceiveScreen       (mode select, amount, beacon)    │
│   ├─ ProximityNearbyScreen        (list-first; sonar is decorative)│
│   ├─ ProximityConfirmSheet        (signed details + SlideToConfirm)│
│   ├─ component/SlideToConfirm.kt  (new reusable gesture button)    │
│   ├─ component/SonarBackdrop.kt   (decorative, reuses ParticleField)│
│   ├─ component/HandleBadge.kt     (color+emoji pairing handle)     │
│   ├─ session/ProximitySession.kt  (token rotation, state machine)  │
│   ├─ transport/BleProximityTransport.kt   (plain INTERFACE, §4.2)  │
│   ├─ transport/ProximityPayload.kt        (20-byte codec, shared)  │
│   ├─ security/Handshake.kt        (X25519 ECDH over GATT)          │
│   ├─ security/SignatureVerifier.kt(Ed25519 verify, kid key-ring)   │
│   ├─ permission/BlePermissionState.kt (expect fun, §6.6)           │
│   ├─ navigation/ProximityNavigation.kt                             │
│   └─ di/ProximityModule.kt        (commonMain: VM, repo, session)  │
│  core/data:    ProximityRepository (resolve/confirm/session APIs)  │
│  core/network: ProximityService (Ktorfit)                          │
└────────────────────────────────────────────────────────────────────┘
        │ platform seam = interface bound by per-platform Koin module
        ▼
┌─ androidMain ─────────────┐  ┌─ iosMain (nativeMain) ─────────────┐
│ AndroidBleProximityTransport│ │ IosBleProximityTransport           │
│  BluetoothLeAdvertiser     │ │  CBCentralManager (scan)           │
│  BluetoothLeScanner        │ │  CBPeripheralManager (advertise)   │
│  BluetoothGattServer/Client│ │  GATT server/client (value=nil!)   │
│  di/ProximityModule.android│ │  di/ProximityModule.native         │
│  (binds via androidContext)│ │  (self-constructs CB managers)     │
└───────────────────────────┘  └────────────────────────────────────┘
┌─ desktopMain (JVM) ───────────────────────────────────────────────┐
│ DesktopBleProximityTransport — BEST-EFFORT, Sender(scan)-only where │
│ the OS BLE central stack is reachable (macOS CoreBluetooth via a    │
│ JVM bridge / Linux BlueZ); advertising NOT supported. Probe at      │
│ runtime → if unreachable, capabilities=all false (§8.1 gray-out).   │
├─ jsMain / wasmJsMain ─────────────────────────────────────────────┤
│ NoopBleProximityTransport → capabilities=all false → "Device not    │
│ capable" + QR fallback (Web Bluetooth can't advertise; scan gated). │
└────────────────────────────────────────────────────────────────────┘
```

### 4.2 The transport seam — plain interface, Koin-bound per platform

**Decision (revised from v1):** use a plain `commonMain` **interface**, not `expect class`. An `expect class` cannot receive the Android `Context`/`BluetoothManager` its actual needs, and it diverges from the repo's convention (`CameraPermissionState`, `IdOcr`, `IntentManager` are all interface/`expect fun` + platform binding). Bind the implementation in a **per-platform Koin module**.

```kotlin
// commonMain — transport/BleProximityTransport.kt
interface BleProximityTransport {
    val capabilities: BleCapabilities

    // RECEIVER role -------------------------------------------------
    /** Advertise the discovery service UUID + run the GATT server.
     *  [peripheral] supplies the rotating payload per-read (iOS value=nil, §6.1), the
     *  commitment C_r, and the receiver ephemeral key it reveals after the sender writes
     *  RX_PUBKEY. Emits each completed sender handshake so the session layer can /session/bind. */
    suspend fun startAdvertising(peripheral: PeripheralHandshake): Flow<PeripheralHandshakeResult>
    suspend fun stopAdvertising()

    // SENDER role ---------------------------------------------------
    /** Cold callbackFlow: starts scan on first collect, stops in awaitClose.
     *  ViewModel shares it via stateIn(WhileSubscribed) — ONE upstream scan (§9). */
    fun scanForReceivers(): Flow<BleDiscovery>
    suspend fun stopScan()

    /** Connect → commit/reveal ECDH (§5.7) → atomic windowed read of PAYLOAD.
     *  Returns token + shared secret (for badge/handle/link_proof) + the window id the
     *  token & receiver pubkey were read under; caller ABORTS if windows disagree (§6.1). */
    suspend fun connectAndHandshake(deviceId: String): GattHandshakeResult
    suspend fun disconnect(deviceId: String)
}

// Receiver-side per-connection inputs/outputs (badge/handle derived in commonMain security/)
interface PeripheralHandshake {
    fun currentPayload(): ByteArray          // 20 B, current window (§5.1)
    fun commitment(): ByteArray              // C_r = HKDF(epr_pub ‖ token)
    fun revealOnSenderKey(senderPub: ByteArray): RevealedKeys // returns epr_pub + shared
}
data class PeripheralHandshakeResult(val shared: ByteArray, val token: ByteArray, val windowId: Long)

data class BleDiscovery(val deviceId: String, val rssi: Int)   // no inline payload (§4.3)
data class GattHandshakeResult(
    val payload: ByteArray, val ecdhShared: ByteArray,
    val receiverPub: ByteArray, val windowId: Long,            // atomic snapshot (§6.1)
)
data class BleCapabilities(
    val canAdvertise: Boolean,         // Receiver role — false on most desktop, all web
    val canScan: Boolean,              // Sender role — true where a BLE central stack exists
    val isBluetoothOn: Boolean,        // runtime adapter state
    val supportsPreciseRanging: Boolean, // UWB present (same-platform only, §6.5)
)
// Capability is PROBED AT RUNTIME (not assumed per platform): if neither role is available the
// feature entry is grayed out with a "Device not capable" banner + QR fallback (§8.1/§12).
```

```kotlin
// androidMain — di/ProximityModule.android.kt
val proximityPlatformModule = module {
    single<BleProximityTransport> {
        AndroidBleProximityTransport(
            context = androidContext(),
            bluetoothManager = androidContext()
                .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager,
        )
    }
}
// nativeMain — di/ProximityModule.native.kt
val proximityPlatformModule = module { single<BleProximityTransport> { IosBleProximityTransport() } }
// desktopMain/jsMain/wasmJsMain — bind NoopBleProximityTransport (capabilities all false)
```

`proximityPlatformModule` is added to `KoinModules.allModules` per platform; the commonMain `ProximityModule` holds the ViewModel/repository/session only.

**Why a custom seam, not a library:** no maintained KMP BLE library (Kable, BlueFalcon, Nordic KMM) supports the **peripheral/advertising** role on **both** platforms — they are central-only. (Research-confirmed.)

### 4.3 Discovery vs. data — the cross-platform rule
- The **advertisement carries only the discovery service UUID** (one fixed 128-bit UUID). Lowest common denominator that works iOS↔Android in the foreground.
- The **20-byte payload + ECDH pubkey are obtained via GATT** (`connectAndRead`). This is the **single canonical path** (research-confirmed for iOS, which forbids settable service data). **v1 always GATT-reads** — the Android "inline service-data fast-path" is dropped (it adds a second payload producer + a security-test surface for an Android-only branch; the GATT path is needed for iOS regardless). `BleDiscovery` carries no inline payload.
- **Radar identity is keyed on the resolved `session_id`/token, NOT `deviceId`.** The advertiser's MAC (Android) and `CBPeripheral.identifier` (iOS) are unstable across the mandated 60–120 s rotation; keying the UI on `deviceId` would split one receiver into multiple stale blips. `deviceId` is used only as the ephemeral GATT-connect handle.

---

## 5. Security Protocol (normative)

### 5.1 On-air payload (exactly 20 bytes)

| Offset | Len | Field | Notes |
|-------:|----:|-------|-------|
| 0 | 1 | `version` | `0x01` |
| 1 | 1 | `device_class` | low 3 bits: `0x01`=P2P, `0x02`=DONATION, `0x03`=POS; high 5 reserved. **Hint only** (§5.6) |
| 2 | 1 | `flags` | bit0 donation/HMAC-windowed token · bit1 POS-attested (Phase 3) · bit2 ECDH-handshake required · bits3–7 reserved |
| 3 | 16 | `ephemeral_token` | 128-bit opaque, rotating |
| 19 | 1 | `crc8` | CRC-8 of bytes 0–18 (integrity/parse sanity, **not** security) |

**MUST NOT appear on the air:** display name, amount, currency, account number, pay-link/barcode, phone/email, photo, any PII or PII-hash, any static device identifier, the human handle. (The handle is **derived locally** from the live ECDH shared secret — §5.7 — so it is never broadcast.)

### 5.2 Token rules (unified windowed-HMAC derivation)
**All classes derive the rotating token on-device from a server-issued key — no per-rotation round-trip.** (v2 said P2P was "CSPRNG per token", which cannot rotate without a server call every window; unified here.)
- **Token derivation (all classes)**: `token = trunc16(HMAC_SHA256(token_key, floor(unixtime / W)))`, where `trunc16` = the **first 16 bytes (128 bits)** — *bytes, not bits*. `token_key` (32 B) and `W` are issued at session start.
- **P2P / POS**: `token_key` + `W = 90 s` from `POST /proximity/session`. Session hard-`exp` bounds the whole flow; **single-settle per `session_id`** (§5.9) is the replay defense, not just TTL.
- **DONATION**: `token_key` (named `K`) + `W = 60 s` from `POST /proximity/provision-donation`. `K` is **per-session, never per-device**, high-entropy, stored in Android Keystore / iOS Keychain, **never logged**, **rotated hourly** via re-provision, and instantly revocable server-side via a `session_active` flag checked at resolve.
- **Window acceptance**: server accepts the **current and previous** window (`±1`, i.e. token for window `N` and `N-1`). This absorbs the 1–3 s GATT-read + network latency (§6.3) that would otherwise 404 a legitimate read straddling a boundary; single-settle/idempotency makes the overlap harmless. (v2's `±0` produced field failures near boundaries — corrected.)
- **MAC/timing jitter**: the **BLE MAC address** rotates ~15 min (platform) and P2P advertising timing may jitter; the **token itself is deterministically aligned to `W` boundaries** (no jitter) so the server's window index can reproduce it. Token rotation (every `W`) + MAC rotation together defeat passive linkage/tracking.

### 5.3 Resolve scaling (no O(N) HMAC scan)
The backend maintains a **precomputed reverse index** for every active session (P2P, POS, donation): each window it computes the token for the **current and previous** window and inserts `token → session_id` into a hash map (refreshed every `W`). Resolve is an **O(1)** lookup, not an O(active-sessions) HMAC sweep. ~2 entries per session per refresh.

### 5.4 Resolve → signed display payload (PII-minimizing, server-verifiable proximity proof)
- Sender calls `POST /proximity/resolve { token, link_proof? }` over TLS (authenticated). **No `coarse_loc`** (it built a sender location dataset for negligible relay benefit; see §5.5).
- **Two-tier disclosure to avoid a PII oracle:**
  - **Pre-connect / radar tier** (`link_proof` absent): returns **initials + verified badge + amount *band* only** — never the full name. Rate-limited per account.
  - **Connected tier** (`link_proof` present **and matched**): returns the full `display { session_id, token, name, photo, amount, currency, device_class, merchant_verified, iat, exp }`, **Ed25519-signed** (the **handle is NOT in this payload** — it is derived locally on both ends, §5.7). Full PII is released only to a caller that genuinely completed the live ECDH.
- **How the server verifies `link_proof` without holding the secret (corrected from v2):** the server is **not** a DH party and cannot recompute the shared secret. Instead the **receiver** — which also completed the ECDH — registers its proof via `POST /proximity/session/bind { session_id, link_proof, ttl }`. The sender's `/resolve` `link_proof` is then matched by **byte-equality** against the receiver-registered proof for that session. Equality ⇒ both completed the *same* live ECDH ⇒ proximity proven. The server never learns `shared` (it sees only the one-way proof from both sides). `link_proof = HKDF(IKM=shared, salt=receiver_session_PUBLIC_key, info="simplipay/proximity/link-proof/v1", L=32)` — session-fixed salt (not the rotating token) so receiver and sender always agree.
- The signed-payload public key is **pinned in the app as a key-ring** (§5.8); the signed blob is an **opaque byte string** (verify signature over raw bytes, *then* decode — sidesteps JSON canonicalization). Client MUST verify signature + `kid` before rendering. Unknown/expired token → `404`, dropped silently. `link_proof` supplied but unmatched → stays initials tier / `403`. Rate-limited → `429`.
- **This is where name + amount come from** — over TLS, signed, never from the air.

### 5.5 Threat model (residual-risk view, corrected)

| Threat | Mitigation | Residual |
|--------|-----------|:--------:|
| Eavesdrop (T1) | only opaque token on air; **full name gated behind GATT proximity proof** (§5.4) | Low |
| MITM/tamper (T2) | opaque token + server-signed resolve | Low |
| Replay (T3) | 90 s TTL, skew ≤ TTL, **single-settle per `session_id`** (§5.9) | Low |
| **Relay/wormhole (T4)** | handle bound to **live ECDH** (defeats token-rebroadcast relay) + **mandatory receiver-accept above threshold** (two-sided) + identity-first UX. RSSI/geo catch only gross long-distance relays. **True relay resistance needs UWB (Phase 4).** | **Med** |
| Spoof/impersonate (T5) | token maps to authenticated receiver record; signed payload + pinned key | Low |
| Tracking/linkability (T6) | MAC randomization + token rotation 60–120 s + locally-derived (never broadcast) handle | Low |
| DoS (T7) | client caps list; backend rate-limits resolve; O(1) donation index (§5.3) | Low |
| **Wrong-person UX (T8)** | identity-first confirm (photo+name) + handle compare + slide gesture | Med→Low |
| Repudiation (T9) | signed audit + idempotent `payment_intent_id` | Low |
| POS class abuse (T10) | **Phase 3** — pre-provisioned + attested (Play Integrity/App Attest). Not in v1 trust model. | Phase 3 |

**Honest statement (normative):** the match handle is a **wrong-person (T8)** check and — once bound to live ECDH — a **token-rebroadcast relay** check; it is **not** a cryptographic defense against a live two-ended radio proxy. "Server geo/velocity" catches only gross long-distance relays; **RSSI is a UX hint, not a security control.** Full relay resistance is deferred to UWB distance-bounding (Phase 4); v1 mitigates relay operationally via two-sided human confirmation. **T4 residual is Med, not Low.**

### 5.6 Trust anchor
BLE is **untrusted transport**. Trust = the **server Ed25519 signature** over the resolve payload, key pinned in the app. The on-air `device_class` byte is a **hint only** (lets the radar pre-filter/icon and selects the donation HMAC parse path); the authoritative class is in the signed payload; mismatch → trust signed payload, flag locally. **No mandatory BLE pairing/LESC in v1.**

### 5.7 Two visual identifiers: session **badge** (pick-the-right-person) vs match **handle** (anti-relay)
v2 conflated one "handle" that was defined two incompatible ways and could not be the single value a receiver shows (a pairwise secret differs per sender). They are **two distinct objects** with different security properties; both are **derived locally and never broadcast**:

**A. Session BADGE** — one per session, identical for every sender, **session-stable**.
- The receiver holds **one session-fixed** ephemeral X25519 keypair per session (rotated only when the session/`K` rotates — e.g. donation's hourly re-provision — **not** per token window, so the badge does not flicker at window boundaries).
- `badge = symbolMap(HKDF(IKM=receiver_session_PUBLIC_key, salt=session_id, info="…/badge/v1")[:20 bits])`. Salted with the **session_id** (session-stable), not the rotating token.
- The receiver displays it from advertise time (it has its own pubkey + session_id). A **sender** can only compute/match it **after connecting** (it needs the receiver pubkey from `TX_PUBKEY`), so the badge appears on the sender side in the **confirm sheet**, not on pre-connect radar rows (§8.4).
- **Security role: wrong-person (T8) aid only.** It is **NOT relay-proof** (a rebroadcast relay copies the public key + session_id and reproduces it). UI MUST label it as a pick-the-right-person cue, never as anti-relay.

**B. Match HANDLE** — pairwise, per-connection, revealed on **both** screens only in the escalated (above-threshold) confirm; **P2P only**.
- `handle = symbolMap(HKDF(IKM=ecdh_shared, salt=receiver_session_PUBLIC_key, info="…/match/v1")[:20 bits])`. **Salted with the session-fixed receiver pubkey, NOT the rotating token** — both `shared` (sender ephemeral × receiver *session-fixed* key) and the salt are constant for the connection, so the two devices always agree even if the on-air token rotates mid-handshake (the rotating token salt caused ~3–5% boundary mismatches).
- A token-rebroadcast relay is **not** in a live ECDH with the real receiver → cannot reproduce the handle → mismatch → abort. This is the value that resists the **passive rebroadcast relay**.
- **DONATION drops the visual match handle** (pairwise secrets can't be shown on one many-payer screen); it relies on amount/tally + mandatory receiver-accept (§5.10).

**Commitment round (REQUIRED — a 20-bit SAS without it is grindable).** Plain ECDH-then-compare lets an active 2-key MITM **grind** its ephemeral keypair (~2²⁰ ≈ 10⁶ ops, seconds) until its handle matches the receiver's on-screen handle. Prevent it with commit-before-reveal (the ZRTP/Numeric-Comparison construction):
```
1. Receiver picks session-fixed (epr_priv, epr_pub); publishes C_r = HKDF(epr_pub, info="…/commit/v1")
   on COMMIT char (session-fixed; no rotating token in it). The receiver WITHHOLDS epr_pub (TX_PUBKEY)
   until that central has written RX_PUBKEY (normative — this withholding is what makes grinding impossible).
2. Sender reads C_r; picks (eps_priv, eps_pub); WRITES eps_pub to RX_PUBKEY.
3. Receiver (this central's key now fixed) REVEALS epr_pub on TX_PUBKEY.
4. Sender reads TX_PUBKEY; verifies  HKDF(epr_pub, info="…/commit/v1") == C_r  (else ABORT — receiver
   adapted its key). (PAYLOAD/token is read in the same connection for resolve, but is NOT an input here.)
5. Both derive shared = X25519(...) and handle = symbolMap(HKDF(shared, salt=epr_pub, info="…/match/v1")[:20]).
```
Because each side is committed before learning the other's key, neither can grind; the MITM is reduced to a blind 2⁻²⁰ guess (~1-in-a-million, acceptable). All bilateral values (`C_r`, `handle`, `link_proof`) use **session-fixed inputs only** (`epr_pub`/`shared`), never the rotating token, so the two devices can never disagree at a window boundary. The **server is not involved** in deriving either identifier — both ends compute identically and humans compare. (A *transparent* live proxy still passes; only UWB distance-bounding defeats that — Phase 4.)

**Per-central read gating (normative, both platforms):** the receiver tracks, per connected central, whether it has written `RX_PUBKEY`, and refuses `TX_PUBKEY` reads until it has. This is expressible on both stacks — Android `BluetoothGattServer.onCharacteristicReadRequest` and iOS `peripheralManager(_:didReceiveRead:)` both deliver the requesting central — and on iOS **`COMMIT`, `TX_PUBKEY`, and `PAYLOAD` MUST all be `value = nil`** (dynamic), or CoreBluetooth answers them from cache without invoking the delegate and the withholding/rotation silently breaks (§6.1).

**Domain separation & hygiene (normative):** always run the raw X25519 output through **HKDF** (never `shared[:n]` directly); use **distinct `info` strings** per purpose — `…/badge/v1`, `…/match/v1`, `…/link-proof/v1` — so the shared secret is never reused across roles. Never use a bare `H(a‖b)`; use HKDF/HMAC. The GATT link is intentionally **not confidential** and the ECDH is intentionally **unauthenticated** — the human compare *is* the authentication; the token is not secret.

### 5.8 Signing-key rotation (root-chain primary; ring fail-closed is *safe* but not *available*)
Fail-closed on an unknown `kid` is the security-correct direction, but "the ring guarantees overlap" is **only safe, not available**: a key added in app v5 is absent from v4's pinned ring, so if the server signs with it while v4 users are live, **v4 fails closed → proximity silently breaks** for the update tail. Two normative rules:
- **Operational rule for the ring:** the server MUST sign only with a `kid` present in the pinned ring of the **current minimum-supported app version** (enforced via force-update / kill-switch), not merely "a `kid` in some ring." A new operational key is activatable only after the min-supported version that includes it is enforced.
- **Primary mechanism = root-chain (promoted from v2's "optional future"):** pin one long-lived **root** Ed25519 key; the root signs short-lived **operational** signing keys (a 2-cert chain delivered alongside the signed payload). Operational keys then rotate with **no app update** and no `kid`↔population coupling — the only design that rotates safely across an unbounded update tail. The pinned ring of operational keys remains a fallback for clients on the pre-root-chain version.

### 5.9 Confirm → settlement (single-settle, amount-integrity)
- Sender calls `POST /proximity/confirm { session_id, token, payment_intent_id, amount, link_proof }`.
- **Single-settle per `session_id`** for P2P: the lock is acquired **on settle** (a below-threshold confirm, or an above-threshold `/accept`), **not** on a `pending_receiver` confirm — a pending confirm is **non-exclusive** and is released on reject/expire, so a hostile or wrong-person above-threshold attempt cannot brick the session for the real payer. Once settled, later confirms (even with a different `payment_intent_id`) return `409 already_settled`. This closes the double-settle race (two senders both paying one fixed P2P request). `payment_intent_id` (client 128-bit nonce) additionally guarantees **idempotency** (a retried confirm returns the original result, never double-charges).
- **Revocation/expiry is re-checked at `confirm` and `accept`, not only at resolve** — a sender who resolved seconds before a session was stopped or a donation `K` revoked cannot still settle (`410 session_expired`).
- **Fixed amount:** server re-validates `amount` equals the session's bound amount; mismatch → reject.
- **Open amount:** there is nothing server-side to validate against, so open-amount integrity is **user-attested**: the amount rendered immediately above the slide MUST be byte-equal to the `amount` submitted, and a **signed quote** is required — the sender first POSTs the intended amount to `POST /proximity/quote`, gets back a short-lived **signed** `{session_id, amount_minor, currency, iat, exp}`. The confirm request carries that opaque `signed_quote` (the v2 confirm body omitted it — corrected); the server re-verifies its signature, `exp`, and that `amount` matches before settling. Above the receiver-accept threshold (§5.10) the receiver also sees and approves the exact amount — and because the human-accept latency can exceed a short quote `exp`, an escalated OPEN payment **freezes the quoted amount into the `pending_receiver` record at confirm time** (the server validates the quote once, at confirm, then no longer requires a live quote during the accept wait) so a legitimate large gift can't fail with a stale-quote `410`.
- DONATION/POS: each payment independent; idempotent; no single-settle lock.

### 5.10 Two-sided confirmation (mandatory anti-relay backstop)
A **server-configurable amount threshold** (default e.g. R500) gates a **mandatory receiver-side accept**: above it, `confirm` returns `pending_receiver`; the receiver's device shows "**X is paying you R___ — that's me ✓ / Not me ✗**" and must approve (`POST /proximity/accept`) before settlement. Always required for POS/DONATION large gifts. Below threshold the happy path is one-sided (slide only) for speed. A wormhole victim (distant receiver) gets an accept prompt for a sender they cannot see → rejects → no settle.

**Inbound channel (the receiver is advertising, not the server's client — v2 had no way to deliver this):** while a session is active and foreground, `ProximitySession` holds a lightweight **long-poll / SSE** on `GET /proximity/session/{id}/events` (1–2 s latency budget) that surfaces `pending_receiver`, `settled`, `rejected`, `expired` → populates `ReceiveState.pendingAccept`. A FCM **data message** is a redundant wake. The BLE link cannot carry this (it's sender↔server, not receiver↔server), and FCM-push latency alone is too slow for a live approval gate — hence the events channel is a first-class part of the transport/session layer, not just the existing settle notification (§7.3).

On receiver **background** or **Stop receiving**, the client calls `POST /proximity/session/stop` to **invalidate the server session** (not merely stop the radio), so nothing settles on a screen no one is watching.

### 5.11 Privacy notes
- Resolve telemetry is **minimized and ephemeral** (kept only as long as rate-limiting needs); the backend unavoidably can map token→receiver, so **resolve-on-tap is the default** (eager resolution limited to N=3 nearest, initials-tier only) to shrink the physical-proximity social graph the backend can observe. The fixed discovery service UUID is an unavoidable "a SimpliPay receiver is here" fingerprint — accepted residual.
- **CI privacy gate:** assert no advertisement exceeds 20 bytes or contains any sensitive field; assert two captures > 1 rotation apart are unlinkable by MAC or token.

---

## 6. Platform Constraints (the hard facts that shaped this design)

### 6.1 iOS advertising is a beacon; GATT characteristic MUST be dynamic
CoreBluetooth allows **only** `LocalName` + `ServiceUUIDs` in an advertisement (~28 B foreground), with **no settable service/manufacturer data** ⇒ token rides GATT, not the ad (§4.3). **Critical iOS behavior:** a `CBMutableCharacteristic` created with a **non-nil `value`** is cached and answered by the OS **without** calling the delegate — so `payloadProvider()` would run once and the token would **never rotate over the air**. The `PAYLOAD` characteristic — **and `COMMIT` and `TX_PUBKEY`** — MUST be created with **`value = nil`** (dynamic), forcing `peripheralManager(_:didReceiveRead:)`, where the actual sets `request.value = …` then `respond(.success)`. (A non-nil value is answered from the OS cache without the delegate, which would break token rotation *and* the per-central `TX_PUBKEY` withholding that the anti-grind commitment depends on, §5.7.) Ordering: `addService` **before** `startAdvertising`; both managers no-op until `...DidUpdateState == .poweredOn`, so `suspend fun startAdvertising/scan` **must await poweredOn**. **Atomic windowed read:** only the **token** rotates per window; `TX_PUBKEY` is session-fixed (§5.7). The sender records the window `w = floor(epoch/W)` when it reads `PAYLOAD` and **aborts + retries** if `floor(epoch/W)` advanced across the `TX_PUBKEY`+`PAYLOAD` read pair (else `token_N` could pair with a `C_r` committed over `token_{N+1}` → a spurious commitment-verify failure). No extra on-air window byte is needed — the §5.1 payload stays exactly 20 bytes; the window is reconstructed from the clock. CRC-8 of `PAYLOAD` is CRC-8/SMBUS (poly `0x07`, init `0x00`, no reflection).

### 6.2 Android can embed data (but we don't, v1)
`BluetoothLeAdvertiser` allows service/manufacturer data within the legacy 31-byte budget; our 20 bytes fit. **Not used in v1** (§4.3) — always GATT-read for a single uniform path. Never depend on Extended Advertising (not universally received; invisible to iOS scanners).

### 6.3 GATT reads cost ~1–3 s each
Never connect to every blip. Show list rows immediately (RSSI-ordered, initials), then **resolve nearest N=3 eagerly (initials tier)** and **full-resolve on tap**. Hold 1–2 GATT connections max. Distinguish a **pending** (slow GATT/network) state from a **failed** one (§12).

### 6.4 Foreground-only
Backgrounded iOS advertising drops the local name and hides the service UUID in an Apple "overflow" area **Android cannot read**; Android background scanning is throttled (≥5 starts / 30 s ⇒ silent stop). ⇒ A session requires both apps **foreground**. Keep the screen awake (`FLAG_KEEP_SCREEN_ON` / `isIdleTimerDisabled`) while advertising/scanning; communicate *why* ("Screen stays on so we can connect over Bluetooth"). On background: stop scan/advertise, invalidate receiver session (§5.10), fire a local notification "Tap to keep receiving."

### 6.5 Distance on the radar — estimated baseline (cross-platform) + precise (same-platform UWB)
**Product requirement:** the radar reflects how far each receiver/POS is, and tapping a blip shows the distance. Two tiers, auto-selected per pair:

- **Estimated distance (baseline, ALWAYS available, fully cross-platform iOS↔Android).** Derived from **smoothed RSSI** with the advertiser's calibrated TX-power (a 1-byte calibrated `txPower` reference MAY ride the GATT payload or be a per-model constant; the BLE "measured power @ 1 m" path, same maths as iBeacon `accuracy`). Radar positions blips by this estimate and the confirm sheet shows e.g. **"~2 m"**. **Honest accuracy:** RSSI distance is coarse and noisy (multipath, body-shadowing, device-model variance) — best at <1 m, roughly ±1–2 m in the 1–3 m band. So **display it as an estimate** ("~2 m", or near/here/in-the-room bands when confidence is low), **never as a precise or security-bearing value** (it does not gate payment; relay defense is §5.7/§5.10).
- **Precise distance (enhancement, opt-in, SAME-PLATFORM only).** Centimetre-accurate ranging when both ends are UWB-capable **and on the same OS family**: **iOS↔iOS** via `NearbyInteraction` (iPhone 11+ U1/U2), **Android↔Android** via `androidx.core.uwb` (UWB-capable devices, e.g. Pixel 6 Pro+, Galaxy S21+/Note). UWB is bootstrapped out-of-band over the existing BLE/GATT channel (exchange UWB config + the NI/UWB token). **There is no iOS↔Android UWB** (no cross-vendor profile), so a mixed pair silently uses the estimated tier. This is why precise ranging is an *enhancement*, not the baseline — the baseline MUST stay RSSI so an iPhone customer ↔ Android POS still gets a distance.
- **Relay note:** UWB also enables true **distance-bounding** (the only cryptographic relay defense, §5.5 T4) — but again only same-platform, so it can't be the cross-platform security baseline; it remains a Phase-4 hardening on same-platform pairs (notably **Android POS ↔ Android customer**, a common path).

`BleCapabilities` exposes `supportsPreciseRanging` so the UI can show "Precise" vs "Approx" on the distance readout.

### 6.6 Permissions (with priming)
- **Android 12+**: runtime `BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN` (+`neverForLocation`), `BLUETOOTH_CONNECT`. Pre-12 (minSdk 26): `ACCESS_FINE_LOCATION` (`maxSdkVersion=30`) for scanning. Use `accompanist.permissions` (already an androidMain dep).
- **iOS**: `NSBluetoothAlwaysUsageDescription` in `cmp-ios/iosApp/Info.plist` (**missing string = crash** — release gate). Prompt fires on first BLE use; iOS needs only the Info.plist key, no library.
- **Prime before the OS prompt** (§8.6): a benefit-first education screen ("Pay people right next to you… we never send your name, account, or location over Bluetooth"), then trigger the OS dialog. Pre-12 location copy must pre-empt the scare ("Android asks for Location to scan Bluetooth — SimpliPay does not use your location"). Request only the role needed (Sender: SCAN/CONNECT; Receiver: ADVERTISE).
- Build `BlePermissionState` via `expect fun rememberBlePermissionState()`, mirroring `CameraPermissionState`.

---

## 7. Pay-link Reuse & Backend

### 7.1 Reuse existing pay-link
The receiver already has a pay-link (`https://pay.simplipay.co.za/l/<id>`) and the `mpay://pay?...` payload via `MpayQrCodeProcessor`. The proximity **session binds that existing pay-link** to a freshly minted opaque token; settlement at `/proximity/confirm` drives the **same** server-side settlement the QR flow already uses. Proximity is a **new front door to existing rails**, not a new payment processor.

### 7.2 Backend endpoints (new)

| Endpoint | Purpose |
|----------|---------|
| `POST /proximity/session` | Receiver opens a P2P/POS session bound to pay-link + amount/open. Returns `{session_id, token_key, window_secs, exp, accept_threshold_minor}` — device derives the rotating token locally (§5.2). **No** handle/handle_seed (derived locally, §5.7). Single-settle. |
| `POST /proximity/provision-donation` | Issue `{session_id, K, window_secs, rotate_after_secs, exp}` for a long-lived donation beacon; hourly re-issue rotates `K`; revocable. |
| `POST /proximity/session/bind` | **Receiver** registers `link_proof` for the in-progress connection so the server can match the sender's proof by equality (§5.4). |
| `POST /proximity/resolve` | Token → **initials tier** (pre-connect) or **full signed tier** (with matched `link_proof`). Signed payload carries **no** handle. |
| `POST /proximity/quote` | (Open amount) intended amount → short-lived **signed** `{session_id, amount_minor, currency, iat, exp}`. |
| `POST /proximity/confirm` | Idempotent, **single-settle per `session_id`** `{session_id, token, payment_intent_id, amount, currency, link_proof, signed_quote?}` → `{status: settled\|pending_receiver\|rejected, settlement_ref}`. |
| `POST /proximity/accept` | Receiver approves/declines a `pending_receiver` confirm above threshold (§5.10). |
| `GET /proximity/session/{id}/events` | **Receiver** long-poll/SSE inbound channel: `pending_receiver` / `settled` / `rejected` / `expired` (§5.10). FCM data wake is redundant. |
| `POST /proximity/session/stop` | Invalidate the server session on background / Stop receiving (§5.10) — not just stop the radio. |

See `API.md` (companion file) for full request/response DTOs, encodings, error codes, the GATT profile, and threading.

### 7.3 Receiver notification
On settle, backend pushes via the **existing notification infra** (`NotificationRepository` + FCM/poll). The active `ProximitySession` also surfaces an in-app success animation. DONATION shows a running tally + per-payment alert. The settle notification carries a "Not who you expected? [Flag]" affordance (§12).

---

## 8. Screens & UI

### 8.1 Entry points (two doors, one graph) + device-capability gating
- **Send money → "Nearby 📡"** (alongside Contact / QR scan) → Sender radar/list. (Matches the user's "under Send money" ask.)
- **Request / Get paid → "Get paid nearby 📡"** (alongside show-QR / pay-link) → Receiver beacon.
- Both navigate into the single `feature/proximity` graph (`Mode.RadarScan` vs `Mode.Receive`). User-facing label is **"Nearby"**, never "Proximity" (jargon).
- **Device-capability gating (probed at runtime via `BleCapabilities`):**
  - Entry shows **enabled** when the device can perform the relevant role (Sender needs `canScan`; Receiver needs `canAdvertise`).
  - When the role isn't available (most desktops, all web, a phone with no BLE), the entry is rendered **grayed-out/disabled** with a small inline **"Device not capable"** banner, and a secondary **"Use QR instead"** CTA routes to `mpay-qr`. Bluetooth merely *off* (`isBluetoothOn=false`) is **not** "not capable" → show "Turn on Bluetooth" instead (§12).
```
┌─ Send money ─────────────────────────┐
│  [ Contact ]  [ QR scan ]            │
│  [ 📡 Nearby ]  ← enabled            │     …or, on an incapable device:
│                                       │     │ 📡 Nearby            (grayed) │
│                                       │     │ ⓘ Device not capable          │
│                                       │     │ [ Use QR instead ]            │
└───────────────────────────────────────┘

### 8.2 Receive — mode + amount
```
┌─────────────────────────────────────┐
│  ←         Get paid nearby            │
├─────────────────────────────────────┤
│   How much do you want to receive?    │
│   ┌───────────────┐ ┌───────────────┐ │
│   │   ● Open      │ │   ○ Fixed     │ │  ← segmented
│   │ Pay-what-you  │ │ Set an amount │ │
│   └───────────────┘ └───────────────┘ │
│   (Fixed) ┌─────────────────────────┐ │
│           │  R  [   120.00   ]  ZAR │ │  ← Geist Mono, tabular figures
│           └─────────────────────────┘ │
│        [   Start receiving   ]        │  ← jade glow button
└─────────────────────────────────────┘
```

### 8.3 Receive — live beacon (swappable bottom region for DONATION forward-compat)
```
┌─────────────────────────────────────┐
│  ←        You're receiving            │
├─────────────────────────────────────┤
│            ((( sonar pulse )))        │  ← decorative ParticleField
│              ◢ R 120.00 ◣             │  ← mono amount (or "Open")
│         Your badge:  🟢🦊             │  ← session BADGE (pick-the-right-person, §5.7)
│   ┌── swappable region (mode param) ─┐ │
│   │ P2P:  👀 Someone's about to pay  │ │  ← liveness when a sender connects
│   │ DON:  Today R1,240 · 18 gifts    │ │  ← Phase 2 tally + live feed
│   └──────────────────────────────────┘ │
│   Keep this screen open — we connect  │
│   over Bluetooth, like a QR that      │
│   finds itself.                       │
│        [   Stop receiving   ]         │
└─────────────────────────────────────┘
   above-threshold pay → "X pays you R___? [That's me ✓][Not me ✗]"
   on settle → success animation + notification, P2P auto-stops
   idle 3 min → "Still receiving? [Keep going]" (not silent death)
```

### 8.4 Send — Nearby (list-first; sonar decorative)
```
┌─────────────────────────────────────┐
│  ←            Nearby                  │
├─────────────────────────────────────┤
│        ·  ambient sonar  ·            │  ← decorative only, never a picker
│  Tap the person/till you're paying:   │   blips placed by ESTIMATED distance
│  ┌─────────────────────────────────┐  │   (RSSI, cross-platform §6.5); rows
│  │ T.M.  ~R100–500     ~0.8 m       │  │   show the same estimate, nearest 1st
│  │ L.K.  ~R0–100       ~2 m         │  │   (pre-connect: initials + amount band
│  │ ····  resolving…    ~in the room │  │   + distance only — NO badge until the
│  └─────────────────────────────────┘  │   sender connects, §5.7/§6.3)
│  Both on this screen & close together?│  ← empty/again coaching
└─────────────────────────────────────┘
  distance is an estimate (~m), or "Precise" when both ends are UWB-capable same-platform (§6.5)
  hold a row → buzzes their phone ("Someone's looking at you") to confirm it's them
  tap a row → connect + full resolve → confirm sheet shows photo + name + BADGE + distance
```

### 8.5 Confirm sheet — identity-first; pairwise match-handle only as escalation
> Default sheet shows the session **badge** (pick-the-right-person). The escalation tier reveals the **pairwise match handle** (the relay-resistant one, §5.7), which appears on both screens *after connect* and is distinct from the badge. P2P only.
```
DEFAULT (any amount, identity-first)        ESCALATED (> threshold) adds:
┌──────────────────────────┐                ┌──────────────────────────────┐
│        ( photo )          │                │  Match handle:  🟣🦉          │
│        Thabo M.   🟢🦊     │                │  ↳ same on their screen now?  │
│      ✔ Verified · Personal│                │  (receiver also taps accept)  │
│        ~0.8 m away        │  ← distance (Approx/Precise, §6.5)
│        R 120.00           │  ← Fixed: read-only, no input chrome
│   (Open) [ R 0.00  ⌨ ]    │  ← Open: primary numeric input, slide gated >0
│   [+10%] [+15%] [Round up]│  ← optional tip chips (Open/POS/donation)
│   ((●))  slide to pay  →   │  ← SlideToConfirm
│   Paying via SimpliPay     │
└──────────────────────────┘
```
- Amount: right-aligned, decimal-locked 2 places, currency symbol fixed-width-spaced so digits don't jitter while typing (tabular Geist Mono).
- A GATT drop while this sheet is open → "Lost connection to Thabo — move closer and try again" (not a silent blip-drop).

### 8.6 First-run permission priming
Education screen → OS Bluetooth/permission prompt → first radar/beacon. Never lead with the OS dialog. Pre-12 location reassurance copy as in §6.6.

### 8.7 SlideToConfirm component (new, reusable)
- Full-width track; circular thumb anchored left; drag L→R; crossing ~85% triggers `onConfirmed()`.
- Below threshold release → spring back. Past threshold → snap to end, jade fill, haptic + checkmark morph, then `onConfirmed`.
- States: `Idle → Dragging(progress) → Confirmed → Loading → Done/Error`.
- Tokens: jade fill, ivory track, mono label; feel mirrors `Button.kt` ripple. Disabled until (Open) amount > 0 and (escalated) handle acknowledged.
- Accessibility: exposes an explicit **button + confirm-dialog** fallback (NOT long-press — long-press is reserved for radar "hold to ping", §8.4); content description; honors reduce-motion.

---

## 9. State Model (commonMain)

```kotlin
@Serializable
data class ProximityState(
    val mode: Mode = Mode.Chooser,
    val capability: Capability = Capability.Unknown,
    val permission: BlePermissionStatus = BlePermissionStatus.Unknown,
    val foreground: Boolean = true,                // drives stop on background (§6.4)

    val receive: ReceiveState = ReceiveState(),    // persisted config only

    @Transient val discoveries: List<RadarItem> = emptyList(),   // live, not persisted
    @Transient val selected: ResolvedReceiver? = null,
    @Transient val confirm: ConfirmState? = null,
    val dialog: ProximityDialog? = null,
) {
    enum class Mode { Chooser, Receive, RadarScan }
    enum class Capability { Unknown, Supported, NoBluetooth, Unsupported /*→QR*/ }
}

@Serializable
data class ReceiveState(
    val amountMode: AmountMode = AmountMode.Open,
    val amountMinor: Long? = null,
    val sessionId: String? = null,
    val advertising: Boolean = false,
    @Transient val badge: Handle? = null,            // session badge (per-session, derived, not persisted)
    @Transient val lastPayment: SettledPayment? = null,
    @Transient val pendingAccept: PendingAccept? = null,   // from /session/{id}/events (§5.10)
)

// Radar keyed on resolved session/token, NOT deviceId (§4.3)
data class RadarItem(
    val sessionKey: String?,        // resolved token/session id; null until resolved
    val deviceId: String,           // ephemeral GATT handle only
    val rssiSmoothed: Int,
    val ring: Ring,                 // Near | Mid | Far
    val resolved: ResolvedReceiver? = null,
)

data class ResolvedReceiver(
    val token: String, val name: String, val photoUrl: String?,
    val amountMinor: Long?, val currency: String,
    val deviceClass: DeviceClass, val merchantVerified: Boolean,
    val badge: Handle,                    // derived locally from receiver pubkey (not from server, §5.7)
    val matchHandle: Handle?,             // pairwise; non-null only after ECDH, escalation tier
    val signatureValid: Boolean, val expEpoch: Long,
)

sealed interface ProximityEvent {
    data class NavigateToSuccess(val ref: String) : ProximityEvent
    data class ShowError(val msg: String) : ProximityEvent
    data object NavigateToQrFallback : ProximityEvent
}

sealed interface ProximityAction {
    data object EnterReceive : ProximityAction
    data class SetAmountMode(val m: AmountMode) : ProximityAction
    data class SetAmount(val minor: Long) : ProximityAction
    data object StartReceiving : ProximityAction
    data object StopReceiving : ProximityAction
    data class ReceiverAccept(val accept: Boolean) : ProximityAction
    data object EnterRadar : ProximityAction
    data class HoldToPing(val deviceId: String) : ProximityAction
    data class SelectRow(val sessionKey: String) : ProximityAction
    data class SetPayAmount(val minor: Long) : ProximityAction
    data object SlideConfirmed : ProximityAction
    data object RequestPermission : ProximityAction
    data class LifecycleChanged(val foreground: Boolean) : ProximityAction
    data object Retry : ProximityAction
}
```
- Scan is `scanForReceivers()` collected via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)` → **one** upstream scan, auto-stops shortly after the screen leaves; restart debounced ≥6 s to respect the Android 5/30 s throttle (§6.4). Cancellation of the collecting scope **is** the stop (single authoritative stop path).
- Live discovery/confirm fields are `@Transient` so process-death restore doesn't resurrect stale blips/expired tokens.

### 9.1 `ProximitySession` state machine (receiver side)
```
Idle ──StartReceiving──► Minting ──/session ok──► Advertising(rotating token, holds events channel)
  Advertising ──sender connects (events: someone resolving)──► SenderConnecting (badge revealed)
  Advertising ──below-threshold settle (events: settled)─────► Settled ──(P2P)──► Idle (auto-stop)
  Advertising ──above-threshold (events: pending_receiver)──► PendingAccept
     PendingAccept ──ReceiverAccept(true)→/accept──► Settling ──► Settled
     PendingAccept ──ReceiverAccept(false)/timeout──► Advertising (declined)
  any ──background / StopReceiving──► Stopping ──/session/stop──► Idle
  any ──session exp / events:expired──► Expired ──► Idle
```
Token rotation is a timer sub-behavior of `Advertising`/`SenderConnecting`/`PendingAccept` (every `W`, §5.2). DONATION never auto-stops on settle (returns to `Advertising`, tally++).

### 9.2 Threading (transport → commonMain)
- iOS: construct `CBCentralManager`/`CBPeripheralManager` with a dedicated **serial** `dispatch_queue`; delegate callbacks `trySend` into the `callbackFlow`; retain managers + delegates as **strong properties** (else callbacks silently never fire, §6.1/§15).
- Android: pass an explicit `Handler`/executor to scan/GATT callbacks; `trySend` into the flow.
- The collecting ViewModel runs on `Dispatchers.Main.immediate`; `trySend` is thread-safe so no `flowOn` is required upstream.

---

## 10. Device-Class Handling

> **Role convention:** the **Receiver advertises** (it's the thing being walked-up-to and paid into); the **Sender scans** and pays. This holds for all three classes — including POS, where the **till is the advertising Receiver** ("the central payment device" the customer's radar points at) and the **customer's phone is the Sender**. ("Accept payments on the POS" = the POS is the money *recipient*; it does not send money.)

| Class | Advertises (Receiver) | Scans + pays (Sender) | Token | Lifetime | Trust UI |
|-------|------------------------|------------------------|-------|----------|----------|
| **P2P** | a phone | a phone | windowed-HMAC (`token_key`, `W=90 s`, O(1) index), single-settle | one payment → auto-stop | "Personal" (badge + match handle) |
| **DONATION** | one device | many phones | windowed-HMAC (`K`, `W=60 s`, K rotated hourly, O(1) index) | long-lived, many payments | "Donation" (badge only; no match handle) |
| **POS** | the till/terminal (merchant acct) | customer phone | windowed-HMAC (`token_key`, `W=90 s`, O(1) index); **Phase 3** device-bound/attested key | per-checkout (re-advertise per sale) | "Verified merchant" (Phase 3) |

- POS reuses the Receiver advertising path; an **Android POS ↔ Android customer** pair additionally unlocks UWB precise distance + distance-bounding (§6.5) — the recommended hardening for retail.
- The `device_class` air byte pre-filters/icons the list and selects the donation parse path; authoritative class comes from the **signed** payload; mismatch flagged.
- Reserved class values + `version`/`flags` reserved bits keep the protocol forward-compatible (future `TRANSIT`, `VENDING`).
- **POS attestation mechanism (Play Integrity / DeviceCheck / App Attest) is Phase 3** — the reserved byte/flag ship now; the mechanism does not.

---

## 11. Phased Delivery

| Phase | Scope | Notes |
|------:|-------|-------|
| **0** | Transport interface + per-platform Koin bindings + protocol codec + ECDH handshake + backend `session/resolve/quote/confirm/accept` | No UI; two-device integration test |
| **1** | **P2P** Receive (open/fixed) + Nearby list + identity-first Confirm + SlideToConfirm + receiver-accept threshold + notification | This release |
| **2** | DONATION (open beacon, tally, per-payment alert, `provision-donation`, hourly K rotation) | Protocol ready |
| **3** | POS Receiver (till app/profile advertises a checkout; attested keys; merchant-verified badge) | Protocol ready |
| **4** | UWB precise ranging + distance-bounding — **iOS↔iOS (U1) and Android↔Android (`androidx.core.uwb`)**; baseline RSSI distance ships in Phase 1 (§6.5) | Enhancement; same-platform only |
| **5** | **Cloud / remote-POS connector** (future): a backend/agent that pushes a transaction down to a **specific counter** and switches the proximity Receiver on for that counter's device; a thin **POS-device module** (incl. non-KMP OSes the till may run) that accepts payments. Leverages the existing server-minted session model (`/proximity/session` driven remotely, bound to a counter id). | Future; out of current scope |

---

## 12. Error Handling & Edge Cases

| Scenario | Behavior |
|----------|----------|
| **Device not capable** (no BLE adapter / desktop-no-central / web) | Entry **grayed-out** + inline **"Device not capable"** banner + "Use QR instead" → `mpay-qr` (§8.1). Distinct from Bluetooth merely off. |
| Bluetooth off (start) | Entry stays enabled; banner + "Turn on Bluetooth"; deep-link to settings (capability exists, adapter off) |
| Bluetooth off (mid-session) | "You went off-air — [Resume]" |
| Permission denied | Rationale sheet (primed copy) → settings; gated |
| Mixed-platform pair (iOS↔Android) | Works fully on RSSI **estimated** distance; "Precise" ranging simply not offered (no cross-vendor UWB, §6.5) — no error |
| No receivers found | "Both on this screen & close together? Ask them to tap Get paid nearby" |
| Token expired mid-flow | `404`/expired → "Request expired, ask again" |
| Signature/`kid` invalid | Drop the row; never render unsigned/forged entries |
| Handle mismatch | "Handles don't match — you might have picked the wrong person. Check who you mean to pay." (common case = wrong tap, not attack) |
| Amount tampered (Fixed) | Server re-check rejects at confirm → error, no charge |
| GATT drop on radar | Retry once; else drop row with soft error |
| GATT drop during confirm sheet | "Lost connection to Thabo — move closer and try again" (sheet stays) |
| Slow vs failed | Distinct **pending** state for resolve/confirm; never show slow as mismatch |
| Double-tap pay / retry | `payment_intent_id` idempotency + single-settle per session |
| Wrong person paid | Settle notification "Not who you meant to pay? [Report]"; receiver "Didn't expect this? [Flag]" |
| App backgrounded mid-session | Stop scan/advertise, invalidate receiver session, local notif "Tap to keep receiving" |

---

## 13. Test Plan

- **Unit (commonMain):** payload codec (encode/decode/CRC-8/SMBUS); windowed token derivation (`trunc16`=16 bytes); **commitment binds before reveal — a grinding attempt fails** (§5.7); badge from receiver pubkey vs match-handle from ECDH shared (distinct, correct domain-separated `info`); `link_proof` equality; X25519 + HKDF; Ed25519 verify over raw signed bytes (good/forged/expired/unknown-`kid`/root-chain); single-settle lock; state reducer.
- **Security gates (CI):** no advertisement > 20 bytes or any sensitive field; two captures > 1 rotation apart unlinkable; forged-signature rejected; replayed `payment_intent_id` returns original (no double-charge); second confirm on a settled `session_id` → 409; initials-tier resolve never returns full name without a **matched** `link_proof`; badge is reproducible from public data (documented non-secret) while match-handle is not.
- **Transport (instrumented, real devices):** iOS↔Android, Android↔Android, iOS↔iOS for advertise→scan→commit/reveal→atomic windowed read; **iOS dynamic-value rotation** (token changes per read); window-straddle read aborts+retries; MTU≥64 negotiation; foreground requirement; events-channel latency for receiver-accept; RSSI smoothing buckets.
- **UX:** SlideToConfirm threshold/spring-back/haptics/reduce-motion + non-long-press a11y fallback; list resolution laziness (no GATT storm; eager N=3 initials only); permission priming; Open vs Fixed amount entry; badge match (default) vs escalated match-handle compare.
- **E2E:** full P2P pay (open + fixed); above-threshold receiver-accept via events channel; notification on settle; expiry; **relay simulation** (distant beacon rebroadcast → match-handle mismatch + receiver-accept rejection block settle; transparent-proxy documented as Phase-4/UWB-only).

---

## 14. Resolved Decisions (was "Open Questions")

1. **Receiver-side accept?** YES, **mandatory above a server-configurable threshold** (default R500) and for POS/DONATION large gifts; below threshold one-sided slide for speed (§5.10).
2. **Handle/badge size?** 20 bits of entropy via a **color+emoji symbol pair** from a ≥1024 curated, colorblind-safe, culturally-neutral set — entropy in set size, not visible length. **Safe at 20 bits only because of the commitment round** (§5.7); without it a 20-bit SAS is grindable.
3. **GATT challenge-response in v1?** YES — the **commit/reveal ECDH handshake is required** (`flags` bit2); both the relay-resistant match handle and the `link_proof` PII gate depend on it (§5.7).
4. **Radar enrichment policy?** **Resolve-on-tap by default**, with eager resolution of nearest **N=3 at initials tier only** (privacy + battery, §5.11/§6.3).
5. **Where are badge & handle derived?** **Both locally** — badge from the receiver's session **public** key (shown to all), match handle from the live **ECDH shared secret** (pairwise, escalation tier). **The server is NOT involved** in either (v2's "server echoes the handle" was cryptographically impossible — the server holds no shared secret). The server only performs a **byte-equality** check of receiver- vs sender-submitted `link_proof` to gate full PII (§5.4/§5.7).

## 15. Build/Wiring Checklist (must-do or build breaks)
- [ ] `feature/proximity` added to `settings.gradle.kts`, to `cmp-shared/build.gradle.kts` `commonMain.dependencies`, and `ProximityModule` + `proximityPlatformModule` added to `KoinModules.allModules` per platform.
- [ ] Actuals/bindings present in **every** compiled leaf set: `androidMain`, `nativeMain`, `desktopMain`, `jsMain`, `wasmJsMain` (Noop binding for the last three) — or the module won't compile.
- [ ] **Crypto provider** for Ed25519 (verify) + X25519 (ECDH) + HMAC-SHA256 + HKDF in commonMain (e.g. `cryptography-kotlin`, or `expect/actual` over platform crypto) — none exists in the tree today. CRC-8 + symbol map are pure Kotlin.
- [ ] iOS: `NSBluetoothAlwaysUsageDescription` in `cmp-ios/iosApp/Info.plist`; CB characteristics `value=nil`; hold `CBCentralManager`/`CBPeripheralManager` + delegates as strong properties (else callbacks silently never fire); dedicated serial dispatch queue (§9.2).
- [ ] Android manifest: `BLUETOOTH_ADVERTISE/SCAN`(`neverForLocation`)`/CONNECT`, `ACCESS_FINE_LOCATION maxSdkVersion=30`; negotiate `requestMtu(64)`.
- [ ] **GATT profile UUIDs** (discovery service + `PAYLOAD`/`COMMIT`/`TX_PUBKEY`/`RX_PUBKEY`) assigned and shared (random 128-bit) — see `API.md` Appendix A.
- [ ] Backend: `/session`, `/provision-donation`, `/session/bind`, `/resolve`, `/quote`, `/confirm`, `/accept`, `/session/{id}/events`, `/session/stop` — see `API.md`.

---

## Changelog
| Date | Version | Change |
|------|---------|--------|
| 2026-06-27 | v1 | Initial spec from research synthesis. |
| 2026-06-27 | v2 | **Review-loop gen 1.** Match-code reclassified T8 (not T4); live-ECDH handle binding (§5.7); mandatory two-sided receiver-accept above threshold (§5.10); `/resolve` two-tier PII gating (§5.4); donation O(1) index + K rotation/revocation (§5.2-5.3); single-settle per session (§5.9); signed open-amount quote; Ed25519 key-ring rotation (§5.8); transport `expect class`→interface+Koin (§4.2); iOS dynamic-value + poweredOn (§6.1); drop Android inline fast-path (§4.3); radar keyed on resolved token (§4.3); identity-first confirm + color+emoji handle (§8.5); list-first disambiguation + hold-to-ping (§8.4); two nav doors + "Nearby" naming (§8.1); permission priming (§8.6); humane failure states (§12); crypto/wiring checklist (§15); T4 honestly Med. |
| 2026-06-27 | v3 | **Review-loop gen 2 (crypto correctness).** Fixed impossible server-side `link_proof` verification → receiver-registered proof + server byte-equality (`/session/bind`, §5.4). Split the one "handle" into **session badge** (public-key-derived, T8 aid, shown always) vs **pairwise match handle** (ECDH, relay-resistant, P2P/escalation only) — DONATION drops the visual handle (§5.7). Added **commitment round** (commit-before-reveal) so the 20-bit SAS isn't grindable (§5.7). Unified P2P token to **windowed-HMAC** `token_key` derivation (rotates without round-trips) + `±1` window + current/previous reverse index (§5.2-5.3). Root-chain promoted to primary signing-key mechanism (§5.8). `signed_quote` wired into confirm (§5.9). Added inbound **events channel** for receiver-accept + `/session/stop` (§5.10). Concrete **GATT profile** + atomic windowed read + MTU≥64 + CRC-8/SMBUS (§6.1). HKDF + domain separation everywhere (§5.7). `ProximitySession` state machine + threading (§9.1-9.2). Companion **`API.md`** authored. |
| 2026-06-27 | v4 | **Review-loop gen 3 (convergence/consistency).** Handshake reordered so the token is read **before** the commitment verify (§3, §5.7, API Appendix A). Badge made **session-stable**: salted with `session_id` not the rotating token, from a **session-fixed** receiver key (§5.7, API Appendix C). Badge gated to **post-connect** on the sender side; pre-connect radar rows show initials + amount band + RSSI only, no badge (§8.4). Dropped the contradictory on-air window-tag byte — window reconstructed from the clock; payload stays exactly 20 B (§6.1, API Appendix A). Receiver **withholds `TX_PUBKEY`** until `RX_PUBKEY` written, stated normatively (§5.7, API Appendix A). TL;DR no longer implies a handle in the signed payload (§0). Story 3 de-conflated badge/relay (§2). POS token row → windowed (§10). `ReceiveState.badge` comment per-session (§9). |
| 2026-06-27 | v6 | **Product-clarification pass.** Explicit cross-platform iOS↔Android requirement (§1.1a). **Desktop best-effort** (Sender-only where OS BLE central is reachable) + **runtime capability probe** → **"Device not capable"** grayed-out entry/banner when BLE absent (§1.1a, §4.1, §8.1, §12). Radar now **shows distance** — RSSI **estimated** baseline (cross-platform) + **UWB precise** when same-platform-capable; honest accuracy; iOS NearbyInteraction **and Android `androidx.core.uwb`** (matters for Android POS) (§6.5, §8.4-8.5). `BleCapabilities.supportsPreciseRanging` added (§4.2). **POS role corrected**: the till is the advertising **Receiver** (customer phone = Sender) — "the central device the radar points at" (§10). Added **Phase 5 cloud/remote-POS connector** (push a sale to a specific counter; thin POS module incl. non-KMP OSes) (§1.2, §11). |
| 2026-06-27 | v5 | **Review-loop gen 4 (final sign-off, "GO-WITH-FIXES") + gen 5 (architect pass).** iOS keystone: `COMMIT` + `TX_PUBKEY` (not just `PAYLOAD`) MUST be `value=nil` or the cache bypasses token rotation and the per-central `TX_PUBKEY` withholding that the anti-grind commitment depends on (§5.7, §6.1, API App. A). **All bilateral crypto re-salted with the session-fixed receiver pubkey** (`C_r`, match handle, `link_proof`) — never the rotating token — eliminating the ~3–5% window-boundary mismatch (false "wrong person" / PII-gate failure) for legitimate pairs (§5.4, §5.7, API App. C). Commitment dropped the token (`C_r=HKDF(epr_pub, info=commit/v1)`, session-fixed). Single-settle lock acquired **on settle, not on a pending confirm** (pending is non-exclusive, released on reject/expire) so a hostile above-threshold attempt can't brick the session (§5.9). Revocation/expiry **re-checked at confirm & accept**, not only resolve (§5.2, §5.9). Escalated OPEN payment **freezes the quoted amount at confirm** so a slow human-accept can't hit a stale-quote `410` (§5.9). Per-central read-gating stated as expressible on both Android `BluetoothGattServer` and iOS `CBPeripheralManager` (§5.7). Status → **READY FOR IMPLEMENTATION**. |
