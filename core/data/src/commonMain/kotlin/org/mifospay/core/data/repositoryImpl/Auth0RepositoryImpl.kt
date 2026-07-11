package org.mifospay.core.data.repositoryImpl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.mifospay.core.common.DataState
import org.mifospay.core.data.repository.Auth0Identity
import org.mifospay.core.data.repository.Auth0Repository
import org.mifospay.core.model.user.NewUser
import org.mifospay.core.network.Auth0ApiManager
import org.mifospay.core.network.config.Auth0Config
import org.mifospay.core.network.model.auth0.Auth0SignupPayload
import org.mifospay.core.network.model.auth0.Auth0TokenPayload
import kotlin.io.encoding.Base64

class Auth0RepositoryImpl(
    private val api: Auth0ApiManager,
    private val ioDispatcher: CoroutineDispatcher,
) : Auth0Repository {
    override suspend fun signup(user: NewUser, mobileNumber: String): DataState<Auth0Identity> = try {
        if (Auth0Config.CLIENT_ID.isBlank()) error("Auth0 mobile client ID is not configured")
        withContext(ioDispatcher) {
            api.auth0Api.signup(
                Auth0SignupPayload(
                    client_id = Auth0Config.CLIENT_ID,
                    connection = Auth0Config.DATABASE_CONNECTION,
                    username = user.username,
                    email = user.email,
                    password = user.password,
                    user_metadata = mapOf(
                        "first_name" to user.firstname,
                        "last_name" to user.lastname,
                        "mobile_number" to mobileNumber,
                    ),
                ),
            )
            login(user.username, user.password)
        }
    } catch (e: Exception) {
        DataState.Error(e)
    }

    override suspend fun login(username: String, password: String): DataState<Auth0Identity> = try {
        if (Auth0Config.CLIENT_ID.isBlank()) error("Auth0 mobile client ID is not configured")
        val token = withContext(ioDispatcher) {
            api.auth0Api.token(
                Auth0TokenPayload(
                    username = username,
                    password = password,
                    realm = Auth0Config.DATABASE_CONNECTION,
                    client_id = Auth0Config.CLIENT_ID,
                    audience = Auth0Config.AUDIENCE,
                ),
            )
        }
        val subject = token.id_token?.let(::jwtSubject).orEmpty().ifBlank { username }
        DataState.Success(Auth0Identity(subject, token.access_token))
    } catch (e: Exception) {
        DataState.Error(e)
    }

    private fun jwtSubject(jwt: String): String {
        val payload = jwt.split('.').getOrNull(1) ?: return ""
        val normalized = payload.replace('-', '+').replace('_', '/')
            .padEnd(((payload.length + 3) / 4) * 4, '=')
        val json = Base64.decode(normalized).decodeToString()
        return Regex("\\\"sub\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(json)?.groupValues?.get(1).orEmpty()
    }
}
