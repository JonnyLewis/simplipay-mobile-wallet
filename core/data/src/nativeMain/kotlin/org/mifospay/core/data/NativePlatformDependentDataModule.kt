/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.TimeZone
import org.mifospay.core.data.di.PlatformDependentDataModule
import org.mifospay.core.data.util.NetworkMonitor
import org.mifospay.core.data.util.TimeZoneMonitor
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.Foundation.NSTimeZone
import platform.Foundation.resetSystemTimeZone
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

class NativePlatformDependentDataModule : PlatformDependentDataModule {
    override val networkMonitor: NetworkMonitor by lazy {
        object : NetworkMonitor {
            override val isOnline: Flow<Boolean> = callbackFlow {
                val monitor = nw_path_monitor_create()
                nw_path_monitor_set_update_handler(monitor) { path ->
                    trySend(nw_path_get_status(path) == nw_path_status_satisfied)
                }
                nw_path_monitor_set_queue(
                    monitor,
                    dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0uL),
                )
                nw_path_monitor_start(monitor)
                awaitClose { nw_path_monitor_cancel(monitor) }
            }.distinctUntilChanged().conflate()
        }
    }

    override val timeZoneMonitor: TimeZoneMonitor by lazy {
        object : TimeZoneMonitor {
            override val currentTimeZone: Flow<TimeZone> = callbackFlow {
                trySend(TimeZone.currentSystemDefault())
                val observer = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = NSSystemTimeZoneDidChangeNotification,
                    `object` = null,
                    queue = NSOperationQueue.mainQueue,
                ) { _ ->
                    // NSTimeZone caches the system zone; drop the cache before re-reading.
                    NSTimeZone.resetSystemTimeZone()
                    trySend(TimeZone.currentSystemDefault())
                }
                awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
            }.distinctUntilChanged().conflate()
        }
    }
}
