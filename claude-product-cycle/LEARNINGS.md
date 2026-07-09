# Skill Learnings Ledger

Durable, cross-session learnings mined by `/session-end`. Newest first.
Each entry follows the format documented in `.claude/commands/session-end.md` Step 3e.
Transient session state lives in `CURRENT_WORK.md`, NOT here.

---

## 2026-07-03 — Cross-platform parity is mandatory, and it hides in the platform shell

**Learning:** Every feature must exist on **both** iOS and Android; a feature built on one OS is only half-done, and porting to the other is part of the same task (both directions). The only per-platform exception is an **OS-specific bug**, never a missing feature. Full rule in `CLAUDE.md` → *Cross-Platform Parity*; enforced by `/implement` Phase 6, `/feature` header note, and `/verify` parity checks.

**Why it matters / how it bit us:** Because ~everything is in KMP `commonMain`, features *look* cross-platform for free — but anything needing a device capability lives in the un-shared platform shell (`iosMain`/`nativeMain`/Swift/plist/Podfile vs `androidMain`/manifest/Gradle/`MainActivity`). Push notifications, iOS contacts, and Android ID-OCR were each built/working on one platform while the other silently had an **empty `actual` stub that still compiled** — the gap was invisible until an explicit iOS-vs-Android shell audit. Treat a stub `actual` (`// TODO` / returns empty) as *missing*, not *present*.

**Rule of thumb:** the moment you touch an `expect`/`actual`, a Swift file, a plist/manifest, a Podfile/Gradle dep, or platform DI — mirror it on the other OS in the same unit of work, and build-verify both (Android: check the real gradle exit code, never pipe `xcodebuild`/`gradlew` through `tail`).
