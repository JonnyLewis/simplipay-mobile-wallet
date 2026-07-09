/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.send.money

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.PaymentsRepository
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.network.model.payments.PartyIdType
import org.mifospay.core.network.model.payments.PaymentState
import org.mifospay.core.network.model.payments.PaymentTarget
import org.mifospay.core.network.model.payments.ZarAmount
import org.mifospay.core.ui.utils.BaseViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drives a real SimpliPay Payments API payment: initiate (`POST /channel/transfer`) then poll
 * (`GET /payments/{id}`) until a terminal state.
 *
 * Idempotency: [clientRefId] is generated once (when this view-model is created for a confirmed
 * payment) and **reused on every retry** so a resend never double-charges.
 *
 * Payee threading: this screen receives the payee identifier + type as nav args. Pay-by-phone
 * passes [PartyIdType.MSISDN]; account / QR / proximity passes [PartyIdType.ACCOUNT_ID]. When the
 * identifier is absent (a not-yet-wired flow or a @Preview) the screen reports a failure rather
 * than faking a success — see docs/MOBILE_PAYMENTS_WIRING.md for the remaining hops.
 */
class PaymentProcessingViewModel(
    savedStateHandle: SavedStateHandle,
    private val paymentsRepository: PaymentsRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : BaseViewModel<PaymentProcessingState, PaymentProcessingEvent, PaymentProcessingAction>(
    initialState = PaymentProcessingState(),
) {
    // Stable per confirmed payment; survives retries because the VM instance is reused.
    @OptIn(ExperimentalUuidApi::class)
    private val clientRefId: String = Uuid.random().toString()

    init {
        val payeeName = savedStateHandle.get<String>("payeeName") ?: ""
        // Amount arrives in minor units (paise) — see navigateToPaymentProcessingScreen.
        val amountMinor = savedStateHandle.get<String>("amount") ?: ""
        val isUpiCode = savedStateHandle.get<Boolean>("isUpiCode") ?: false
        val payeeIdentifier = savedStateHandle.get<String>("payeeIdentifier") ?: ""
        val payeeType = savedStateHandle.get<String>("payeeType") ?: PartyIdType.ACCOUNT_ID

        mutableStateFlow.update {
            it.copy(
                payeeName = payeeName,
                amount = amountMinor,
                isUpiCode = isUpiCode,
                payeeIdentifier = payeeIdentifier,
                payeeType = payeeType,
            )
        }

        startPaymentProcessing()
    }

    private fun startPaymentProcessing() {
        viewModelScope.launch {
            mutableStateFlow.update { it.copy(isProcessing = true) }

            // defaultAccountId is DataStore-backed and resolves asynchronously — await the
            // first real (non-null) value instead of reading .value synchronously, which
            // returns the flow's null initial value even when a default account exists.
            val payerAccountId = withTimeoutOrNull(PAYER_LOOKUP_TIMEOUT_MS) {
                preferencesRepository.defaultAccountId.first { it != null }
            }?.toString()
            if (payerAccountId.isNullOrEmpty()) {
                sendEvent(PaymentProcessingEvent.PaymentFailed("No wallet account is selected."))
                return@launch
            }

            val target = state.toPaymentTarget()
            if (target == null) {
                sendEvent(
                    PaymentProcessingEvent.PaymentFailed(
                        "Payment destination is missing. This flow is not fully wired yet.",
                    ),
                )
                return@launch
            }

            val amount = try {
                ZarAmount.fromMinorUnits(state.amount.toLong())
            } catch (e: Exception) {
                sendEvent(PaymentProcessingEvent.PaymentFailed("Invalid amount."))
                return@launch
            }

            // 1) Initiate — 200 means accepted, not settled.
            val initiate = paymentsRepository.payOnUs(
                payerAccountId = payerAccountId,
                target = target,
                amount = amount,
                clientRefId = clientRefId,
            ).first { it !is DataState.Loading }

            when (initiate) {
                is DataState.Error -> {
                    sendEvent(PaymentProcessingEvent.PaymentFailed(initiate.message))
                }
                is DataState.Success -> {
                    pollUntilTerminal(initiate.data.transactionId)
                }
                DataState.Loading -> Unit // unreachable: filtered above
            }
        }
    }

    /** Polls the status endpoint until SUCCESS / FAILED / EXPIRED or the attempt budget runs out. */
    private suspend fun pollUntilTerminal(paymentId: String) {
        repeat(MAX_POLLS) {
            val status = paymentsRepository.getPaymentStatus(paymentId)
                .first { it !is DataState.Loading }
            when (status) {
                is DataState.Success -> {
                    val paymentState = status.data.state
                    if (PaymentState.isTerminal(paymentState)) {
                        mutableStateFlow.update { it.copy(isProcessing = false) }
                        if (paymentState == PaymentState.SUCCESS) {
                            sendEvent(
                                PaymentProcessingEvent.PaymentComplete(
                                    payeeName = state.payeeName,
                                    amount = state.amount,
                                    upiName = state.payeeName.uppercase(),
                                    transactionTimestamp = getCurrentUnixTimestamp(),
                                ),
                            )
                        } else {
                            sendEvent(
                                PaymentProcessingEvent.PaymentFailed(
                                    "Payment $paymentState. Please try again.",
                                ),
                            )
                        }
                        return
                    }
                }
                is DataState.Error -> {
                    // A transient status-read failure shouldn't fail the payment; keep polling.
                }
                DataState.Loading -> Unit
            }
            delay(POLL_INTERVAL_MS)
        }
        // Budget exhausted without a terminal state — the payment may still settle server-side.
        sendEvent(
            PaymentProcessingEvent.PaymentFailed(
                "Still processing. Check payment history for the final status.",
            ),
        )
    }

    override fun handleAction(action: PaymentProcessingAction) {
        when (action) {
            // Retry reuses the same clientRefId (idempotency) — never re-generated.
            PaymentProcessingAction.RetryPayment -> startPaymentProcessing()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getCurrentUnixTimestamp(): String = Clock.System.now().epochSeconds.toString()

    companion object {
        private const val MAX_POLLS = 20
        private const val POLL_INTERVAL_MS = 2_000L
        private const val PAYER_LOOKUP_TIMEOUT_MS = 3_000L
    }
}

/** Builds the [PaymentTarget] for the initiate call, or null when the identifier is missing. */
private fun PaymentProcessingState.toPaymentTarget(): PaymentTarget? {
    if (payeeIdentifier.isEmpty()) return null
    return when (payeeType) {
        PartyIdType.MSISDN -> PaymentTarget.Phone(payeeIdentifier)
        else -> PaymentTarget.Account(payeeIdentifier)
    }
}

data class PaymentProcessingState(
    val payeeName: String = "",
    val amount: String = "",
    val isUpiCode: Boolean = false,
    val isProcessing: Boolean = true,
    val payeeIdentifier: String = "",
    val payeeType: String = PartyIdType.ACCOUNT_ID,
) {
    val formattedAmount: String
        get() = AmountUtils.formatPaiseForUI(amount)
}

sealed interface PaymentProcessingEvent {
    data class PaymentComplete(
        val payeeName: String,
        val amount: String,
        val upiName: String,
        val transactionTimestamp: String,
    ) : PaymentProcessingEvent
    data class PaymentFailed(val errorMessage: String) : PaymentProcessingEvent
}

sealed interface PaymentProcessingAction {
    data object RetryPayment : PaymentProcessingAction
}
