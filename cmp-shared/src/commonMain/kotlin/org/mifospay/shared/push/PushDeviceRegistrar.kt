/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.push

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Client for the notification engine's device registry
 * (`notification-service`, POST/DELETE /v1/devices). The engine — not
 * Fineract — owns the FCM token store.
 */
class PushDeviceRegistrar(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun register(clientId: Long, token: String) {
        client.post("$baseUrl/v1/devices") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("clientId", clientId.toString())
                    put("token", token)
                    put("platform", PushBridge.platformName)
                },
            )
        }
    }

    suspend fun unregister(token: String) {
        client.delete("$baseUrl/v1/devices/${token.encodeURLPathPart()}")
    }
}
