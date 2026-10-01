package com.mikonoma.drivinglog.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Covers the plain-value logic ([FakeAuthRepository.applyLaunchArguments], `signOut()`, and `signIn()`'s
 * no-current-activity failure). The picker outcomes themselves (`signIn()` reaching `TestAccountPickerActivity`)
 * need a real `Activity`, which this project has no Robolectric setup for yet (`docs/test-strategy.md`'s own
 * "not yet adopted" tier) — verified instead by the new `maestro/auth/` flows (design.md decision 7).
 */
class FakeAuthRepositoryTest {

    @Test
    fun startsAlreadySignedInAsTestAccountAByDefault() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        assertEquals(TestAccountA, repository.observeAuthState().first())
    }

    @Test
    fun applyLaunchArgumentsNoneStartsSignedOut() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        repository.applyLaunchArguments("none")

        assertIs<AuthState.SignedOut>(repository.observeAuthState().first())
    }

    @Test
    fun applyLaunchArgumentsBStartsSignedInAsTestAccountB() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        repository.applyLaunchArguments("B")

        assertEquals(TestAccountB, repository.observeAuthState().first())
    }

    @Test
    fun applyLaunchArgumentsIsANoOpOnceAlreadyApplied() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        repository.applyLaunchArguments("none")
        repository.applyLaunchArguments("B") // e.g. a rotation recreating the activity

        assertIs<AuthState.SignedOut>(repository.observeAuthState().first())
    }

    @Test
    fun signOutReachesSignedOut() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        repository.signOut()

        assertIs<AuthState.SignedOut>(repository.observeAuthState().first())
    }

    @Test
    fun signInFailsSafelyWithNoCurrentActivity() = runTest {
        val repository = FakeAuthRepository(CurrentActivityHolder())

        val result = repository.signIn()

        assertTrue(result.isFailure)
    }
}
