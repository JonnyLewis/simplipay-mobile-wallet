/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import org.mifospay.core.network.model.payments.BankDto
import org.mifospay.core.network.model.payments.PayinRequest
import org.mifospay.core.network.model.payments.PaymentStatusResponse
import org.mifospay.core.network.model.payments.TransferRequest
import org.mifospay.core.network.model.payments.TransferResponse

/**
 * SimpliPay Payments API. Backed by the dedicated `PaymentsClient` Ktorfit instance, whose
 * base URL is [org.mifospay.core.network.config.PaymentsApiConfig] and whose default headers
 * carry `Platform-TenantId`. The `?token=` query is a shared secret injected by the caller.
 */
interface PaymentsService {

    /**
     * Initiate a payment (on-us / payout). The server decides the rail from the destination.
     * `200` means *accepted*, not settled — poll [getPaymentStatus] for the outcome.
     */
    @POST("channel/transfer")
    suspend fun initiateTransfer(
        // The channel connector requires a correlation id header and NPEs (500) without it.
        // Reuse the idempotency key so retries correlate to the same payment.
        @Header("X-CorrelationID") correlationId: String,
        @Body request: TransferRequest,
    ): TransferResponse

    /** Bank catalog (name + universal branch code + PayShap participation). Public reference data, no token. */
    @GET("banks")
    suspend fun banks(): List<BankDto>

    /** Poll a payment until a terminal state (SUCCESS / FAILED / EXPIRED). */
    @GET("payments/{paymentId}")
    suspend fun getPaymentStatus(
        @Path("paymentId") paymentId: String,
        @Query("token") token: String,
    ): PaymentStatusResponse

    /**
     * Add money to a wallet. The app does **not** initiate this in v1 (top-ups are
     * backend-side / receive-money share flow); kept here for completeness.
     */
    @POST("payin")
    suspend fun payin(
        @Query("token") token: String,
        @Body request: PayinRequest,
    ): PaymentStatusResponse
}
