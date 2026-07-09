/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.auth.signup.kyc

/**
 * Parsed contents of a South African Smart ID card PDF417 barcode.
 *
 * The payload is a plaintext, pipe-delimited string in this fixed field order:
 *
 * ```
 * SURNAME | NAME | GENDER | NATIONALITY | ID NUMBER | BIRTH DATE |
 * COUNTRY OF BIRTH | CITIZENSHIP STATUS | ISSUE DATE | SECURITY NUMBER |
 * SMART ID NUMBER | FILLER
 * ```
 *
 * Trailing fields (issue date onwards) are not always present, so parsing is lenient
 * about anything past the birth date.
 */
data class SaSmartIdBarcode(
    val surname: String,
    val firstNames: String,
    val gender: String,
    val nationality: String,
    val idNumber: String,
    val birthDate: String,
    val countryOfBirth: String,
    val citizenshipStatus: String,
    val issueDate: String,
    val securityNumber: String,
    val smartIdNumber: String,
) {
    /** "Male" / "Female" where recognisable, otherwise the raw token. */
    val genderLabel: String
        get() = when (gender.trim().uppercase().firstOrNull()) {
            'M' -> "Male"
            'F' -> "Female"
            else -> gender.trim()
        }
}

private const val MIN_FIELDS = 6

/**
 * Parses a scanned SA Smart ID PDF417 payload. Returns `null` if the string is clearly
 * not in the expected pipe-delimited format (so callers can fall back to showing the
 * raw payload). Whitespace around each field is trimmed.
 */
fun parseSaSmartIdBarcode(raw: String): SaSmartIdBarcode? {
    if (!raw.contains('|')) return null
    val parts = raw.split('|')
    if (parts.size < MIN_FIELDS) return null

    fun at(index: Int): String = parts.getOrNull(index)?.trim().orEmpty()

    return SaSmartIdBarcode(
        surname = at(0),
        firstNames = at(1),
        gender = at(2),
        nationality = at(3),
        idNumber = at(4),
        birthDate = at(5),
        countryOfBirth = at(6),
        citizenshipStatus = at(7),
        issueDate = at(8),
        securityNumber = at(9),
        smartIdNumber = at(10),
    )
}
