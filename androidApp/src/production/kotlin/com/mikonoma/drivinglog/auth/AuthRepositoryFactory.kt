package com.mikonoma.drivinglog.auth

import android.content.Intent

fun createAuthRepository(currentActivity: CurrentActivityHolder): AuthRepository = FirebaseAuthRepository(currentActivity)

/** The real flavor has no test-only launch arguments to apply. */
@Suppress("UnusedParameter") // The signature is shared with the fake flavor's, which uses both.
fun applyAuthLaunchArguments(authRepository: AuthRepository, intent: Intent) {
    // Nothing to apply in the real flavor.
}
