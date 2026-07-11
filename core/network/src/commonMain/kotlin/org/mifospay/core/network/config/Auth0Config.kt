/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.network.config

/** Public Auth0 settings for the native mobile application. No client secret belongs here. */
object Auth0Config {
    const val DOMAIN = "simplipay.eu.auth0.com"
    const val CLIENT_ID = "07LfaEKDp4oBLOXgsjZBb7eeUci4TNL2"
    const val AUDIENCE = "https://api.simplipay.co.za/"
    const val DATABASE_CONNECTION = "Username-Password-Authentication"
}
