package com.mikonoma.drivinglog.auth

import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

data class SignInState(val isSigningIn: Boolean = false, val error: String? = null) : ViewState

sealed interface SignInIntent : ViewIntent {
    data object SignIn : SignInIntent
}

/** The actual transition to the signed-in screen follows `AuthRepository.observeAuthState()` directly (App.kt), not
 * a side effect from this processor — there's nothing this screen itself needs to signal. */
sealed interface SignInEffect : SideEffect
