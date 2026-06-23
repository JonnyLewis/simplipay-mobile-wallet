# /implement - E2E Feature Implementation

## Purpose

Full end-to-end implementation using O(1) lookup and pattern detection. Implements client layer (Network + Data) and feature layer (UI) with automatic code generation matching existing codebase conventions.

---

## Command Variants

```
/implement                       # Show feature status list
/implement [Feature]             # Full E2E implementation
/implement [Feature] --quick     # Skip checkpoints
/implement [Feature] --no-git    # Skip git integration
/implement improve [Feature]     # Improve existing feature
```

---

## E2E Pipeline with O(1) Optimization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  /implement [Feature] - O(1) OPTIMIZED PIPELINE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PHASE 0: CONTEXT LOADING (O(1))        ~50-200 lines instead of scanning   │
│  ├─→ Read FEATURE_MAP.md                → Get services + repositories       │
│  ├─→ Read MODULES_INDEX.md              → Get module structure              │
│  ├─→ Read SCREENS_INDEX.md              → Get existing screens/VMs          │
│  └─→ Read feature/*/SPEC.md + API.md    → Get requirements                  │
│                                                                              │
│  PHASE 1: PATTERN DETECTION             Match existing conventions          │
│  ├─→ Read existing ViewModel            → Extract State/Event/Action pattern│
│  ├─→ Read existing Screen               → Extract Composable pattern        │
│  ├─→ Read existing Repository           → Extract DataState pattern         │
│  └─→ Store conventions in memory        → Apply to generated code           │
│                                                                              │
│  PHASE 2: CLIENT LAYER                  Services + Repositories             │
│  ├─→ Check if exists in FEATURE_MAP     → Skip or create                    │
│  ├─→ Generate with pattern matching     → Matches existing code style       │
│  ├─→ Register in KtorfitClient/DI       → API manager + RepositoryModule    │
│  ├─→ Build: ./gradlew :core:network:build :core:data:build                  │
│  └─→ ⏸️ CHECKPOINT                                                           │
│                                                                              │
│  PHASE 3: FEATURE LAYER                 ViewModel + Screen + Navigation     │
│  ├─→ Generate ViewModel (MVI)           → With testTags built-in            │
│  ├─→ Generate Screen                    → With design tokens if available   │
│  ├─→ Generate Navigation                → Type-safe routes                  │
│  ├─→ Register in DI (KoinModules.kt)    → Feature Koin module               │
│  ├─→ Register in MifosNavHost.kt        → Navigation graph                  │
│  ├─→ Build: ./gradlew :feature:[name]:build                                 │
│  └─→ ⏸️ CHECKPOINT                                                           │
│                                                                              │
│  PHASE 4: FINALIZE                      Update indexes + status             │
│  ├─→ Update FEATURE_MAP.md              → Add new mappings                  │
│  ├─→ Update MODULES_INDEX.md            → Add module entry                  │
│  ├─→ Update SCREENS_INDEX.md            → Add screen entries                │
│  ├─→ Update STATUS.md files             → Mark as implemented               │
│  └─→ Final build: ./gradlew build                                           │
│                                                                              │
│  PHASE 5: TEST STUBS (TDD Support)      Generate test scaffolding           │
│  ├─→ Generate ViewModel test            → commonTest with Turbine           │
│  ├─→ Generate Screen test               → androidInstrumentedTest           │
│  ├─→ Generate Fake repository           → For testing isolation             │
│  ├─→ Update TESTING_STATUS.md           → Mark stubs created                │
│  └─→ ⏸️ CHECKPOINT                       → Review generated tests            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PHASE 0: O(1) Context Loading

### Step 0.1: Read Index Files

| File | Purpose | Data Extracted |
|------|---------|----------------|
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Service/Repo mapping | services[], repositories[] |
| `claude-product-cycle/feature-layer/MODULES_INDEX.md` | Module structure | moduleExists, vmCount, screenCount |
| `claude-product-cycle/feature-layer/SCREENS_INDEX.md` | Screen details | existingScreens[], existingViewModels[] |
| `claude-product-cycle/design-spec-layer/features/[name]/SPEC.md` | Requirements | screens[], actions[], states[] |
| `claude-product-cycle/design-spec-layer/features/[name]/API.md` | Endpoints | endpoints[], dtos[] |

---

## PHASE 1: Pattern Detection

### Step 1.1: Read Reference Files

```
1. ViewModel Reference:
   feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeViewModel.kt

2. Screen Reference:
   feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeScreen.kt

3. Repository Reference:
   core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/BeneficiaryRepository.kt
   core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/BeneficiaryRepositoryImpl.kt
```

> **Note**: `home` uses the canonical flat layout. Before reading or writing files in any other module, verify its actual directory structure — some modules place files in `viewmodel/`, `screens/`, or `ui/` subdirectories, or use dot-separated sub-packages. See MODULES_INDEX.md Path Pattern notes for known exceptions.

### Step 1.2: Extract Patterns

```kotlin
// Extracted ViewModel Pattern
val vmPattern = ViewModelPattern(
    baseClass = "BaseViewModel<State, Event, Action>",
    baseImport = "org.mifospay.core.ui.utils.BaseViewModel",
    stateAnnotation = "@Immutable",
    eventPattern = "sealed interface ${Feature}Event",
    actionPattern = "sealed interface ${Feature}Action",
    handleActionPattern = "override fun handleAction(action: ${Feature}Action)",
    dataLoadingPattern = "launchIO { repository.method().collect { ... } }"
)

// Extracted Repository Pattern
val repoPattern = RepositoryPattern(
    returnType = "Flow<DataState<T>>",
    implPattern = "asDataStateFlow().flowOn(ioDispatcher)",
    mutationPattern = "withContext(ioDispatcher) { apiManager.service.method() }"
)
```

---

## PHASE 2: Client Layer

### Step 2.1: Check Existing (O(1))

From FEATURE_MAP.md, check if services/repositories exist:

```markdown
## Decision Matrix

| Component | Exists | Action |
|-----------|:------:|--------|
| BeneficiaryService | ✅ | Skip creation |
| BeneficiaryRepository | ✅ | Skip creation |
| NewFeatureService | ❌ | CREATE |
| NewFeatureRepository | ❌ | CREATE |
```

### Step 2.2: API Manager Selection

Choose based on endpoint type:

| Manager | Used For |
|---------|----------|
| `SelfServiceApiManager` | Self-service APIs: beneficiaries, savings accounts, transfers, user, clients |
| `FineractApiManager` | Direct Fineract APIs: KYC, invoices, autopay, bills, billers, saved cards, standing instructions, notifications |

### Step 2.3: Generate Service (if needed)

```kotlin
// Generated from SPEC.md + API.md + pattern detection
interface ${Feature}Service {

    @GET(ApiEndPoints.${ENDPOINT_CONSTANT})
    fun get${Feature}List(): Flow<List<${Dto}>>

    @GET(ApiEndPoints.${ENDPOINT_CONSTANT} + "/{id}")
    fun get${Feature}ById(@Path("id") id: Long): Flow<${Dto}>

    @POST(ApiEndPoints.${ENDPOINT_CONSTANT})
    suspend fun create${Feature}(@Body payload: ${Payload})

    @PUT(ApiEndPoints.${ENDPOINT_CONSTANT} + "/{id}")
    suspend fun update${Feature}(
        @Path("id") id: Long,
        @Body payload: ${Payload},
    )

    @DELETE(ApiEndPoints.${ENDPOINT_CONSTANT} + "/{id}")
    suspend fun delete${Feature}(@Path("id") id: Long): Unit
}
```

Register in KtorfitClient and API Manager (two steps):
```kotlin
// Step 1 — add to KtorfitClient.kt:
internal val ${feature}Api by lazy { ktorfit.create${Feature}Service() }

// Step 2 — expose in the appropriate manager (SelfServiceApiManager or FineractApiManager):
val ${feature}Api by lazy { ktorfitClient.${feature}Api }
```

### Step 2.4: Generate Repository (if needed)

**Interface:**
```kotlin
interface ${Feature}Repository {
    suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>>
    suspend fun get${Feature}ById(id: Long): Flow<DataState<${Model}>>
    suspend fun create${Feature}(data: ${Payload}): DataState<String>
    suspend fun update${Feature}(id: Long, data: ${Payload}): DataState<String>
    suspend fun delete${Feature}(id: Long): DataState<String>
}
```

**Implementation (pattern-matched from BeneficiaryRepositoryImpl):**
```kotlin
class ${Feature}RepositoryImpl(
    private val apiManager: SelfServiceApiManager,  // or FineractApiManager
    private val ioDispatcher: CoroutineDispatcher,
) : ${Feature}Repository {

    override suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>> {
        return apiManager.${feature}Api.get${Feature}List()
            .asDataStateFlow()
            .flowOn(ioDispatcher)
    }

    override suspend fun create${Feature}(data: ${Payload}): DataState<String> {
        return try {
            withContext(ioDispatcher) {
                apiManager.${feature}Api.create${Feature}(data)
            }
            DataState.Success("${Feature} created successfully")
        } catch (e: Exception) {
            DataState.Error(e)
        }
    }
}
```

### Step 2.5: Register DI

**RepositoryModule.kt:**
```kotlin
// ioDispatcher is defined at file scope in RepositoryModule.kt as:
//   private val ioDispatcher = named(MifosDispatchers.IO.name)
single<${Feature}Repository> { ${Feature}RepositoryImpl(get(), get(ioDispatcher)) }
```

### Step 2.6: Build & Verify

```bash
./gradlew :core:network:build :core:data:build
./gradlew spotlessApply --no-configuration-cache
```

---

## PHASE 3: Feature Layer

### Step 3.1: Generate ViewModel (MVI Pattern)

```kotlin
internal class ${Feature}ViewModel(
    private val repository: ${Feature}Repository,
) : BaseViewModel<${Feature}State, ${Feature}Event, ${Feature}Action>(
    initialState = ${Feature}State()
) {

    init {
        load${Feature}()
    }

    override fun handleAction(action: ${Feature}Action) {
        when (action) {
            is ${Feature}Action.Retry -> load${Feature}()
            is ${Feature}Action.OnItemClick -> handleItemClick(action.id)
            // ... from SPEC.md actions
        }
    }

    private fun load${Feature}() {
        launchIO {
            repository.get${Feature}List()
                .collect { dataState ->
                    when (dataState) {
                        is DataState.Loading -> mutableStateFlow.update {
                            it.copy(uiState = ${Feature}UiState.Loading)
                        }
                        is DataState.Success -> mutableStateFlow.update {
                            it.copy(
                                uiState = ${Feature}UiState.Success,
                                data = dataState.data
                            )
                        }
                        is DataState.Error -> mutableStateFlow.update {
                            it.copy(uiState = ${Feature}UiState.Error(dataState.exception.message ?: "Error"))
                        }
                    }
                }
        }
    }
}
```

### Step 3.2: Generate Screen with TestTags

```kotlin
internal object ${Feature}TestTags {
    const val SCREEN = "${feature}:screen"
    const val LOADING = "${feature}:loading"
    const val ERROR = "${feature}:error"
    const val LIST = "${feature}:list"
    const val ITEM_PREFIX = "${feature}:item:"  // + id
    const val RETRY_BUTTON = "${feature}:retry"
    const val ADD_BUTTON = "${feature}:add"
}

@Composable
fun ${Feature}Screen(
    viewModel: ${Feature}ViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ${Feature}Event.NavigateBack -> onNavigateBack()
                is ${Feature}Event.NavigateToDetail -> onNavigateToDetail(event.id)
            }
        }
    }

    ${Feature}Content(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = Modifier.testTag(${Feature}TestTags.SCREEN)
    )
}
```

### Step 3.3: Generate Navigation

```kotlin
// navigation/${Feature}Navigation.kt
fun NavGraphBuilder.${feature}Screen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
) {
    composable<${Feature}Route> {
        ${Feature}Screen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

@Serializable
data object ${Feature}Route
```

Register in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/MifosNavHost.kt`.

### Step 3.4: Generate DI Module

```kotlin
// di/${Feature}Module.kt
val ${feature}Module = module {
    viewModelOf(::${Feature}ViewModel)
}
```

Register in `cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt`.

### Step 3.5: Build & Verify

```bash
./gradlew :feature:${name}:build
./gradlew spotlessApply detekt --no-configuration-cache
```

---

## PHASE 4: Finalize

### Step 4.1: Update O(1) Index Files

**FEATURE_MAP.md:**
```markdown
| ${feature} | ${Service} | ${Repository} | Notes |
```

**MODULES_INDEX.md:**
```markdown
| ${n} | ${module} | feature/${module} | ✅ | ${vmCount} | ${screenCount} |
```

**SCREENS_INDEX.md:**
```markdown
### ${module} (${screenCount} screens)

| Screen | ViewModel | File |
|--------|-----------|------|
| ${Screen}Screen | ${Screen}ViewModel | ${Screen}Screen.kt |
```

### Step 4.2: Final Build

```bash
./gradlew build
git add .
git commit -m "feat(${feature}): complete E2E implementation"
```

---

## PHASE 5: Test Stub Generation (TDD Support)

### Step 5.1: Generate ViewModel Test Stub

**Location**: `feature/${name}/src/commonTest/kotlin/org/mifospay/feature/${package}/${Feature}ViewModelTest.kt`

```kotlin
/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.${package}

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ${Feature}ViewModelTest {

    private lateinit var viewModel: ${Feature}ViewModel
    private lateinit var fakeRepository: Fake${Feature}Repository

    @BeforeTest
    fun setup() {
        fakeRepository = Fake${Feature}Repository()
        viewModel = ${Feature}ViewModel(
            repository = fakeRepository,
        )
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel.stateFlow.test {
            val state = awaitItem()
            assertTrue(state.uiState is ${Feature}UiState.Loading)
        }
    }

    @Test
    fun `when data loads successfully, state is success`() = runTest {
        fakeRepository.setSuccessResponse(/* test data */)
        viewModel.trySendAction(${Feature}Action.Retry)

        viewModel.stateFlow.test {
            val state = expectMostRecentItem()
            assertTrue(state.uiState is ${Feature}UiState.Success)
        }
    }

    @Test
    fun `when data load fails, state is error`() = runTest {
        fakeRepository.setErrorResponse("Network error")
        viewModel.trySendAction(${Feature}Action.Retry)

        viewModel.stateFlow.test {
            val state = expectMostRecentItem()
            assertTrue(state.uiState is ${Feature}UiState.Error)
        }
    }

    @Test
    fun `item click emits navigation event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.trySendAction(${Feature}Action.OnItemClick(itemId = 1L))
            val event = awaitItem()
            assertTrue(event is ${Feature}Event.NavigateToDetail)
        }
    }
}
```

### Step 5.2: Generate Fake Repository

**Location**: `feature/${name}/src/commonTest/kotlin/org/mifospay/feature/${package}/testing/Fake${Feature}Repository.kt`

> Note: There is no `core/testing` module in this project. Place fake repositories in the feature module's own `commonTest` source set.

```kotlin
class Fake${Feature}Repository : ${Feature}Repository {

    var loadCallCount = 0
        private set

    private var response: DataState<List<${Model}>> = DataState.Loading

    fun setSuccessResponse(data: List<${Model}>) {
        response = DataState.Success(data)
    }

    fun setErrorResponse(message: String) {
        response = DataState.Error(Exception(message))
    }

    fun setEmptyResponse() {
        response = DataState.Success(emptyList())
    }

    override suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>> = flow {
        loadCallCount++
        emit(DataState.Loading)
        emit(response)
    }
}
```

---

## Checkpoint Templates

### CLIENT Checkpoint

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ✅ CLIENT LAYER COMPLETE (Phase 2)                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  📚 O(1) Context Used:                                                        │
│  ├─ FEATURE_MAP.md → Identified existing: [services], [repos]                │
│  └─ API.md → Mapped [n] endpoints                                            │
│                                                                               │
│  🔧 Created/Updated:                                                          │
│  ├─ core/network/services/${Feature}Service.kt     [CREATED|UPDATED|SKIPPED] │
│  ├─ core/data/repository/${Feature}Repository.kt   [CREATED|UPDATED|SKIPPED] │
│  ├─ core/data/repositoryImpl/${Feature}RepositoryImpl.kt                     │
│  └─ DI Modules                                     [REGISTERED]              │
│                                                                               │
│  📊 Pattern Matching:                                                         │
│  └─ Applied patterns from: BeneficiaryRepositoryImpl.kt                      │
│                                                                               │
│  🔨 BUILD: :core:network ✅ :core:data ✅                                     │
│  🧹 LINT: spotlessApply ✅                                                    │
│                                                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│  Options:                                                                     │
│  • c / continue  → Proceed to FEATURE layer                                  │
│  • i / improve   → Describe improvements                                     │
│  • v / view      → Show generated file                                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

### FEATURE Checkpoint

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ✅ FEATURE LAYER COMPLETE (Phase 3)                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  🔧 Created/Updated:                                                          │
│  ├─ feature/${name}/${Feature}ViewModel.kt                                   │
│  ├─ feature/${name}/${Feature}Screen.kt                                      │
│  ├─ feature/${name}/navigation/${Feature}Navigation.kt                       │
│  └─ feature/${name}/di/${Feature}Module.kt                                   │
│                                                                               │
│  🧭 Registered in:                                                            │
│  ├─ KoinModules.kt                                                           │
│  └─ MifosNavHost.kt                                                          │
│                                                                               │
│  📊 Pattern Matching:                                                         │
│  └─ Applied patterns from: HomeViewModel.kt, HomeScreen.kt                   │
│                                                                               │
│  🔨 BUILD: :feature:${name} ✅                                                │
│  🧹 LINT: spotlessApply ✅ detekt ✅                                          │
│                                                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│  Options:                                                                     │
│  • c / continue  → Finalize and update indexes                               │
│  • i / improve   → Describe improvements                                     │
│  • v / view [file] → Show specific file                                      │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Final Report

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║  /implement ${Feature} - COMPLETE                                             ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                                ║
║  ✅ PHASE 2: CLIENT LAYER                                                      ║
║     ├─ Files: [n] created, [n] updated                                        ║
║     └─ Build: :core:network ✅ :core:data ✅                                   ║
║                                                                                ║
║  ✅ PHASE 3: FEATURE LAYER                                                     ║
║     ├─ Files: [n] created                                                     ║
║     ├─ TestTags: [n] generated                                                ║
║     ├─ Registered in: KoinModules.kt, MifosNavHost.kt                        ║
║     └─ Build: :feature:${name} ✅                                             ║
║                                                                                ║
║  ✅ PHASE 4: FINALIZE                                                          ║
║     ├─ Updated: FEATURE_MAP.md, MODULES_INDEX.md, SCREENS_INDEX.md           ║
║     └─ Final Build: ./gradlew build ✅                                        ║
║                                                                                ║
║  ✅ PHASE 5: TEST STUBS                                                        ║
║     ├─ ${Feature}ViewModelTest.kt ✅                                          ║
║     └─ Fake${Feature}Repository.kt ✅                                         ║
║                                                                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║  Next steps:                                                                   ║
║  • Verify: /verify ${Feature}                                                 ║
║  • Test: /verify-tests ${Feature}                                             ║
║                                                                                ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

---

## Feature List (No Argument)

When `/implement` called without arguments, read MODULES_INDEX.md:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  📋 FEATURES - Implementation Status (from MODULES_INDEX.md)                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  | # | Feature              | Client | Feature | Gaps | Command           │  │
│  |:-:|----------------------|:------:|:-------:|:----:|------------------|  │
│  | 1 | auth                 | ✅     | ✅      | 0    | /implement auth   │  │
│  | 2 | home                 | ✅     | ✅      | 0    | /implement home   │  │
│  | 3 | accounts             | ✅     | ✅      | 0    | /implement accounts│ │
│  | 4 | beneficiary          | ✅     | ✅      | 0    | /implement beneficiary│
│  | 5 | transfer-intrabank   | ✅     | ✅      | 0    | /implement transfer-intrabank│
│  | ...                                                                       │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/client [Feature]` | Client layer only |
| `/feature [Feature]` | Feature layer only |
| `/verify [Feature]` | Verify implementation vs spec |
| `/verify-tests [Feature]` | Run tests |
| `/gap-analysis` | Check what needs implementation |
