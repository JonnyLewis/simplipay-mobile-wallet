/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.payments.pay

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.PaymentsRepository
import org.mifospay.core.data.repository.UserVerificationRepository
import org.mifospay.core.datastore.UserPreferencesRepository
import org.mifospay.core.network.model.payments.BankAccount
import org.mifospay.core.network.model.payments.BankAccountType
import org.mifospay.core.network.model.payments.PayShapLimits
import org.mifospay.core.network.model.payments.PaymentReasonCode
import org.mifospay.core.network.model.payments.PaymentState
import org.mifospay.core.network.model.payments.PaymentTarget
import org.mifospay.core.network.model.payments.PayoutRail
import org.mifospay.core.network.model.payments.SaBank
import org.mifospay.core.network.model.payments.SouthAfricanBanks
import org.mifospay.core.network.model.payments.ZarAmount
import org.mifospay.core.ui.utils.BaseViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drives the "Pay" screen against the SimpliPay Payments API. Two destinations:
 *  - [PayMode.NUMBER] — pay a phone (MSISDN). The server decides on-us vs PayShap; the app
 *    mirrors the PayShap sub-R50,000 cap in the UI.
 *  - [PayMode.BANK]   — pay a bank account on the USER-chosen rail: Instant
 *    ([PayoutRail.PAYSHAP], the preferred default) or Standard ([PayoutRail.EFT]).
 *
 * Initiate (`POST /channel/transfer`) is *accepted, not settled*; the VM then polls
 * `GET /payments/{id}` until a terminal state. A FAILED payment carries a machine-readable
 * [PaymentReasonCode]; `BANK_NOT_ON_PAYSHAP` / `AMOUNT_OVER_PAYSHAP_CAP` offer a one-tap
 * resend on the Standard (EFT) rail — with a NEW clientRefId, since the original payment is
 * terminally FAILED. [clientRefId] is otherwise generated once per confirmed payment and
 * reused on retry so a resend never double-charges.
 */
class PayViewModel(
    private val paymentsRepository: PaymentsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val userVerificationRepository: UserVerificationRepository,
) : BaseViewModel<PayState, PayEvent, PayAction>(
    initialState = PayState(),
) {
    // Stable per confirmed payment; regenerated only after success or an explicit reset.
    private var clientRefId: String? = null

    init {
        loadBanks()
    }

    /** Pull the server bank catalog (single source of truth); keep the bundled fallback on failure. */
    private fun loadBanks() {
        viewModelScope.launch {
            val result = paymentsRepository.getBanks().first { it !is DataState.Loading }
            if (result is DataState.Success && result.data.isNotEmpty()) {
                mutableStateFlow.update { it.copy(banks = result.data) }
            }
        }
    }

    override fun handleAction(action: PayAction) {
        when (action) {
            is PayAction.ModeChanged -> mutableStateFlow.update {
                it.copy(mode = action.mode, error = null)
            }
            is PayAction.AmountChanged -> mutableStateFlow.update {
                it.copy(amount = action.value.filterAmount(), error = null)
            }
            is PayAction.PhoneChanged -> mutableStateFlow.update {
                it.copy(phone = action.value.trim(), error = null)
            }
            is PayAction.HolderNameChanged -> mutableStateFlow.update {
                it.copy(accountHolderName = action.value, error = null)
            }
            // Selecting a bank auto-populates the universal branch code (the user never types it).
            is PayAction.BankSelected -> mutableStateFlow.update {
                it.copy(bankName = action.bank.name, branchCode = action.bank.universalBranchCode, error = null)
            }
            is PayAction.AccountNumberChanged -> mutableStateFlow.update {
                it.copy(accountNumber = action.value.trim(), error = null)
            }
            is PayAction.AccountTypeChanged -> mutableStateFlow.update {
                it.copy(accountType = action.value, error = null)
            }
            is PayAction.RailChanged -> mutableStateFlow.update {
                it.copy(bankRail = action.rail, error = null)
            }
            // Tapping the pay button REVIEWS the payment (shows the confirm step); it does not send.
            PayAction.Submit -> review()
            PayAction.Retry -> review()
            // The user confirmed on the review screen — this is the only path that actually sends money.
            PayAction.ConfirmSend -> performSend()
            // Back out of the confirm step to edit the payment.
            PayAction.EditPayment -> mutableStateFlow.update { it.copy(showConfirm = false) }
            PayAction.SendAsEft -> {
                // The instant attempt is terminally FAILED server-side — this is a NEW payment
                // on the Standard rail, so it needs a fresh idempotency key.
                clientRefId = null
                mutableStateFlow.update { it.copy(result = null, showConfirm = false, bankRail = PayoutRail.EFT) }
                review()
            }
            PayAction.DismissResult -> {
                clientRefId = null
                mutableStateFlow.update { PayState(mode = it.mode) }
            }
            // Round-trip result from the passcode/biometric gate (screen-observed saved-state-handle).
            is PayAction.UpdateUserVerificationResult -> mutableStateFlow.update {
                it.copy(userVerificationResult = action.result, isAwaitingPasscodeVerification = false)
            }
        }
    }

    /** Validate the form and, if valid, show the confirm step. Does NOT send. */
    private fun review() {
        if (validate() == null) return
        mutableStateFlow.update { it.copy(showConfirm = true, error = null) }
    }

    /** Returns the parsed amount if the form is valid, else null (and sets an error). */
    private fun validate(): ZarAmount? {
        val current = state
        val amount = try {
            ZarAmount.fromRands(current.amount)
        } catch (e: Exception) {
            mutableStateFlow.update { it.copy(error = "Enter a valid amount.") }
            return null
        }
        when (current.mode) {
            PayMode.NUMBER -> {
                if (current.phone.isBlank()) {
                    mutableStateFlow.update { it.copy(error = "Enter a phone number.") }
                    return null
                }
                if (!PayShapLimits.isWithinPayShapCap(amount)) {
                    mutableStateFlow.update {
                        it.copy(error = "Amounts of R50,000 or more must be paid to a bank account.")
                    }
                    return null
                }
            }
            PayMode.BANK -> {
                if (current.bankName.isBlank() || current.branchCode.isBlank()) {
                    mutableStateFlow.update { it.copy(error = "Select the recipient's bank.") }
                    return null
                }
                if (current.accountHolderName.isBlank() || current.accountNumber.isBlank()) {
                    mutableStateFlow.update { it.copy(error = "Enter the account holder name and number.") }
                    return null
                }
                if (current.bankRail == PayoutRail.PAYSHAP && !PayShapLimits.isWithinPayShapCap(amount)) {
                    mutableStateFlow.update {
                        it.copy(error = "Instant payments must be under R50,000 — switch to Standard (EFT).")
                    }
                    return null
                }
            }
        }
        return amount
    }

    private fun performSend() {
        val current = state
        val amount = validate() ?: return

        viewModelScope.launch {
            // Step-up auth: a send at or above the threshold must be re-authenticated (passcode or
            // biometric) right before it leaves the wallet, even though the session is already unlocked.
            if (amount.minorUnits >= REAUTH_THRESHOLD_MINOR_UNITS && !verifyUser()) {
                mutableStateFlow.update {
                    it.copy(
                        isSubmitting = false,
                        statusText = null,
                        error = "We couldn't verify it's you. Please try again.",
                    )
                }
                return@launch
            }
            executeSend(current, amount)
        }
    }

    /**
     * Suspends until the passcode/biometric gate round trip completes. Emits
     * [PayEvent.NavigateForPasscodeVerification], waits for the screen to write the boolean back via
     * [PayAction.UpdateUserVerificationResult], then confirms the 30 s token with
     * [UserVerificationRepository.consumeVerification]. Returns true only when both pass.
     */
    private suspend fun verifyUser(): Boolean {
        mutableStateFlow.update {
            it.copy(userVerificationResult = null, isAwaitingPasscodeVerification = true)
        }
        sendEvent(PayEvent.NavigateForPasscodeVerification)
        val result = stateFlow
            .map { it.userVerificationResult }
            .filter { it != null }
            .first()
        return result == true && userVerificationRepository.consumeVerification()
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun executeSend(current: PayState, amount: ZarAmount) {
        val ref = clientRefId ?: Uuid.randomRef().also { clientRefId = it }

        mutableStateFlow.update { it.copy(isSubmitting = true, error = null, statusText = "Sending…") }

        // defaultAccountId is DataStore-backed and resolves asynchronously — await the
        // first real (non-null) value instead of reading .value synchronously, which
        // returns the flow's null initial value even when a default account exists.
        val payerAccountId = withTimeoutOrNull(PAYER_LOOKUP_TIMEOUT_MS) {
            preferencesRepository.defaultAccountId.first { it != null }
        }?.toString()
        if (payerAccountId.isNullOrEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    isSubmitting = false,
                    statusText = null,
                    error = "No wallet account is selected. Open Home once to load your wallet, then try again.",
                )
            }
            return
        }

        val initiate = when (current.mode) {
            PayMode.NUMBER -> paymentsRepository.payOnUs(
                payerAccountId = payerAccountId,
                target = PaymentTarget.Phone(current.phone),
                amount = amount,
                clientRefId = ref,
            )
            PayMode.BANK -> paymentsRepository.payoutToBank(
                payerAccountId = payerAccountId,
                bankAccount = BankAccount(
                    accountHolderName = current.accountHolderName.trim(),
                    bankName = current.bankName.trim(),
                    universalBranchCode = current.branchCode,
                    accountNumber = current.accountNumber,
                    accountType = current.accountType,
                ),
                amount = amount,
                clientRefId = ref,
                rail = current.bankRail,
            )
        }.first { it !is DataState.Loading }

        when (initiate) {
            is DataState.Error -> mutableStateFlow.update {
                it.copy(isSubmitting = false, statusText = null, error = initiate.message)
            }
            is DataState.Success -> pollUntilTerminal(initiate.data.transactionId)
            DataState.Loading -> Unit
        }
    }

    private suspend fun pollUntilTerminal(paymentId: String) {
        mutableStateFlow.update { it.copy(statusText = "Confirming…") }
        repeat(MAX_POLLS) {
            val status = paymentsRepository.getPaymentStatus(paymentId)
                .first { it !is DataState.Loading }
            if (status is DataState.Success) {
                val paymentState = status.data.state
                if (PaymentState.isTerminal(paymentState)) {
                    clientRefId = null
                    val reasonCode = status.data.reasonCode
                    mutableStateFlow.update {
                        it.copy(
                            isSubmitting = false,
                            statusText = null,
                            result = if (paymentState == PaymentState.SUCCESS) {
                                PayResult.Success(paymentId, status.data.route)
                            } else {
                                PayResult.Failure(
                                    message = failureMessage(paymentState, reasonCode, status.data.reasonMessage),
                                    reasonCode = reasonCode,
                                    canSendAsEft = state.mode == PayMode.BANK &&
                                        PaymentReasonCode.isRetryableAsEft(reasonCode),
                                )
                            },
                        )
                    }
                    return
                }
            }
            delay(POLL_INTERVAL_MS)
        }
        // Still pending after the budget — the payment may still settle server-side.
        mutableStateFlow.update {
            it.copy(
                isSubmitting = false,
                statusText = null,
                result = PayResult.Pending(paymentId),
            )
        }
    }

    /** Human message for a terminal failure, keyed on the server's machine-readable reason. */
    private fun failureMessage(state: String, reasonCode: String?, reasonMessage: String?): String =
        when (reasonCode) {
            PaymentReasonCode.BANK_NOT_ON_PAYSHAP ->
                "This bank doesn't support instant payments yet."
            PaymentReasonCode.AMOUNT_OVER_PAYSHAP_CAP ->
                "Instant payments must be under R50,000."
            PaymentReasonCode.LEDGER_REJECTED ->
                "The payment was declined — check your available balance."
            PaymentReasonCode.PROVIDER_FAILED ->
                "The payment couldn't be completed. Your money has been returned to your wallet."
            else -> reasonMessage ?: "Payment $state."
        }

    companion object {
        private const val MAX_POLLS = 20
        private const val POLL_INTERVAL_MS = 2_000L
        private const val PAYER_LOOKUP_TIMEOUT_MS = 3_000L

        /** Sends at or above R1,000.00 require a step-up passcode/biometric re-auth before leaving. */
        const val REAUTH_THRESHOLD_MINOR_UNITS: Long = 1_000_00L
    }
}

/** SavedStateHandle key the Pay screen observes for the passcode-gate round-trip boolean. */
const val PAY_VERIFICATION_KEY = "pay_send_verification_key"

@OptIn(ExperimentalUuidApi::class)
private fun Uuid.Companion.randomRef(): String = random().toString()

/** Keep only digits and a single decimal point for the rands amount field. */
private fun String.filterAmount(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    return if (firstDot == -1) {
        cleaned
    } else {
        cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")
    }
}

enum class PayMode { NUMBER, BANK }

/** Bank account types accepted by the Payments API for EFT. */
val ACCOUNT_TYPES = listOf(
    BankAccountType.CHEQUE,
    BankAccountType.SAVINGS,
    BankAccountType.TRANSMISSION,
)

data class PayState(
    val mode: PayMode = PayMode.NUMBER,
    val amount: String = "",
    val phone: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val branchCode: String = "",
    val accountNumber: String = "",
    val accountType: String = BankAccountType.CHEQUE,
    // Bank picker options — defaults to the bundled fallback, replaced by the server catalog on load.
    val banks: List<SaBank> = SouthAfricanBanks.ALL,
    // Instant (PayShap) is the preferred default for bank payouts; Standard (EFT) is the fallback.
    val bankRail: String = PayoutRail.PAYSHAP,
    // True once the form is validated and the user is on the Confirm/review step (before sending).
    val showConfirm: Boolean = false,
    val isSubmitting: Boolean = false,
    val statusText: String? = null,
    val error: String? = null,
    val result: PayResult? = null,
    // Step-up auth round-trip: null until the passcode gate returns; true = verified, false = cancelled/rejected.
    val userVerificationResult: Boolean? = null,
    // True between emitting NavigateForPasscodeVerification and receiving the result (drives the cancel guard).
    val isAwaitingPasscodeVerification: Boolean = false,
) {
    val payButtonLabel: String
        get() = when (mode) {
            PayMode.NUMBER -> "Pay to number"
            PayMode.BANK -> if (bankRail == PayoutRail.PAYSHAP) "Pay instantly" else "Pay by EFT"
        }

    /** Confirm-step: who the money is going to. */
    val confirmRecipient: String
        get() = when (mode) {
            PayMode.NUMBER -> phone
            PayMode.BANK -> buildString {
                append(accountHolderName.trim().ifBlank { "Bank account" })
                if (bankName.isNotBlank()) append(" · $bankName")
                if (accountNumber.length >= 4) append(" ••${accountNumber.takeLast(4)}")
            }
        }

    /** Confirm-step: how it will be sent + timing. */
    val confirmMethod: String
        get() = when (mode) {
            PayMode.NUMBER -> "To mobile number · instant if they're a SimpliPay user"
            PayMode.BANK -> if (bankRail == PayoutRail.PAYSHAP) {
                "Instant (PayShap) · arrives in seconds"
            } else {
                "Standard (EFT) · 1–2 business days"
            }
        }

    /** Confirm-step: fee. On-us and current payouts are free; the receipt confirms any fee applied. */
    val confirmFee: String get() = "Free"
}

sealed interface PayResult {
    data class Success(val paymentId: String, val route: String?) : PayResult
    data class Pending(val paymentId: String) : PayResult
    data class Failure(
        val message: String,
        val reasonCode: String? = null,
        /** True when the same payment can be re-sent one-tap on the Standard (EFT) rail. */
        val canSendAsEft: Boolean = false,
    ) : PayResult
}

sealed interface PayAction {
    data class ModeChanged(val mode: PayMode) : PayAction
    data class AmountChanged(val value: String) : PayAction
    data class PhoneChanged(val value: String) : PayAction
    data class HolderNameChanged(val value: String) : PayAction
    data class BankSelected(val bank: SaBank) : PayAction
    data class AccountTypeChanged(val value: String) : PayAction
    data class AccountNumberChanged(val value: String) : PayAction
    data class RailChanged(val rail: String) : PayAction

    /** Review the payment (validate + show the confirm step). Does not send. */
    data object Submit : PayAction
    data object Retry : PayAction

    /** Confirm on the review step — the only action that actually sends money. */
    data object ConfirmSend : PayAction

    /** Return from the confirm step to the form. */
    data object EditPayment : PayAction

    /** Resend the failed instant attempt as a Standard (EFT) payment — new clientRefId. */
    data object SendAsEft : PayAction
    data object DismissResult : PayAction

    /** Result of the passcode/biometric gate, written back by the screen from its saved-state-handle. */
    data class UpdateUserVerificationResult(val result: Boolean) : PayAction
}

sealed interface PayEvent {
    /** Ask the host to push the internal passcode gate; the screen forwards [PAY_VERIFICATION_KEY]. */
    data object NavigateForPasscodeVerification : PayEvent
}
