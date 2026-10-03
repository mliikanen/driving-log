package com.mikonoma.drivinglog.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal val TestAccountA = AuthState.SignedIn(
    uid = "maestro-test-user-a",
    displayName = "Test Account A",
    email = "test-account-a@maestro.local",
    photoUrl = null,
)

internal val TestAccountB = AuthState.SignedIn(
    uid = "maestro-test-user-b",
    displayName = "Test Account B",
    email = "test-account-b@maestro.local",
    photoUrl = null,
)

/**
 * The `fake` flavor's `AuthRepository` (design.md decisions 6, 7): starts already signed in as Test Account A by
 * default, so the 22 pre-existing Maestro flows need no changes at all — `signIn()`/`signOut()` still work
 * normally from there for manual exploration of a `fake` build. `signIn()` launches [TestAccountPickerActivity] (a
 * plain screen, not a system picker) and suspends for the result, mirroring [FirebaseAuthRepository]'s own "launch
 * something, suspend until it resolves" shape — only what shows the UI differs.
 */
class FakeAuthRepository(private val currentActivity: CurrentActivityHolder) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(TestAccountA)
    private var appliedLaunchArguments = false

    /**
     * Called once from `MainActivity.onCreate` (via [applyAuthLaunchArguments], which reads the
     * `testingStartUser` Intent extra — kept out of this class so the branch logic itself is a plain-value unit
     * test, not one needing Robolectric for a real `Intent`) — a no-op on a later call (e.g. a rotation recreating
     * the activity), so an in-progress session is never reset by it. Lets a Maestro flow declare its starting auth
     * state as test setup (`"none"`/`"A"`/`"B"`), the same way a flow may start from a seeded fixture database
     * (docs/test-strategy.md), instead of building it through the UI.
     */
    fun applyLaunchArguments(startUser: String?) {
        if (appliedLaunchArguments) return
        appliedLaunchArguments = true
        state.value = when (startUser) {
            "none" -> AuthState.SignedOut
            "B" -> TestAccountB
            else -> TestAccountA
        }
    }

    override fun observeAuthState(): Flow<AuthState> = state

    override suspend fun signIn(): Result<Unit> {
        val activity = currentActivity.current
            ?: return Result.failure(IllegalStateException("No current activity to sign in from."))
        return when (TestAccountPickerActivity.pick(activity)) {
            TestAccountPickerActivity.PickResult.ACCOUNT_A -> {
                state.value = TestAccountA
                Result.success(Unit)
            }

            TestAccountPickerActivity.PickResult.ACCOUNT_B -> {
                state.value = TestAccountB
                Result.success(Unit)
            }

            TestAccountPickerActivity.PickResult.CANCELLED -> Result.failure(SignInCancelledException())

            TestAccountPickerActivity.PickResult.SIMULATED_FAILURE -> Result.failure(Exception("Simulated sign-in failure"))
        }
    }

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}
