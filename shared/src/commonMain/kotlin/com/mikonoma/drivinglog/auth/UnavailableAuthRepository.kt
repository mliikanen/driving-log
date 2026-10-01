package com.mikonoma.drivinglog.auth

import kotlinx.coroutines.flow.MutableStateFlow

/** iOS has no Credential Manager/Firebase Auth integration yet (`add-firebase-auth` builds Android only). */
object UnavailableAuthRepository : AuthRepository {
    private val state = MutableStateFlow<AuthState>(AuthState.SignedOut)

    override fun observeAuthState() = state

    override suspend fun signIn(): Result<Unit> = Result.failure(UnsupportedOperationException("Sign-in is not available on this platform yet."))

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}
