/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.send.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType

@Composable
actual fun ContactPermissionHandler() {
    // iOS shows the system prompt itself; requesting on first composition mirrors
    // the Android PermissionBox flow without adding a second in-app dialog layer.
    LaunchedEffect(Unit) {
        CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { _, _ -> }
    }
}
