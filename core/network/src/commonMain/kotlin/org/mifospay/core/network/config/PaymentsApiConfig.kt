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

/**
 * Static configuration for the **SimpliPay Payments API** (`POST /channel/transfer`,
 * `GET /payments/{id}`, `POST /payin`).
 *
 * This is a *different* service from Fineract: it is not driven by the per-instance
 * [InstanceConfigManager]. It has its own host, its own `Platform-TenantId` header, and a
 * shared `?token=` query secret. Values live here so the base URL is a single build-config
 * point and the token is trivial to source/rotate.
 *
 * SECURITY — read before shipping:
 *  - [SHARED_TOKEN] is a **secret**. It must be blank in version control and injected at
 *    build time from a gitignored properties file / CI secret / build-time constant (the
 *    same approach recommended for [ServiceAccountConfig]). Never commit a real token.
 *  - v1 calls the Payments API directly from the app. If/when a backend proxy exists,
 *    move the token server-side and drop it from the app entirely.
 *
 * PRODUCT NOTE: [BASE_URL] and [NON_PROD_BASE_URL] currently point at the same host until a
 * dedicated staging host exists. Swap [NON_PROD_BASE_URL] when the non-prod host is known.
 */
object PaymentsApiConfig {
    /** Production Payments API base URL. Must end with a trailing slash for Ktor. */
    const val BASE_URL: String = "https://api.simplipay.co.za/"

    /** Non-prod / staging Payments API base URL. Same as prod for now (see class KDoc). */
    const val NON_PROD_BASE_URL: String = "https://api.simplipay.co.za/"

    /** Tenant sent as the `Platform-TenantId` header on `POST /channel/transfer`. */
    const val TENANT_ID: String = "default"

    /**
     * Shared token for the `?token=` query on `GET /payments/{id}` and `POST /payin`.
     * MUST be injected at build time — keep blank in VCS. See class KDoc.
     */
    const val SHARED_TOKEN: String = ""

    /** Header name carrying [TENANT_ID] on the transfer endpoint. */
    const val HEADER_TENANT_ID: String = "Platform-TenantId"

    /**
     * Resolves the base URL for the given build flavour.
     *
     * @param isProduction whether the running build targets production.
     */
    fun baseUrl(isProduction: Boolean): String =
        if (isProduction) BASE_URL else NON_PROD_BASE_URL
}
