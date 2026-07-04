/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.paylink

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.corebase.network.httpClient
import org.mifos.corebase.network.setupDefaultHttpClient
import org.mifospay.shared.ui.paylink.PayLinkViewModel

val PayLinkModule = module {
    single {
        PayLinkApi(
            client = httpClient(
                config = setupDefaultHttpClient(
                    baseUrl = "$SIMPLILINK_URL/",
                    loggableHosts = listOf("localhost"),
                ),
            ),
            baseUrl = SIMPLILINK_URL,
        )
    }

    viewModelOf(::PayLinkViewModel)
}
