/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.data.repositoryImpl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mobile_wallet.core.data.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.AssetRepository
import org.mifospay.core.model.utils.Country

class AssetRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : AssetRepository {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getCountriesWithStates(): DataState<Map<String, List<String>>> {
        // The countries asset is ~434 KB; do the read + decode + parse off the main thread so
        // it can't stall the signup enter animation (it was previously parsed on Main).
        return withContext(ioDispatcher) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val bytes = Res.readBytes("files/countries.json")
                val jsonString = bytes.decodeToString()
                val countries = json.decodeFromString<List<Country>>(jsonString)
                DataState.Success(
                    countries.associate { country ->
                        country.name to country.states.map { it.name }
                    },
                )
            } catch (e: Exception) {
                DataState.Error(e)
            }
        }
    }
}
