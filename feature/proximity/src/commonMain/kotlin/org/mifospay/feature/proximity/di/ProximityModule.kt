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

import org.koin.dsl.module
import org.mifospay.feature.proximity.transport.BleProximityTransport
import org.mifospay.feature.proximity.transport.NoopBleProximityTransport

/**
 * Koin wiring for the proximity feature.
 *
 * The transport binding is currently the capability-less
 * [NoopBleProximityTransport] on every platform. When the real Android
 * (`BluetoothLe*`) and iOS (`CoreBluetooth`) transports land, this binding
 * moves to an `expect val proximityPlatformModule` with per-platform `actual`s
 * (mirroring `core/common`'s `ioDispatcherModule`), and `ProximityModule`
 * pulls it in via `includes(proximityPlatformModule)` — see the implementation
 * plan, tasks T1/T4.
 */
val ProximityModule = module {
    single<BleProximityTransport> { NoopBleProximityTransport() }
}
