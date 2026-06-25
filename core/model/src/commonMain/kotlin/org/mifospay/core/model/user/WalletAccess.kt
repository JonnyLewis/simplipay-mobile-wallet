/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.model.user

/** Backend role name for an explicitly revoked wallet (see [WalletAccessState.REVOKED]). */
const val WALLET_REVOKED_ROLE: String = "wallet-access-revoked"

/**
 * Client-side sentinel role stamped when a previously-activated client becomes forbidden
 * (e.g. KYC removed mid-session, detected via a 403 probe). Kept DISTINCT from
 * [WALLET_REVOKED_ROLE] so a later successful probe can clear this inference without ever
 * stripping a genuine backend revoked role — which is authoritative and must always block.
 */
const val WALLET_REVOKED_PROBE_MARKER: String = "__wallet-access-revoked-probe__"

/** True if [this] role is either the real backend revoked role or the client-side probe marker. */
private fun RoleInfo.isRevoked(): Boolean =
    name.equals(WALLET_REVOKED_ROLE, ignoreCase = true) ||
        name.equals(WALLET_REVOKED_PROBE_MARKER, ignoreCase = true)

/**
 * The three mutually-exclusive states the wallet UI gates on.
 *
 * Activation is ultimately a backend concept (roles + permission to read the client), but the
 * phone can only observe two signals: the roles carried by the login response (authoritative,
 * but only at login) and whether `GET /self/clients/{id}` currently succeeds. [resolveWalletAccess]
 * folds both into one state the screens can switch on.
 */
enum class WalletAccessState {
    /** KYC-activated: real client readable → full wallet. */
    ACTIVATED,

    /** Never activated (new signup, `wallet-no-access`): locked/dummy home + OTP. */
    NOT_ACTIVATED,

    /** Access taken away (`wallet-access-revoked`, or a previously-activated client now forbidden):
     *  full-screen "Blocked, contact support". */
    REVOKED,
}

/**
 * Resolves the current [WalletAccessState] from the persisted [roles] and the stored [clientId].
 *
 * Order matters: revocation wins over everything (so `wallet-access-revoked` being present
 * anywhere in [roles] — even alongside an active KYC role — blocks the user), then a real
 * (non-zero) client means activated, otherwise the user is simply not activated yet. The stored
 * client is never null in practice (it defaults to id 0), so `clientId == 0` is the "no real
 * client" sentinel.
 */
fun resolveWalletAccess(roles: List<RoleInfo>, clientId: Long?): WalletAccessState = when {
    roles.any { it.isRevoked() } -> WalletAccessState.REVOKED
    (clientId ?: 0L) != 0L -> WalletAccessState.ACTIVATED
    else -> WalletAccessState.NOT_ACTIVATED
}
