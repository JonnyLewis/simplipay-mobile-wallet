/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.push

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifos.corebase.network.httpClient
import org.mifos.corebase.network.setupDefaultHttpClient
import org.mifospay.feature.payments.pay.WalletTransferNotifier

/**
 * Base URL of the standalone notification engine (`notification-service`).
 * Dev default reaches the engine on the Mac host from the iOS simulator.
 * TODO: move into instance config (Supabase-managed like the Fineract URLs)
 *  before device/TestFlight builds — a physical phone cannot see localhost.
 */
const val NOTIFICATION_ENGINE_URL = "http://localhost:8085"

private val EngineHttpClient = named("notificationEngineHttpClient")

val PushModule = module {
    single(EngineHttpClient) {
        httpClient(
            config = setupDefaultHttpClient(
                baseUrl = "$NOTIFICATION_ENGINE_URL/",
                loggableHosts = listOf("localhost"),
            ),
        )
    }

    single {
        PushDeviceRegistrar(
            client = get(EngineHttpClient),
            baseUrl = NOTIFICATION_ENGINE_URL,
        )
    }

    single<WalletTransferNotifier> {
        NotificationEngineWalletTransferNotifier(
            client = get(EngineHttpClient),
            baseUrl = NOTIFICATION_ENGINE_URL,
            searchRepository = get(),
        )
    }

    single {
        PushRegistrationCoordinator(
            preferencesRepository = get(),
            registrar = get(),
        )
    }
}
