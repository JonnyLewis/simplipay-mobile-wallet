# iOS platform guidance

This file contains guidance for work inside iOS-specific platform areas.

> **Directory status:** The `ios/` directory currently contains only this
> CLAUDE.md file. There is no iOS application source code here.
> The iOS application shell is in `cmp-ios/`.

## Scope

Use this guidance when working in:
- `cmp-ios/` — the real iOS application shell
- iOS-specific code inside shared modules
- CocoaPods / framework bridging
- iOS packaging and platform behavior

(`ios/` itself has no source code and is not an active work area.)

## Why this area matters

iOS in this repo is a platform target sitting on top of shared KMP code. Changes here may involve framework packaging, Podfile integration, iOS app bootstrap, simulator/device behavior, and iOS-specific platform wiring.

## Known upstream facts

The iOS app shell is `cmp-ios`. The `cmp-shared` module produces a CocoaPods framework with iOS deployment target 16.0, using the Podfile at `../cmp-ios/Podfile`.

## Known facts: cmp-ios

Confirmed from `cmp-ios/` filesystem and `cmp-shared/build.gradle.kts`:

- **Xcode workspace:** `cmp-ios/iosApp.xcworkspace`
  *(Always open the workspace, not the `.xcodeproj`, when CocoaPods is in use)*
- **Xcode project:** `cmp-ios/iosApp.xcodeproj`
- **Podfile:** `cmp-ios/Podfile`
- **CocoaPods framework imported by iOS code:** `ComposeApp`
  (static; produced by `cmp-shared`, `baseName = "ComposeApp"`)
- **iOS deployment target:** 16.0
- **`cmp-ios/Configuration/`:** This directory exists and likely contains build
  configurations. Its contents have not been audited in detail; inspect before
  modifying.

## iOS rules

Before editing iOS code, identify:
- whether the change belongs in `cmp-ios` or in shared code
- whether CocoaPods / framework generation is involved
- whether the issue is shared logic surfacing on iOS, or truly iOS-specific behavior
- whether iOS-specific storage, permissions, or lifecycle behavior is involved

When working in iOS-specific areas:
- keep iOS shell logic separate from shared business logic
- do not move shared logic into iOS-only code casually
- preserve Podfile / framework integration unless the task explicitly targets it
- call out when an iOS issue appears to originate in shared code

## Validation expectations

Prefer the narrowest useful validation first:
- affected iOS target
- relevant shared module if the issue originates there
- simulator/device-specific validation only when justified

## Avoid

- casual changes to CocoaPods / framework packaging
- treating shared logic bugs as iOS-only by default
- mixing iOS shell changes with shared behavior changes without stating blast radius
