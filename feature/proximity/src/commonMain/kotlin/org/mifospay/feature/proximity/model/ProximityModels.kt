/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.proximity.model

/**
 * On-air device classifier (spec §5.1, §10). Encoded in the low 3 bits of the
 * advertisement's `device_class` byte; the high 5 bits are reserved.
 *
 * The on-air value is a HINT only — the authoritative class comes from the
 * server-signed resolve payload (spec §5.6).
 */
enum class DeviceClass(val wire: Int) {
    P2P(0x01),
    DONATION(0x02),
    POS(0x03),
    ;

    companion object {
        fun fromWire(value: Int): DeviceClass? = entries.firstOrNull { it.wire == (value and 0x07) }
    }
}

/** Whether the receiver wants a fixed amount or "pay what you want" (spec §8.2). */
enum class AmountMode { Open, Fixed }

/**
 * The human-comparable visual identifier — a colour + emoji pair (spec §5.7, §14.2).
 * Two roles share this shape but are derived differently:
 *  - the session **badge** (pick-the-right-person, shown always), and
 *  - the pairwise **match handle** (anti-rebroadcast-relay, escalation only).
 */
data class Handle(
    val color: HandleColor,
    val symbol: HandleSymbol,
)

enum class HandleColor(val emoji: String) {
    Green("🟢"),
    Blue("🔵"),
    Purple("🟣"),
    Red("🔴"),
    Orange("🟠"),
    Yellow("🟡"),
    Brown("🟤"),
    Black("⚫"),
}

enum class HandleSymbol(val emoji: String) {
    Fox("🦊"),
    Whale("🐳"),
    Owl("🦉"),
    Otter("🦦"),
    Bee("🐝"),
    Cat("🐱"),
    Frog("🐸"),
    Bear("🐻"),
    Lion("🦁"),
    Panda("🐼"),
    Penguin("🐧"),
    Turtle("🐢"),
    Rabbit("🐰"),
    Dolphin("🐬"),
    Wolf("🐺"),
    Tiger("🐯"),
}

/**
 * Estimated proximity to a discovered receiver/POS (spec §6.5). RSSI-derived,
 * coarse, **never a security claim**. [meters] is null when confidence is too
 * low to render a number (fall back to [band]).
 */
data class DistanceEstimate(
    val meters: Double?,
    val band: ProximityBand,
    // true only when UWB ranging is active (same-platform)
    val precise: Boolean,
)

enum class ProximityBand { VeryClose, Nearby, InTheRoom, Unknown }

/**
 * A nearby receiver as seen on the radar before connecting — identified only by
 * the ephemeral scan id, with signal strength. Name/amount come later (after
 * GATT + resolve). [rssi] is in dBm (higher = closer).
 */
data class NearbyDevice(
    val id: String,
    val rssi: Int,
) {
    /** Coarse RSSI → band (spec §6.5); never a precise/metric or security claim. */
    val band: ProximityBand
        get() = when {
            rssi >= -55 -> ProximityBand.VeryClose
            rssi >= -75 -> ProximityBand.Nearby
            rssi >= -90 -> ProximityBand.InTheRoom
            else -> ProximityBand.Unknown
        }
}
