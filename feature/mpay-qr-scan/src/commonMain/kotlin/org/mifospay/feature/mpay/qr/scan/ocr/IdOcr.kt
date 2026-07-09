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

/**
 * A single recognised line of text from an ID document, with its position on the
 * card. Positions are normalised to the image: 0..1, origin top-left.
 *
 * The bounding box is kept because SA ID documents have a fixed print layout, so a
 * later parser can map a line to a field by *where* it sits on the card (e.g. the
 * 13-digit ID number band, the surname row) rather than relying on labels alone.
 */
data class OcrLine(
    val text: String,
    val confidence: Float,
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
)

/**
 * The full OCR result for one captured image.
 */
data class RecognizedText(
    val lines: List<OcrLine>,
) {
    /** All recognised lines joined top-to-bottom, for quick inspection / copy. */
    val fullText: String
        get() = lines.joinToString("\n") { it.text }

    val isEmpty: Boolean get() = lines.isEmpty()
}

/**
 * Runs on-device OCR over a still image of an identity document and returns the raw
 * recognised text. No parsing or field mapping happens here — this is the capture +
 * recognise stage; mapping to SignUp fields is a deliberately separate step driven by
 * what real cards actually produce.
 *
 * iOS: Apple Vision (`VNRecognizeTextRequest`, accurate, no language correction).
 * Other platforms: not yet implemented (returns empty) — Android will use ML Kit.
 */
expect suspend fun recognizeTextFromImage(file: PlatformFile): RecognizedText
