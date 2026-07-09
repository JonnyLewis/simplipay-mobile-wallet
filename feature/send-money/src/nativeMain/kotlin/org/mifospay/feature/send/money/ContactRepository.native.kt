/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.mifospay.feature.send.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Contacts.CNContact
import platform.Contacts.CNContactFamilyNameKey
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactGivenNameKey
import platform.Contacts.CNContactIdentifierKey
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber

@Composable
actual fun rememberContactRepository(): ContactRepository {
    return remember { IosContactRepository() }
}

private class IosContactRepository : ContactRepository {

    override suspend fun getContacts(): List<Contact> = withContext(Dispatchers.Default) {
        PhoneNumberUtils.filterAndFormatContacts(fetchDeviceContacts())
    }

    override suspend fun searchContacts(query: String): List<Contact> {
        val trimmed = query.trim()
        val contacts = getContacts()
        if (trimmed.isEmpty()) return contacts
        return contacts.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.phoneNumber.contains(trimmed)
        }
    }

    private fun fetchDeviceContacts(): List<Contact> {
        val keys = listOf(
            CNContactIdentifierKey,
            CNContactGivenNameKey,
            CNContactFamilyNameKey,
            CNContactPhoneNumbersKey,
        )
        val request = CNContactFetchRequest(keysToFetch = keys)
        val contacts = mutableListOf<Contact>()
        CNContactStore().enumerateContactsWithFetchRequest(request, error = null) { cnContact, _ ->
            val contact = cnContact?.toContactOrNull()
            if (contact != null) {
                contacts.add(contact)
            }
        }
        return contacts
    }

    private fun CNContact.toContactOrNull(): Contact? {
        val name = listOf(givenName, familyName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val firstNumber = phoneNumbers
            .filterIsInstance<CNLabeledValue>()
            .firstNotNullOfOrNull { (it.value as? CNPhoneNumber)?.stringValue }
        if (name.isBlank() || firstNumber.isNullOrBlank()) return null
        return Contact(
            id = identifier,
            name = name,
            phoneNumber = firstNumber,
        )
    }
}
