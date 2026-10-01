package com.mikonoma.drivinglog.auth

import android.content.Intent

fun createAuthRepository(currentActivity: CurrentActivityHolder): AuthRepository = FakeAuthRepository(currentActivity)

fun applyAuthLaunchArguments(authRepository: AuthRepository, intent: Intent) {
    (authRepository as FakeAuthRepository).applyLaunchArguments(intent.getStringExtra("testingStartUser"))
}
