package com.mikonoma.drivinglog.auth

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInProcessorTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun successReachesASignedInStateWithNoError() = runTest {
        val repository = TestAuthRepository()
        val processor = SignInProcessor(repository)

        processor.test { dispatch(SignInIntent.SignIn) }

        assertEquals(1, repository.signInCallCount)
        assertFalse(processor.state.isSigningIn)
        assertNull(processor.state.error)
    }

    @Test
    fun cancellationLeavesTheScreenOnItselfWithNoError() = runTest {
        val repository = TestAuthRepository().apply { signInResult = Result.failure(SignInCancelledException()) }
        val processor = SignInProcessor(repository)

        processor.test { dispatch(SignInIntent.SignIn) }

        assertFalse(processor.state.isSigningIn)
        assertNull(processor.state.error)
    }

    @Test
    fun failureLeavesTheScreenOnItselfWithAnError() = runTest {
        val repository = TestAuthRepository().apply { signInResult = Result.failure(RuntimeException("no network")) }
        val processor = SignInProcessor(repository)

        processor.test { dispatch(SignInIntent.SignIn) }

        assertFalse(processor.state.isSigningIn)
        assertNotNull(processor.state.error)
    }
}
