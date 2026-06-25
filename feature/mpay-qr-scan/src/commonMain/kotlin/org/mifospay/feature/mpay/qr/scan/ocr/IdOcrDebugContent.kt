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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

/**
 * Development tool: pick a still photo of a South African driver's licence or Smart ID
 * card and dump the raw Apple Vision OCR output (every line + confidence + vertical
 * position). This is the "scan first, map later" step — feed it real example cards and
 * the output drives the fixed-format field parser we build next.
 *
 * [onRecognized] lets a host hook the result once a parser exists.
 */
@Composable
fun IdOcrDebugContent(
    modifier: Modifier = Modifier,
    onRecognized: (RecognizedText) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var isProcessing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RecognizedText?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            scope.launch {
                isProcessing = true
                error = null
                try {
                    val recognized = recognizeTextFromImage(file)
                    result = recognized
                    onRecognized(recognized)
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                    result = null
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "ID OCR — dev capture",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Pick a photo of a driver's licence or Smart ID. Vision reads it and " +
                "lists every line with confidence + position so we can build the field map.",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = { imagePicker.launch() },
            enabled = !isProcessing,
        ) {
            Text(if (result == null) "Pick ID photo" else "Pick another photo")
        }

        when {
            isProcessing -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Reading…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            error != null -> {
                Text(
                    text = "OCR error: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            result != null && result!!.isEmpty -> {
                Text(
                    text = "No text recognised. Try a sharper, evenly-lit, glare-free photo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            result != null -> {
                val recognized = result!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${recognized.lines.size} lines",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "Copy all",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { clipboard.setText(AnnotatedString(recognized.fullText)) }
                            .padding(vertical = 4.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recognized.lines.forEachIndexed { index, line ->
                        Column {
                            Text(
                                text = "${index + 1}. ${line.text}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                            Text(
                                text = "conf ${(line.confidence * 100).toInt()}%  •  " +
                                    "y ${(line.top * 100).toInt()}%  •  " +
                                    "x ${(line.left * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
