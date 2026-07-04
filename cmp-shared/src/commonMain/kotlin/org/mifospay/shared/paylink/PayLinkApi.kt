/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.paylink

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Base URL of the SimpliLink pay link service (`SimpliLink` repo).
 * Dev default reaches the service on the Mac host from the iOS simulator.
 * TODO: move into instance config alongside NOTIFICATION_ENGINE_URL before
 *  device/TestFlight builds — a physical phone cannot see localhost.
 */
const val SIMPLILINK_URL = "http://localhost:8086"

/** A pay link as served by SimpliLink (`GET/POST /v1/paylinks`). */
@Serializable
data class PayLinkDto(
    val id: String,
    val slug: String,
    val url: String,
    val payeeName: String? = null,
    val description: String,
    val amountMinor: Long? = null,
    val currency: String = "ZAR",
    val status: String,
)

@Serializable
internal data class CreatePayLinkBody(
    val clientId: Long,
    val accountId: Long,
    val payeeName: String?,
    val description: String,
    val amountMinor: Long?,
)

/**
 * Client for the SimpliLink service. clientId rides as an explicit parameter
 * until the service's JWT exchange lands (SimpliLink DESIGN.md §6).
 */
class PayLinkApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {

    suspend fun list(clientId: Long): List<PayLinkDto> =
        client.get("$baseUrl/v1/paylinks") {
            parameter("clientId", clientId)
        }.body()

    suspend fun create(
        clientId: Long,
        accountId: Long,
        payeeName: String?,
        description: String,
        amountMinor: Long?,
    ): PayLinkDto =
        client.post("$baseUrl/v1/paylinks") {
            contentType(ContentType.Application.Json)
            setBody(CreatePayLinkBody(clientId, accountId, payeeName, description, amountMinor))
        }.body()
}
