package com.mikonoma.drivinglog.auth

import kotlinx.coroutines.flow.Flow

/** Whether the user is signed in, and with which Google account. */
sealed interface AuthState {
    /** The first callback from the real session store hasn't fired yet; genuinely unknown. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
    ) : AuthState
}

/**
 * Gates the app behind a Google account (`firebase-auth`). Constructed by each platform shell and passed into
 * [com.mikonoma.drivinglog.di.AppGraph.Factory.create] — not `expect`/`actual` — the same shape as
 * [com.mikonoma.drivinglog.vehicle.ocr.TextRecognizer]/[com.mikonoma.drivinglog.vehicle.picture.ImageCodec].
 */
interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>

    suspend fun signIn(): Result<Unit>

    suspend fun signOut()
}

/**
 * `signIn()`'s failure cause when the user dismissed the account picker without choosing one — distinct from every
 * other failure, since `firebase-auth` requires cancelling to leave no error on screen while a real failure shows
 * one ([com.mikonoma.drivinglog.auth.SignInProcessor] branches on this, not on a failure message).
 */
class SignInCancelledException : Exception("Sign-in cancelled")
