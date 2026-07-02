/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.network.model.payments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the SimpliPay Payments API. Money on `POST /channel/transfer` is a **string in
 * rands** (`"50.00"`); money on the status/payin JSON is a number. On-us is detected by the
 * server; for a bank-account payout the USER picks the rail — Instant (PayShap) vs Standard
 * (EFT) — sent as `payee.partyIdInfo.subIdOrType`. Infeasible instant requests come back as a
 * FAILED payment with a machine-readable [PaymentReasonCode].
 */

/** `partyIdType` values the app is allowed to send. */
object PartyIdType {
    const val ACCOUNT_ID = "ACCOUNT_ID"
    const val MSISDN = "MSISDN"
}

/** The user's rail choice for a bank-account payout, sent as `payee.subIdOrType`. */
object PayoutRail {
    /** Instant — PayShap. Needs a participating bank and amount < R50,000. */
    const val PAYSHAP = "PAYSHAP"

    /** Standard — EFT batch, typically 1–2 business days. Always available. */
    const val EFT = "EFT"
}

/** `customData` keys. */
object CustomDataKey {
    const val PROVIDER = "provider"
    const val BANK_ACCOUNT = "bankAccount"
}

/** Default payment provider tag sent in `customData`. */
const val DEFAULT_PROVIDER = "ADPAY"

const val CURRENCY_ZAR = "ZAR"

// ---------------------------------------------------------------------------------------
// POST /channel/transfer
// ---------------------------------------------------------------------------------------

@Serializable
data class PartyIdInfo(
    val partyIdType: String,
    val partyIdentifier: String,
    // Rail choice for bank-account payouts (PayoutRail.PAYSHAP | PayoutRail.EFT); null elsewhere.
    val subIdOrType: String? = null,
)

@Serializable
data class TransferParty(
    val partyIdInfo: PartyIdInfo,
)

@Serializable
data class TransferAmount(
    val amount: String,
    val currency: String = CURRENCY_ZAR,
)

/**
 * One `{ "key": ..., "value": ... }` entry. For EFT the [value] is a **JSON string** — the
 * serialized [BankAccount] — not a nested object.
 */
@Serializable
data class TransferCustomData(
    val key: String,
    val value: String,
)

@Serializable
data class TransferRequest(
    val clientRefId: String,
    val payer: TransferParty,
    val payee: TransferParty,
    val amount: TransferAmount,
    // Single `{key,value}` object — the DEPLOYED channel connector's
    // TransactionChannelRequestDTO.customData is a single CustomData (verified: an array 400s
    // with "Cannot deserialize CustomData from Array value"). Not a list.
    val customData: TransferCustomData,
)

/** `200` from `/channel/transfer` — accepted, not settled. */
@Serializable
data class TransferResponse(
    val transactionId: String,
)

// ---------------------------------------------------------------------------------------
// EFT bank account (serialized into TransferCustomData.value as a JSON string)
// ---------------------------------------------------------------------------------------

/** `accountType` values accepted by the Payments API for EFT payouts. */
object BankAccountType {
    const val CHEQUE = "CHEQUE"
    const val SAVINGS = "SAVINGS"
    const val TRANSMISSION = "TRANSMISSION"
}

@Serializable
data class BankAccount(
    val accountHolderName: String,
    val bankName: String,
    val universalBranchCode: String,
    val accountNumber: String,
    val accountType: String,
)

// ---------------------------------------------------------------------------------------
// GET /payments/{paymentId}
// ---------------------------------------------------------------------------------------

/** Terminal + non-terminal payment states. */
object PaymentState {
    const val ACCEPTED = "ACCEPTED"
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val EXPIRED = "EXPIRED"

    /** States after which polling should stop. */
    val TERMINAL = setOf(SUCCESS, FAILED, EXPIRED)

    fun isTerminal(state: String?): Boolean = state != null && state in TERMINAL
}

/** Rail the server chose for a payment. */
object PaymentRoute {
    const val ON_US = "ON_US"
    const val PAYSHAP = "PAYSHAP"
    const val EFT = "EFT"
}

/**
 * Machine-readable reason on a FAILED payment (status + callback). The first two mean the
 * payment is re-sendable as Standard (EFT) — with a NEW clientRefId.
 */
object PaymentReasonCode {
    const val BANK_NOT_ON_PAYSHAP = "BANK_NOT_ON_PAYSHAP"
    const val AMOUNT_OVER_PAYSHAP_CAP = "AMOUNT_OVER_PAYSHAP_CAP"
    const val LEDGER_REJECTED = "LEDGER_REJECTED"
    const val PROVIDER_FAILED = "PROVIDER_FAILED"
    const val VALIDATION = "VALIDATION"

    /** True when the same payment can be re-offered on the Standard (EFT) rail. */
    fun isRetryableAsEft(code: String?): Boolean =
        code == BANK_NOT_ON_PAYSHAP || code == AMOUNT_OVER_PAYSHAP_CAP
}

@Serializable
data class PaymentStatusResponse(
    val paymentId: String,
    val clientRef: String? = null,
    val route: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val state: String,
    val reasonCode: String? = null,
    val reasonMessage: String? = null,
    val providerRef: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

// ---------------------------------------------------------------------------------------
// POST /payin  (app does not initiate this in v1 — kept for completeness)
// ---------------------------------------------------------------------------------------

/** `source` values for a pay-in. */
object PayinSource {
    const val CASH = "CASH"
    const val EFT = "EFT"
}

@Serializable
data class PayinRequest(
    val account: String,
    val amount: Double,
    val currency: String = CURRENCY_ZAR,
    val source: String,
    @SerialName("reference")
    val reference: String,
)
