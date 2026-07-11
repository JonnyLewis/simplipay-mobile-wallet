package org.mifospay.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import org.mifospay.core.network.model.auth0.Auth0SignupPayload
import org.mifospay.core.network.model.auth0.Auth0SignupResponse
import org.mifospay.core.network.model.auth0.Auth0TokenPayload
import org.mifospay.core.network.model.auth0.Auth0TokenResponse

interface Auth0Service {
    @POST("dbconnections/signup")
    suspend fun signup(@Body payload: Auth0SignupPayload): Auth0SignupResponse

    @POST("oauth/token")
    suspend fun token(@Body payload: Auth0TokenPayload): Auth0TokenResponse
}
