/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.network.model.auth0

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Auth0SignupPayload(
    val client_id: String,
    val connection: String,
    val username: String,
    val email: String,
    val password: String,
    val user_metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class Auth0SignupResponse(
    @SerialName("_id") val id: String? = null,
    val user_id: String? = null,
    val email: String? = null,
)

@Serializable
data class Auth0TokenPayload(
    val grant_type: String = "http://auth0.com/oauth/grant-type/password-realm",
    val username: String,
    val password: String,
    val realm: String,
    val client_id: String,
    val audience: String,
    val scope: String = "openid profile email",
)

@Serializable
data class Auth0TokenResponse(
    val access_token: String,
    val id_token: String? = null,
    val token_type: String = "Bearer",
    val expires_in: Long = 0,
)
