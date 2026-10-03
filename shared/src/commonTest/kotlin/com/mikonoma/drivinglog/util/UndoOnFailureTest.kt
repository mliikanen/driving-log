package com.mikonoma.drivinglog.util

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UndoOnFailureTest {

    @Test
    fun successReturnsTheResultWithoutUndoing() {
        var undone = false
        val result = undoOnFailure(undo = { undone = true }) { 42 }
        assertEquals(43, result)
        assertFalse(undone)
    }

    @Test
    fun failureUndoesAndRethrowsTheSameException() {
        var undone = false
        val failure = IllegalStateException("disk full")
        val thrown = assertFailsWith<IllegalStateException> {
            undoOnFailure(undo = { undone = true }) { throw failure }
        }
        assertTrue(undone)
        assertSame(failure, thrown)
    }

    @Test
    fun cancellationUndoesAndStillPropagates() {
        var undone = false
        val cancellation = CancellationException("left the screen")
        val thrown = assertFailsWith<CancellationException> {
            undoOnFailure(undo = { undone = true }) { throw cancellation }
        }
        assertTrue(undone)
        assertSame(cancellation, thrown)
    }
}
