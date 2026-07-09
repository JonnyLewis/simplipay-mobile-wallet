/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.core.model.client

import org.mifospay.core.common.Parcelable
import org.mifospay.core.common.Parcelize

@Parcelize
data class NewClient(
    val firstname: String,
    val lastname: String,
    val externalId: String,
    val mobileNo: String,
    // Optional: the Fineract backend has the address feature disabled (enable-address=false),
    // so a client can be created without one. Null when the user skips the address.
    val address: ClientAddress? = null,
    val savingsProductId: Int,
) : Parcelable
