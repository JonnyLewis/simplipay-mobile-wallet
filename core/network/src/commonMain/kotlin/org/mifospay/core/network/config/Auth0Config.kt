package org.mifospay.core.network.config

/** Public Auth0 settings for the native mobile application. No client secret belongs here. */
object Auth0Config {
    const val DOMAIN = "simplipay.eu.auth0.com"
    const val CLIENT_ID = "07LfaEKDp4oBLOXgsjZBb7eeUci4TNL2"
    const val AUDIENCE = "https://api.simplipay.co.za/"
    const val DATABASE_CONNECTION = "Username-Password-Authentication"
}
