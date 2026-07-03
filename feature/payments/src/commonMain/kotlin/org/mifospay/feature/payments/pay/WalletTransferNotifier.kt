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

/** Who received a completed wallet-to-wallet (on-us) credit. */
sealed interface WalletCreditRecipient {
    /** Known only by phone number (Pay to number) — the notifier resolves the wallet owner. */
    data class Phone(val msisdn: String) : WalletCreditRecipient

    /** Owner already resolved (beneficiary payment resolves the account before sending). */
    data class Client(val clientId: Long) : WalletCreditRecipient
}

/**
 * Tells the notification engine that a wallet-to-wallet payment settled, so the
 * receiving wallet gets an in-app notification. Fired only for [org.mifospay.core.network.model.payments.PaymentRoute.ON_US]
 * payments — external rails (PayShap/EFT) notify through the recipient's own bank.
 *
 * Best-effort side channel: callers fire-and-forget after the payment is already
 * settled; a delivery failure must never surface into the pay flow.
 */
interface WalletTransferNotifier {
    suspend fun notifyWalletCredit(
        recipient: WalletCreditRecipient,
        /** Display amount, e.g. `R150.00`. */
        amountDisplay: String,
        /** Sender's display name, if known. */
        senderName: String?,
        /** Settled payment id — used as the engine's dedupe key. */
        paymentId: String,
    )
}
