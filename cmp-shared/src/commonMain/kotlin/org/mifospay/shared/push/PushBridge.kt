/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.shared.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Boundary between the host platform's push machinery and shared code.
 *
 * The iOS AppDelegate pushes FCM registration tokens and notification taps in
 * through [onNewToken] / [onNotificationTapped], and assigns
 * [permissionRequester] so shared code can trigger the OS permission prompt at
 * the right moment (post-login, see [PushRegistrationCoordinator]) without
 * knowing anything about UNUserNotificationCenter. Android will wire the same
 * three points from its FirebaseMessagingService when push lands there.
 */
object PushBridge {

    private val _token = MutableStateFlow<String?>(null)

    /** Latest FCM registration token, null until the platform delivers one. */
    val token: StateFlow<String?> = _token

    private val _lastTappedType = MutableStateFlow<String?>(null)

    /**
     * `type` field of the most recently tapped notification
     * (TRANSACTION | LOGIN | OTP | PROMO | STATEMENT). Navigation consumes
     * this to deep-link (Phase 4).
     */
    val lastTappedType: StateFlow<String?> = _lastTappedType

    /**
     * Platform tag reported to the engine's device registry ("ios" | "android").
     * Defaults to "ios" for binaries predating this field; each host sets it
     * explicitly at startup.
     */
    var platformName: String = "ios"

    /** Assigned by the platform at startup; invoking it shows the OS permission prompt. */
    var permissionRequester: (() -> Unit)? = null

    /** Assigned by the platform at startup; invoking it resets the app icon badge. */
    var badgeClearer: (() -> Unit)? = null

    fun onNewToken(token: String) {
        _token.value = token
    }

    fun onNotificationTapped(type: String) {
        _lastTappedType.value = type
    }

    /** Called by the router after acting on a tap so the event fires exactly once. */
    fun consumeTap() {
        _lastTappedType.value = null
    }

    /** @return true if a platform requester was registered and invoked. */
    fun requestPermission(): Boolean {
        val requester = permissionRequester ?: return false
        requester.invoke()
        return true
    }

    fun clearBadge() {
        badgeClearer?.invoke()
    }
}
