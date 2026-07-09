# Proximity Payment — MVP Plan (iOS first)

**Date:** 2026-07-02
**Owner:** mobile
**Spec (north star):** `claude-product-cycle/design-spec-layer/features/proximity/proximity-payment.md` (v6) + `API.md`
**Decisions (2026-07-02):** MVP reuses the existing pay-link / PayShap-on-us rails over BLE (no new backend now); build **iOS first** (the first platform we'll use), then Android; full signed-token backend + crypto is deferred to Phase 4; Donor (DONATION) later.

## Why (current state review)
The `feature/proximity` module is a **discovery-only scaffold**:
- **Android is inert** — binds `NoopBleProximityTransport`, and `AndroidManifest.xml` has **no** Bluetooth permissions.
- **No connection anywhere** — `connectAndHandshake` throws `NotImplementedError` (iOS) / unsupported (Noop). You can *see* a device (iOS advertise + scan work; LightBlue confirms) but can't connect, read, or pay.
- **No money path** — no `ProximityService`/`ProximityRepository`, no `ProximitySession`/`Handshake`/`SignatureVerifier`. The receiver's amount is never attached to the beacon.
- **UI is a placeholder** — plain `Column` of Material cards, emoji, raw CoreBluetooth UUID + "−67 dBm", no radar/sonar, rows not tappable, `SlideToConfirm` misused to "start receiving".

## MVP architecture
- **Advertise = service UUID only.** Receiver's identity/amount is served **over a GATT read**, not in the broadcast (privacy floor without the signed-token backend).
- **MVP payload (served over GATT):** receiver MSISDN + `amountMode` (Open/Fixed) + `amount` (minor units). Reuses the on-us identifier the Pay screen already uses.
- **Payment:** sender reads the payload → **navigates to the existing Pay flow prefilled** (mode=NUMBER, phone, amount) → the standard confirm + mandatory re-auth + send + polling + result. No new money path; behaviour stays consistent with normal sends.
- **Security tradeoff (explicit):** no server-signed token / ECDH badge / relay defense yet — Phase 4. Transport interface keeps the handshake hooks so Phase 4 slots in.

## Phase 1 — iOS transport (this increment)
- `transport/ProximityPayLink.kt` (commonMain): compact codec for {phone, amountMode, amount}.
- `IosBleProximityTransport`: add a `CBPeripheralManager` GATT **service + readable characteristic** that responds to reads with `PeripheralHandshake.currentPayload()`; implement `connectAndHandshake` (central connect → discover service/char → read → return payload). Keep managers/delegates strongly held.
- `ProximityViewModel`: inject `UserPreferencesRepository`; Receive builds+serves the payload; Send tap → `connectAndHandshake` → decode → emit navigate-to-Pay(phone, amount).
- `PayRoute`/`navigateToPay` + `PayScreen`/`PayViewModel`: accept optional prefill (phone, amount).
- UI redesign (commonMain, benefits all platforms): radar Nearby (SonarBackdrop via existing `ParticleField`, tappable rows, initials + amount + human distance), cleaner Receive beacon (Open/Fixed + amount + live state), remove misused slide.
- **Verify:** compiles + iOS builds. Real BLE needs **two physical iPhones** (the iOS simulator has no BLE radio → shows "not capable").

## Phase 2 — Android transport
`AndroidBleProximityTransport` (advertiser + scanner + GATT server/client), manifest BLE permissions + runtime permission flow, real `BleCapabilities`. Bind it (replace Noop).

## Phase 3 — Polish
Confirm-sheet polish, distance smoothing, empty/permission states, haptics.

## Phase 4 — Harden to full spec
`/proximity/*` endpoints, Ed25519 verify, X25519 commit/reveal, badge + match handle, receiver-accept above threshold.

## Later — Donor (DONATION)
One-receiver / many-payers tally beacon (spec Phase 2).

## Risks
- iOS Kotlin/Native CoreBluetooth (strong-ref delegates, `value=nil` dynamic reads, `poweredOn` timing); GATT server is the new surface.
- Simulator can't test BLE — device testing required.
- BLE payload/MTU — keep payload compact.
