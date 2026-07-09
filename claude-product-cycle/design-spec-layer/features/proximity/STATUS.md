# Proximity Payment — Status

**Feature**: proximity (Proximity Payment — BLE)
**Last Updated**: 2026-06-27

| Layer | Status | Notes |
|-------|:------:|-------|
| Design (SPEC) | ✅ Ready | `proximity-payment.md` v5 — engineer-ready |
| Design (API) | ✅ Ready | `API.md` — DTOs, encodings, error codes, GATT profile, threading |
| Mockups | ➖ ASCII-only | In-spec ASCII; no Figma/Stitch yet |
| Server (backend) | ❌ Not started | 9 new endpoints (`/proximity/*`) — see API.md |
| Client | ❌ Not started | `core/network ProximityService`, `core/data ProximityRepository` |
| Feature (UI) | ❌ Not started | new `feature/proximity` KMP module |
| Platform | ❌ Not started | Android BLE + iOS CoreBluetooth transport actuals; Desktop/Web → QR fallback |

## How it was produced
Research (iOS CoreBluetooth feasibility · Android BLE + KMP library landscape · codebase integration points · BLE security protocol) → v1 synthesis → **5-generation review loop** (security red-team → crypto-correctness → convergence → final sign-off → architect pass). Gen-4 verdict: GO-WITH-FIXES, all cleared in v5.

## Key decisions (see spec for detail)
- BLE carries only a 20-byte opaque rotating token; **no PII/pay-link on air**.
- Trust anchored server-side (Ed25519-signed resolve); BLE untrusted.
- X25519 **commit/reveal** ECDH over GATT → session **badge** (wrong-person aid) + pairwise **match handle** (anti-rebroadcast-relay). All bilateral crypto salted with the session-fixed receiver pubkey.
- Two-sided **receiver-accept** above a threshold (anti-relay backstop). True relay resistance deferred to UWB (Phase 4).
- Transport = plain `commonMain` interface bound per-platform via Koin (no `expect class`; no KMP BLE lib does peripheral on both platforms).
- Device classes **P2P** (this release) / **DONATION** / **POS** baked into the protocol.

## Next step
`/gap-planning proximity` → backend endpoints + `feature/proximity` module scaffold + transport actuals. Needs: crypto provider (Ed25519/X25519/HMAC/HKDF) added to the tree; GATT UUIDs assigned; iOS Info.plist + Android manifest BLE permissions.
