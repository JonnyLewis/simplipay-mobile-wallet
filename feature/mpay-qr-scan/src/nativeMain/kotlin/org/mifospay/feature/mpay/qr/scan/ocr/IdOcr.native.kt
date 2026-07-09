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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * iOS OCR via Apple Vision. Reads the still image at [PlatformFile.nsUrl] and returns
 * every recognised line with its confidence and normalised (top-left origin) box.
 *
 * Tuned for ID cards: `.accurate` recognition (we are not latency-bound on a still),
 * language correction OFF (so the engine never "autocorrects" a 13-digit ID number or
 * surname into dictionary words). Validation (Luhn etc.) happens downstream in the parser.
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText =
    withContext(Dispatchers.Default) {
        try {
            val nsUrl = file.nsUrl

            val request = VNRecognizeTextRequest().apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                usesLanguageCorrection = false
                recognitionLanguages = listOf("en")
            }

            val handler = VNImageRequestHandler(uRL = nsUrl, options = emptyMap<Any?, Any?>())

            val ok = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                handler.performRequests(listOf(request), error.ptr)
            }
            if (!ok) return@withContext RecognizedText(emptyList())

            val observations = request.results
                ?.filterIsInstance<VNRecognizedTextObservation>()
                .orEmpty()

            val lines = observations.mapNotNull { obs ->
                val candidate = obs.topCandidates(1.convert()).firstOrNull() as? VNRecognizedText
                    ?: return@mapNotNull null
                // Vision boxes are normalised with a BOTTOM-left origin; flip to top-left.
                obs.boundingBox.useContents {
                    OcrLine(
                        text = candidate.string,
                        confidence = candidate.confidence,
                        left = origin.x,
                        top = 1.0 - (origin.y + size.height),
                        width = size.width,
                        height = size.height,
                    )
                }
            }.sortedBy { it.top }

            RecognizedText(lines)
        } catch (e: Exception) {
            println("IdOcr: OCR failed: ${e.message}")
            RecognizedText(emptyList())
        }
    }
