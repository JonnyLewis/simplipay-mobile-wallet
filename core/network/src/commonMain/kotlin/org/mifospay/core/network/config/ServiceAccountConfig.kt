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
 * HTTP Basic-auth credentials for Fineract **platform** API calls that run *before* a
 * user is authenticated — i.e. the signup uniqueness search and the
 * `createUser` → `createClient` → `assignClientToUser` onboarding chain, all routed
 * through `FineractApiManager` (the `BaseClient`). The matching account must exist on the
 * target Fineract instance and have permission to search clients and create users/clients.
 *
 * Externalised here (instead of inline in NetworkModule) so the credentials live in one
 * place and are trivial to rotate.
 *
 * SECURITY — read before shipping:
 *  - This is a stop-gap. Do **not** keep a real production secret in version control
 *    long-term. Before release, source it from a gitignored properties file / build-time
 *    secret / CI variable instead of a committed constant.
 *  - Prefer a **dedicated, least-privilege service account** (permission to search +
 *    create client/user only) over a full admin login, and rotate it if it leaks.
 */
object ServiceAccountConfig {
    /** Fineract platform service-account username. */
    const val PLATFORM_USERNAME: String = "mifos"

    /** Fineract platform service-account password. */
    const val PLATFORM_PASSWORD: String = "jdj2yMw5jUQ6Nw2Q!"
}
