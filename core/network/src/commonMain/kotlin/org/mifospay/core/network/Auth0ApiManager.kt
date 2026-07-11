package org.mifospay.core.network

class Auth0ApiManager(private val ktorfitClient: KtorfitClient) {
    val auth0Api by lazy { ktorfitClient.auth0Api }
}
