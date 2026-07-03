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

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Android OCR via ML Kit Text Recognition v2 (Latin, on-device). Mirrors the iOS
 * Vision actual: every recognised line with its confidence and a normalised
 * top-left-origin box (ML Kit boxes are already top-left, in pixels — divide by
 * the bitmap dimensions).
 *
 * Same decode path as [decodeQrFromFile]: bytes → bitmap → InputImage. Like that
 * path, EXIF rotation is not applied (rotation 0) — the KYC capture flow feeds
 * upright stills; revisit if gallery imports of rotated photos become a case.
 */
actual suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText =
    withContext(Dispatchers.IO) {
        try {
            val bytes = file.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext RecognizedText(emptyList())

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()

            val imageWidth = bitmap.width.toDouble()
            val imageHeight = bitmap.height.toDouble()

            val lines = result.textBlocks
                .flatMap { block -> block.lines }
                .mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    OcrLine(
                        text = line.text,
                        confidence = line.confidence,
                        left = box.left / imageWidth,
                        top = box.top / imageHeight,
                        width = box.width() / imageWidth,
                        height = box.height() / imageHeight,
                    )
                }
                .sortedBy { it.top }

            RecognizedText(lines)
        } catch (e: Exception) {
            println("IdOcr: OCR failed: ${e.message}")
            RecognizedText(emptyList())
        }
    }
