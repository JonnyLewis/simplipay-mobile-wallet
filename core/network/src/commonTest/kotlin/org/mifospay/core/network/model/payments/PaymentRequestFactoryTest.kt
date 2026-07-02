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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PaymentRequestFactoryTest {

    private val json = Json { encodeDefaults = true }
    private val payer = "1001"
    private val ref = "ref-uuid-123"
    private val amount = ZarAmount.fromRands("50.00")

    @Test
    fun payByPhone_mapsToMsisdnWithAdpayProvider() {
        val req = PaymentRequestFactory.onUs(
            payerAccountId = payer,
            target = PaymentTarget.Phone("27831234567"),
            amount = amount,
            clientRefId = ref,
        )

        assertEquals(ref, req.clientRefId)
        assertEquals(PartyIdType.ACCOUNT_ID, req.payer.partyIdInfo.partyIdType)
        assertEquals(payer, req.payer.partyIdInfo.partyIdentifier)
        assertEquals(PartyIdType.MSISDN, req.payee.partyIdInfo.partyIdType)
        assertEquals("27831234567", req.payee.partyIdInfo.partyIdentifier)
        assertEquals("50.00", req.amount.amount)
        assertEquals(CURRENCY_ZAR, req.amount.currency)
        assertEquals(CustomDataKey.PROVIDER, req.customData.key)
        assertEquals(DEFAULT_PROVIDER, req.customData.value)
    }

    @Test
    fun payByAccount_mapsToAccountId() {
        val req = PaymentRequestFactory.onUs(
            payerAccountId = payer,
            target = PaymentTarget.Account("acc-42"),
            amount = amount,
            clientRefId = ref,
        )
        assertEquals(PartyIdType.ACCOUNT_ID, req.payee.partyIdInfo.partyIdType)
        assertEquals("acc-42", req.payee.partyIdInfo.partyIdentifier)
        assertEquals(CustomDataKey.PROVIDER, req.customData.key)
    }

    @Test
    fun payoutToBank_encodesBankAccountAsJsonString() {
        val bank = BankAccount(
            accountHolderName = "Jane Doe",
            bankName = "Test Bank",
            universalBranchCode = "250655",
            accountNumber = "62001234567",
            accountType = BankAccountType.CHEQUE,
        )

        val req = PaymentRequestFactory.payoutToBank(
            payerAccountId = payer,
            bankAccount = bank,
            amount = ZarAmount.fromRands("75000.00"),
            clientRefId = ref,
        )

        // Payee is the bank account number under ACCOUNT_ID; default rail is Standard (EFT).
        assertEquals(PartyIdType.ACCOUNT_ID, req.payee.partyIdInfo.partyIdType)
        assertEquals("62001234567", req.payee.partyIdInfo.partyIdentifier)
        assertEquals(PayoutRail.EFT, req.payee.partyIdInfo.subIdOrType)
        assertEquals(CustomDataKey.BANK_ACCOUNT, req.customData.key)

        // customData.value must be a JSON *string* that round-trips back to the object,
        // not a nested object. (It also carries an extra "rail" field the connector reads, so decode
        // with a lenient parser that ignores it.)
        val lenient = Json { ignoreUnknownKeys = true }
        val decoded = lenient.decodeFromString(BankAccount.serializer(), req.customData.value)
        assertEquals(bank, decoded)
        // rail is embedded in the same JSON (survives the channel where subIdOrType does not).
        assertTrue(req.customData.value.contains("\"rail\":\"EFT\""))

        // And when the whole request is serialized, the value is an escaped string literal
        // (i.e. "value":"{\"accountHolderName\":...}"), never a nested {"value":{...}}.
        val wire = json.encodeToString(TransferRequest.serializer(), req)
        assertTrue(
            wire.contains("\"value\":\"{\\\"accountHolderName\\\":\\\"Jane Doe\\\""),
            "bankAccount must be a serialized JSON string, was: $wire",
        )
    }

    @Test
    fun payoutToBank_instantRail_setsSubIdOrType() {
        val bank = BankAccount("Jane Doe", "Test Bank", "250655", "62001234567", BankAccountType.CHEQUE)
        val req = PaymentRequestFactory.payoutToBank(
            payerAccountId = payer,
            bankAccount = bank,
            amount = ZarAmount.fromRands("1200.00"),
            clientRefId = ref,
            rail = PayoutRail.PAYSHAP,
        )
        assertEquals(PayoutRail.PAYSHAP, req.payee.partyIdInfo.subIdOrType)
        assertEquals(CustomDataKey.BANK_ACCOUNT, req.customData.key)
    }

    @Test
    fun payoutToBank_instantRail_atOrAboveCap_throws() {
        val bank = BankAccount("Jane Doe", "Test Bank", "250655", "62001234567", BankAccountType.CHEQUE)
        assertFailsWith<PayShapCapExceededException> {
            PaymentRequestFactory.payoutToBank(
                payerAccountId = payer,
                bankAccount = bank,
                amount = ZarAmount.fromRands("50000.00"),
                clientRefId = ref,
                rail = PayoutRail.PAYSHAP,
            )
        }
        // The same amount is fine on the Standard rail.
        val eft = PaymentRequestFactory.payoutToBank(
            payerAccountId = payer,
            bankAccount = bank,
            amount = ZarAmount.fromRands("50000.00"),
            clientRefId = ref,
            rail = PayoutRail.EFT,
        )
        assertEquals(PayoutRail.EFT, eft.payee.partyIdInfo.subIdOrType)
    }

    @Test
    fun onUs_hasNoRail() {
        val req = PaymentRequestFactory.onUs(
            payerAccountId = payer,
            target = PaymentTarget.Phone("27831234567"),
            amount = amount,
            clientRefId = ref,
        )
        assertEquals(null, req.payee.partyIdInfo.subIdOrType)
        // subIdOrType must be ABSENT from the wire for non-bank payments (default omitted).
        val wire = Json.encodeToString(TransferRequest.serializer(), req)
        assertTrue(!wire.contains("subIdOrType"), "null subIdOrType must not serialize, was: $wire")
    }

    @Test
    fun payoutInstant_belowCap_ok() {
        val req = PaymentRequestFactory.payoutInstant(
            payerAccountId = payer,
            phoneMsisdn = "27831234567",
            amount = ZarAmount.fromMinorUnits(PayShapLimits.CAP_MINOR_UNITS - 1),
            clientRefId = ref,
        )
        assertEquals(PartyIdType.MSISDN, req.payee.partyIdInfo.partyIdType)
        assertEquals(DEFAULT_PROVIDER, req.customData.value)
    }

    @Test
    fun payoutInstant_atOrAboveCap_throws() {
        assertFailsWith<PayShapCapExceededException> {
            PaymentRequestFactory.payoutInstant(
                payerAccountId = payer,
                phoneMsisdn = "27831234567",
                amount = ZarAmount.fromMinorUnits(PayShapLimits.CAP_MINOR_UNITS),
                clientRefId = ref,
            )
        }
        assertFailsWith<PayShapCapExceededException> {
            PaymentRequestFactory.payoutInstant(
                payerAccountId = payer,
                phoneMsisdn = "27831234567",
                amount = ZarAmount.fromRands("60000.00"),
                clientRefId = ref,
            )
        }
    }

    @Test
    fun payShapCap_boundary() {
        assertTrue(PayShapLimits.isWithinPayShapCap(ZarAmount.fromRands("49999.99")))
        assertTrue(!PayShapLimits.isWithinPayShapCap(ZarAmount.fromRands("50000.00")))
    }
}
