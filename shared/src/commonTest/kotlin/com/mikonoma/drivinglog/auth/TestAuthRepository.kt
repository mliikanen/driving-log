package com.mikonoma.drivinglog.auth

import kotlinx.coroutines.flow.MutableStateFlow

/** A controllable fake for processor unit tests — distinct from `FakeAuthRepository`, the `fake` Android flavor's
 * own shipped implementation that Maestro installs. This one is a plain commonTest double, fresh per test case. */
class TestAuthRepository(initialState: AuthState = AuthState.SignedOut) : AuthRepository {
    private val state = MutableStateFlow(initialState)

    var signInResult: Result<Unit> = Result.success(Unit)
    var signInCallCount = 0
        private set

    override fun observeAuthState() = state

    override suspend fun signIn(): Result<Unit> {
        signInCallCount++
        signInResult.onSuccess {
            state.value = AuthState.SignedIn(uid = "test-uid", displayName = "Test User", email = "test@example.com", photoUrl = null)
        }
        return signInResult
    }

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}
