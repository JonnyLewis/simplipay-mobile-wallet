package org.mifospay.core.data.repository

import org.mifospay.core.common.DataState
import org.mifospay.core.model.user.NewUser

data class Auth0Identity(val subject: String, val accessToken: String)

interface Auth0Repository {
    suspend fun signup(user: NewUser, mobileNumber: String): DataState<Auth0Identity>
    suspend fun login(username: String, password: String): DataState<Auth0Identity>
}
