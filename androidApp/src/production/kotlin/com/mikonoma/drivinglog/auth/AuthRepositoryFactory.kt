package com.mikonoma.drivinglog.auth

import android.content.Intent

fun createAuthRepository(currentActivity: CurrentActivityHolder): AuthRepository = FirebaseAuthRepository(currentActivity)

/** The real flavor has no test-only launch arguments to apply. */
fun applyAuthLaunchArguments(authRepository: AuthRepository, intent: Intent) {}
