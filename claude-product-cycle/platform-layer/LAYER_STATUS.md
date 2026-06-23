# Layer Status — Platform Layer

**Last Updated**: 2026-06-23

---

## Platform Summary

| Platform | Status | Build Module | Notes |
|----------|:------:|:------------:|-------|
| Android | ✅ Primary | `cmp-android/` | Production ready |
| iOS | ✅ Supported | `cmp-ios/` | CocoaPods integration |
| Desktop | ✅ Supported | `cmp-desktop/` | JVM target |
| Web | ⚠️ Experimental | `cmp-web/` | Kotlin/JS + WASM |

---

## Build Commands

### Android

```bash
# Debug APK
./gradlew :cmp-android:assembleDebug

# Release APK
./gradlew :cmp-android:assembleRelease

# Install on device
./gradlew :cmp-android:installDebug

# Run tests
./gradlew :cmp-android:connectedDebugAndroidTest
```

### iOS

```bash
# Install CocoaPods dependencies
cd cmp-ios && pod install

# Build framework
./gradlew :cmp-ios:generateDerivedXcodeProject

# Open in Xcode
open cmp-ios/MifosPayIOS.xcworkspace
```

### Desktop

```bash
# Run desktop app
./gradlew :cmp-desktop:run

# Package
./gradlew :cmp-desktop:package
```

### Web

```bash
# Run dev server
./gradlew :cmp-web:wasmJsBrowserDevelopmentRun

# Build for production
./gradlew :cmp-web:wasmJsBrowserProductionWebpack
```

---

## All Platforms Build

```bash
./gradlew build
```

---

## Known Issues

| Platform | Issue | Severity |
|----------|-------|----------|
| Web | WASM target experimental | P2 |
| iOS | Requires Xcode + CocoaPods | Env setup |
