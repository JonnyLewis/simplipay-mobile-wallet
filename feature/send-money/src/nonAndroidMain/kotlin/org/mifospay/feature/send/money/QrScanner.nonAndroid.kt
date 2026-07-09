/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.send.money

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * No-op QR scanner for non-Android platforms (iOS, Desktop, Web).
 *
 * The Android implementation uses Google ML Kit's barcode scanner, which has no
 * multiplatform equivalent. Until a platform-native scanner is wired up, this
 * implementation emits no result so dependency injection can initialize without
 * crashing.
 */
class NoOpQrScanner : QrScanner {
    override fun startScanning(): Flow<String?> = flowOf(null)
}

actual val ScannerModule: Module
    get() = module {
        single<QrScanner> { NoOpQrScanner() }
    }
