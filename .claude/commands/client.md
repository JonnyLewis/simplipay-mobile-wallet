# /client - Client Layer Implementation

## Purpose

Implement the client layer (Network + Data) using O(1) lookup and pattern detection. Creates Services, Repositories, and DI registration with code matching existing codebase conventions.

---

## Command Variants

```
/client                        # Show client layer status
/client [Feature]              # Implement client layer for feature
/client [Feature] --network    # Network layer only (Service)
/client [Feature] --data       # Data layer only (Repository)
```

---

## Workflow with O(1) Optimization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  /client [Feature] - O(1) OPTIMIZED WORKFLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PHASE 0: O(1) CONTEXT LOADING                                              │
│  ├─→ Read FEATURE_MAP.md              → Check if service/repo exist         │
│  ├─→ Read API_INDEX.md                → Get endpoint definitions            │
│  ├─→ Read design-spec-layer/features/[name]/API.md  → feature endpoints     │
│  └─→ Read design-spec-layer/features/[name]/SPEC.md → data requirements     │
│                                                                              │
│  PHASE 1: PATTERN DETECTION                                                 │
│  ├─→ Read existing Service            → Extract interface pattern           │
│  ├─→ Read existing Repository         → Extract implementation pattern      │
│  └─→ Read API Managers                → Extract DI pattern                  │
│                                                                              │
│  PHASE 2: NETWORK LAYER (if needed)                                         │
│  ├─→ Check FEATURE_MAP for existing   → Skip if exists                      │
│  ├─→ Create Service interface         → Pattern-matched code                │
│  └─→ Register in KtorfitClient        → Wire into API manager               │
│                                                                              │
│  PHASE 3: DATA LAYER (if needed)                                            │
│  ├─→ Check FEATURE_MAP for existing   → Skip if exists                      │
│  ├─→ Create Repository interface      → Pattern-matched code                │
│  ├─→ Create RepositoryImpl            → Pattern-matched code                │
│  └─→ Register in RepositoryModule     → DI registration                     │
│                                                                              │
│  PHASE 4: BUILD & VERIFY                                                    │
│  ├─→ ./gradlew :core:network:build                                          │
│  ├─→ ./gradlew :core:data:build                                             │
│  ├─→ ./gradlew spotlessApply                                                │
│  └─→ Update FEATURE_MAP.md            → Maintain O(1) index                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PHASE 0: O(1) Context Loading

### Files to Read

| File | Purpose | Data Extracted |
|------|---------|----------------|
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Service/Repo inventory | existingServices[], existingRepos[] |
| `claude-product-cycle/server-layer/API_INDEX.md` | All API endpoints | endpoints[], dtos[] |
| `claude-product-cycle/design-spec-layer/features/[name]/API.md` | Feature endpoints | featureEndpoints[] |
| `claude-product-cycle/design-spec-layer/features/[name]/SPEC.md` | Data requirements | models[], fields[] |

### Decision Matrix (from FEATURE_MAP.md lookup)

```markdown
| Component | Exists | Action |
|-----------|:------:|--------|
| ${Feature}Service | ✅/❌ | SKIP/CREATE |
| ${Feature}Repository | ✅/❌ | SKIP/CREATE |
```

---

## PHASE 1: Pattern Detection

### Reference Files

```
1. Service Reference:
   core/network/src/commonMain/kotlin/org/mifospay/core/network/services/BeneficiaryService.kt

2. Repository Reference:
   core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/BeneficiaryRepository.kt
   core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/BeneficiaryRepositoryImpl.kt

3. DI Reference:
   core/data/src/commonMain/kotlin/org/mifospay/core/data/di/RepositoryModule.kt
```

### API Manager Selection

The wallet project uses two API managers — choose based on endpoint type:

| Manager | Class | Used For |
|---------|-------|----------|
| `SelfServiceApiManager` | `org.mifospay.core.network.SelfServiceApiManager` | Fineract self-service APIs: beneficiaries, savings accounts, transfers, user, clients |
| `FineractApiManager` | `org.mifospay.core.network.FineractApiManager` | Direct Fineract APIs: KYC, invoices, autopay, bills, billers, saved cards, standing instructions, notifications, registration, search, documents, run reports, two-factor auth |

### Extracted Patterns

```kotlin
// Service Pattern (Ktorfit interface)
interface ${Feature}Service {
    @GET(ApiEndPoints.${ENDPOINT_CONSTANT})
    fun get${Feature}List(): Flow<List<${Model}>>

    @GET(ApiEndPoints.${ENDPOINT_CONSTANT} + "/{id}")
    suspend fun get${Feature}ById(@Path("id") id: Long): Flow<${Model}>

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

// Repository Implementation Pattern (matches BeneficiaryRepositoryImpl)
class ${Feature}RepositoryImpl(
    private val apiManager: SelfServiceApiManager, // or FineractApiManager
    private val ioDispatcher: CoroutineDispatcher,
) : ${Feature}Repository {

    override suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>> {
        return apiManager.${feature}Api.get${Feature}List()
            .asDataStateFlow()
            .flowOn(ioDispatcher)
    }

    override suspend fun create${Feature}(payload: ${Payload}): DataState<String> {
        return try {
            withContext(ioDispatcher) {
                apiManager.${feature}Api.create${Feature}(payload)
            }
            DataState.Success("${Feature} created successfully")
        } catch (e: Exception) {
            DataState.Error(e)
        }
    }
}
```

---

## PHASE 2: Network Layer

### File Locations

| Component | Location |
|-----------|----------|
| Service Interface | `core/network/src/commonMain/kotlin/org/mifospay/core/network/services/` |
| API Endpoints | `core/network/src/commonMain/kotlin/org/mifospay/core/network/utils/ApiEndPoints.kt` |
| Ktorfit Client | `core/network/src/commonMain/kotlin/org/mifospay/core/network/KtorfitClient.kt` |

### Service Template (Pattern-Matched)

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
package org.mifospay.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import kotlinx.coroutines.flow.Flow
import org.mifospay.core.network.utils.ApiEndPoints

interface ${Feature}Service {

    @GET(ApiEndPoints.${ENDPOINT_CONSTANT})
    fun get${Feature}List(): Flow<List<${Model}>>

    @GET(ApiEndPoints.${ENDPOINT_CONSTANT} + "/{id}")
    suspend fun get${Feature}ById(@Path("id") id: Long): Flow<${Model}>

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

### Add Endpoint Constant (if needed)

```kotlin
// ApiEndPoints.kt
object ApiEndPoints {
    // ... existing constants
    const val ${ENDPOINT_CONSTANT} = "${endpoint_path}"
}
```

### Register in KtorfitClient and API Manager

**Step 1 — `KtorfitClient.kt`** — add internal lazy property using Ktorfit's KSP-generated extension:
```kotlin
internal val ${feature}Api by lazy { ktorfit.create${Feature}Service() }
```

**Step 2 — Expose via the appropriate manager class:**

For self-service APIs — `SelfServiceApiManager.kt`:
```kotlin
val ${feature}Api by lazy { ktorfitClient.${feature}Api }
```

For direct Fineract APIs — `FineractApiManager.kt`:
```kotlin
val ${feature}Api by lazy { ktorfitClient.${feature}Api }
```

> Note: `KtorfitClient` takes a single `ktorfit: Ktorfit` constructor parameter. The manager classes (`SelfServiceApiManager`, `FineractApiManager`) each take a `KtorfitClient` and selectively expose the APIs they own.

---

## PHASE 3: Data Layer

### File Locations

| Component | Location |
|-----------|----------|
| Repository Interface | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/` |
| Repository Impl | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/` |
| Data DI | `core/data/src/commonMain/kotlin/org/mifospay/core/data/di/RepositoryModule.kt` |

### Repository Interface Template

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
package org.mifospay.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.mifospay.core.common.DataState
import org.mifospay.core.model.{domain}.${Model}  // e.g. org.mifospay.core.model.beneficiary.Beneficiary

interface ${Feature}Repository {
    suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>>
    suspend fun get${Feature}ById(id: Long): Flow<DataState<${Model}>>
    suspend fun create${Feature}(payload: ${Payload}): DataState<String>
    suspend fun update${Feature}(id: Long, payload: ${Payload}): DataState<String>
    suspend fun delete${Feature}(id: Long): DataState<String>
}
```

### Repository Implementation Template

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
package org.mifospay.core.data.repositoryImpl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.mifospay.core.common.DataState
import org.mifospay.core.common.asDataStateFlow
import org.mifospay.core.data.repository.${Feature}Repository
import org.mifospay.core.model.{domain}.${Model}  // e.g. org.mifospay.core.model.beneficiary.Beneficiary
import org.mifospay.core.network.SelfServiceApiManager  // or FineractApiManager

class ${Feature}RepositoryImpl(
    private val apiManager: SelfServiceApiManager,  // or FineractApiManager
    private val ioDispatcher: CoroutineDispatcher,
) : ${Feature}Repository {

    override suspend fun get${Feature}List(): Flow<DataState<List<${Model}>>> {
        return apiManager.${feature}Api.get${Feature}List()
            .asDataStateFlow()
            .flowOn(ioDispatcher)
    }

    override suspend fun get${Feature}ById(id: Long): Flow<DataState<${Model}>> {
        return apiManager.${feature}Api.get${Feature}ById(id)
            .asDataStateFlow()
            .flowOn(ioDispatcher)
    }

    override suspend fun create${Feature}(payload: ${Payload}): DataState<String> {
        return try {
            withContext(ioDispatcher) {
                apiManager.${feature}Api.create${Feature}(payload)
            }
            DataState.Success("${Feature} created successfully")
        } catch (e: Exception) {
            DataState.Error(e)
        }
    }

    override suspend fun update${Feature}(id: Long, payload: ${Payload}): DataState<String> {
        return try {
            withContext(ioDispatcher) {
                apiManager.${feature}Api.update${Feature}(id, payload)
            }
            DataState.Success("${Feature} updated successfully")
        } catch (e: Exception) {
            DataState.Error(e)
        }
    }

    override suspend fun delete${Feature}(id: Long): DataState<String> {
        return try {
            withContext(ioDispatcher) {
                apiManager.${feature}Api.delete${Feature}(id)
            }
            DataState.Success("${Feature} deleted successfully")
        } catch (e: Exception) {
            DataState.Error(e)
        }
    }
}
```

### Register in RepositoryModule

```kotlin
// RepositoryModule.kt — add to the existing RepositoryModule val
// Note: ioDispatcher is defined at file scope as:
//   private val ioDispatcher = named(MifosDispatchers.IO.name)
single<${Feature}Repository> { ${Feature}RepositoryImpl(get(), get(ioDispatcher)) }
```

The first `get()` resolves `SelfServiceApiManager` or `FineractApiManager` by type, and the second `get(ioDispatcher)` resolves the coroutine dispatcher via the `MifosDispatchers.IO` qualifier. Use positional `get()` calls — do not use named arguments.

---

## PHASE 4: Build & Verify

### Build Commands

```bash
# Build network module
./gradlew :core:network:build

# Build data module
./gradlew :core:data:build

# Format code
./gradlew spotlessApply --no-configuration-cache

# Run detekt
./gradlew detekt
```

### Update FEATURE_MAP.md

Add new entry to maintain O(1) lookup:

```markdown
| ${feature} | ${Feature}Service | ${Feature}Repository | ${Notes} |
```

---

## Output Template

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ✅ CLIENT LAYER COMPLETE                                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  📚 O(1) Context Used:                                                        │
│  ├─ FEATURE_MAP.md → Checked existing: [existing services/repos]             │
│  ├─ API_INDEX.md → Mapped [n] endpoints                                      │
│  └─ API.md → Feature endpoints: [list]                                       │
│                                                                               │
│  📊 Pattern Matching:                                                         │
│  ├─ Service pattern from: BeneficiaryService.kt                              │
│  └─ Repository pattern from: BeneficiaryRepositoryImpl.kt                    │
│                                                                               │
│  🔧 Network Layer:                                                            │
│  ├─ ${Feature}Service.kt                [CREATED|SKIPPED]                    │
│  ├─ ApiEndPoints.${CONSTANT}            [ADDED|EXISTS]                       │
│  └─ KtorfitClient registration          [ADDED|EXISTS]                       │
│                                                                               │
│  🔧 Data Layer:                                                               │
│  ├─ ${Feature}Repository.kt             [CREATED|SKIPPED]                    │
│  ├─ ${Feature}RepositoryImpl.kt         [CREATED|SKIPPED]                    │
│  └─ RepositoryModule registration       [ADDED|EXISTS]                       │
│                                                                               │
│  📋 Index Updated:                                                            │
│  └─ FEATURE_MAP.md                      [UPDATED]                            │
│                                                                               │
│  🔨 BUILD:                                                                    │
│  ├─ :core:network ✅                                                          │
│  └─ :core:data ✅                                                             │
│                                                                               │
│  🧹 LINT: spotlessApply ✅                                                    │
│                                                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│  NEXT STEP:                                                                   │
│  Run:  /feature ${Feature}                                                   │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Client Status (No Argument)

When `/client` called without arguments, read FEATURE_MAP.md:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  📋 CLIENT LAYER STATUS (from FEATURE_MAP.md)                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Summary: 23 services | 29 repositories | 2 DI modules                       │
│                                                                               │
│  | Feature              | Service                   | Repository             │
│  |----------------------|---------------------------|----------------------  │
│  | auth                 | AuthenticationService     | AuthenticationRepository│
│  | home                 | (SelfServiceApiManager)   | SelfServiceRepository  │
│  | accounts             | SavingsAccountsService    | AccountRepository      │
│  | beneficiary          | BeneficiaryService        | BeneficiaryRepository  │
│  | autopay              | AutoPayService            | AutoPayRepository      │
│  | ...                                                                       │
│                                                                               │
│  Commands:                                                                    │
│  • /client [feature] → Implement client layer                                │
│  • /gap-analysis client → Check for gaps                                     │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Error Handling

### Missing API Endpoint

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ⚠️ MISSING API ENDPOINT                                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Feature: ${feature}                                                         │
│  Expected: API.md with endpoint definitions                                  │
│  Found: File missing or empty                                                │
│                                                                               │
│  Options:                                                                     │
│  • d / design   → Run /design ${feature} api first                           │
│  • m / manual   → Enter endpoints manually                                   │
│  • a / abort    → Cancel implementation                                      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Build Failure

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  ❌ BUILD FAILED: :core:network                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Error: Unresolved reference: ${Model}                                       │
│                                                                               │
│  📍 Auto-Fix Suggestion:                                                      │
│  Create model in core/model/src/commonMain/kotlin/org/mifospay/core/model/: │
│                                                                               │
│  ```kotlin                                                                   │
│  @Serializable                                                               │
│  data class ${Model}(                                                        │
│      val id: Long,                                                           │
│      // ... fields from API.md                                               │
│  )                                                                           │
│  ```                                                                         │
│                                                                               │
│  Options:                                                                     │
│  • f / fix    → Create model and rebuild                                     │
│  • m / manual → Show full model template                                     │
│  • a / abort  → Stop implementation                                          │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Related Files

### O(1) Index Files

| File | Purpose |
|------|---------|
| `claude-product-cycle/client-layer/FEATURE_MAP.md` | Service/Repository inventory |
| `claude-product-cycle/server-layer/API_INDEX.md` | All API endpoints |

### Reference Code

| Component | Reference File |
|-----------|----------------|
| Service | `core/network/src/commonMain/kotlin/org/mifospay/core/network/services/BeneficiaryService.kt` |
| Repository Interface | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repository/BeneficiaryRepository.kt` |
| Repository Impl | `core/data/src/commonMain/kotlin/org/mifospay/core/data/repositoryImpl/BeneficiaryRepositoryImpl.kt` |
| DI | `core/data/src/commonMain/kotlin/org/mifospay/core/data/di/RepositoryModule.kt` |

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/feature [Feature]` | Feature layer (ViewModel + Screen) |
| `/implement [Feature]` | Full E2E (Client + Feature) |
| `/gap-analysis client` | Check client layer gaps |
| `/verify [Feature]` | Verify implementation |
