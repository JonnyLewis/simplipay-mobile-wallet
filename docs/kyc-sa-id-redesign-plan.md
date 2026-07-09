# SimpliPay KYC + Signup Redesign — SA ID Scanning Plan

> iOS-first (Android later). Source design: `~/Downloads/splash-screen-redesign/project/SimpliPay All Screens.html`.
> Status: research/planning. No production code written yet. Next action = scan a physical SA ID to determine the viable decode path.

---

## 1. What the design specifies (the redesigned flow)

16 screens in 3 sections: Onboarding (01–04), Identity·KYC (05–12), Wallet (13–16).
KYC progress model is **Identity → Selfie → Done** (NOT Level 1/2/3). End state = "KYC complete · Tier 2".

**Flow:**
```
Splash → Landing
  ├ "I already have an account" → Login → Enter PIN → Wallet
  └ "Create account" → 05 Verify·Method
        ├ "Scan an ID card" (FASTEST) → 06 Scan ID → 07 Scanning (OCR) → 08 Review details ┐
        └ "Enter manually" ─────────────────────────────► 09 Enter manually ───────────────┘
              → 10 Face·Intro → 11 Face·Capture (liveness, "Matching to your ID…") → 12 Verified → Wallet
```

**Fields collected:**
| Field | Scan path | Manual path | Notes |
|---|---|---|---|
| Full name | auto (OCR) | First + Last (split) | model-merge decision needed |
| ID number | auto | manual | 13-digit SA ID |
| Date of birth | auto | manual | also derivable from ID digits 1–6 |
| Nationality | auto | **not collected** | branch mismatch |
| Sex/gender | on card art only, not surfaced | not collected | derivable from ID digits 7–10 |
| Residential address | **not collected** | manual | branch mismatch; FICA needs this |
| Selfie | captured | captured | 1:1 face-to-ID match + liveness |

**Design gaps to resolve:** no credential-creation screens (email/phone/username/password/PIN), no OTP screen, no document-type picker, branch field mismatch (nationality vs address), gender never confirmed, no error/retry states, no POPIA consent/T&Cs, tier model unexplained.

---

## 2. What already exists in the codebase (big finding)

**Live backend:** `https://wallet.simplipay.co.za/fineract-provider/api/v1/` (Fineract). Dynamic base URL via `InstanceConfigManager.kt:23-30` + `DynamicBaseUrlPlugin`. Demo/prod flavors do NOT change backend.

**Signup already renders the redesigned wizard.** `SignupScreenNavigation.kt:19,45` renders `feature/auth/signup/kyc/KycSignupScreen.kt` (NOT the legacy `SignupScreen.kt`). It's a 4-step wizard (`KycStep`: VERIFY → SCANNING → DETAILS → REVIEW) driving `SignupViewModel`.

**A PDF417 scanner is ALREADY WIRED.** `KycSignupScreen.kt:289-296` uses `QrScannerWithPermissions(types = listOf(CodeType.PDF417))` aimed at the SA smart-ID back. It captures `rawPayload` and just displays it for copy/inspect (`:258-325`) — **it does not parse or map any fields yet.** This is the core missing piece and it matches our "scan raw first, then map" approach perfectly.

**Signup submit pipeline (works against live Fineract, with rollback):**
1. `SearchRepository.searchResources` — username + mobile uniqueness (`@GET search`).
2. `UserService.createUser` `@POST users` (`NewUserEntity`).
3. `ClientService.createClient` `@POST clients` (`NewClientEntity` → `ClientResponseEntity`).
4. `UserService.assignClientToUser` `@PUT users/{userId}` `{clients:[id]}`.
(`SignupViewModel.kt:418-583`.) Note: a `RegistrationService` (`@POST registration`) exists but is **unused**.

**KYC module (post-login, currently UNREACHABLE):**
- Routes registered at `MifosNavHost.kt:440-466`, but entry point commented out at `:291-297`; no caller of `navigateToKYCLevel1`.
- L1 = basic details via Fineract **datatable** `@GET/POST/PUT datatables/kyc_level1_details/{clientId}` (`KYCLevel1Service.kt`), model `KYCLevel1Details{firstName,lastName,addressLine1,addressLine2,mobileNo,dob,currentLevel}`. **Only place DOB is persisted.**
- L2 = document upload, multipart `@POST {entityType}/{entityId}/documents` (`DocumentService.kt`). **Server-side broken** on this instance — Fineract can't create the document dir (`KYCLevel2ViewModel.kt:145-150`).
- L3 = `// TODO` stub, unimplemented.

**OTP is fake.** `MobileVerificationViewModel.kt:143-172` — request/verify are stubbed, no SMS backend; verify auto-succeeds on 6 digits.

**Backend has no field for SA ID number or gender** anywhere (`NewClient`, `NewClientEntity`, `KYCLevel1Details`). DOB exists only in KYC L1 datatable. Address fields expect numeric Fineract `countryId`/`stateProvinceId` codes; UI currently sends free-text strings (pre-existing mismatch).

---

## 3. SA ID scanning — what's actually decodable

| Document | Machine-readable | Open decoder? | Fields obtainable |
|---|---|---|---|
| **Smart ID card** (new) | PDF417 + Code 39 on back; chip NOT public; no MRZ | **No FOSS** — commercial SDK only (Scandit/Regula/barKoder). Code 39 = ID number only | (PDF417, if decodable) name, sex, nationality, ID, birth, status, dates |
| **Green ID book** (old) | 1D barcode = ID number only | ID number: yes | ID number only; rest printed |
| **Driver's licence** | PDF417, "encrypted" with PUBLIC keys | **Yes — fully** | surname, initials, ID, licence no., codes, restrictions, gender, DOB, validity, **JPEG face photo** |

**13-digit ID number `YYMMDD GGGG C A Z`:** 1–6 DOB; 7–10 gender (0000–4999 F, 5000–9999 M); 11 citizenship (0 citizen / 1 PR); 12 deprecated (ignore); 13 Luhn check. Validate everywhere regardless of scan vs manual.

**Key correction to assumptions:** the smart ID *back* does have a PDF417, but there is **no public byte-format spec and no open-source decoder** — only paid SDKs. So for the smart ID, realistic free routes are: (a) read the Code 39 = ID number only, then derive DOB/gender/citizenship from it; (b) OCR the front for name/surname. Full barcode decode = commercial SDK.

**Tooling for KMP/iOS:**
- **zxing-cpp** (Apache-2.0) — decodes PDF417 to **raw bytes** (essential; needed for licence + to dump smart-ID payload). C++20 via cinterop/xcframework.
- **Apple Vision** — `VNRecognizeTextRequest` (front OCR) + `VNDetectBarcodesRequest.payloadData` (raw barcode bytes), on-device, iOS-native.
- **ML Kit Barcode/Text** — free on-device, Android+iOS; string-only by default (mangles binary).
- **KScan** (Apache-2.0, true KMP) — already the family the app uses (`QrScannerWithPermissions`/`CodeType.PDF417`); returns **String** → fine for ID numbers, **loses bytes** for encrypted payloads.
- Open licence decoders: `dalion619/sadl-pdf417-decryption` (MIT, ships keys), `DanieLeeuwner/Reply.Net.SADL` (C#).

**Critical pitfall:** the current scanner returns a **String**; encrypted PDF417 must be carried as **`ByteArray`** end-to-end or the payload is corrupted. The `rawPayload` boundary needs to become bytes.

---

## 4. Recommended architecture

> **Scope decision (2026-06-25):** support **Smart ID card + Driver's licence**. Green ID book is OUT.

- Keep **all parsing in `commonMain`**: ID-number/Luhn math, SADL RSA decode + section parse, field→client mapping. Pure Kotlin, testable, no platform deps.
- Only **camera capture + barcode/OCR decode** in `expect/actual` (Vision/zxing-cpp on iOS; ML Kit on Android later).
- Pipeline: `capture → decode (barcode→ByteArray | OCR→String) → parse → ScannedIdentity → map to NewClient/KYC fields → user review (editable) → submit`.
- Reuse the existing `KycSignupScreen` wizard + `SignupViewModel` submit pipeline; add a parser and wire its output into `SignUpState`.

### 4a. Supporting both documents behind one pipeline

One `IdDocumentParser` interface, two implementations, a common `ScannedIdentity` output (`firstName, surname, idNumber, dob, gender, citizenship, faceJpeg?`). Route by **document type**, which can be auto-detected from the decoded bytes (no manual picker needed):

| | **Driver's licence** | **Smart ID card** |
|---|---|---|
| Capture target | PDF417 (back) | PDF417 (back) + front (OCR) |
| Decode tech | zxing-cpp / Vision → **ByteArray** | zxing-cpp / Vision → bytes; Vision OCR on front |
| Auto-detect signature | 720 bytes starting `01 9B 09 45` (v2) | different length/header (confirm by dump) |
| Decode cost | **Free** — public SADL RSA keys (`dalion619`) | **DECISION REQUIRED** — see 4b |
| Fields yielded | surname, initials, ID, licence no., codes, gender, DOB, validity, **face JPEG** | depends on path chosen (4b) |
| Face photo | embedded JPEG (can seed selfie match) | none in barcode → use front photo |

### 4b. The Smart ID decode fork (the one real decision)

The smart-ID PDF417 has **no open-source decoder and no published byte format**. Three options:

- **Option A — Free / partial.** Read the back **Code 39** (= ID number only) → derive DOB + gender + citizenship from the 13 digits (Luhn-checked). OCR the **front** with Apple Vision for first name + surname. No SDK cost; OCR is the fragile link; no embedded photo.
- **Option B — Commercial SDK (full).** Scandit / Regula / barKoder decode the smart-ID PDF417 to full structured fields. Costs money, BUT a single SDK typically decodes **both** the smart ID and the licence — so it can replace the whole bespoke decode layer and simplify everything. Price it before deciding.
- **Option C — Self-decode.** Only viable IF a raw byte dump of an actual smart-ID PDF417 turns out to be mostly plaintext / reversible (low entropy, visible ID/strings). If it's high-entropy encrypted with non-public keys, this is a dead end. **Must dump bytes to know.**

Recommendation: **dump a real smart-ID PDF417 first** (settles A vs C), and **in parallel price one commercial SDK** that covers both docs (settles B). Licence work (free, full) can proceed immediately regardless.

---

## 5. EXPERIMENTATION PLAN (do this first — before committing to a path)

**Goal: scan the physical ID, dump the RAW payload, discover which decode path works for the card type the team actually has.**

1. **Identify the document type** the team holds (smart ID card / green book / driver's licence) — decode path differs hard per type.
2. **Fast raw dump (no build):** photograph the back barcode; run **zxing-cpp `ZXingReader` CLI** requesting hex/bytes.
   - Driver's licence → exactly **720 bytes** starting `01 9B 09 45` (v2). Decode with `dalion619` keys → verify against printed fields → extract Section-3 JPEG.
   - Smart-ID Code 39 / green book → instant **ID number**; derive DOB/gender/citizenship + Luhn.
   - Smart-ID PDF417 → dump hex; inspect: plaintext ID/strings ⇒ partially reversible by diffing against printed values; high-entropy fixed blocks ⇒ needs unavailable keys ⇒ commercial SDK or fall back to front OCR.
3. **In-app raw capture (already possible today):** `KycSignupScreen` SCANNING step already shows `rawPayload` — scan the team's card and read what comes back. (Caveat: it's currently String — for smart-ID bytes, switch the boundary to ByteArray, or use the desktop dump in step 2.)
4. **OCR fallback test:** Apple Vision `VNRecognizeTextRequest` on the front for name/surname if barcode is not openly decodable.
5. **Record the field mapping** discovered (offset → field) in this doc, per document type.

---

## 6. Implementation roadmap (after experimentation confirms the path)

**Phase A — Decode + parse (commonMain), no UI change**
1. Switch `rawPayload` boundary from `String` → `ByteArray` (capture decoder must preserve bytes; likely needs zxing-cpp/Vision via expect/actual rather than string-only KScan).
2. `SaIdNumber` parser: validate length/Luhn, derive DOB/gender/citizenship.
3. Per-confirmed-type parser: SADL (licence) and/or smart-ID-PDF417 and/or Code-39+OCR. Output a common `ScannedIdentity` model.
4. Unit tests against the team's real dumped payload(s).

**Phase B — Wire into signup**
5. Map `ScannedIdentity` → `SignUpState` (firstname/lastname/dob/idNumber/gender/nationality) and prefill the DETAILS/REVIEW steps; keep all fields editable.
6. Reconcile model gaps: add SA ID number + gender persistence (new datatable fields or KYC L1 extension; client creation has no DOB/ID/gender today).
7. Reconcile branch field set (nationality vs address) → one canonical schema.

**Phase C — Selfie / liveness**
8. Face capture screen + 1:1 face-to-ID match + liveness (research a KMP/iOS option separately; not yet investigated).

**Phase D — Backend + persistence**
9. Fix or route around broken document upload (`@POST clients/{id}/documents`) so the ID image/selfie persist.
10. Address-code mapping (free-text → Fineract numeric countryId/stateProvinceId).
11. Decide signup persistence: keep users+clients+assign, or switch to the unused `registration` endpoint.

**Phase E — Hardening / gaps**
12. Real OTP backend (currently stubbed).
13. Error/retry states (scan fail, glare, face-match fail, ID-in-use, underage, invalid ID).
14. POPIA: explicit consent, on-device decode only, encrypt at rest, minimise fields, no PII logging, short retention, ignore deprecated race digit.

---

## 6b. BUILT — iOS Vision OCR capture tool (2026-06-25)

Decision locked: **OCR the front of the card with Apple Vision**, one pipeline for **both** driver's licence + Smart ID (green book dropped). First deliverable shipped = a "scan first, map later" dev tool.

New files (module `feature/mpay-qr-scan`, package `…/ocr/`):
- `IdOcr.kt` — `data class OcrLine(text, confidence, left, top, width, height)`, `RecognizedText(lines){fullText}`, and `expect suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText`.
- `IdOcr.native.kt` — iOS actual using Apple **Vision** (`VNImageRequestHandler(uRL=…)` + `VNRecognizeTextRequest`, `.accurate`, `usesLanguageCorrection=false`, languages `["en"]`); maps observations → `OcrLine` (Vision's bottom-left box flipped to top-left), sorted top→bottom.
- `IdOcr.{android,desktop,js,wasmJs}.kt` — stubs returning empty (Android = ML Kit later).
- `IdOcrDebugContent.kt` — composable: FileKit image picker → `recognizeTextFromImage` → dumps every line with confidence + x/y position, "Copy all".

Wired into signup: `feature/auth/.../signup/kyc/KycSignupScreen.kt` `KycScanningStep` now has **"Or OCR a photo of the front instead"** → shows `IdOcrDebugContent` (and a "← Back to barcode scan").

Verified: `:feature:mpay-qr-scan` and `:feature:auth` both `compileKotlinIosSimulatorArm64` **BUILD SUCCESSFUL**. (Only warning on new code = pre-existing deprecated `LocalClipboardManager`.) No camera/Info.plist change needed — FileKit uses PHPicker (out-of-process). No barcode/cinterop/Podfile changes.

**NEXT:**
1. Build+run on iPhone (see [[ios-build-sequence]]), open signup → KYC scan step → "Or OCR a photo of the front instead", pick example licence + Smart ID photos, read the dumped lines.
2. From real output across the many examples, write a pure-`commonMain` parser: licence (fixed front layout) + Smart ID (fixed front layout) → `ScannedIdentity` (firstName, surname, idNumber, dob, gender, nationality); validate SA ID number (len 13 + Luhn) and derive DOB/gender/citizenship.
3. Map `ScannedIdentity` → `SignUpState` (prefill DETAILS/REVIEW, keep editable). Then live still-capture (AVCapturePhotoOutput) to replace gallery picking; consider auto-capture quality gating.

## 6c. BUILT — SA Smart ID PDF417 parse + prefill (2026-06-25)

**Key discovery:** the SA Smart ID PDF417 payload is **plaintext, pipe-delimited** (NOT encrypted) — free, full, reliable decode. No OCR, no commercial SDK needed for the Smart ID. Confirmed field order:
`SURNAME | NAME | GENDER | NATIONALITY | ID NUMBER | BIRTH DATE | COUNTRY OF BIRTH | CITIZENSHIP STATUS | ISSUE DATE | SECURITY NUMBER | SMART ID NUMBER | FILLER`

Implemented:
- `feature/auth/.../signup/kyc/SaSmartIdBarcode.kt` — `data class SaSmartIdBarcode` + `parseSaSmartIdBarcode(raw): SaSmartIdBarcode?` (lenient: needs `|` and ≥6 fields; trims; `genderLabel` → Male/Female).
- `SignupViewModel`: new `SignUpState` fields `idNumberInput, dobInput, genderInput, nationalityInput, citizenshipInput`; new actions `IdNumberInputChange/DobInputChange/GenderInputChange/NationalityInputChange/CitizenshipInputChange` + `IdScanPrefill(...)`; handlers added (when stays exhaustive).
- `KycSignupScreen`: `KycScanningStep` now parses the PDF417 `onScanned` → `IdScanPrefill` → `onContinue()` to the DETAILS step. Falls back to raw-payload display if parse fails. DETAILS step renders the 5 new identity fields (editable); REVIEW step shows them.

Verified: `:feature:auth:compileKotlinIosSimulatorArm64` **BUILD SUCCESSFUL**.

**Still open from this:** these 5 identity fields have **no backend slot** yet (client create has no DOB/ID/gender; DOB only persists in KYC L1 datatable). Prefill+display+edit works; persisting them to Fineract is the next backend task. Also: birth-date format from the barcode is shown raw (refine formatting after seeing a real value); optionally validate ID number (len 13 + Luhn) and cross-derive DOB/gender from it.

## 6d. BUILT — bug fixes + signup wizard split (2026-06-25)

**Bug fixes (`SignupViewModel.kt`):**
- Password mismatch: password & confirm are now `.trim()`-ed on input (`handlePasswordInput`, `ConfirmPasswordInputChange`) so an invisible autofill/keyboard space can't fail the equality check.
- Country: `loadCountriesFromJson` reorders **South Africa to the top** of the dropdown and sets it as the default `countryInput` (when empty); all other countries keep original order.

**Signup wizard restructure (`KycSignupScreen.kt`):** the one big DETAILS screen is split into 3 indicated steps + PIN + review. New `KycStep`: VERIFY → SCANNING → **PERSONAL → CONTACT → APP → PIN** → REVIEW.
- `KycPersonalStep`: first/last name, ID number, DOB, gender, nationality, citizenship (the ID-scan fields).
- `KycContactStep`: email, mobile, address1/2, postal code, country/state.
- `KycAppStep`: username, password (+ strength card), confirm.
- `KycPinStep`: embeds the shared `MifosPasscode` keypad → sets the **real app-unlock PIN** (PasscodeManager); `onPasscodeCreation`/`onAuthenticationSuccess` → review. Added `implementation(projects.feature.passcode)` to `feature/auth/build.gradle.kts` (no dep cycle).
- `KycStepIndicator`: wordless 3-segment progress bar (jade = current/done, faint = upcoming); shown on PERSONAL/CONTACT/APP/PIN (PIN shares step 3); hidden on VERIFY/SCANNING/REVIEW. `wizardStepIndex()` maps steps → dots.

Verified: `:feature:auth` and `:cmp-shared` both `compileKotlinIosSimulatorArm64` **BUILD SUCCESSFUL**.

**To validate on device / follow-ups:**
- Confirm `MifosPasscode` embeds cleanly under the top bar + indicator (it's normally a full screen); may need visual trim. After signup sets the PIN, first login will ask to *enter* that PIN at the root passcode gate (expected).
- Per-step validation isn't added — final `handleSubmitClick` still validates everything at REVIEW (shows a dialog). Consider gating each step's Continue.
- Identity fields still aren't persisted to Fineract (see §6c).

## 7. Open questions for product / next session
- Which ID document type(s) must be supported at launch? (Determines decode feasibility — licence is free/full; smart ID likely needs a paid SDK or OCR-only.)
- Budget for a commercial barcode SDK (Scandit/Regula) if full smart-ID decode is required?
- Canonical field schema (nationality + address + gender + ID number) and where each persists in Fineract.
- Is post-login KYC Level 1/2/3 still in scope, or fully replaced by the new Identity·Selfie·Done signup flow?
- Selfie/liveness provider (not yet researched).

---

## File references (for fast re-entry)
- Redesigned signup wizard + scanner: `feature/auth/src/commonMain/kotlin/org/mifospay/feature/auth/signup/kyc/KycSignupScreen.kt` (PDF417 scan `:289-296`, raw payload `:258-325`)
- Signup VM submit pipeline: `feature/auth/.../signup/SignupViewModel.kt:418-583`
- OTP stub: `feature/auth/.../mobileVerify/MobileVerificationViewModel.kt:143-172`
- KYC module: `feature/kyc/...` (L1 `KYCLevel1Service.kt`, L2 doc upload broken `KYCLevel2ViewModel.kt:145-150`, L3 stub)
- KYC entry disabled: `cmp-shared/.../navigation/MifosNavHost.kt:291-297` (routes `:440-466`)
- Backend instance: `core/network/.../config/InstanceConfigManager.kt:23-30`
- Full scanning research: `scratchpad/sa-id-scanning-research.md`
