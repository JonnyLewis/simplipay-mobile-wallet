/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.mpay.qr.scan.ocr

import io.github.vinceglb.filekit.PlatformFile

// TODO(android): implement with ML Kit Text Recognition v2 (com.google.mlkit:text-recognition).
// iOS-first per current scope.
actual suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText =
    RecognizedText(emptyList())
