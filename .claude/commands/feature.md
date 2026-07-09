# /feature - Feature/UI Layer Implementation

## Purpose

Implement the feature/UI layer using O(1) lookup and pattern detection. Creates ViewModel (MVI), Screen (Compose), Navigation, and DI with TestTags built-in and code matching existing codebase conventions.

---

## ⚠️ Cross-Platform Parity is MANDATORY

See `CLAUDE.md` → **Cross-Platform Parity**. The ViewModel/Screen/Navigation/DI this command generates all live in `commonMain`, so they are cross-platform automatically — no extra work. **But** if the feature needs a platform capability (camera, contacts, push, biometrics, share sheet, file access, a native SDK), that requires an `expect`/`actual` plus shell wiring (Swift/plist/Podfile on iOS, `androidMain`/manifest/Gradle on Android) — and both sides must be delivered together. A feature with only one platform's actual implemented is **not done**. Finish parity via `/implement`'s Phase 6 (Platform Parity) before reporting complete.

---

## Command Variants

```
/feature                         # Show feature layer status
/feature [Feature]               # Implement feature layer
/feature [Feature] --vm          # ViewModel only
/feature [Feature] --ui          # Screen only
/feature [Feature] --nav         # Navigation only
```

---

## Workflow with O(1) Optimization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  /feature [Feature] - O(1) OPTIMIZED WORKFLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PHASE 0: O(1) CONTEXT LOADING                                              │
│  ├─→ Read MODULES_INDEX.md            → Check if module exists              │
│  ├─→ Read SCREENS_INDEX.md            → Get existing screens/VMs            │
│  ├─→ Read FEATURE_MAP.md              → Get repository dependencies         │
│  ├─→ Read design-spec-layer/features/[name]/SPEC.md → requirements          │
│  └─→ Read design-spec-layer/features/[name]/mockups/ → design tokens        │
│                                                                              │
│  PHASE 1: PATTERN DETECTION                                                 │
│  ├─→ Read existing ViewModel          → Extract MVI pattern                 │
│  ├─→ Read existing Screen             → Extract Composable pattern          │
│  ├─→ Read existing Navigation         → Extract route pattern               │
│  └─→ Read existing TestTags           → Extract tag naming convention       │
│                                                                              │
│  PHASE 2: VIEWMODEL                                                         │
│  ├─→ Generate State class             → From SPEC.md state fields           │
│  ├─→ Generate Event sealed interface  → From SPEC.md navigation             │
│  ├─→ Generate Action sealed interface → From SPEC.md user actions           │
│  ├─→ Implement handleAction()         → Pattern-matched                     │
│  └─→ Implement data loading           → Using repository                    │
│                                                                              │
│  PHASE 3: SCREEN + TESTTAGS + DESIGN TOKENS                                 │
│  ├─→ Generate TestTags object         → feature:component pattern           │
│  ├─→ Generate main Screen composable  → With testTag modifiers              │
│  ├─→ Generate Content composable      → State-driven rendering              │
│  ├─→ Generate state composables       → Loading, Success, Error, Empty      │
│  └─→ Apply design tokens (Phase 3.5)  → If mockups/design-tokens.json exists│
│                                                                              │
│  PHASE 3.5: DESIGN TOKEN INTEGRATION (if tokens exist)                      │
│  ├─→ Read DESIGN_TOKENS_INDEX.md      → Check if feature has tokens         │
│  ├─→ Read design-tokens.json          → Parse colors, typography, components│
│  ├─→ Generate ${Feature}Theme.kt      → Feature-specific colors/gradients   │
│  ├─→ Apply component specs            → Button heights, radii, shadows      │
│  └─→ Add animation modifiers          → If animations defined               │
│                                                                              │
│  PHASE 4: NAVIGATION + DI                                                   │
│  ├─→ Generate NavGraphBuilder ext     → Type-safe navigation                │
│  ├─→ Generate Route data class        → @Serializable                       │
│  ├─→ Generate Koin module             → viewModelOf()                       │
│  └─→ Register in MifosNavHost.kt      → Update navigation graph             │
│                                                                              │
│  PHASE 5: BUILD & UPDATE INDEXES                                            │
│  ├─→ ./gradlew :feature:[name]:build                                        │
│  ├─→ ./gradlew spotlessApply detekt                                         │
│  ├─→ Update MODULES_INDEX.md          → Add/update module entry             │
│  └─→ Update SCREENS_INDEX.md          → Add screen entries                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PHASE 0: O(1) Context Loading

### Files to Read

| File | Purpose | Data Extracted |
|------|---------|----------------|
| `claude-product-cycle/feature-layer/MODULES_INDEX.md` | Module existence | moduleExists, vmCount, screenCount |
| `claude-product-cycle/feature-layer/SCREENS_INDEX.md` | Screen details | existingScreens[], existingVMs[] |
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Repository deps | repositories[] |
| `claude-product-cycle/design-spec-layer/features/[name]/SPEC.md` | UI requirements | screens[], states[], actions[], events[] |
| `claude-product-cycle/design-spec-layer/features/[name]/mockups/design-tokens.json` | Design tokens | colors, spacing, typography |
| `claude-product-cycle/design-spec-layer/DESIGN_TOKENS_INDEX.md` | Token availability | hasTokens, format, components (create if needed) |

---

## PHASE 1: Pattern Detection

### Reference Files

```
1. ViewModel Reference:
   feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeViewModel.kt

2. Screen Reference:
   feature/home/src/commonMain/kotlin/org/mifospay/feature/home/HomeScreen.kt

3. Navigation Reference (Modern — type-safe routes):
   feature/transfer-intrabank/src/commonMain/kotlin/org/mifospay/feature/transfer/intrabank/navigation/IntraBankHubNavigation.kt

   > Note: HomeNavigation.kt uses legacy string routes. For the `@Serializable data object Route` +
   > `composable<Route>` pattern used by these templates, reference transfer-intrabank instead.

4. DI Reference:
   feature/home/src/commonMain/kotlin/org/mifospay/feature/home/di/HomeModule.kt
```

> **Note**: `home` uses the canonical flat layout. Before reading or writing files in any other module, check its actual directory structure — some modules deviate:
> - `upi-setup`: ViewModels in `viewmodel/`, Screens in `screens/`; package `org.mifospay.feature.upi.setup`
> - `merchants`: Screens in `ui/`
> - `transfer-interbank`: Screens in `screens/`; package `org.mifospay.feature.transfer.interbank`
> - `beneficiary`: Files in sub-packages (`list/`, `addupdatebeneficiary/`, `deletebeneficiary/`)

### Extracted Patterns

```kotlin
// ViewModel Pattern
val vmPattern = ViewModelPattern(
    baseClass = "BaseViewModel<${Feature}State, ${Feature}Event, ${Feature}Action>",
    stateAnnotation = "@Immutable",
    initBlock = "init { load${Feature}() }",
    handleAction = "override fun handleAction(action: ${Feature}Action)",
    updateState = "mutableStateFlow.update { it.copy(...) }",
    sendEvent = "sendEvent(${Feature}Event.NavigateTo...)"
)

// Screen Pattern
val screenPattern = ScreenPattern(
    viewModelParam = "viewModel: ${Feature}ViewModel = koinViewModel()",
    stateCollection = "val state by viewModel.stateFlow.collectAsStateWithLifecycle()",
    eventCollection = "LaunchedEffect(Unit) { viewModel.eventFlow.collect { ... } }",
    contentCall = "${Feature}Content(state = state, onAction = viewModel::trySendAction)",
    testTagModifier = "Modifier.testTag(${Feature}TestTags.SCREEN)"
)

// TestTag Pattern
val testTagPattern = TestTagPattern(
    objectName = "${Feature}TestTags",
    screenTag = "${feature}:screen",
    componentTag = "${feature}:{component}",
    itemTag = "${feature}:item:{id}"
)
```

---

## PHASE 2: ViewModel

### File Location

```
feature/[name]/src/commonMain/kotlin/org/mifospay/feature/[package]/${Feature}ViewModel.kt
```

### ViewModel Template (MVI Pattern)

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

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.${Feature}Repository
// import org.mifospay.core.model.{domain}.${Model}  // e.g. org.mifospay.core.model.beneficiary.Beneficiary
import org.mifospay.core.ui.utils.BaseViewModel

internal class ${Feature}ViewModel(
    private val ${feature}Repository: ${Feature}Repository,
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
            is ${Feature}Action.OnBackClick -> sendEvent(${Feature}Event.NavigateBack)
            is ${Feature}Action.OnAddClick -> sendEvent(${Feature}Event.NavigateToAdd)
            is ${Feature}Action.Refresh -> load${Feature}()
            // ... from SPEC.md actions
        }
    }

    private fun load${Feature}() {
        launchIO {
            ${feature}Repository.get${Feature}List()
                .collect { dataState ->
                    when (dataState) {
                        is DataState.Loading -> mutableStateFlow.update {
                            it.copy(uiState = ${Feature}UiState.Loading)
                        }
                        is DataState.Success -> mutableStateFlow.update {
                            it.copy(
                                uiState = if (dataState.data.isEmpty()) {
                                    ${Feature}UiState.Empty
                                } else {
                                    ${Feature}UiState.Success
                                },
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

    private fun handleItemClick(id: Long) {
        sendEvent(${Feature}Event.NavigateToDetail(id))
    }
}

// State - fields from SPEC.md
@Immutable
data class ${Feature}State(
    val data: List<${Item}> = emptyList(),
    val uiState: ${Feature}UiState = ${Feature}UiState.Loading,
    val isRefreshing: Boolean = false,
    // ... additional fields from SPEC.md
)

// UI States
sealed interface ${Feature}UiState {
    data object Loading : ${Feature}UiState
    data object Success : ${Feature}UiState
    data object Empty : ${Feature}UiState
    data class Error(val message: String) : ${Feature}UiState
}

// Events - one-time navigation/effects from SPEC.md
sealed interface ${Feature}Event {
    data class NavigateToDetail(val id: Long) : ${Feature}Event
    data object NavigateToAdd : ${Feature}Event
    data object NavigateBack : ${Feature}Event
    data class ShowSnackbar(val message: String) : ${Feature}Event
}

// Actions - user interactions from SPEC.md
sealed interface ${Feature}Action {
    data object Retry : ${Feature}Action
    data object Refresh : ${Feature}Action
    data object OnBackClick : ${Feature}Action
    data class OnItemClick(val id: Long) : ${Feature}Action
    data object OnAddClick : ${Feature}Action
    // ... from SPEC.md
}
```

---

## PHASE 3: Screen + TestTags

### File Locations

```
feature/[name]/src/commonMain/kotlin/org/mifospay/feature/[package]/
├── ${Feature}Screen.kt
├── ${Feature}TestTags.kt
└── components/
    └── ${Feature}Item.kt
```

### TestTags Template

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

/**
 * Test tags for ${Feature} screen components.
 * Pattern: feature:component[:identifier]
 *
 * Usage in tests:
 * ```
 * composeTestRule.onNodeWithTag(${Feature}TestTags.SCREEN).assertIsDisplayed()
 * composeTestRule.onNodeWithTag(${Feature}TestTags.itemTag(123)).performClick()
 * ```
 */
internal object ${Feature}TestTags {
    // Screen
    const val SCREEN = "${feature}:screen"
    const val TOP_BAR = "${feature}:topBar"
    const val CONTENT = "${feature}:content"

    // States
    const val LOADING = "${feature}:loading"
    const val ERROR = "${feature}:error"
    const val EMPTY = "${feature}:empty"
    const val LIST = "${feature}:list"

    // Actions
    const val RETRY_BUTTON = "${feature}:retryButton"
    const val ADD_BUTTON = "${feature}:addButton"
    const val REFRESH = "${feature}:refresh"

    // Items (dynamic)
    private const val ITEM_PREFIX = "${feature}:item:"
    fun itemTag(id: Long) = "$ITEM_PREFIX$id"
    fun itemTag(id: String) = "$ITEM_PREFIX$id"

    // Item components
    const val ITEM_TITLE = "${feature}:item:title"
    const val ITEM_SUBTITLE = "${feature}:item:subtitle"
    const val ITEM_ICON = "${feature}:item:icon"
}
```

---

## PHASE 3.5: Design Token Integration

### When to Apply

Design tokens are applied when `claude-product-cycle/design-spec-layer/DESIGN_TOKENS_INDEX.md` exists and shows the feature has tokens. If this file does not yet exist, skip Phase 3.5.

### Token Integration Steps

```
IF hasTokens THEN
  1. Read design-tokens.json from features/[feature]/mockups/
  2. Parse token format (google-stitch vs md3)
  3. Extract relevant tokens:
     - colors → Custom colors or gradients
     - typography → Font sizes/weights (usually use MD3 defaults)
     - spacing → Map to DesignToken.spacing
     - radius → Map to DesignToken.shapes
     - components → Apply specs to generated components
     - animations → Add animation modifiers
  4. Generate ${Feature}Theme.kt if custom colors/gradients needed
  5. Apply tokens in Screen generation
END
```

### File: ${Feature}Theme.kt (Generated if Custom Colors)

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
package org.mifospay.feature.${package}.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens from features/${feature}/mockups/design-tokens.json
 * Generated: [DATE]
 * Format: [google-stitch | md3]
 */
object ${Feature}Theme {
    val primaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF667EEA),
            Color(0xFF764BA2)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    object Colors {
        val success = Color(0xFF00D09C)
        val error = Color(0xFFFF4757)
        val warning = Color(0xFFFFB800)
        val info = Color(0xFF667EEA)
    }

    object Components {
        val buttonHeight = 56.dp
        val buttonRadius = 16.dp
        val inputHeight = 56.dp
        val inputRadius = 12.dp
        val cardRadius = 16.dp
    }
}
```

---

### Screen Template

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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.designsystem.component.*
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.ui.EmptyContentScreen
import org.mifospay.core.ui.ErrorScreenContent

@Composable
fun ${Feature}Screen(
    viewModel: ${Feature}ViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ${Feature}Event.NavigateBack -> onNavigateBack()
                is ${Feature}Event.NavigateToDetail -> onNavigateToDetail(event.id)
                is ${Feature}Event.NavigateToAdd -> onNavigateToAdd()
                is ${Feature}Event.ShowSnackbar -> {
                    // Handle snackbar
                }
            }
        }
    }

    ${Feature}ScreenContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier.testTag(${Feature}TestTags.SCREEN),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ${Feature}ScreenContent(
    state: ${Feature}State,
    onAction: (${Feature}Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )
    Scaffold(
        topBar = {
            MifosTopAppBar(
                title = "${Feature}",
                scrollBehavior = scrollBehavior,
                navigationIcon = MifosIcons.Back,
                navigationIconContentDescription = "Back",
                onNavigationIconClick = { onAction(${Feature}Action.OnBackClick) },
                modifier = Modifier.testTag(${Feature}TestTags.TOP_BAR),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(${Feature}Action.OnAddClick) },
                modifier = Modifier.testTag(${Feature}TestTags.ADD_BUTTON),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(${Feature}TestTags.CONTENT),
        ) {
            when (state.uiState) {
                is ${Feature}UiState.Loading -> {
                    ${Feature}Loading(
                        modifier = Modifier.testTag(${Feature}TestTags.LOADING),
                    )
                }
                is ${Feature}UiState.Success -> {
                    ${Feature}Success(
                        data = state.data,
                        onItemClick = { onAction(${Feature}Action.OnItemClick(it)) },
                        modifier = Modifier.testTag(${Feature}TestTags.LIST),
                    )
                }
                is ${Feature}UiState.Empty -> {
                    ${Feature}Empty(
                        onAddClick = { onAction(${Feature}Action.OnAddClick) },
                        modifier = Modifier.testTag(${Feature}TestTags.EMPTY),
                    )
                }
                is ${Feature}UiState.Error -> {
                    ${Feature}Error(
                        message = state.uiState.message,
                        onRetry = { onAction(${Feature}Action.Retry) },
                        modifier = Modifier.testTag(${Feature}TestTags.ERROR),
                    )
                }
            }
        }
    }
}

@Composable
private fun ${Feature}Loading(modifier: Modifier = Modifier) {
    MifosLoadingWheel(contentDesc = "Loading", modifier = modifier)
}

@Composable
private fun ${Feature}Success(
    data: List<${Item}>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = data,
            key = { it.id }
        ) { item ->
            ${Feature}Item(
                item = item,
                onClick = { onItemClick(item.id) },
                modifier = Modifier.testTag(${Feature}TestTags.itemTag(item.id)),
            )
        }
    }
}

@Composable
private fun ${Feature}Empty(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyContentScreen(
        title = "No ${Feature} Found",
        subTitle = "Add your first ${feature}",
        modifier = modifier,
    )
}

@Composable
private fun ${Feature}Error(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorScreenContent(
        subTitle = message,
        onClickRetry = onRetry,
        modifier = modifier,
    )
}
```

---

## PHASE 4: Navigation + DI

### Navigation Template

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
package org.mifospay.feature.${package}.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.mifospay.feature.${package}.${Feature}Screen

@Serializable
data object ${Feature}Route

@Serializable
data class ${Feature}DetailRoute(val id: Long)

fun NavGraphBuilder.${feature}Screen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
) {
    composable<${Feature}Route> {
        ${Feature}Screen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToAdd = onNavigateToAdd,
        )
    }
}

fun NavController.navigateTo${Feature}() {
    navigate(${Feature}Route)
}

fun NavController.navigateTo${Feature}Detail(id: Long) {
    navigate(${Feature}DetailRoute(id))
}
```

### Register Navigation

Register the new nav destination in:
```
cmp-shared/src/commonMain/kotlin/org/mifospay/shared/navigation/MifosNavHost.kt
```

Add inside the appropriate nav graph (LOGIN_GRAPH for auth flows, MAIN_GRAPH for app content):

```kotlin
${feature}Screen(
    onNavigateBack = navController::popBackStack,
    onNavigateToDetail = { navController.navigateTo${Feature}Detail(it) },
    onNavigateToAdd = { navController.navigateTo${Feature}() },
)
```

### DI Module Template

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
package org.mifospay.feature.${package}.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifospay.feature.${package}.${Feature}ViewModel

val ${feature}Module = module {
    viewModelOf(::${Feature}ViewModel)
}
```

### Register DI Module

Add module to:
```
cmp-shared/src/commonMain/kotlin/org/mifospay/shared/di/KoinModules.kt
```

```kotlin
val allModules = listOf(
    // ... existing modules
    ${feature}Module,
)
```

---

## PHASE 5: Build & Update Indexes

### Build Commands

```bash
# Build feature module
./gradlew :feature:${name}:build

# Format and lint
./gradlew spotlessApply detekt --no-configuration-cache
```

### Update MODULES_INDEX.md

```markdown
| ${n} | ${module} | feature/${module} | ✅ | ${vmCount} | ${screenCount} |
```

### Update SCREENS_INDEX.md

```markdown
### ${module} (${screenCount} screens)

| Screen | ViewModel | File |
|--------|-----------|------|
| ${Feature}Screen | ${Feature}ViewModel | ${Feature}Screen.kt |
```

---

## Output Template

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ✅ FEATURE LAYER COMPLETE                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  📚 O(1) Context Used:                                                        │
│  ├─ MODULES_INDEX.md → Verified module: [exists/new]                         │
│  ├─ SCREENS_INDEX.md → Existing screens: [count]                             │
│  ├─ FEATURE_MAP.md → Repository: ${Feature}Repository                        │
│  └─ SPEC.md → Mapped [n] screens, [n] states, [n] actions                    │
│                                                                               │
│  📊 Pattern Matching:                                                         │
│  ├─ ViewModel pattern from: HomeViewModel.kt                                 │
│  └─ Screen pattern from: HomeScreen.kt                                       │
│                                                                               │
│  🔧 ViewModel:                                                                │
│  └─ ${Feature}ViewModel.kt       [CREATED|UPDATED]                           │
│     ├─ State: ${Feature}State                                                │
│     ├─ Events: [count] defined                                               │
│     └─ Actions: [count] defined                                              │
│                                                                               │
│  🎨 Screen:                                                                   │
│  ├─ ${Feature}Screen.kt          [CREATED|UPDATED]                           │
│  ├─ ${Feature}TestTags.kt        [CREATED]                                   │
│  └─ components/                  [count] files                               │
│                                                                               │
│  🏷️ TestTags Generated:                                                       │
│  ├─ ${feature}:screen                                                         │
│  ├─ ${feature}:loading                                                        │
│  ├─ ${feature}:error                                                          │
│  ├─ ${feature}:empty                                                          │
│  ├─ ${feature}:list                                                           │
│  ├─ ${feature}:item:{id}                                                      │
│  └─ ... [n] total tags                                                        │
│                                                                               │
│  🧭 Navigation:                                                               │
│  ├─ navigation/${Feature}Navigation.kt  [CREATED|UPDATED]                    │
│  ├─ Routes: ${Feature}Route, ${Feature}DetailRoute                           │
│  └─ Registered in: MifosNavHost.kt                                           │
│                                                                               │
│  📦 DI:                                                                       │
│  ├─ di/${Feature}Module.kt       [CREATED|UPDATED]                           │
│  └─ Registered in: KoinModules.kt                                            │
│                                                                               │
│  📋 Indexes Updated:                                                          │
│  ├─ MODULES_INDEX.md             [UPDATED]                                   │
│  └─ SCREENS_INDEX.md             [UPDATED]                                   │
│                                                                               │
│  🔨 BUILD: :feature:${name} ✅                                                │
│  🧹 LINT: spotlessApply ✅ detekt ✅                                          │
│                                                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│  NEXT STEPS:                                                                  │
│  • Verify: /verify ${Feature}                                                │
│  • Test: /verify-tests ${Feature}                                            │
│  • Full E2E: /implement ${Feature} (if client layer also needed)             │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Feature Status (No Argument)

When `/feature` called without arguments, read MODULES_INDEX.md and SCREENS_INDEX.md:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  📋 FEATURE LAYER STATUS (from MODULES_INDEX.md)                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Summary: 29 modules | screens | ViewModels | DI modules                     │
│                                                                               │
│  | Module          | VMs | Screens | DI | Status     | Command           │   │
│  |-----------------|:---:|:-------:|:--:|------------|-------------------|   │
│  | auth            | -   | -       | ✅ | ✅ Complete | /feature auth     │   │
│  | home            | 1   | 1       | ✅ | ✅ Complete | /feature home     │   │
│  | accounts        | -   | -       | ✅ | ✅ Complete | /feature accounts │   │
│  | beneficiary     | -   | -       | ✅ | ✅ Complete | /feature beneficiary ││
│  | ...                                                                       │
│                                                                               │
│  Commands:                                                                    │
│  • /feature [name] → Implement feature layer                                 │
│  • /gap-analysis feature → Check for gaps                                    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Error Handling

### Missing Repository

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ⚠️ MISSING PREREQUISITE: Repository                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Feature: ${feature}                                                         │
│  Expected: ${Feature}Repository in FEATURE_MAP.md                            │
│  Found: Not registered                                                       │
│                                                                               │
│  The ViewModel requires a repository for data operations.                    │
│                                                                               │
│  Options:                                                                     │
│  • c / client   → Run /client ${feature} first (recommended)                 │
│  • s / skip     → Continue without repository (limited functionality)        │
│  • a / abort    → Cancel implementation                                      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Build Failure

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ❌ BUILD FAILED: :feature:${name}                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Error: Unresolved reference: ${Feature}Repository                           │
│                                                                               │
│  📍 Root Cause:                                                               │
│  Repository not registered in RepositoryModule                               │
│                                                                               │
│  📍 Auto-Fix:                                                                 │
│  Add to core/data/src/commonMain/kotlin/.../di/RepositoryModule.kt:          │
│  single<${Feature}Repository> { ${Feature}RepositoryImpl(get(), get()) }     │
│                                                                               │
│  Options:                                                                     │
│  • f / fix    → Apply fix and rebuild                                        │
│  • c / client → Run /client ${feature} to create full client layer           │
│  • a / abort  → Stop implementation                                          │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## TestTag Reference

### Naming Convention

```
Pattern: feature:component[:identifier]

Examples:
- beneficiary:screen              # Main screen
- beneficiary:loading             # Loading state
- beneficiary:error               # Error state
- beneficiary:list                # List content
- beneficiary:item:123            # Specific item by ID
- beneficiary:addButton           # Action button
- beneficiary:retryButton         # Retry action
```

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/client [Feature]` | Client layer (Repository) |
| `/implement [Feature]` | Full E2E (Client + Feature) |
| `/verify [Feature]` | Verify implementation vs spec |
| `/verify-tests [Feature]` | Run tests |
| `/gap-analysis feature` | Check feature layer gaps |
