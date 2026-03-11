# iOS platform guidance

This file contains guidance for work inside iOS-specific platform areas.

## Scope

Use this guidance when working in:
- `ios/`
- `cmp-ios/`
- iOS-specific code inside shared modules
- CocoaPods / framework bridging
- iOS packaging and platform behavior

## Why this area matters

iOS in this repo is a platform target sitting on top of shared KMP code. Changes here may involve framework packaging, Podfile integration, iOS app bootstrap, simulator/device behavior, and iOS-specific platform wiring.

## Known upstream facts

The upstream project is intended to run an iOS app via `cmp-ios`, and `cmp-shared` is configured to produce a CocoaPods framework with deployment target iOS 16.0 and a Podfile located at `../cmp-ios/Podfile`.

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