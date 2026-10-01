package com.mikonoma.drivinglog.auth

import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

/** `firebase-auth`'s "Signing in uses Google": offers "Sign in with Google," shows an error on failure/cancellation
 * without crashing. The actual move to the signed-in screen follows `AuthState` directly (App.kt), not an effect
 * here. */
class SignInProcessor(private val authRepository: AuthRepository) :
    PresentationProcessor<SignInIntent, SignInState, SignInEffect>(SignInState()) {

    override suspend fun map(intent: SignInIntent): Action<SignInState, SignInEffect>? = when (intent) {
        SignInIntent.SignIn -> signIn()
    }

    private fun signIn(): Action<SignInState, SignInEffect>? {
        if (state.isSigningIn) return null
        return async("sign-in") {
            reduce { copy(isSigningIn = true, error = null) }
            val result = authRepository.signIn()
            result.fold(
                onSuccess = { reduce { copy(isSigningIn = false) } },
                // Cancelling leaves no error (`firebase-auth`'s "Cancelling the account picker" scenario); any
                // other failure shows one (its "A sign-in failure shows an error" scenario).
                onFailure = { error ->
                    reduce {
                        copy(
                            isSigningIn = false,
                            error = if (error is SignInCancelledException) null else "Couldn't sign in. Check your connection and try again.",
                        )
                    }
                },
            )
        }
    }
}
