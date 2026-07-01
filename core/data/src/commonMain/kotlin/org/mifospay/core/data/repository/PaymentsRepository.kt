/*
 * Copyright 2026 Mifos Initiative
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
import org.mifospay.core.network.model.payments.BankAccount
import org.mifospay.core.network.model.payments.PaymentStatusResponse
import org.mifospay.core.network.model.payments.PaymentTarget
import org.mifospay.core.network.model.payments.TransferResponse
import org.mifospay.core.network.model.payments.ZarAmount

/**
 * SimpliPay Payments API. Every `initiate*` call returns a [TransferResponse] whose
 * `transactionId` is **accepted, not settled** — poll [getPaymentStatus] until a terminal
 * state.
 *
 * `clientRefId` is the idempotency key: generate it once when the user confirms a payment and
 * **reuse the same value on every retry** of that payment so a resend never double-charges.
 */
interface PaymentsRepository {

    /**
     * On-us / phone / account / QR / proximity payment. The server decides the rail (on-us
     * vs PayShap for a phone) from the destination.
     */
    fun payOnUs(
        payerAccountId: String,
        target: PaymentTarget,
        amount: ZarAmount,
        clientRefId: String,
    ): Flow<DataState<TransferResponse>>

    /** Payout to a bank account (EFT). Use for amounts at/above the PayShap cap. */
    fun payoutToBank(
        payerAccountId: String,
        bankAccount: BankAccount,
        amount: ZarAmount,
        clientRefId: String,
    ): Flow<DataState<TransferResponse>>

    /**
     * Instant payout to a phone (PayShap proxy). Emits [DataState.Error] with a
     * [org.mifospay.core.network.model.payments.PayShapCapExceededException] if the amount is
     * at/above R50,000 — use [payoutToBank] instead.
     */
    fun payoutInstant(
        payerAccountId: String,
        phoneMsisdn: String,
        amount: ZarAmount,
        clientRefId: String,
    ): Flow<DataState<TransferResponse>>

    /** Fetch the current state of a payment. Poll this until [PaymentStatusResponse.state]
     *  is terminal (SUCCESS / FAILED / EXPIRED). */
    fun getPaymentStatus(paymentId: String): Flow<DataState<PaymentStatusResponse>>
}
