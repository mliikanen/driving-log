package com.mikonoma.drivinglog.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.mikonoma.drivinglog.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The real implementation (design.md decisions 1, 2): Credential Manager's Google ID flow for sign-in, Firebase
 * Auth for the session itself. Needs a current [Activity][android.app.Activity] (from [CurrentActivityHolder]) to
 * launch the system account picker from, since one doesn't exist yet when this is constructed at `Application`
 * scope.
 */
class FirebaseAuthRepository(private val currentActivity: CurrentActivityHolder) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth -> trySend(firebaseAuth.currentUser.toAuthState()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(): Result<Unit> {
        val activity = currentActivity.current
            ?: return Result.failure(IllegalStateException("No current activity to sign in from."))
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential).awaitResult()
            Result.success(Unit)
        } catch (e: GetCredentialCancellationException) {
            // Distinct from every other failure: `firebase-auth` requires cancelling to leave no error on screen
            // (SignInProcessor branches on the exception type, not a message).
            Result.failure(SignInCancelledException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser?.toAuthState(): AuthState =
        this?.let { AuthState.SignedIn(uid = it.uid, displayName = it.displayName, email = it.email, photoUrl = it.photoUrl?.toString()) }
            ?: AuthState.SignedOut
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
