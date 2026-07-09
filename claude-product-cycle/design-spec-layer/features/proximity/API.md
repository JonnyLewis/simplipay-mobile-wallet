# Proximity Payment — Backend API & GATT Profile

> Companion to `proximity-payment.md` (spec v5). Resolves field-level encodings, error codes,
> the receiver-accept inbound channel, the GATT profile, and threading.
> Items marked **[FLAG]** call out a spec decision/ambiguity and the contract chosen.

---

## 0. Conventions (apply to every endpoint)

- **Base URL**: same host as the self-service API (`SelfServiceApiManager`). TLS only.
- **Auth**: `Authorization: Bearer <self-service JWT>` on every endpoint. The authenticated
  principal is the *caller* — receiver for `session`/`provision-donation`/`session/bind`/`accept`/`events`/`session/stop`; sender for `resolve`/`quote`/`confirm`. Missing/invalid → `401`.
- **Content-Type**: `application/json; charset=utf-8`.
- **Money**: integer **minor units** `amount_minor: Long` + ISO-4217 `currency: String` (e.g. `"ZAR"`). Never floats. (Matches `ProximityState.amountMinor`.)
- **Time**: `iat` / `exp` / `*_secs` are **epoch SECONDS** (`Long`).
- **Binary blobs** (token, keys, signatures, proofs): **base64url, no padding** (`String`).
  - `token` 16 B → 22 chars · `signature` Ed25519 64 B → 86 chars · X25519 pubkeys 32 B raw little-endian (RFC 7748) · `link_proof` 32 B (HKDF-SHA256) · `payment_intent_id` 16 B.
- **device_class**: wire string enum `"P2P" | "DONATION" | "POS"` (maps to on-air int, spec §5.1).
- **Signed payloads**: server returns `{ signed_payload, signature, kid }` where `signed_payload`
  is base64url of the **exact UTF-8 bytes the server signed**. **Clients verify the Ed25519
  signature over those raw bytes BEFORE decoding** — avoids JSON-canonicalization ambiguity.
  `kid` selects the key from the pinned ring / root-chain (spec §5.8); unknown/expired `kid` → fail-closed.
- **Idempotency**: state-changing calls accept an `Idempotency-Key` (UUID) header → original result on retry. `confirm` is additionally idempotent on `payment_intent_id`.
- **Rate-limit headers** `X-RateLimit-Remaining`, `Retry-After` on every endpoint.

```kotlin
@Serializable
data class ApiError(
    @SerialName("error") val error: String,           // machine code e.g. "already_settled"
    @SerialName("message") val message: String? = null,
    @SerialName("retry_after_secs") val retryAfterSecs: Long? = null,
)
```

| HTTP | code | meaning |
|---|---|---|
| 400 | `bad_request` | malformed/missing field |
| 401 | `unauthorized` | missing/invalid bearer |
| 403 | `forbidden` | caller not session owner / class not permitted / proof unmatched |
| 404 | `unknown_token` / `unknown_session` | unknown **or** expired token (returned identically; client drops silently, spec §5.4) |
| 409 | `already_settled` | second confirm on a single-settle session (spec §5.9) |
| 410 | `session_expired` | session/quote TTL elapsed or invalidated |
| 422 | `amount_mismatch` | confirm amount ≠ bound/quoted amount |
| 429 | `rate_limited` | throttled; `Retry-After` set |

---

## 1. POST /proximity/session — open a P2P/POS receive session

Binds an existing pay-link + amount config to a windowed-token session. **[FLAG]** Token is not
returned; the device derives it locally each window as
`token = base64url(trunc16(HMAC_SHA256(token_key, floor(epoch/window_secs))))` (spec §5.2), and the
server keeps an O(1) reverse index over current+previous window (spec §5.3). **No** `handle`/`handle_seed`
(badge & match-handle are derived locally, spec §5.7).

### Request
```kotlin
@Serializable
data class SessionRequest(
    @SerialName("pay_link_id") val payLinkId: String,
    @SerialName("device_class") val deviceClass: String,        // "P2P" | "POS"
    @SerialName("amount_mode") val amountMode: String,          // "OPEN" | "FIXED"
    @SerialName("amount_minor") val amountMinor: Long? = null,  // required iff FIXED
    @SerialName("currency") val currency: String,
)
```
### Response 200
```kotlin
@Serializable
data class SessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("token_key") val tokenKey: String,              // base64url 32B HMAC key
    @SerialName("window_secs") val windowSecs: Long,            // =90 for P2P (spec §5.2)
    @SerialName("iat") val iat: Long,
    @SerialName("exp") val exp: Long,                           // session hard expiry
    @SerialName("accept_threshold_minor") val acceptThresholdMinor: Long, // spec §5.10
)
```
Errors: 400 (FIXED without amount), 403 (pay-link not owned / class not permitted), 401. Rate: 10/min/account.

---

## 2. POST /proximity/provision-donation — long-lived donation beacon

Class implicitly `DONATION`. Hourly re-issue rotates `K`; previous `K` revoked (`session_active=false`).
Token window is deterministic on `floor(epoch/60)`, **no jitter** (spec §5.2). **[FLAG]** acceptance is
current+previous window (`±1`), like P2P.

### Request
```kotlin
@Serializable
data class ProvisionDonationRequest(
    @SerialName("pay_link_id") val payLinkId: String,
    @SerialName("currency") val currency: String,
)
```
### Response 200
```kotlin
@Serializable
data class ProvisionDonationResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("k") val k: String,                    // base64url 32B; Keystore/Keychain, NEVER log
    @SerialName("window_secs") val windowSecs: Long,   // =60
    @SerialName("rotate_after_secs") val rotateAfterSecs: Long, // re-provision before this (≈3600)
    @SerialName("exp") val exp: Long,
)
```
Errors: 403, 401. Rate: 5/min/account.

---

## 3. POST /proximity/session/bind — receiver registers proximity proof

**[FLAG] Required so the server can gate full-PII resolve without holding the ECDH secret (spec §5.4).**
The receiver, having completed the same commit/reveal ECDH, computes the identical
`link_proof = HKDF(IKM=shared, salt=receiver_session_PUBLIC_key, info="simplipay/proximity/link-proof/v1", L=32)`
and registers it. **Salt is the session-fixed receiver pubkey, NOT the rotating token** (spec §5.4/§5.7; a
token salt reintroduces a ~3–5% window-boundary mismatch). The sender's `/resolve` proof is matched by
**byte-equality**. Server never recomputes (it can't).

### Request
```kotlin
@Serializable
data class SessionBindRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("link_proof") val linkProof: String,   // base64url 32B
    @SerialName("ttl_secs") val ttlSecs: Long = 30,    // how long the proof is matchable
)
```
### Response 200 `{ "ok": true }` · Errors: 403 (not owner), 410 (expired), 401.

---

## 4. POST /proximity/resolve — token → display (two tiers)

Tier selected by presence of a **matched** `link_proof`. `coarse_loc` intentionally omitted (spec §5.4).
The signed payload carries **no handle** (derived locally, spec §5.7).

### Request
```kotlin
@Serializable
data class ResolveRequest(
    @SerialName("token") val token: String,
    @SerialName("link_proof") val linkProof: String? = null,   // null = initials tier
)
```
### Response 200 — initials tier
```kotlin
@Serializable
data class ResolveInitialsResponse(
    @SerialName("tier") val tier: String,                      // "INITIALS"
    @SerialName("initials") val initials: String,             // e.g. "T.M."
    @SerialName("merchant_verified") val merchantVerified: Boolean,
    @SerialName("device_class") val deviceClass: String,
    @SerialName("amount_band") val amountBand: String,         // "LT_100"|"100_500"|"500_1K"|"GT_1K"|"OPEN"
)
```
### Response 200 — connected (full, signed) tier
```kotlin
@Serializable
data class ResolveSignedResponse(
    @SerialName("tier") val tier: String,                      // "FULL"
    @SerialName("signed_payload") val signedPayload: String,   // base64url of canonical bytes
    @SerialName("signature") val signature: String,            // Ed25519 over signed_payload bytes
    @SerialName("kid") val kid: String,
)

// Decoded contents of signed_payload (verify signature FIRST, then decode):
@Serializable
data class ProximityDisplay(
    @SerialName("session_id") val sessionId: String,
    @SerialName("token") val token: String,                    // echoes resolved token (binds sig↔token)
    @SerialName("name") val name: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("amount_minor") val amountMinor: Long? = null, // null when amount_mode = OPEN
    @SerialName("currency") val currency: String,
    @SerialName("device_class") val deviceClass: String,
    @SerialName("merchant_verified") val merchantVerified: Boolean,
    @SerialName("iat") val iat: Long,
    @SerialName("exp") val exp: Long,                          // ≤ token TTL (spec §5.4)
)
```
Errors: 404 `unknown_token` (unknown **or** expired — drop silently), 403 (`link_proof` supplied but
unmatched → no proximity proof), 429, 401. Rate: initials 30/min, full 10/min per account.

---

## 5. POST /proximity/quote — signed open-amount quote (OPEN only)

### Request
```kotlin
@Serializable
data class QuoteRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("token") val token: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("currency") val currency: String,
)
```
### Response 200
```kotlin
@Serializable
data class QuoteResponse(
    @SerialName("signed_quote") val signedQuote: String,       // base64url canonical bytes
    @SerialName("signature") val signature: String,
    @SerialName("kid") val kid: String,
)
// Decoded signed_quote: { session_id, amount_minor, currency, iat, exp(=iat+120) }
```
Errors: 403 (session FIXED — no quote needed), 410 (expired), 429, 401. Rate: 15/min/account.

---

## 6. POST /proximity/confirm — settle (single-settle, idempotent)

**[FLAG]** `signed_quote` carried for OPEN sessions (spec §5.9); server re-verifies its signature, `exp`,
amount. FIXED ignores it and re-checks the bound amount.

### Request
```kotlin
@Serializable
data class ConfirmRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("token") val token: String,
    @SerialName("payment_intent_id") val paymentIntentId: String, // client 128-bit nonce, base64url
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("currency") val currency: String,
    @SerialName("link_proof") val linkProof: String,
    @SerialName("signed_quote") val signedQuote: String? = null,  // required iff OPEN
)
```
### Response 200
```kotlin
@Serializable
data class ConfirmResponse(
    @SerialName("status") val status: String,            // "settled" | "pending_receiver" | "rejected"
    @SerialName("settlement_ref") val settlementRef: String? = null, // iff settled
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("reject_reason") val rejectReason: String? = null,
)
```
Semantics:
- `settled` — below threshold, one-sided; pay-link settled via existing rails (spec §7.1).
- `pending_receiver` — above `accept_threshold_minor`; awaits `/accept` (spec §5.10). Sender polls
  `confirm` (idempotent on `payment_intent_id`) or watches its own UI for the final state.
- `rejected` — receiver declined or session invalidated (background, spec §5.10).

Errors: 409 `already_settled` (single-settle, different intent), 422 `amount_mismatch`, 410 `session_expired`,
404 `unknown_token`, 429, 401. Single-settle lock on `session_id` for P2P/POS; DONATION has none.

---

## 7. POST /proximity/accept — receiver approves a pending confirm

The receiver learns of the pending confirm via **§9 events channel** (this endpoint only records the decision).

### Request
```kotlin
@Serializable
data class AcceptRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("accept") val accept: Boolean,
)
```
### Response 200
```kotlin
@Serializable
data class AcceptResponse(
    @SerialName("status") val status: String,            // "settled" | "rejected"
    @SerialName("settlement_ref") val settlementRef: String? = null,
)
```
Errors: 403 (not owner), 404 (no such pending intent), 409 (already resolved), 410 (expired), 401.

---

## 8. POST /proximity/session/stop — teardown / invalidate

Required by spec §5.10 (background / "Stop receiving"). Local `stopAdvertising()` only stops the radio;
this invalidates the server record so nothing settles on an unwatched screen.

```kotlin
@Serializable
data class SessionStopRequest(@SerialName("session_id") val sessionId: String)
```
Response 200 `{ "ok": true }` · Errors: 403, 401.

---

## 9. GET /proximity/session/{id}/events — receiver inbound channel (long-poll/SSE)

The receiver-accept gate's delivery path (spec §5.10). While a session is active+foreground,
`ProximitySession` holds this open; a FCM **data message** is a redundant wake. Latency budget 1–2 s.

Event frames (SSE `data:` lines, JSON):
```kotlin
@Serializable
data class SessionEvent(
    @SerialName("type") val type: String,           // "pending_receiver" | "settled" | "rejected" | "expired"
    @SerialName("payment_intent_id") val paymentIntentId: String? = null, // for pending/settled
    @SerialName("amount_minor") val amountMinor: Long? = null,            // for pending
    @SerialName("currency") val currency: String? = null,
    @SerialName("payer_name") val payerName: String? = null,             // for the accept prompt
    @SerialName("settlement_ref") val settlementRef: String? = null,     // for settled
)
```
Errors: 403 (not owner), 410 (session ended), 401.

---

## Appendix A — GATT profile

> Assign real randomly-generated 128-bit UUIDs before Phase 0. Multi-byte numeric fields little-endian;
> opaque blobs byte-exact.

| Element | UUID (assign) | Props | Size | Notes |
|---|---|---|---|---|
| Discovery service | `0000F5xx-…` (fixed) | — | — | the ONLY thing in the advertisement (spec §4.3) |
| `PAYLOAD` | `…01` | **READ** (iOS `value=nil`) | exactly 20 B | spec §5.1 payload (version, device_class, flags, 16-B token, crc8). No window byte — window reconstructed from `floor(epoch/W)` (spec §6.1) |
| `COMMIT` | `…02` | **READ** (iOS `value=nil`) | 32 B | receiver commitment `C_r = HKDF(epr_pub, info="…/commit/v1")` — session-fixed, **no token** (spec §5.7) |
| `TX_PUBKEY` | `…03` | **READ** (iOS `value=nil`) | 32 B | receiver **session-fixed** ephemeral X25519 pubkey. Receiver **withholds** it per-central until that central has written `RX_PUBKEY` (normative anti-grind, spec §5.7) — requires `value=nil` on iOS or the cache bypasses the gate |
| `RX_PUBKEY` | `…04` | **WRITE** (w/ response) | 32 B | sender writes its ephemeral X25519 pubkey to complete ECDH |

**Sender handshake order:** connect → read `COMMIT` (`C_r`) → write `RX_PUBKEY` (own pubkey) →
read `TX_PUBKEY` (receiver pubkey) → verify `HKDF(epr_pub, info="…/commit/v1")==C_r` (else abort) →
derive `shared`, `badge`, `matchHandle`, `link_proof` (all from session-fixed inputs — no token, so no
window race). Read `PAYLOAD` (token) for the resolve/confirm calls; the atomic windowed read (record
`w=floor(epoch/W)`, abort+retry if it advances) applies only to that token read, **not** to the bilateral crypto.
**MTU**: negotiate **≥ 64** on connect (Android `requestMtu(64)`; iOS auto) so 32-byte keys fit one PDU.
**CRC-8** (payload last byte): CRC-8/SMBUS — poly `0x07`, init `0x00`, no reflection, xorout `0x00`.
**`flags` bit2 (ECDH required)** is set in v1; a peer reading `PAYLOAD` without completing the
key exchange is refused at the receiver's discretion.

## Appendix B — Threading
- iOS: `CBCentralManager`/`CBPeripheralManager` on a dedicated **serial** `dispatch_queue`; delegates
  `trySend` into the `callbackFlow`; retain managers + delegates as **strong properties** (spec §9.2/§15).
- Android: explicit `Handler`/executor on scan/GATT callbacks; `trySend` into the flow.
- ViewModel collects on `Dispatchers.Main.immediate`; `trySend` is thread-safe (no upstream `flowOn`).

## Appendix C — HKDF labels (domain separation)
```
badge       : HKDF(IKM=receiver_session_pubkey, salt=session_id,           info="simplipay/proximity/badge/v1",      L=20 bits→symbol pair)
match handle: HKDF(IKM=ecdh_shared,             salt=receiver_session_pubkey, info="simplipay/proximity/match/v1",    L=20 bits→symbol pair)
link_proof  : HKDF(IKM=ecdh_shared,             salt=receiver_session_pubkey, info="simplipay/proximity/link-proof/v1", L=32 bytes)
commitment  : C_r = HKDF(IKM=epr_pub,                                       info="simplipay/proximity/commit/v1",     L=32 bytes)
```
**All bilateral values use session-fixed inputs only — never the rotating on-air token** — so receiver and
sender never disagree at a window boundary (spec §5.7). The token is purely the on-air discovery/resolve
identifier. Never use the raw X25519 output directly; never a bare `H(a‖b)`.
