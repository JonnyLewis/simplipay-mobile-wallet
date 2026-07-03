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

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.SearchRepository
import org.mifospay.core.data.util.Constants
import org.mifospay.feature.payments.pay.WalletCreditRecipient
import org.mifospay.feature.payments.pay.WalletTransferNotifier

/**
 * Sends the wallet-to-wallet "Payment received" notice through the notification
 * engine (`notification-service`, POST /v1/notifications, channel IN_APP — the
 * recipient sees it in their in-app notification feed).
 *
 * A phone recipient is resolved to the owning Fineract client via the same
 * exact-match client search the signup flow uses; the settled paymentId doubles
 * as the engine's dedupe key so a re-poll can never double-notify.
 */
class NotificationEngineWalletTransferNotifier(
    private val client: HttpClient,
    private val baseUrl: String,
    private val searchRepository: SearchRepository,
) : WalletTransferNotifier {

    override suspend fun notifyWalletCredit(
        recipient: WalletCreditRecipient,
        amountDisplay: String,
        senderName: String?,
        paymentId: String,
    ) {
        val clientId = when (recipient) {
            is WalletCreditRecipient.Client -> recipient.clientId
            is WalletCreditRecipient.Phone -> resolveClientId(recipient.msisdn) ?: run {
                Logger.w(tag = TAG, messageString = "no wallet client found for recipient phone; skipping credit notice")
                return
            }
        }
        val message = if (senderName.isNullOrBlank()) {
            "You received $amountDisplay"
        } else {
            "You received $amountDisplay from $senderName"
        }
        client.post("$baseUrl/v1/notifications") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("channel", "IN_APP")
                    put("type", "TRANSACTION")
                    put("target", clientId.toString())
                    put("title", "Payment received")
                    put("message", message)
                    put("priority", "HIGH")
                    put("dedupeKey", "wallet-credit-$paymentId")
                },
            )
        }
        Logger.d(tag = TAG, messageString = "wallet credit notice queued for client $clientId")
    }

    private suspend fun resolveClientId(msisdn: String): Long? {
        val result = searchRepository.searchResources(
            query = msisdn,
            resources = Constants.CLIENTS,
            exactMatch = true,
        )
        return (result as? DataState.Success)?.data?.firstOrNull()?.entityId?.toLong()
    }

    private companion object {
        const val TAG = "WalletTransferNotifier"
    }
}
