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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Thrown when a proxy/phone (PayShap) payout is attempted at or above the server cap. */
class PayShapCapExceededException(
    val amount: ZarAmount,
) : IllegalArgumentException(
    "PayShap payouts must be below R${PayShapLimits.CAP_MINOR_UNITS / 100}. " +
        "Amount R${amount.toApiString()} requires bank details (EFT).",
)

/** Server-enforced limits the app mirrors in its UX. */
object PayShapLimits {
    /** PayShap proxy/phone payout cap: amount must be strictly below R50,000. */
    const val CAP_MINOR_UNITS: Long = 50_000_00L

    /** True if [amount] is a valid PayShap proxy payout (strictly below the cap). */
    fun isWithinPayShapCap(amount: ZarAmount): Boolean =
        amount.minorUnits < CAP_MINOR_UNITS
}

/**
 * Where a payment is going. The server decides the actual rail (ON_US / PAYSHAP / EFT); the
 * app only fills `payee` + `customData` per the destination-mapping table.
 */
sealed interface PaymentTarget {
    val partyIdType: String
    val identifier: String

    /** Pay by phone (E.164, e.g. `27831234567`). On-us if the number is a SimpliPay
     *  customer, else PayShap — the server decides. Also the shape used for instant payout. */
    data class Phone(val msisdn: String) : PaymentTarget {
        override val partyIdType = PartyIdType.MSISDN
        override val identifier = msisdn
    }

    /** Pay by account number / barcode / QR / proximity — the account id decoded in-app. */
    data class Account(val accountId: String) : PaymentTarget {
        override val partyIdType = PartyIdType.ACCOUNT_ID
        override val identifier = accountId
    }
}

/**
 * Builds [TransferRequest]s for the Payments API. Pure and side-effect-free so the mapping
 * — especially the EFT `bankAccount` **JSON-string** encoding — is unit-testable.
 *
 * `clientRefId` is supplied by the caller: it is the idempotency key, generated once when the
 * user confirms a payment and **reused on retry** so a resend never double-charges.
 */
object PaymentRequestFactory {

    // encodeDefaults so every BankAccount field is present in the serialized JSON string.
    private val bankAccountJson = Json { encodeDefaults = true }

    /**
     * On-us / phone / account / QR / proximity payment. The payer is always the wallet
     * savings account (ACCOUNT_ID); the payee follows [target].
     */
    fun onUs(
        payerAccountId: String,
        target: PaymentTarget,
        amount: ZarAmount,
        clientRefId: String,
    ): TransferRequest = build(
        payerAccountId = payerAccountId,
        payeeType = target.partyIdType,
        payeeIdentifier = target.identifier,
        amount = amount,
        customData = providerCustomData(),
        clientRefId = clientRefId,
    )

    /**
     * Instant payout to a phone (PayShap proxy). Enforces the sub-R50,000 cap up front;
     * amounts at/above the cap must use [payoutToBank].
     */
    fun payoutInstant(
        payerAccountId: String,
        phoneMsisdn: String,
        amount: ZarAmount,
        clientRefId: String,
    ): TransferRequest {
        if (!PayShapLimits.isWithinPayShapCap(amount)) throw PayShapCapExceededException(amount)
        return build(
            payerAccountId = payerAccountId,
            payeeType = PartyIdType.MSISDN,
            payeeIdentifier = phoneMsisdn,
            amount = amount,
            customData = providerCustomData(),
            clientRefId = clientRefId,
        )
    }

    /**
     * Payout to a bank account on the user-chosen [rail]: [PayoutRail.PAYSHAP] (instant —
     * enforces the sub-R50,000 cap up front; the server additionally requires a participating
     * bank) or [PayoutRail.EFT] (standard). The payee identifier is the bank account number,
     * the rail rides `payee.subIdOrType`, and the [BankAccount] is serialized to a JSON
     * **string** carried in `customData.value` (not nested as an object).
     */
    fun payoutToBank(
        payerAccountId: String,
        bankAccount: BankAccount,
        amount: ZarAmount,
        clientRefId: String,
        rail: String = PayoutRail.EFT,
    ): TransferRequest {
        require(rail == PayoutRail.PAYSHAP || rail == PayoutRail.EFT) { "rail must be PAYSHAP or EFT" }
        if (rail == PayoutRail.PAYSHAP && !PayShapLimits.isWithinPayShapCap(amount)) {
            throw PayShapCapExceededException(amount)
        }
        return build(
            payerAccountId = payerAccountId,
            payeeType = PartyIdType.ACCOUNT_ID,
            payeeIdentifier = bankAccount.accountNumber,
            // subIdOrType is the preferred rail transport, but the deployed openMF channel image drops it
            // (verified on prod). We ALSO embed the rail in the bankAccount JSON below (customData survives),
            // and the connector reads that as the fallback. Keep subIdOrType for when the channel is fixed.
            payeeSubIdOrType = rail,
            amount = amount,
            customData = TransferCustomData(
                key = CustomDataKey.BANK_ACCOUNT,
                value = encodeBankAccountWithRail(bankAccount, rail),
            ),
            clientRefId = clientRefId,
        )
    }

    /** Serialize the bank details plus a `rail` field, so the rail rides customData (which the channel forwards). */
    private fun encodeBankAccountWithRail(bankAccount: BankAccount, rail: String): String {
        val fields = bankAccountJson
            .encodeToJsonElement(BankAccount.serializer(), bankAccount)
            .let { it as JsonObject }
            .toMutableMap()
        fields["rail"] = JsonPrimitive(rail)
        return bankAccountJson.encodeToString(JsonObject(fields))
    }

    private fun providerCustomData(): TransferCustomData =
        TransferCustomData(key = CustomDataKey.PROVIDER, value = DEFAULT_PROVIDER)

    private fun build(
        payerAccountId: String,
        payeeType: String,
        payeeIdentifier: String,
        amount: ZarAmount,
        customData: TransferCustomData,
        clientRefId: String,
        payeeSubIdOrType: String? = null,
    ): TransferRequest = TransferRequest(
        clientRefId = clientRefId,
        payer = TransferParty(
            PartyIdInfo(
                partyIdType = PartyIdType.ACCOUNT_ID,
                partyIdentifier = payerAccountId,
            ),
        ),
        payee = TransferParty(
            PartyIdInfo(
                partyIdType = payeeType,
                partyIdentifier = payeeIdentifier,
                subIdOrType = payeeSubIdOrType,
            ),
        ),
        amount = TransferAmount(amount = amount.toApiString(), currency = CURRENCY_ZAR),
        customData = customData,
    )
}
