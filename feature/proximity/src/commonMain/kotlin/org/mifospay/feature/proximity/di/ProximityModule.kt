/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifospay.feature.proximity.ProximityViewModel

/**
 * Koin wiring for the proximity feature.
 *
 * The platform-specific BLE transport is bound by [proximityPlatformModule]
 * (an `expect/actual` per source set, mirroring `core/common`'s
 * `ioDispatcherModule`): iOS binds the real CoreBluetooth transport, while
 * Android/Desktop/Web bind the capability-less Noop for now (Android real BLE
 * is the next increment — plan T4).
 */
val ProximityModule = module {
    includes(proximityPlatformModule)
    viewModelOf(::ProximityViewModel)
}

expect val proximityPlatformModule: Module
