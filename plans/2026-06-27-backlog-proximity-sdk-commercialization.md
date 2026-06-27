# Backlog / Future Spec — Proximity Payment SDK (Commercialization)

> **Type**: feature-request → future spec (NOT an active implementation plan)
> **Status**: BACKLOG — full product spec to be written later
> **Created**: 2026-06-27
> **Depends on**: `claude-product-cycle/design-spec-layer/features/proximity/proximity-payment.md` (v6) shipping first
> **Owner**: Jonathan Lewis

---

## 1. The request (as stated)

After we implement the in-app proximity payment feature, **commercialize it as an embeddable, white-label SDK** that banks deploy:

- A **master sign-in key** that anchors trust for the whole program.
- A **library banks embed into their (consumer) devices/apps**.
- A **separate library embedded into their kiosks** (POS / counter terminals).
- Currently in conversation with banks to deploy.

## 2. What this means technically (analysis)

It is a **productization** of the proximity feature into a multi-tenant, white-label **KMP SDK** with two embed profiles, anchored by a master key hierarchy. It is *not* a new payment protocol — it reuses the proximity protocol we already specced — but it adds the commercial/operational layer around it.

### 2.1 Two SDK profiles
| Profile | Who embeds | Role (proximity model) | Core surface |
|---------|-----------|------------------------|--------------|
| **Consumer/device SDK** | a bank's mobile app / their issued devices | Sender (pay) + Receiver (P2P get-paid) | discover, resolve, confirm, slide-to-pay, receive-beacon |
| **Kiosk/POS SDK** | a bank's counter kiosks / tills | advertising **Receiver** (accept payment) | open checkout session, advertise, receiver-accept, settle callback |

### 2.2 The "master sign-in key" → a multi-tenant key hierarchy
The proximity spec already defines a **root → operational-key chain** (§5.8) for signing resolve payloads. Commercialization extends that into a **multi-tenant PKI**:
- **Master (program) root** — SimpliPay-held, ideally in an HSM; the anchor pinned in every embedding.
- **Per-bank (tenant) keys** — issued/provisioned under the master; identify and authorize each bank's fleet of devices/kiosks.
- **Per-device/kiosk provisioning** — each embedded unit gets a device-bound credential (tie into the spec's POS attestation: Play Integrity / DeviceCheck / App Attest, §10 Phase 3), so a stolen SDK binary can't impersonate a licensed kiosk.
- This same hierarchy doubles as **licensing enforcement** (only provisioned tenants/devices can mint/resolve sessions) and **revocation** (kill a tenant or a single device).

### 2.3 What it reuses vs. what is new
**Reuses (already specced):** the whole BLE/GATT transport, the commit/reveal ECDH, token/badge/handle model, the resolve/confirm endpoints, the device-class field (P2P/DONATION/**POS**), the Ed25519 root-chain, the Phase-5 cloud/remote-POS connector.

**New (the commercialization layer):**
1. **Multi-tenant backend** — tenant isolation, per-tenant signing keys, per-tenant config, usage metering/billing.
2. **SDK packaging & distribution** — Android (AAR / Maven), iOS (XCFramework / SwiftPM / CocoaPods), from the KMP `feature/proximity` + transport, with a stable public API surface and semantic versioning. Decide which internals are exposed vs. sealed.
3. **White-labelling** — themeable UI (the SimpliPay design tokens become tenant-configurable), tenant branding, configurable strings/locales.
4. **Provisioning & onboarding flow** — how a bank registers, gets its tenant key, and provisions devices/kiosks (likely an admin console + a provisioning API).
5. **Deployment model** — do banks call **our** backend (SaaS) or **self-host** a deployment? (Drives the key-custody and data-residency design.)
6. **Compliance & certification** — PCI-DSS scope, banking-partner security review, attestation requirements, possibly per-jurisdiction.
7. **Kiosk OS reach** — kiosks may run OSes outside KMP's targets; ties to the Phase-5 "connector for other OS that POSs run on" — a thin native agent + the server-minted session model.

## 3. Open questions (to resolve in the future spec)
- SaaS vs. self-hosted (or both)? This is the biggest fork — it changes key custody, multi-tenancy, and data residency.
- Master key custody: HSM? Which? Rotation/ceremony for the program root.
- Licensing model & enforcement point (key-gated at session-mint vs. contractual only).
- SDK API stability commitment and support matrix (min OS versions, KMP vs. native-thin wrappers per platform).
- Certification targets demanded by the first bank partners.
- Does the kiosk SDK need to run on non-mobile OSes day one, or is mobile-first acceptable?

## 4. Sequencing
1. Ship in-app proximity payment (v6 spec) — **prerequisite**.
2. Write the full **Proximity SDK product + technical spec** (this backlog item → a real spec).
3. Pilot with one bank partner (likely the cloud/remote-POS connector, Phase 5, lands here).
4. Generalize to multi-tenant GA.

## 5. Tracking
- GitHub feature request (redacted, public): **JonnyLewis/simplipay-mobile-wallet#2** — https://github.com/JonnyLewis/simplipay-mobile-wallet/issues/2. Commercial/partner specifics intentionally omitted from the public issue; full detail lives in this private backlog doc.
- Indexed in `plans/PLANS_INDEX.md` under Backlog.
