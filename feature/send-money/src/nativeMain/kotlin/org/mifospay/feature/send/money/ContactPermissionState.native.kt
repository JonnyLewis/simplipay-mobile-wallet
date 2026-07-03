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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun rememberContactPermissionState(): ContactPermissionState {
    var currentStatus by remember { mutableStateOf(readAuthorizationStatus()) }

    return remember {
        object : ContactPermissionState {
            override val status: ContactPermissionStatus
                get() = currentStatus

            override fun requestContactPermission() {
                CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                    currentStatus = if (granted) {
                        ContactPermissionStatus.Granted
                    } else {
                        ContactPermissionStatus.Denied
                    }
                }
            }

            override fun goToSettings() {
                val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
                UIApplication.sharedApplication.openURL(settingsUrl, emptyMap<Any?, Any>(), null)
            }
        }
    }
}

private fun readAuthorizationStatus(): ContactPermissionStatus {
    val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
    return if (status == CNAuthorizationStatusAuthorized) {
        ContactPermissionStatus.Granted
    } else {
        ContactPermissionStatus.Denied
    }
}
