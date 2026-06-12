---
scope: cmp-ios/
features: []
last-synced: bc52526c
budget: 150
---

# cmp-ios — iOS app entry point

Read after the root [AGENTS.md](../AGENTS.md) and the iOS platform guidance in [`ios/AGENTS.md`](../ios/AGENTS.md), whose rules (CocoaPods/framework packaging, shared-vs-shell separation) all apply here.

## What exists

- `iosApp/iOSApp.swift`, `iosApp/ContentView.swift` — Swift shell hosting the `cmp-shared` Compose framework.
- `iosApp.xcodeproj` / `iosApp.xcworkspace` — open the **workspace** when Pods are involved.
- `Podfile`, `Podfile.lock`, `Pods/` — CocoaPods integration; `cmp-shared` is produced as a CocoaPods framework (deployment target iOS 16.0) whose Podfile path is configured as `../cmp-ios/Podfile`.
- `Configuration/Config.xcconfig`, `iosApp/Info.plist`, `iosApp/GoogleService-Info.plist`, asset catalogs.

## Invariants and gotchas

- This is a thin shell — almost everything observable on iOS originates in shared KMP code. Do not fix shared bugs here.
- Podfile / framework integration is load-bearing and coupled to `cmp-shared/build.gradle.kts` CocoaPods config; change them together or not at all.
- Below the source-file trigger (2 Swift files) — this context file exists for the packaging invariants, not the code volume.

## Feature map

| Feature | What it added here |
|---|---|
| — | none tracked yet |
