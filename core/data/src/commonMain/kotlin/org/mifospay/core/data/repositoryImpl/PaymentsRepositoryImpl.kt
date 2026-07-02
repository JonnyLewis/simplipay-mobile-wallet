/*
 * Copyright 2026 Mifos Initiative
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.mifospay.core.common.DataState
import org.mifospay.core.common.asDataStateFlow
import org.mifospay.core.data.repository.PaymentsRepository
import org.mifospay.core.data.util.parseMifosError
import org.mifospay.core.network.PaymentsApiManager
import org.mifospay.core.network.config.PaymentsApiConfig
import org.mifospay.core.network.model.payments.BankAccount
import org.mifospay.core.network.model.payments.PaymentRequestFactory
import org.mifospay.core.network.model.payments.PaymentStatusResponse
import org.mifospay.core.network.model.payments.PaymentTarget
import org.mifospay.core.network.model.payments.TransferResponse
import org.mifospay.core.network.model.payments.ZarAmount

class PaymentsRepositoryImpl(
    private val apiManager: PaymentsApiManager,
    private val ioDispatcher: CoroutineDispatcher,
) : PaymentsRepository {

    override fun payOnUs(
        payerAccountId: String,
        target: PaymentTarget,
        amount: ZarAmount,
        clientRefId: String,
    ): Flow<DataState<TransferResponse>> = initiate {
        PaymentRequestFactory.onUs(payerAccountId, target, amount, clientRefId)
    }

    override fun payoutToBank(
        payerAccountId: String,
        bankAccount: BankAccount,
        amount: ZarAmount,
        clientRefId: String,
        rail: String,
    ): Flow<DataState<TransferResponse>> = initiate {
        // Throws PayShapCapExceededException for instant amounts at/above the cap; surfaced
        // as DataState.Error by asDataStateFlow's catch.
        PaymentRequestFactory.payoutToBank(payerAccountId, bankAccount, amount, clientRefId, rail)
    }

    override fun payoutInstant(
        payerAccountId: String,
        phoneMsisdn: String,
        amount: ZarAmount,
        clientRefId: String,
    ): Flow<DataState<TransferResponse>> = initiate {
        // Throws PayShapCapExceededException for amounts at/above the cap; surfaced as
        // DataState.Error by asDataStateFlow's catch.
        PaymentRequestFactory.payoutInstant(payerAccountId, phoneMsisdn, amount, clientRefId)
    }

    override fun getPaymentStatus(
        paymentId: String,
    ): Flow<DataState<PaymentStatusResponse>> =
        flow {
            emit(
                apiManager.paymentsApi.getPaymentStatus(
                    paymentId = paymentId,
                    token = PaymentsApiConfig.SHARED_TOKEN,
                ),
            )
        }
            .asDataStateFlow(parseMifosError)
            .flowOn(ioDispatcher)

    /**
     * Builds the request lazily (inside the flow) so a request-construction failure — e.g. the
     * PayShap cap check — is caught and emitted as [DataState.Error] rather than thrown
     * synchronously to the caller.
     */
    private fun initiate(
        buildRequest: () -> org.mifospay.core.network.model.payments.TransferRequest,
    ): Flow<DataState<TransferResponse>> =
        flow {
            val request = buildRequest()
            emit(
                apiManager.paymentsApi.initiateTransfer(
                    correlationId = request.clientRefId,
                    request = request,
                ),
            )
        }
            .asDataStateFlow(parseMifosError)
            .flowOn(ioDispatcher)
}
