# /verify-tests - Test Verification (O(1) Enhanced)

Run and verify tests for features across the project with O(1) status lookups.

## Usage

```
/verify-tests                        # Show test status dashboard (O(1))
/verify-tests [feature]              # Run all tests for feature
/verify-tests [feature] unit         # Run ViewModel tests only
/verify-tests [feature] ui           # Run UI tests only
/verify-tests [feature] integration  # Run integration tests
/verify-tests [feature] screenshot   # Run screenshot tests
/verify-tests client                 # Run all client layer tests
/verify-tests feature                # Run all feature layer tests
/verify-tests platform               # Run all platform tests
```

---

## O(1) Workflow

```
+-------------------------------------------------------------------------+
|                /verify-tests WORKFLOW (O(1) ENHANCED)                    |
+-------------------------------------------------------------------------+
|                                                                          |
|  PHASE 0: O(1) CONTEXT LOADING                                          |
|  +--> Read feature-layer/TESTING_STATUS.md     --> VM/Screen test status|
|  +--> Read client-layer/TESTING_STATUS.md      --> Repository test status|
|  +--> Read platform-layer/TESTING_STATUS.md    --> E2E/Screenshot status|
|  +--> Read feature-layer/MODULES_INDEX.md      --> Feature paths        |
|                                                                          |
|  PHASE 1: DETERMINE TEST SCOPE                                          |
|  +--> If no args: Show test dashboard from indexes                      |
|  +--> If [feature]: Get paths from MODULES_INDEX                        |
|  +--> If [layer]: Get layer test config                                 |
|                                                                          |
|  PHASE 2: EXECUTE TESTS                                                 |
|  +--> Build appropriate Gradle command                                  |
|  +--> Run tests via Bash                                                |
|  +--> Capture output                                                    |
|                                                                          |
|  PHASE 3: PARSE RESULTS                                                 |
|  +--> Extract: passed, failed, skipped                                  |
|  +--> Extract: failure details                                          |
|  +--> Calculate coverage (if available)                                 |
|                                                                          |
|  PHASE 4: UPDATE STATUS                                                 |
|  +--> Update TESTING_STATUS.md with new results                         |
|  +--> Log test run timestamp                                            |
|                                                                          |
|  PHASE 5: REPORT                                                        |
|  +--> Show results with next steps                                      |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## Phase 0: O(1) Context Loading

### Index Files to Read

| File | Purpose | Lines |
|------|---------|:-----:|
| `claude-product-cycle/design-spec-layer/TESTING_STATUS.md` | **Primary** test status (all features) | ~200 |
| `claude-product-cycle/feature-layer/MODULES_INDEX.md` | Feature → Path mapping | ~180 |

*Per-layer testing indexes (`feature-layer/TESTING_STATUS.md`, `client-layer/TESTING_STATUS.md`, `platform-layer/TESTING_STATUS.md`) are created by this command when tests are first written — they will not exist initially.*

### O(1) Path Pattern

```
feature/[module]/src/commonTest/             # Unit tests (ViewModel) — the only test infra wired today
core/data/src/commonTest/                    # Repository tests
```

> **Test infrastructure state**: Only `commonTest` (kotlin.test + Turbine, fakes in-module) is set up in this repo. There are **no** `androidInstrumentedTest` source sets, no `cmp-android` E2E suite, and no Roborazzi screenshot tests yet. The UI / integration / screenshot sections below describe the *intended* layout for when that infra is added — their Gradle commands are not runnable until those source sets exist.

---

## If No Arguments: Test Dashboard

Read from TESTING_STATUS.md files and show:

```
+=========================================================================+
|                      TEST STATUS DASHBOARD (O(1))                        |
+=========================================================================+

## Layer Summary

| Layer | Tests | Passed | Failed | Coverage | Status |
|-------|:-----:|:------:|:------:|:--------:|:------:|
| Client | - | - | - | 0% | [          ] |
| Feature | 0 | 0 | 0 | 0% | [          ] |
| Platform | 0 | 0 | 0 | 0% | [          ] |

## Feature Testing Matrix (from TESTING_STATUS.md)

| Feature | VMs | VM Tests | Screens | UI Tests | Status |
|---------|:---:|:--------:|:-------:|:--------:|:------:|
| auth | - | 0 | - | 0 | [ ] Not Started |
| home | 1 | 0 | 1 | 0 | [ ] Not Started |
| accounts | - | 0 | - | 0 | [ ] Not Started |
| ... (from feature-layer/TESTING_STATUS.md)

## Repository Testing (from client-layer/TESTING_STATUS.md)

| Repository | Tests | Success | Error | Empty | Status |
|------------|:-----:|:-------:|:-----:|:-----:|:------:|
| BeneficiaryRepository | 0 | [ ] | [ ] | [ ] | Not Started |
| AuthRepository | 0 | [ ] | [ ] | [ ] | Not Started |
| ... (from client-layer/TESTING_STATUS.md)

## Quick Commands

| Action | Command |
|--------|---------|
| Run all Android unit tests | `./gradlew testDebugUnitTest` |
| Run feature tests | `/verify-tests [feature]` |
| Run client tests | `/verify-tests client` |
| Check gaps | `/gap-analysis testing` |

+=========================================================================+
```

---

## Feature Test Mapping (from MODULES_INDEX.md)

| # | Feature | Module Path | Unit Test Path | UI Test Path |
|:-:|---------|-------------|----------------|--------------|
| 1 | auth | feature/auth | feature/auth/src/commonTest/ | feature/auth/src/androidInstrumentedTest/ |
| 2 | home | feature/home | feature/home/src/commonTest/ | feature/home/src/androidInstrumentedTest/ |
| 3 | accounts | feature/accounts | feature/accounts/src/commonTest/ | feature/accounts/src/androidInstrumentedTest/ |
| 4 | history | feature/history | feature/history/src/commonTest/ | feature/history/src/androidInstrumentedTest/ |
| 5 | receipt | feature/receipt | feature/receipt/src/commonTest/ | feature/receipt/src/androidInstrumentedTest/ |
| 6 | faq | feature/faq | feature/faq/src/commonTest/ | feature/faq/src/androidInstrumentedTest/ |
| 7 | make-transfer | feature/make-transfer | feature/make-transfer/src/commonTest/ | feature/make-transfer/src/androidInstrumentedTest/ |
| 8 | send-money | feature/send-money | feature/send-money/src/commonTest/ | feature/send-money/src/androidInstrumentedTest/ |
| 9 | transfer-intrabank | feature/transfer-intrabank | feature/transfer-intrabank/src/commonTest/ | feature/transfer-intrabank/src/androidInstrumentedTest/ |
| 10 | transfer-interbank | feature/transfer-interbank | feature/transfer-interbank/src/commonTest/ | feature/transfer-interbank/src/androidInstrumentedTest/ |
| 11 | notification | feature/notification | feature/notification/src/commonTest/ | feature/notification/src/androidInstrumentedTest/ |
| 12 | editpassword | feature/editpassword | feature/editpassword/src/commonTest/ | feature/editpassword/src/androidInstrumentedTest/ |
| 13 | kyc | feature/kyc | feature/kyc/src/commonTest/ | feature/kyc/src/androidInstrumentedTest/ |
| 14 | savedcards | feature/savedcards | feature/savedcards/src/commonTest/ | feature/savedcards/src/androidInstrumentedTest/ |
| 15 | invoices | feature/invoices | feature/invoices/src/commonTest/ | feature/invoices/src/androidInstrumentedTest/ |
| 16 | settings | feature/settings | feature/settings/src/commonTest/ | feature/settings/src/androidInstrumentedTest/ |
| 17 | profile | feature/profile | feature/profile/src/commonTest/ | feature/profile/src/androidInstrumentedTest/ |
| 18 | finance | feature/finance | feature/finance/src/commonTest/ | feature/finance/src/androidInstrumentedTest/ |
| 19 | merchants | feature/merchants | feature/merchants/src/commonTest/ | feature/merchants/src/androidInstrumentedTest/ |
| 20 | beneficiary | feature/beneficiary | feature/beneficiary/src/commonTest/ | feature/beneficiary/src/androidInstrumentedTest/ |
| 21 | standing-instruction | feature/standing-instruction | feature/standing-instruction/src/commonTest/ | feature/standing-instruction/src/androidInstrumentedTest/ |
| 22 | payments | feature/payments | feature/payments/src/commonTest/ | feature/payments/src/androidInstrumentedTest/ |
| 23 | upi-setup | feature/upi-setup | feature/upi-setup/src/commonTest/ | feature/upi-setup/src/androidInstrumentedTest/ |
| 24 | qr | feature/qr | feature/qr/src/commonTest/ | feature/qr/src/androidInstrumentedTest/ |
| 25 | autopay | feature/autopay | feature/autopay/src/commonTest/ | feature/autopay/src/androidInstrumentedTest/ |
| 26 | mpay-qr | feature/mpay-qr | feature/mpay-qr/src/commonTest/ | feature/mpay-qr/src/androidInstrumentedTest/ |
| 27 | mpay-qr-scan | feature/mpay-qr-scan | feature/mpay-qr-scan/src/commonTest/ | feature/mpay-qr-scan/src/androidInstrumentedTest/ |
| 28 | fast-mpay | feature/fast-mpay | feature/fast-mpay/src/commonTest/ | feature/fast-mpay/src/androidInstrumentedTest/ |
| 29 | passcode | feature/passcode | feature/passcode/src/commonTest/ | feature/passcode/src/androidInstrumentedTest/ |

> The **UI Test Path** column is the intended location for Compose UI tests. These `androidInstrumentedTest` source sets do **not** exist yet — only the `commonTest` (Unit Test Path) source sets are in use today.

---

## Test Type Commands

### `/verify-tests [feature]` - All Tests

```bash
# Unit tests (ViewModel) — Android target
./gradlew :feature:[module]:testDebugUnitTest

# Common/JVM tests on the desktop target
./gradlew :feature:[module]:desktopTest
```

### `/verify-tests [feature] unit` - ViewModel Only

```bash
./gradlew :feature:[module]:testDebugUnitTest   # or :desktopTest for JVM
```

### `/verify-tests [feature] ui` - Screen Only

```bash
# NOT YET WIRED — no androidInstrumentedTest source sets exist.
# When UI tests are added: ./gradlew :feature:[module]:connectedDebugAndroidTest
```

### `/verify-tests [feature] integration` - E2E Flow

```bash
# NOT YET WIRED — no cmp-android instrumented E2E suite exists.
# Intended: ./gradlew :cmp-android:connectedDebugAndroidTest \
#   -Pandroid.testInstrumentationRunnerArguments.class=org.mifospay.[Feature]FlowTest
```

### `/verify-tests [feature] screenshot` - Visual

```bash
# NOT YET WIRED — no Roborazzi screenshot tests exist yet (the plugin is applied,
# but core/designsystem has no test source set / golden images).
# Intended: ./gradlew :core:designsystem:compareRoborazziDebug   # compare
#           ./gradlew :core:designsystem:recordRoborazziDebug    # record
```

---

## Layer Test Commands

### `/verify-tests client` - Repository Tests

```bash
./gradlew :core:data:desktopTest
```

### `/verify-tests feature` - All Feature Tests

```bash
./gradlew testDebugUnitTest   # runs the Android unit-test task across all modules that have it
```

### `/verify-tests platform` - Platform Tests

```bash
# NOT YET WIRED — E2E and screenshot infra do not exist yet (see note in Phase 0).
# Intended:
#   ./gradlew :cmp-android:connectedDebugAndroidTest      # E2E
#   ./gradlew :core:designsystem:compareRoborazziDebug    # screenshots
```

---

## Output Format

### After Running Tests

```
+=========================================================================+
|  VERIFY TESTS - [target]                                                 |
+=========================================================================+

## Test Execution

| Type | Command | Tests | Passed | Failed | Status |
|------|---------|:-----:|:------:|:------:|:------:|
| Unit | `./gradlew :feature:auth:testDebugUnitTest` | 45 | 45 | 0 | [x] |
| UI | `(not yet wired)` | - | - | - | - |

## Failed Tests

| Test | Error | File |
|------|-------|------|
| LoginScreenTest.testErrorState | AssertionError | LoginScreenTest.kt:45 |
| LoginScreenTest.testLoading | TimeoutException | LoginScreenTest.kt:32 |

## Coverage Summary

| Component | Coverage | Target | Status |
|-----------|:--------:|:------:|:------:|
| ViewModel | 85% | 80% | [x] Pass |
| Screen | 72% | 60% | [x] Pass |
| Repository | 90% | 80% | [x] Pass |

## Index Updated

[x] feature-layer/TESTING_STATUS.md - Updated test counts
[x] Last run: [timestamp]

+---------+----------------------------------------------------------+
|  NEXT STEPS                                                        |
+---------+----------------------------------------------------------+
| 1 | Fix failing tests: LoginScreenTest.kt:45, :32              |
| 2 | Increase coverage: Add tests for uncovered paths           |
| 3 | Re-run: /verify-tests auth                                 |
+---------+----------------------------------------------------------+
```

---

## Error Handling

### Feature Not Found

```
+-------------------------------------------------------------------------+
|  ERROR: Feature '[name]' not found                                       |
+-------------------------------------------------------------------------+
|                                                                          |
|  The feature '[name]' does not exist in MODULES_INDEX.md                |
|                                                                          |
|  Available features:                                                    |
|  auth, home, accounts, history, receipt, faq, make-transfer,            |
|  send-money, transfer-intrabank, transfer-interbank, notification,      |
|  editpassword, kyc, savedcards, invoices, settings, profile, finance,   |
|  merchants, beneficiary, standing-instruction, payments, upi-setup,     |
|  qr, autopay, mpay-qr, mpay-qr-scan, fast-mpay, passcode               |
|                                                                          |
|  Did you mean: [closest match]?                                         |
|                                                                          |
+-------------------------------------------------------------------------+
```

### No Tests Found

```
+-------------------------------------------------------------------------+
|  WARNING: No tests found for '[feature]'                                 |
+-------------------------------------------------------------------------+
|                                                                          |
|  Test directory: feature/[module]/src/commonTest/                       |
|  Status: Empty                                                          |
|                                                                          |
|  To create tests:                                                       |
|  1. Run /gap-planning [feature] testing                                 |
|  2. Follow TDD pattern in TESTING_STATUS.md                             |
|                                                                          |
+-------------------------------------------------------------------------+
```

### Gradle Error

```
+-------------------------------------------------------------------------+
|  ERROR: Gradle build failed                                              |
+-------------------------------------------------------------------------+
|                                                                          |
|  Command: ./gradlew :feature:[module]:testDebugUnitTest                 |
|  Exit code: 1                                                           |
|                                                                          |
|  Error output:                                                          |
|  [Gradle error message]                                                 |
|                                                                          |
|  Suggestions:                                                           |
|  1. Check compilation: ./gradlew :feature:[module]:compileDebugKotlinAndroid |
|  2. Clean build: ./gradlew clean                                        |
|  3. Check dependencies: ./gradlew :feature:[module]:dependencies        |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## Coverage Targets

| Component | Minimum | Target | Excellent |
|-----------|:-------:|:------:|:---------:|
| ViewModel | 60% | 80% | 90%+ |
| Repository | 70% | 80% | 90%+ |
| Screen | 40% | 60% | 80%+ |
| Integration | - | 8 flows | 15+ flows |
| Screenshot | - | 30 golden | 60+ golden |

---

## TestTag System (pattern to adopt)

> This `TestTags` convention is the recommended pattern for when UI tests are added — there is no shared `TestTags` object in the codebase yet. Feature screens generated by `/feature` create a per-feature `${Feature}TestTags` object.

### Pattern: `feature:component:element`

```kotlin
object TestTags {
    object Auth {
        const val SCREEN = "auth:screen"
        const val USERNAME_FIELD = "auth:username"
        const val PASSWORD_FIELD = "auth:password"
        const val LOGIN_BUTTON = "auth:loginButton"
        const val ERROR_MESSAGE = "auth:error"
        const val LOADING_INDICATOR = "auth:loading"
    }
    // ... for all features
}
```

---

## Integration Test Flows (from platform-layer/TESTING_STATUS.md)

| # | Flow | Screens | Tests | Status |
|:-:|------|:-------:|:-----:|:------:|
| 1 | Login -> Passcode -> Home | 3 | 0 | [ ] |
| 2 | Registration -> OTP -> Login | 4 | 0 | [ ] |
| 3 | Home -> Account Details | 2 | 0 | [ ] |
| 4 | Home -> Transfer (Intrabank) -> Confirm | 3 | 0 | [ ] |
| 5 | Home -> Beneficiary -> Add | 2 | 0 | [ ] |
| 6 | Settings -> Change Password | 2 | 0 | [ ] |
| 7 | QR -> Scan -> Transfer | 3 | 0 | [ ] |
| 8 | Payments -> Autopay -> Schedule | 3 | 0 | [ ] |

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/gap-analysis testing` | View all testing gaps |
| `/gap-analysis [layer] testing` | Layer-specific test gaps |
| `/gap-planning [feature] testing` | Plan test implementation |
| `/verify [feature]` | Verify implementation vs spec |

---

## Key Files

```
claude-product-cycle/
+-- feature-layer/
|   +-- TESTING_STATUS.md        # O(1) ViewModel/Screen test status
|   +-- MODULES_INDEX.md         # Feature -> path mapping
+-- client-layer/
|   +-- TESTING_STATUS.md        # O(1) Repository test status
+-- platform-layer/
|   +-- TESTING_STATUS.md        # O(1) E2E/Screenshot status
```

---

## Gradle Commands Reference

### Unit Tests

```bash
# All Android unit tests across modules
./gradlew testDebugUnitTest

# Specific module (Android target, or desktop for common/JVM)
./gradlew :feature:auth:testDebugUnitTest
./gradlew :feature:auth:desktopTest
./gradlew :core:data:desktopTest

# With coverage (Jacoco is configured in build-logic)
./gradlew testDebugUnitTest jacocoTestReport
```

### UI Tests — NOT YET WIRED

```bash
# No androidInstrumentedTest source sets exist yet. When added:
#   ./gradlew connectedDebugAndroidTest
#   ./gradlew :feature:auth:connectedDebugAndroidTest
```

### Screenshot Tests (Roborazzi) — NOT YET WIRED

```bash
# Roborazzi plugin is applied to core/designsystem + cmp-android, but no screenshot
# tests/golden images exist yet. When added:
#   ./gradlew :core:designsystem:recordRoborazziDebug    # record
#   ./gradlew :core:designsystem:compareRoborazziDebug   # compare
#   open build/reports/roborazzi/
```
