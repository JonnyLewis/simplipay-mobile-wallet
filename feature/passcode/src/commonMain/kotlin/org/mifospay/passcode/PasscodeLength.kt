/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.passcode

/** Vendored from openMF/mifos-passcode-cmp (MPL-2.0). */
enum class PasscodeLength(val length: Int) {
    FOUR_DIGIT(4),
    SIX_DIGIT(6),
}
