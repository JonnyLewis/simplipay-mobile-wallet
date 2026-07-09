# Platform Parity — Bring iOS Redesign Changes to Android & Desktop

> **Created**: 2026-06-25
> **Type**: platform
> **Branch**: `feature/simplipay-platinum-ivory-redesign`
> **Status**: planning
> **Scope**: 247 files changed vs `development`; this plan covers what it takes to make the iOS-validated work run at parity on Android and Desktop (JVM).

---

## 0. TL;DR

The SimpliPay Platinum-Ivory redesign (theme, onboarding/splash, KYC SA-ID signup,
home/finance redesign, send/receive menus, VAS hub, pay-links, top-up, access gating,
custom PIN keypad, passcode) was **built in shared `commonMain` / `cmp-shared`** and
has so far only been **run and verified on iOS**.

**The good news:** ~184 of the 247 changed files are `commonMain` — they already
compile and target Android, Desktop, and Web from the same source. There is no
"re-implement the UI per platform" work.

**The actual gap is small and well-bounded:**

| # | Area | iOS | Android | Desktop | Work needed |
|:-:|------|-----|---------|---------|-------------|
| 1 | **SA-ID OCR** (`recognizeTextFromImage`) | ✅ Apple Vision (real) | ⛔ stub returns empty | ⛔ no-op (by design) | **Android: implement ML Kit Text Recognition.** Desktop: decide UX fallback |
| 2 | QR/PDF417 camera scan | ✅ `CameraView` (AVFoundation) | ✅ `CameraView` (CameraX) — exists | ⛔ no camera | Android: verify against new scan flow. Desktop: file-import fallback |
| 3 | Shared redesign UI | ✅ run & verified | ❓ compiles, **never run** | ❓ compiles, **never run** | Build + manual QA pass on each platform |
| 4 | Platform config (permissions, splash, theme) | ✅ Info.plist done | ⚠️ camera perm present — verify rest | ⚠️ verify | Audit per platform |

So the bulk of this plan is **verification + QA**, with **one real feature
implementation (Android OCR)** and **one product decision (desktop OCR/camera fallback)**.

---

## 1. Where the changes actually live

Breakdown of the 247 changed files by source set:

```
184  commonMain      → shared across Android / Desktop / iOS / Web (already targets all)
  2  nonAndroidMain  → QrScanner.nonAndroid.kt (iOS/desktop/web), send-money
  2  nativeMain      → CameraView.kt + IdOcr.native.kt (iOS, AVFoundation/Vision)
  2  desktopMain     → cmp-desktop main.kt area + RenderShots.kt (screenshot tooling)
  1  androidMain     → IdOcr.android.kt (the OCR STUB)
  1  wasmJsMain / 1 jsMain → IdOcr no-ops
   … remainder: composeResources strings, build.gradle.kts, plans/, prompt-layer/
```

**Implication:** there is *no* large body of iOS-only UI to translate. The redesign is
single-source. What differs per platform is confined to the `expect`/`actual`
seams below.

### The `expect`/`actual` seams (the only true platform divergence)

**A. ID OCR** — `feature/mpay-qr-scan/.../ocr/IdOcr.kt`
```kotlin
expect suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText
```
| Platform | File | State |
|----------|------|-------|
| iOS (native) | `IdOcr.native.kt` | **Real** — Apple Vision `VNRecognizeTextRequest`, `.accurate`, language-correction off, per-line boxes |
| Android | `IdOcr.android.kt` | **STUB** — returns `RecognizedText(emptyList())`, `// TODO(android): implement with ML Kit Text Recognition v2` |
| Desktop | `IdOcr.desktop.kt` | No-op by design ("ID OCR is a mobile capture feature") |
| Web (js/wasm) | `IdOcr.{js,wasmJs}.kt` | No-op by design |

**B. Camera preview** — `feature/mpay-qr-scan/.../CameraView.kt`
| Platform | State |
|----------|-------|
| iOS (native) | 289-line AVFoundation impl (torch, orientation, metadata output) |
| Android | 98-line CameraX impl (`ProcessCameraProvider`, torch) — **present** |
| Desktop/Web | none (no camera hardware path) |

**C. QR scanner host** — `feature/send-money/.../QrScanner.{android,nonAndroid}.kt`
Android + nonAndroid actuals both present.

### Dependency state (already wired)

`gradle/libs.versions.toml` and `feature/mpay-qr-scan/build.gradle.kts` already declare,
for `androidMain`: `androidx.camera.{view,camera2,lifecycle}`, `mlkit-barcode-scanning`
(17.3.0), `filekit-core`, `filekit-compose`. **Missing for the OCR work:** an ML Kit
**Text Recognition** artifact (`com.google.mlkit:text-recognition`) — only
*barcode*-scanning is present today.

---

## 2. Workstream 1 — Android SA-ID OCR (the one real feature gap)

This is the headline item: on iOS a user can scan the front of an SA ID / driver's
licence and OCR auto-fills signup fields; on Android the same screen currently produces
**nothing** (empty result), silently degrading the scan path.

**Steps**

1. **Add the dependency.** Add `mlkit-text-recognition = { module = "com.google.mlkit:text-recognition", version.ref = "mlkit" }` (or the latin-script bundled variant) to `libs.versions.toml`; wire it into `feature/mpay-qr-scan/build.gradle.kts` `androidMain`.
2. **Implement `IdOcr.android.kt`.** Replace the stub:
   - Load `PlatformFile` → `InputImage.fromFilePath(...)` (FileKit gives a path/URI on Android).
   - Run `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)`.
   - Map ML Kit `Text.Line` → `OcrLine(text, confidence, left, top, width, height)`, **normalising bounding boxes to 0..1 (top-left origin)** to match the iOS contract that the downstream SA-ID field parser depends on. ML Kit gives pixel `boundingBox` + image dimensions — divide through. ML Kit line confidence is limited; populate as available and keep parser tolerant.
   - Wrap in `withContext(Dispatchers.Default)` and `await` the `Task` (kotlinx-coroutines-play-services or a `suspendCancellableCoroutine`).
3. **Match iOS tuning intent.** No language "correction" of the 13-digit ID number — ML Kit's default recogniser doesn't autocorrect like a keyboard, but verify digit bands survive; restrict to latin script.
4. **Verify the consumer.** `IdOcrDebugContent.kt` (commonMain dev tool) and the KYC signup scan→review step both call `recognizeTextFromImage`. Confirm the parser/field-mapping (the "map later" stage referenced in `docs/kyc-sa-id-redesign-plan.md`) is fed identically on both platforms.
5. **Test on a real device** with a physical SA ID + driver's licence; compare extracted fields against the iOS output.

**Acceptance:** scanning the same card on Android and iOS yields equivalent
recognised lines and the same auto-filled signup fields.

---

## 3. Workstream 2 — Desktop OCR / camera product decision

Desktop has **no camera** and OCR is a deliberate no-op. The shared KYC signup flow will
still render on desktop, but the "Scan an ID card" path returns empty. Two options
(**decision required**):

- **(A) Hide/disable the scan path on desktop**, leaving only "Enter manually" (the
  manual path already exists in the redesigned wizard). Cleanest; matches "mobile capture
  feature" intent. *Recommended.*
- **(B) File-picker OCR on desktop** — let the user pick a still image and run a JVM OCR
  engine (e.g. Tess4J/Tesseract). Higher effort, new native dependency, lower accuracy;
  only worth it if desktop KYC is a real use case.

Until decided, ensure the desktop build at least **degrades gracefully** (no crash, clear
"not available on desktop" affordance) rather than showing a dead scan button.

---

## 4. Workstream 3 — Build & compile verification (per platform)

The shared code has only been **compiled/run for iOS** in practice. Confirm the other
targets build clean:

```bash
# Android
./gradlew :cmp-android:assembleDemoDebug
./gradlew :cmp-android:lintRelease

# Desktop (JVM)
./gradlew :cmp-desktop:run                 # actually launch the app
./gradlew :cmp-desktop:renderShots -PshotsOut=/abs/dir   # headless screen render (new task)

# Whole-project gate
./ci-prepush.sh
```

Watch for: `expect`/`actual` mismatches surfaced only on a given target, Android-only
Compose/material API differences, desktop-only missing-resource or font issues, and any
`commonMain` API that quietly assumed an iOS behaviour.

---

## 5. Workstream 4 — Platform config & manifest audit

| Concern | iOS | Android | Desktop | Action |
|---------|-----|---------|---------|--------|
| Camera permission | Info.plist `NSCameraUsageDescription` (done) | `CAMERA` permission **present** (`AndroidManifest.xml:19`) + `uses-feature camera` | n/a | Verify runtime-permission prompt fires in new scan flow |
| Splash / theme | done | uses shared `SplashScreen.kt` + theme; confirm Android system splash + status-bar colour match Platinum-Ivory light-only | confirm window chrome | QA |
| Light-only UI | enforced | confirm no dark-mode leakage on Android | confirm | QA |
| File access (FileKit) | done | verify image path resolution for OCR | verify file picker | part of WS1/WS2 |
| Biometrics (passcode) | via `mifos-authenticator-biometrics` KMP lib | confirm Android biometric prompt works with new `SimpliPayPasscodeScreen` / `PasscodeManager` | n/a (no biometric) — confirm PIN fallback | QA |

---

## 6. Workstream 5 — Manual QA parity pass

Run the full redesigned journey on **Android device** and **Desktop**, comparing to iOS:

- Splash → Landing → Create account → **Verify method** → Scan ID (Android: now real OCR; Desktop: per WS2 decision) → Review → Face intro/capture → Verified → Wallet.
- Login → Enter PIN (custom keypad) → Wallet.
- Home redesign, Finance tab, Payments tab.
- Send/Receive option menus, Pay-link screen, Top-up wallet.
- VAS / Buy hub.
- Access gating / `BlockedScreen` (locked-wallet probe — see `[[wallet-activation-roles]]`).
- Passcode + biometric setup/auth.

Log any platform-specific visual or behavioural divergence as follow-up items.

---

## 7. Effort summary & sequencing

| Workstream | Effort | Blocking? |
|------------|:------:|-----------|
| 1. Android SA-ID OCR (ML Kit Text Recognition) | **M** (~1–2 days incl. device testing) | The only true feature gap |
| 2. Desktop OCR/camera decision | **S** (decision) + S–L (impl if option B) | Needs product call |
| 3. Build verification (Android + Desktop) | **S** | Do first — surfaces hidden breakage |
| 4. Platform config audit | **S** | Parallel |
| 5. Manual QA parity pass | **M** | After 1, 3 |

**Recommended order:** WS3 (prove it builds/runs) → WS1 (Android OCR) → WS4 (config) →
WS2 decision → WS5 (QA sign-off).

---

## 8. Open decisions (need user/product input)

1. **Desktop scan path**: hide it (A, recommended) or build JVM file-picker OCR (B)?
2. **ML Kit Text Recognition variant**: bundled latin-script (`text-recognition`, larger
   APK, offline) vs. on-the-fly downloadable model? Affects APK size and first-run UX.
3. **Web (js/wasm)**: out of scope here (OCR/camera already no-op). Confirm web is not a
   parity target for this redesign.
4. **Scope of "parity"**: ship Android at full functional parity, with desktop as
   "renders correctly, mobile-capture features gracefully unavailable" — confirm that's
   acceptable.

---

## 9. References

- `docs/kyc-sa-id-redesign-plan.md` — the iOS-first SA-ID scan design & backend wiring.
- `feature/mpay-qr-scan/src/*/ocr/IdOcr.*.kt` — the OCR `expect`/`actual` seam.
- `feature/mpay-qr-scan/src/{androidMain,nativeMain}/.../CameraView.kt` — camera impls.
- `feature/send-money/src/*/QrScanner.*.kt` — scanner host.
- Memory: `[[ios-build-sequence]]`, `[[kyc-sa-id-redesign]]`, `[[wallet-activation-roles]]`.
