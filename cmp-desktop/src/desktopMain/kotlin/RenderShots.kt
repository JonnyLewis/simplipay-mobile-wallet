/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Image
import org.mifospay.shared.onboarding.AppIconShot
import org.mifospay.shared.onboarding.BuyServiceShot
import org.mifospay.shared.onboarding.BuyShot
import org.mifospay.shared.onboarding.HistoryShot
import org.mifospay.shared.onboarding.HomeShot
import org.mifospay.shared.onboarding.KycReviewShot
import org.mifospay.shared.onboarding.KycVerifyShot
import org.mifospay.shared.onboarding.LandingRenderPreview
import org.mifospay.shared.onboarding.LoadingShot
import org.mifospay.shared.onboarding.LogoPainterShot
import org.mifospay.shared.onboarding.LoginShot
import org.mifospay.shared.onboarding.ProfileShot
import org.mifospay.shared.onboarding.SplashRenderPreview
import java.io.File

/**
 * Headless render harness: draws the onboarding screens off-screen with
 * [ImageComposeScene] (Skia, no AWT window) and writes PNGs. Advances ~1.5s of
 * frames so resource fonts load and the particle field / rosette settle into a
 * representative state. Run with `./gradlew :cmp-desktop:renderShots -PshotsOut=<dir>`.
 */
private fun renderOne(name: String, outDir: String, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = 720, height = 1560, density = Density(2f), content = content)
    try {
        var nanos = 0L
        var img: Image = scene.render(nanos)
        repeat(90) {
            nanos += 16_000_000L
            img = scene.render(nanos)
        }
        val data = img.encodeToData() ?: error("PNG encode failed for $name")
        File(outDir).mkdirs()
        val out = File(outDir, "$name.png")
        out.writeBytes(data.bytes)
        println("WROTE ${out.absolutePath}")
    } finally {
        scene.close()
    }
}

private fun renderSquare(name: String, outDir: String, sizePx: Int, content: @Composable () -> Unit) {
    val scene = ImageComposeScene(width = sizePx, height = sizePx, density = Density(4f), content = content)
    try {
        var nanos = 0L
        var img: Image = scene.render(nanos)
        repeat(60) {
            nanos += 16_000_000L
            img = scene.render(nanos)
        }
        val data = img.encodeToData() ?: error("PNG encode failed for $name")
        File(outDir).mkdirs()
        val out = File(outDir, "$name.png")
        out.writeBytes(data.bytes)
        println("WROTE ${out.absolutePath}")
    } finally {
        scene.close()
    }
}

fun main(args: Array<String>) {
    val outDir = args.getOrNull(0) ?: "build/shots"
    renderSquare("appicon", outDir, sizePx = 1024) { AppIconShot() }
    renderOne("splash", outDir) { SplashRenderPreview() }
    renderOne("landing", outDir) { LandingRenderPreview() }
    renderOne("loading", outDir) { LoadingShot() }
    renderOne("login", outDir) { LoginShot() }
    renderOne("home", outDir) { HomeShot() }
    renderOne("history", outDir) { HistoryShot() }
    renderOne("profile", outDir) { ProfileShot() }
    renderOne("buy", outDir) { BuyShot() }
    renderOne("buy_service", outDir) { BuyServiceShot() }
    renderOne("logo_painter", outDir) { LogoPainterShot() }
    renderOne("kyc_verify", outDir) { KycVerifyShot() }
    renderOne("kyc_review", outDir) { KycReviewShot() }
}
